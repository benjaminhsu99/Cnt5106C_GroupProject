/*
CNT 5105C "Computer Networks" - Spring 2021
Group Project - P2P File Sharing
Benjamin Hsu, Lavanya Khular, Chitranshu Raj
*/

//Imports
import java.io.*; //Exceptions, BufferedWriter
import java.nio.file.*; //Files, Path, StandardOpenOption
import java.nio.charset.*; //StandardCharsets
import java.time.*; //LocalDateTime
import java.time.format.*; //DateTimeFormatter
import java.lang.*; //Integer
import java.util.*; //List, ArrayList

public class LogWriter
{
    //parameters
    private BufferedWriter theLog;
    private int myPeerId;

    //object constructor
    public LogWriter(int peerId)
    {
        //set myPeerId
        this.myPeerId = peerId;

        //project specifications states that log should be written to a "log_peer_[peerID].log" file in the working directory
        String logName = "log_peer_" + Integer.toString(peerId) + ".log";

        try
        {
            //convert the file's string name into a Path type object
            Path logNamePath = Paths.get(logName);

            //if the file does not currently exist
            if(Files.notExists(logNamePath))
            {
                //create the file
                //and set the file pointer
                Files.createFile(logNamePath);
            }

            //set the file pointer to the pre-existing or new log file
            this.theLog = Files.newBufferedWriter(logNamePath, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        }
        catch(FileNotFoundException exception)
        {
            System.out.print("ERROR: LogWriter.java Constructor --- Couldn't find the file (it should have already been created?).\n");
            exception.printStackTrace();
            System.exit(1);
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: LogWriter.java Constructor --- some IO error.\n");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    //Methods
    private String getTime()
    {
        DateTimeFormatter printFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
        LocalDateTime currentTime = LocalDateTime.now();
        String timeAsString = printFormat.format(currentTime);

        return timeAsString;
    }

    public void logSendConnection(int otherPeerId)
    {
        //write the log entry
        try
        {
            this.theLog.append("[" + getTime() + "]: Peer " + this.myPeerId + " makes a connection to Peer " + otherPeerId + ".\n");
            this.theLog.flush();
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: FileWriter.java logStartConnection() --- some IO error.\n");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    public void logReceiveConnection(int otherPeerId)
    {
        //write the log entry
        try
        {
            this.theLog.append("[" + getTime() + "]: Peer " + this.myPeerId + " is connected from Peer " + otherPeerId + ".\n");
            this.theLog.flush();
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: FileWriter.java logStartConnection() --- some IO error.\n");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    public void logChangePreferredNeighbors(List<PeerObject> preferredNeighbors)
    {
        //write the log entry
        try
        {
            if(true == preferredNeighbors.isEmpty())
            {
                this.theLog.append("[" + getTime() + "]: Peer " + this.myPeerId + " has the preferred neighbors [NONE].\n");
                this.theLog.flush();
            }
            else
            {
                this.theLog.append("[" + getTime() + "]: Peer " + this.myPeerId + " has the preferred neighbors ");
                for(int i = 0; i < preferredNeighbors.size(); i++)
                {
                    this.theLog.append(Integer.toString(preferredNeighbors.get(i).getPeerId()));
                    if(preferredNeighbors.size() - 1 != i)
                    {
                        this.theLog.append(", ");
                    }
                } 
                this.theLog.append(".\n");
                this.theLog.flush();
            }
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: FileWriter.java logChangePreferredNeighbors() --- some IO error.\n");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    public void logChangeOptimisticNeighbor(PeerObject optimisticNeighbor)
    {
        //write the log entry
        try
        {
            if(null != optimisticNeighbor)
            {
                this.theLog.append("[" + getTime() + "]: Peer " + this.myPeerId + " has the optimistically unchoked neighbor " + Integer.toString(optimisticNeighbor.getPeerId()) + ".\n");
            }
            else
            {
                this.theLog.append("[" + getTime() + "]: Peer " + this.myPeerId + " has the optimistically unchoked neighbor [NONE].\n");
            }
            this.theLog.flush();
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: FileWriter.java logChangeOptimisticNeighbors() --- some IO error.\n");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    public void logUnchoked(int otherPeerId)
    {
        //write the log entry
        try
        {
            this.theLog.append("[" + getTime() + "]: Peer " + this.myPeerId + " is unchoked by " + otherPeerId + ".\n");
            this.theLog.flush();
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: FileWriter.java logUnchoked() --- some IO error.\n");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    public void logChoked(int otherPeerId)
    {
        //write the log entry
        try
        {
            this.theLog.append("[" + getTime() + "]: Peer " + this.myPeerId + " is choked by " + otherPeerId + ".\n");
            this.theLog.flush();
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: FileWriter.java logChoked() --- some IO error.\n");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    public void logHave(int otherPeerId, int pieceIndex)
    {
        //write the log entry
        try
        {
            this.theLog.append("[" + getTime() + "]: Peer " + this.myPeerId + " received the 'have' message from " + otherPeerId + " for the piece " + pieceIndex + ".\n");
            this.theLog.flush();
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: FileWriter.java logHave() --- some IO error.\n");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    public void logInterested(int otherPeerId)
    {
        //write the log entry
        try
        {
            this.theLog.append("[" + getTime() + "]: Peer " + this.myPeerId + " received the 'interested' message from " + otherPeerId + ".\n");
            this.theLog.flush();
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: FileWriter.java logInterested() --- some IO error.\n");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    public void logNotInterested(int otherPeerId)
    {
        //write the log entry
        try
        {
            this.theLog.append("[" + getTime() + "]: Peer " + this.myPeerId + " received the 'not interested' message from " + otherPeerId + ".\n");
            this.theLog.flush();
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: FileWriter.java logNotInterested() --- some IO error.\n");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    public void logDownload(int otherPeerId, int pieceIndex, int numberOfCollectedPieces)
    {
        //write the log entry
        try
        {
            this.theLog.append("[" + getTime() + "]: Peer " + this.myPeerId + " has downloaded the piece " + pieceIndex + " from " + otherPeerId + ". Now the number of pieces it has is " + numberOfCollectedPieces + ".\n");
            this.theLog.flush();
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: FileWriter.java logDownload() --- some IO error.\n");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    public void logComplete(int peerId)
    {
        //write the log entry
        try
        {
            this.theLog.append("[" + getTime() + "]: Peer " + peerId + " has downloaded the complete file.\n");
            this.theLog.flush();
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: FileWriter.java logComplete() --- some IO error.\n");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    //non-specified function that can write any arbitrary debug message to the log
    public void logDebug(String debugMessage)
    {
        //write the log entry
        try
        {
            this.theLog.append(debugMessage);
            this.theLog.flush();
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: FileWriter.java logDebug() --- some IO error.\n");
            exception.printStackTrace();
            System.exit(1);
        }
    }
}
