package proxy.server;

import java.io.*;

import static java.lang.Integer.parseInt;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Harun ÖZDEMİR 150116043
 */

public class ProxyServer {

    private static ExecutorService service = Executors.newFixedThreadPool(500);
    private static int portNumber = 8888;

    public static void main(String[] args) throws IOException {

        ServerSocket welcomeSocket = new ServerSocket(portNumber); //create welcoming socket at port 6789
        System.out.println("Proxy Server");
        while (true) {
            //ServerSocket.accept() is blocking method and blocks until a socket connection made.
            Socket connectionSocket = welcomeSocket.accept();

            Task task = new Task(connectionSocket, service);

            service.submit(task);

        }

    }

}

/*
    Read Client's request and send to HTTP server or
    directly to the Client if index>9999 or 404 Not Found
    errors occur.
 */
class ProxyToHtppServer implements Runnable {

    private String splitMessage[];
    static String responseIndex;
    byte[] reply = new byte[1024];
    private Socket client;
    private Socket server;
    private boolean serverStatus;
    private String statusCode = "";
    private File fileCache;
    InputStream inFromClient;
    OutputStream outToServer;
    OutputStream outToClient;
    ExecutorService exe;

    public ProxyToHtppServer(InputStream inFromClient, OutputStream outToServer, OutputStream outToClient, Socket client, Socket server, boolean serverStatus) {

        this.inFromClient = inFromClient;
        this.outToServer = outToServer;
        this.outToClient = outToClient;
        this.client = client;
        this.server = server;
        this.serverStatus = serverStatus;

    }

    @Override
    public void run() {

        int bytesRead;
        String dataString = "";
        try {
            // Read request coming from client
            while ((bytesRead = inFromClient.read(reply)) != -1) {

                dataString += new String(reply, 0, bytesRead);
                split(dataString, outToServer);
            }

        } catch (IOException ex) {

        }
    }

    private void split(String message, OutputStream outToServer) throws IOException {
        System.out.println("Client connected! Port number is: " + client.getPort() + "\n");
        System.out.println(message);
        splitMessage = message.split("\r\n");
        // Split operations for If Host is localhost:8888
        if (splitMessage[1].equals("Host: localhost:8888")) {
            responseIndex = responseIndex(splitMessage, "8888/").replaceAll("/", "");
            if (splitMessage[0].startsWith("GET /")) {
                splitMessage[0] = splitMessage[0].substring(0, 4) + splitMessage[0].substring(splitMessage[0].indexOf("8888/") + 5, splitMessage[0].length());
            } else {
                splitMessage[0] = splitMessage[0].substring(0, splitMessage[0].indexOf(" ") + 1) + "/" + splitMessage[0].substring(splitMessage[0].indexOf("8888/") + 5, splitMessage[0].length());
            }
            message = splitMessage[0] + message.substring(message.indexOf("\r\n"), message.length());

            // Split operations for If Host is localhost:8080
        } else if (splitMessage[1].equals("Host: localhost:8080")) {
            responseIndex = responseIndex(splitMessage, "8080/").replaceAll("/", "");
            if (splitMessage[0].startsWith("GET /"))
                splitMessage[0] = splitMessage[0].substring(0, 4) + "/" + splitMessage[0].substring(splitMessage[0].indexOf("8080/") + 5, splitMessage[0].length());
            else
                splitMessage[0] = splitMessage[0].substring(0, splitMessage[0].indexOf(" ") + 1) + "/" + splitMessage[0].substring(splitMessage[0].indexOf("8080/") + 5, splitMessage[0].length());
            message = splitMessage[0] + message.substring(message.indexOf("\r\n"), message.length());

        }
        /*
        If requested index already cached then adding to response message
        If-Match header with odd or even value.
        Because HTTP server assumes even numbered indexes always modified but Proxy does not know this.
        According to coming response from HTTP server, we will send from cache or response data from HTTP server.
         */

        if (cacheControl(responseIndex)) {
            if (parseInt(responseIndex) % 2 == 0)
                message = splitMessage[0] + "\r\n" + splitMessage[1] + "\r\n" + splitMessage[2] + "\r\n" + "If-Match: <even>" + "\r\n";
            else if (parseInt(responseIndex) % 2 == 1)
                message = splitMessage[0] + "\r\n" + splitMessage[1] + "\r\n" + splitMessage[2] + "\r\n" + "If-Match: <odd>" + "\r\n";
        }

        byte[] toServer = message.getBytes();

        // System.out.println(message);
        sendToServerOrRestrictionCheck(responseIndex, outToServer, toServer);

    }

