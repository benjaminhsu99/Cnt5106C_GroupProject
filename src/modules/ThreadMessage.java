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
        RECEIVEDUNCHOKE;
    }
    ThreadMessageType threadMessageType;

    private byte[] bitfield;
    private boolean interestStatus;

    //constructors
    public ThreadMessage(byte[] bitfield)
    {
        this.threadMessageType = ThreadMessageType.BITFIELD;
        this.bitfield = bitfield;
    }
    public ThreadMessage(boolean interestStatus)
    {
        this.threadMessageType = ThreadMessageType.INTERESTSTATUS;
        this.interestStatus = interestStatus;
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
    public byte[] getBitfield()
    {
        return this.bitfield;
    }
    public boolean getInterestStatus()
    {
        return this.interestStatus;
    }
}