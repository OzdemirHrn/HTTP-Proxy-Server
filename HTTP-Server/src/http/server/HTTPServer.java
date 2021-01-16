package http.server;

import java.io.*;

import static java.lang.Integer.parseInt;

import java.net.*;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Harun ÖZDEMİR 150116043
 */
public class HTTPServer {

    private static ExecutorService service = Executors.newFixedThreadPool(500);
    private static int portNumber = 8080;

    public static void main(String[] args) throws IOException {

        ServerSocket welcomeSocket = new ServerSocket(portNumber); //create welcoming socket at port 6789
        System.out.println("Server is waiting for client connection...");
        while (true) {
            //ServerSocket.accept() is blocking method and blocks until a socket connection made. 
            Socket connectionSocket = welcomeSocket.accept();

            ServerOperations serverOperations = new ServerOperations(connectionSocket);

            service.submit(serverOperations);

        }
    }

}

final class ServerOperations implements Runnable {


    private Socket client;
    private String clientSentence;
    private String requestLine[];
    private int contentLength;
    private String contentType = "text/html";
    private String method;
    private String statusCode = "200 OK";//DEFAULT
    private int sizeOfTheDocument;
    private boolean nanCheck = false;
    private boolean invalidMethod = false;
    private boolean notImp = false;
    byte[] request = new byte[1024];
    private String splitMessage[];

    public ServerOperations(Socket clientSocket) {
        this.client = clientSocket;

    }

    @Override
    public void run() {


        try {
            // Request coming from Client
            final InputStream inFromClient = client.getInputStream();

            int bytesRead1;
            String dataString = "";
            /*
            Read request from InputStream
             */
            while ((bytesRead1 = inFromClient.read(request)) != -1) {

                dataString += new String(request, 0, bytesRead1);
                prevSplitPhase(dataString);
            }


        } catch (IOException ex) {

        }

    }

    private void prevSplitPhase(String data) throws IOException {

        //Getting every line of request
        splitMessage = data.split("\r\n");
        //Method /index HTTP.version
        clientSentence = splitMessage[0];
        splitOperations();

        System.out.println("Client connected! Port number is: " + client.getPort() + "\n");
        System.out.println(data);

        sendResponse(createHtmlFile(sizeOfTheDocument));
    }

    private void splitOperations() {
        //Set of valid HTTP methods
        Set<String> validMethods = Set.of("GET", "PUT", "POST", "PATCH", "DELETE", "COPY", "OPTIONS", "LINK",
                "UNLINK", "PURGE", "LOCK", "UNLOCK", "PROPFIND", "VIEW");
        //Splitting --> Method + index + version
        requestLine = clientSentence.split(" ");
        method = requestLine[0];
        //If method is not GET then return 501 or 400 status code.
        if (!method.equals("GET")) {

            if (validMethods.contains(method)) {
                statusCode = "501 Not Implemented";
                notImp = true;
            } else {
                //Invalid methods
                statusCode = "400 Bad Request";
                invalidMethod = true;
            }
            return;
        }
        //  /index=> index
        requestLine[1] = requestLine[1].substring(1);
        try {
            // index checking
            sizeOfTheDocument = parseInt(requestLine[1]);
            // index restriction check less than 100 or greater than 20000
            if (sizeOfTheDocument < 100 || sizeOfTheDocument > 20000) {
                statusCode = "400 Bad Request";
            }

        } catch (NumberFormatException e) {
            // If index is not a number!
            statusCode = "400 Bad Request";
            nanCheck = true;
        }


    }

    // This method create .txt file according to index or error messages.
    private File createHtmlFile(int size) throws IOException {
        /*

         */
        String FILE_TO_SEND = "C:/Users/hrnoz/Desktop/HTTP-Server/" + size + ".txt";
        String badRequest_gt20000 = "C:/Users/hrnoz/Desktop/HTTP-Server/gt-20000.txt";
        String badRequest_lt100 = "C:/Users/hrnoz/Desktop/HTTP-Server/lt-100.txt";
        String badRequest_nan = "C:/Users/hrnoz/Desktop/HTTP-Server/nan.txt";
        String badRequest_notGet = "C:/Users/hrnoz/Desktop/HTTP-Server/not-get.txt";
        String badRequest_notImp = "C:/Users/hrnoz/Desktop/HTTP-Server/not-imp.txt";
        String emptyFile = "C:/Users/hrnoz/Desktop/HTTP-Server/empty.txt";
        File file = null;
        FileWriter filewriter = null;

        // 501 NOT IMPLEMENTED
        if (notImp) {
            file = new File(badRequest_notImp);
            return file;
            // 400 INVALID METHOD
        } else if (invalidMethod) {
            file = new File(badRequest_notGet);
            return file;
            // 400 <100
        } else if (sizeOfTheDocument < 100 && !nanCheck) {
            file = new File(badRequest_lt100);
            return file;
            // 400 >20000
        } else if (sizeOfTheDocument > 20000) {
            file = new File(badRequest_gt20000);
            return file;
            // 400 NOT a NUMBER
        } else if (nanCheck) {
            file = new File(badRequest_nan);
            return file;
        /*
         CHECK CONDITIONAL GET!
         IF requested uri is odd and proxy has its cache file,
         then HTTP server returns 304 Not Modified without data.
         Because data will send from Proxy's cache.
         */
        } else if (splitMessage[3].equals("If-Match: <odd>")) {
            statusCode = "304 Not Modified";
            file = new File(emptyFile);
            return file;
        }


        file = new File(FILE_TO_SEND);
        if (!file.exists()) {
            filewriter = new FileWriter(file);

            // Base HTML file is 73 byte. So that We should add more bytes until reach to index
            String htmlFile = "<HTML>\n"
                    + "<HEAD>\n"
                    + "<TITLE>Yesile Hasret</TITLE>\n"
                    + "</HEAD>\n"
                    + "<BODY>";
            for (int i = 0; i < size - 73; i++) {

                htmlFile += "a";
            }

            htmlFile += "\n</BODY>\n"
                    + "</HTML>";

            filewriter.write(htmlFile);
            filewriter.close();
        }
        return file;
    }

    /*
    This method send response to Client according to request
     */
    private void sendResponse(File file) throws IOException {
        //create output stream, attached to socket
        DataOutputStream outToClient = new DataOutputStream(client.getOutputStream());
        FileInputStream fileInputStream;
        BufferedInputStream bufferedInputStream;
        // read from file and fill these to byte array.
        byte[] htmlFileByte = new byte[(int) file.length()];
        fileInputStream = new FileInputStream(file);
        bufferedInputStream = new BufferedInputStream(fileInputStream);
        bufferedInputStream.read(htmlFileByte, 0, htmlFileByte.length);
        contentLength = htmlFileByte.length;

        /*
        Send response message
         */
        String response = "";
        response += "HTTP/1.1 " + statusCode + "\r\n"
                + "ContentType: " + contentType + "\r\n"
                + "ContentLength: " + contentLength + "\r\n"
                + "\r\n"
                + new String(htmlFileByte);

        System.out.println("Response Port number: " + client.getPort() + "\n" + response);
        htmlFileByte = response.getBytes();
        // Write to client socket.
        outToClient.write(htmlFileByte);
        outToClient.flush();
        outToClient.close();
        client.close();
        System.out.println("Connection Reset " + client.getPort() + "\n");

    }
}
