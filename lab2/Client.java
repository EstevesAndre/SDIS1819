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
public class Client {
   
    public static void main(String args[]) throws Exception {
        
        if(args.length != 4)
		{
			System.out.println("Wrong number of arguments.\nUsage: java lab2/Client <multicast_address> <multicast_port> <oper> <opnd>*");
			System.exit(-1);
        }
        
		System.setProperty("java.net.preferIPv4Stack", "true");
        Integer portNumber = Integer.parseInt(args[1]);

        // Join multicast group
        InetAddress IPAddress = InetAddress.getByName(args[0]);
        MulticastSocket multiSocket = new MulticastSocket(portNumber);
        multiSocket.joinGroup(IPAddress);

        // Receive advertisement
        byte[] receiveDataAdvertisement = new byte[512];
        DatagramPacket receiveAdvertisement = new DatagramPacket(receiveDataAdvertisement, receiveDataAdvertisement.length);
        multiSocket.receive(receiveAdvertisement);

        // Parse server's advertisement
        String[] registryInfo = new String(receiveAdvertisement.getData(), 0, receiveAdvertisement.getLength()).split(" ");
        System.out.println("IP registry: " + registryInfo[0] + "\nRegistry port: " + registryInfo[1]);
        InetAddress registryAddress = InetAddress.getByName(registryInfo[0]);
        Integer registryPort = Integer.parseInt(registryInfo[1]);

        // Received advertisement
        // Now creates a DatagramSocket to comunicate with Registry
        DatagramSocket clientSocket = new DatagramSocket();
        
        byte[] sendData = new byte[512];
        byte[] receiveData = new byte[512];
        String oper = args[2];
        String opnd = args[3];

        String data = oper + " " + opnd;
        System.out.println(data);

        sendData = data.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, registryAddress, registryPort);
        clientSocket.send(sendPacket);
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        
        clientSocket.receive(receivePacket);
        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
        System.out.println("Received from server: " + response);
        
        clientSocket.close();
        multiSocket.close();
    }
}
