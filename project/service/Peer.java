package project.service;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

import project.channels.MDBChannel;
import project.database.Chunk;
import project.database.FileManager;
import project.rmi.RemoteInterface;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;


public class Peer implements RemoteInterface {

    private short peerID;
    private float version;
    private String accessPoint;
    private MulticastSocket MCSocket;
    //private MulticastSocket MDBSocket;
    private MulticastSocket MDRSocket;

    private InetAddress MCAddress;
    private int MCPortNumber;
    //private InetAddress MDBAddress;
    //private int MDBPortNumber;
    private InetAddress MDRAddress;
    private int MDRPortNumber;

    private MDBChannel MDBchannel;
    private FileManager fileManager;

    public Peer(String version, String serverID, String accessPoint, String MCAddr, String MDBAddr, String MDRAddr) throws Exception {
        
        System.setProperty("java.net.preferIPv4Stack", "true");

        this.peerID = Short.parseShort(serverID);
        this.version = Float.parseFloat(version);
        this.accessPoint = accessPoint;

        this.joinMC(MCAddr);
        this.joinMDR(MDRAddr);

        this.MDBchannel = new MDBChannel(MDBAddr, this.peerID, this.version);

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

    public void joinMC(String MCAddr) throws Exception {

        String multicastHostName = MCAddr.split(" ")[0];
        String mcastPort = MCAddr.split(" ")[1];

        this.MCAddress = InetAddress.getByName(multicastHostName);
        this.MCPortNumber = Integer.parseInt(mcastPort);

        // Join multicast group
        this.MCSocket = new MulticastSocket(this.MCPortNumber);
        this.MCSocket.joinGroup(this.MCAddress);
    }

    public void joinMDR(String MDRAddr) throws Exception {

        String multicastHostName = MDRAddr.split(" ")[0];
        String mcastPort = MDRAddr.split(" ")[1];

        this.MDRAddress = InetAddress.getByName(multicastHostName);
        this.MDRPortNumber = Integer.parseInt(mcastPort);

        // Join multicast group
        this.MDRSocket = new MulticastSocket(this.MDRPortNumber);
        this.MDRSocket.joinGroup(this.MDRAddress);
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
        
    }

    @Override
    public void backupOperation(ArrayList<String> info) throws Exception{
        System.out.print("Received the following request: \n BACKUP ");
        
        String path = info.get(0);
        int rd = Integer.parseInt(info.get(1));

        System.out.print(path + " " + rd + "\n\n");

        this.fileManager = new FileManager("1", path, rd);
        ArrayList<Chunk> chunks = this.fileManager.splitFile();

        this.MDBchannel.sendPutChunk("1", chunks.get(0), rd);
        this.MDBchannel.receivePutChunk();
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