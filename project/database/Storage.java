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
    private ConcurrentHashMap<Map.Entry<String,Integer>, Chunk> storedChunks;
    private ArrayList<FileManager> storedFiles;
    private ConcurrentHashMap<String, byte[]> restoredChunks;
    private ConcurrentHashMap<String, Integer> reclaimedChunks;

    public Storage(Peer peer) {
        maxCapacity = 1000000000;
        capacityAvailable = maxCapacity;
        storedChunks = new ConcurrentHashMap<Map.Entry<String,Integer>, Chunk>();
        storedFiles = new ArrayList<FileManager>();
        restoredChunks = new ConcurrentHashMap<String, byte[]>();
        reclaimedChunks = new ConcurrentHashMap<String, Integer>();
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

    public synchronized boolean storeChunk(AbstractMap.SimpleEntry<String, Integer> key, Chunk chunk) {
        if(this.storedChunks.containsKey(key))
            return false;

        this.storedChunks.put(key, chunk);
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

    public void addFileManager(FileManager fm) {
        this.storedFiles.add(fm);
    }
}