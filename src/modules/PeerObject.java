/*
CNT 5105C "Computer Networks" - Spring 2021
Group Project
Lavanya Khular, Chitranshu Raj, Benjamin Hsu

Last Edited: 1/23/2021
*/

//Created package to group together all the various .java files together
package src.modules;

//Imports
import java.io.*; //Exceptions, File, BufferedReader
import java.util.*; //List, ArrayList

public class PeerObject
{
    //parameters
    private final int peerId;
    private final String hostName;
    private final int portNumber;
    private boolean hasFile;

    //object constructor
    public PeerObject(int peerId, String hostName, int portNumber, boolean hasFile)
    {
        this.peerId = peerId;
        this.hostName = hostName;
        this.portNumber = portNumber;
        this.hasFile = hasFile;
    }

    //accessor methods
    public int getPeerId()
    {
        return this.peerId;
    }
    public String getHostName()
    {
        return this.hostName;
    }
    public int getPortNumber()
    {
        return this.portNumber;
    }
    public boolean getHasFile()
    {
        return this.hasFile;
    }
}
