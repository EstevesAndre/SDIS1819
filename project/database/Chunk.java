package project.database;

import java.net.*;
import java.util.HashSet;
import java.io.*;

public class Chunk {
    private int id;
    private String fileID;
    private String fileName;
    private int size;
    private byte[] content; // restore

    private int observedRD;
    private int desiredRD;

    private HashSet<Integer> storers; // id's of the peers that backed up the chunk

    public Chunk(int id, byte[] content, int size) {
        this.id = id;
        this.content = content;
        this.size = size;
        this.observedRD = 0;
        storers =  new HashSet<Integer>();
    }

    public Chunk(String fileID, int id, byte[] content, int size) {
        this.fileID = fileID;
        this.id = id;
        this.content = content;
        this.size = size;
        this.observedRD = 0;

        storers =  new HashSet<Integer>();
    }

    public Chunk(String fileID, int id, byte[] content, int size, int rd, int observedRD) {
        this.fileID = fileID;
        this.id = id;
        this.content = content;
        this.size = size;
        this.desiredRD = rd;
        this.observedRD = observedRD;

        storers =  new HashSet<Integer>();
    }

    public void storeChunk(int peerID) throws IOException {
        this.fileName = String.format("%s.%03d", this.fileID, this.id);
        File newFile = new File(peerID + "/backup/" + this.fileName);
        newFile.getParentFile().mkdirs();

        try (FileOutputStream out = new FileOutputStream(newFile)) {
            out.write(this.content, 0, this.size);
        }
    }

    public int getSize() {
        return size;
    }

    public byte[] getChunk(String fileID, int id) {
        if(this.fileID.equals(fileID) && this.id == id)
            return this.content;
        
        return null;
    }

    public void setData(byte[] data) {
        this.content = data;
    }

    public void eraseData() {
        this.content = null;
    }

    public boolean isStored(int peerID) {
        return this.storers.contains(peerID);
    }

    public void deleteChunk(int peerID) throws IOException {
        this.observedRD--;
        this.storers.remove(peerID);
        File file = new File(peerID + "/backup/" + this.fileName);
        file.delete();
    }

    public int getId(){
        return this.id;
    }

    public String getFileId() {
        return this.fileID;
    }

    public byte[] getContent() {
        return this.content;
    }

    public void setReplicationDegree(int newRD) {
        this.desiredRD = newRD;
    }

    public void addStorer(int storer) {
        if(storers.add(storer)) {
            observedRD++;
        }
    }

    public int getObservedRD(){
        return observedRD;
    }

}