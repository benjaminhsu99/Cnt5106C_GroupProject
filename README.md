# CNT 5106C "Computer Networks" - Spring 2021
## Group Project - P2P File Sharing

- Benjamin Hsu
- Lavanya Khular
- Chitranshu Raj


### Instructions On How To Compile
1) In shell, go to root folder (should contain a "src" and "build" sub-folders when in correct directory).

2) Compile with "javac -d build src/*.java src/modules/*.java"

3) Run with "java -cp build src.PeerProcess PEERID"



### Instruction On How To Run The "Main" Program "PeerProcess"
1) Follow compilation steps as above.
Note that the Configuration Files (Common.cfg and PeerInfo.cfg) should be located in the aforementioned root folder.
Also, the particular peer process's subfolder (e.g. "peer_1111") should also be in this root folder.

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

#### "src" folder
Holds the .java files that collectively compose the program.

#### "build" folder
This holds the executable program .class files that are compiled from the "src" folder's .java files.

## ----------

### Description of Files & Folders at "src" folder level
#### PeerProcess.java
The actual release "main method"-type file. This is the file that will be called from the command-line to start the program. Receives its peerId from the command-line arguments, then reads the Common.cfg and PeerInfo.cfg (using methods defined in other .java files).

A PeerObject.java class object is created for each of the peers. This PeerObject class object is the "glue" that holds together the entirety of the project. This PeerObject class object is used to store state-information (such as interested status, and file bitfield info) and is shared by the PeerObject.java process and its spawned ClientThread.java threads.

PeerProcess.java will then create TCP connections to the peer processes above it, and receive TCP connections from peer process below it. Next, a ServerThread and a ClientThread pair of threads are created for each of the neighboring peers (so if there are 5 peers total, then there are 4 neighbors, so 8 threads total are created).

In a while loop that lasts until the aforementioned ClientThread and ServerThread threads finish, PeerProcess.java will then continuously maintain timers to change preferred and optimistic neighbors. If a timer expires, PeerProcess.java will choose new peers to be unchoked (where the unchoked state info is held in the aforementioned PeerObject class objects). Because PeerProcess.java needs to read the Peer Object class objects - which is also simulataneously used and modified by the ClientThread threads - a synchronization lock is used to ensure that PeerProcess.java accesses and modifies the PeerObject info without ClientThread threads' interference.

PeerProcess.java also acts as the intermediary communication link for when a file piece is obtained by any of the ClientThreads. When such a file piece is obtained, a "have piece" message needs to be sent to all the neighbors. However, the single particular ClientThread that obtained the piece only has TCP socket info for one of the neighbor peers. Thus, that ClientThread tells the PeerProcess.java to signal to all of its spawned ClientThreads to for them to each send a have message to the respective neighbor peer that the ClientThread is handling.

#### "modules" folder
Basically contains all the .java files that are not the "main-method" PeerProcess.java file.

## ----------

### Description of Files at "modules" folder level
#### ReadCommon.java
Reads the Common.cfg file, and has methods that will return the values from that file. Unlike all the other .java files in this project, ReadCommon.java is all written in a static{} block, so no instantiation of a ReadCommon() object is required. Instead, just use "ReadCommon.method_name()" to get info, instead of having to create a "new ReadCommon()" instance as would be normal for the other .java files.

#### ReadPeerInfo.java
Reads the PeerInfo.cfg file, and creates a bunch of PeerObject objects (from the PeerObject.java file) and populates its respective info (e.g. peerId, serverInfo, etc.) for each of the peers. Unlike the ReadCommon.java file, this ReadPeerInfo.java is "normal" in that it has to first be instantiated as an new object in order to use it's "get" methods to retrieve info from the PeerInfo.cfg file.

#### FileWriter.java
Both writes to and reads from (despite being called "FileWriter") the file that is being transferred in between the peers. The constructor will first check if the peer's subfolder (e.g. "peer_1111") exists, and will make it if not. Then it will check if the P2P file (e.g. "file.txt") is in the folder. If it is not, the P2P file of the size specified in Common.cfg will be created (with initially null/undefined byte contents). Finally, a file reading/writer pointer to the file (whether pre-existing or newly created by the constructor) is used to read and write file pieces.

#### LogWriter.java
Writes to the log file (e.g. "log_peer_1111.log") of the process. The class constructor first checks if the log file exists, and if not, creates it. If the log file was already pre-existing for some reason, new entires are appended to the old contents (not overriden). Differently named methods (e.g. "log_complete()") are used to write the log messages specified by the project's specifications.

#### PeerObject.java
"Intermediate" .java file, where each object instance of PeerObject is used to store info about each of the peers. As described in the previous description for PeerProcess.java:

