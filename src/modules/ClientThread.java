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
import java.util.*; //Queue, LinkedList

public class ClientThread extends Thread
{
    //parameters
    private PeerObject neighborPeer;
    private PeerObject myPeer;
    private LogWriter logger;
    private final Object peerProcessLock;
    private DataOutputStream socketStream;

    private Queue<ThreadMessage> messagesFromServer = new LinkedList<ThreadMessage>();

    //constructor
    public ClientThread(PeerObject neighborPeer, PeerObject myPeer, LogWriter logger, Object peerProcessLock)
    {
        this.neighborPeer = neighborPeer;
        this.myPeer = myPeer;
        this.logger = logger;
        this.peerProcessLock = peerProcessLock;
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
                    if(ThreadMessage.ThreadMessageType.BITFIELD == topMessage.getThreadMessageType())
                    {
                        processBitfieldMessage(topMessage);
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

    private void processBitfieldMessage(ThreadMessage bitfieldMessage)
    {
        this.neighborPeer.setBitfieldFromBytes(bitfieldMessage.getBitfield(), this.logger);
System.out.print("ClientThread for " + this.neighborPeer.getPeerId() + " received bitfield from ServerThread and processed it.\n");
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

    private void sendBitfield()  throws IOException
    {
        //get the bitfield as bytes
        byte[] bitfield = myPeer.getBitfieldAsBytes();

        //calculate the message length (message type 1 byte + the bitfield length in bytes)
        int messageLength = 1 + bitfield.length;
        //send the 4-byte int message length
        socketStream.writeInt(messageLength);
System.out.print("Sent bitfield message length " + this.neighborPeer.getPeerId() + ".\n");

        //send the 1-byte message type (5 = bitfield)
        socketStream.writeByte(5);
System.out.print("Sent bitfield message type " + this.neighborPeer.getPeerId() + ".\n");

        //send the bitfield
        socketStream.write(bitfield, 0, bitfield.length);
System.out.print("Sent bitfield bitfield as bytes to " + this.neighborPeer.getPeerId() + ".\n");
    }
}