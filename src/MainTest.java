//Just a simple temporary main method to call on and see if the other java files work correctly

package src;

//Imports
import src.modules.*;
import java.util.*; //List, ArrayList, Iterator
import java.nio.file.*; //Files, Path

public class MainTest
{
    public static void main(String[] args)
    {
        System.out.print("MainTest Hello!\n");

        System.out.print("\nCommand Line arguments # count: " + args.length + "\n");
        for(int i = 0; i < args.length; i++)
        {
            System.out.print("Arg: " + args[i] + "\n");
        }
        System.out.print("\n");

        System.out.print("From Common.cfg, the number of preferred neighbors was read to be: " + ReadCommon.getNumberOfPreferredNeighbors() + "\n");
        System.out.print("From Common.cfg, the unchoking interval was read to be: " + ReadCommon.getUnchokingInterval() + "\n");
        System.out.print("From Common.cfg, the optimistic unchoking interval was read to be: " + ReadCommon.getOptimisticUnchokingInterval() + "\n");
        System.out.print("From Common.cfg, the file name was read to be: " + ReadCommon.getFileName() + "\n");
        System.out.print("From Common.cfg, the file size was read to be: " + ReadCommon.getFileSize() + "\n");
        System.out.print("From Common.cfg, the piece size was read to be: " + ReadCommon.getPieceSize() + "\n");
        
        ReadPeerInfo readPeerInfoInstance = new ReadPeerInfo();
        List<PeerObject> peers = readPeerInfoInstance.getPeersInfo();
        for(int i = 0; i < peers.size(); i++)
        {
            System.out.print("PEER INDEX #" + i + ": " + 
            peers.get(i).getPeerId() + " " + 
            peers.get(i).getPortNumber() + " " + 
            peers.get(i).getHostName() + " " + 
            peers.get(i).getHasFile() + 
            "\n");
        }

        System.out.print("Peer ID 1000 position index is: " + readPeerInfoInstance.getPeerIndexPosition(1000) + "\n");
        System.out.print("Peer ID 1001 position index is: " + readPeerInfoInstance.getPeerIndexPosition(1001) + "\n");
        System.out.print("Peer ID 1002 position index is: " + readPeerInfoInstance.getPeerIndexPosition(1002) + "\n");
    }
}
