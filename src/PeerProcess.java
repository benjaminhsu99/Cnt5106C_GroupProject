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
import java.io.*; //IOException, OutputStream, DataOutputStream

public class PeerProcess extends Thread
{
    //MAIN METHOD
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

        //read the ".cfg" files
        //the constructor also starts TCP connections to neighbor peers
        PeerProcess myPeerProcess = new PeerProcess(myPeerId);

        //start the server threads that receive TCP messages from neighbors
        myPeerProcess.startServerThreads();

        //start the client threads that send TCP messages to neighbors
        myPeerProcess.startClientThreads();

        //link the client threads to the server threads, since the client is chosen to be the one to
        //modify any PeerObject variables (in order to avoid race conditions)
        myPeerProcess.linkClientToServerThreads();

        //in a while loop, determine which peer partners to send either data to, or send other messages (e.g. requests) to,
        //until all peers (including self) have finished downloading everything
        while(false == myPeerProcess.allThreadsTerminated())
        {
//
//DO STUFF HERE
//
            //determine the top bytes-downloaded-from peers, and change preferred + optimistic neighbors, and reset the bytes counts
            synchronized(myPeerProcess.getPeerProcessLock())
            {

            }
        }

        //wait for the client and server threads to finish
        myPeerProcess.closeThreads();
    }

    //class parameters
    private PeerObject myPeer;
    private PeerObject[] neighborPeers;
    private ServerThread[] serverThreads;
    private ClientThread[] clientThreads;
    private final Object peerProcessLock = new Object();
    private PeerObject[] preferredNeighbors;
    private PeerObject optimisticNeighbor;
    private LogWriter logger;

    //class constructor - which also sets up the TCP connections
    PeerProcess(int myPeerId)
    {
        //read the PeerInfo.cfg file
        ReadPeerInfo readPeerInfoInstance = new ReadPeerInfo();
        List<PeerObject> peers = readPeerInfoInstance.getPeersInfo();

        //start a LogWriter instance
        this.logger = new LogWriter(myPeerId);

        //check if there are at least 2 peers
        if(2 > peers.size())
        {
            System.out.print("ERROR PeerProcess.java initalizeConnections(): less than 2 peers specified in PeerInfo.cfg .\n");
            System.exit(1);
        }

        //find this process's index position in the ordering of peers in the PeerInfo.cfg file
        int myPeerIndexPosition = readPeerInfoInstance.getPeerIndexPosition(myPeerId);
        if(-1 == myPeerIndexPosition)
        {
            System.out.print("ERROR PeerProcess.java initializeConnections(): the specified peer id " + myPeerId + " is not in the PeerInfo.cfg listing.\n");
            System.exit(1);
        }
        //assign this process's peer to the myPeer parameter
        this.myPeer = peers.get(myPeerIndexPosition);
        //if this process's peer already has the file, then log it as having "completed the download" already (technically it didn't download anything)
        if(true == this.myPeer.getHasFile())
        {
            this.logger.logComplete(this.myPeer.getPeerId());
        }

        //the neighborPeers array will hold all the peers that are not this process's own peers
        //with the ordering of the peers being starting from this process's peer list downwards (and looping back to the top)
        this.neighborPeers = new PeerObject[peers.size() - 1];
    
        //initiate socket connections to peers ABOVE/BEFORE this process's peerId, in the PeerInfo.cfg ordering of peers
        try
        {
            int j = 0;
            for(int i = myPeerIndexPosition - 1; i >= 0; i--)
            {
                //make socket connection
                Socket connectToHostSocket = new Socket(peers.get(i).getHostName(), peers.get(i).getPortNumber());
                //log the "connection made" entry
                this.logger.logSendConnection(peers.get(i).getPeerId());
System.out.print("Peer " + myPeerId + " started a connection to " + peers.get(i).getPeerId() + ".\n");
                //set the peer's socket in its PeerObject object
                peers.get(i).setSocket(connectToHostSocket);
                //add the peer to the neighborPeers array, in the appropriate index position
                this.neighborPeers[(peers.size() - 1) - 1 - j] = peers.get(i);
                j++;
            }
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: PeerProcess.java --- some IO error when initiating TCP connections.\n");
            exception.printStackTrace();
            System.exit(1);
        }
            
        //accept socket connections from peers BELOW/AFTER this process's peerId, in the PeerInfo.cfg ordering of peerSockets
        try
        {
            int j = 0;
            ServerSocket serverSocket = new ServerSocket(peers.get(myPeerIndexPosition).getPortNumber());
            for(int i = myPeerIndexPosition + 1; i < peers.size(); i++)
            {
                //make socket connection
                Socket clientSocket = serverSocket.accept();
                //log the "connection received" entry
                this.logger.logReceiveConnection(peers.get(i).getPeerId());
System.out.print("Peer " + myPeerId + " accepted connection from " + peers.get(i).getPeerId() + ".\n");
                //set the peer's socket in its PeerObject object
                peers.get(i).setSocket(clientSocket);
                //add the peer to the neighborPeers array, in the appropriate index position
                this.neighborPeers[j] = peers.get(i);
                j++;
            }
            serverSocket.close();
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: PeerProcess.java --- some IO error when initiating TCP connections.\n");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    //helper methods
    private void startServerThreads()
    {
        //for each peer socket connection, create thread server process thread to receive messages
        this.serverThreads = new ServerThread[this.neighborPeers.length];
        for(int i = 0; i < this.serverThreads.length; i++)
        {
            serverThreads[i] = new ServerThread(this.neighborPeers[i], this.myPeer, this.logger);
            serverThreads[i].start();
        }
    }

    private void startClientThreads()
    {
        //for each peer socket connection, create thread client process thread to send messages
        this.clientThreads = new ClientThread[this.neighborPeers.length];
        for(int i = 0; i < this.clientThreads.length; i++)
        {
            clientThreads[i] = new ClientThread(this.neighborPeers[i], this.myPeer, this.logger, this.peerProcessLock);
            clientThreads[i].start();
        }
    }

    private void linkClientToServerThreads()
    {
        //set the server's pointers to the clients each other
        for(int i = 0; i < this.serverThreads.length; i++)
        {
            this.serverThreads[i].linkClientThread(this.clientThreads[i]);
        }
    }

    public Object getPeerProcessLock()
    {
        return this.peerProcessLock;
    }

    private boolean allThreadsTerminated()
    {
        boolean allThreadsTerminated = true;

        //check if all server and client threads have completed
        for(int i = 0; i < this.serverThreads.length; i++)
        {
            if(Thread.State.TERMINATED != this.serverThreads[i].getState() || Thread.State.TERMINATED != this.clientThreads[i].getState())
            {
                allThreadsTerminated = false;
                break;
            }
        }

        return allThreadsTerminated;
    }

    private void closeThreads()
    {
        //wait for client and server threads to finish
        for(int i = 0; i < this.neighborPeers.length; i++)
        {
            try
            {
                this.clientThreads[i].join();
                this.serverThreads[i].join();
            }
            catch(InterruptedException exception)
            {
                System.out.print("ERROR PeerProcess.java closeThreads(): some problem closing a server thread.");
                exception.printStackTrace();
            }
        }
    }

}
