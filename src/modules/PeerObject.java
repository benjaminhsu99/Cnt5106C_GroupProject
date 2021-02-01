/*
CNT 5105C "Computer Networks" - Spring 2021
Group Project
Lavanya Khular, Chitranshu Raj, Benjamin Hsu

Last Edited: 2/1/2021
*/

//Created package to group together all the various .java files together
package src.modules;

//Imports
import java.io.*; //Exceptions, File, BufferedReader
import java.util.*; //List, ArrayList
import java.net.*; //Socket

public class PeerObject
{
    //parameters
    private final int peerId;
    private final String hostName;
    private final int portNumber;
    private boolean hasFile;
    private Socket socket;
    private volatile int bytesDownloadedFrom;

    //object constructor
    public PeerObject(int peerId, String hostName, int portNumber, boolean hasFile)
    {
        this.peerId = peerId;
        this.hostName = hostName;
        this.portNumber = portNumber;
        this.hasFile = hasFile;

        this.socket = null;
        this.bytesDownloadedFrom = 0;
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
    public void setSocket(Socket socket)
    {
        this.socket = socket;
    }
    public Socket getSocket()
    {
        return this.socket;
    }
    public void addBytesDownloadedFrom(int bytesDownloaded)
    {
        this.bytesDownloadedFrom += bytesDownloaded;
    }
    public void resetBytesDownloadedFrom()
    {
        this.bytesDownloadedFrom = 0;
    }
    public int getBytesDownloadedFrom()
    {
        return this.bytesDownloadedFrom;
    }
}
