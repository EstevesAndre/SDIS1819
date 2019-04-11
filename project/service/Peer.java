package project.service;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import project.channels.MCChannel;
import project.channels.MDBChannel;
import project.channels.MDRChannel;
import project.database.Chunk;
import project.database.FileManager;
import project.database.InitiatedChunk;
import project.database.Storage;
import project.rmi.RemoteInterface;
import project.threads.SendGetChunk;
import project.threads.SendPutChunk;
import project.threads.SendRemoved;
import project.threads.SendChunk;

import java.rmi.registry.Registry;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;

public class Peer implements RemoteInterface, Remote {

    private short peerID;
    private float version;
    private String accessPoint;
    private boolean restoring;
    private int numberchunks;

    private MCChannel MCchannel;
    private MDBChannel MDBchannel;
    private MDRChannel MDRchannel;

    private Storage storage;

    private ConcurrentHashMap<Map.Entry<String,Integer>, Chunk> backedUpChunks;
    private ConcurrentHashMap<Map.Entry<String,Integer>, InitiatedChunk> initiatedChunks;
    private ConcurrentHashMap<String,Integer> storedFiles;
    private ConcurrentHashMap<Map.Entry<String,Integer>, byte[]> restoredFile;

    private ThreadPoolExecutor executor;

    public Peer(String version, String serverID, String accessPoint, String MCAddr, String MDBAddr, String MDRAddr) throws Exception {
        
        System.setProperty("java.net.preferIPv4Stack", "true");

        this.peerID = Short.parseShort(serverID);
        this.version = Float.parseFloat(version);
        this.accessPoint = accessPoint;

        this.MCchannel = new MCChannel(MCAddr, this);
        this.MDBchannel = new MDBChannel(MDBAddr, this);
        this.MDRchannel = new MDRChannel(MDRAddr, this);

        this.storage = new Storage(this);
        
        this.storedFiles = new ConcurrentHashMap<String,Integer>();
        this.backedUpChunks = new ConcurrentHashMap<Map.Entry<String,Integer>, Chunk>();
        this.initiatedChunks = new ConcurrentHashMap<Map.Entry<String,Integer>, InitiatedChunk>();
        this.restoring = false;
        this.restoredFile = new ConcurrentHashMap<Map.Entry<String,Integer>, byte[]>();

        this.joinRMI();
    }

