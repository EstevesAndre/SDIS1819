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
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.concurrent.ThreadPoolExecutor;

import java.util.concurrent.Executors;

import project.channels.MCChannel;
import project.channels.MDBChannel;
import project.channels.MDRChannel;
import project.database.Chunk;
import project.database.FileManager;
import project.database.InitiatedChunk;
import project.rmi.RemoteInterface;
import project.threads.SendPutChunk;

import java.rmi.registry.Registry;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;

public class Peer implements RemoteInterface, Remote {

    private short peerID;
    private float version;
    private String accessPoint;

    private MCChannel MCchannel;
    private MDBChannel MDBchannel;
    private MDRChannel MDRchannel;

    private HashMap<Map.Entry<String,Integer>, Chunk> backedUpChunks;
    private HashMap<Map.Entry<String,Integer>, InitiatedChunk> initiatedChunks;

    private ThreadPoolExecutor executor;

    public Peer(String version, String serverID, String accessPoint, String MCAddr, String MDBAddr, String MDRAddr) throws Exception {
        
        System.setProperty("java.net.preferIPv4Stack", "true");

        this.peerID = Short.parseShort(serverID);
        this.version = Float.parseFloat(version);
        this.accessPoint = accessPoint;

        this.MCchannel = new MCChannel(MCAddr, this);
        this.MDBchannel = new MDBChannel(MDBAddr, this);
        this.MDRchannel = new MDRChannel(MDRAddr, this);

        this.backedUpChunks = new HashMap<Map.Entry<String,Integer>, Chunk>();
        this.initiatedChunks = new HashMap<Map.Entry<String,Integer>, InitiatedChunk>();

        this.joinRMI();
    }

