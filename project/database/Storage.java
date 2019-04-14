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

    private ConcurrentHashMap<AbstractMap.SimpleEntry<String, Integer>, InitiatedChunk> initiatedChunks;
    private ConcurrentHashMap<AbstractMap.SimpleEntry<String,Integer>, Chunk> storedChunks;
    private ConcurrentHashMap<AbstractMap.SimpleEntry<String, Integer>, byte[]> restoredChunks;
    private HashSet<String> deletedFiles;
    private ArrayList<FileManager> storedFiles;

    public Storage() {
        maxCapacity = 1000000000;
        capacityAvailable = maxCapacity;
        
        initiatedChunks = new ConcurrentHashMap<AbstractMap.SimpleEntry<String, Integer>, InitiatedChunk>();
        storedChunks = new ConcurrentHashMap<AbstractMap.SimpleEntry<String,Integer>, Chunk>();
        restoredChunks = new ConcurrentHashMap<AbstractMap.SimpleEntry<String, Integer>, byte[]>();
        deletedFiles = new HashSet<String>();

        storedFiles = new ArrayList<FileManager>();
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

    public synchronized ConcurrentHashMap<AbstractMap.SimpleEntry<String, Integer>, Chunk> getStoredChunks() {
        return this.storedChunks;
    }

    public synchronized ConcurrentHashMap<AbstractMap.SimpleEntry<String, Integer>, byte[]> getRestoredChunks() {
        return this.restoredChunks;
    }

    public synchronized Chunk getStoredChunk(AbstractMap.SimpleEntry<String,Integer> key) {
        return this.storedChunks.get(key); // Chunk or null
    }

    public synchronized boolean storeChunk(AbstractMap.SimpleEntry<String, Integer> key, Chunk chunk, int peerID) throws IOException {
        if(this.storedChunks.containsKey(key))
        {
            if(this.storedChunks.get(key).wasDeleted()) {
                this.storedChunks.get(key).storeChunk(peerID);
                this.storedChunks.get(key).addStorer(peerID);
                return true;
            }
            else return false;
        }
        
        this.storedChunks.put(key, chunk);
        this.storedChunks.get(key).addStorer(peerID);
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
            this.storedChunks.remove(key);
        }
    }

    public synchronized void deleteFileSent(String fileID) {
        for (Map.Entry<AbstractMap.SimpleEntry<String, Integer>, Chunk> entry : this.getStoredChunks().entrySet()) {
            if(entry.getKey().getKey().equals(fileID)) {
                this.storedChunks.get(entry.getKey()).eraseStorers();
            }
        }
    }

    public synchronized String getFileName(String fileID) {
        for(FileManager fm : this.storedFiles) {
            if(fm.getFileID().equals(fileID)) {
                String path = fm.getPath();
                while(path.charAt(0) == '.' || path.charAt(0) == '/') {
                    path = path.substring(1);
                }
                return path;
            }
        }

        return "NotFound.txt";
    }

    public synchronized void addFileManager(FileManager newFM) {
        boolean found = false;
        for(FileManager fm : this.storedFiles) {
            if(fm.getFileID().equals(newFM.getFileID())) {
                fm.setRD(newFM.getDRD());
                for (Map.Entry<AbstractMap.SimpleEntry<String, Integer>, Chunk> entry : this.getStoredChunks().entrySet()) {
                    if(entry.getKey().getKey().equals(fm.getFileID())) {
                        this.storedChunks.get(entry.getKey()).setRD(newFM.getDRD());
                    }
                }
                found = true;
            }
        }
        if(!found) this.storedFiles.add(newFM);
    }

    public synchronized void addRestoredChunk(AbstractMap.SimpleEntry<String, Integer> key, byte[] content) {
        if(!this.restoredChunks.containsKey(key)) {
            this.restoredChunks.put(key, content);
        }
    }

    public synchronized int getNrChunks(String fileID) {
        int nrChunks = 0;

        for(FileManager fm : this.storedFiles) {
            if(fm.getFileID().equals(fileID)) {
                nrChunks = fm.getChunkNr();
                break;
            }
        }
        
        return nrChunks;
    }

    public boolean isrestorePossible(String fileID) {
        int nrChunks = 0;

        for (AbstractMap.SimpleEntry<String, Integer> key : this.getRestoredChunks().keySet()) {
            if(key.getKey().equals(fileID)) {
                nrChunks++;
            }
        }

        return getNrChunks(fileID) == nrChunks;
    }

    public void deleteRestoreChunks(String fileID) {
        for (AbstractMap.SimpleEntry<String, Integer> key : this.getRestoredChunks().keySet()) {
            if(key.getKey().equals(fileID))
                this.getRestoredChunks().remove(key);
        }
    }

    public synchronized boolean hasInitiatedChunk(String fileID) {
        for(FileManager fm : this.storedFiles) {
            if(fm.getFileID().equals(fileID)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean hasChunkStored(AbstractMap.SimpleEntry<String,Integer> key) {
        return this.storedChunks.containsKey(key);
    }

    public synchronized boolean hasInitiatedChunk(AbstractMap.SimpleEntry<String,Integer> key) {
        return this.initiatedChunks.containsKey(key);
    }

    public synchronized void initiateChunk(AbstractMap.SimpleEntry<String,Integer> key, int desiredRD) {
        InitiatedChunk initiated = new InitiatedChunk(desiredRD);
        this.initiatedChunks.put(key, initiated);
        
        if(this.deletedFiles.contains(key.getKey())) {
            this.deletedFiles.remove(key.getKey());
        }
    }

    public synchronized void removeInitiatedChunk(AbstractMap.SimpleEntry<String,Integer> key) {
        this.initiatedChunks.remove(key);
    }

    public synchronized InitiatedChunk getInitiatedChunk(AbstractMap.SimpleEntry<String,Integer> key) {
        return this.initiatedChunks.get(key); // InitiatedChunk or null
    }

    public synchronized ConcurrentHashMap<AbstractMap.SimpleEntry<String, Integer>, InitiatedChunk> getInitiatedChunks() {
        return this.initiatedChunks;
    }

    public synchronized boolean verifyRDInitiated(AbstractMap.SimpleEntry<String, Integer> key) {
        InitiatedChunk initiated = this.initiatedChunks.get(key);
        if(initiated != null) {
            return ((initiated.getDesiredRD() <= initiated.getObservedRD())? true : false);
        }

        return false;
    }

    public HashSet<String> getDeletedFiles() {
        return deletedFiles;
    }

    public void addDeletedFile(String fileId) {
        deletedFiles.add(fileId);
    }
}