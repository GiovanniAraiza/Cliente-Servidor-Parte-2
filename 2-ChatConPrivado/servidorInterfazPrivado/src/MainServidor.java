import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
public class MainServidor 
{
    //estaticos porque los hilos compartiran informacion y son nombres que no se pueden repetir
    private static Set<String> names = new HashSet<>();
   //puros escritores diferentes
    private static Set<PrintWriter> writers = new HashSet<>();
    private static Map<String,PrintWriter> writers2 = new HashMap<>();
    
    public static void main(String[] args)
    {
        System.out.println("The chat server is running...");
        ExecutorService pool = Executors.newFixedThreadPool(500);
        try (ServerSocket listener = new ServerSocket(59001)) {
            while (true)
            {
                pool.execute(new Handler(listener.accept()));
            }
            
            
        }catch(Exception e)
        {
            System.out.println("Error al crear el pool" + e.toString());
            System.exit(1);
        }
    }
    //los hilos siempre implementan runnable
    private static class Handler implements Runnable
    {
            private String name;
            private Socket socket;
            private Scanner in;
            private PrintWriter out;
            
            //intentar que sea lo mas rapido posible porque corre en el main
            public Handler(Socket socket)
            {
                this.socket = socket;
            }
            public void run()
            {
                try {
                    in = new Scanner(socket.getInputStream());
                    out = new PrintWriter(socket.getOutputStream(),true);

                    while (true)
                    {
                        out.println("SUBMITNAME");
                        name = in.nextLine();
                        if (name == null || name.contains(" ") || name.length()==0)
                        {
                            return;
                        }
                        synchronized(names)
                        {
                            if (!names.contains(name))
                            {
                                names.add(name);
                                writers2.put(name, out);
                                break;
                            }
                        }
                    }

                    out.println("NAMEACCEPTED " + name);
                    for(PrintWriter writer : writers)
                    {
                        writer.println("Message " + name + " has joined");
                    }
                    writers.add(out);

                    while (true)
                    {
                        String input = in.nextLine();
                        if(input.toLowerCase().startsWith("/"))
                        {
                            if(input.toLowerCase().startsWith("/quit"))
                            {
                                return;
                            }
                            try {
                                if(input.toLowerCase().startsWith("/") && names.contains(input.substring(1,input.indexOf(" "))))
                                {
                                    String nombreRecibir = input.substring(1,input.indexOf(" "));
                                    writers2.get(nombreRecibir).println("Message " + name + ": " + input.substring(input.indexOf(" ")) + "   *privado*");
                                    writers2.get(name).println("Message " + name + ": " + input.substring(input.indexOf(" ")) + "   *privado-a-" + nombreRecibir + "*");
                                }
                            } catch (Exception e) {
                            }
                        }
                        else
                        {
                            for(PrintWriter writer : writers)
                            {
                                writer.println("Message " + name + ": " + input);
                            }
                        }
                    }


                }catch(Exception e){
                    System.out.println(e);
                }finally
                {
                    if (out != null)
                    {
                        writers.remove(out);
                    }
                    if (name != null)
                    {
                        System.out.println(name + " is leaving");
                        names.remove(name);
                        for(PrintWriter writer : writers)
                        {
                            writer.println("Message " + name + " has left");
                        }
                    }
                    try {socket.close();} catch (IOException e) {}
                }
            }
        
    }
    
    
}
