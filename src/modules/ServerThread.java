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
System.out.print("ServerThread for " + this.neighborPeer.getPeerId() + " started.\n");

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

                //5 = Handshake
                if(5 == messageType)
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
System.out.print("ServerThread for " + this.neighborPeer.getPeerId() + " KILLED BY SOCKETEXCEPTION ended.\n");
        }
        catch(EOFException exception)
        {
            //this means the socket was closed by a ClientThread process
            //so do nothing (which means the Thread will end naturally)
System.out.print("ServerThread for " + this.neighborPeer.getPeerId() + " KILLED BY EOFEXCEPTION ended.\n");
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: ServerThread.java --- some IO error in the loop that reads the TCP connection.\n");
            exception.printStackTrace();
            System.exit(1);
        }
System.out.print("ServerThread for " + this.neighborPeer.getPeerId() + " ended.\n");
    }

    //helper methods
    void receiveHandshake() throws SocketException, IOException
    {
        //byte array to hold the 18-byte string as bytes
        byte[] headerAsBytes = new byte[18];
        //read the 18-byte header
        this.socketStream.readFully(headerAsBytes);
        //convert the bytes to String
        String headerAsString = new String(headerAsBytes, StandardCharsets.UTF_8);
System.out.print("From peer " + this.neighborPeer.getPeerId() + " got handshake header " + headerAsString + "\n");
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
System.out.print("From peer " + this.neighborPeer.getPeerId() + " got handshake zero bytes.\n");

        //read the 4-byte int that is the handshake's peerId
        int handshakePeerId = this.socketStream.readInt();
        //check if the handshake's peerId is as expected
        if(this.neighborPeer.getPeerId() != handshakePeerId)
        {
            System.out.print("ERROR: ServerThread.java receieveHandshake() --- from peer " + this.neighborPeer.getPeerId() + " hand shake peerID is " + handshakePeerId + " which does not match.\n");
            System.exit(1);
        }
System.out.print("Got completed handshake from " + handshakePeerId + ".\n");
    }

    void receiveBitfield(int payloadLength) throws SocketException, IOException
    {
        //byte array to hold the bitfield string as bytes
        byte[] bitfieldAsBytes = new byte[payloadLength];
        //read in the bitfield payload
        this.socketStream.readFully(bitfieldAsBytes);
System.out.print("From peer " + this.neighborPeer.getPeerId() + " got bitfield.\n");

        //enter the bitfield into the peer's PeerObject bitfield parameter
        this.neighborPeer.setBitfieldFromBytes(bitfieldAsBytes);
        //check if the peer has the full file
        boolean neighborHasFile = this.neighborPeer.checkHasFile();
        //if the neighbor has the file, log that neighbor as having "completed the download"
        if(true == neighborHasFile)
        {
            this.logger.logComplete(this.neighborPeer.getPeerId());
        }
System.out.print("From peer " + this.neighborPeer.getPeerId() + " set the PeerObject's bitfield.\n");
    }
}