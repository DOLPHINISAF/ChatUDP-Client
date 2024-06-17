import javax.swing.*;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class Server {
    private Inet4Address server_addr;
    private int SERVER_PORT;
    private String CLIENT_NAME;

    private final int CLIENT_PORT = 20000;

    private DatagramSocket socket;
    private DatagramPacket packet;


    Server(){
        SERVER_PORT = 0;
    }
    //folosim metoda pentru a verifica daca IP-ul / portul duc la un server si informam serverul de conexiune
    public void Conectare() {

        boolean isTryingtoConnect = true;

        while (isTryingtoConnect) {

            //[0]- IP server; [1]- PORT server; [2]- nume ales client
            JTextField[] serverInfo = DrawServerInput();// mai intai luam datele introduse pentru conectare

            if(serverInfo != null) {
                try {

                    String SERVER_IP = serverInfo[0].getText();
                    server_addr = (Inet4Address) Inet4Address.getByName(SERVER_IP);

                    this.SERVER_PORT = Integer.parseInt(serverInfo[1].getText());
                    this.CLIENT_NAME = serverInfo[2].getText();

                    socket = new DatagramSocket(CLIENT_PORT,Inet4Address.getLocalHost());

                    isTryingtoConnect = false;

                    if(TryToConnect()) {
                        System.out.println("Connected to: " + server_addr.toString() + ":" + SERVER_PORT + " using name: " + CLIENT_NAME);
                    }
                    else{
                        JOptionPane.showMessageDialog(null, "Serverul nu a raspuns mesajului de conectare!", "Server not responding", JOptionPane.ERROR_MESSAGE);
                    }


                } catch (SocketException ex) {
                    JOptionPane.showMessageDialog(null, "Datele nu corespund cu , reintroduceti-le!", "DATE GRESITE!", JOptionPane.ERROR_MESSAGE);
                }
                catch (UnknownHostException ignored){}

            }
            else{
                isTryingtoConnect = false;
            }
        }
    }

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

        if(ip.getText().isEmpty() || port.getText().isEmpty() ||numeChat.getText().isEmpty()){
            return null;
        }

        return new JTextField[]{ip,port,numeChat};
    }

    private boolean TryToConnect(){


        //asamblam pachetul pentru a informa serverul de dorinta de a ne conecta si il trimitem.
        //255 max
        byte[] sgn = {-1,-1,-1,-1};

        byte[] name_buf = CLIENT_NAME.getBytes();

        byte name_length = Byte.parseByte(String.valueOf(name_buf.length));

        byte[] databuf = new byte[32];
        //adaugam semnatura pachetului de conectare
        System.arraycopy(sgn,0,databuf,0,sgn.length);
        //adaugam numele in pachet
        databuf[4] = name_length;

        System.arraycopy(name_buf,0,databuf,5,name_buf.length);

        DatagramPacket connect_packet = new DatagramPacket(databuf, 32, server_addr, SERVER_PORT);

        try {
            socket.send(connect_packet);
            System.out.println("A fost trimis pachetul de conectare la server!");
        }
        catch (IOException ioe){
            System.out.println("Eroare in a trimite pachetul pentru conectare la server!");
            return false;
        }

        //dupa ce trimitem pachetul asteptam un raspuns al serverului pentru a ne confirma conectare
        boolean waitConfirmationPacket = true;
        while(waitConfirmationPacket){

            DatagramPacket connectionConfirmed = new DatagramPacket(new byte[4], 4);

            try {
                socket.receive(connectionConfirmed);

                if(Arrays.equals(connectionConfirmed.getData(), new byte[]{-1,-1,-1,-1})){
                    System.out.println("A fost primit pachetul de confirmare!");
                    return true;
                }
            }
            catch (IOException ignored){
                waitConfirmationPacket = false;
            }
        }
        System.out.println("Nu a fost primit packetul de confirmare a conectarii la server!");
        return false;
    }

    public void Close() {
        if(socket != null) {
            socket.close();
            System.out.println("Closed socket!");
        }
    }
}