    public void joinRMI() throws Exception {

        try {
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(this, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(this.accessPoint, stub);

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

    public void receivePutChunk(DatagramPacket receivePacket) throws IOException {
        
        String[] received = new String(receivePacket.getData(), 0, receivePacket.getLength()).split("\r\n\r\n");
        String[] header = received[0].split("\\s+");

        // if peer is reading its own message
        if(Integer.parseInt(header[2]) == this.peerID) {
            return;
        }
        
        String fileID = header[3];
        Integer chunkID = Integer.parseInt(header[4]);
        System.out.println("receivePutChunk " + fileID + " " + chunkID);
        
        //if(spaceAvailable() >= received[1].length)
        //{
            int rd = Integer.parseInt(header[5]);
            AbstractMap.SimpleEntry<String, Integer> chunk = new AbstractMap.SimpleEntry<String, Integer>(fileID, chunkID);
            
            if(!this.backedUpChunks.containsKey(chunk)) {
                Chunk newChunk = new Chunk(fileID, chunkID, received[1].getBytes(), rd);
                this.backedUpChunks.put(chunk, newChunk);
                newChunk.storeChunk(this.peerID);       
            }

            this.MCchannel.sendStored(fileID, chunkID);

        //}
        // error, no space available
        

    }

    public void receiveStored(String[] message) {
        
        // verifies if is not the same peer
        int sender = Integer.parseInt(message[2]);
        if(sender == this.peerID)
            return;

        String fileId = message[3];
        int chunkId = Integer.parseInt(message[4]);

        System.out.println("Received stored " + fileId + " " + chunkId);
        
        InitiatedChunk initiated = this.initiatedChunks.get(new AbstractMap.SimpleEntry<String, Integer>(fileId, chunkId));
        if(initiated != null) {
            initiated.addStorer(sender);
        }

        AbstractMap.SimpleEntry<String, Integer> chunk = new AbstractMap.SimpleEntry<String, Integer>(fileId, chunkId);
            
        if(this.backedUpChunks.containsKey(chunk)){
            this.backedUpChunks.get(chunk).addStorer(sender);
        }
    }

    public void receiveDelete(String[] message) throws IOException {
        String fileId = message[3];

        int chunkId = 0;
        Map.Entry<String, Integer> chunk = new AbstractMap.SimpleEntry<String, Integer>(fileId, chunkId);

        if(this.backedUpChunks.containsKey(chunk)) {
            boolean stillHasChunks = true;

            while(stillHasChunks) {
                this.backedUpChunks.get(chunk).deleteChunk(this.peerID);
                this.backedUpChunks.remove(chunk);

                chunkId++;
                chunk = new AbstractMap.SimpleEntry<String, Integer>(fileId, chunkId);
                if(!this.backedUpChunks.containsKey(chunk)) {
                    stillHasChunks = false;
                }
            }
        }

        if(this.initiatedChunks.containsKey(chunk)){
            this.initiatedChunks.remove(chunk);
        }
    }

    public static void main(String[] args) throws Exception {
        
        // example initiator peer: java project/service/Peer 1.0 1234 RemoteInterface "230.0.0.0 9876" "230.0.0.1 9877" "230.0.0.2 9878"
        // example peer: java project/service/Peer 1.0 444 RemoteInterface2 "230.0.0.0 9876" "230.0.0.1 9877" "230.0.0.2 9878"
        // example peer: java project/service/Peer 1.0 555 RemoteInterface3 "230.0.0.0 9876" "230.0.0.1 9877" "230.0.0.2 9878"
        if(args.length != 6)
		{
            System.out.println("Wrong number of arguments.\nUsage: java project/Peer <version> <serverID> <accessPoint> \"<MC_address> <MC_port>\" \"<MDB_address> <MDB_port>\" \"<MDR_address> <MDR_port>\"\r\n");
            
			System.exit(-1);
        }

        Peer peer = new Peer(args[0], args[1], args[2], args[3], args[4], args[5]);
        peer.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(30);
        peer.executor.execute(peer.MDBchannel);
        peer.executor.execute(peer.MCchannel);
    }

    @Override
    public void backupOperation(ArrayList<String> info) throws Exception {

        if(info.size() != 2)
        {
            System.out.println("Wrong number of arguments for BACKUP operation\n");
			System.exit(-1);
        }

        System.out.println("Received the following request: \n - BACKUP ");
        
        String path = info.get(0);
        int rd = Integer.parseInt(info.get(1));
        ArrayList<Chunk> chunks = FileManager.splitFile(path);
        //String fileID = "1";

        // generating file id with SHA-256
        File file = new File(path);
        Path p = Paths.get(file.getAbsolutePath());
        BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String toHash = file.getName() + attr.creationTime().toString() + attr.lastModifiedTime().toString();
        String fileID = FileManager.bytesToHex(digest.digest(toHash.getBytes(StandardCharsets.UTF_8)));

        for(int i = 0; i < chunks.size(); i++){
            InitiatedChunk initiatedChunk = new InitiatedChunk();
            this.initiatedChunks.put(new AbstractMap.SimpleEntry<String, Integer>(fileID, i), initiatedChunk);
            this.executor.execute(new SendPutChunk(this.MDBchannel, fileID, chunks.get(i), rd));
            Thread.sleep(50);
        }

        for(Map.Entry<String, Integer> key : this.initiatedChunks.keySet()){
            InitiatedChunk initiated = this.initiatedChunks.get(key);
            while(initiated.getObservedRD() < rd) {
                this.executor.execute(new SendPutChunk(this.MDBchannel, fileID, chunks.get(key.getValue()), rd));
                Thread.sleep(300);
            }
        }

        System.out.println("Reached desired RD, finished backup operation.");
        //System.out.println(this.executor.getActiveCount());
    }

    @Override
    public void restoreOperation(ArrayList<String> info) {

    }
    
    @Override
    public void deleteOperation(ArrayList<String> info) throws Exception {
        if(info.size() != 1)
        {
            System.out.println("Wrong number of arguments for DELETE operation\n");
			System.exit(-1);
        }

        System.out.println("Received the following request: \n - DELETE ");   
        
        String path = info.get(0);

        // generating file id with SHA-256
        File file = new File(path);
        Path p = Paths.get(file.getAbsolutePath());
        BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String toHash = file.getName() + attr.creationTime().toString() + attr.lastModifiedTime().toString();
        String fileId = FileManager.bytesToHex(digest.digest(toHash.getBytes(StandardCharsets.UTF_8)));

        System.out.println("File id: " + fileId);

        

        this.MCchannel.sendDelete(fileId);
    }
    
    @Override
    public void reclaimOperation(ArrayList<String> info) {
        
    }

    @Override
    public void stateOperation(ArrayList<String> info) {
        
    }
}