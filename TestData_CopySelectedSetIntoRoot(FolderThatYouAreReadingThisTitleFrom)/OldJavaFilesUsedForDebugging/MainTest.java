/*
CNT 5105C "Computer Networks" - Spring 2021
Group Project - P2P File Sharing
Benjamin Hsu, Lavanya Khular, Chitranshu Raj
*/
//Just a simple temporary main method for debugging, to call on and see if the other java files work correctly

package src;

//Imports
import src.modules.*;

import java.util.*;
import java.nio.file.*;

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
        System.out.print("From Common.cfg, the number of pieces was read to be: " + ReadCommon.getNumberOfPieces() + "\n");
        
        ReadPeerInfo readPeerInfoInstance = new ReadPeerInfo();
        List<PeerObject> peers = readPeerInfoInstance.getPeersInfo();
        PeerObject[] peerArrayForLater = new PeerObject[peers.size()];
        for(int i = 0; i < peers.size(); i++)
        {
            System.out.print("PEER INDEX #" + i + ": " + 
            peers.get(i).getPeerId() + " " + 
            peers.get(i).getPortNumber() + " " + 
            peers.get(i).getHostName() + " " + 
            peers.get(i).getHasFile() + 
            "\n");
            peerArrayForLater[i] = peers.get(i);
        }

        System.out.print("Peer ID 1000 position index is: " + readPeerInfoInstance.getPeerIndexPosition(1000) + "\n");
        System.out.print("Peer ID 1001 position index is: " + readPeerInfoInstance.getPeerIndexPosition(1001) + "\n");
        System.out.print("Peer ID 1002 position index is: " + readPeerInfoInstance.getPeerIndexPosition(1002) + "\n");

        FileWriter testFile = new FileWriter(999);
        System.out.print("\nA test peer subfolder with peerId 999 was created, and an empty P2P file in it was created. Check the file directory to see if it's there.\n");
        byte[] firstWriteTest = {'d', 'e', 'f'};
        testFile.writePiece(1, firstWriteTest);
        System.out.print("Piece size index 1 with contents def was written.\n");
        byte[] secondWriteTest = {'A'};
        testFile.writePiece(17, secondWriteTest);
        System.out.print("Piece size index 17 (last piece with size 1) with contents A was written.\n");
        byte[] firstReadTest = testFile.readPiece(1);
        System.out.print("Piece index 1 of size " + firstReadTest.length + " was read with contents " + new String(firstReadTest) + "\n");
        byte[] secondReadTest = testFile.readPiece(17);
        System.out.print("Piece index 17 of size " + secondReadTest.length + " was read with contents " + new String(secondReadTest) + "\n");
        byte[] thirdReadTest = testFile.readPiece(0);
        System.out.print("Piece index 0 of size " + thirdReadTest.length + " was read with contents (should be random undefined or null contents since not written) " + new String(thirdReadTest) + "\n");

        LogWriter testLog = new LogWriter(999);
        System.out.print("\nA test log file with peerId 999 was created. Check the file directory to see if it's there.\n");
        testLog.logSendConnection(111);
        System.out.print("Test log had a send-connection to fake peer 111 entry.\n");
        testLog.logReceiveConnection(11111);
        System.out.print("Test log had a receive-connection to fake peer 111 entry.\n");
        testLog.logChangePreferredNeighbors(Arrays.asList(peerArrayForLater));
        System.out.print("Test log had change of preferred message added.\n");
        testLog.logChangeOptimisticNeighbor(peerArrayForLater[0]);
        System.out.print("Test log had change of optimistic message added.\n");
        testLog.logUnchoked(222);
        System.out.print("Test log had unchoked message added.\n");
        testLog.logChoked(333);
        System.out.print("Test log had choked message added.\n");
        testLog.logHave(444, 5);
        System.out.print("Test log had have message added.\n");
        testLog.logInterested(555);
        System.out.print("Test log had interested message added.\n");
        testLog.logNotInterested(777);
        System.out.print("Test log had not interested message added.\n");
        testLog.logDownload(888, 9, 50);
        System.out.print("Test log had piece downloaded message added.\n");
        testLog.logComplete(999);
        System.out.print("Test log had all-complete message added.\n");

        System.out.print("\nFirst Peer\n");
        for(int i = 0; i < ReadCommon.getNumberOfPieces(); i++)
        {
            System.out.print(peers.get(0).hasPiece(i) + " ");
        }
        System.out.print("\nSecond Peer\n");
        for(int i = 0; i < ReadCommon.getNumberOfPieces(); i++)
        {
            System.out.print(peers.get(1).hasPiece(i) + " ");
        }
        System.out.print("\n");
        peers.get(1).setBitfieldPieceAsTrue(3, testLog);
        System.out.print("Set second peer's bit index 3 to true.\n");
        for(int i = 0; i < ReadCommon.getNumberOfPieces(); i++)
        {
            System.out.print(peers.get(1).hasPiece(i) + " ");
        }
        System.out.print("\n");
        byte[] firstPeerBitsetAsBytes = peers.get(0).getBitfieldAsBytes();
        byte[] secondPeerBitsetAsBytes = peers.get(1).getBitfieldAsBytes();
        peers.get(0).setBitfieldFromBytes(secondPeerBitsetAsBytes, testLog);
        peers.get(1).setBitfieldFromBytes(firstPeerBitsetAsBytes, testLog);
        System.out.print("After swapping\n");
        System.out.print("First Peer\n");
        for(int i = 0; i < ReadCommon.getNumberOfPieces(); i++)
        {
            System.out.print(peers.get(0).hasPiece(i) + " ");
        }
        System.out.print("\nSecond Peer\n");
        for(int i = 0; i < ReadCommon.getNumberOfPieces(); i++)
        {
            System.out.print(peers.get(1).hasPiece(i) + " ");
        }
        System.out.print("\n");
    }
}
