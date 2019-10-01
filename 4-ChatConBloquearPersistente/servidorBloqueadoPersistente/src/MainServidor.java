import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
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
    public String ruta()
    {
        URL link = this.getClass().getProtectionDomain().getCodeSource().getLocation();
        return link.toString();
    }
    
    public static void main(String[] args)
    {
       System.out.println("The chat server is running...");
        ExecutorService pool = Executors.newFixedThreadPool(500);
        try (ServerSocket listener = new ServerSocket(59001)) {
            while (true)
            {
                pool.execute(new Handler(listener.accept()));
            }
            
            
        }catch(Exception ee)
        {
            System.out.println("Error al crear el pool" + ee.toString());
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
            MainServidor camino = new MainServidor();
            String ruta = camino.ruta();
            int discoDuro = ruta.indexOf(":") + 2;
            int nombreCarpeta = ruta.indexOf("servidorBloqueadoPersistente") + 29;
            String rutaFinal = ruta.substring(discoDuro, nombreCarpeta);
            
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
                    if (((String)mentry.getKey()).equalsIgnoreCase(nombre)) {
                        setDeLaPersona = (Set)mentry.getValue();
                        break;
                    }
                }
                if(setDeLaPersona.contains(name))
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }
            public void run()
            {
                try {
                    in = new Scanner(socket.getInputStream());
                    out = new PrintWriter(socket.getOutputStream(),true);
                    //intenta leer el archivo donde se guardan quien tiene bloqueado a quien
                    try {
                        FileInputStream fi = new FileInputStream(rutaFinal + "listabloqueados.conf");
                        ObjectInputStream oi = new ObjectInputStream(fi);
                        listaBloqueados = (HashMap)oi.readObject();
                        oi.close();
                        fi.close();
                    } catch (Exception e) {
                        
                    }
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
                                if(!listaBloqueados.containsKey(name))
                                {
                                    listaBloqueados.put(name, new HashSet());
                                }
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
                                 //verifica que la persona a enviar mensaje privado exista
                                if(input.toLowerCase().startsWith("/") && names.contains(input.substring(1,input.indexOf(" "))))
                                {
                                    String nombreRecibir = input.substring(1,input.indexOf(" ")); 
                                    //comprueba que esa persona no te tenga bloqueado
                                    if(!verificarBloqueados(nombreRecibir))
                                    {
                                        writers2.get(nombreRecibir).println("Message " + name + ": " + input.substring(input.indexOf(" ")) + "   *privado*");
                                    }
                                    writers2.get(name).println("Message " + name + ": " + input.substring(input.indexOf(" ")) + "   *privado-a-" +nombreRecibir+ "*");
                                }
                            } catch (Exception e) {
                            }
                           
                            if(input.toLowerCase().startsWith("/bloquear"))
                            {
                                String nombre = input.substring(10);
                                //sincroniza el orden con el que las personas quieran agregar a sus bloqueados y luego lo escribe
                                synchronized(listaBloqueados.replace(name,agregarBloqueados(nombre)))
                                {
                                  try {
                                        FileOutputStream fo = new FileOutputStream(rutaFinal + "listabloqueados.conf");
                                        ObjectOutputStream ou = new ObjectOutputStream(fo);
                                        ou.writeObject(listaBloqueados);
                                        ou.close();
                                        fo.close();
                                    } catch (Exception e) {
                                    }          
                                }
                                
                            }
                            if(input.toLowerCase().startsWith("/desbloquear"))
                            {
                                    String nombre = input.substring(13);
                                    //sincroniza el orden con el que las personas quieran eliminar de sus bloqueados y luego lo escribe
                                    synchronized(listaBloqueados.replace(name,eliminarBloqueado(nombre)))
                                    {
                                        try {
                                        FileOutputStream fo = new FileOutputStream(rutaFinal + "listabloqueados.conf");
                                        ObjectOutputStream ou = new ObjectOutputStream(fo);
                                        ou.writeObject(listaBloqueados);
                                        ou.close();
                                        fo.close();
                                        } catch (Exception e) {
                                        }   
                                    }
                                    
                            }
                        }
                        else
                        {
                            for(PrintWriter writer : writers)
                            {
                                String identificado = "";
                                //busca de quien es el writer en turno
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
                                //verifica que el dueño del writer no tenga a la persona que quiera enviar el mensaje bloqueada
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
