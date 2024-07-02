import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Client {
    private enum PacketType{
        CONNECTREQUEST(0),
        DISCONNECT(1),
        MESSAGE(2),
        ADDUSER(3),
        REMOVEUSER(4),
        CONNECTCONFIRM(5);

        private final byte value;

        PacketType(int val){
            this.value = (byte)val;
        }

        public static PacketType fromInt(byte value) {
            for (PacketType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return null;
        }
        public static byte toInt(PacketType type) {
            return type.value;
        }
    }
    private String CLIENT_NAME;
    private Inet4Address server_addr;
    private int SERVER_PORT;

    public ConnectionManager manager;

    private boolean clientIsRunning;

    JFrame frame;
    JButton message;
    JButton quit;
    JList userList;

    Vector<String> users;
    Vector<ChatWindow> chatWindows;

    public static void main(String[] args) {
        Client client = new Client();
        if(!client.Conectare()) {
            client.Close();
            return;
        }
        client.DrawChatMenu();

       while(client.clientIsRunning){

           if(client.manager.packetReceived){

               DatagramPacket packet = client.manager.incomingPack;
               byte[] data = packet.getData();

               //we get the first byte of the packet's data to find what kind of packet we have
               switch (PacketType.fromInt(packet.getData()[0])) {
                   case PacketType.MESSAGE    -> client.HandleReceivedMessage(packet);
                   case PacketType.ADDUSER    -> client.HandleAddUser(packet);
                   case PacketType.REMOVEUSER -> client.HandleRemoveUser(packet);
                   case null -> {}
                   default -> {System.out.println("Received unknown packet");}
               }

               for(ChatWindow chats : client.chatWindows){
                   if(!chats.messageToSend.isEmpty()){
                       client.SendMessage(chats.connectionName, chats.messageToSend);
                       chats.messageToSend = "";
                   }
               }

               client.manager.packetReceived = false;
           }


       }
       client.Disconnect();

       client.Close();

    }

    Client(){
        clientIsRunning = true;
        SERVER_PORT = 0;
        manager = new ConnectionManager();
        manager.start();
        users = new Vector<String>(16);
        chatWindows = new Vector<>(4);
    }
    //all 3 used to establish connection with a server
    public boolean Conectare() {
        //[0]- IP server; [1]- PORT server; [2]- nume ales client
        JTextField[] serverInfo = DrawServerInput();// mai intai luam datele introduse pentru conectare
        if(serverInfo != null) {
            try {
                String SERVER_IP = serverInfo[0].getText();
                this.server_addr = (Inet4Address) Inet4Address.getByName(SERVER_IP);
                this.SERVER_PORT = Integer.parseInt(serverInfo[1].getText());
                this.CLIENT_NAME = serverInfo[2].getText();
                if(TryToConnect()) {
                    System.out.println("Connected to server using name: " + CLIENT_NAME);

                    return true;
                }
                else{
                    JOptionPane.showMessageDialog(null, "Serverul nu a raspuns mesajului de conectare!", "Server not responding", JOptionPane.ERROR_MESSAGE);
                }
            }
            catch (UnknownHostException e){
                return false;
            }
        }
        return false;
    }
    private JTextField[] DrawServerInput(){
        JTextField ip = new JTextField();
        JTextField port = new JTextField();
        JTextField numeChat = new JTextField();
        ip.setText("192.168.1.104");
        port.setText("1337");
        numeChat.setText("name");
        final JComponent[] optiuni = new JComponent[]{
                new JLabel("IP:"),
                ip,
                new JLabel("PORT:"),
                port,
                new JLabel("Nume"),
                numeChat
        };

        int result = JOptionPane.showConfirmDialog(null,optiuni,"Introdu informatiile necesare conectarii la un server!",JOptionPane.OK_CANCEL_OPTION);
        if(result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION){
            return null;
        }

        if(ip.getText().isEmpty() || port.getText().isEmpty() ||numeChat.getText().isEmpty()){
            return null;
        }

        return new JTextField[]{ip,port,numeChat};
    }
    private boolean TryToConnect(){


        //asamblam pachetul pentru a informa serverul de dorinta de a ne conecta si il trimitem.
        //255 max
        byte packettype = PacketType.toInt(PacketType.CONNECTREQUEST);
        byte[] name_buf = CLIENT_NAME.getBytes();
        byte name_length = (byte) name_buf.length;
        byte[] databuf = new byte[32];

        //adaugam semnatura pachetului de conectare
        databuf[0] = packettype;
        //adaugam numele in pachet
        databuf[1] = name_length;

        System.arraycopy(name_buf,0,databuf,2,name_buf.length);

        DatagramPacket connect_packet = new DatagramPacket(databuf, 32, server_addr, SERVER_PORT);

        try {
            manager.socket.send(connect_packet);
            System.out.println("A fost trimis pachetul de conectare la server!");
        }
        catch (IOException ioe){
            System.out.println("Eroare in a trimite pachetul pentru conectare la server!");
            return false;
        }


        //dupa ce trimitem pachetul asteptam un raspuns al serverului pentru a ne confirma conectare
        try{
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        if(!manager.packetReceived){
            System.out.println("Nu a fost primit packetul de confirmare a conectarii la server!");
            return false;
        }

        DatagramPacket connectionConfirmed = manager.incomingPack;
        if (connectionConfirmed.getData()[0] == PacketType.CONNECTCONFIRM.value) {
            System.out.println("A fost primit pachetul de confirmare!");
            manager.packetReceived = false;
            return true;
        }

        return false;
    }

    public void DrawChatMenu(){


        frame = new JFrame("Menu");
        quit = new JButton("Disconnect");
        message = new JButton("Message");
        userList = new JList<String>(users);

        GridLayout layout = new GridLayout(3,1,0,20);

        frame.setLayout(layout);
        frame.setLocation(200,300);


        JScrollPane userlistscrollpane = new JScrollPane(userList);

        userList.setLayoutOrientation(JList.VERTICAL);
        userList.setVisibleRowCount(3);


        message.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(userList.getSelectedValue() != null) {
                    String selectedName = userList.getSelectedValue().toString();
                    chatWindows.add(new ChatWindow(selectedName));
                }
            }
        });

        quit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clientIsRunning = false;
            }
        });


        frame.add(userlistscrollpane);
        frame.add(message);
        frame.add(quit);

        frame.pack();
        frame.setVisible(true);
    }

    private void HandleReceivedMessage(DatagramPacket packet){

        String messsageSrcName = new String(packet.getData(),2,packet.getData()[1]);

        for(ChatWindow i : chatWindows){
            if(i.connectionName.equals(messsageSrcName)){
                i.AddMessage(GetMessageFromPacket(packet.getData()),messsageSrcName);
            }
        }
    }
    private void HandleAddUser(DatagramPacket packet){
        byte[] data = packet.getData();

        String name = new String(data,2,data[1]);

        users.add(name);
        userList.updateUI();
    }
    private void HandleRemoveUser(DatagramPacket packet){
        String removedUserName = new String(packet.getData(),2,packet.getData()[1]);
            for(String i : users){
                if(i.equals(removedUserName)){
                        users.remove(i);
                        userList.updateUI();
                }
            }
    }

    private void SendMessage(String destName, String message){
        byte[] data = new byte[32];
        data[0] = PacketType.toInt(PacketType.MESSAGE);
        data[1] = (byte) destName.length();
        System.arraycopy(destName.getBytes(),0,data,2,destName.length());
        data[1 + destName.length()] = (byte) message.length();
        System.arraycopy(message.getBytes(),0,data,2 + destName.length(),message.length());

        DatagramPacket packet = new DatagramPacket(data,data.length,server_addr,SERVER_PORT);
        try {
            manager.socket.send(packet);
        }
        catch (IOException e){
            System.out.println("Failed to send message trough socket");
        }
    }


    private static String GetMessageFromPacket(byte[] data){

        return new String(data,data[1] + 2,data[1] + 1);
    }

    private void Disconnect(){
        DatagramPacket disconnectPacket = new DatagramPacket(new byte[]{PacketType.toInt(PacketType.DISCONNECT)},1,server_addr,SERVER_PORT);
        try {
            manager.socket.send(disconnectPacket);
            System.out.println("Disconnected from server");
        }
        catch (IOException ignored){}
    }
    public void Close() {
        System.out.println("Closing client");
        manager.receivingPackets = false;
        if(frame != null)
            frame.dispose();
    }
}
