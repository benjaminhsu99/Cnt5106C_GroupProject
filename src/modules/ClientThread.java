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
    private BlockingQueue<ThreadMessage> messagesToPeerProcess;
    private final Object peerProcessLock;
    private final Object clientThreadLock;
    private DataOutputStream socketStream;

    ///ArrayBlockingQueue for thread-safe message receiving from the ServerThread and the PeerProcess
    //its constructor requires specifying outright the capacity of the queuu
    //since the queue blocks sending & receiving if full, there is technically a change of deadlock since PeerProcess also has this same
    //type of queue that it uses to get messages from the ServerThreads, so the size of this queue was made very large just in case
    //assume that 9,999 capacity is good enough??? (BANDAID FIX)
    private volatile BlockingQueue<ThreadMessage> messagesFromServer = new ArrayBlockingQueue<ThreadMessage>(9999);

    //constructor
    public ClientThread(PeerObject neighborPeer, PeerObject myPeer, LogWriter logger, BlockingQueue<ThreadMessage> messagesToPeerProcess, Object peerProcessLock, Object clientThreadLock)
    {
        this.neighborPeer = neighborPeer;
        this.myPeer = myPeer;
        this.logger = logger;
        this.messagesToPeerProcess = messagesToPeerProcess;
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
            while(true)
            {
if(0 == this.messagesFromServer.remainingCapacity())
{
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " QUEUE IS FULL!!!!!.\n");
}
                //process any incoming task messages from the sibling ServerThread
                ThreadMessage topMessage = this.messagesFromServer.take();

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
                else if(ThreadMessage.ThreadMessageType.PIECE == topMessage.getThreadMessageType())
                {
                    processPiece(topMessage);
                }
                else if(ThreadMessage.ThreadMessageType.SENDHAVE == topMessage.getThreadMessageType())
                {
                    sendHave(topMessage);
                }
                else if(ThreadMessage.ThreadMessageType.HAVE == topMessage.getThreadMessageType())
                {
                    processHave(topMessage);
                }
                else
                {
                    System.out.print("ERROR: CLIENTTHREAD " + this.neighborPeer.getPeerId() + " GOT AN UNKNOWN THREADMESSAGE TYPE.\n");
                }

                //check if a piece can be requested after processing the task above
                determineRequest();

                //check if the conditions to terminate the while loop have been met
                if(
                true == this.messagesFromServer.isEmpty() 
                && true == this.myPeer.getHasFile() 
                && true == this.neighborPeer.getHasFile() 
                && true == this.neighborPeer.getMyChoked() 
                && true == this.neighborPeer.getNeighborChoked() 
                && true == this.neighborPeer.getAllPiecesNotified()
                && true == this.neighborPeer.getNeighborWasToldChoked())
                {
                    break;
                }
            }

            //close the socket (which should cause the ServerThread to close via SocketException or EOFException)
            this.neighborPeer.getSocket().close();
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " told ServerThread to kill itself.\n");
        }
        catch(InterruptedException exception)
        {
            System.out.print("\n\n\n\n\nPOSSIBLE ERROR (OR MAY JUST BE HARMLESS EXCEPTION): ClientThread " + this.neighborPeer.getPeerId() + " --- InterupptedException (BlockingQueue).\n");
            exception.printStackTrace();
            System.out.print("\n\n\n\n\n");
        }
        catch(IOException exception)
        {
            System.out.print("\n\n\n\n\nPOSSIBLE ERROR (OR MAY JUST BE HARMLESS EXCEPTION): ClientThread " + this.neighborPeer.getPeerId() + " --- some IO error in the loop that sends to the TCP connection.\n");
            exception.printStackTrace();
            System.out.print("\n\n\n\n\n");
        }
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " ENDED.\n");
    }

    //helper methods
    public void addThreadMessage(ThreadMessage messageFromServer)
    {
        try
        {
            this.messagesFromServer.put(messageFromServer);
        }
        catch(InterruptedException exception)
        {
            System.out.print("\n\n\n\n\nPOSSIBLE ERROR (OR MAY JUST BE HARMLESS EXCEPTION): ClientThread " + this.neighborPeer.getPeerId() + " --- InterupptedException (BlockingQueue while doing 'addThreadMessage').\n");
            exception.printStackTrace();
            System.out.print("\n\n\n\n\n");
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

            //also set the "have" bitfield of the piecesNotified to the peer equal to this initial bitfield sent
            for(int i = 0; i < ReadCommon.getNumberOfPieces(); i++)
            {
                if(true == this.myPeer.hasPiece(i))
                {
                    this.neighborPeer.setPiecesNotified(i);
                }
            }
        }
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " sent Bitfield.\n");
    }

    private void processBitfieldMessage(ThreadMessage bitfieldMessage) throws IOException
    {
        this.neighborPeer.setBitfieldFromBytes(bitfieldMessage.getBytesArray(), this.logger);

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
// System.out.print("(DETERMINEINTEREST) BITFIELD OF " + this.neighborPeer.getPeerId() + " : ");
// for(int i = 0; i < ReadCommon.getNumberOfPieces(); i++)
// {
// System.out.print("#" + i + " = " + this.neighborPeer.hasPiece(i) + ", ");
// }
// System.out.print("\n");
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

            //update status of neighborWasToldChoked to true
            this.neighborPeer.setNeighborWasToldChoked(true);
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " sent Choke.\n");
        }
        else
        {
            //send the 1-byte message type (1 = unchoke)
            socketStream.writeByte(1);

            //update status of neighborWasToldChoked to false
            this.neighborPeer.setNeighborWasToldChoked(false);
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " sent UN-Choke.\n");
        }
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

        //change the request status
        synchronized(this.clientThreadLock)
        {
            synchronized(this.peerProcessLock)
            {
                this.neighborPeer.setNeighborRequestedPiece(requestedPieceIndex);
            }
        }
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " processed Request for Piece # " + requestedPieceIndex + " from ServerThread.\n");

        //check if the peer is unchoked, and send the piece if so
        //there is a rare chance for the PeerProcess to have changed unchoking peers by this point
        synchronized(this.clientThreadLock)
        {
            synchronized(this.peerProcessLock)
            {
                if(false == this.neighborPeer.getMyChoked() && -1 != this.neighborPeer.getNeighborRequestedPiece())
                {
                    sendPiece(this.neighborPeer.getNeighborRequestedPiece());
                }
else
{
System.out.print("\n\n\nClientThread " + this.neighborPeer.getPeerId() + " didn't qualify for sendPiece(), probably due to unchoke change --- IT ACTUALLY HAPPENED!!!\n\n\n");
}
                //clear the "neighborRequestedPiece" field
                this.neighborPeer.setNeighborRequestedPiece(-1);
            }
        }
    }

    private void sendPiece(int pieceIndex) throws IOException
    {
        //check if the peer even has the piece in the first place (shouldn't be possible to get a request for a piece it doesn't have - but perform this check anyways)
        if(false == this.myPeer.hasPiece(pieceIndex))
        {
            System.out.print("ERROR: ClientThread " + this.neighborPeer.getPeerId() + " was requested to send piece " + pieceIndex + " but doesn't have it!?\n");
            return;
        }

        //set message length (message type 1 byte + 4-byte int piece index + bytes of the piece size payload)
        int messageLength;
        //case in which the piece is the last piece (which may have a non-normal piece size)
        if(ReadCommon.getNumberOfPieces() - 1 == pieceIndex)
        {
            int sizeOfLastPiece = ReadCommon.getFileSize() - (ReadCommon.getNumberOfPieces() - 1) * ReadCommon.getPieceSize();
            messageLength = 1 + 4 + sizeOfLastPiece;
        }
        //otherwise just use a normal piece size
        else
        {
            messageLength = 1 + 4 + ReadCommon.getPieceSize();
        }
        //send the 4-byte int message length
        socketStream.writeInt(messageLength);

        //send the 1-byte message type (7 = piece)
        socketStream.writeByte(7);

        //send the 4-byte int piece index
        socketStream.writeInt(pieceIndex);

        //send the piece itself
        byte[] pieceContents = this.myPeer.getFileWriter().readPiece(pieceIndex);
        socketStream.write(pieceContents, 0, pieceContents.length);
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " sent Piece # " + pieceIndex + ".\n");
    }

    private void processPiece(ThreadMessage pieceMessage) throws InterruptedException, IOException
    {
        synchronized(this.clientThreadLock)
        {
            synchronized(this.peerProcessLock)
            {
                int pieceIndex = pieceMessage.getPieceIndex();
                byte[] pieceBytes = pieceMessage.getBytesArray();

                //if the peer doesn't already have the piece, write it in and set the bitfield as having the piece
                //write the piece
                if(false == this.myPeer.hasPiece(pieceIndex))
                {
                    //write the piece to the file
                    this.myPeer.getFileWriter().writePiece(pieceIndex, pieceBytes);
                    //log the event (do this BEFORE actually setting the piece, since the setBitFieldPieceAsTrue may log a "file download complete" entry
                    //--- so do this first to make sure the log entries are in order)
                    this.logger.logDownload(this.neighborPeer.getPeerId(), pieceIndex, this.myPeer.countNumberOfPieces() + 1);
                    //set the bitfield segment as true
                    this.myPeer.setBitfieldPieceAsTrue(pieceIndex, this.logger);
                    //add the number of bytes to the bytesDownloadedFrom count
                    this.neighborPeer.addBytesDownloadedFrom(pieceBytes.length);

                    //clear any record of currently requested pieces
                    this.myPeer.clearRequested(this.neighborPeer.getPeerId());

                    //tell the main PeerProcess to tell all the ClientThreads to notify their peer partners with a "have" message
                    ThreadMessage messageToPeerProcess = new ThreadMessage(ThreadMessage.ThreadMessageType.PEERPROCESSHAVE, pieceIndex);
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " is trying... to tell the PeerProcess to notify all the ClientThreads to send a Have Piece # " + pieceIndex + " message.\n");
if(0 == this.messagesToPeerProcess.remainingCapacity())
{
System.out.print("PeerProcess/ClientThread " + this.neighborPeer.getPeerId() + " QUEUE IS FULL!!!!!.\n");
}
                    this.messagesToPeerProcess.put(messageToPeerProcess);
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " told the PeerProcess to notify all the ClientThreads to send a Have Piece # " + pieceIndex + " message.\n");
                    
                    //check if this peer is still interested in the neighbor now that it has a new piece
                    determineInterest();
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " processed Piece # " + pieceIndex + " from ServerThread.\n");                    
                }
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
                        //otherwise, check if the piece has not been requested and is required, AND if the neighbor has the piece
                        if(-1 == this.myPeer.getRequested(i) && false == this.myPeer.hasPiece(i) && true == this.neighborPeer.hasPiece(i))
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

    private void sendHave(ThreadMessage messageFromPeerProcess) throws IOException
    {
        //extract the piece index that the have message should be sent for
        int havePieceIndex = messageFromPeerProcess.getPieceIndex();

        //set message length (message type 1 byte + 4 byte int payload)
        int messageLength = 1 + 4;
        //send the 4-byte int message length
        socketStream.writeInt(messageLength);

        //send the 1-byte message type (4 = have)
        socketStream.writeByte(4);

        //send the 4-byte int piece that is requested
        socketStream.writeInt(havePieceIndex);

        //also set the "have" bitfield of the piecesNotified to the peer for the piece that was just notified
        synchronized(this.clientThreadLock)
        {
            this.neighborPeer.setPiecesNotified(havePieceIndex);

            //and also re-determine interest with the neighbor
            synchronized(this.peerProcessLock)
            {
                determineInterest();
            }
        }
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " sent Have Piece # " + havePieceIndex + " message.\n");
    }

    private void processHave(ThreadMessage pieceMessage) throws IOException
    {
        synchronized(this.clientThreadLock)
        {
            int pieceIndex = pieceMessage.getPieceIndex();

            //log the have event
            this.logger.logHave(this.neighborPeer.getPeerId(), pieceIndex);

            //write the piece into the local "view" of the neighbor's bitfield
            this.neighborPeer.setBitfieldPieceAsTrue(pieceIndex, this.logger);

            //re-determine if this peer is now interested
            synchronized(this.peerProcessLock)
            {
                determineInterest();
            }
System.out.print("ClientThread " + this.neighborPeer.getPeerId() + " processed Have Piece # " + pieceIndex + " from ServerThread.\n");
        }
    }
}