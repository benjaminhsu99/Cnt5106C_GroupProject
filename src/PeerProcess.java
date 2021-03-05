/*
CNT 5105C "Computer Networks" - Spring 2021
Group Project - P2P File Sharing
Benjamin Hsu, Lavanya Khular, Chitranshu Raj
*/

package src;

//Imports
import src.modules.*;
import java.util.*; //List, ArrayList, Iterator, Random
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
            myPeerProcess.unchokingRoutine();
        }

        //wait for the client and server threads to finish
        myPeerProcess.closeThreads();
    }

    //class parameters
    private PeerObject myPeer;
    private PeerObject[] neighborPeers;
    private List<PeerObject> chokedNeighbors;
    private ServerThread[] serverThreads;
    private ClientThread[] clientThreads;
    private final Object peerProcessLock = new Object();
    private final Object clientThreadLock = new Object();
    private LogWriter logger;
    private long unchokingTimer;
    private long optimisticUnchokingTimer;
    private PeerObject optimisticPeer;

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
            clientThreads[i] = new ClientThread(this.neighborPeers[i], this.myPeer, this.logger, this.peerProcessLock, this.clientThreadLock);
            clientThreads[i].start();
        }
    }

    private void linkClientToServerThreads()
    {
        //set the server's pointers to the clients each other
        for(int i = 0; i < this.serverThreads.length; i++)
        {
            this.serverThreads[i].linkClientThread(this.clientThreads[i]);
            //oh and also link it into the PeerObject for later use in this PeerObject.java unchokingRoutine()
            this.neighborPeers[i].setClientThread(this.clientThreads[i]);
        }
    }

    private void unchokingRoutine()
    {
        //if the initial unchoked neighbors and optimistically unchoked neighbor have not yet been selected
        if(0 == this.unchokingTimer && 0 == this.optimisticUnchokingTimer)
        {
            synchronized(this.peerProcessLock)
            {
                randomlyUnchokePreferred();
                this.unchokingTimer = System.currentTimeMillis();
                unchokeOptimistic();
                this.optimisticUnchokingTimer = System.currentTimeMillis();
            }
System.out.print("INITIAL RANDOM: Unchoked Status :");
for(int i = 0; i < this.neighborPeers.length; i++)
{
System.out.print("Peer " + this.neighborPeers[i].getPeerId() + " choked?->" + this.neighborPeers[i].getMyChoked() + " --- ");
}
System.out.print("\n");
        }
        else
        {
            //evaluate if the preferred choking interval time has passed
            if(System.currentTimeMillis() > this.unchokingTimer + 1000 * ReadCommon.getUnchokingInterval())
            {
                synchronized(this.peerProcessLock)
                {
                    //if this peer already has the full file, do random selection of preferred neighbors
                    if(true == this.myPeer.getHasFile())
                    {
                        randomlyUnchokePreferred();
                        this.unchokingTimer = System.currentTimeMillis();
                    }
                    //otherwise select based on bytes received from neighbors
                    else
                    {
                        selectivelyUnchokePreferred();
                        this.unchokingTimer = System.currentTimeMillis();
                    }
                }
System.out.print("PREFERRED TIMED OUT: Unchoked Status :");
for(int i = 0; i < this.neighborPeers.length; i++)
{
System.out.print("Peer " + this.neighborPeers[i].getPeerId() + " choked?->" + this.neighborPeers[i].getMyChoked() + " --- ");
if(this.neighborPeers[i] == this.optimisticPeer)
{
System.out.print("<===OPTIMISTIC   ");
}
}
System.out.print("\n");
            }

            //evaluate if the optimistic choking interval time has passed
            if(System.currentTimeMillis() > this.unchokingTimer + 1000 * ReadCommon.getOptimisticUnchokingInterval())
            {
                synchronized(this.peerProcessLock)
                {
                    unchokeOptimistic();
                    this.optimisticUnchokingTimer = System.currentTimeMillis();
                }
System.out.print("OPTIMISTIC TIMED OUT: Unchoked Status :");
for(int i = 0; i < this.neighborPeers.length; i++)
{
System.out.print("Peer " + this.neighborPeers[i].getPeerId() + " choked?->" + this.neighborPeers[i].getMyChoked() + " --- ");
if(this.neighborPeers[i] == this.optimisticPeer)
{
System.out.print("<===OPTIMISTIC   ");
}
}
System.out.print("\n");
            }   
        }
    }

    private void randomlyUnchokePreferred()
    {
        this.chokedNeighbors = new ArrayList<PeerObject>(Arrays.asList(neighborPeers));
        //remove neighbor peers that already have the complete file or are not interested
        int index = 0;
        while(this.chokedNeighbors.size() != index)
        {
            //remove peers that have the complete file
            if(true == this.chokedNeighbors.get(index).getHasFile())
            {
                //choke the neighbor
                this.chokedNeighbors.get(index).setMyChoked(true);
                //tell the ClientThread to send a choking message (if currently unchoked) to the neighbor
                ThreadMessage chokeMessage = new ThreadMessage(ThreadMessage.ThreadMessageType.CHOKE);
                this.chokedNeighbors.get(index).getClientThread().addThreadMessage(chokeMessage);
                //remove the neighbor from the temporary chokedNeighbors list
                this.chokedNeighbors.remove(index);
                index--;
            }
            //remove peers that are not interested
            else if(false == this.chokedNeighbors.get(index).getNeighborInterested())
            {
                //choke the neighbor
                this.chokedNeighbors.get(index).setMyChoked(true);
                //tell the ClientThread to send a choking message (if currently unchoked) to the neighbor
                ThreadMessage chokeMessage = new ThreadMessage(ThreadMessage.ThreadMessageType.CHOKE);
                this.chokedNeighbors.get(index).getClientThread().addThreadMessage(chokeMessage);
                //remove the neighbor from the temporary chokedNeighbors list
                this.chokedNeighbors.remove(index);
                index--;
            }

            index++;
        }

        //select random peers and add set them as unchoked neighbors
        int unchokedNeighborsAllowance = ReadCommon.getNumberOfPreferredNeighbors();
        Random random = new Random();
        boolean findNewOptimistic = false;
        while(0 != unchokedNeighborsAllowance && false == this.chokedNeighbors.isEmpty())
        {
            int randomIndex = random.nextInt(this.chokedNeighbors.size());

            //unchoke the neighbor
            this.chokedNeighbors.get(randomIndex).setMyChoked(false);
            //tell the ClientThread to send an unchoking message (if currently choked) to the neighbor
            ThreadMessage unchokeMessage = new ThreadMessage(ThreadMessage.ThreadMessageType.UNCHOKE);
            this.chokedNeighbors.get(randomIndex).getClientThread().addThreadMessage(unchokeMessage);
            //if the newly unchoked preferred neighbor was the optimistic neighbor, then mark that a new one needs to be found
            if(this.optimisticPeer == this.chokedNeighbors.get(randomIndex))
            {
                findNewOptimistic = true;
                this.optimisticPeer = null;
            }
            //remove the neighbor from the temporary chokedNeighbors list
            this.chokedNeighbors.remove(randomIndex);

            //decrement the unchokedNeighors count
            unchokedNeighborsAllowance--;
        }

        //remove the current optimistic neighbor if it is remaining in the pool of peers
        this.chokedNeighbors.remove(this.optimisticPeer);

        //find a new optimistic neighbor if the old one was converted into a preferred neighbors
        if(true == findNewOptimistic)
        {
            unchokeOptimistic();
        }

        //choke the remaining neighbors
        for(int i = 0; i < this.chokedNeighbors.size(); i++)
        {
            //choke the neighbor
            this.chokedNeighbors.get(i).setMyChoked(true);
            //tell the ClientThread to send a choking message (if currently unchoked) to the neighbor
            ThreadMessage chokeMessage = new ThreadMessage(ThreadMessage.ThreadMessageType.CHOKE);
            this.chokedNeighbors.get(i).getClientThread().addThreadMessage(chokeMessage);
        }
    }

    private void selectivelyUnchokePreferred()
    {
        this.chokedNeighbors = new ArrayList<PeerObject>(Arrays.asList(neighborPeers));
        //remove neighbor peers that already have the complete file or are not interested
        int index = 0;
        while(this.chokedNeighbors.size() != index)
        {
            //remove peers that have the complete file
            if(true == this.chokedNeighbors.get(index).getHasFile())
            {
                //choke the neighbor
                this.chokedNeighbors.get(index).setMyChoked(true);
                //tell the ClientThread to send a choking message (if currently unchoked) to the neighbor
                ThreadMessage chokeMessage = new ThreadMessage(ThreadMessage.ThreadMessageType.CHOKE);
                this.chokedNeighbors.get(index).getClientThread().addThreadMessage(chokeMessage);
                //remove the neighbor from the temporary chokedNeighbors list
                this.chokedNeighbors.remove(index);
                index--;
            }
            //remove peers that are not interested
            else if(false == this.chokedNeighbors.get(index).getNeighborInterested())
            {
                //choke the neighbor
                this.chokedNeighbors.get(index).setMyChoked(true);
                //tell the ClientThread to send a choking message (if currently unchoked) to the neighbor
                ThreadMessage chokeMessage = new ThreadMessage(ThreadMessage.ThreadMessageType.CHOKE);
                this.chokedNeighbors.get(index).getClientThread().addThreadMessage(chokeMessage);
                //remove the neighbor from the temporary chokedNeighbors list
                this.chokedNeighbors.remove(index);
                index--;
            }

            index++;
        }

        //find the highest byteDownloadedFrom and set those peers as preferred neighbors
        int unchokedNeighborsAllowance = ReadCommon.getNumberOfPreferredNeighbors();
        boolean findNewOptimistic = false;
        while(0 != unchokedNeighborsAllowance && false == this.chokedNeighbors.isEmpty())
        {
            //iterate through the neighbors and find the highest bytes contributor
            int highestBytesCount = this.chokedNeighbors.get(0).getBytesDownloadedFrom();
            int indexOfHighestBytesCount = 0;
            for(int i = 1; i < this.chokedNeighbors.size(); i++)
            {
                if(this.chokedNeighbors.get(i).getBytesDownloadedFrom() > highestBytesCount)
                {
                    highestBytesCount = this.chokedNeighbors.get(i).getBytesDownloadedFrom();
                    indexOfHighestBytesCount = i;
                }
            }

            //unchoke the neighbor
            this.chokedNeighbors.get(indexOfHighestBytesCount).setMyChoked(false);
            //tell the ClientThread to send an unchoking message (if currently choked) to the neighbor
            ThreadMessage unchokeMessage = new ThreadMessage(ThreadMessage.ThreadMessageType.UNCHOKE);
            this.chokedNeighbors.get(indexOfHighestBytesCount).getClientThread().addThreadMessage(unchokeMessage);
            //if the newly unchoked preferred neighbor was the optimistic neighbor, then mark that a new one needs to be found
            if(this.optimisticPeer == this.chokedNeighbors.get(indexOfHighestBytesCount))
            {
                findNewOptimistic = true;
                this.optimisticPeer = null;
            }
            //remove the neighbor from the temporary chokedNeighbors list
            this.chokedNeighbors.remove(indexOfHighestBytesCount);

            //decrement the unchokedNeighors count
            unchokedNeighborsAllowance--;
        }

        //remove the current optimistic neighbor if it is remaining in the pool of peers
        this.chokedNeighbors.remove(this.optimisticPeer);

        //find a new optimistic neighbor if the old one was converted into a preferred neighbors
        if(true == findNewOptimistic)
        {
            unchokeOptimistic();
        }

        //choke the remaining neighbors
        for(int i = 0; i < this.chokedNeighbors.size(); i++)
        {
            //choke the neighbor
            this.chokedNeighbors.get(i).setMyChoked(true);
            //tell the ClientThread to send a choking message (if currently unchoked) to the neighbor
            ThreadMessage chokeMessage = new ThreadMessage(ThreadMessage.ThreadMessageType.CHOKE);
            this.chokedNeighbors.get(i).getClientThread().addThreadMessage(chokeMessage);
        }
    }

    private void unchokeOptimistic()
    {
        //if the current optimistic neighbor is not null, add it back to the pool of potential optimistic neighbors
        if(null != this.optimisticPeer)
        {
            this.chokedNeighbors.add(this.optimisticPeer);
        }

        //if there are no potential optimistic peers, exit
        if(0 == this.chokedNeighbors.size())
        {
            return;
        }

        //randomly choose an optimistic peer
        Random random = new Random();
        int randomIndex = random.nextInt(this.chokedNeighbors.size());

        //if the newly selected choked neighbor is not the same as the old one, choke the old one and have ClientThread send a choke message
        if(chokedNeighbors.get(randomIndex) != this.optimisticPeer && null != this.optimisticPeer)
        {
            this.optimisticPeer.setMyChoked(true);
            ThreadMessage chokeMessage = new ThreadMessage(ThreadMessage.ThreadMessageType.CHOKE);
            this.optimisticPeer.getClientThread().addThreadMessage(chokeMessage);
        }

        //set the new optimistic peer
        this.optimisticPeer = this.chokedNeighbors.get(randomIndex);
        //remove it from the optimisticPeer pool
        this.chokedNeighbors.remove(randomIndex);

        //unchoke the new optimistic and have ClientThread send an unchoke message
        this.optimisticPeer.setMyChoked(false);
        ThreadMessage unchokeMessage = new ThreadMessage(ThreadMessage.ThreadMessageType.UNCHOKE);
        this.optimisticPeer.getClientThread().addThreadMessage(unchokeMessage);
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
