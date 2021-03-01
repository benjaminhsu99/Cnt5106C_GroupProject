/*
CNT 5105C "Computer Networks" - Spring 2021
Group Project - P2P File Sharing
Benjamin Hsu, Lavanya Khular, Chitranshu Raj
*/

//Created package to group together all the various .java files together
package src.modules;

public class ServerThread extends Thread
{
    private PeerObject peer;
    private int threadName;

    public ServerThread(PeerObject peer, int threadName)
    {
        this.peer = peer;
        this.threadName = threadName;
    }

    public void run()
    {
        
//THREAD TEST TO SEE IF DIFFERENT THREADS CAN USE THE SAME COMMON VARIABLE AND MODIFY IT TOGETHER (TESTS = THEY CAN)
        for(int i = 0; i < 20; i++)
        {
            if(threadName == 0)
                peer.addBytesDownloadedFrom(1);
            else
                peer.addBytesDownloadedFrom(5);
            
            System.out.print("I'm thread " + threadName + " hur dur the sum is: " + peer.getBytesDownloadedFrom() + "\n");;
        }
    }
}