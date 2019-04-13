package project.database;

import project.service.Peer;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Storage implements java.io.Serializable {
   
    private static final long serialVersionUID = 1L;

    private long capacityAvailable;
    private long maxCapacity;
    private ConcurrentHashMap<AbstractMap.SimpleEntry<String,Integer>, Chunk> storedChunks;
    private ArrayList<FileManager> storedFiles;
    //private ConcurrentHashMap<String, byte[]> restoredChunks;
    //private ConcurrentHashMap<String, Integer> reclaimedChunks;

    public Storage() {
        maxCapacity = 1000000000;
        capacityAvailable = maxCapacity;
        storedChunks = new ConcurrentHashMap<AbstractMap.SimpleEntry<String,Integer>, Chunk>();
        storedFiles = new ArrayList<FileManager>();
        //restoredChunks = new ConcurrentHashMap<String, byte[]>();
        //reclaimedChunks = new ConcurrentHashMap<String, Integer>();
    }

    public synchronized void incSpaceAvailable(long length) {
        this.capacityAvailable += length;
    }

    public synchronized void decSpaceAvailable(long length) {
        this.capacityAvailable -= length;
    }

    public synchronized long getSpaceAvailable() {
        return this.capacityAvailable;
    }

    public synchronized void setSpaceAvailable(long spaceAvailable) {
        this.capacityAvailable = spaceAvailable;
    }

    public synchronized void setMaxCapacity(long capacity) {
        this.maxCapacity = capacity;
    }

    public synchronized long getMaxCapacity() {
        return this.maxCapacity;
    }

    public ArrayList<FileManager> getStoredFiles() {
        return this.storedFiles;
    }

    public ConcurrentHashMap<AbstractMap.SimpleEntry<String,Integer>, Chunk> getStoredChunks() {
        return this.storedChunks;
    }

    public synchronized boolean storeChunk(AbstractMap.SimpleEntry<String, Integer> key, Chunk chunk, int peerID, boolean onStore) {
        if(this.storedChunks.containsKey(key))
            return false;

        this.storedChunks.put(key, chunk);
        if(onStore) this.storedChunks.get(key).addStorer(peerID);
        return true;
    }

    public synchronized boolean containsChunk(AbstractMap.SimpleEntry<String, Integer> key) {
        if(this.storedChunks.containsKey(key))
            return true;
        return false;
    }

    public synchronized void deleteChunk(AbstractMap.SimpleEntry<String, Integer> key, int peerID) throws IOException {
        if(this.storedChunks.containsKey(key) && this.storedChunks.get(key).isStored(peerID))
        {
            this.incSpaceAvailable(this.storedChunks.get(key).getSize());
            this.storedChunks.get(key).deleteChunk(peerID);
        }
    }

    public synchronized void deleteFileSent(String fileID) {
        for (Map.Entry<AbstractMap.SimpleEntry<String, Integer>, Chunk> entry : this.getStoredChunks().entrySet()) {
            if(entry.getKey().getKey().equals(fileID)) {
                this.storedChunks.get(entry.getKey()).eraseStorers();
            }
        }
    }

    public void addFileManager(FileManager fm) {
        this.storedFiles.add(fm);
    }
}