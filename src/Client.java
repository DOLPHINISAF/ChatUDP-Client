import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

/**
 * @author Nedelescu Catalin
 * @version 1.0
 */
public class Client {
    /** enum used to ID every sent/received packet */
    private enum PacketType{
        CONNECTREQUEST(0),
        DISCONNECT(1),
        MESSAGE(2),
        ADDUSER(3),
        REMOVEUSER(4),
        CONNECTCONFIRM(5),
        UNKNOWN(6);

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
            return UNKNOWN;
        }
        public static byte toInt(PacketType type) {
            return type.value;
        }
    }
    private String CLIENT_NAME;
    private Inet4Address server_addr;
    private int SERVER_PORT;
    /** a class used to interact with the network in a separate thread
     */
    public ConnectionManager manager;

    private boolean clientIsRunning;
    /**
     * frame/button/... needed to create the window of the chat menu/ input field/ chat box
     */
    JFrame frame;
    JButton message;
    JButton quit;
    JList userList;
    DefaultListModel listmodel;
    Vector<String> users;
    Vector<ChatWindow> chatWindows;

    public Logger logger = Logger.getLogger(Client.class.getName());
    Handler fileHandler;

    Client(){
        clientIsRunning = true;
        SERVER_PORT = 0;
        manager = new ConnectionManager();
        manager.start();
        users = new Vector<>(16);
        chatWindows = new Vector<>(8);

        logger.setLevel(Level.FINE);
        try {
            fileHandler = new FileHandler("./logs.txt");
            fileHandler.setFormatter(new SimpleFormatter());

            logger.addHandler(fileHandler);
        }
        catch (IOException ignored){}
    }

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
                switch (PacketType.fromInt(data[0])) {
                    case PacketType.MESSAGE    -> client.HandleReceivedMessage(packet);
                    case PacketType.ADDUSER    -> client.HandleAddUser(packet);
                    case PacketType.REMOVEUSER -> client.HandleRemoveUser(packet);
                    case PacketType.UNKNOWN -> client.logger.info("Pachet necunoscut primit");
                }
                client.manager.packetReceived = false;
           }

           if(!client.chatWindows.isEmpty()) {
               for (int i = 0; i < client.chatWindows.size(); i++) {

                   if (!client.chatWindows.get(i).messageToSend.isEmpty()) {
                       client.SendMessage(client.chatWindows.get(i).connectionName, client.chatWindows.get(i).messageToSend);
                       client.chatWindows.get(i).messageToSend = "";
                   }
                   if(!client.chatWindows.get(i).enabled) {
                       client.chatWindows.remove(i);
                       client.logger.info("Fereastra chat inchisa");
                   }
               }
           }
       }
       client.Disconnect();

       client.Close();

    }
    /**all 3 used to establish connection with a server*/
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
                    logger.info("Conectat la server folosind numele: " + CLIENT_NAME);
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
    /**
     * displays a simple input field and returns an array of the entered values
     * @return an {@code JTextField} value
     */
    private JTextField[] DrawServerInput(){
        JTextField ip = new JTextField();
        JTextField port = new JTextField();
        JTextField numeChat = new JTextField();
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

        if(ip.getText().isEmpty() || port.getText().isEmpty() || numeChat.getText().isEmpty()){
            return null;
        }

        return new JTextField[]{ip,port,numeChat};
    }
    /**
     * sends a connectionrequest packet to the server and waits 1 second
     * for a reply before either succesfully connecting or failing
     */
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
            logger.info("A fost trimis pachetul de conectare la server");
        }
        catch (IOException ioe){
            logger.warning("Eroare in a trimite pachetul pentru conectare la server!");
            return false;
        }


        //dupa ce trimitem pachetul asteptam un raspuns al serverului pentru a ne confirma conectare
        try{
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        if(!manager.packetReceived){
            logger.info("Nu a fost primit packetul de confirmare a conectarii la server");
            return false;
        }

        DatagramPacket connectionConfirmed = manager.incomingPack;
        if (connectionConfirmed.getData()[0] == PacketType.CONNECTCONFIRM.value) {
            logger.info("A fost primit pachetul de confirmare");
            manager.packetReceived = false;
            return true;
        }

        return false;
    }
    public void DrawChatMenu(){


        frame = new JFrame("Menu");
        quit = new JButton("Disconnect");
        message = new JButton("Message");
        listmodel = new DefaultListModel<String>();
        userList = new JList(listmodel);
        userList.setLayoutOrientation(JList.VERTICAL);
        userList.setVisibleRowCount(3);

        GridLayout layout = new GridLayout(3,1,0,20);

        JScrollPane userlistscrollpane = new JScrollPane(userList);

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

        frame.setLayout(layout);
        frame.setLocation(200,300);
        frame.add(userlistscrollpane);
        frame.add(message);
        frame.add(quit);
        frame.pack();
        frame.setVisible(true);
    }

    private void HandleReceivedMessage(DatagramPacket packet){
        logger.info("A fost primit un mesaj");
        byte[] data = packet.getData();

        String messageSrcName = new String(data,2,data[1]);
        boolean chatwindowfound = false;
        for(ChatWindow i : chatWindows){
            if(i.connectionName.equals(messageSrcName)){
                i.AddMessage(GetMessageFromPacket(packet.getData()),messageSrcName);
                chatwindowfound = true;
            }
        }

        //if the user doesn't currently chat with the user that sent him a message we create the window
        if(!chatwindowfound){
            chatWindows.add(new ChatWindow(messageSrcName));
            HandleReceivedMessage(packet);
        }
    }
    private void HandleAddUser(DatagramPacket packet){
        //structura unui pachet de tip remove user trebuie sa fie:
        //data[0]- tipul de pachet
        //data[1]- lungimea numelui de adaugat
        //data[2]- inceputul numelui
        byte[] data = packet.getData();

        String name = new String(data,2,data[1]);

        listmodel.addElement(name);
        users.add(name);
    }
    private void HandleRemoveUser(DatagramPacket packet){
        //structura unui pachet de tip remove user trebuie sa fie:
        //data[0]- tipul de pachet
        //data[1]- lungimea numelui de sters
        //data[2]- inceputul numelui
        String removedUserName = new String(packet.getData(),2,packet.getData()[1]);
        for(int i = 0; i < users.size(); i++){
            if(users.get(i).equals(removedUserName)){
                listmodel.removeElementAt(i);
                users.remove(i);
            }
        }

    }

    private void SendMessage(String destName, String message){
        byte[] data = new byte[32];

        //structura unui pachet de tip message trebuie sa fie:
        //data[0]- tipul de pachet
        //data[1]- lungimea numelui destinatie
        //data[2]- inceputul numelui destinatie
        //data[sfarsit nume + 1]- lungimea mesajului
        //data[sfarsit nume + 2]- mesaj transmis

        data[0] = PacketType.toInt(PacketType.MESSAGE);
        data[1] = (byte) destName.length();
        System.arraycopy(destName.getBytes(),0,data,2,destName.length());//inseram numele
        data[2 + destName.length()] = (byte) message.length();
        System.arraycopy(message.getBytes(),0,data,3 + destName.length(),message.length());

        DatagramPacket packet = new DatagramPacket(data,data.length,server_addr,SERVER_PORT);
        try {
            manager.socket.send(packet);
            logger.info("Mesaj trimis catre " + destName);
        }
        catch (IOException e){
            logger.warning("Eroare la trimitere mesaj");
        }
    }

    private static String GetMessageFromPacket(byte[] data){
        final int messageLengthOffset = data[1] + 2;
        final int messageOffset = messageLengthOffset + 1;

        return new String(data,messageOffset,messageLengthOffset);
    }

    private void Disconnect(){
        DatagramPacket disconnectPacket = new DatagramPacket(new byte[]{PacketType.toInt(PacketType.DISCONNECT)},1,server_addr,SERVER_PORT);
        try {
            manager.socket.send(disconnectPacket);
            logger.info("Deconectat de la server");
        }
        catch (IOException ignored){}
    }
    public void Close() {
        logger.info("Inchidere client");
        manager.receivingPackets = false;
        if(frame != null)
            frame.dispose();
    }
}
