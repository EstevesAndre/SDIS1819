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
            String[] received = new String(this.packet.getData(), 0, this.packet.getLength()).trim().split(" ");

            switch(received[0])
            {
                case "STORED": //ex: STORED version(1.0) peerID(12) fileID chunkID RD
                    receivedStored(received);
                break;
                case "PUTCHUNK":
                    Thread.sleep((long)(Math.random() * 1000)%400);
                    this.peer.receivePutChunk(this.packet);
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

    private void receivedStored(String[] message) {
        
        // verifies if is not the send message peer
        if(Integer.parseInt(message[2]) == this.peer.getID())
            return;

        
        //for(int i = 0; i < message.length; i++)
        //    System.out.println(message[i]);
    }
}