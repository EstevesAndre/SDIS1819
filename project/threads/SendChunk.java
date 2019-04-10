package project.threads;

import java.io.IOException;

import project.channels.MDRChannel;
import project.database.Chunk;

public class SendChunk implements Runnable {

    private MDRChannel mdr;
    private String fileID;
    private Chunk chunk;

    public SendChunk(MDRChannel mdr, String fileID, Chunk chunk) {
        this.mdr = mdr;
        this.fileID = fileID;
        this.chunk = chunk;
    }

    @Override
    public void run() {
        try {
            this.mdr.sendChunk(fileID, chunk);
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }


}