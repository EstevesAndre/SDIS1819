package project.threads;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Random;
import java.lang.Thread;

import project.channels.MDBChannel;
import project.database.Chunk;
import project.service.Peer;

public class ReceiveMessage implements Runnable{

    private byte[] packet;
    private Peer peer;

    public ReceiveMessage(Peer peer, byte[] packet) {
        this.peer = peer;
        this.packet = packet;
    }

    @Override
    public void run() {
        try {
            int splitIndex = 0;
            for(int i = 0; i < this.packet.length; i++) {
                if(this.packet[i] == 13 && this.packet[i+1] == 10 && this.packet[i+2] == 13 && this.packet[i+3] == 10){
                    splitIndex = i + 4;
                    break;
                }
            }

            String[] received = new String(this.packet, 0, splitIndex).trim().split("\\s+");
            
            switch(received[0])
            {
                case "STORED": //ex: STORED version(1.0) peerID(12) fileID chunkID RD
                    this.peer.receiveStored(received);
                    break;
                case "PUTCHUNK":
                    this.peer.receivePutChunk(this.packet);
                    Thread.sleep((long)(Math.random() * 1000)%400);
                    break;
                case "DELETE":
                    this.peer.receiveDelete(received);
                    break;
                case "GETCHUNK":
                    this.peer.receiveGetChunk(received);
                    Thread.sleep((long)(Math.random() * 1000)%400);
                    break;
                case "CHUNK":
                    this.peer.receiveChunk(this.packet);
                    break;
                case "REMOVED":
                    this.peer.receiveRemoved(received);
                    break;
                default:
                    break;
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}