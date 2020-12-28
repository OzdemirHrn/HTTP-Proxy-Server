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

class ProxyToHtppServer implements Runnable {

    private String splitMessage[];
    private String responseIndex;
    byte[] reply = new byte[1024];
    private Socket client;
    private Socket server;
    private boolean serverStatus;
    private String statusCode="";
    InputStream inFromClient;
    OutputStream outToServer;
    OutputStream outToClient;
    ExecutorService exe;

    public ProxyToHtppServer(InputStream inFromClient, OutputStream outToServer, OutputStream outToClient, Socket client, Socket server,boolean serverStatus) {

        this.inFromClient = inFromClient;
        this.outToServer = outToServer;
        this.outToClient = outToClient;
        this.client = client;
        this.server = server;
        this.serverStatus=serverStatus;

    }

    @Override
    public void run() {

        int bytesRead;
        String dataString = "";
        try {

            while ((bytesRead = inFromClient.read(reply)) != -1) {

                dataString += new String(reply, 0, bytesRead);
                split(dataString, outToServer);

            }

        } catch (IOException ex) {

        }
    }

    private void split(String message, OutputStream outToServer) throws IOException {

        splitMessage = message.split("\r\n");
        //responseIndex bunun içinde index var 
        responseIndex = splitMessage[0].substring(splitMessage[0].indexOf("8080/") + 5, splitMessage[0].lastIndexOf(" "));
        splitMessage[0] = splitMessage[0].substring(0, 4) + "/" + splitMessage[0].substring(splitMessage[0].indexOf("8080/") + 5, splitMessage[0].length());

        //System.out.println(responseIndex);
        //System.out.println(splitMessage[0]);
        //System.out.println(message);
        //System.out.println("---------------");
        message = splitMessage[0] + message.substring(message.indexOf("\r\n"), message.length());

        byte[] toServer = message.getBytes();

        System.out.println(message);
        sendToServerOrRestrictionCheck(responseIndex, outToServer, toServer);
        //outToServer.write(toServer);

        //return toServer;
    }

    private void sendToServerOrRestrictionCheck(String index, OutputStream outToServer, byte[] toServer) throws IOException {
  
        int parseIndex;
        
        if(!serverStatus){
            sendFileFromProxy(outToClient);
            return;
        }
        
        try{
            parseIndex=parseInt(index);
        }catch(NumberFormatException e){
            
                 outToServer.write(toServer);
                 return; 
        }
        
        if (parseIndex > 9999) {
            sendFileFromProxy(outToClient);

        } else {
           outToServer.write(toServer);
        }

    }

    private void sendFileFromProxy(OutputStream outToClient) throws FileNotFoundException, IOException {
        String tooLong_gt9999 = "C:/Users/hrnoz/Desktop/Proxy-Server/gt-9999.txt";
        String server_notFound = "C:/Users/hrnoz/Desktop/Proxy-Server/server-404.txt";
        File file = null;
        String response="";
        
        if(serverStatus){
            file = new File(tooLong_gt9999);
            statusCode="414 Request-URI Too Long";
        }
        else if(!serverStatus){
            file = new File(server_notFound);
            statusCode="404 Not Found";
        }
        
        
        
        byte[] directlyToClient = new byte[(int) file.length()];
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream bufferedInputStream;
        bufferedInputStream = new BufferedInputStream(fileInputStream);
        bufferedInputStream.read(directlyToClient, 0, directlyToClient.length);
        
        String str=new String(directlyToClient);
       // System.out.println(str);
        
            response+="HTTP/1.1 "+statusCode+"\r\n"
            +"ContentType: " + "text/html" + "\r\n"
            +"\r\n"
            +str+"\r\n\r\n";
        directlyToClient=response.getBytes();
        outToClient.write(directlyToClient);
        client.close();
        //server.close();
    }

}

class Resp implements Runnable {

    private Socket client;
    private Socket server;
    InputStream inFromServer;
    OutputStream outToClient;
    byte[] reply = new byte[4096];
    ExecutorService exe;

    public Resp(InputStream inFromServer, OutputStream outToClient, Socket client, Socket server) {

        this.inFromServer = inFromServer;
        this.outToClient = outToClient;
        this.client = client;
        this.server = server;
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
            System.out.println("SIKINTI VAR");
        }

    }

    ///her opicten kaç tane geldi onu tut- parametre ver kaç message atsın diye
}

class Task implements Runnable {

    ExecutorService exe;
    private Socket client;
    private boolean serverStatus=true;

    public Task(Socket socket, ExecutorService exe) {
        this.client = socket;
        this.exe = exe;

    }

    @Override
    public void run() {

        try {
            Socket server = null;
            final InputStream inFromClient = client.getInputStream();
            final OutputStream outToClient = client.getOutputStream();
            try{
               server = new Socket("127.0.0.1",8080);
            }
            catch(IOException e){
                serverStatus=false;
            }
            //finalleri sildim ??
            InputStream inFromServer=null;
            OutputStream outToserver=null;
            if(serverStatus){
            inFromServer = server.getInputStream();
            outToserver = server.getOutputStream();
            }
            /*
            şuan 8888 portuna yollanan veriler patlıyor. Onlara split uygulamama
            lazım. Belki hostları değiştirilebilir.
             */
            ProxyToHtppServer toHttpServer = new ProxyToHtppServer(inFromClient, outToserver, outToClient, client, server,serverStatus);
            exe.submit(toHttpServer);

            Resp resp = new Resp(inFromServer, outToClient, client, server);
            exe.submit(resp);


        } catch (IOException ex) {

            Logger.getLogger(Task.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