    public void joinRMI() throws Exception {
        
        try {
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(this, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(this.accessPoint, stub);

			System.out.println("\nPeer ready\n");
        } 
        catch (Exception e) {
            System.err.println("ERROR --> " + this.getClass() + ": Failed to initiate RMI\n" + 
                                "      --> Exception: " + e.toString());
            //e.printStackTrace();
            System.exit(-2);
        }
    }

    public short getID() {
        return this.peerID;
    }

    public float getVersion() {
        return this.version;
    }

    public ThreadPoolExecutor getExec() {
        return this.executor;
    }

    public boolean hasInitiatedChunk(Map.Entry<String, Integer> chunk) {
        return this.initiatedChunks.containsKey(chunk);
    }

    public void receiveGetChunk(String[] message) throws IOException {
        // ex: GETCHUNK version senderID fileID chunkID

        if(this.peerID == Integer.parseInt(message[2]))
            return;

        String fileID = message[3];
        int chunkID = Integer.parseInt(message[4]);
        
        AbstractMap.SimpleEntry<String, Integer> key = new AbstractMap.SimpleEntry<String, Integer>(fileID, chunkID);
        
        if(this.storage.containsChunk(key))
        {
            String fileName = String.format("%s.%03d", fileID, chunkID);
            File chunkFile = new File(this.peerID + "/backup/" + fileName);
            FileInputStream in = new FileInputStream(chunkFile);

            byte[] buffer = new byte[(int) chunkFile.length()];
            in.read(buffer);
            
            Chunk chunk = new Chunk(fileID, chunkID, buffer, buffer.length);

            this.executor.execute((Runnable) new SendChunk(this.MDRchannel, fileID, chunk));
        }
    }

    public void receiveChunk(byte[] receivePacket) throws IOException {
        if(this.restoring)
        {
            int splitIndex = 0;
            for(int i = 0; i < receivePacket.length; i++) {
                if(receivePacket[i] == 13)
                {
                    splitIndex = i + 4;
                    break;
                }
            }

            byte[] chunkByte = Arrays.copyOfRange(receivePacket, splitIndex, receivePacket.length);

            String[] header = new String(receivePacket, 0, receivePacket.length).split("\r\n\r\n")[0].split("\\s+");

            String fileID = header[3];
            Integer chunkID = Integer.parseInt(header[4]);
            System.out.println("receive Chunk " + fileID + " " + chunkID);
            AbstractMap.SimpleEntry<String, Integer> key = new AbstractMap.SimpleEntry<String, Integer>(fileID, chunkID);

            if(!this.restoredFile.containsKey(key))
                this.restoredFile.put(key, chunkByte);
            
            if(this.numberchunks == this.restoredFile.size())
            {
                this.restoring = false;
                FileManager.restoreFile(String.valueOf(peerID), fileID, this.restoredFile);
                this.restoredFile = null;
            }
        }
    }

    public void receivePutChunk(byte[] receivePacket) throws IOException {

        int splitIndex = 0;
        for(int i = 0; i < receivePacket.length; i++) {
            if(receivePacket[i] == 13)
            {
                splitIndex = i + 4;
                break;
            }
        }

        byte[] chunkByte = Arrays.copyOfRange(receivePacket, splitIndex, receivePacket.length);
        String[] received = new String(receivePacket, 0, splitIndex).trim().split("\\s+");

        // if peer is reading its own message
        if(Integer.parseInt(received[2]) == this.peerID) {
            return;
        }
        System.out.println("receivePutChunk " + received[4] + " " + chunkByte.length);
        
        String fileID = received[3];
        Integer chunkID = Integer.parseInt(received[4]);
        System.out.println("receivePutChunk " + chunkID + " " + chunkByte.length);
        
        if(this.storage.getSpaceAvailable() >= chunkByte.length)
        {
            int rd = Integer.parseInt(received[5]);
            AbstractMap.SimpleEntry<String, Integer> key = new AbstractMap.SimpleEntry<String, Integer>(fileID, chunkID);
            
            Chunk chunk = new Chunk(fileID, chunkID, chunkByte, chunkByte.length, rd, 0);
            if(this.storage.storeChunk(key, chunk)) {
                this.storage.decSpaceAvailable(chunk.getSize());
                chunk.storeChunk(peerID);
            }

            this.MCchannel.sendStored(fileID, chunkID);
        }
        else
            System.err.println("No space available");
    }

    public void receiveStored(String[] message) {
        
        int chunkId = Integer.parseInt(message[4]);
        int sender = Integer.parseInt(message[2]);
        String fileId = message[3];

        for(int i = 0; i < this.storage.getStoredFiles().size(); i++) {
            if(this.storage.getStoredFiles().get(i).getFileID().equals(fileId)) {
                for(int j = 0; j < this.storage.getStoredFiles().get(i).getChunks().size(); j++) {
                    if(this.storage.getStoredFiles().get(i).getChunks().get(j).getId() == chunkId) {
                        this.storage.getStoredFiles().get(i).getChunks().get(j).addStorer(sender);
                    }
                }
            }
        }
        
        // verifies if is not the same peer
        if(sender == this.peerID)
            return;

        //System.out.println("Received stored " + fileId + " " + chunkId);
        
        InitiatedChunk initiated = this.initiatedChunks.get(new AbstractMap.SimpleEntry<String, Integer>(fileId, chunkId));
        if(initiated != null) {
            initiated.addStorer(sender);
        }

        AbstractMap.SimpleEntry<String, Integer> chunk = new AbstractMap.SimpleEntry<String, Integer>(fileId, chunkId);
            
        if(this.storage.containsChunk(chunk)) {
            this.storage.getStoredChunks().get(chunk).addStorer(sender);
        }
    }

    public void receiveDelete(String[] message) throws IOException {
        
        if(Integer.parseInt(message[2]) == this.peerID)
            return;

        String fileId = message[3];

        int chunkId = this.storage.getStoredChunks().size();

        //if(this.storedFiles.containsKey(fileId))
        //    chunkId = this.storedFiles.get(fileId);
        
        AbstractMap.SimpleEntry<String, Integer> key = new AbstractMap.SimpleEntry<String, Integer>(fileId, chunkId);
        for(int i = 0; i <= chunkId; i++) {
            key = new AbstractMap.SimpleEntry<String, Integer>(fileId, i);
            if(this.storage.containsChunk(key)) {
                this.storage.deleteChunk(key, this.peerID);
            }
        }

        if(this.initiatedChunks.containsKey(key)){
            this.initiatedChunks.remove(key);
        }
    }

    public void receiveRemoved(String[] message) {
        if(Integer.parseInt(message[2]) == this.peerID) {
            return;
        }

        System.out.println("Received removed " + message[3] + message[4]);
    }

    public boolean verifyRDInitiated(Map.Entry<String, Integer> chunk) {
        InitiatedChunk init = this.initiatedChunks.get(chunk);
        if(init != null) {
            return ((init.getObservedRD() >= init.getDesiredRD()? true : false));
        }
        return false;
    }

    public long getFolderSize(File dir) {
        if(!dir.isDirectory()) {
            return 0;
        }

        long size = 0;
        for (File file : dir.listFiles()) {
            if (file.isFile()) {
                System.out.println(file.getName() + " " + file.length());
                size += file.length();
            }
            else if(dir.isDirectory()){
                size += getFolderSize(file);
            }
        }
        return size;
    }    

    // generating file id with SHA-256
    public String getFileHashID(String path) throws Exception {
        File file = new File(path);
        Path p = Paths.get(file.getAbsolutePath());
        
        BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        String toHash = file.getName() + attr.creationTime().toString() + attr.lastModifiedTime().toString();
        
        return FileManager.bytesToHex(digest.digest(toHash.getBytes(StandardCharsets.UTF_8)));
    }

    public String getFileName (String hash) {
        return "";
    }

    public static void main(String[] args) throws Exception {
        
        // example initiator peer: java project/service/Peer 1.0 1234 RemoteInterface "230.0.0.0 9876" "230.0.0.1 9877" "230.0.0.2 9878"
        // example peer: java project/service/Peer 1.0 444 RemoteInterface2 "230.0.0.0 9876" "230.0.0.1 9877" "230.0.0.2 9878"
        // example peer: java project/service/Peer 1.0 555 RemoteInterface3 "230.0.0.0 9876" "230.0.0.1 9877" "230.0.0.2 9878"
        // example peer: java project/service/Peer 1.0 666 RemoteInterface4 "230.0.0.0 9876" "230.0.0.1 9877" "230.0.0.2 9878"
        if(args.length != 6)
		{
            System.out.println("Wrong number of arguments.\nUsage: java project/Peer <version> <serverID> <accessPoint> \"<MC_address> <MC_port>\" \"<MDB_address> <MDB_port>\" \"<MDR_address> <MDR_port>\"\r\n");
            
			System.exit(-1);
        }

        Peer peer = new Peer(args[0], args[1], args[2], args[3], args[4], args[5]);
        peer.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(200);
        peer.executor.execute(peer.MDBchannel);
        peer.executor.execute(peer.MCchannel);
        peer.executor.execute(peer.MDRchannel);
    }

    @Override
    public void backupOperation(ArrayList<String> info) throws Exception {

        if(info.size() != 2)
        {
            System.out.println("Wrong number of arguments for BACKUP operation\n");
			return;
        }
        
        String path = info.get(0);
        int rd = Integer.parseInt(info.get(1));
        System.out.println("Received the following request: - BACKUP " + path + " rd - " + rd);
        String fileID = getFileHashID(path);

        this.storage.addFileManager(new FileManager(fileID, path, rd));
        ArrayList<Chunk> chunks = FileManager.splitFile(path);

        for(int i = 0; i < chunks.size(); i++){

            for(int j = 0; j < this.storage.getStoredFiles().size(); j++) {
                if(this.storage.getStoredFiles().get(j).getFileID().equals(fileID)) {
                    this.storage.getStoredFiles().get(j).getChunks().add(chunks.get(i));
                }
            }

            InitiatedChunk initiatedChunk = new InitiatedChunk(rd);
            this.initiatedChunks.put(new AbstractMap.SimpleEntry<String, Integer>(fileID, i), initiatedChunk);
            this.executor.execute(new SendPutChunk(this.MDBchannel, fileID, chunks.get(i), rd));
            Thread.sleep(100);
        }
    }

    @Override
    public void restoreOperation(ArrayList<String> info) throws Exception {
        
        if(info.size() != 1)
        {
            System.out.println("Wrong number of arguments for RESTORE operation\n");
			return;
        }

        String fileID = getFileHashID(info.get(0));

        for(int i = 0; i < this.storage.getStoredFiles().size(); i++) {
            if(this.storage.getStoredFiles().get(i).getFileID().equals(fileID)) {

                int nChunks = this.storage.getStoredFiles().get(i).getChunks().size();
                System.out.println("nChunks: " + nChunks);
                this.restoredFile = new ConcurrentHashMap<Map.Entry<String,Integer>, byte[]>();
                this.restoring = true;
                this.numberchunks = nChunks + 1;

                for(int j = 0; j <= nChunks; j++) // chunks ids -> [0,nChunks];
                {
                    this.executor.execute(new SendGetChunk(this.MCchannel, fileID, j));
                    Thread.sleep(150);
                }
                break;
            }
        }
    }
    
    @Override
    public void deleteOperation(ArrayList<String> info) throws Exception {
        if(info.size() != 1)
        {
            System.out.println("Wrong number of arguments for DELETE operation\n");
			return;
        }
         
        String path = info.get(0);

        System.out.println("Received the following request: - DELETE " + path);  

        String fileID = getFileHashID(path);
        System.out.println("File id: " + fileID);

        this.MCchannel.sendDelete(fileID);
    }
    
    @Override
    public void reclaimOperation(ArrayList<String> info) {
        long diskSpacePermitted = Long.parseLong(info.get(0));
        this.storage.setMaxCapacity(diskSpacePermitted);

        File backup = new File("peer" + this.peerID + "/backup/");
        long folderSize = getFolderSize(backup);
        this.storage.setSpaceAvailable(diskSpacePermitted - folderSize);

        if(this.storage.getSpaceAvailable() > 0){
            return;
        }

        ConcurrentHashMap<AbstractMap.SimpleEntry<String,Integer>, Chunk> stored = this.storage.getStoredChunks();
        for (Map.Entry<AbstractMap.SimpleEntry<String, Integer>, Chunk> entry : stored.entrySet()) {
            AbstractMap.SimpleEntry<String, Integer> key = entry.getKey();
            Chunk chunk = entry.getValue();
            this.storage.deleteChunk(key, this.peerID);
            this.executor.execute(new SendRemoved(this.MCchannel, key.getKey(), key.getValue()));
            if(this.storage.getSpaceAvailable() > 0){
                return;
            }
        }

    }

    @Override
    public void stateOperation(ArrayList<String> info) {
        if(info.size() != 0) {
            System.out.println("Wrong number of arguments for STATE operation\n");
			return;
        }

        System.out.println("Received the following request: - STATE");  

        System.out.println("------------------------------------------------------");
        System.out.println(" For each file whose backup it has initiated:");
        for(int i = 0; i < this.storage.getStoredFiles().size(); i++) {
            System.out.println(" - The file pathname: " + this.storage.getStoredFiles().get(i).getPath());
            System.out.println(" - The backup service id of the file: " + this.storage.getStoredFiles().get(i).getFileID());
            System.out.println(" - The desired replication degree: " + this.storage.getStoredFiles().get(i).getDRD());
            System.out.println(" - For each chunk of the file:");
            for(int j = 0; j < this.storage.getStoredFiles().get(i).getChunks().size(); j++) {
                System.out.println("   - Its id: " + this.storage.getStoredFiles().get(i).getChunks().get(j).getId());
                System.out.println("   - Its perceived replication degree: " + this.storage.getStoredFiles().get(i).getChunks().get(j).getObservedRD());
            }
        }
        System.out.println(" For each chunk it stores:");
        for(int i = 0; i < this.storage.getStoredChunks().size(); i++) {

        }
    }
}