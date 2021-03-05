/*
CNT 5105C "Computer Networks" - Spring 2021
Group Project - P2P File Sharing
Benjamin Hsu, Lavanya Khular, Chitranshu Raj
*/

//Created package to group together all the various .java files together
package src.modules;

//Imports
import java.io.*; //IOException, OutputStream, DataOutputStream
import java.net.*; //SocketException
import java.nio.charset.*; //StandardCharsets
import java.util.*; //Queue, Random
import java.util.concurrent.*; //ArrayBlockingQueue

public class ClientThread extends Thread
{
    //parameters
    private PeerObject neighborPeer;
    private PeerObject myPeer;
    private LogWriter logger;
    private final Object peerProcessLock;
    private final Object clientThreadLock;
    private DataOutputStream socketStream;

    //ArrayBlockingQueue for thread-safe message passing between threads
    //its constructor requires specifying outright the capacity of the queue
    //assume that 100 capacity is good enough??? (unknown how much slower the ClientThread might be than the ServerThread)
    private volatile Queue<ThreadMessage> messagesFromServer = new ArrayBlockingQueue<ThreadMessage>(100);

    //constructor
    public ClientThread(PeerObject neighborPeer, PeerObject myPeer, LogWriter logger, Object peerProcessLock, Object clientThreadLock)
    {
        this.neighborPeer = neighborPeer;
        this.myPeer = myPeer;
        this.logger = logger;
        this.peerProcessLock = peerProcessLock;
        this.clientThreadLock = clientThreadLock;
    }

    //the "main" method (override the run() method) that is executed for the Thread
    public void run()
    {
        try
        {
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " STARTED.\n");
            //create a DataOutputStream (using a OutputStream in the constructor) that can send data to the TCP socket
            this.socketStream = new DataOutputStream(this.neighborPeer.getSocket().getOutputStream());

            //send the initial handshake
            sendHandshake();
            //send the bitfield after the initial handshake
            sendBitfield();

            //continously send messages to the TCP socket until both peers of the socket have the file
            while(false == this.myPeer.getHasFile() || false == this.neighborPeer.getHasFile())
            {
                //process any incoming task messages from the sibling ServerThread
                if(false == this.messagesFromServer.isEmpty())
                {
                    ThreadMessage topMessage = this.messagesFromServer.remove();

                    //examine what kind of message task the ServerThread sent
                    if(ThreadMessage.ThreadMessageType.BITFIELD == topMessage.getThreadMessageType())
                    {
                        processBitfieldMessage(topMessage);
                    }
                    else if(ThreadMessage.ThreadMessageType.INTERESTSTATUS == topMessage.getThreadMessageType())
                    {
                        processInterestStatusMessage(topMessage);
                    }
                    else if(ThreadMessage.ThreadMessageType.SENDCHOKE == topMessage.getThreadMessageType())
                    {
                        sendChokeOrUnchoke(true);
                    }
                    else if(ThreadMessage.ThreadMessageType.SENDUNCHOKE == topMessage.getThreadMessageType())
                    {
                        sendChokeOrUnchoke(false);
                    }
                    else if(ThreadMessage.ThreadMessageType.RECEIVEDCHOKE == topMessage.getThreadMessageType())
                    {
                        processChokeOrUnchoke(true);
                    }
                    else if(ThreadMessage.ThreadMessageType.RECEIVEDUNCHOKE == topMessage.getThreadMessageType())
                    {
                        processChokeOrUnchoke(false);
                    }
                    else if(ThreadMessage.ThreadMessageType.REQUEST == topMessage.getThreadMessageType())
                    {
                        processRequest(topMessage);
                    }
                    else
                    {
                        System.out.print("ERROR: CLIENTTHREAD " + this.neighborPeer.getPeerId() + " GOT AN UNKNOWN THREADMESSAGE TYPE.\n");
                    }
                }
                //otherwise, determine a piece to request for
                else
                {
                    determineRequest();
                }
            }

Thread.sleep(5000);
            //close the socket (which should cause the ServerThread to close via SocketException or EOFException)
            this.neighborPeer.getSocket().close();
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " told ServerThread to kill itself.\n");
        }
