package lab2;

import java.net.*;
import java.io.*;

/**
 * Compile as javac *.java
 * Class Client
 * Usage: java lab1/Client <multicast address> <multicast_port> <oper> <opnd>*
 *      Example: java lab2/Client 230.0.0.0 9876 register "87-UI-64 Andre"
 *      Example: java lab2/Client 230.0.0.0 9876 lookup "87-UI-64"
 * 
 * Application: Client-server application to manage a small database of license plates.
 * 				Server must execute in an infinite loop waiting for client requests, processing, and reply to them.
 *              For any given vehicle, the system should allow to store its plate number and the name of its owner.
 *              Also, the system should allow to visualize the owner of a given plate number (if exists).
 * Submits a request to the server, Client waits a reply to the request, prints the reply, and then terminates.
 */
public class Client{
    InetAddress multicastAddress;
    InetAddress registryAddress;

    Integer multicastPort;
    Integer registryPort;

    MulticastSocket multicastSocket;
    DatagramSocket clientSocket;
   
    public Client(String multicastHostName, String mcastPort) throws Exception {
        this.multicastAddress = InetAddress.getByName(multicastHostName);
        this.multicastPort = Integer.parseInt(mcastPort);

        // Join multicast group
        this.multicastSocket = new MulticastSocket(this.multicastPort);
        this.multicastSocket.joinGroup(this.multicastAddress);
    }

    public void receiveAdvertisement() throws Exception {
        // Receive advertisement
        byte[] advertisementData = new byte[512];
        DatagramPacket advertisementPacket = new DatagramPacket(advertisementData, advertisementData.length);
        this.multicastSocket.receive(advertisementPacket);

        // Parse server's advertisement
        String[] registryInfo = new String(advertisementPacket.getData(), 0, advertisementPacket.getLength()).split(" ");
        System.out.println("IP registry: " + registryInfo[0] + "\nRegistry port: " + registryInfo[1]);
        this.registryAddress = InetAddress.getByName(registryInfo[0]);
        this.registryPort = Integer.parseInt(registryInfo[1]);

        // Create a DatagramSocket to comunicate with Registry
        this.clientSocket = new DatagramSocket();

    }

    public void sendRequest(String oper, String opnd) throws Exception {
        String data = oper + " " + opnd;
        System.out.println("My request: " + data);

        byte[] requestData = data.getBytes();
        DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, this.registryAddress, this.registryPort);

        this.clientSocket.send(requestPacket);

    }

    public void receiveReply() throws Exception {
        DatagramPacket replyPacket = new DatagramPacket(new byte[512], 512);
        
        this.clientSocket.receive(replyPacket);
        String reply = new String(replyPacket.getData(), 0, replyPacket.getLength());
        System.out.println("Reply from server: " + reply);
    }

    public static void main(String args[]) throws Exception {
        
        if(args.length != 4)
		{
			System.out.println("Wrong number of arguments.\nUsage: java lab2/Client <multicast_address> <multicast_port> <oper> <opnd>*");
			System.exit(-1);
        }
        
		System.setProperty("java.net.preferIPv4Stack", "true");
        
        Client client = new Client(args[0], args[1]);

        client.receiveAdvertisement();
        client.sendRequest(args[2], args[3]);
        client.receiveReply();
        
        client.clientSocket.close();
        client.multicastSocket.close();
    }
}
