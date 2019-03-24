package project.channels;

import java.net.InetAddress;
import java.net.MulticastSocket;

public class Channel {
    protected InetAddress address;
    protected int portNumber;
    protected MulticastSocket socket;

    protected short peerID;
    protected float version;

    public Channel(String MDBAddr, short peerId, float version) throws Exception{
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

}