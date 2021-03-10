/*
CNT 5105C "Computer Networks" - Spring 2021
Group Project - P2P File Sharing
Benjamin Hsu, Lavanya Khular, Chitranshu Raj
*/

//Created package to group together all the various .java files together
package src.modules;

//Imports
import java.io.*; //IOException, EOFException, InputStream, DataInputStream
import java.net.*; //SocketException
import java.nio.charset.*; //StandardCharsets
import java.util.concurrent.*; //ArrayBlockingQueue
import java.time.*; //LocalDateTime

public class ServerThread extends Thread
{
    //parameters
    private PeerObject neighborPeer;
    private PeerObject myPeer;
    private LogWriter logger;
    private BlockingQueue<ThreadMessage> messagesToPeerProcess;
    private final Object threadsLinkedTogetherLock;
    private DataInputStream socketStream;

    private ClientThread clientThread;

    //constructor
    public ServerThread(PeerObject neighborPeer, PeerObject myPeer, LogWriter logger, BlockingQueue<ThreadMessage> messagesToPeerProcess, Object threadsLinkedTogetherLock)
    {
        this.neighborPeer = neighborPeer;
        this.myPeer = myPeer;
        this.logger = logger;
        this.messagesToPeerProcess = messagesToPeerProcess;
        this.threadsLinkedTogetherLock = threadsLinkedTogetherLock;
    }

    //the "main" method (override the run() method) that is executed for the Thread
    public void run()
    {
        try
        {
            //create a DataInputStream (using a InputStream in the constructor) that can receive data from the TCP socket
            this.socketStream = new DataInputStream(this.neighborPeer.getSocket().getInputStream());
System.out.print("ServerThread " + this.neighborPeer.getPeerId() + " STARTED.\n");

            //receieve the initial handshake
            receiveHandshake();

            //wait until the ClientThread pointer has been set by PeerProcess.java
            synchronized(this.threadsLinkedTogetherLock)
            {
                //do nothing
            }
//System.out.print(LocalDateTime.now() + " ServerThread " + this.neighborPeer.getPeerId() + " was linked to ClientThread " + this.neighborPeer.getPeerId() + " for message passing purposes.\n");

            //continously accept messages at this listening socket until the client closes the TCP socket (which causes a SocketException)
            while(true)
            {
                //read the 4-byte int that is the message's message length
                int messageLength = this.socketStream.readInt();
                //the messageLength must always be at least 1 byte (for messageType)
                if(1 > messageLength)
                {
                    System.out.print("ERROR: ServerThread.java read message header loops --- from peer " + this.neighborPeer.getPeerId() + " message length is " + messageLength + " which is less than 1.\n");
                    System.exit(1);
                }

                //read the 1-byte int that is the message type
                int messageType = this.socketStream.readByte();

                //0 = Choke
                if(0 == messageType)
                {
                    receiveChokeOrUnchoke(true);
                }
                //1 = Unchoke
                else if(1 == messageType)
                {
                    receiveChokeOrUnchoke(false);
                }
                //2 = Interested
                else if(2 == messageType)
                {
                    receiveInterested();
                }
                //3 = Not Interested
                else if(3 == messageType)
                {
                    receiveNotInterested();
                }
                //4 = Have
                else if(4 == messageType)
                {
                    receiveHave();
                }
                //5 = Handshake
                else if(5 == messageType)
                {
                    receiveBitfield(messageLength - 1);
                }
                //6 = request
                else if(6 == messageType)
                {
                    receiveRequest();
                }
                //7 = piece
                else if(7 == messageType)
                {
                    receivePiece(messageLength - 1 - 4);
                }
                //otherwise, this was not a valid message type code
                else
                {
                    System.out.print("ERROR: ServerThread.java read message header loops --- from peer " + this.neighborPeer.getPeerId() + " message type is " + messageType + " which is not valid.\n");
                    System.exit(1);
                }


            }
        }
        catch(SocketException exception)
        {
            //this means the socket was closed by a ClientThread process
            //so do nothing (which means the Thread will end naturally)
System.out.print("ServerThread " + this.neighborPeer.getPeerId() + " KILLED BY SOCKETEXCEPTION.\n");
        }
        catch(EOFException exception)
        {
            //this means the socket was closed by a ClientThread process
            //so do nothing (which means the Thread will end naturally)
System.out.print("ServerThread " + this.neighborPeer.getPeerId() + " KILLED BY EOFEXCEPTION.\n");
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: ServerThread.java --- some IO error in the loop that reads the TCP connection.\n");
            exception.printStackTrace();
            System.exit(1);
        }
System.out.print("ServerThread " + this.neighborPeer.getPeerId() + " ENDED.\n");
    }

