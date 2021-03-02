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
    private boolean hasFile;
    private Socket socket;
    private volatile int bytesDownloadedFrom;
    private volatile BitSet bitfield;
    private volatile boolean interested;

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
            System.out.print("ERROR: FileWriter.java getNumberOfPieces() --- pieceIndex " + pieceIndex +" is not a valid piece number.\n");
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
    public void setBitfieldFromBytes(byte[] bitfieldAsBytes)
    {
        //BitSet's valueOf() method converts a little-endian byte array to a BitField
        this.bitfield = BitSet.valueOf(bitfieldAsBytes);
    }
    public void setBitfieldPieceAsTrue(int pieceIndex)
    {
        //error-check: check if the piece index is within bounds
        if(ReadCommon.getNumberOfPieces() <= pieceIndex || 0 > pieceIndex)
        {
            System.out.print("ERROR: FileWriter.java getNumberOfPieces() --- pieceIndex " + pieceIndex +" is not a valid piece number.\n");
            System.exit(1);
        }

        //else set the bitfield to true, for the specified piece
        //Bitset's set() method changes the indicated index to the second parameter's value
        this.bitfield.set(pieceIndex, true);
    }
    public boolean getInterested()
    {
        return this.interested;
    }
    public void setInterested(boolean interested)
    {
        this.interested = interested;
    }
}
