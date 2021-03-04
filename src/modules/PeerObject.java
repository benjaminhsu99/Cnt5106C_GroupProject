/*
CNT 5105C "Computer Networks" - Spring 2021
Group Project - P2P File Sharing
Benjamin Hsu, Lavanya Khular, Chitranshu Raj
*/

//Created package to group together all the various .java files together
package src.modules;

//Imports
import java.io.*; //Exceptions, File, BufferedReader
import java.util.*; //List, ArrayList, BitSet
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
    private volatile int[] requested;
    private volatile boolean interested;

    private final Object hasFileLock = new Object();
    private final Object bytesDownloadedFromLock = new Object();
    private final Object bitfieldLock = new Object();
    private final Object requestedLock = new Object();
    private final Object interestedLock = new Object();

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

        //set default value of interested to false
        this.interested = false;
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
        synchronized(hasFileLock)
        {
            return this.hasFile;
        }
    }
    public boolean checkHasFile()
    {
        //check every bit in the bitfield
        boolean hasAllPieces = true;
        synchronized(bitfieldLock)
        {
            for(int i = 0; i < ReadCommon.getNumberOfPieces(); i++)
            {
                if(false == this.bitfield.get(i))
                {
                    hasAllPieces = false;
                    break;
                }
            }
        }

        //if all the file's pieces are present, and if it hasn't been marked before, then mark "hasFile" as true
        synchronized(hasFileLock)
        {
            if(true == hasAllPieces && false == this.hasFile)
            {
                this.hasFile = true;
            }
        }

        //return whether the peer has all bitfield pieces (i.e. has the file) or not
        return hasAllPieces;
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
        synchronized(bytesDownloadedFromLock)
        {
            this.bytesDownloadedFrom += bytesDownloaded;
        }
    }
    public void resetBytesDownloadedFrom()
    {
        synchronized(bytesDownloadedFromLock)
        {
            this.bytesDownloadedFrom = 0;
        }
    }
    public int getBytesDownloadedFrom()
    {
        synchronized(bytesDownloadedFromLock)
        {
            return this.bytesDownloadedFrom;
        }
    }
    public boolean hasPiece(int pieceIndex)
    {
        //error-check: check if the piece index is within bounds
        if(ReadCommon.getNumberOfPieces() <= pieceIndex || 0 > pieceIndex)
        {
            System.out.print("ERROR: FileWriter.java hasPiece() --- pieceIndex " + pieceIndex +" is not a valid piece number.\n");
            System.exit(1);
        }

        synchronized(bitfieldLock)
        {
            //else return the boolean value of the bitfield, for the piece specified
            return this.bitfield.get(pieceIndex);
        }
    }
    public byte[] getBitfieldAsBytes()
    {
        synchronized(bitfieldLock)
        {
            //BitSet's valueOf() method converts a Bitfield to a little-endian byte array
            return this.bitfield.toByteArray();
        }
    }
    public void setBitfieldFromBytes(byte[] bitfieldAsBytes)
    {
        synchronized(bitfieldLock)
        {
            //BitSet's valueOf() method converts a little-endian byte array to a BitField
            this.bitfield = BitSet.valueOf(bitfieldAsBytes);
        }
    }
    public void setBitfieldPieceAsTrue(int pieceIndex)
    {
        //error-check: check if the piece index is within bounds
        if(ReadCommon.getNumberOfPieces() <= pieceIndex || 0 > pieceIndex)
        {
            System.out.print("ERROR: FileWriter.java setBitFieldPieceAsTrue() --- pieceIndex " + pieceIndex +" is not a valid piece number.\n");
            System.exit(1);
        }
        
        synchronized(bitfieldLock)
        {
            //else set the bitfield to true, for the specified piece
            //Bitset's set() method changes the indicated index to the second parameter's value
            this.bitfield.set(pieceIndex, true);
        }
    }
    public int isCurrentlyRequested(int pieceIndex)
    {
        //error-check: check if the piece index is within bounds
        if(ReadCommon.getNumberOfPieces() <= pieceIndex || 0 > pieceIndex)
        {
            System.out.print("ERROR: FileWriter.java isCurrentlyRequested() --- pieceIndex " + pieceIndex +" is not a valid piece number.\n");
            System.exit(1);
        }

        synchronized(requestedLock)
        {
 
            //else return whether the piece is currently already requested
            return this.requested[pieceIndex];
        }
    }
    public void setAsRequested(int pieceIndex, int peerIdRequestedFrom)
    {
        //error-check: check if the piece index is within bounds
        if(ReadCommon.getNumberOfPieces() <= pieceIndex || 0 > pieceIndex)
        {
            System.out.print("ERROR: FileWriter.java setAsRequested() --- pieceIndex " + pieceIndex +" is not a valid piece number.\n");
            System.exit(1);
        }
        
        synchronized(requestedLock)
        {
            //set the piece as requested
            this.requested[pieceIndex] = peerIdRequestedFrom;
        }
    }
    public void clearRequested(int peerIdRequestedFrom)
    {
        synchronized(requestedLock)
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
    }
    public boolean getInterested()
    {
        synchronized(interestedLock)
        {
            return this.interested;
        }
    }
    public void setInterested(boolean interested)
    {
        synchronized(interestedLock)
        {
            this.interested = interested;
        }
    }
}
