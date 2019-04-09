package project.threads;

import java.io.IOException;

import project.channels.MCChannel;

public class SendGetChunk implements Runnable{

    private MCChannel mc;
    private String fileID;
    private int chunkID;

    public SendGetChunk(MCChannel mc, String fileID, int chunkID) {
        this.mc = mc;
        this.fileID = fileID;
        this.chunkID = chunkID;
    }

    @Override
    public void run() {
        try {
            System.out.println(" - Get " + this.fileID + "." + this.chunkID);
            this.mc.sendGetChunk(fileID, chunkID);
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }


}