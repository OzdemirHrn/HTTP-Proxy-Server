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
    private static int portNumber=8888;
    
    public static void main(String[] args) throws IOException {
       
        ServerSocket welcomeSocket = new ServerSocket(portNumber); //create welcoming socket at port 6789
        System.out.println("Proxy Server");
        while (true) {
            //ServerSocket.accept() is blocking method and blocks until a socket connection made. 
            Socket connectionSocket = welcomeSocket.accept();
            System.out.println("Client connected!  " + connectionSocket.getPort());
            
            Task task = new Task(connectionSocket);

            service.submit(task);

        }
        
        
}
    
}

class Resp implements Runnable{
     private Socket client;
     InputStream inFromServer;
     OutputStream  outToClient;
     byte[] reply = new byte[4096];
     public Resp(InputStream inFromServer,OutputStream  outToClient){

         this.inFromServer=inFromServer;
         this.outToClient=outToClient;
     }
     
    @Override
    public void run() {
        
        int bytesRead1;
                  
         try {
             while ((bytesRead1 = inFromServer.read(reply)) != -1) {
                 outToClient.write(reply, 0, bytesRead1);
                 outToClient.flush();
             }
             
         } catch (IOException ex) {
             Logger.getLogger(Resp.class.getName()).log(Level.SEVERE, null, ex);
         }
        
    }
    
    ///her opicten kaç tane geldi onu tut- parametre ver kaç message atsın diye
}

class Task implements Runnable{
    private Socket client;
    private String clientSentence;

  
    
    public Task(Socket socket){
        this.client=socket;
   
    }
    
  

    @Override
    public void run() {
       
            

                try {
                    final byte[] request = new byte[1024];
                   final byte[] reply = new byte[4096];
                    
                    final InputStream streamFromClient = client.getInputStream();
                    final OutputStream  outToClient =client.getOutputStream();
                    
                    Socket server = new Socket("127.0.0.1", 8080);
                    final InputStream inFromServer = server.getInputStream();
                    final OutputStream  outToserver = server.getOutputStream(); 
                    
                            Thread t8 = new Thread(new Runnable() {
        @Override
        public void run() {
   int bytesRead;
            try {
                while ((bytesRead = streamFromClient.read(request)) != -1) {
                    
                    outToserver.write(request, 0, bytesRead);
                    outToserver.flush();
                }
            } catch (IOException ex) {
                Logger.getLogger(Task.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
            
}});
                            t8.start();
                    
                    
                    
                    
                    
                    
                 
                     
                    
                  Runnable serverSide=new Resp(inFromServer,outToClient);
                  Thread threadServer=new Thread(serverSide);
                  threadServer.start();
                  
                  
                    
                } catch (IOException ex) {
               
                    
                    Logger.getLogger(Task.class.getName()).log(Level.SEVERE, null, ex);
                }
        
    }
       

            private void sendResponse(BufferedReader inFromClient) throws IOException{
                
            
            
     
                System.out.println("geldi");
            
            
            while ((clientSentence = inFromClient.readLine()) != null){
                System.out.println("--"+clientSentence);
                //outToserver.writeBytes(clientSentence);
                    }
     
        }
    
    
}
