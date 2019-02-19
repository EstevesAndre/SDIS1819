package lab1;

import java.net.*;
import java.io.*;

/**
 * Class Client
 * Usage: javac lab1/client <host_name> <port_number> <oper> <opnd>*
 *      Example: javac lab1/client localhost 9876 "register 87-UI-64"
 *      Example: javac lab1/client localhost 9876 "lookup 87-UI-64"
 * 
 * Application: Client-server application to manage a small database of license plates.
 * 				Server must execute in an infinite loop waiting for client requests, processing, and reply to them.
 *              For any given vehicle, the system should allow to store its plate number and the name of its owner.
 *              Also, the system should allow to visualize the owner of a given plate number (if exists).
 * Submites a request to the server, Client waits a reply to the request, prints the reply, and then terminates.
 */
public class Client {
   
    public static void main(String args[]) throws Exception {
        
        String hostName = args[0];
        Integer portNumber = Integer.parseInt(args[1]);
        String oper = args[2];
        String opnd = args[3];
    
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByName(hostName);
        
        byte[] sendData = new byte[512];
        byte[] receiveData = new byte[512];
        
        String data = oper + " " + opnd;
        sendData = data.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, portNumber);
        clientSocket.send(sendPacket);
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        
        clientSocket.receive(receivePacket);
        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
        System.out.println("Received from server: " + response);
        
        clientSocket.close();
    }
}
