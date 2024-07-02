import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

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
            System.out.println("Socket created");
        }
        catch (IOException e){
            System.out.println("Caught exception when creating socket. " + e.getMessage());
        }
    }

    public void run(){
        System.out.println("Started thread");
        while(receivingPackets){
            GetIncomingPackages();
        }
        System.out.println("Stopped thread");
        socket.close();
    }

    public void GetIncomingPackages(){
        //if we already received a packet we wait until it was read and used
        if(packetReceived) return;

        try {
            socket.receive(incomingPack);
            packetReceived = true;
        }
        catch (IOException e) {
            System.out.println("Caught exception at thread. " + e.getMessage());
        }

    }
}
