package lab2;

import java.net.*;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Timer;

/**
 * Compile as javac *.java
 * Class Server
 * Usage: java lab2/Server <srvc_port> <mcast_addr> <mcast_port>
 *      Example: java lab2/Server 4745 230.0.0.0 9876
 * 
 * Application: Client-server application to manage a small database of license plates.
 * 				Server must execute in an infinite loop waiting for client requests, processing, and reply to them.
 *              For any given vehicle, the system should allow to store its plate number and the name of its owner.
 *              Also, the system should allow to visualize the owner of a given plate number (if exists).
 */
public class Server {

	private InetAddress IPAddress;
	private Integer portNumber;
	private Integer servicePort;
	private MulticastSocket multicastSocket;
	private DatagramSocket socket;
	private HashMap<String, String> plateNumbers;

	// time between each advertisement (period)
    private static final int TIMER_PERIOD = 1000;
	private static final int MAX_REQUEST_SIZE = 274;

	public Server(String srvcPort, String multicastHostName, String mcastPort) throws Exception {
		
		this.plateNumbers = new HashMap<String, String>();

		this.IPAddress = InetAddress.getByName(multicastHostName);
		this.portNumber = Integer.parseInt(mcastPort);
		this.servicePort = Integer.parseInt(srvcPort);

		this.socket = new DatagramSocket(this.servicePort);
		this.multicastSocket = new MulticastSocket(this.portNumber);

		// Creates threads
			// 1 for loop of advertisements
			// other to recieve requests and response to them
		//Timer timer  = new Timer();
		//timer.scheduleAtFixedRate(null, 0, TIMER_PERIOD);
		
		System.out.println("Created server!!");
	}

	private void receiveRequest() throws Exception {
		byte[] receiveData = new byte[MAX_REQUEST_SIZE];
		DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);

		this.socket.receive(packet);
		String received = new String(packet.getData(), 0, packet.getLength());
		System.out.println("Received packet: " + received);

		String[] receivedSplited = received.split(" ");
		switch (receivedSplited[0]) {
            case "REGISTER":
                //sendReply(registerUser(groups[1], received.substring(OWNER_NAME_POS)));
                break;
            case "LOOKUP":
                //sendReply(getOwner(groups[1]));
                break;
            default:
                System.err.println("Server Error: Received unknown request.");
        }

		byte[] sendData = new byte[MAX_REQUEST_SIZE];
	}

	private static boolean validatePlateNumber(String platen) throws Exception {
		Pattern pattern = Pattern.compile("(\\w\\w-){2}\\w{2}");
		Matcher matcher = pattern.matcher(platen);

		return matcher.matches();
	}

	private void sendAdvertisement() throws Exception {
		// sends advertisement
		// "IP"+" "+"Port"
		byte[] advertisementData = new byte[MAX_REQUEST_SIZE];
		advertisementData = (this.IPAddress.getHostName() + " " + this.servicePort).getBytes();	
		
		DatagramPacket advertisementPacket = new DatagramPacket(advertisementData, advertisementData.length, this.IPAddress, this.portNumber);
		this.multicastSocket.send(advertisementPacket);

		System.out.println("Advertisement sent!");
	}

	public static void main(String args[]) throws Exception {
	   
		if(args.length != 3)
		{
			System.out.println("Wrong number of arguments.\nUsage: java lab2/Server <srvc_port> <mcast_addr> <mcast_port>");
			System.exit(-1);
		}
		
		Server server = new Server(args[0], args[1], args[2]);
		
		server.sendAdvertisement();

		server.multicastSocket.close();
        
		/*
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
}
