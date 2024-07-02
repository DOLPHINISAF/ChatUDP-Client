import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChatWindow extends Thread {

    JFrame frame;
    JTextArea chatArea;
    JTextArea inputArea;
    JButton send;
    public volatile String connectionName;

    public volatile String messageToSend;
    ChatWindow(String name){
        connectionName = name;
        frame = new JFrame("Chat with " + name);
        chatArea = new JTextArea("");
        inputArea = new JTextArea("");
        send = new JButton("Send Message");
        DrawChatWindow();

    }

    private void DrawChatWindow(){
        JPanel inputPanel = new JPanel(new BorderLayout(15,0));

        BorderLayout layout = new BorderLayout(0,20);

        chatArea.setEditable(false);
        chatArea.setBackground(Color.GRAY);
        chatArea.setCaretColor(Color.GRAY);
        chatArea.setColumns(45);
        chatArea.setRows(15);
        frame.setResizable(false);
        frame.setLayout(layout);
        frame.setLocation(300,300);
        inputArea.setRows(5);
        inputArea.setColumns(30);

        send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                messageToSend = inputArea.getText();
                chatArea.append("Me:" + messageToSend + "\n");
            }
        });

        JScrollPane inputtextscollpane = new JScrollPane(inputArea);

        inputPanel.add(inputtextscollpane,BorderLayout.CENTER);
        inputPanel.add(send,BorderLayout.EAST);

        frame.add(chatArea,BorderLayout.CENTER);
        frame.add(inputPanel,BorderLayout.SOUTH);

        frame.pack();
        frame.setVisible(true);
    }

    public void run(){

    }

    public void AddMessage(String messsage, String srcName){
        chatArea.append(srcName + " :" + messsage + "\n");
    }
}
