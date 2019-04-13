package project.channels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.TimeUnit;
import java.lang.Runnable;
import java.util.regex.*;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;

import project.database.Chunk;
import project.service.Peer;
import project.threads.ReceiveMessage;


public class MDBChannel extends Channel implements Runnable{
    public MDBChannel(String MDBAddr, Peer peer) throws Exception{
        super(MDBAddr, peer);
    }

    public void sendPutChunk(String fileID, Chunk chunk, int rd) throws IOException {
        byte[] header = super.createHeader("PUTCHUNK", fileID, chunk.getId(), rd).getBytes();
        byte[] putChunk = new byte[header.length + chunk.getSize()];
        System.arraycopy(header, 0, putChunk, 0, header.length);
        System.arraycopy(chunk.getContent(), 0, putChunk, header.length, chunk.getSize());
        
        DatagramPacket sendPacket = new DatagramPacket(putChunk, putChunk.length, this.address, this.portNumber);

        MulticastSocket socket = new MulticastSocket(this.portNumber);
        socket.joinGroup(this.address);
        socket.send(sendPacket);
        socket.close();
    }

    public void verifyRDinitiated(String fileID, Chunk chunk, int rd) throws Exception {
        int attempts = 1;
        int waitingTime = 1000;
        AbstractMap.SimpleEntry<String, Integer> putchunk = new AbstractMap.SimpleEntry<String, Integer>(fileID, chunk.getId());

        if(!this.peer.hasInitiatedChunk(putchunk)) {return;}

        while(attempts < 5) {
            System.out.println("Attempt: " + attempts);
            Thread.sleep(waitingTime);
            if(!this.peer.verifyRDInitiated(putchunk)){
                sendPutChunk(fileID, chunk, rd);
            }
            else {
                System.out.println("Backup the chunk(" + chunk.getId() + ") with the desired RD(" + rd + ")");
                return;
            }
            waitingTime *= 2;
            attempts++;
        }
        
        Thread.sleep(500);
        System.out.println("Couldn't backup the chunk(" + chunk.getId() + ") with the desired RD(" + rd + ")");
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