    // Split response index from requested line
    private String responseIndex(String[] message, String port) {
        return splitMessage[0].substring(splitMessage[0].indexOf(port)
                + 5, splitMessage[0].lastIndexOf(" "));

    }

    // Check If requested file has already cached or not in file system.
    private boolean cacheControl(String index) {
        fileCache = new File("C:/Users/hrnoz/Desktop/Proxy-Server/" + index + ".txt");
        return fileCache.exists();
    }

    private void sendToServerOrRestrictionCheck(String index, OutputStream outToServer, byte[] toServer) throws IOException {

        int parseIndex;
        String toHTTP = new String(toServer);
        /*
        Is this object already cached?
         */

        boolean cache = fileCache.exists();

        /*
        Is HTTP server running? If not send 404 Not Found.
         */
        if (!serverStatus) {
            sendFileFromProxy(outToClient, cache, fileCache);
            return;
        }

        /*
        If index is not a number.
        Send to HTTP server. It will handle this and return 404 Bad Request.
         */
        try {
            parseIndex = parseInt(index);
        } catch (NumberFormatException e) {
            System.out.println("Response to HTTP Server: ");
            System.out.println(toHTTP + "\n");
            outToServer.write(toServer);

            return;
        }
        /*
        If requested uri greater than 9999 then send directly to the Client
        404 Bad Request - Requested uri too long
         */
        if (parseIndex > 9999) {
            sendFileFromProxy(outToClient, cache, fileCache);

        } else {
            // Send request to HTTP server
            System.out.println("Response to HTTP Server: ");
            System.out.println(toHTTP + "\n");
            outToServer.write(toServer);


        }
    }

    // This method send response to client without sending to HTTP server.
    private void sendFileFromProxy(OutputStream outToClient, boolean cache, File fileCache) throws FileNotFoundException, IOException {
        String tooLong_gt9999 = "C:/Users/hrnoz/Desktop/Proxy-Server/gt-9999.txt";
        String server_notFound = "C:/Users/hrnoz/Desktop/Proxy-Server/server-404.txt";
        File file = null;
        String response = "";
        // If HTTP server is not running.
        if (!serverStatus) {
            file = new File(server_notFound);
            statusCode = "404 Not Found";
        }
        // Send 414 error message
        else if (serverStatus && !cache) {
            file = new File(tooLong_gt9999);
            statusCode = "414 Request-URI Too Long";
        }

        byte[] directlyToClient = new byte[(int) file.length()];
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream bufferedInputStream;
        bufferedInputStream = new BufferedInputStream(fileInputStream);
        bufferedInputStream.read(directlyToClient, 0, directlyToClient.length);
        /*
        Send Response message directly to the Client from Proxy Server.
         */
        String str = new String(directlyToClient);
        response += "HTTP/1.1 " + statusCode + "\r\n"
                + "ContentType: " + "text/html" + "\r\n"
                + "\r\n"
                + str;
        System.out.println("Response Directly to Client");
        System.out.println(response + "\n");
        directlyToClient = response.getBytes();
        outToClient.write(directlyToClient);
        client.close();
        server.close();
    }

}

/*
    Read messages coming from HTTP Server.
    Then send to Client.
 */

class ProxyToClient implements Runnable {

    private Socket client;
    private Socket server;
    private String splitMessage[];
    private String messageObject = "";
    InputStream inFromServer;
    OutputStream outToClient;
    byte[] reply = new byte[14096];
    ExecutorService exe;

    public ProxyToClient(InputStream inFromServer, OutputStream outToClient, Socket client, Socket server) {

        this.inFromServer = inFromServer;
        this.outToClient = outToClient;
        this.client = client;
        this.server = server;
    }

    @Override
    public void run() {

        int bytesRead1;
        String dataString = "";
        try {
            while ((bytesRead1 = inFromServer.read(reply)) != -1) {
                // Read From Server
                dataString += new String(reply, 0, bytesRead1);
                splitFile(dataString, bytesRead1);

            }
            client.close();
            server.close();

        } catch (IOException ex) {

        }

    }

