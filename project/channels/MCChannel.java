package project.channels;

import java.io.IOException;
import java.lang.Runnable;
import java.net.DatagramPacket;
import java.util.concurrent.TimeUnit;

import project.threads.ReceiveMessage;
import project.service.Peer;

public class MCChannel extends Channel implements Runnable{

    public MCChannel(String MCCAddr, Peer peer) throws Exception {
        super(MCCAddr, peer);
    }

    public void sendStored(String messageType, String fileID, int chunkNumber, int replicationDegree) throws IOException {
        
        byte[] message = super.createHeader(messageType, fileID, chunkNumber, replicationDegree).getBytes();
        
        DatagramPacket sendPacket = new DatagramPacket(message, message.length, this.address, this.portNumber);
		this.socket.send(sendPacket);
    }

    @Override
    public void run() {
        try {
            byte[] receiveData = new byte[66000];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, 66000);

            while(true) {
                //Long duration = (long) (Math.random() * 10);
                //System.out.println("Executing: MCC");
                System.out.println("Reading from MCChannel");
                this.socket.receive(receivePacket);
                this.peer.getExec().execute(new ReceiveMessage(this.peer, receivePacket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}