/*
CNT 5105C "Computer Networks" - Spring 2021
Group Project - P2P File Sharing
Benjamin Hsu, Lavanya Khular, Chitranshu Raj
*/

//Created package to group together all the various .java files together
package src.modules;

//Imports
import java.io.*; //IOException, InputStream, DataInputStream
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
            //continously accept messages at this listening socket until the client closes the TCP socket (which causes a SocketException)
            //while(true)
            {
                //receieve the handshake
                receiveHandshake();


            }
System.out.print("ServerThread for " + this.neighborPeer.getPeerId() + " ended.\n");
        }
        catch(SocketException exception)
        {
            //this means the socket was closed by the PeerProcess.java process
            //so do nothing (which means the Thread will end naturally)
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: ServerThread.java --- some IO error in the loop that reads the TCP connection.\n");
            exception.printStackTrace();
            System.exit(1);
        }
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
}