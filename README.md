# CNT 5106C "Computer Networks" - Spring 2021
## Group Project

- Lavanya Khular
- Chitranshu Raj
- Benjamin Hsu


#### To Compile
1) In shell, go to root folder (should contain a "src" and "build" sub-folders when in correct directory).

2) Compile with "javac -d build src/*.java src/modules/*.java"

3) Run with "java -cp build src.MainTest"
Note that the Configuration Files should be located in the root folder.


#### To Run The Main Program "PeerProcess"
1) Follow compilation steps as above

2) Run the PeerProcess for each peer, in a separate shell window, in descending order (top to bottom) of the listings of PeerProcess.cfg --- for example if the peers are listed as peerId 1000, 1001, 1002 then run, in order:
"java -cp build src.PeerProcess 1000"
"java -cp build src.PeerProcess 1001"
"java -cp build src.PeerProcess 1002"