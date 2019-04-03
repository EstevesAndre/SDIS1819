package project.channels;

import java.net.InetAddress;
import java.net.MulticastSocket;

import project.service.Peer;

public class Channel {
    protected InetAddress address;
    protected int portNumber;
    protected MulticastSocket socket;

    protected short peerID;
    protected float version;
    protected Peer peer;

    public Channel(String MDBAddr, Peer peer) throws Exception{
        String multicastHostName = MDBAddr.split(" ")[0];
        String mcastPort = MDBAddr.split(" ")[1];

        this.address = InetAddress.getByName(multicastHostName);
        this.portNumber = Integer.parseInt(mcastPort);

        // Join multicast group
        this.socket = new MulticastSocket(this.portNumber);
        this.socket.joinGroup(this.address);

        this.peer = peer;
        this.peerID = peer.getID();
        this.version = peer.getVersion();
    }

    public String createHeader(String messageType, String fileID, int chunkNumber, int replicationDegree){
        return messageType + " " + this.version + " " + this.peerID + " " + fileID + " " + chunkNumber + " " + replicationDegree + "\r\n\r\n";
    }

}