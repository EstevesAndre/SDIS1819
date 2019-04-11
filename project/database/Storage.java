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

    private int capacity;
    private Peer peer;
    private ConcurrentHashMap<Map.Entry<String,Integer>, Chunk> storedChunks;
    private ArrayList<FileManager> storedFiles;
    private ConcurrentHashMap<String, byte[]> restoredChunks;
    private ConcurrentHashMap<String, Integer> reclaimedChunks;
    private ConcurrentHashMap<String, HashSet<Integer>> storedMessages;

    public Storage(Peer peer) {
        capacity = 1000000000;
        this.peer = peer;
        storedChunks = new ConcurrentHashMap<Map.Entry<String,Integer>, Chunk>();
        storedFiles = new ArrayList<FileManager>();
        restoredChunks = new ConcurrentHashMap<String, byte[]>();
        reclaimedChunks = new ConcurrentHashMap<String, Integer>();
        storedMessages = new ConcurrentHashMap<String, HashSet<Integer>>();
    }

    public synchronized void incSpaceAvailable(int length) {
        this.capacity += length;
    }

    public synchronized void decSpaceAvailable(int length) {
        this.capacity -= length;
    }

    public synchronized int getSpaceAvailable() {
        return this.capacity;
    }

    public ArrayList<FileManager> getStoredFiles() {
        return this.storedFiles;
    }

    public ConcurrentHashMap<Map.Entry<String,Integer>, Chunk> getStoredChunks() {
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
        this.incSpaceAvailable(this.storedChunks.get(key).getSize());
        this.storedChunks.get(key).deleteChunk(peerID);
        this.storedChunks.remove(key);
    }

    public void addFileManager(FileManager fm) {
        this.storedFiles.add(fm);
    }
}