import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Objects;
import java.util.logging.Logger;

public class ConnectionManager extends Thread{

    public volatile DatagramSocket socket;
    public volatile DatagramPacket incomingPack;

    public volatile boolean receivingPackets;
    public volatile boolean packetReceived;

    ConnectionManager(){
        super();
        //variable initialisation
        int CLIENT_PORT = 20000;
        incomingPack = new DatagramPacket(new byte[32], 32);
        packetReceived = false;
        receivingPackets = true;
        //creating a network socket
        try {
            socket = new DatagramSocket(CLIENT_PORT);
        }
        catch (IOException e){
            Logger.getLogger(Client.class.getName()).warning("Caught exception when creating socket. " + e.getMessage());
        }
    }
    /**
     * when thread is started it continuosly receives packets from the newtwork until client is closed
     */
    public void run(){
        Logger.getLogger(Client.class.getName()).info("Started thread");
        while(receivingPackets){
            GetIncomingPackages();
        }
        Logger.getLogger(Client.class.getName()).info("Stopped thread");
        socket.close();
    }
    public void GetIncomingPackages(){
        //if we already received a packet we wait until it was read and used
        if(packetReceived) return;

        try {
            if(Objects.isNull(socket))
                throw new IOException("Socket is null!");
            socket.receive(incomingPack);
            packetReceived = true;
        }
        catch (IOException e) {
            Logger.getLogger(Client.class.getName()).warning("Caught exception at thread. " + e.getMessage());
        }
    }
}
