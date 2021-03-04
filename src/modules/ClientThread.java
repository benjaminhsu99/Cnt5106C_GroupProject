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

public class ClientThread extends Thread
{
    //parameters
    private PeerObject neighborPeer;
    private PeerObject myPeer;
    private LogWriter logger;
    private DataOutputStream socketStream;

    //constructor
    public ClientThread(PeerObject neighborPeer, PeerObject myPeer, LogWriter logger)
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
            //create a DataOutputStream (using a OutputStream in the constructor) that can send data to the TCP socket
            this.socketStream = new DataOutputStream(this.neighborPeer.getSocket().getOutputStream());
System.out.print("ClientThread for " + this.neighborPeer.getPeerId() + " started.\n");
            //continously send messages at this listening socket until the client closes the TCP socket (which causes a SocketException)
            //while(true)
            {
                //receieve the handshake
                sendHandshake();


            }
System.out.print("ClientThread for " + this.neighborPeer.getPeerId() + " ended.\n");
        }
        catch(SocketException exception)
        {
            //this means the socket was closed by the PeerProcess.java process
            //so do nothing (which means the Thread will end naturally)
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: ClientThread.java --- some IO error in the loop that sends to the TCP connection.\n");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    //helper methods
    void sendHandshake() throws SocketException, IOException
    {
        String headerAsString = "P2PFILESHARINGPROJ";
        //convert the string to a byte array
        byte[] headerAsBytes = headerAsString.getBytes(StandardCharsets.UTF_8);
        //send the header
        socketStream.write(headerAsBytes, 0, headerAsBytes.length);
System.out.print("Sent header to " + this.neighborPeer.getPeerId() + ".\n");

        //byte array to hold the 10-byte zero bytes portion of the handshake
        byte[] zeroBytes = new byte[10];
        //populate the bytes with 0 bits bytes (null chars)
        for(int i = 0; i < zeroBytes.length; i++)
        {
            zeroBytes[i] = (byte)0;
        }
        //semd the 0-bytes
        socketStream.write(zeroBytes, 0, zeroBytes.length);
System.out.print("Sent zero bytes to " + this.neighborPeer.getPeerId() + ".\n");

        //send the 4-byte int peerId
        socketStream.writeInt(this.myPeer.getPeerId());
System.out.print("Sent peerId to " + this.neighborPeer.getPeerId() + ".\n");
    }
}