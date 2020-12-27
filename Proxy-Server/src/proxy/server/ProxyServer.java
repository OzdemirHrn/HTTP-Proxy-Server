package proxy.server;

import java.io.*;
import static java.lang.Integer.parseInt;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author hrnoz
 */
public class ProxyServer {

    private static ExecutorService service = Executors.newFixedThreadPool(20);
    private static int portNumber = 8888;

    public static void main(String[] args) throws IOException {

        ServerSocket welcomeSocket = new ServerSocket(portNumber); //create welcoming socket at port 6789
        System.out.println("Proxy Server");
        while (true) {
            //ServerSocket.accept() is blocking method and blocks until a socket connection made. 
            Socket connectionSocket = welcomeSocket.accept();
            System.out.println("Client connected!  " + connectionSocket.getPort());

            Task task = new Task(connectionSocket, service);

            service.submit(task);

        }

    }

}

class Resp implements Runnable {

    private Socket client;
    private Socket server;
    InputStream inFromServer;
    OutputStream outToClient;
    byte[] reply = new byte[4096];
    ExecutorService exe;

    public Resp(InputStream inFromServer, OutputStream outToClient,Socket client, Socket server) {

        this.inFromServer = inFromServer;
        this.outToClient = outToClient;
        this.client=client;
        this.server=server;
    }

    @Override
    public void run() {

        int bytesRead1;

        try {
            while ((bytesRead1 = inFromServer.read(reply)) != -1) {
                outToClient.write(reply, 0, bytesRead1);
                
            }
            client.close();
            server.close();

        } catch (IOException ex) {
            Logger.getLogger(Resp.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    ///her opicten kaç tane geldi onu tut- parametre ver kaç message atsın diye
}

class Task implements Runnable {
    private String splitMessage[];
    private String responseIndex;
    ExecutorService exe;
    private Socket client;
    private String clientSentence;

    public Task(Socket socket, ExecutorService exe) {
        this.client = socket;
        this.exe = exe;

    }

    @Override
    public void run() {

        try {
            

            final InputStream streamFromClient = client.getInputStream();
            final OutputStream outToClient = client.getOutputStream();

            Socket server = new Socket("127.0.0.1", 8080);
            final InputStream inFromServer = server.getInputStream();
            final OutputStream outToserver = server.getOutputStream();
            
            
            //clientten gelen isteği direkt web aervera yolladığım yer
            
            Thread t8 = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] request = new byte[1024];
                    String dataString="";
                    int bytesRead;
                    try {
                        while ((bytesRead = streamFromClient.read(request)) != -1) {
                            
                            /*
                            unutma bu split işlemi sadece http serverim için
                            eğer any web servera yollayacaksan bu split fonk olmaz
                            onun için de otomatik server tanımlamican
                            tahminim server socketi oluştururken oraya host:.. kısmı falan gelebilir !?
                            */
                            dataString += new String(request, 0, bytesRead);
                            request=split(dataString);
                            outToserver.write(request);
                           
                            
                        }
                        
                    }catch (IOException ex) {
                        
                    }

                }
            });
            
            t8.start();
            Resp resp = new Resp(inFromServer, outToClient,client,server);
            exe.submit(resp);

        } catch (IOException ex) {

            Logger.getLogger(Task.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    private byte[] split(String message){
        
        splitMessage=message.split("\r\n");
        //responseIndex bunun içinde index var 
        responseIndex=splitMessage[0].substring(splitMessage[0].indexOf("8080/")+5,splitMessage[0].lastIndexOf(" "));
        splitMessage[0]=splitMessage[0].substring(0, 4)+"/"+splitMessage[0].substring(splitMessage[0].indexOf("8080/")+5,splitMessage[0].length());
        
        //System.out.println(responseIndex);
        //System.out.println(splitMessage[0]);
        //System.out.println(message);
        //System.out.println("---------------");
        message=splitMessage[0]+message.substring(message.indexOf("\r\n"),message.length());
        //System.out.println(message);
        
        byte[] toServer=message.getBytes();
        
        return toServer;
        
        
        
        

    }

    private void sendResponse(BufferedReader inFromClient) throws IOException {

        System.out.println("geldi");

        while ((clientSentence = inFromClient.readLine()) != null) {
            System.out.println("--" + clientSentence);
            //outToserver.writeBytes(clientSentence);
        }

    }

}
