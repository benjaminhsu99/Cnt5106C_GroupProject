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
        BITFIELD;
    }
    ThreadMessageType threadMessageType;

    private byte[] bitfield;

    //constructors
    public ThreadMessage(byte[] bitfield)
    {
        this.threadMessageType = ThreadMessageType.BITFIELD;
        this.bitfield = bitfield;
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
}