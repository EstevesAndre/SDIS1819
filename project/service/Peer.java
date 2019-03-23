package project.service;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

import project.rmi.RemoteInterface;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;


public class Peer implements RemoteInterface {

    private short peerID;
    private float version;
    private String accessPoint;
    private MulticastSocket MCSocket;
    private MulticastSocket MDBSocket;
    private MulticastSocket MDRSocket;

    private InetAddress MCAddress;
    private int MCPortNumber;
    private InetAddress MDBAddress;
    private int MDBPortNumber;
    private InetAddress MDRAddress;
    private int MDRPortNumber;
    
    private String filename;

    public Peer(String version, String serverID, String accessPoint, String MCAddr, String MDBAddr, String MDRAddr) throws Exception {
        
        System.setProperty("java.net.preferIPv4Stack", "true");

        this.peerID = Short.parseShort(serverID);
        this.version = Float.parseFloat(version);
        this.accessPoint = accessPoint;

        this.joinMC(MCAddr);
        this.joinMDB(MDBAddr);
        this.joinMDR(MDRAddr);

        this.joinRMI();
    }

    public void joinRMI() throws Exception {

        try {
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(this, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(this.accessPoint, stub);

			System.out.println("Server ready\n");
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

    public void joinMDB(String MDBAddr) throws Exception {

        String multicastHostName = MDBAddr.split(" ")[0];
        String mcastPort = MDBAddr.split(" ")[1];

        this.MDBAddress = InetAddress.getByName(multicastHostName);
        this.MDBPortNumber = Integer.parseInt(mcastPort);

        // Join multicast group
        this.MDBSocket = new MulticastSocket(this.MDBPortNumber);
        this.MDBSocket.joinGroup(this.MDBAddress);
    }

    public void joinMDR(String MDRAddr) throws Exception {

        String multicastHostName = MDRAddr.split(" ")[0];
        String mcastPort = MDRAddr.split(" ")[1];

        this.MDRAddress = InetAddress.getByName(multicastHostName);
        this.MDRPortNumber = Integer.parseInt(mcastPort);

        // Join multicast group
        this.MDRSocket = new MulticastSocket(this.MDBPortNumber);
        this.MDRSocket.joinGroup(this.MDBAddress);
    }

    public String createHeader(String messageType, String fileID, String chunkNumber, String replicationDegree){
        return messageType + " " + this.version + " " + this.peerID + " " + fileID + " " + chunkNumber + " " + replicationDegree + "\r\n \r\n";
    }

    public void sendPutChunk() throws Exception {
        byte[] header = createHeader("test", "1", "1", "2").getBytes();
        
		DatagramPacket sendPacket = new DatagramPacket(header, header.length, this.MDBAddress, this.MDBPortNumber);
		this.MDBSocket.send(sendPacket);
    }

    public void splitFile(File file) throws IOException {
        int partCounter = 1;//I like to name parts from 001, 002, 003, ...
        //you can change it to 0 if you want 000, 001, ...

        int sizeOfFiles = 1000 * 64;// 64KB
        byte[] buffer = new byte[sizeOfFiles];

        String fileName = file.getName();

        //try-with-resources to ensure closing stream
        try (FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis)) {

            int bytesAmount = 0;
            while ((bytesAmount = bis.read(buffer)) > 0) {
                //write each chunk of data into separate file with different number in name
                String filePartName = String.format("%s.%03d", fileName, partCounter++);
                File newFile = new File(file.getParent(), filePartName);
                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    out.write(buffer, 0, bytesAmount);
                }
            }
        }
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

        /*
        if(args[0].equals("1")) // ja nao existe
        {
            peer.sendPutChunk();
            System.out.println("Sent");
        }
        
		byte[] receiveData = new byte[256];
		DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
        peer.MDBSocket.receive(packet);
		String received = new String(packet.getData(), 0, packet.getLength());
		System.out.println("Received packet: " + received);

        File file = new File("project/example_file.txt");

        peer.splitFile(file);

        */
        
    }

    @Override
    public void backupOperation(ArrayList<String> info) {
        System.out.print("Received the following request: \n - BACKUP");
        for(String i : info) {
            System.out.print(" " + i);
        }
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