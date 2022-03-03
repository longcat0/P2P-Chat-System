
import java.io.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime; 
import registry.*;
  

public class PeerProcess {

    Socket socket;
    BufferedReader in;
    PrintWriter out; 
    ConcurrentHashMap<String,Peer> peerLog = new ConcurrentHashMap<String,Peer>();
    ConcurrentHashMap<String,Vector<Peer>> sources = new ConcurrentHashMap<String,Vector<Peer>>();
    ConcurrentHashMap<String,String> Dates = new ConcurrentHashMap<String,String>();

    UdpServer server;

    //ArrayList<String> sources = new ArrayList<String>();
    Scanner scan = new Scanner(System.in);

    public void runProcess(String address, int port, String teamName) {



        // Add code to handle duplicate requests after
        try {
            
            // Make a connection to the given address and port number
            socket = new Socket(address, port);

            // Create input and output streams to read/write
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream());

            String request;
            boolean connected = true;
            while(connected) { 

                request = in.readLine();

                // Check what the request is and take the appropriate action
                switch(request) {

                    case "get team name":
                        System.out.println(request);
                        handleTeamNameRequest(teamName);
                        break;

                    case "get code":
                        System.out.println(request);
                        handleCodeRequest("PeerProcess.java");
                        break;
                    
                    case "receive peers":
                        System.out.println(request);
                        handleReceivePeersRequest();
                        break;
                    
                    case "get report":
                        System.out.println(request);
                        handleReportRequest();
                        break;
                    
                    case "close":
                        System.out.println(request);
                        connected = false;
                        break;

                    case "get location":
                        System.out.println(request);
                        handleLocationRequest();
                        break;
                    default: 
                        System.out.println("Invalid request");
                }
            }


            // Close stuff
            socket.close();
            out.close();
            in.close();



        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Handles a team name request by putting the team name in the appropriate format
     * and then sending it to the server through the Soutput stream
     * 
     * @param teamName
     */
    public void handleTeamNameRequest(String teamName) {

        String response; // Holds the response message

        try {

            // Prompt for user to continue
            System.out.println("Sending the team name...");
            System.out.println("Please hit ENTER to continue");
            scan.nextLine();
    
            // Prepare team name
            response = teamName + "\n";
    
            // Send team name
            out.print(response);
            out.flush();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles a code request by first reading the current file to get the source code,
     * formatting the read source code into the appropriate format, and then sending it
     * to the server
     * 
     * @param fileName - Name of the file to read from
     */
    public void handleCodeRequest(String fileName) {

        // Prompt for user to continue
        System.out.println("Sending code...");
        System.out.println("Please hit ENTER to continue");
        scan.nextLine();

        // Source code will be read into this string
        String sourceCode = "";
        
        // Create FileReader and read in the source code
        try (FileReader javaReader = new FileReader(fileName)) {

            int data;  // Single character

            while (true) {

                data = javaReader.read(); // Read in single character

                if (data == -1) break; // Break if the reader reaches EOF

                sourceCode = sourceCode + (char)data;

            }

            javaReader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Prepare the code response
        String response = prepareCodeResponse(sourceCode);
        out.print(response);
        out.flush();        

    }

    /**
     * Puts the input source coce into the appropriate format so it can be sent to 
     * the server 
     * 
     * @param sourceCode - Entire source code stored in a single string
     * @return - Returns the source code request in the appropriate format
     */
    public static String prepareCodeResponse(String sourceCode) {

        String response = "Java\n";

        response = response + sourceCode;
        response = response + "\n";
        response = response + "..."; // End of line code
        response = response + "\n";

        return response; 

    }

    /**
     * Receives list of peers from the server and stores them into a HashMap
     */
    public void handleReceivePeersRequest() {
    
        int numOfUsers;

        try {

            numOfUsers = Integer.parseInt(in.readLine());

            // Prompt for user to continue
            System.out.println("Number of user: " + numOfUsers);
            System.out.println("Please hit ENTER to read the received peers");
            scan.nextLine();
    
            // Read in the list of peers
            String peer; 
            String[] peerAddress; 
            
            for (int i = 0; i < numOfUsers; i++) {
    
                peer = in.readLine();
                peerAddress = peer.split(":"); // Split the address and port number

                
                // Create a new peer object and add it to the HashMap
                Peer temp = new Peer(); 
                temp.address = peerAddress[0];
                temp.port = Integer.parseInt(peerAddress[1]);
                peerLog.putIfAbsent(peer, temp); // Add a new peer only if it doesn't already exist. Else, do nothing

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");  
                LocalDateTime now = LocalDateTime.now();
                String date = dtf.format(now).toString();

                if(sources.size() > 0){
                    if(!sources.containsKey(socket.getRemoteSocketAddress().toString())){
                        Vector<Peer> vec = new Vector<>();
                        vec.add(temp);
                        sources.put(socket.getRemoteSocketAddress().toString(),vec);
                        Dates.putIfAbsent(socket.getRemoteSocketAddress().toString(), date);
                          
                    } else {
                        Boolean duplicate = false;
                        for(Peer p : sources.get(socket.getRemoteSocketAddress().toString())) {
                            if ((p.address).equals(temp.address)) duplicate = true;
                        }
                        if (!duplicate) sources.get(socket.getRemoteSocketAddress().toString()).add(temp);
                        Dates.putIfAbsent(socket.getRemoteSocketAddress().toString(), date);
                    }
                } else {
                    Vector<Peer> vec = new Vector<>();
                    vec.add(temp);
                    sources.put(socket.getRemoteSocketAddress().toString(),vec);
                    Dates.putIfAbsent(socket.getRemoteSocketAddress().toString(), date);
                }

                // System.out.println("IP addr: " + socket.getRemoteSocketAddress().toString());
                // System.out.println("Peer val: " + peer);
                // System.out.println("Peer log val" + peerLog.get(peer));
                // System.out.println("hash map size: " + peerLog.size());
    
            }

        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // for (String add : peerLog.keySet()) {
        //     System.out.println("Address: " + add + " " + "PortNum: " + peerLog.get(add).port);
        // }

        // Peer something = peerLog.putIfAbsent("192.168.1.83:52857", new Peer());
        // System.out.println("Test: " + something);

    }

    // create a reponse report

    public void handleReportRequest() {

        // number of peers
        String numOfPeers = Integer.toString(peerLog.size());
        System.out.println("Number of peers: " + numOfPeers);

        //* num of peers 
        String report = numOfPeers + "\n";

        // peers
        for(String key : peerLog.keySet()){
            Peer temp = peerLog.get(key);
            //* peers
            report = report + temp.address + ":" + temp.port + "\n";
            System.out.println(temp.address + ":" + temp.port);
        }

        // number of sources
        ///* add the num of sources
        report = report + Integer.toString(sources.size()) + "\n";
        System.out.println("Number of sources: " + sources.size());

        //Sources
        for(String key : sources.keySet()){
            
            //* add the source location
            report = report + key.substring(1)+ "\n";
            System.out.println(key.substring(1));

            //* add the date
            report = report + Dates.get(key).toString() + "\n";
            System.out.println(Dates.get(key).toString());

            //* add the num of peers
            report = report + Integer.toString(sources.get(key).size()) + "\n";
            System.out.println(sources.get(key).size());

            for(int i = 0; i < sources.get(key).size(); i++){
                System.out.println(sources.get(key).get(i).address + ":" + sources.get(key).get(i).port);
                //* add each peer for that source 
                report = report + sources.get(key).get(i).address + ":" + sources.get(key).get(i).port + "\n";
            }
        }

        out.print(report);
        out.flush();

    }

    public void handleLocationRequest() {

        String location = server.getIP();
        String response = location + ":" + server.getPortNum() + "\n";
        System.out.println("My location: " + response);

        out.print(response);
        out.flush();

    }

    public static void main(String[] args) {

        if (args.length != 4) {
			System.out.println("Please give provide three command-line arguments in the following order: IP address, TCP port number, team name, UDP port number");
			System.exit(0);
		}

        PeerProcess process = new PeerProcess();

        String address = args[0];

		int port = Integer.parseInt(args[1]);

		String teamName = args[2];

        int udpPort = Integer.parseInt(args[3]);

        try {
            DatagramSocket udpSocket = new DatagramSocket(udpPort);
            process.server = new UdpServer(udpSocket, process.peerLog);
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        }

        process.runProcess(address, port, teamName); // Send information to the server

        process.server.run();

        //process.runProcess(address, port, teamName); // Reconnect to the server and send final report

        System.out.println("Finish");
    }

    
}

class UdpServer {

    private DatagramSocket udpSocket;
    private boolean running;
    private ConcurrentHashMap<String,Peer> peerList = new ConcurrentHashMap<String,Peer>();
    private ConcurrentHashMap<String,Peer> activePeers;
    private AtomicInteger timestamp = new AtomicInteger(); 


    public UdpServer(DatagramSocket server, ConcurrentHashMap<String,Peer> peers) {    
        
        running = true;
        udpSocket = server;
        peerList = peers;

    }

    public int getPortNum() {
        return udpSocket.getLocalPort();
    }

    public String getIP() {
        String address = "";
        try {
            address = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return address;
    }

    public void run() {

        // Create copy of the peers list. This copy will store the list of active peers while the original stores all peers
        activePeers = new ConcurrentHashMap<String,Peer>(peerList);

        SenderThread sender = new SenderThread();
        ReaderThread reader = new ReaderThread();

        Thread t1 = new Thread(sender);
        Thread t2 = new Thread(reader);

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println("Done");
    }

    class SenderThread implements Runnable {

        Scanner scan = new Scanner(System.in);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");  

        public void run() {
            
            // Create task that sends peer message every 3 seconds
            Timer timer = new Timer();
            TimerTask task = new Send();
            timer.scheduleAtFixedRate(task, 1000, 5000);

            SnippetSender snipSend = new SnippetSender();
            Thread t = new Thread(snipSend);
            t.start();
            
            while (!t.isInterrupted()) {
                if (!running) {
                    t.interrupt();
                    timer.cancel();
                    System.out.println("t is interrupted " + t.isInterrupted());
                    scan.close();

                }
            }
        }
        
        class Send extends TimerTask {

            public void run() {

                // Getting random peer to send
                Random random = new Random();
                ArrayList<String> keys = new ArrayList<>(activePeers.keySet());
                String randomPeer = keys.get(random.nextInt(keys.size()));

                for (String address : keys) { // Send the peer to all active peers 
        
                    try {

                        //System.out.println("Sending " + randomPeer + " to " + address);
                        String message = "peer" + randomPeer;
                        InetAddress ipAddress = InetAddress.getByName(peerList.get(address).address);
                        DatagramPacket peerMessage = new DatagramPacket(message.getBytes(), message.getBytes().length, ipAddress, peerList.get(address).port);
                        udpSocket.send(peerMessage);
    
                    } catch (UnknownHostException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
        
                }
            }
        }

        class SnippetSender implements Runnable {

            public void run() { // HOW TO UNBLOCK SCANNER?

                while(running) {
                    String snippet; 
                    snippet = scan.nextLine();
                    if (!snippet.replaceAll("\\s","").equals("")) { // Snippet message is not empty
                        System.out.println("Your message: " + snippet);
                        // LocalDateTime now = LocalDateTime.now();
                        // String date = dtf.format(now).toString();
                        int snippetTimestamp = timestamp.incrementAndGet();
                        
                        // Create snippet message in datagram packet 
                        String snippetMsg = "snip" + snippetTimestamp + " " +  snippet;
                        
                        // Send the snippet to all active peers
                        for (String address : activePeers.keySet()) {

                            try {
                                System.out.println("Sending snippet to " + address);
                                InetAddress ipAddress = InetAddress.getByName(peerList.get(address).address);
                                DatagramPacket peerMessage = new DatagramPacket(snippetMsg.getBytes(), snippetMsg.getBytes().length, ipAddress, peerList.get(address).port);
                                udpSocket.send(peerMessage);
                                System.out.println(snippetMsg);
                                
                            } catch (Exception e) {
                                //TODO: handle exception
                                e.printStackTrace();
                            }
                        }
                    }
                    

                }
                // Send the snippet message 
            }
        }
        
        
    }



    class ReaderThread implements Runnable {

        public void run() {
    
            byte[] buf = new byte[256];
            DatagramPacket receivedMsg = new DatagramPacket(buf, 256);
            try {
                udpSocket.setSoTimeout(1500);
            } catch (SocketException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            while (running) { // Keep receiving packets 
        
                try {
                    udpSocket.receive(receivedMsg);
                    String message = new String(receivedMsg.getData(), receivedMsg.getOffset(), receivedMsg.getLength());
                    String senderAddress = receivedMsg.getAddress().getHostAddress();
                    int senderPort = receivedMsg.getPort();
    
                    MessageHandler handler = new MessageHandler(message, senderAddress, senderPort);
                    Thread t = new Thread(handler);
    
                    t.start();
    
    
                } catch (Exception e) {
                    continue;
                }
    
            }
    
            System.out.println("Reading stopped");
        }
    
        class MessageHandler implements Runnable {
    
            String message;
            String senderAddress;
            int senderPortNum;
    
            public MessageHandler(String message, String address, int portNum) {
                this.message = message;
                this.senderAddress = address;
                this.senderPortNum = portNum;
            }
    
    
            public void run() {

                System.out.println(message);
    
                if (message.equals("stop")) {
                    // shutdown action
                    running = false;
                    System.out.println("Stopping");
                    return;
                }
    
                // not a stop message, so parse it
                Pattern pattern1 = Pattern.compile("peer.*", Pattern.CASE_INSENSITIVE);
                Pattern pattern2 = Pattern.compile("snip.*", Pattern.CASE_INSENSITIVE);
    
                Matcher match1 = pattern1.matcher(message);
                Matcher match2 = pattern2.matcher(message);
    
                if (match1.find()) { // Peer message handling
    
                    message = message.replaceFirst("peer", ""); // Contains information about a third peer
    
                    String[] info = message.split(":");
                    String peerAddress = info[0];
                    int peerPortNum = Integer.parseInt(info[1]);
    
                    Peer tempPeer = new Peer();
                    tempPeer.address = senderAddress;
                    tempPeer.port = senderPortNum;

                    //System.out.println("Got " + peerAddress + ":" + peerPortNum + " From " + senderAddress + ":" + senderPortNum);
    
                    // Add sender to the list of peers and active peers
                    peerList.putIfAbsent(senderAddress + ":" + senderPortNum, tempPeer);
                    activePeers.putIfAbsent(senderAddress + ":" + senderPortNum, tempPeer);

    
                    // Add the third peer to the list of peers
                    Peer tempPeer2 = new Peer();
                    tempPeer.address = peerAddress;
                    tempPeer.port = peerPortNum;
                    peerList.putIfAbsent(peerAddress + ":" + peerPortNum, tempPeer2);
            
                } else if (match2.find()) { // Snippet message handling 
                    
                    message = message.replaceFirst("snip", ""); 

                    String[] info = message.split(" ");
                    int time = Integer.parseInt(info[0]);
                    String content = info[1];

                    timestamp.set(Math.max(timestamp.get(), time));
    
                    System.out.println(timestamp.get() + " " + content);

                }
    
    
            }
         }
    }

}
