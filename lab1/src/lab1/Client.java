package lab1;

import java.net.*;
import java.io.*;

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
      System.out.println("FROM SERVER:" + response);
      
      clientSocket.close();
   }
}
