package project.threads;

import java.io.IOException;

import project.channels.MCChannel;

public class SendRemoved implements Runnable{

    private MCChannel mc;
    private String fileID;
    private int chunkID;

    public SendRemoved(MCChannel mc, String fileID, int chunkID) {
        this.mc = mc;
        this.fileID = fileID;
        this.chunkID = chunkID;
    }

    @Override
    public void run() {
        try {
            this.mc.sendRemoved(fileID, chunkID);
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

}