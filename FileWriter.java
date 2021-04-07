/*
CNT 5105C "Computer Networks" - Spring 2021
Group Project - P2P File Sharing
Benjamin Hsu, Lavanya Khular, Chitranshu Raj
*/

//Imports
import java.io.*; //Exceptions, RandomAccessFile
import java.nio.file.*; //Files, Path
import java.lang.*; //Integer

public class FileWriter
{
    //parameters
    private RandomAccessFile theFile;

    //object constructor
    public FileWriter(int peerId)
    {
        //project specifications states that file should be stored in a "peer_[peer_id]" sub-folder
        String fileName = "peer_" + Integer.toString(peerId) + "\\" + ReadCommon.getFileName();

        try
        {
            //convert the file's string name into a Path type object
            Path fileNamePath = Paths.get(fileName);

            //if the subfolder does not currently exist
            if(Files.notExists(fileNamePath.getParent()))
            {
                //create the subfolder
                Files.createDirectory(fileNamePath.getParent());
            }

            //if the file does not currently exist
            if(Files.notExists(fileNamePath))
            {
                //create the file
                Files.createFile(fileNamePath);

                //set the RandomAccessFile pointer
                this.theFile = new RandomAccessFile(fileNamePath.toString(), "rw");

                //set the length of the file (should be undefined contents according to the documentation)
                this.theFile.setLength(ReadCommon.getFileSize());
            }
            //else just set the file pointer to the already-existing file
            else
            {
                //set the RandomAccessFile pointer
                this.theFile = new RandomAccessFile(fileNamePath.toString(), "rw");
            }
        }
        catch(FileNotFoundException exception)
        {
            System.out.print("ERROR: FileWriter.java Constructor --- RandomAccessFile() couldn't find the file (it should have already been created?).\n");
            exception.printStackTrace();
            System.exit(1);
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: FileWriter.java Constructor --- some IO error.\n");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    //Methods
    public byte[] readPiece(int pieceIndex)
    {
        //if the requested piece index (starting counting at 0) is greater than the number of pieces that exists (or a negative number), then print an error and exit
        if(ReadCommon.getNumberOfPieces() <= pieceIndex || 0 > pieceIndex)
        {
            System.out.print("ERROR: FileWriter.java --- Piece index # " + pieceIndex + " was requested but this exceeds the max number of pieces --- or the number was negative.\n");
            System.exit(1);
        }

        //bytes container to store the file's read-in contents
        byte[] extractedBytes;

        //if the piece is the last one (which means it may be shorter than other pieces)
        if(ReadCommon.getNumberOfPieces() == pieceIndex + 1)
        {
            //since the last piece may be shorter than the piece size, calculate its actual size and make an array to store the file-read contents
            int sizeOfLastPiece = ReadCommon.getFileSize() - (ReadCommon.getNumberOfPieces() - 1) * ReadCommon.getPieceSize();
            extractedBytes = new byte[sizeOfLastPiece];
        }
        //otherwise, this is normal-sized piece
        else
        {
            //make array to store a normal piece size
            extractedBytes = new byte[ReadCommon.getPieceSize()];
        }

        //read the piece contents
        try
        {
            //set the file-reading pointer to start at the piece's beginning index
            this.theFile.seek(pieceIndex * ReadCommon.getPieceSize());
            //read the piece
            this.theFile.read(extractedBytes);
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: FileWriter.java getPiece() --- some IO error.\n");
            exception.printStackTrace();
            System.exit(1);
        }

        //return the found piece file contents
        return extractedBytes;
    }

    public void writePiece(int pieceIndex, byte[] pieceContents)
    {
        //if the requested piece index (starting counting at 0) is greater than the number of pieces that exists (or a negative number), then print an error and exit
        if(ReadCommon.getNumberOfPieces() <= pieceIndex || 0 > pieceIndex)
        {
            System.out.print("ERROR: FileWriter.java --- Piece index # " + pieceIndex + " was requested but this exceeds the max number of pieces --- or the number was negative.\n");
            System.exit(1);
        }

        //check if the bytes array is the right size that is expected
        //if the piece is the last one (which may have a shorter piece size than normal)
        if(ReadCommon.getNumberOfPieces() == pieceIndex + 1)
        {
            //since the last piece may be shorter than the piece size, calculate its actual size and make an array to store the file-read contents
            int sizeOfLastPiece = ReadCommon.getFileSize() - (ReadCommon.getNumberOfPieces() - 1) * ReadCommon.getPieceSize();

            //check if the input byte[] array is the right size
            if(pieceContents.length != sizeOfLastPiece)
            {
                System.out.print("ERROR: FileWriter.java writePiece() --- Piece index # " + pieceIndex + " expected size " + sizeOfLastPiece + " but is actually " + pieceContents.length + " .\n");
                System.exit(1);
            }
        }
        //otherwise, this is normal-sized piece
        else
        {
            //check if the input byte[] array is the right size
            if(pieceContents.length != ReadCommon.getPieceSize())
            {
                System.out.print("ERROR: FileWriter.java writePiece() --- Piece index # " + pieceIndex + " expected size " + ReadCommon.getPieceSize() + " but is actually " + pieceContents.length + " .\n");
                System.exit(1);
            }
        }

        //write the piece contents
        try
        {
            //set the file-reading pointer to start at the piece's beginning index
            this.theFile.seek(pieceIndex * ReadCommon.getPieceSize());
            //read the piece
            this.theFile.write(pieceContents);
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: FileWriter.java writePiece() --- some IO error.\n");
            exception.printStackTrace();
            System.exit(1);
        }
    }
}
