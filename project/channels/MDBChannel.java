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

import project.database.Chunk;
import project.service.Peer;

public class MDBChannel extends Channel implements Runnable{
    public MDBChannel(String MDBAddr, Peer peer) throws Exception{
        super(MDBAddr, peer);
    }

    public void sendPutChunk(String fileID, Chunk chunk, int rd) throws IOException {
        byte[] header = super.createHeader("PUTCHUNK", fileID, chunk.getId(), rd).getBytes();
        byte[] chunkContent = chunk.getContent();
        //System.out.println("sendPutChunk");
        byte[] putChunk = new byte[header.length + chunkContent.length];
        System.arraycopy(header, 0, putChunk, 0, header.length);
        System.arraycopy(chunkContent, 0, putChunk, header.length, chunkContent.length);
        
		DatagramPacket sendPacket = new DatagramPacket(putChunk, putChunk.length, this.address, this.portNumber);
		this.socket.send(sendPacket);
    }

    @Override
    public void run() {
        try {
            byte[] receiveData = new byte[66000];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, 66000);

            while(true) {
                System.out.println("Reading from MDBChannel");
                this.socket.receive(receivePacket);
                this.peer.receivePutChunk(receivePacket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}