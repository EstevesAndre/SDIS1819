package project.channels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import project.database.Chunk;

public class MDBChannel {
    private InetAddress address;
    private int portNumber;
    private MulticastSocket socket;

    private short peerID;
    private float version;

    public MDBChannel(String MDBAddr, short peerId, float version) throws Exception{
        String multicastHostName = MDBAddr.split(" ")[0];
        String mcastPort = MDBAddr.split(" ")[1];

        this.address = InetAddress.getByName(multicastHostName);
        this.portNumber = Integer.parseInt(mcastPort);

        // Join multicast group
        this.socket = new MulticastSocket(this.portNumber);
        this.socket.joinGroup(this.address);

        this.peerID = peerId;
        this.version = version;
    }

    public String createHeader(String messageType, String fileID, int chunkNumber, int replicationDegree){
        return messageType + " " + this.version + " " + this.peerID + " " + fileID + " " + chunkNumber + " " + replicationDegree + "\r\n \r\n";
    }

    public void sendPutChunk(String fileID, Chunk chunk, int rd) throws Exception {
        byte[] header = createHeader("PUTCHUNK", fileID, chunk.getId(), rd).getBytes();
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
    
}