catch(InterruptedException e)
{

}
        catch(IOException exception)
        {
            System.out.print("ERROR: ClientThread.java --- some IO error in the loop that sends to the TCP connection.\n");
            exception.printStackTrace();
            System.exit(1);
        }
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " ENDED.\n");
    }

    //helper methods
    public void addThreadMessage(ThreadMessage messageFromServer)
    {
        this.messagesFromServer.add(messageFromServer);
    }

    private void processBitfieldMessage(ThreadMessage bitfieldMessage) throws IOException
    {
        this.neighborPeer.setBitfieldFromBytes(bitfieldMessage.getBitfield(), this.logger);

        determineInterest();
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " processed Bitfield from ServerThread.\n");
    }

    private void determineInterest() throws IOException
    {
        synchronized(this.clientThreadLock)
        {
            boolean initialInterestState = this.neighborPeer.getMyInterested();
            boolean neighborHasMissing = false;
            //find if the neighbor has any missing piece
            for(int i = 0; i < ReadCommon.getNumberOfPieces(); i++)
            {
                if(false == this.myPeer.hasPiece(i) && true == this.neighborPeer.hasPiece(i))
                {
                    neighborHasMissing = true;
                    break;
                }
            }
            
            //check if the interest state should change and change interest state (and send message to peer) if so
            if(false == initialInterestState && true == neighborHasMissing)
            {
                this.neighborPeer.setMyInterested(true);
                sendInterested();
            }
            else if(true == initialInterestState && false == neighborHasMissing)
            {
                this.neighborPeer.setMyInterested(false);
                sendNotInterested();
            }
        }
    }

    private void determineRequest() throws IOException
    {
        synchronized(this.clientThreadLock)
        {
            synchronized(this.peerProcessLock)
            {
                //can only request a piece if is unchoked by the neighbor and if is interested in the neighbor
                if(false == this.neighborPeer.getNeighborChoked() && true == this.neighborPeer.getMyInterested())
                {
                    //find if this peer has already requested a piece from the neighbor
                    //also identify pieces that can randomly select as the requested piece
                    List<Integer> potentialRequestPieces = new ArrayList<Integer>();
                    for(int i = 0; i < ReadCommon.getNumberOfPieces(); i++)
                    {
                        //if a piece is already currently requested from the neighbor, abort
                        if(this.neighborPeer.getPeerId() == this.myPeer.getRequested(i))
                        {
                            return;
                        }
                        //otherwise, check if the piece has not been requested and is required
                        if(-1 == this.myPeer.getRequested(i) && false == this.myPeer.hasPiece(i))
                        {
                            potentialRequestPieces.add(i);
                        }
                    }

                    //if there are any potential request pieces, choose one randomly and send a request message
                    if(false == potentialRequestPieces.isEmpty())
                    {
                        Random random = new Random();
                        int randomIndex = random.nextInt(potentialRequestPieces.size());

                        //mark the requested piece as requested
                        this.myPeer.setRequested(potentialRequestPieces.get(randomIndex), this.neighborPeer.getPeerId());
                        
                        //send a request message
                        sendRequest(potentialRequestPieces.get(randomIndex));
                    }
                }
            }
        }
    }

    private void sendRequest(int pieceIndex) throws IOException
    {
        //set message length (message type 1 byte + 4 byte int payload)
        int messageLength = 1 + 4;
        //send the 4-byte int message length
        socketStream.writeInt(messageLength);

        //send the 1-byte message type (6 = request)
        socketStream.writeByte(6);

        //send the 4-byte int piece that is requested
        socketStream.writeInt(pieceIndex);
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " sent Request Piece # " + pieceIndex + " message.\n");
    }

    private void processInterestStatusMessage(ThreadMessage interestStatusMessage)
    {
        //change the PeerObject value for the neighbor peer
        this.neighborPeer.setNeighborInterested(interestStatusMessage.getInterestStatus());

        //log the event
        if(true == interestStatusMessage.getInterestStatus())
        {
            this.logger.logInterested(this.neighborPeer.getPeerId());
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " processed Interested message from ServerThread.\n");
        }
        else
        {
            this.logger.logNotInterested(this.neighborPeer.getPeerId());
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " processed NOT-Interested message from ServerThread.\n");
        }
    }

    private void sendHandshake() throws IOException
    {
        String headerAsString = "P2PFILESHARINGPROJ";
        //convert the string to a byte array
        byte[] headerAsBytes = headerAsString.getBytes(StandardCharsets.UTF_8);
        //send the header
        socketStream.write(headerAsBytes, 0, headerAsBytes.length);

        //byte array to hold the 10-byte zero bytes portion of the handshake
        byte[] zeroBytes = new byte[10];
        //populate the bytes with 0 bits bytes (null chars)
        for(int i = 0; i < zeroBytes.length; i++)
        {
            zeroBytes[i] = (byte)0;
        }
        //send the 0-bytes
        socketStream.write(zeroBytes, 0, zeroBytes.length);

        //send the 4-byte int peerId
        socketStream.writeInt(this.myPeer.getPeerId());
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " sent Handshake.\n");
    }

    private void sendBitfield() throws IOException
    {
        synchronized(this.clientThreadLock)
        {
            //get the bitfield as bytes
            byte[] bitfield = myPeer.getBitfieldAsBytes();

            //calculate the message length (message type 1 byte + the bitfield length in bytes)
            int messageLength = 1 + bitfield.length;
            //send the 4-byte int message length
            socketStream.writeInt(messageLength);

            //send the 1-byte message type (5 = bitfield)
            socketStream.writeByte(5);

            //send the bitfield
            socketStream.write(bitfield, 0, bitfield.length);
        }
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " sent Bitfield.\n");
    }

    private void processChokeOrUnchoke(boolean choked) throws IOException
    {
        synchronized(this.clientThreadLock)
        {
            if(true == choked)
            {
                this.neighborPeer.setNeighborChoked(true);
                this.logger.logChoked(this.neighborPeer.getPeerId());

                //also clear out any record of current piece requests to the neighbor that just choked this peer
                this.myPeer.clearRequested(this.neighborPeer.getPeerId());
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " processed Choke message from ServerThread.\n");
            }
            else
            {
                this.neighborPeer.setNeighborChoked(false);
                this.logger.logUnchoked(this.neighborPeer.getPeerId());
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " processed UN-Choke message from ServerThread.\n");
            }
        }
    }

    private void processRequest(ThreadMessage requestMessage) throws IOException
    {
        //extract the piece request index
        int requestedPieceIndex = requestMessage.getPieceIndex();

        synchronized(this.clientThreadLock)
        {
            synchronized(this.peerProcessLock)
            {
                this.neighborPeer.setNeighborRequestedPiece(requestedPieceIndex);
            }
        }
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " processed Request for Piece # " + requestedPieceIndex + " from ServerThread.\n");
    }

    private void sendInterested() throws IOException
    {
        //set message length (message type 1 byte + 0 payload)
        int messageLength = 1;
        //send the 4-byte int message length
        socketStream.writeInt(messageLength);

        //send the 1-byte message type (2 = interested)
        socketStream.writeByte(2);
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " sent Interested message.\n");
    }

    private void sendNotInterested() throws IOException
    {
        //set message length (message type 1 byte + 0 payload)
        int messageLength = 1;
        //send the 4-byte int message length
        socketStream.writeInt(messageLength);

        //send the 1-byte message type (3 = not interested)
        socketStream.writeByte(3);
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " sent NOT-Interested.\n");
    }

    private void sendChokeOrUnchoke(boolean choked) throws IOException
    {
        //set message length (message type 1 byte + 0 payload)
        int messageLength = 1;
        //send the 4-byte int message length
        socketStream.writeInt(messageLength);

        if(true == choked)
        {
            //send the 1-byte message type (0 = choke)
            socketStream.writeByte(0);
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " sent Choke.\n");
        }
        else
        {
            //send the 1-byte message type (1 = unchoke)
            socketStream.writeByte(1);
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " sent UN-Choke.\n");
        }
    }
}