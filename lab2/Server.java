package lab2;

import java.net.*;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compile as javac *.java
 * Class Server
 * Usage: java lab1/server <port_number>
 *      Example: java lab1/Server 9876
 * 
 * Application: Client-server application to manage a small database of license plates.
 * 				Server must execute in an infinite loop waiting for client requests, processing, and reply to them.
 *              For any given vehicle, the system should allow to store its plate number and the name of its owner.
 *              Also, the system should allow to visualize the owner of a given plate number (if exists).
 */
public class Server {
	public static void main(String args[]) throws Exception {
	   
		if(args.length != 4)
		{
			System.out.println("Wrong number of arguments.\nUsage: java lab2/Server <multicast_address> <port_number> <IP_registry> <registry_port>");
			System.exit(-1);
		}
		
		Integer portNumber = Integer.parseInt(args[1]); 

		InetAddress IPAddress = InetAddress.getByName(args[0]);
		MulticastSocket multiSocket = new MulticastSocket(portNumber);

		byte[] sendAdvertisement = new byte[512];
		// "IP"+" "+"Port"
		sendAdvertisement = (InetAddress.getByName(args[2]) + " " + args[3]).getBytes();

		while(true)
		{	
			DatagramPacket advertisement = new DatagramPacket(sendAdvertisement, sendAdvertisement.length, IPAddress, portNumber);
			multiSocket.send(advertisement);

			break;
		}

		multiSocket.close();
        
		/*System.out.println("Server Running...");
		byte[] receiveData = new byte[512];
		byte[] sendData = new byte[512];
		
		HashMap<String, String> plateNumbers = new HashMap<String, String>();
	   
	   	while(true) { 
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket);
			String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
			System.out.println("Received packet: " + sentence);
			String[] received = sentence.split(" ");
			
			if(received.length == 0)
			{
				sendData = "-2".getBytes();
			}
			else if(received[0].compareTo("register") == 0) {
				if(validatePlateNumber(received[1]) && !plateNumbers.containsKey(received[1])) {
					plateNumbers.put(received[1],received[2]);
					sendData = String.valueOf(plateNumbers.size()).getBytes();
				}
				else {
					sendData = "-1".getBytes();
				}
			}
			else if(received[0].compareTo("lookup") == 0) {
				if(validatePlateNumber(received[1]))
				{
					String owner = plateNumbers.get(received[1]);
					if(owner != null) {
						sendData = owner.getBytes();
					}
					else {
						sendData = "NOT_FOUND".getBytes();
					}	
				}
				else 
				{
					sendData = "NOT_FOUND".getBytes();
				}
			}
			else
			{
				sendData = "-2".getBytes();
			}
			
			InetAddress IPAddress = receivePacket.getAddress();
			System.out.println("Server sending back: " + new String(sendData));
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, receivePacket.getPort());
			serverSocket.send(sendPacket);
		}*/
	}

	private static boolean validatePlateNumber(String platen)
	{
		Pattern pattern = Pattern.compile("(\\w\\w-){2}\\w{2}");
		Matcher matcher = pattern.matcher(platen);

		return matcher.matches();
	}
}
