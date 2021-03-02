# CNT 5106C "Computer Networks" - Spring 2021
## Group Project - P2P File Sharing

- Lavanya Khular
- Chitranshu Raj
- Benjamin Hsu


### Instructions On How To Compile
1) In shell, go to root folder (should contain a "src" and "build" sub-folders when in correct directory).

2) Compile with "javac -d build src/*.java src/modules/*.java"

3) Run with "java -cp build src.MainTest" (MainTest is just a simple test main method file)
Note that the Configuration Files should be located in the root folder.


### Instruction On How To Run The "Main" Program "PeerProcess"
1) Follow compilation steps as above

2) Run the PeerProcess for each peer, in a separate shell window, in descending order (top to bottom) of the listings of PeerProcess.cfg --- for example if the peers are listed as peerId 1000, 1001, 1002 then run, in order:
"java -cp build src.PeerProcess 1000"
"java -cp build src.PeerProcess 1001"
"java -cp build src.PeerProcess 1002"

## ----------
## ----------

### Description of Files & Folders at "root" level
#### README.md
This is the GitHub documentation file that you are reading currently.

#### Common.cfg
Holds the info about what file is being transferred, and how that file should be divided into pieces. Also has info on number of peers and choking interval.

#### PeerInfo.cfg
Holds the info on peers: peerID number, peer's server port number, name of the peer's address, and whether the peer has the complete file to begin with.

#### testfile.txt
A temporary 52-byte test file that is used to test the file transfer with. Contents are "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".

#### "src" folder
Holds the .java files that collectively compose the program.

#### "build" folder
This holds the executable program .class files that are compiled from the "src" folder's .java files.

## ----------

### Description of Files & Folders at "src" folder level
#### MainTest.java
A temporary "main method"-type file that simply calls and outputs certain intermediary functions from the other .java files, for debugging purposes.

#### PeerProcess.java
The actual release "main method"-type file. This is the file that will be called from the command-line to start the program. Receives its peerId from the command-line arguments, then reads the Common.cfg and PeerInfo.cfg (using methods defined in other .java files). Then, creates TCP connections to the peer processes above it, and receives TCP connections from peer process below it. And then creates listening threads that handle these TCP connections. Finally, has a loop that handles the sending of data to these TCP connections.

#### "modules" folder
Basically contains all the .java files that are not the "main-method" .java file.

## ----------

### Description of Files at "modules" folder level
#### FileWriter.java
Both reads from and writes to the file that is being transferred in between the peers. Data of the file is read or written piece-by-piece. The constructor will first check if the peer's subfolder exists, and will make it if not. Then it will check if the P2P file is in the folder. If it is not, the P2P file of the size specified in Common.cfg will be created (with initially null/undefined byte contents). Now, a pointer to the pre-existing or new P2P file is created and can be used to read and write from it.

Constructor:
- FileWriter(int peerId)

Class methods of note:
- byte[] readPiece(int pieceIndex)
- void writePiece(int pieceIndex, byte[] pieceContents)

#### LogWriter.java
Writes to the log file of the process. The class constructor first checks if the log file exists, and if not, creates it. Different methods are used to write the log messages specified by the project's specifications.

Constructor:
- LogWriter(int peerId)

Class methods of note:
- void logStartConnection(int otherPeerId)
- void logChangePreferredNeighbors(PeerObject[] preferredNeighbors)
- void logChangeOptimisticNeighbor(PeerObject optimisticNeighbors)
- void logUnchoked(int otherPeerId)
- void logChoked(int otherPeerId)
- void logHave(int otherPeerId, int pieceIndex)
- void logInterested(int otherPeerId)
- void logNotInterested(int otherPeerId)
- void logDownload(int otherPeerId, int pieceIndex, int numberOfCollectedPieces)
- void logComplete()

#### ReadCommon.java
Reads the Common.cfg file, and has methods that will return the values from that file. This particular .java file reads the file in a static{} block, so no instantiation of a ReadCommon() object is required. Instead, just use "ReadCommon.method_name()" to get info.

Class methods of note:
- static int getNumberOfPreferredNeighbors()
- static int getUnchokingInterval()
- static int getOptimisticUnchokingInterval()
- static String getFileName()
- static int getFileSize()
- static int getPieceSize()
- static int getNumberOfPieces()

#### ReadPeerInfo.java
Reads the PeerInfo.cfg file, and creates a bunch of PeerObject objects (from the PeerObject.java file) and populates its respective info (e.g. peerId, serverInfo, etc.) for each of the peers.

Class methods of note:
- List<PeerObject> getPeersInfo()
- int getPeerIndexPosition(int peerId)

#### PeerObject.java
"Intermediate" .java file, where each object instance of PeerObject is used to store info about each of the peers.

Constructor:
- PeerObject(int peerId, String hostName, int portNumber, boolean hasFile)

Class methods of note:
- int getPeerId()
- String getHostName()
- int getPortNumber()
- boolean getHasFile()
- getSocket()
- setSocket(Socket socket)
- void addBytesDownloadedFrom(int bytesDownloaded)
- void resetBytesDownloadedFrom()
- int getBytesDownloadedFrom()
- boolean hasPiece(int pieceIndex)
- byte[] getBitfieldAsBytes()
- void setBitfieldFromBytes(byte[] bitfieldAsBytes)
- void setBitfieldPieceAsTrue(int pieceIndex)
- boolean getInterested()
- void setInterested(boolean interested)

#### ServerThread.java
Called as a new thread in the PeerProcess.java file. This ServerThread is called for each of the adjacent peer TCP connections of the particular peer that the PeerProcess is called for. As the name implies, this ServerThread acts to receive incoming messages from those TCP connections.

Class methods of note:
