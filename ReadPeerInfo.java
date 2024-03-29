/*
CNT 5105C "Computer Networks" - Spring 2021
Group Project - P2P File Sharing
Benjamin Hsu, Lavanya Khular, Chitranshu Raj
*/

//Imports
import java.io.*; //Exceptions
import java.util.*; //List, ArrayList, Iterator
import java.nio.file.*; //Files, Path
import java.lang.*; //Integer

public class ReadPeerInfo
{
    //names of the file to be read
    private static final String peerFileName = "PeerInfo.cfg";

    //PeerInfo.cfg parameters
    private List<String> peerFileLines;
    private final List<PeerObject> peers = new ArrayList<PeerObject>();
    
    //Constructor reads the PeerInfo.cfg file
    public ReadPeerInfo()
    {
        //first read in the file
        try
        {
            //convert the file's string name into a Path type object
            Path peerFilePath = Paths.get(peerFileName);

            //read the file's lines in entirety first
            peerFileLines = Files.readAllLines(peerFilePath);

            //check if for some reason the line is just whitespace (e.g. just a newline), if so - remove it
            for(Iterator<String> iterator = peerFileLines.iterator(); iterator.hasNext(); )
            {
                String line = iterator.next();
                if(line.isEmpty())
                {
                    iterator.remove();
                }
            }
        }
        catch(FileNotFoundException exception)
        {
            System.out.print("ERROR: ReadPeerInfo.java --- PeerInfo.cfg was not found.\n");
            exception.printStackTrace();
            System.exit(1);
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: ReadPeerInfo.java --- some IO error for PeerInfo.cfg .\n");
            exception.printStackTrace();
            System.exit(1);
        }

        //check if the read-in lines have the appropriate number of lines
        if(null == peerFileLines || 2 > peerFileLines.size())
        {
            System.out.print("ERROR: ReadPeerInfo.java --- PeerInfo.cfg has less than 2 peers or was not read properly.\n");
            System.exit(1);
        }

        //Extract the parameters' values from the PeerInfo.cfg read-in lines
        try
        {
            for(int i = 0; i < peerFileLines.size(); i++)
            {
                int peerId = Integer.parseInt(peerFileLines.get(i).trim().split(" ")[0]);
                String hostName = peerFileLines.get(i).split(" ")[1].trim();
                int portNumber = Integer.parseInt(peerFileLines.get(i).trim().split(" ")[2]);
                int hasFileAsInt = Integer.parseInt(peerFileLines.get(i).trim().substring(peerFileLines.get(i).trim().lastIndexOf(' ') + 1));
                boolean hasFile;
                if(1 == hasFileAsInt)
                {
                    hasFile = true;
                }
                else
                {
                    hasFile = false;
                }

                //add the extracted peer info to the peers list
                peers.add(new PeerObject(peerId, hostName, portNumber, hasFile));
            }
        }
        catch(NumberFormatException exception)
        {
            System.out.print("ERROR: ReadPeerInfo.java --- some error processing PeerInfo.cfg Strings into ints.\n");
            exception.printStackTrace();
            System.exit(1);
        }
        catch(StringIndexOutOfBoundsException exception)
        {
            System.out.print("ERROR: ReadPeerInfo.java --- some error processing PeerInfo.cfg String.\n");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    //PeerInfo.cfg public accessor methods

    public List<PeerObject> getPeersInfo()
    {
        return peers;
    }

    //search the peers to find the given peer id #, and return its index position (e.g. 0, 1, 2, etc.) in the list
    public int getPeerIndexPosition(int peerId)
    {
        //iterate through the list of peers
        for(int i = 0; i < this.peers.size(); i++)
        {
            if(peerId == peers.get(i).getPeerId())
            {
                return i;
            }
        }

        //if this has been reached, then the peer id # was not found, so return error value -1
        return -1;
    }
}
