package project.channels;

import java.io.IOException;
import java.lang.Runnable;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;

import project.threads.ReceiveMessage;
import project.service.Peer;

public class MCChannel extends Channel implements Runnable{

    public MCChannel(String MCCAddr, Peer peer) throws Exception {
        super(MCCAddr, peer);
    }

    public void sendStored(String fileID, int chunkNumber) throws IOException {
        
        byte[] message = super.createHeader("STORED", fileID, chunkNumber).getBytes();
        
        DatagramPacket sendPacket = new DatagramPacket(message, message.length, this.address, this.portNumber);
        MulticastSocket socket = new MulticastSocket(this.portNumber);
        socket.joinGroup(this.address);
		socket.send(sendPacket);
    }

    public void sendDelete(String fileId) throws IOException {
        byte[] message = super.createHeader("DELETE", fileId).getBytes();

        DatagramPacket sendPacket = new DatagramPacket(message, message.length, this.address, this.portNumber);
        MulticastSocket socket = new MulticastSocket(this.portNumber);
        socket.joinGroup(this.address);
		socket.send(sendPacket);
    }

    @Override
    public void run() {
        
        try {
            byte[] receiveData = new byte[66000];

            MulticastSocket socket = new MulticastSocket(this.portNumber);
            socket.joinGroup(this.address);

            while(true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                socket.receive(receivePacket);
                
                this.peer.getExec().execute(new ReceiveMessage(this.peer, receivePacket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}