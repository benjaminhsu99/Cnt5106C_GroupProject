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

public class ServerThread extends Thread
{
    //parameters
    private PeerObject neighborPeer;
    private PeerObject myPeer;
    private LogWriter logger;
    private DataInputStream socketStream;

    private  ClientThread clientThread;

    //constructor
    public ServerThread(PeerObject neighborPeer, PeerObject myPeer, LogWriter logger)
    {
        this.neighborPeer = neighborPeer;
        this.myPeer = myPeer;
        this.logger = logger;
    }

    //the "main" method (override the run() method) that is executed for the Thread
    public void run()
    {
        try
        {
            //create a DataInputStream (using a InputStream in the constructor) that can receive data from the TCP socket
            this.socketStream = new DataInputStream(this.neighborPeer.getSocket().getInputStream());
System.out.print("ServerThread " + this.neighborPeer.getPeerId() + " STARTED.\n");

            //wait until the sibling ClientThread has been created
            //the ClientThread will be the one that will modify the peer's PeerObject
            //in order to avoid race conditions, so all new data is passed to the ClientThread
            while(null == this.clientThread)
            {
                //block until clientThread is linked
            }

            //receieve the initial handshake
            receiveHandshake();

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
                //5 = Handshake
                else if(5 == messageType)
                {
                    receiveBitfield(messageLength - 1);
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
System.out.print("ServerThread " + handshakePeerId + " got Handshake.\n");
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
System.out.print("ServerThread " + this.neighborPeer.getPeerId() + " got Bitfield and forwarded to ClientThread.\n");
    }

    private void receiveInterested() throws SocketException, IOException
    {
        //pass a message to the ClientProcess for it to change PeerObject's interested status
        //so as to avoid two concurrent threads modifying the interested portion fo the PeerObject
        ThreadMessage messageToClient = new ThreadMessage(true);
        this.clientThread.addThreadMessage(messageToClient);
System.out.print("ServerThread " + this.neighborPeer.getPeerId() + " got Interested message and forwarded to ClientThread.\n");
    }

    private void receiveNotInterested() throws SocketException, IOException
    {
        //pass a message to the ClientProcess for it to change PeerObject's interested status
        //so as to avoid two concurrent threads modifying the interested portion fo the PeerObject
        ThreadMessage messageToClient = new ThreadMessage(false);
        this.clientThread.addThreadMessage(messageToClient);
System.out.print("ServerThread " + this.neighborPeer.getPeerId() + " got NOT-Interested message and forwarded to ClientThread.\n");
    }

    private void receiveChokeOrUnchoke(boolean choked) throws SocketException, IOException
    {
        //pass a message to the ClientProcess for it to change PeerObject's neighborChoked status
        //so as to avoid two concurrent threads modifying the interested portion fo the PeerObject
        if(true == choked)
        {
            ThreadMessage messageToClient = new ThreadMessage(ThreadMessage.ThreadMessageType.RECEIVEDCHOKE);
            this.clientThread.addThreadMessage(messageToClient);
System.out.print("ServerThread " + this.neighborPeer.getPeerId() + " got Choked message and forwarded to ClientThread.\n");
        }
        else
        {
            ThreadMessage messageToClient = new ThreadMessage(ThreadMessage.ThreadMessageType.RECEIVEDUNCHOKE);
            this.clientThread.addThreadMessage(messageToClient);
System.out.print("ServerThread " + this.neighborPeer.getPeerId() + " got UN-Choked message and forwarded to ClientThread.\n");
        }
    }
}