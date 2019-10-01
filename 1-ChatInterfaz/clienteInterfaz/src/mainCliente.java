
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class mainCliente 
{
    String serverAddress;
    Scanner in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    JTextArea messageArea = new JTextArea(16,50);
    
    public mainCliente(String serverAddress)
    {
        this.serverAddress = serverAddress;
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, BorderLayout.SOUTH);
        frame.getContentPane().add(new JScrollPane(messageArea),BorderLayout.CENTER);
        frame.pack();
        
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        });
    }
    
    private String getname(){
        return JOptionPane.showInputDialog(frame, "Choose a screen name:","Screen name selection", JOptionPane.PLAIN_MESSAGE);
    }
    
    private void run()
    {
        try {
            Socket socket = new Socket(serverAddress, 59001);
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(),true);
            
            while(in.hasNextLine())
            {
                String line = in.nextLine();
                if(line.startsWith("SUBMITNAME"))
                {
                    out.println(getname());
                } else if (line.startsWith("NAMEACCEPTED"))
                {
                    this.frame.setTitle("Chatter - " + line.substring(13));
                    textField.setEditable(true);
                }else if(line.startsWith("Message"))
                {
                    messageArea.append(line.substring(8) + "\n");
                }
                
                        
            }
            
        }catch(IOException mu)
        {
            System.out.println("Error en el chat " + mu.toString());
            System.exit(1);
        }finally 
        {
            frame.setVisible(false);
            frame.dispose();
        }
    }
    public static void main(String[] args)
    {
        if (args.length != 1)
        {
            System.err.println("Pass the server ip as the source argument");
            return;
        }
        mainCliente client = new mainCliente(args[0]);
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        try {
            client.run();
        } catch (Exception en) {
            System.out.println("Error al ejecutar el frame" + en.toString());
            System.exit(1);
        }
    }
    
}
