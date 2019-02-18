package lab1;

import java.net.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.io.*;

public class Server {
   public static void main(String args[]) throws Exception {
	   
	   DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt(args[0]));
	   
	   System.out.println("Running");
	   byte[] receiveData = new byte[512];
	   byte[] sendData = new byte[512];
	   
	   HashMap<String, String> plateNumbers = new HashMap<String, String>();
	   
	   
	   while(true) { 
          DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
          serverSocket.receive(receivePacket);
          String sentence = new String( receivePacket.getData(), 0, receivePacket.getLength());
          System.out.println(sentence);
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
          int port = receivePacket.getPort();
          System.out.println(new String(sendData));
          DatagramPacket sendPacket =
        		  new DatagramPacket(sendData, sendData.length, IPAddress, port);
          serverSocket.send(sendPacket);
	   }
      }
}
