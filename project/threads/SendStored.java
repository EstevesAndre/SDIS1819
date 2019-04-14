package project.threads;

import java.io.IOException;

import project.channels.MCChannel;
import project.channels.MDBChannel;
import project.database.Chunk;

public class SendStored implements Runnable{

    private MCChannel mc;
    private String fileId;
    private int chunkId;

    public SendStored(MCChannel mc, String fileId, int chunkId) {
        this.mc = mc;
        this.fileId = fileId;
        this.chunkId = chunkId;
    }

    @Override
    public void run() {
        try {
            this.mc.sendStored(fileId, chunkId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //catch (InterruptedException e) {}
        catch(Exception e){
            e.printStackTrace();
        }

    }

}