A PeerObject.java class object is created (by PeerProcess.java) for each of the peers. This PeerObject class object is the "glue" that holds together the entirety of the project. This PeerObject class object is used to store state-information (such as interested status, and file bitfield info) and is shared by the PeerObject.java process and its spawned ClientThread.java threads.

Many of PeerObject.java's state information is kept in different variables representing the "my" and "neighbor's" view. For example, the "myChoked" variable represents if the peer that the program is running for is choking the neighbor. On the other hand, the "neighborChoked" variable represents if the neighbor is choking the peer.

Different state variables are read and changed by different processes or functions. Specifically, each ClientThread will have access to the same "myPeer" PeerObject object instance. This is the PeerObject for the peer that the PeerProcess program was started for. On the other hand, each ClientThread will have a different "neighborPeer" PeerObject instance, since each ClientThread is for sending TCP messages to one neighboring peer.

That said, the "myPeer" PeerObject would maintain the bitfield for the peer that PeerProcess.java was started for. On the other hand, the "neighborPeer" PeerObject's bitfield would represent what the myPeer (the peer that the PeerProcess was started for) thinks the neighbor's bitfield is - which would be built up gradually via the initial bitfield received from the neighbor and from subsequent "has piece" messages. The "true" bitfield of the neighbor is in an entirely different computer's PeerProcess process.

Because "myPeer" is shared by different ClientThread threads, any ClientThread methods accessing it are synchronized with a "clientThreadsLock" lock, ensuring that there are no race conditions caused simultaneous access by the threads.

#### ServerThread.java
A ServerThread thread is spawned by PeerProcess.java for each of the adjacent peer TCP connections of the particular peer that the PeerProcess is called for. As the name implies, this ServerThread acts to receive incoming messages from those TCP connections.

As mentioned previously, peer state information is maintained in PeerObject.java object instances. Instead of directly accessing and modifying these PeerObject objects, ServerThread.java instead "passes" any modifying actions to its ClientThread.java partner thread. This was chosen so as to avoid having to deal with locking ServerThread and ClientThread threads from simulatenously accessing the PeerObject objects.

Instead, by delegating the modification actions to the ClientThreads, the ServerThreads are "removed" from the competition that accesses the PeerObject objects. This way, only the PeerProcess.java and multiple ClientThreads are competing with accessing the PeerObject information.

Delegation is performed with a thread-safe "ArrayBlockingQueue". Messages such as "write this received file piece into the file" are passed by pushing that message into the ClientThread thread's queue, which the ClientThread will then pop out, analyze the message, and perform the indicated action.

#### ThreadMessage.java
This is the intermediary message class objects that are used by ServerThread.java threads to tell ClientThread.java threads to do actions regarding modifying the PeerObject instances.

PeerProcess.java also uses this to tell its multiple ClientThread thread spawns to send choke/unchoke messages to their respective handled neighbor peers. In addition, PeerProcess.java also uses this to "relay" orders for the ClientThreads to send "have piece" messages, as described in the description for PeerProcess.java .

#### ClientThread.java
The second-most important file, behind PeerProcess.java. ClientThread threads handle sending TCP information to their assigned neighbor peer. Besides that, ClientThreads are also the delegated thread to handle any changing of state information held in PeerObject.java objects. As described in the ServerThread.java description, such tasks are delegated from the ServerThread threads.

PeerProcess.java also accesses and changes state information held in the PeerObject.java objects, so a synchronization lock is maintain between the ClientThread thread and the PeerProcess.java process, so as to ensure that they do not access the information at the same time (and cause race conditions). Because there are multiple ClientThread threads, there is also a second lock to ensure that ClientThread threads do not cause race conditions when they access the "myPeer" PeerObject object instance, which is used by all ClientThread threads.

The ClientThread thread will operate on a while loop, only terminating when both "myPeer" and it assigned "neighborPeer" have the complete file. There are also other additional minor conditions imposed in this while loop - for example, ensuring that the ClientThread thread has sent a final "choke" message.

In the while loop, the ClientThread thread will first check if it has any messages received from either its ServerThread.java thread partner, or from the main PeerProcess.java process. These messages are the ones mentioned previously, that command the ClientThread to process received data into the PeerObject objects, or to send choke/unchoke messages, etc.

Otherwise, if there are no unprocessed messages, the ClientThread will evaluate if it can request any pieces from its neighbors. Conditions such as being unchoked by the neighbor, and if the neighbor has a missing piece are taken into account. The ClientThread was also designed to only request one piece at a time from its neighbor, and only if that particular piece is not currently requested by an other ClientThread thread.

Because there is a chance for the neighbor to change its unchoked peers, after the ClientThread thread has sent a request, ClientThread.java is designed to "clear" its memory of any request to that neighbor peer, if it receives a "choke" message (received by its partner ServerThread thread) from the neighbor peer. This allows other ClientThread thread instances to request that piece.