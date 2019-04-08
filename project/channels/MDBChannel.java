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
        byte[] chunkContent = chunk.getContent();
      
        byte[] putChunk = new byte[header.length + chunkContent.length];
        System.arraycopy(header, 0, putChunk, 0, header.length);
        System.arraycopy(chunkContent, 0, putChunk, header.length, chunkContent.length);
        
        DatagramPacket sendPacket = new DatagramPacket(putChunk, putChunk.length, this.address, this.portNumber);
        MulticastSocket socket = new MulticastSocket(this.portNumber);
        socket.joinGroup(this.address);
        socket.send(sendPacket);
    }

    public void verifyRDinitiated(String fileID, Chunk chunk, int rd) throws Exception {
        int attempts = 1;
        int waitingTime = 1000;
        Map.Entry<String, Integer> putchunk = new AbstractMap.SimpleEntry<String, Integer>(fileID, chunk.getId());

        if(!this.peer.hasInitiatedChunk(putchunk)) {return;}

        while(attempts < 5) {
            Thread.sleep(waitingTime);
            if(!this.peer.verifyRDInitiated(putchunk)){
                sendPutChunk(fileID, chunk, rd);
            }
            else {
                return;
            }
            waitingTime *= 2;
            attempts++;
        }
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