    //helper methods
    public void linkClientThread(ClientThread clientThread)
    {
        this.clientThread = clientThread;
    }

    private void receiveHandshake() throws SocketException, IOException
    {
        //byte array to hold the 18-byte string as bytes
        byte[] headerAsBytes = new byte[18];
        //read the 18-byte header
        this.socketStream.readFully(headerAsBytes);
        //convert the bytes to String
        String headerAsString = new String(headerAsBytes, StandardCharsets.UTF_8);
        //check if the header is "P2PFILESHARINGPROJ" as specified in the project specifications
        if(false == headerAsString.equals("P2PFILESHARINGPROJ"))
        {
            System.out.print("ERROR: ServerThread.java receieveHandshake() --- from peer " + this.neighborPeer.getPeerId() + " header string is not P2PFILESHARINGPROJ.\n");
            System.exit(1);
        }

        //byte array to hold the 10-byte zero bytes portion of the handshake
        byte[] zeroBytes = new byte[10];
        //read the 10-bytes of zero bytes
        this.socketStream.readFully(zeroBytes);
        //check if all 10 bytes are the 0 bits byte (null char)
        for(int i = 0; i < zeroBytes.length; i++)
        {
            if(0 != (int)zeroBytes[i])
            {
                System.out.print("ERROR: ServerThread.java receieveHandshake() --- from peer " + this.neighborPeer.getPeerId() + " header string is not P2PFILESHARINGPROJ.\n");
                System.exit(1);
            }
        }

        //read the 4-byte int that is the handshake's peerId
        int handshakePeerId = this.socketStream.readInt();
        //check if the handshake's peerId is as expected
        if(this.neighborPeer.getPeerId() != handshakePeerId)
        {
            System.out.print("ERROR: ServerThread.java receieveHandshake() --- from peer " + this.neighborPeer.getPeerId() + " hand shake peerID is " + handshakePeerId + " which does not match.\n");
            System.exit(1);
        }
System.out.print("ServerThread " + handshakePeerId + " got Handshake from peer " + handshakePeerId + ".\n");
    }

    private void receiveBitfield(int payloadLength) throws SocketException, IOException
    {
        //byte array to hold the bitfield payload as bytes
        byte[] bitfieldAsBytes = new byte[payloadLength];
        //read in the bitfield payload
        this.socketStream.readFully(bitfieldAsBytes);

        //pass the bitfield to the ClientProcess for it to enter into the PeerObject
        //so as to avoid two concurrent threads modifying the bitfield portion fo the PeerObject
        ThreadMessage messageToClient = new ThreadMessage(bitfieldAsBytes);
        this.clientThread.addThreadMessage(messageToClient);
System.out.print("ServerThread " + this.neighborPeer.getPeerId() + " got Bitfield from peer " + this.neighborPeer.getPeerId() + ".\n");
    }

    private void receiveInterested() throws SocketException, IOException
    {
        //pass a message to the ClientProcess for it to change PeerObject's interested status
        //so as to avoid two concurrent threads modifying the interested portion fo the PeerObject
        ThreadMessage messageToClient = new ThreadMessage(true);
        this.clientThread.addThreadMessage(messageToClient);
System.out.print("ServerThread " + this.neighborPeer.getPeerId() + " got Interested message from peer " + this.neighborPeer.getPeerId() + ".\n");
    }

    private void receiveNotInterested() throws SocketException, IOException
    {
        //pass a message to the ClientProcess for it to change PeerObject's interested status
        //so as to avoid two concurrent threads modifying the interested portion fo the PeerObject
        ThreadMessage messageToClient = new ThreadMessage(false);
        this.clientThread.addThreadMessage(messageToClient);
System.out.print("ServerThread " + this.neighborPeer.getPeerId() + " got NOT-Interested message from peer " + this.neighborPeer.getPeerId() + ".\n");
    }

    private void receiveChokeOrUnchoke(boolean choked) throws SocketException, IOException
    {
        //pass a message to the ClientProcess for it to change PeerObject's neighborChoked status
        //so as to avoid two concurrent threads modifying the interested portion fo the PeerObject
        if(true == choked)
        {
            ThreadMessage messageToClient = new ThreadMessage(ThreadMessage.ThreadMessageType.RECEIVEDCHOKE);
            this.clientThread.addThreadMessage(messageToClient);
System.out.print("ServerThread " + this.neighborPeer.getPeerId() + " got Choked message from peer " + this.neighborPeer.getPeerId() + ".\n");
        }
        else
        {
            ThreadMessage messageToClient = new ThreadMessage(ThreadMessage.ThreadMessageType.RECEIVEDUNCHOKE);
            this.clientThread.addThreadMessage(messageToClient);
System.out.print("ServerThread " + this.neighborPeer.getPeerId() + " got UN-Choked message from peer " + this.neighborPeer.getPeerId() + ".\n");
        }
    }

