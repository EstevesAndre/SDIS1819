package project.channels;

import java.lang.Runnable;
import java.util.concurrent.TimeUnit;

import project.service.Peer;

public class MCChannel extends Channel implements Runnable{

    public MCChannel(String MCCAddr, Peer peer) throws Exception {
        super(MCCAddr, peer);
    }

    public boolean sendStored(String messageType, String fileID, int chunkNumber, int replicationDegree)
    {
        String message = super.createHeader(messageType, fileID, chunkNumber, replicationDegree);
        
        return true;
    }

    public void sendMessage(byte[] msg) {
        
    }

    @Override
    public void run() {
        try {
            while(true) {
                //Long duration = (long) (Math.random() * 10);
                //System.out.println("Executing: MCC");
                TimeUnit.SECONDS.sleep(2);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}