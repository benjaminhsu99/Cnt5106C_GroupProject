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
import java.util.*; //Queue
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
System.out.print("ClientThread for " + this.neighborPeer.getPeerId() + " started.\n");
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
                }
                //otherwise, determine a piece to request for
                else
                {

                }
            }

Thread.sleep(5000);
            //close the socket (which should cause the ServerThread to close via SocketException or EOFException)
            this.neighborPeer.getSocket().close();
System.out.print("ClientThread told ServerThread " + this.neighborPeer.getPeerId() + " to kill itself.\n");
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
System.out.print("ClientThread for " + this.neighborPeer.getPeerId() + " ended.\n");
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
System.out.print("ClientThread for " + this.neighborPeer.getPeerId() + " received bitfield from ServerThread and processed it.\n");
    }

    private void processInterestStatusMessage(ThreadMessage interestStatusMessage)
    {
        //change the PeerObject value for the neighbor peer
        this.neighborPeer.setNeighborInterested(interestStatusMessage.getInterestStatus());

        //log the event
        if(true == interestStatusMessage.getInterestStatus())
        {
            this.logger.logInterested(this.neighborPeer.getPeerId());
System.out.print("ClientThread for " + this.neighborPeer.getPeerId() + " received Interested message from ServerThread and processed it.\n");
        }
        else
        {
            this.logger.logNotInterested(this.neighborPeer.getPeerId());
System.out.print("ClientThread for " + this.neighborPeer.getPeerId() + " received NOT-Interested message from ServerThread and processed it.\n");
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
System.out.print("ClientThread sent complete handshake to " + this.neighborPeer.getPeerId() + ".\n");
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
System.out.print("ClientThread sent complete bitfield message to peer " + this.neighborPeer.getPeerId() + ".\n");
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

    private void sendInterested() throws IOException
    {
        //set message length (message type 1 byte + 0 payload)
        int messageLength = 1;
        //send the 4-byte int message length
        socketStream.writeInt(messageLength);

        //send the 1-byte message type (2 = interested)
        socketStream.writeByte(2);
System.out.print("ClientThread sent interested message to " + this.neighborPeer.getPeerId() + ".\n");
    }

    private void sendNotInterested() throws IOException
    {
        //set message length (message type 1 byte + 0 payload)
        int messageLength = 1;
        //send the 4-byte int message length
        socketStream.writeInt(messageLength);

        //send the 1-byte message type (3 = not interested)
        socketStream.writeByte(3);
System.out.print("ClientThread sent NOT-interested message to " + this.neighborPeer.getPeerId() + ".\n");
    }
}