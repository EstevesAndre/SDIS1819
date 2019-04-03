package project.service;

import java.io.*;
import java.net.*;
import javafx.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.concurrent.ThreadPoolExecutor;

import java.util.concurrent.Executors;

import project.channels.MCChannel;
import project.channels.MDBChannel;
import project.channels.MDRChannel;
import project.database.Chunk;
import project.database.FileManager;
import project.rmi.RemoteInterface;
import project.threads.SendPutChunk;

import java.rmi.registry.Registry;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class Peer implements RemoteInterface, Remote {

    private short peerID;
    private float version;
    private String accessPoint;

    private MCChannel MCchannel;
    private MDBChannel MDBchannel;
    private MDRChannel MDRchannel;

    private ArrayList<Chunk> backedUpChunks;
    private HashSet<Pair<String, Integer>> backedUp;

    private ThreadPoolExecutor executor;

    public Peer(String version, String serverID, String accessPoint, String MCAddr, String MDBAddr, String MDRAddr) throws Exception {
        
        System.setProperty("java.net.preferIPv4Stack", "true");

        this.peerID = Short.parseShort(serverID);
        this.version = Float.parseFloat(version);
        this.accessPoint = accessPoint;

        this.MCchannel = new MCChannel(MCAddr, this);
        this.MDBchannel = new MDBChannel(MDBAddr, this);
        this.MDRchannel = new MDRChannel(MDRAddr, this);

        this.backedUp = new HashSet<Pair<String,Integer>>();
        this.backedUpChunks = new ArrayList<Chunk>();

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
            System.out.println("Reading my own message...\n");
            return;
        }
        
        String fileID = header[3];
        Integer chunkID = Integer.parseInt(header[4]);
        System.out.println("receivePutChunk " + fileID + " " + chunkID);
        
        //if(spaceAvailable() >= received[1].length)
        //{
            int rd = Integer.parseInt(header[5]);
            
            if(!this.backedUp.contains(new Pair<String,Integer>(fileID, chunkID))) {
                
                this.backedUp.add(new Pair<String,Integer>(fileID, chunkID));

                Chunk newChunk = new Chunk(fileID, chunkID, received[1].getBytes(), rd);
                this.backedUpChunks.add(newChunk);
                newChunk.storeChunk(this.peerID);       
            }

            this.MCchannel.sendStored("STORED", fileID, chunkID, rd);

        //}
        // error, no space available
        

    }

    public static void main(String[] args) throws Exception {
        
        // example: java project/service/Peer 1.0 1234 RemoteInterface "230.0.0.0 9876" "230.0.0.1 9877" "230.0.0.2 9878"
        // 1 envia packet

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

        System.out.print("Received the following request: \n - BACKUP ");
        
        String path = info.get(0);
        int rd = Integer.parseInt(info.get(1));
        ArrayList<Chunk> chunks = FileManager.splitFile(path);
        String fileID = "1";
        
        for(int i = 0; i < chunks.size(); i++)
        {
            this.executor.execute(new SendPutChunk(this.MDBchannel, fileID, chunks.get(i), rd));
            Thread.sleep(50);
        }
        //System.out.println(this.executor.getActiveCount());
    }

    @Override
    public void restoreOperation(ArrayList<String> info) {

    }
    
    @Override
    public void deleteOperation(ArrayList<String> info) {
        
    }
    
    @Override
    public void reclaimOperation(ArrayList<String> info) {
        
    }

    @Override
    public void stateOperation(ArrayList<String> info) {
        
    }
}