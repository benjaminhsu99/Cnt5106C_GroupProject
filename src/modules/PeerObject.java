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

public class PeerObject
{
    //parameters
    private final int peerId;
    private final String hostName;
    private final int portNumber;
    private volatile boolean hasFile;
    private Socket socket;
    private volatile int bytesDownloadedFrom;
    private volatile BitSet bitfield;
    private int[] requested;
    private int neighborRequestedPiece;
    private volatile boolean neighborInterested;
    private volatile boolean myInterested;
    private volatile boolean neighborChoked;
    private volatile boolean myChoked;
    private ClientThread clientThread;

    //object constructor
    public PeerObject(int peerId, String hostName, int portNumber, boolean hasFile)
    {
        this.peerId = peerId;
        this.hostName = hostName;
        this.portNumber = portNumber;
        this.hasFile = hasFile;

        this.socket = null;
        this.bytesDownloadedFrom = 0;

        //set the bitfield to a size that is the number of pieces, as calculated from the Common.cfg file
        this.bitfield = new BitSet(ReadCommon.getNumberOfPieces());
        //if the peer has the setting in Peers.cfg indicating that it has the file, set the bitfield entries to true
        if(true == this.hasFile)
        {
            for(int i = 0; i < ReadCommon.getNumberOfPieces(); i++)
            {
                //Bitset's set() method changes the indicated index to the second parameter's value
                bitfield.set(i, true);
            }
        }
        //else set the initial bitfield entries to false
        else
        {
            //BitSet's clear method sets all bits to false
            bitfield.clear();
        }

        //set the default values of requested pieces from peerIds to "no peer" (represented by -1)
        this.requested = new int[ReadCommon.getNumberOfPieces()];
        for(int i = 0; i < this.requested.length; i++)
        {
            requested[i] = -1;
        }

        //set the default neighbor's requested piece index to -1
        this.neighborRequestedPiece = -1;

        //set default value of interested to false
        this.neighborInterested = false;
        this.myInterested = false;

        //set default value of choked to true
        this.neighborChoked = true;
        this.myChoked = true;
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
    public Socket getSocket()
    {
        return this.socket;
    }
    public void setSocket(Socket socket)
    {
        this.socket = socket;
    }
    public void closeSocket()
    {
        try
        {
            this.socket.close();
        }
        catch(IOException exception)
        {
            System.out.print("Error (but no action taken): Some IO Exception on trying to close socket of peer " + this.peerId + ".\n");
            exception.printStackTrace();
        }
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
    public boolean hasPiece(int pieceIndex)
    {
        //error-check: check if the piece index is within bounds
        if(ReadCommon.getNumberOfPieces() <= pieceIndex || 0 > pieceIndex)
        {
            System.out.print("ERROR: FileWriter.java hasPiece() --- pieceIndex " + pieceIndex +" is not a valid piece number.\n");
            System.exit(1);
        }

        //else return the boolean value of the bitfield, for the piece specified
        return this.bitfield.get(pieceIndex);
    }
    public byte[] getBitfieldAsBytes()
    {
        //BitSet's valueOf() method converts a Bitfield to a little-endian byte array
        return this.bitfield.toByteArray();
    }
    public void setBitfieldFromBytes(byte[] bitfieldAsBytes, LogWriter logger)
    {
        //BitSet's valueOf() method converts a little-endian byte array to a BitField
        this.bitfield = BitSet.valueOf(bitfieldAsBytes);

        //check if the receieve bitfield indicates that the peer has the full file
        //check every bit in the bitfield
        boolean hasAllPieces = true;
        for(int i = 0; i < ReadCommon.getNumberOfPieces(); i++)
        {
            if(false == this.bitfield.get(i))
            {
                hasAllPieces = false;
                break;
            }
        }

        //if all the file's pieces are present, then the message as the peer having "completed the download" (technically it already had it)
        if(true == hasAllPieces)
        {
            logger.logComplete(this.peerId);
        }
    }
    public void setBitfieldPieceAsTrue(int pieceIndex, LogWriter logger)
    {
        //error-check: check if the piece index is within bounds
        if(ReadCommon.getNumberOfPieces() <= pieceIndex || 0 > pieceIndex)
        {
            System.out.print("ERROR: FileWriter.java setBitFieldPieceAsTrue() --- pieceIndex " + pieceIndex +" is not a valid piece number.\n");
            System.exit(1);
        }
        
        //else set the bitfield to true, for the specified piece
        //Bitset's set() method changes the indicated index to the second parameter's value
        this.bitfield.set(pieceIndex, true);

        //check if this causes the hasFile status to change from false to true
        if(false == this.hasFile)
        {
            //check every bit in the bitfield
            boolean hasAllPieces = true;
            for(int i = 0; i < ReadCommon.getNumberOfPieces(); i++)
            {
                if(false == this.bitfield.get(i))
                {
                    hasAllPieces = false;
                    break;
                }
            }

            //if all the file's pieces are present, then mark "hasFile" as true and log the message as having completed the download
            if(true == hasAllPieces)
            {
                this.hasFile = true;
                logger.logComplete(this.peerId);
            }
        }
    }
    public int getRequested(int pieceIndex)
    {
        //error-check: check if the piece index is within bounds
        if(ReadCommon.getNumberOfPieces() <= pieceIndex || 0 > pieceIndex)
        {
            System.out.print("ERROR: FileWriter.java isCurrentlyRequested() --- pieceIndex " + pieceIndex +" is not a valid piece number.\n");
            System.exit(1);
        }

        //else return whether the piece is currently already requested
        return this.requested[pieceIndex];
    }
    public void setRequested(int pieceIndex, int peerIdRequestedFrom)
    {
        //error-check: check if the piece index is within bounds
        if(ReadCommon.getNumberOfPieces() <= pieceIndex || 0 > pieceIndex)
        {
            System.out.print("ERROR: FileWriter.java setAsRequested() --- pieceIndex " + pieceIndex +" is not a valid piece number.\n");
            System.exit(1);
        }
        
        //set the piece as requested
        this.requested[pieceIndex] = peerIdRequestedFrom;
    }
    public void clearRequested(int peerIdRequestedFrom)
    {
        //find any instances of the peerId specified and set the piece as not-requested (represented by -1)
        for(int i = 0; i < this.requested.length; i++)
        {
            if(peerIdRequestedFrom == this.requested[i])
            {
                this.requested[i] = -1;
            }
        }
    }
    public int getNeighborRequestedPiece()
    {
        return this.neighborRequestedPiece;
    }
    public void setNeighborRequestedPiece(int requestedPieceIndex)
    {
        this.neighborRequestedPiece = requestedPieceIndex;
    }
    public boolean getNeighborInterested()
    {
        return this.neighborInterested;
    }
    public void setNeighborInterested(boolean neighborInterested)
    {
        this.neighborInterested = neighborInterested;
    }
    public boolean getMyInterested()
    {
        return this.myInterested;
    }
    public void setMyInterested(boolean myInterested)
    {
        this.myInterested = myInterested;
    }
    public boolean getNeighborChoked()
    {
        return this.neighborChoked;
    }
    public void setNeighborChoked(boolean neighborChoked)
    {
        this.neighborChoked = neighborChoked;
    }
    public boolean getMyChoked()
    {
        return this.myChoked;
    }
    public void setMyChoked(boolean myChoked)
    {
        this.myChoked = myChoked;
    }
    public void setClientThread(ClientThread clientThread)
    {
        this.clientThread = clientThread;
    }
    public ClientThread getClientThread()
    {
        return this.clientThread;
    }
}
