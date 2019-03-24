package project.channels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.TimeUnit;
import java.lang.Runnable;

import project.database.Chunk;

public class MDBChannel extends Channel implements Runnable{
    public MDBChannel(String MDBAddr, short peerId, float version) throws Exception{
        super(MDBAddr, peerId, version);
    }

    public void sendPutChunk(String fileID, Chunk chunk, int rd) throws Exception {
        byte[] header = super.createHeader("PUTCHUNK", fileID, chunk.getId(), rd).getBytes();
        byte[] chunkContent = chunk.getContent();

        byte[] putChunk = new byte[header.length + chunkContent.length];
        System.arraycopy(header, 0, putChunk, 0, header.length);
        System.arraycopy(chunkContent, 0, putChunk, header.length, chunkContent.length);
        
		DatagramPacket sendPacket = new DatagramPacket(putChunk, putChunk.length, this.address, this.portNumber);
		this.socket.send(sendPacket);
    }

    public void receivePutChunk() throws IOException{
        byte[] receiveData = new byte[66000];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, 66000);

        this.socket.receive(receivePacket);

        String[] received = new String(receivePacket.getData(), 0, receivePacket.getLength()).split("\r\n \r\n");

        System.out.println("RECEIVED HEADER: " + received[0]);

    }

    @Override
    public void run() {
        try {
            while(true) {
                System.out.println("Executing: MDB");
                TimeUnit.SECONDS.sleep(2); 
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
}