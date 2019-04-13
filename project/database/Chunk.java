package project.database;

import java.net.*;
import java.util.HashSet;
import java.io.*;

public class Chunk implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

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

    public synchronized void storeChunk(int peerID) throws IOException {

        this.fileName = String.format("chk%d", this.id);
        File newFile = new File("peer" + peerID + "/backup/" + this.fileID + "/" + this.fileName);
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

    public synchronized void eraseStorers() {
        this.observedRD -= this.storers.size();
        this.storers = new HashSet<Integer>();
    }

    public boolean isStored(int peerID) {
        return this.storers.contains(peerID);
    }

    public synchronized void deleteChunk(int peerID) throws IOException {
        this.observedRD--;
        this.storers.remove(peerID);
        this.fileName = String.format("chk%d", this.id); 
        File file = new File("peer" + peerID + "/backup/" + this.fileID + "/" + this.fileName);
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

    public synchronized void addStorer(int storer) {
        if(storers.add(storer)) {
            observedRD++;
        }
    }

    public HashSet<Integer> getStorers() {
        return storers;
    }

    public synchronized void deleteStorer(int storer) {
        if(storers.remove(storer)){
            observedRD--;
        }
    }

    public int getObservedRD(){
        return observedRD;
    }

    public int getDesiredRD(){
        return desiredRD;
    }
}