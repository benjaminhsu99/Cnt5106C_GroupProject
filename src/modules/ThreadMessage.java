/*
CNT 5105C "Computer Networks" - Spring 2021
Group Project - P2P File Sharing
Benjamin Hsu, Lavanya Khular, Chitranshu Raj
*/

//Created package to group together all the various .java files together
package src.modules;

//Imports
import java.io.*; //Exceptions
import java.util.*; //BitSet
import java.net.*; //Socket
import java.util.concurrent.*; //ArrayBlockingQueue

public class ThreadMessage
{
    //parameters
    public enum ThreadMessageType
    {
        BITFIELD,
        INTERESTSTATUS,
        SENDCHOKE,
        SENDUNCHOKE,
        RECEIVEDCHOKE,
        RECEIVEDUNCHOKE,
        REQUEST,
        PIECE,
        PEERPROCESSHAVE,
        SENDHAVE,
        HAVE;
    }
    ThreadMessageType threadMessageType;

    private byte[] bytesArray;
    private boolean interestStatus;
    private int pieceIndex;
    private BlockingQueue<Integer> pieceProcessedLock;

    //constructors
    public ThreadMessage(byte[] bitfield)
    {
        this.threadMessageType = ThreadMessageType.BITFIELD;
        this.bytesArray = bitfield;
    }
    public ThreadMessage(boolean interestStatus)
    {
        this.threadMessageType = ThreadMessageType.INTERESTSTATUS;
        this.interestStatus = interestStatus;
    }
    public ThreadMessage(int pieceIndex, byte[] pieceBytes, BlockingQueue<Integer> pieceProcessedLock)
    {
        this.threadMessageType = ThreadMessageType.PIECE;
        this.pieceIndex = pieceIndex;
        this.bytesArray = pieceBytes;
        this.pieceProcessedLock = pieceProcessedLock;
    }
    public ThreadMessage(ThreadMessageType threadMessageType, int pieceIndex, BlockingQueue<Integer> pieceProcessedLock)
    {
        this.threadMessageType = threadMessageType;
        this.pieceIndex = pieceIndex;
        this.pieceProcessedLock = pieceProcessedLock;
    }
    public ThreadMessage(ThreadMessageType threadMessageType, int pieceIndex)
    {
        this.threadMessageType = threadMessageType;
        this.pieceIndex = pieceIndex;
    }
    public ThreadMessage(ThreadMessageType threadMessageType)
    {
        this.threadMessageType = threadMessageType;
    }

    //accessor methods
    public ThreadMessageType getThreadMessageType()
    {
        return this.threadMessageType;
    }
    public byte[] getBytesArray()
    {
        return this.bytesArray;
    }
    public boolean getInterestStatus()
    {
        return this.interestStatus;
    }
    public int getPieceIndex()
    {
        return this.pieceIndex;
    }
    public BlockingQueue<Integer> getPieceProcessedLock()
    {
        return this.pieceProcessedLock;
    }
}