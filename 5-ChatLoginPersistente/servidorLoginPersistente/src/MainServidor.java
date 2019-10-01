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
    //para el login
    private static Map<String,String> listaLogin = new HashMap<>();
    //calcula la ruta donde se esta ejecutando el proyecto
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
            
            
        }catch(Exception e)
        {
            System.out.println("Error al crear la pool" + e.toString());
            System.exit(1);
        }
    }
    private static class Handler implements Runnable
    {
            private String name;
            private String pass;
            private Socket socket;
            private Scanner in;
            private PrintWriter out;
            private static Set<String> bloqueados = new HashSet<>();
            //determina la ruta final ya solo con lo que se necesita
            MainServidor camino = new MainServidor();
            String ruta = camino.ruta();
            int discoDuro = ruta.indexOf(":") + 2;
            int nombreCarpeta = ruta.indexOf("servidorLoginPersistente") + 25;
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
                if(!setDeLaPersona.contains(nombre) && nombre!=name)
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
            public boolean checarPass(String password)
            {
                String passDeLaPersona = "";
                Set set = listaLogin.entrySet();
                Iterator iterator = set.iterator();
                while (iterator.hasNext()) {
                    Map.Entry mentry = (Map.Entry) iterator.next();
                    if ( ((String)mentry.getKey()).equalsIgnoreCase(name)) 
                    {
                        passDeLaPersona = (String)mentry.getValue();
                        break;
                    }
                }
                if(passDeLaPersona.equalsIgnoreCase(password))
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
                    //se intenta leer el archivo que guarda nombres y claves
                    try {
                        FileInputStream fi2 = new FileInputStream(rutaFinal + "listalogin.conf");
                        ObjectInputStream oi2 = new ObjectInputStream(fi2);
                        listaLogin = (HashMap)oi2.readObject();
                        oi2.close();
                        fi2.close();
                    } catch (Exception e) {
                    }
                    //se intenta leer el archivo que contiene a quienes bloquea cada uno
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
                        //sincroniza los que ya estan registrados y los que no
                        synchronized(listaLogin)
                        {
                            if (!listaLogin.containsKey(name))
                            {
                                out.println("SUBMITPASS");
                                pass = in.nextLine();
                                if (pass == null || pass.contains(" ") || pass.length() == 0) 
                                {
                                    return;
                                }
                                names.add(name);
                                //guarda el nombre con el out
                                writers2.put(name, out);
                                //se le crea un espacio para añadir bloqueados
                                if(!listaBloqueados.containsKey(name))
                                {
                                    listaBloqueados.put(name, new HashSet());
                                }
                                //se registra este usuario
                                listaLogin.put(name, pass);
                                //se guarda en el archivo
                                try {
                                        FileOutputStream fo2 = new FileOutputStream(rutaFinal + "listalogin.conf");
                                        ObjectOutputStream ou2 = new ObjectOutputStream(fo2);
                                        ou2.writeObject(listaLogin);
                                        ou2.close();
                                        fo2.close();
                                    } catch (Exception e) {
                                    }
                                break;
                            }
                            else
                            {
                                while (true) {
                                    out.println("SUBMITPASS");
                                    pass = in.nextLine();
                                    if (pass == null || pass.contains(" ") || pass.length()==0)
                                    {
                                        return;
                                    }
                                    if(checarPass(pass))
                                    {
                                        break;
                                    }
                                }
                                names.add(name);
                                if(writers2.containsKey(name))
                                {
                                    writers2.replace(name, out);
                                }
                                else
                                {
                                    writers2.put(name, out);
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
                                //verifica que el nombre este en la lista
                                if(input.toLowerCase().startsWith("/") && names.contains(input.substring(1,input.indexOf(" "))))
                                {
                                    String nombreRecibir = input.substring(1,input.indexOf(" "));
                                    //verifica que no te tenga bloqueado
                                    if(!verificarBloqueados(nombreRecibir))
                                    {
                                        writers2.get(nombreRecibir).println("Message " + name + ": " + input.substring(input.indexOf(" ")) + "   *privado*");
                                    }
                                    writers2.get(name).println("Message " + name + ": " + input.substring(input.indexOf(" ")) + "   *privado-a-" + nombreRecibir + "*");
                                }
                            } catch (Exception e) {
                            }
                            if(input.toLowerCase().startsWith("/bloquear"))
                            {
                                String nombre = input.substring(10);
                                //sincroniza para agregar al archivo
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
                            //sincroniza para borrar del arhivo
                            if(input.toLowerCase().startsWith("/desbloquear"))
                            {
                                    String nombre = input.substring(13);
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
                                //encuentra a quien pertenece el out
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
                                //verifica si no te tiene bloqueado a ti
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
                    if(listaBloqueados != null)
                    {
                        listaBloqueados = new HashMap<>();
                    }
                    try {socket.close();} catch (IOException e) {}
                }
            }
        
    }
}
