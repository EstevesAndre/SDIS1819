package project.threads;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Random;
import java.lang.Thread;

import project.channels.MDBChannel;
import project.database.Chunk;
import project.service.Peer;

public class ReceiveMessage implements Runnable{

    private DatagramPacket packet;
    private Peer peer;

    public ReceiveMessage(Peer peer, DatagramPacket packet) {
        this.peer = peer;
        this.packet = packet;
    }

    @Override
    public void run() {
        try {
            String[] received = new String(this.packet.getData(), 0, this.packet.getLength()).trim().split("\\s+");

            switch(received[0])
            {
                case "STORED": //ex: STORED version(1.0) peerID(12) fileID chunkID RD
                    this.peer.receiveStored(received);
                break;
                case "PUTCHUNK":
                    Thread.sleep((long)(Math.random() * 1000)%400);
                    this.peer.receivePutChunk(this.packet);
                break;
                case "DELETE":
                    this.peer.receiveDelete(received);
                default:
                break;
                case "GETCHUNK":
                    Thread.sleep((long)(Math.random() * 1000)%400);
                    this.peer.receiveGetChunk(received);
                break;
                case "CHUNK":
                    System.out.println("HERE");
                    this.peer.receiveChunk(this.packet);
                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}