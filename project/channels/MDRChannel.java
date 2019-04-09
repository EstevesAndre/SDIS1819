package project.channels;

import java.io.IOException;
import java.lang.Runnable;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

import project.service.Peer;
import project.database.Chunk;
import project.threads.ReceiveMessage;

public class MDRChannel extends Channel implements Runnable{

    private boolean listening;

    public MDRChannel(String MDRAddr, Peer peer) throws Exception{
        super(MDRAddr, peer);

        listening = false;
    }

    public void startListening() {
        listening = true;
    }

    public void stopListening() {
        listening = false;
    }

    public boolean isListening() {
        return listening;
    }

    public void sendChunk(String fileID, Chunk chunk) throws IOException {
        byte[] header = super.createHeader("CHUNK", fileID, chunk.getId()).getBytes();
        byte[] chunkContent = chunk.getContent();
        byte[] putChunk = new byte[header.length + chunkContent.length];
        System.arraycopy(header, 0, putChunk, 0, header.length);
        System.arraycopy(chunkContent, 0, putChunk, header.length, chunkContent.length);
        
        DatagramPacket sendPacket = new DatagramPacket(putChunk, putChunk.length, this.address, this.portNumber);
        MulticastSocket socket = new MulticastSocket(this.portNumber);
        socket.joinGroup(this.address);
        socket.send(sendPacket);
        System.out.println("SendChunk");
    }
    
    @Override
    public void run() {
        try {
            byte[] receiveData = new byte[66000];
            
            MulticastSocket socket = new MulticastSocket(this.portNumber);
            socket.joinGroup(this.address);
            
            while(true) {
                if(listening)
                {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);
                    this.peer.getExec().execute(new ReceiveMessage(this.peer, receivePacket));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}