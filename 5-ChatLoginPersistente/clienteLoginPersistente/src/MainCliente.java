import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
public class MainCliente 
{
    String serverAddress;
    Scanner in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    JTextArea messageArea = new JTextArea(16,50);
    
    public MainCliente(String serverAddress)
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
    private String getpass(){
        String pass = "";
        //se crea una caja
        Box caja = Box.createVerticalBox();
        //se crea un label
        JLabel j1 = new JLabel("Password: ");
        //se crea un panel
        JPanel j1p = new JPanel();
        //se crea un layout con rango en x
        j1p.setLayout(new BoxLayout(j1p, BoxLayout.X_AXIS));
        //se agrega el label al panel
        j1p.add(j1);
        //se le agrega pegamento horizontal para alineacion y se añade al panel
        j1p.add(Box.createHorizontalGlue());
        //se añade a la caja
        caja.add(j1p);
        
        //se crea un campo de password se le pide focus y se añade a la caja
        JPasswordField jpf = new JPasswordField(4);
        jpf.requestFocusInWindow();
        caja.add(jpf);
        //para obtener el focus en el password a traves del confirmdialog
        jpf.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorRemoved(AncestorEvent event) {}

            @Override
            public void ancestorMoved(AncestorEvent event) {}

            @Override
            public void ancestorAdded(AncestorEvent event) {
                event.getComponent().requestFocusInWindow();
            }
        });
        int opc = JOptionPane.showConfirmDialog(frame,caja,"Screen password selection", JOptionPane.PLAIN_MESSAGE,-1,null);
        if(opc == 0)
        {
            char[] contra = jpf.getPassword();
            pass = new String(contra);
        }
        return pass;
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
                    
                } else if(line.startsWith("SUBMITPASS"))//se agrega la opcion de pedir la contraseña
                {
                    out.println(getpass());
                }else if (line.startsWith("NAMEACCEPTED"))
                {
                    this.frame.setTitle("Chatter - " + line.substring(13));
                    textField.setEditable(true);
                }else if(line.startsWith("Message"))
                {
                    messageArea.append(line.substring(8) + "\n");
                }
                
                        
            }
        } catch(IOException f)
        {
            System.out.println("error en el chat" + f.toString());
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
        MainCliente client = new MainCliente(args[0]);
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        try {
            client.run();
        } catch (Exception e) {
            System.out.println("Error al correr el frame" + e.toString());
            System.exit(0);
        }
        
    }
}