    /*
     Split response message
     If it is not cached then cache it
     I added x-cache header. If HTTP server send 304 not modified message
     then I send object from cache. If coming message is 200 OK then
     direct to the Client.
     */
    private void splitFile(String message, int bytesRead1) throws IOException {

        System.out.println("Response message From HTTP Server:");
        System.out.println(message + "\n");

        splitMessage = message.split("\r\n");
        String size[];
        // content length header
        size = splitMessage[2].split(" ");
        // Split first line of response message
        String statusChecker[];
        statusChecker = splitMessage[0].split(" ");
        int sizeInt = Integer.parseInt(size[1]);
        // cache coming object from HTTP server
        if (sizeInt != 0 && statusChecker[1].equals("200")) {
            messageObject = message.substring(message.indexOf("<HTML>"));
            cachingObjects(size[1]);

        }

        /*
            Adding x-cache header for identify this object sent from cache or not.
            If from cache --> x-cache: HIT
            Or x-cache: MISS
         */
        if (statusChecker[1].equals("304")) {
            message = "HTTP/1.1 200 OK" + "\r\n" + splitMessage[1] + "\r\n" + "ContentLength: " + ProxyToHtppServer.responseIndex + "\r\n" + "X-Cache: HIT\r\n" + "\r\n" + sendFromCache();
        } else {
            message = splitMessage[0] + "\r\n" + splitMessage[1] + "\r\n" + splitMessage[2] + "\r\nX-Cache: MISS" + message.substring(message.indexOf("\r\n\r\n"));
        }

        sendToClient(message);
    }

    //cache object coming from HTTP server
    private void cachingObjects(String size) throws IOException {
        String FILE_TO_SEND = "C:/Users/hrnoz/Desktop/Proxy-Server/" + size + ".txt";
        File file = null;
        FileWriter filewriter = null;

        file = new File(FILE_TO_SEND);
       // if (!file.exists()) {
            filewriter = new FileWriter(file);


            filewriter.write(messageObject);
            filewriter.close();
       // }
    }

    /*
    Send object from cache file system because HTTP server returned 304
    Not Modified
     */
    private String sendFromCache() throws IOException {
        String filePath = "C:/Users/hrnoz/Desktop/Proxy-Server/" + ProxyToHtppServer.responseIndex + ".txt";
        File file = new File(filePath);
        FileInputStream fileInputStream;
        BufferedInputStream bufferedInputStream;

        byte[] cachedObject = new byte[(int) file.length()];
        fileInputStream = new FileInputStream(file);
        bufferedInputStream = new BufferedInputStream(fileInputStream);
        bufferedInputStream.read(cachedObject, 0, cachedObject.length);
        return new String(cachedObject);
    }

    /*
    Send final response message to the Client.
     */
    private void sendToClient(String message) throws IOException {

        reply = message.getBytes();
        System.out.println("Response Message from Proxy Server to Client");
        System.out.println(message + "\n");
        outToClient.write(reply);
    }


}

/*
    Coordinator class.
    Create object for each class.
    Creating server port 8080.
    I send all messages directly to the HTTP server as mentioned about project file.
 */
class Task implements Runnable {

    ExecutorService exe;
    private Socket client;
    private boolean serverStatus = true;

    public Task(Socket socket, ExecutorService exe) {
        this.client = socket;
        this.exe = exe;

    }

    @Override
    public void run() {
        /*
        Creating Input and Output Streams.
         */
        try {
            Socket server = null;
            final InputStream inFromClient = client.getInputStream();
            final OutputStream outToClient = client.getOutputStream();

            try {
                server = new Socket("localhost", 8080);
            } catch (IOException e) {
                /*
                If HTTP server is not running then
                server Status initialized to false.
                 */

                System.out.println("HTTP Server may not be running!");
                serverStatus = false;
            }

            InputStream inFromServer = null;
            OutputStream outToserver = null;

            /*
             I don't need socket communication between HTTP server and Proxy
             Because Server is not running!
             */
            if (serverStatus) {
                inFromServer = server.getInputStream();
                outToserver = server.getOutputStream();
            }
            /*
            Creating classes objects with necessary parameters.
             */
            ProxyToHtppServer toHttpServer = new ProxyToHtppServer(inFromClient, outToserver, outToClient, client, server, serverStatus);
            exe.submit(toHttpServer);

            ProxyToClient proxyToClient = new ProxyToClient(inFromServer, outToClient, client, server);
            exe.submit(proxyToClient);

        } catch (IOException ex) {

            Logger.getLogger(Task.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}