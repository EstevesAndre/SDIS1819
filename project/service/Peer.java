package project.service;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import java.net.*;
import java.io.*;

public class Peer {

    private short peerID;
    private float version;
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

    public Peer(String version, String serverID) {
        this.peerID = Short.parseShort(serverID);
        this.version = Float.parseFloat(version);
        System.out.println(version);
    }

    public void joinMC(String multicastHostName, String mcastPort) throws Exception {

        this.MCAddress = InetAddress.getByName(multicastHostName);
        this.MCPortNumber = Integer.parseInt(mcastPort);

        // Join multicast group
        this.MCSocket = new MulticastSocket(this.MCPortNumber);
        this.MCSocket.joinGroup(this.MCAddress);
    }

    public void joinMDB(String multicastHostName, String mcastPort) throws Exception {

        this.MDBAddress = InetAddress.getByName(multicastHostName);
        this.MDBPortNumber = Integer.parseInt(mcastPort);

        // Join multicast group
        this.MDBSocket = new MulticastSocket(this.MDBPortNumber);
        this.MDBSocket.joinGroup(this.MDBAddress);
    }

    public void joinMDR(String multicastHostName, String mcastPort) throws Exception {

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
        
        if(args.length != 10)
		{
            System.out.println("Wrong number of arguments.\nUsage: java project/Peer <type> <version> <server_ID> <server_access_point> <MC_address> <MC_port> <MDB_address> <MDB_port> <MDR_address> <MDR_port>");
            // example: java project/Peer 1 1.0 1234 12 230.0.0.0 9876 230.0.0.1 9877 230.0.0.2 9878
            // example: java project/Peer 2 1.0 1234 12 230.0.0.0 9876 230.0.0.1 9877 230.0.0.2 9878
            // 1 envia packet
			System.exit(-1);
        }

        System.setProperty("java.net.preferIPv4Stack", "true");

        Peer peer = new Peer(args[1], args[2]);

        peer.joinMC(args[4], args[5]);
        peer.joinMDB(args[6], args[7]);
        peer.joinMDR(args[8], args[9]);

        if(args[0].equals("1"))
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

    }
}