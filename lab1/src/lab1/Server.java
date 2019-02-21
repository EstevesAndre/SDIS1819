package lab1;

import java.net.*;
import java.util.HashMap;
import java.io.*;

/**
 * Class Server
 * Usage: javac lab1/server <host_name>
 *      Example: javac lab1/server 9876
 * 
 * Application: Client-server application to manage a small database of license plates.
 * 				Server must execute in an infinite loop waiting for client requests, processing, and reply to them.
 *              For any given vehicle, the system should allow to store its plate number and the name of its owner.
 *              Also, the system should allow to visualize the owner of a given plate number (if exists).
 */
public class Server {
	public static void main(String args[]) throws Exception {
	   
		DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt(args[0]));
		
		System.out.println("Server Running...");
		byte[] receiveData = new byte[512];
		byte[] sendData = new byte[512];
		
		HashMap<String, String> plateNumbers = new HashMap<String, String>();
	   
	   	while(true) { 
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket);
			String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
			System.out.println("Received packet: " + sentence);
			String[] received = sentence.split(" ");
			
			if(received[0].compareTo("register") == 0) {
				if(!plateNumbers.containsKey(received[1])) {
					plateNumbers.put(received[1],received[2]);
					sendData = String.valueOf(plateNumbers.size()).getBytes();
				}
				else {
					sendData = "-1".getBytes();
				}
			}
			else if(received[0].compareTo("lookup") == 0) {
				String owner = plateNumbers.get(received[1]);
				if(owner != null) {
					sendData = owner.getBytes();
				}
				else {
					sendData = "NOT_FOUND".getBytes();
				}
			}
			
			InetAddress IPAddress = receivePacket.getAddress();
			System.out.println("Server sending back: " + new String(sendData));
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, receivePacket.getPort());
			serverSocket.send(sendPacket);
		}
	}
}
