package project.channels;

import java.io.IOException;
import java.lang.Runnable;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.net.MulticastSocket;

import project.service.Peer;
import project.database.Chunk;
import project.threads.ReceiveMessage;

public class MDRChannel extends Channel implements Runnable{


    public MDRChannel(String MDRAddr, Peer peer) throws Exception{
        super(MDRAddr, peer);
    }

    public void sendChunk(String fileID, Chunk chunk) throws IOException {
        byte[] header = super.createHeader("CHUNK", fileID, chunk.getId()).getBytes();

        byte[] putChunk = new byte[header.length + chunk.getSize()];
        System.arraycopy(header, 0, putChunk, 0, header.length);
        System.arraycopy(chunk.getContent(), 0, putChunk, header.length, chunk.getSize());

        DatagramPacket sendPacket = new DatagramPacket(putChunk, putChunk.length, this.address, this.portNumber);
        MulticastSocket socket = new MulticastSocket(this.portNumber);
        socket.joinGroup(this.address);
        socket.send(sendPacket);
        socket.close();
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
                byte[] copy = Arrays.copyOf(receiveData, receivePacket.getLength());
               
                this.peer.getExec().execute(new ReceiveMessage(this.peer, copy));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}