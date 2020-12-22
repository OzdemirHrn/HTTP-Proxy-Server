package http.server;

import java.io.*;
import static java.lang.Integer.parseInt;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author ozdemirHarun
 */

public class HTTPServer {
    
    private static ExecutorService service = Executors.newFixedThreadPool(1);
    private static int portNumber=8080;
    
    public static void main(String[] args) throws IOException {
        
        ServerSocket welcomeSocket = new ServerSocket(portNumber); //create welcoming socket at port 6789
        System.out.println("Server is waiting for client connection...");
        while (true) {
            //ServerSocket.accept() is blocking method and blocks until a socket connection made. 
            Socket connectionSocket = welcomeSocket.accept();
            System.out.println("Client connected!  " + connectionSocket.getPort());
            
            Task task = new Task(connectionSocket);

            service.submit(task);

        }
}

}

final class Task implements Runnable{
        private Socket client;
        private String clientSentence;
        private String requestLine[];
        private int contentLength;
        private String contentType;
        private String method;
        private String statusCode="200 OK";//DEFAULT
        private int sizeOfTheDocument;
        private boolean nanCheck=false;
        private boolean invalidMethod=false;
        
        public Task(Socket clientSocket){   
            this.client=clientSocket;   
                    
        }

        @Override
        public void run() {
            BufferedReader inFromClient;
            // clientSentence =  inFromClient.lines().collect(Collectors.joining("\r\n")).split("\\r?\\n");

                try {
                
                    inFromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    clientSentence = inFromClient.readLine();
                    splitOperations();
                    System.out.println(client.getPort()+"  "+clientSentence);
                    sendResponse(createHtmlFile(sizeOfTheDocument));
                    
                } catch (IOException ex) {
               
                    
                    Logger.getLogger(Task.class.getName()).log(Level.SEVERE, null, ex);
                }
            
      
        }
        
        private void splitOperations(){
            
            requestLine=clientSentence.split(" ");
            method=requestLine[0];
            if(!method.equals("GET")){
                if(method.equals("POST") || method.equals("HEAD") || method.equals("PUT") || method.equals("DELETE"))
                    statusCode="501 Not Implemented";

                else
                    statusCode="400 Bad Request";
                    invalidMethod=true;}
            
            requestLine[1]=requestLine[1].replace("/", "");    
            try{
                sizeOfTheDocument=parseInt(requestLine[1]);
                if(sizeOfTheDocument<100 || sizeOfTheDocument>20000)
                    statusCode="400 Bad Request";
            
            }catch(NumberFormatException e){
                statusCode="400 Bad Request";
                nanCheck=true;
            }
            
        }
        
        
        
    private File createHtmlFile(int size) throws IOException{
        String FILE_TO_SEND = "C:/Users/hrnoz/Desktop/byte_html/"+size+".txt";
        String badRequest_gt20000 = "C:/Users/hrnoz/Desktop/byte_html/gt-20000.txt";
        String badRequest_lt100 = "C:/Users/hrnoz/Desktop/byte_html/lt-100.txt";
        String badRequest_nan = "C:/Users/hrnoz/Desktop/byte_html/nan.txt";
        String badRequest_notGet = "C:/Users/hrnoz/Desktop/byte_html/not-get.txt";
        String badRequest_notImp = "C:/Users/hrnoz/Desktop/byte_html/not-imp.txt";
        File file = null;
        FileWriter filewriter = null;
        
        if(sizeOfTheDocument<100&&!nanCheck){
            file = new File(badRequest_lt100);
            return file;
            
        }else if(sizeOfTheDocument>20000){
            file = new File(badRequest_gt20000);
            return file;
            
        }else if(nanCheck){
            file = new File(badRequest_nan);
            return file;
            
        }else if(invalidMethod){
            file = new File(badRequest_notGet);
            return file;
            
        }else if(statusCode.equals("501 Not Implemented")){
            file = new File(badRequest_notImp);
            return file;
            
        }
        
        file = new File(FILE_TO_SEND);
        filewriter = new FileWriter(file);
        
        String deneme="<HTML>\n" +
                      "<HEAD>\n" +
                      "<TITLE>Yesile Hasret</TITLE>\n" +
                      "</HEAD>\n" +
                      "<BODY>";
        for(int i=0;i<size-73;i++){
            if(i%70==0){
                deneme+="\r\n";
                i++;
                continue;
            }
            deneme+="a";
        }
        
        deneme+="\n</BODY>\n" +
                "</HTML>";
        

	filewriter.write(deneme);
	filewriter.close();
        return file;
    }
  
    private void sendResponse(File file) throws IOException{
            DataOutputStream  outToClient = new DataOutputStream(client.getOutputStream()); //create output stream, attached to socket
            FileInputStream fileInputStream;
            BufferedInputStream bufferedInputStream;
            byte [] mybytearray  = new byte [(int)file.length()];
            fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            bufferedInputStream.read(mybytearray,0,mybytearray.length);

            //os.write(mybytearray,0,mybytearray.length);
            outToClient.writeBytes(("HTTP/1.1 "+statusCode+"\r\n"));
            outToClient.writeBytes(("ContentType: " + "text/html" + "\r\n"));
            outToClient.writeBytes("\r\n");
            outToClient.write(mybytearray,0,mybytearray.length);
            outToClient.writeBytes("\r\n\r\n");
            outToClient.flush();
            outToClient.close();
            client.close();
            System.out.println("Connection Reset " + client.getPort());
            
        }
    }