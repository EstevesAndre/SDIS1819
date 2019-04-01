package project.threads;

import java.io.IOException;
import java.net.DatagramPacket;

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
            this.peer.receiveMessage(this.packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}