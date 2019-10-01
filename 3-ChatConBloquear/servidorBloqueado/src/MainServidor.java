import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
    //para los bloqueados
    private static Map<String,Set> listaBloqueados = new HashMap<>();
    
    
    
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
    
    private static class Handler implements Runnable
    {
            private String name;
            private Socket socket;
            private Scanner in;
            private PrintWriter out;
            private static Set<String> bloqueados = new HashSet<>();
            
            //intentar que sea lo mas rapido posible porque corre en el main
            public Handler(Socket socket)
            {
                this.socket = socket;
            }
            public Set agregarBloqueados (String nombre)
            {
                Set setDeLaPersona = new HashSet();
                Set set = listaBloqueados.entrySet();
                Iterator iterator = set.iterator();
                while (iterator.hasNext()) {
                    Map.Entry mentry = (Map.Entry) iterator.next();
                    if (mentry.getKey()== name) {
                        setDeLaPersona = (Set)mentry.getValue();
                        break;
                    }
                }
                if(!setDeLaPersona.contains(nombre))
                {
                    setDeLaPersona.add(nombre);
                }
                return setDeLaPersona;
            }
            public Set eliminarBloqueado (String nombre)
            {
                Set setDeLaPersona = new HashSet();
                Set set = listaBloqueados.entrySet();
                Iterator iterator = set.iterator();
                while (iterator.hasNext()) {
                    Map.Entry mentry = (Map.Entry) iterator.next();
                    if (mentry.getKey()== name) {
                        setDeLaPersona = (Set)mentry.getValue();
                        break;
                    }
                }
                try {
                    setDeLaPersona.remove(nombre);
                } catch (Exception e) {
                }
                return setDeLaPersona;
            }
            public boolean verificarBloqueados (String nombre)
            {
                Set setDeLaPersona = new HashSet();
                Set set = listaBloqueados.entrySet();
                Iterator iterator = set.iterator();
                while (iterator.hasNext()) {
                    Map.Entry mentry = (Map.Entry) iterator.next();
                    if ( ((String)mentry.getKey()).equalsIgnoreCase(nombre) ) {
                        setDeLaPersona = (Set)mentry.getValue();
                        break;
                    }
                }
                if(!setDeLaPersona.contains(name))
                {
                    return false;
                }
                else
                {
                    return true;
                }
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
                                //agregar en el map el nombre de la persona y un set en blanco para ir agregando a los que bloquee
                                listaBloqueados.put(name, new HashSet());
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
                                //verifica si la persona a la que se le envia mensaje privado existe
                                if(input.toLowerCase().startsWith("/") && names.contains(input.substring(1,input.indexOf(" "))))
                                {
                                    String nombreRecibir = input.substring(1,input.indexOf(" "));
                                    System.out.println(nombreRecibir);
                                    //verifica si la persona a la que le quieres enviar privado no te tiene bloqueada
                                    if(!verificarBloqueados(nombreRecibir))
                                    {
                                         writers2.get(nombreRecibir).println("Message " + name + ": " + input.substring(input.indexOf(" ")) + "   *privado*");
                                    }
                                    writers2.get(name).println("Message " + name + ": " + input.substring(input.indexOf(" ")) + "   *privado-a-" + nombreRecibir+ "*");
                                }
                            } catch (Exception en) {
                            }
                            
                            if(input.toLowerCase().startsWith("/bloquear"))
                            {
                                String nombre = input.substring(10);
                                listaBloqueados.replace(name,agregarBloqueados(nombre));
                            }
                            if(input.toLowerCase().startsWith("/desbloquear"))
                            {
                                    String nombre = input.substring(13);
                                    listaBloqueados.replace(name,eliminarBloqueado(nombre));
                            }
                        }
                        else
                        {
                            for(PrintWriter writer : writers)
                            {
                                String identificado = "";
                                Set set = writers2.entrySet();
                                Iterator iterator = set.iterator();
                                while(iterator.hasNext()) {
                                   Map.Entry mentry = (Map.Entry)iterator.next();
                                   if(mentry.getValue() == writer)
                                   {
                                       identificado = mentry.getKey().toString();
                                       break;
                                   }
                                }
                                Set setDeLaPersona = listaBloqueados.get(identificado);
                                if(setDeLaPersona != null)
                                {
                                    if(!setDeLaPersona.contains(name))
                                    {
                                  
                                        writer.println("Message " + name + ": " + input);
                                    }
                                }
                                else
                                {
                                    writer.println("Message " + name + ": " + input);
                                }
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
