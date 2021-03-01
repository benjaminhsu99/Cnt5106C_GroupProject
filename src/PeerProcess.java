/*
CNT 5105C "Computer Networks" - Spring 2021
Group Project - P2P File Sharing
Benjamin Hsu, Lavanya Khular, Chitranshu Raj
*/

package src;

//Imports
import src.modules.*;
import java.util.*; //List, ArrayList, Iterator
import java.nio.file.*; //Files, Path
import java.net.*; //Socket
import java.io.*; //IOException, OutputStream

public class PeerProcess extends Thread
{
    //parameters
    static List<PeerObject> peers;

    //methods
    public static void main(String[] args)
    {
        //check that there is at least one command-line argument, and that it is a number (the peer id of the process)
        //if so, assign that peerId to "myPeerId"
        int myPeerId = -1;
        if(1 > args.length)
        {
            System.out.print("ERROR: at least one integer argument (process peer ID) must be specified.\n");
            System.exit(1);
        }
        try
        {
                myPeerId = Integer.parseInt(args[0]);
        }
        catch(NumberFormatException exception)
        {
            System.out.print("ERROR: the command line argument " + args[0] + " could not be parsed into an int (process peer id).\n");
            exception.printStackTrace();
            System.exit(1);
        }

        //read the PeerInfo.cfg file (the Common.cfg can be read statically so no object required for that one)
        ReadPeerInfo readPeerInfoInstance = new ReadPeerInfo();
        peers = readPeerInfoInstance.getPeersInfo();

        //check if there are at least 2 peers
        if(2 > peers.size())
        {
            System.out.print("ERROR: less than 2 peers specified in PeerInfo.cfg .\n");
            System.exit(1);
        }

        //find this process's index position in the ordering of peers in the PeerInfo.cfg file
        int myPeerIndexPosition = readPeerInfoInstance.getPeerIndexPosition(myPeerId);
        if(-1 == myPeerIndexPosition)
        {
            System.out.print("ERROR: the specified peer id " + myPeerId + " is not in the PeerInfo.cfg listing.\n");
            System.exit(1);
        }

        //array that will hold the Socket connection to peers
        Socket[] peerSockets = new Socket[peers.size() - 1];

        //initiate socket connections to peers ABOVE/BEFORE this process's peerId, in the PeerInfo.cfg ordering of peers
        try
        {
            for(int i = myPeerIndexPosition - 1; i >= 0; i--)
            {
                Socket connectToHostSocket = new Socket(peers.get(i).getHostName(), peers.get(i).getPortNumber());
System.out.print("Peer " + myPeerId + " started a connection to " + peers.get(i).getPeerId() + ".\n");
                peerSockets[i] = connectToHostSocket;
                peers.get(i).setSocket(connectToHostSocket);
            }
        }
        catch(IOException exception)
        {
            exception.printStackTrace();
        }
        
        //accept socket connections from peers BELOW/AFTER this process's peerId, in the PeerInfo.cfg ordering of peerSockets
        try
        {
            ServerSocket serverSocket = new ServerSocket(peers.get(myPeerIndexPosition).getPortNumber());
            for(int i = myPeerIndexPosition + 1; i < peers.size(); i++)
            {
                Socket clientSocket = serverSocket.accept();
System.out.print("Peer " + myPeerId + " accepted connection from " + peers.get(i).getPeerId() + ".\n");
                peerSockets[i-1] = clientSocket;
                peers.get(i).setSocket(clientSocket);
            }
            serverSocket.close();
        }
        catch(IOException exception)
        {
            exception.printStackTrace();
        }

        
        ServerThread[] serverThreads = new ServerThread[peerSockets.length];
        //for each peer socket connection, create thread server process to receive messages
        for(int i = 0; i < peerSockets.length; i++)
        {
            //TODO HERE
            serverThreads[i] = new ServerThread(peers.get(0), i);
            serverThreads[i].start();
        }


        //in a while loop, determine which peer partners to send data to, until all PeerObjects report they have
        //finished downloading everything
            //TODO HERE


        //wait for the server threads to finish
        for(int i = 0; i < peerSockets.length; i++)
        {
            try
            {
                serverThreads[i].join();
            }
            catch(InterruptedException exception)
            {
                System.out.print("ERROR: some thread interupption exception.");
                exception.printStackTrace();
            }
        }
System.out.print("Sum " + peers.get(0).getBytesDownloadedFrom() + "\n");
    }
}
