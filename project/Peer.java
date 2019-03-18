package project;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import java.net.*;
import java.io.*;

public class Peer implements ClientInterface{

    private short peerID;
    private float version;
    private MulticastSocket MCSocket;
    private MulticastSocket MDBSocket;

    private InetAddress MCAddress;
    private int MCPortNumber;
    private InetAddress MDBAddress;
    private int MDBPortNumber;
    
    public Peer(String version, String serverID) {
        this.peerID = Short.parseShort(serverID);
        this.version = Float.parseFloat(version);
        System.out.println(version);
    }

    public void createMC(String multicastHostName, String mcastPort) throws Exception {

        this.MCAddress = InetAddress.getByName(multicastHostName);
        this.MCPortNumber = Integer.parseInt(mcastPort);

        // Join multicast group
        this.MCSocket = new MulticastSocket(this.MCPortNumber);
        this.MCSocket.joinGroup(this.MCAddress);
    }

    public void createMDB(String multicastHostName, String mcastPort) throws Exception {

        this.MDBAddress = InetAddress.getByName(multicastHostName);
        this.MDBPortNumber = Integer.parseInt(mcastPort);

        // Join multicast group
        this.MDBSocket = new MulticastSocket(this.MDBPortNumber);
        this.MDBSocket.joinGroup(this.MDBAddress);
    }

    public void test() throws Exception {
        String x = "aaa asd    xsd";

        String[] s = x.split(" ");

        for(String var : s) {
            if(!var.equals(""))
            {

            }
        }
    }

    public String createHeader(String messageType, String fileID, String chunkNumber, String replicationDegree)
    {
        return messageType + " " + this.version + " " + this.peerID + " " + fileID + " " + chunkNumber + " " + replicationDegree + "\r\n \r\n";
    }

    public void sendPutChunk() throws Exception {
        byte[] header = createHeader("test", "1", "1", "2").getBytes();
        
		DatagramPacket sendPacket = new DatagramPacket(header, header.length, this.MDBAddress, this.MDBPortNumber);
		this.MDBSocket.send(sendPacket);
    }

    public static void main(String[] args) throws Exception {
        // endereço e porta de cada canal
        if(args.length != 10)
		{
            System.out.println("Wrong number of arguments.\nUsage: java project/Peer <type> <version> <server_ID> <server_access_point> <MC_address> <MC_port> <MDB_address> <MDB_port> <MDR_address> <MDR_port>");
            // example: java project/Peer 1 1.0 1234 12 230.0.0.0 9876 230.0.0.1 9877 230.0.0.2 9878
            // example: java project/Peer 2 1.0 1234 12 230.0.0.0 9876 230.0.0.1 9877 230.0.0.2 9878
            
			System.exit(-1);
        }

        System.setProperty("java.net.preferIPv4Stack", "true");
    
        Peer peer = new Peer(args[1], args[2]);

        peer.createMC(args[4], args[5]);
        peer.createMDB(args[6], args[7]);

        peer.test();

        if(args[0].equals("1"))
        {
            peer.sendPutChunk();
            System.out.println("Sent");
        }
        else
        {

        }
        
		byte[] receiveData = new byte[256];
		DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
        peer.MDBSocket.receive(packet);
		String received = new String(packet.getData(), 0, packet.getLength());
		System.out.println("Received packet: " + received);

    }
}