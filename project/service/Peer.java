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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;

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
import project.threads.SendStored;
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

    private MCChannel MCchannel;
    private MDBChannel MDBchannel;
    private MDRChannel MDRchannel;

    private Storage storage;
    
    private ConcurrentHashMap<AbstractMap.SimpleEntry<String, Integer>, ScheduledFuture> waitingOnPutchunks;
    private ConcurrentHashMap<AbstractMap.SimpleEntry<String, Integer>, ScheduledFuture> waitingOnStoreds;

    private ThreadPoolExecutor executor;
    private ScheduledThreadPoolExecutor scheduledExecutor;

    public Peer(String version, String serverID, String accessPoint, String MCAddr, String MDBAddr, String MDRAddr) throws Exception {

        System.setProperty("java.net.preferIPv4Stack", "true");

        this.peerID = Short.parseShort(serverID);
        this.version = Float.parseFloat(version);
        this.accessPoint = accessPoint;

        this.MCchannel = new MCChannel(MCAddr, this);
        this.MDBchannel = new MDBChannel(MDBAddr, this);
        this.MDRchannel = new MDRChannel(MDRAddr, this);
        
        this.restoring = false;

        this.scheduledExecutor = new ScheduledThreadPoolExecutor(5);

        this.waitingOnPutchunks = new ConcurrentHashMap<AbstractMap.SimpleEntry<String, Integer>, ScheduledFuture>();
        this.waitingOnStoreds = new ConcurrentHashMap<AbstractMap.SimpleEntry<String, Integer>, ScheduledFuture>();

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

    public void receiveJoined() throws IOException {
        if(Float.compare(this.version, 1.0F) == 0) {
            return;
        }

        HashSet<String> deletedFiles = this.storage.getDeletedFiles();

        for(String fileId : deletedFiles) {
            this.MCchannel.sendDelete(fileId);
        }
    }

    public void receiveGetChunk(String[] message) throws IOException {
        // ex: GETCHUNK version senderID fileID chunkID

        if(this.peerID == Integer.parseInt(message[2]))
            return;

        String fileID = message[3];
        int chunkID = Integer.parseInt(message[4]);
        
        AbstractMap.SimpleEntry<String, Integer> key = new AbstractMap.SimpleEntry<String, Integer>(fileID, chunkID);
        
        System.out.println("Received getChunk " + fileID + " " + chunkID);
        
        if(this.storage.containsChunk(key) && this.storage.getStoredChunk(key).getObservedRD() > 0)
        {
            String fileName = String.format("chk%d", chunkID);        
            File chunkFile = new File("peer" + this.peerID + "/backup/" + fileID + "/" + fileName);

            if(!chunkFile.exists()) {
                return;
            }
            
            FileInputStream in = new FileInputStream(chunkFile);

            byte[] buffer = new byte[(int) chunkFile.length()];
            in.read(buffer);
            in.close();

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
            System.out.println("Received Chunk " + fileID + " " + chunkID);
            AbstractMap.SimpleEntry<String, Integer> key = new AbstractMap.SimpleEntry<String, Integer>(fileID, chunkID);

            this.storage.addRestoredChunk(key, chunkByte);

            if(this.restoring && this.storage.isrestorePossible(fileID)) {
                this.restoring = false;
                System.out.println("Doing restore of the file " + fileID);
                FileManager.restoreFile("peer" + String.valueOf(peerID) + "/restored/" + this.storage.getFileName(fileID), fileID, this.storage.getNrChunks(fileID), this.storage.getRestoredChunks());
                this.storage.deleteRestoreChunks(fileID);
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
        
        String fileID = received[3];
        Integer chunkID = Integer.parseInt(received[4]);
        System.out.println("Received putChunk " + chunkID + " " + chunkByte.length);
        
        AbstractMap.SimpleEntry<String, Integer> key = new AbstractMap.SimpleEntry<String, Integer>(fileID, chunkID);
        
        if(this.waitingOnPutchunks.containsKey(key)) {
            this.waitingOnPutchunks.get(key).cancel(true);
            this.waitingOnPutchunks.remove(key);
            this.storage.removeInitiatedChunk(key);
            Peer.savesInfoStorage(this, this.storage);
        }

        if(this.storage.hasInitiatedChunk(fileID)) {return;} // a peer shouldn't store its own files

        int delay = ThreadLocalRandom.current().nextInt(400);
        SendStored sendStored = new SendStored(this.MCchannel, fileID, chunkID);
        if(this.storage.hasChunkStored(key)) {
            this.scheduledExecutor.schedule(sendStored, delay, TimeUnit.MILLISECONDS);
            return;
        }

        if(this.storage.getSpaceAvailable() >= chunkByte.length)
        {
            int rd = Integer.parseInt(received[5]);
            
            Chunk chunk = new Chunk(fileID, chunkID, chunkByte, chunkByte.length, rd);
            if(this.storage.storeChunk(key, chunk, this.peerID)) {
                this.storage.decSpaceAvailable(chunk.getSize());
                chunk.storeChunk(peerID);
                Peer.savesInfoStorage(this, this.storage);
            }

            this.waitingOnStoreds.put(key, this.scheduledExecutor.schedule(sendStored, delay, TimeUnit.MILLISECONDS));
        }
        else
            System.err.println("No space available");
    }

    public void receiveStored(String[] message) throws IOException {
        
        int sender = Integer.parseInt(message[2]);
        
        String fileID = message[3];
        int chunkID = Integer.parseInt(message[4]);

        AbstractMap.SimpleEntry<String, Integer> key = new AbstractMap.SimpleEntry<String, Integer>(fileID, chunkID);

        // verifies if is not the same peer
        if(sender == this.peerID) {
            if(this.waitingOnStoreds.containsKey(key)) {
                this.waitingOnStoreds.remove(key);
            }
            return;
        }
        
        System.out.println("Received stored " + fileID + " " + chunkID);

        Chunk chunk = this.storage.getStoredChunk(key);
        if(chunk != null) {
            chunk.addStorer(sender);

            ScheduledFuture storeChunk = this.waitingOnStoreds.get(key);
            if(Float.compare(this.version, 1.0F) != 0 && storeChunk != null) { // enhanced version
                if(chunk.getObservedRD() - 1 >= chunk.getDesiredRD()) {
                    storeChunk.cancel(true);
                    this.storage.deleteChunk(key, this.peerID);                    
                }
            }
        }

        InitiatedChunk initiated = this.storage.getInitiatedChunk(key);
        if(initiated != null) {
            initiated.addStorer(sender);
        }

        Peer.savesInfoStorage(this, this.storage);
    }

    public void receiveDelete(String[] message) throws IOException {

        if(Integer.parseInt(message[2]) == this.peerID)
            return;

        String fileID = message[3];
        
        for(Map.Entry<String, Integer> key : this.storage.getStoredChunks().keySet()) {
            if(key.getKey().equals(fileID)) {
                this.storage.deleteChunk(new AbstractMap.SimpleEntry<String, Integer>(fileID, key.getValue()), this.peerID);
                System.out.println("Received delete " + fileID + " " + key.getValue());
            }
        }
        Peer.savesInfoStorage(this, this.storage);
    }

    public void receiveRemoved(String[] message) {
        int sender = Integer.parseInt(message[2]);
        if(sender == this.peerID) {
            return;
        }
        
        String fileId = message[3];
        int chunkId = Integer.parseInt(message[4]);

        AbstractMap.SimpleEntry<String, Integer> key = new AbstractMap.SimpleEntry<String, Integer>(fileId, chunkId);
        Chunk chunk = this.storage.getStoredChunk(key);

        if(chunk == null) {return;}
        int rd = chunk.getDesiredRD();

        chunk.deleteStorer(sender);
        if(chunk.getObservedRD() >= rd) { // replication degree remains achieved
            return;
        }

        this.storage.initiateChunk(key, rd);
        int delay = ThreadLocalRandom.current().nextInt(400);
        SendPutChunk runnable = new SendPutChunk(this.MDBchannel, fileId, chunk, rd);
        this.waitingOnPutchunks.put(key, this.scheduledExecutor.schedule(runnable, delay, TimeUnit.MILLISECONDS));
    }

    public boolean hasInitiatedChunk(String fileID) {
        return this.storage.hasInitiatedChunk(fileID);
    }

    public boolean hasInitiatedChunk(AbstractMap.SimpleEntry<String, Integer> chunk) {
        return this.storage.hasInitiatedChunk(chunk);
    }

    public boolean verifyRDInitiated(AbstractMap.SimpleEntry<String, Integer> chunk) {
        return this.storage.verifyRDInitiated(chunk);
    }

    public long getFolderSize(File dir) {
        if(!dir.isDirectory()) {
            return 0;
        }

        long size = 0;
        for (File file : dir.listFiles()) {
            if (file.isFile()) {
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

    public static void savesInfoStorage(Peer peer, Storage store) {
        try {
            File storageFile = new File("peer" + peer.getID() + "/storage.ser");
            
            if (!storageFile.exists()) {
                storageFile.getParentFile().mkdirs();
                storageFile.createNewFile();
            }

            FileOutputStream outFile = new FileOutputStream("peer" + peer.getID() + "/storage.ser");
            ObjectOutputStream output = new ObjectOutputStream(outFile);
            output.writeObject(store);
            output.close();
            outFile.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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

        try {   
            FileInputStream fis = new FileInputStream("peer" + peer.peerID + "/storage.ser");
            ObjectInputStream input = new ObjectInputStream(fis);
            peer.storage = (Storage) input.readObject();
            input.close();
            fis.close();
            
            if(Float.compare(peer.version, 1.0F) != 0) {
                peer.MCchannel.sendJoined();
            }
            
        }
        catch (Exception e) {
            peer.storage = new Storage();
        }
    }

    @Override
    public String backupOperation(ArrayList<String> info) throws Exception {
        if(Float.compare(this.version, 1.0F) != 0) {
            System.out.println("Test App version not compatible with Peer version\n");
            return "Test App version not compatible with Peer version";
        }

        if(info.size() != 2) {
            System.out.println("Wrong number of arguments for BACKUP operation\n");
            return " - Wrong number of arguments for BACKUP operation";
        }
        
        String path = info.get(0);
        int rd = Integer.parseInt(info.get(1));
        System.out.println("Received the following request: - BACKUP " + path + " rd - " + rd);

        return backup(path, rd);
    }

    public String backup(String path, int rd) throws Exception  {
        String fileID = getFileHashID(path);
        
        ArrayList<Chunk> chunks = FileManager.splitFile(fileID, path, rd);
        this.storage.addFileManager(new FileManager(fileID, path, rd, chunks.size()));
    
        for(int i = 0; i < chunks.size(); i++){
    
            AbstractMap.SimpleEntry<String, Integer> key = new AbstractMap.SimpleEntry<String, Integer>(fileID, chunks.get(i).getId());
            this.storage.initiateChunk(key, rd);
            
            Peer.savesInfoStorage(this, this.storage);

            this.executor.execute(new SendPutChunk(this.MDBchannel, fileID, chunks.get(i), rd));
            Thread.sleep(100);
        }

        return null;
    }
    
    @Override
    public void backupEnhOperation(ArrayList<String> info) throws Exception {
        if(Float.compare(this.version, 1.0F) == 0) {
            System.out.println("Test App version not compatible with Peer version");
            return;
        }

        if(info.size() != 2) {
            System.out.println("Wrong number of arguments for BACKUP operation\n");
            return;
        }
        
        String path = info.get(0);
        int rd = Integer.parseInt(info.get(1));
        System.out.println("Received the following request: - BACKUPENH " + path + " rd - " + rd);

        backup(path, rd);
    }

    @Override
    public String restoreOperation(ArrayList<String> info) throws Exception {
        
        if(info.size() != 1)
        {
            System.out.println("Wrong number of arguments for RESTORE operation\n");
			return " - Wrong number of arguments for RESTORE operation";
        }

        String fileID = getFileHashID(info.get(0));

        boolean found = false;
        System.out.println("Received the following request: - RESTORE " + info.get(0));

        for(int i = 0; i < this.storage.getStoredFiles().size(); i++) {
            if(this.storage.getStoredFiles().get(i).getFileID().equals(fileID)) {
                found = true;
                int nChunks = this.storage.getStoredFiles().get(i).getChunkNr();
                System.out.println("nChunks: " + nChunks);
                this.restoring = true;
                
                for(int j = 0; j <= nChunks; j++) // chunks ids -> [0,nChunks];
                {
                    this.executor.execute(new SendGetChunk(this.MCchannel, fileID, j));
                    Thread.sleep(150);
                }
                break;
            }
        }

        if(!found) return " - File not backed up";

        return null;
    }
    
    @Override
    public void deleteOperation(ArrayList<String> info) throws Exception {
        if(Float.compare(this.version, 1.0F) != 0) {
            System.out.println("Test App version not compatible with Peer version");
            return;
        }

        if(info.size() != 1)
        {
            System.out.println("Wrong number of arguments for DELETE operation\n");
			return;
        }
         
        String path = info.get(0);

        delete(path);
    }

    @Override
    public void deleteEnhOperation(ArrayList<String> info) throws Exception {
        if(Float.compare(this.version, 1.0F) == 0) {
            System.out.println("Test App version not compatible with Peer version");
            return;
        }

        if(info.size() != 1)
        {
            System.out.println("Wrong number of arguments for DELETE operation\n");
			return;
        }
         
        String path = info.get(0);

        delete(path);
    }

    public void delete(String path) throws Exception{
        System.out.println("Received the following request: - DELETE " + path);  

        String fileID = getFileHashID(path);
        System.out.println("File id: " + fileID);
        
        this.storage.deleteFileSent(fileID);
        
        if(Float.compare(this.version, 1.0F) != 0) {
            this.storage.addDeletedFile(fileID);
        }

        Peer.savesInfoStorage(this, this.storage);
        this.MCchannel.sendDelete(fileID);
    }
    
    @Override
    public void reclaimOperation(ArrayList<String> info) throws Exception {
        long diskSpacePermitted = Long.parseLong(info.get(0));
        this.storage.setMaxCapacity(diskSpacePermitted);

        File backup = new File("peer" + this.peerID + "/backup/");
        long folderSize = getFolderSize(backup);
        this.storage.setSpaceAvailable(diskSpacePermitted - folderSize);
        Peer.savesInfoStorage(this, this.storage);

        if(this.storage.getSpaceAvailable() > 0){
            return;
        }

        for (Map.Entry<AbstractMap.SimpleEntry<String, Integer>, Chunk> entry : this.storage.getStoredChunks().entrySet()) {
            AbstractMap.SimpleEntry<String, Integer> key = entry.getKey();
            Chunk chunk = entry.getValue();
            this.storage.deleteChunk(key, this.peerID);
            Peer.savesInfoStorage(this, this.storage);
            this.executor.execute((Runnable) new SendRemoved(this.MCchannel, key.getKey(), key.getValue()));
            if(this.storage.getSpaceAvailable() > 0){
                return;
            }
        }

    }

    @Override
    public String stateOperation(ArrayList<String> info) {
        if(info.size() != 0) {
            System.out.println("Wrong number of arguments for STATE operation\n");
			return "ERROR";
        }
        String information = "";

        System.out.println("Received the following request: - STATE");  

        information += "------------------------------------------------------\n";

        information += " For each file whose backup it has initiated:\n";
        
        if(this.storage.getStoredFiles().size() == 0) information += " - No files backed up\n";

        for(int i = 0; i < this.storage.getStoredFiles().size(); i++) {
            if(i >= 1 ) information += "\n";
            information += "  - The file pathname: " + this.storage.getStoredFiles().get(i).getPath() + "\n";
            information += "  - The backup service id of the file: " + this.storage.getStoredFiles().get(i).getFileID() + "\n";
            information += "  - The desired replication degree: " + this.storage.getStoredFiles().get(i).getDRD() + "\n";
            information += "  - For each chunk of the file:\n";

            for(AbstractMap.SimpleEntry<String, Integer> key : this.storage.getInitiatedChunks().keySet()) {
                if(key.getKey().equals(this.storage.getStoredFiles().get(i).getFileID())) {
                    information += "    - Its id: " + key.getValue() + "\n";
                    information += "      Its perceived replication degree: " + 
                            this.storage.getInitiatedChunk(key).getObservedRD();
                    if(this.storage.getInitiatedChunk(key).getStorers().size() != 0)
                        information += " -";
                    for(Integer pid : this.storage.getInitiatedChunk(key).getStorers()) {
                        information += " " + pid;
                    }
                    information += "\n";
                }
            }
        }

        information += "\n For each chunk it stores:\n";
        int counter = 0;

        for(AbstractMap.SimpleEntry<String, Integer> key : this.storage.getStoredChunks().keySet()) {
            boolean wasBackupInitiator = false;
            for(FileManager fm : this.storage.getStoredFiles()) {
                if(fm.getFileID().equals(key.getKey())) {
                    wasBackupInitiator = true;
                }
            }
            if(!wasBackupInitiator) {
                counter++;
                Chunk aux = this.storage.getStoredChunks().get(key);
                information += "    - Its id: " + aux.getId() + "\n";
                information += "      Its size: " + aux.getSize() / 1000 + "\n";
                information += "      Its perceived replication degree: " + aux.getObservedRD() + "\n";
            }
        }
        if(this.storage.getStoredChunks().size() == 0 || counter == 0) information += "  - No chunks backed up\n";
        
        information += "\n Storage:\n" +
                    "  - Capacity available: " + this.storage.getSpaceAvailable() + "\n" + 
                    "  - Maximum capacity: " + this.storage.getMaxCapacity() + "\n";

        information += "------------------------------------------------------\n";

        return information;
    }
}