    private void receiveRequest() throws SocketException, IOException
    {
        //read the requested piece index (4 byte int)
        int pieceIndex = this.socketStream.readInt();

        //pass a message to the ClientProcess for it to change PeerObject's interested status
        //so as to avoid two concurrent threads modifying the interested portion fo the PeerObject
        ThreadMessage messageToClient = new ThreadMessage(ThreadMessage.ThreadMessageType.REQUEST, pieceIndex);
        this.clientThread.addThreadMessage(messageToClient);
System.out.print("ServerThread " + this.neighborPeer.getPeerId() + " got Request for Piece # " + pieceIndex + " from peer " + this.neighborPeer.getPeerId() + ".\n");
    }

    private void receivePiece(int payloadLength) throws SocketException, IOException
    {
        //read in the piece index
        int pieceIndex = this.socketStream.readInt();

        //byte array to hold the bitfield payload as bytes
        byte[] pieceBytes = new byte[payloadLength];
        //read in the bitfield payload
        this.socketStream.readFully(pieceBytes);

        //pass the piece to the ClientProcess for it to enter into the PeerObject
        //so as to avoid two concurrent threads modifying the bitfield portion of the PeerObject
        //This lock is used to ensure that the piece is processed fully before any "have" messages are sent.
        BlockingQueue<Integer> pieceProcessedLock = new ArrayBlockingQueue<Integer>(1);
        try
        {
            pieceProcessedLock.put(0);
        }
        catch(InterruptedException exception)
        {
            System.out.print("\n\n\n\n\nPOSSIBLE ERROR (OR MAY JUST BE HARMLESS EXCEPTION): ServerThread " + this.neighborPeer.getPeerId() + " --- InterupptedException (BlockingQueue) when trying to make lock for piece received.\n");
            exception.printStackTrace();
            System.out.print("\n\n\n\n\n");
        }
        ThreadMessage messageToClient = new ThreadMessage(pieceIndex, pieceBytes, pieceProcessedLock);
        this.clientThread.addThreadMessage(messageToClient);
System.out.print("ServerThread " + this.neighborPeer.getPeerId() + " downloaded Piece # " + pieceIndex + " from peer " + this.neighborPeer.getPeerId() + ".\n");

        //tell the main PeerProcess to tell all the ClientThreads to notify their peer partners with a "have" message
        ThreadMessage messageToPeerProcess = new ThreadMessage(ThreadMessage.ThreadMessageType.PEERPROCESSHAVE, pieceIndex, pieceProcessedLock);
// System.out.print(LocalDateTime.now() + " ServerThread " + this.neighborPeer.getPeerId() + " is trying... to tell the PeerProcess to notify all the ClientThreads to send a Have Piece # " + pieceIndex + " message.\n");
// if(0 == this.messagesToPeerProcess.remainingCapacity())
// {
// System.out.print(LocalDateTime.now() + " PeerProcess (accessing by ServerThread  " + this.neighborPeer.getPeerId() + ") --- QUEUE IS FULL!!!!!.\n");
// }
        try
        {
            this.messagesToPeerProcess.put(messageToPeerProcess);
        }
        catch(InterruptedException exception)
        {
            System.out.print("\n\n\n\n\nPOSSIBLE ERROR (OR MAY JUST BE HARMLESS EXCEPTION): ServerThread " + this.neighborPeer.getPeerId() + " --- InterupptedException (BlockingQueue).\n");
            exception.printStackTrace();
            System.out.print("\n\n\n\n\n");
        }
//System.out.print(LocalDateTime.now() + " ServerThread " + this.neighborPeer.getPeerId() + " told the PeerProcess to notify all the ClientThreads to send a Have Piece # " + pieceIndex + " message.\n");
    }

    private void receiveHave() throws SocketException, IOException
    {
        //read the requested piece index (4 byte int)
        int pieceIndex = this.socketStream.readInt();

        //pass a message to the ClientProcess for it to change PeerObject's interested status
        //so as to avoid two concurrent threads modifying the interested portion fo the PeerObject
        ThreadMessage messageToClient = new ThreadMessage(ThreadMessage.ThreadMessageType.HAVE, pieceIndex);
        this.clientThread.addThreadMessage(messageToClient);
System.out.print("ServerThread " + this.neighborPeer.getPeerId() + " got Have Piece # " + pieceIndex + " message from peer " + this.neighborPeer.getPeerId() + ".\n");
    }
}