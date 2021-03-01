/*
CNT 5105C "Computer Networks" - Spring 2021
Group Project - P2P File Sharing
Benjamin Hsu, Lavanya Khular, Chitranshu Raj
*/

//Created package to group together all the various .java files together
package src.modules;

//Imports
import java.io.*; //Exceptions, File, BufferedReader
import java.util.*; //List, ArrayList, Iterator
import java.nio.file.*; //Files, Path
import java.lang.*; //Math

public class ReadCommon
{
    //names of the file to be read
    private static final String commonFileName = "Common.cfg";

    //Common.cfg parameters
    private static final int numberOfPreferredNeighbors;
    private static final int unchokingInterval;
    private static final int optimisticUnchokingInterval;
    private static final String fileName;
    private static final int fileSize;
    private static final int pieceSize;
    private static final int numberOfPieces;
    private static List<String> commonFileLines;

    //Static block that reads the Common.cfg file
    static
    {
        //first read in the file
        try
        {
            //convert the file's string name into a Path type object
            Path commonFilePath = Paths.get(commonFileName);

            //read the file's lines in entirety first
            commonFileLines = Files.readAllLines(commonFilePath);

            //check if for some reason a line is just whitespace (e.g. just a newline), if so - remove it
            for(Iterator<String> iterator = commonFileLines.iterator(); iterator.hasNext(); )
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
            System.out.print("ERROR: ReadCommon.java --- Common.cfg was not found.\n");
            exception.printStackTrace();
            System.exit(1);
        }
        catch(IOException exception)
        {
            System.out.print("ERROR: ReadCommon.java --- some IO error for Common.cfg .\n");
            exception.printStackTrace();
            System.exit(1);
        }

        //check if the read-in lines have the appropriate number of lines
        if(null == commonFileLines || 6 > commonFileLines.size())
        {
            System.out.print("ERROR: ReadCommon.java --- Common.cfg has less than 6 lines or was not read properly.\n");
            System.exit(1);
        }

        //temp variables to workaround the inability to assign "final" variables in try-catch such as below
        int tempNumberOfPreferredNeighbors = -1;
        int tempUnchokingInterval = -1;
        int tempOptimisticUnchokingInterval = -1;
        String tempFileName = "";
        int tempFileSize = -1;
        int tempPieceSize = -1;
        //Extract the parameters' values from the Common.cfg read-in lines
        try
        {
            tempNumberOfPreferredNeighbors = Integer.parseInt(commonFileLines.get(0).trim().split(" ")[1]);
            tempUnchokingInterval = Integer.parseInt(commonFileLines.get(1).trim().split(" ")[1]);
            tempOptimisticUnchokingInterval = Integer.parseInt(commonFileLines.get(2).trim().split(" ")[1]);
            tempFileName = commonFileLines.get(3).substring(commonFileLines.get(3).indexOf(' ') + 1).trim();
            tempFileSize = Integer.parseInt(commonFileLines.get(4).trim().split(" ")[1]);
            tempPieceSize = Integer.parseInt(commonFileLines.get(5).trim().split(" ")[1]);
        }
        catch(NumberFormatException exception)
        {
            System.out.print("ERROR: ReadCommon.java --- some error processing Common.cfg Strings into ints.\n");
            exception.printStackTrace();
            System.exit(1);
        }
        catch(StringIndexOutOfBoundsException exception)
        {
            System.out.print("ERROR: ReadCommon.java --- some error processing Common.cfg String into fileName.\n");
            exception.printStackTrace();
            System.exit(1);
        }

        //assign the Common.cfg parameters, but also check if they are valid values
        if(0 > tempNumberOfPreferredNeighbors)
        {
            System.out.print("ERROR: ReadCommon.java --- Common.cfg's numberOfPreferredNeighbors was not read as a valid value.\n");
            System.exit(1);
        }
        if(0 >= tempUnchokingInterval)
        {
            System.out.print("ERROR: ReadCommon.java --- Common.cfg's unchokingInterval was not read as a valid value.\n");
            System.exit(1);
        }
        if(0 >= tempOptimisticUnchokingInterval)
        {
            System.out.print("ERROR: ReadCommon.java --- Common.cfg's optimisticUnchokingInterval was not read as a valid value.\n");
            System.exit(1);
        }
        if(tempFileName.isEmpty())
        {
            System.out.print("ERROR: ReadCommon.java --- Common.cfg's fileName was not read as a valid value.\n");
            System.exit(1);
        }
        if(0 >= tempFileSize)
        {
            System.out.print("ERROR: ReadCommon.java --- Common.cfg's fileSize was not read as a valid value.\n");
            System.exit(1);
        }
        if(-1 == tempPieceSize)
        {
            System.out.print("ERROR: ReadCommon.java --- Common.cfg's pieceSize was not read as a valid value.\n");
            System.exit(1);
        }
        numberOfPreferredNeighbors = tempNumberOfPreferredNeighbors;
        unchokingInterval = tempUnchokingInterval;
        optimisticUnchokingInterval = tempOptimisticUnchokingInterval;
        fileName = tempFileName;
        fileSize = tempFileSize;
        pieceSize = tempPieceSize;
        
        //calculate the number of pieces, rounding up to the nearest int
        numberOfPieces = (int)Math.ceil((double)fileSize / pieceSize);
    }

    //Common.cfg public accessor methods
    public static int getNumberOfPreferredNeighbors()
    {
        return numberOfPreferredNeighbors;
    }
    public static int getUnchokingInterval()
    {
        return unchokingInterval;
    }
    public static int getOptimisticUnchokingInterval()
    {
        return optimisticUnchokingInterval;
    }
    public static String getFileName()
    {
        return fileName;
    }
    public static int getFileSize()
    {
        return fileSize;
    }
    public static int getPieceSize()
    {
        return pieceSize;
    }
    public static int getNumberOfPieces()
    {
        return numberOfPieces;
    }
}
