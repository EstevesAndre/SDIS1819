package project.threads;

import java.io.IOException;

import project.channels.MDBChannel;
import project.database.Chunk;

public class SendPutChunk implements Runnable{

    private MDBChannel mdb;
    private String fileID;
    private Chunk chunk;
    private int rd;

    public SendPutChunk(MDBChannel mdb, String fileID, Chunk chunk, int rd) {
        this.mdb = mdb;
        this.fileID = fileID;
        this.chunk = new Chunk(chunk.getFileId(), chunk.getId(), chunk.getContent(), chunk.getSize(), rd);
        this.rd = rd;
    }

    @Override
    public void run() {
        try {
            this.mdb.sendPutChunk(fileID, chunk, rd);
            this.mdb.verifyRDinitiated(fileID, chunk, rd);
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch(InterruptedException e) {}
        catch(Exception e){
            e.printStackTrace();
        }

    }


}