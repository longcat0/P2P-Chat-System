import java.io.*;
import java.io.IOException;
import java.lang.reflect.Array;
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
import java.util.HashMap;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime; 
import registry.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentLinkedQueue;
  

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
                            if ((p.address + ":" + p.port).equals(temp.address + ":" + temp.port)) duplicate = true;
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

            // add all the peers for source
            for(int i = 0; i < sources.get(key).size(); i++){
                System.out.println(sources.get(key).get(i).address + ":" + sources.get(key).get(i).port);
                //* add each peer for that source 
                report = report + sources.get(key).get(i).address + ":" + sources.get(key).get(i).port + "\n";
            }
        }

        // Peer msg's recvd from udp
        Queue<String> messagesRecvd = server.getMessagesRecvd(); 

        // Num of peer messages recvd via udp
        report = report + Integer.toString(messagesRecvd.size()) + "\n";

        // Add each peer message recvd to report
        // Message format: <source peer><space><received peer><space><date><newline> 
        for(String elem : messagesRecvd){
            report = report + elem + "\n";
        }

        // Peers msg's sent from udp
        Queue<String> messagesSent = server.getMessagesSent(); 

        // Num of peer messages sent via udp
        report = report + Integer.toString(messagesSent.size()) + "\n";

        // Add each peer message sent to report
        // Message format: <sent to peer><space><peer sent><space><date><newline> 
        for(String elem : messagesSent){
            report = report + elem + "\n";
        }

        // Snippets Recvd
        Queue<String> snippetsRecvd = server.getSnippetsRecvd();

        // Num of snippets recvd
        report = report + Integer.toString(snippetsRecvd.size()) + "\n";

        // Add each snippet recvd
        // snippetRecvd format: <timestamp><space><content><space><source peer><newline> 
        for(String elem : snippetsRecvd){
            report = report + elem + "\n";
        }

        out.print(report);
        out.flush();

    }

    // Handles the location request
    public void handleLocationRequest() {

        String location = server.getIP();
        String response = location + ":" + server.getPortNum() + "\n";
        System.out.println("My location: " + response);

        out.print(response);
        out.flush();

    }

    public static void main(String[] args) {
        // Check if the correct number of arguments are provided
        if (args.length != 4) {
			System.out.println("Please give provide three command-line arguments in the following order: IP address, TCP port number, team name, UDP port number");
			System.exit(0);
		}

        // Create a new peer process
        PeerProcess process = new PeerProcess();

        // IP addr for TCP connection
        String address = args[0];

        // Port for TCP connection 
		int port = Integer.parseInt(args[1]);

		String teamName = args[2];

        int udpPort = Integer.parseInt(args[3]);

        // create a udp server and start it 
        try {
            DatagramSocket udpSocket = new DatagramSocket(udpPort);
            process.server = new UdpServer(udpSocket, process.peerLog);
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Run the process -> connect to the server using TCP connection and get and send the intial information
        process.runProcess(address, port, teamName); // Send information to the server

        // Run the udp server and store the result which is update num of peers into the peer log for the report
        process.peerLog = process.server.run();

        //Reestablish TCP connection with the server to send the updated report
        process.runProcess(address, port, teamName); // Reconnect to the server and send final report

        System.out.println("Finish");
        // Close the software
        System.exit(0);
    }

    
}

class UdpServer {

    // The udp socket that this server will use
    private DatagramSocket udpSocket;
    // The running atomic boolean flag that will be shared to all threads and will be used to stop the threads when needed
    private AtomicBoolean running = new AtomicBoolean(true);

    // A thread safe queue that stores all the peer messages recvd by the udp server
    private Queue<String> messagesRecvd = new ConcurrentLinkedQueue<String>();

    //  A thread safe queue that stores all the peer messages sent by the udp server
    private Queue<String> messagesSent = new ConcurrentLinkedQueue<String>();

    //  A thread safe queue that stores all the snippets recvd by the udp server 
    private Queue<String> snippetsRecvd = new ConcurrentLinkedQueue<String>();

    // A thread safe hashmap that stores all the peers known to this peer
    // The key is the addr -> ip:portnum
    private ConcurrentHashMap<String,Peer> peerList = new ConcurrentHashMap<String,Peer>();

    // A thread safe hashmap that stores all active peers known to this peer
    // The key is the addr -> ip:portnum
    private ConcurrentHashMap<String,Peer> activePeers;

    // A thread safe hashmap that stores all the inactive peers known to this peer
    // The key is the addr -> ip:portnum
    private ConcurrentHashMap<String,Peer> inactivePeers;

    // A thread safe hashmap used to track timeouts
    // The key is the addr -> ip:port num and the int is the amount of cycles in which no messages has been recvd from the attached addr
    private HashMap<String, Integer> timeOuts;

    // Used to keep track of the time stamps of the snippets message. AtomicInteger therefore threadsafe
    private AtomicInteger timestamp = new AtomicInteger(); 


    public UdpServer(DatagramSocket server, ConcurrentHashMap<String,Peer> peers) {    
        
        AtomicBoolean running = new AtomicBoolean(true);
        // running = true;
        udpSocket = server;
        peerList = peers;

    }

    public Queue<String> getMessagesRecvd(){
        return this.messagesRecvd;
    }

    public Queue<String> getMessagesSent(){
        return this.messagesSent;
    }

    public Queue<String> getSnippetsRecvd(){
        return this.snippetsRecvd;
    }

    public int getPortNum() {
        return udpSocket.getLocalPort();
    }

    public String getIP() {
        String address = "";
        try {
            address = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return address;
    }

    // Run method for this thread, returns the updated list of peers 
    public ConcurrentHashMap<String,Peer> run() {

        // Create copy of the peers list. This copy will store the list of active peers while the original stores all peers
        activePeers = new ConcurrentHashMap<String,Peer>(peerList);
        inactivePeers = new ConcurrentHashMap<String,Peer>();
        timeOuts = new HashMap<String, Integer>();

        // Deals with all sending of messages from this udp server
        SenderThread sender = new SenderThread();

        // Deals with all the messages recvd by this udp server
        ReaderThread reader = new ReaderThread();

        Thread t1 = new Thread(sender);
        Thread t2 = new Thread(reader);

        t1.start();
        t2.start();

        // Join once threads are done
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Done");

        return peerList;
    }

    // This class deals will all message sending done by the udp server
    class SenderThread implements Runnable {

        // Scanner so that user can input snippets to send to other peers
        Scanner scan = new Scanner(System.in);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");  

        // run method for this thread
        public void run() {

            // Create task that sends peer message every 5 seconds
            Timer timer = new Timer();
            TimerTask task = new Send();
            timer.scheduleAtFixedRate(task, 1000, 5000);

            SnippetSender snipSend = new SnippetSender();
            Thread t = new Thread(snipSend);
            t.start();
            
            // When running is false we must stop the child threads by interrupting it
            while (!t.isInterrupted()) {
                if (running.get() == false) {
                    t.interrupt();
                    // To get out if the scan
                    System.console().writer().print(" ");
                    timer.cancel();
                    System.out.println("t is interrupted " + t.isInterrupted());
                    // scan.close();

                }
            }
        }
        
        // Responsible for sending peer messages
        class Send extends TimerTask {

            public void run() {
                // Get all the keys of the currenly "inactive" peers
                ArrayList<String> inactive_keys = new ArrayList<>(inactivePeers.keySet());

                if(inactive_keys.size() > 0){
                    // Parse through the keys
                    for(String addr : inactive_keys) {
                        // 
                        timeOuts.putIfAbsent(addr, 0);
                        // If inactive_keys in active peers list add one to its time out
                        if(activePeers.containsKey(addr)){
                            // If timeout is greater than 4 it means no messages have been recvd by this peer for a while and therefore remove it from the active peers list
                            if(timeOuts.get(addr) > 4) {
                                System.out.println("*********************************************");
                                System.out.println("Peer at addr: " + addr + " timed out");
                                System.out.println("*********************************************");
                                activePeers.remove(addr);
                                timeOuts.remove(addr);
                            } else {
                                System.out.println("--------");
                                System.out.println("Added 1 to " + timeOuts.get(addr) + " on peer addr " + addr);
                                System.out.println("--------");
                                timeOuts.put(addr,timeOuts.get(addr)+1);
                            }
                        }

                    }
                }

                // Getting random peer to send
                Random random = new Random();
                ArrayList<String> keys = new ArrayList<>(activePeers.keySet());
                String randomPeer = ""; 
                if(!keys.isEmpty()) {
                    randomPeer = keys.get(random.nextInt(keys.size()));
                }

                for (String address : keys) { // Send the peer to all active peers 
        
                    try {

                        // System.out.println("Sending " + randomPeer + " to " + address); 
                        // Indicate the peer that is being sent and who it is being sent to 
                        String message = "peer" + randomPeer;
                        InetAddress ipAddress = InetAddress.getByName(peerList.get(address).address);
                        DatagramPacket peerMessage = new DatagramPacket(message.getBytes(), message.getBytes().length, ipAddress, peerList.get(address).port);
                        udpSocket.send(peerMessage);


                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");  
                        LocalDateTime now = LocalDateTime.now();
                        String date = dtf.format(now).toString();

                        // Format of storing peer messages sent for the report -> Send to peer /space/ peer sent /space/ date /newline
                        // Add the string in the above format to the messagesSent queue
                        messagesSent.add(address + " " + randomPeer + " " + date);

                        // Add the peer to inactive peers as we have sent it a message and are waiting for it to send a message back so for the time beings it's "inactive"
                        inactivePeers.putIfAbsent(address, peerList.get(address));
    
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
        
                }
            }
        }

        // Responsible for sending snippets from the udp server
        class SnippetSender implements Runnable {

            public void run() {

                while(running.get()) {
                    String snippet; 
                    // Read in the snippet given by the user in the console
                    snippet = scan.nextLine();
                    if (!snippet.replaceAll("\\s","").equals("")) { // Snippet message is not empty
                        // System.out.println("Your message: " + snippet); 

                        // if(snippet.equals("stop")){
                        //     running.set(true);
                        // }
                        // LocalDateTime now = LocalDateTime.now();
                        // String date = dtf.format(now).toString();
                        int snippetTimestamp = timestamp.incrementAndGet();
                        
                        // Create snippet message in datagram packet 
                        String snippetMsg = "snip" + snippetTimestamp + " " +  snippet;
                        
                        // Send the snippet to all active peers
                        for (String address : activePeers.keySet()) {

                            try {
                                System.out.println("Sending snippet to " + address); // Indicating who the snippet is being sent to 
                                InetAddress ipAddress = InetAddress.getByName(peerList.get(address).address);
                                DatagramPacket peerMessage = new DatagramPacket(snippetMsg.getBytes(), snippetMsg.getBytes().length, ipAddress, peerList.get(address).port);
                                udpSocket.send(peerMessage);
                                // System.out.println(snippetMsg); 
                                
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    

                }
                // Send the snippet message 
            }
        }
        
        
    }

    // Handles all the messages recvd by this peer
    class ReaderThread implements Runnable {

        public void run() {
    
            // Set the size of the message buffer
            byte[] buf = new byte[256];
            // Create a new udp datagram packet using the buffer
            DatagramPacket receivedMsg = new DatagramPacket(buf, 256);

            // Set the socket to time out in 1.5s
            try {
                udpSocket.setSoTimeout(1500);
            } catch (SocketException e1) {
                e1.printStackTrace();
            }

            // Create a thread pool of 6 fixed threads 
            ExecutorService pool = Executors.newFixedThreadPool(6); 

            while (running.get()) { // Keep receiving packets while running is true
        
                try {
                    udpSocket.receive(receivedMsg);

                    // Store the message part into message by parsing the received packet
                    String message = new String(receivedMsg.getData(), receivedMsg.getOffset(), receivedMsg.getLength());
                    // Store the senders ip addr
                    String senderAddress = receivedMsg.getAddress().getHostAddress();
                    //Store the sendets port num
                    int senderPort = receivedMsg.getPort();
    
                    // Create a new handler 
                    MessageHandler handler = new MessageHandler(message, senderAddress, senderPort);

                    // Give the job to the pool so it can be put in the queue and one of the threads will pick it up when they are avaliable
                    pool.execute(handler);
    
                    // t.start();
                } catch (Exception e) {
                    continue;
                }
    
            }
    
            System.out.println("Reading stopped");
        }
    
        // Parses a revcd message and does the apporiate action based on the message
        class MessageHandler implements Runnable {
    
            String message;
            // Info of the peer that sent this message
            String senderAddress;
            int senderPortNum;
    
            public MessageHandler(String message, String address, int portNum) {
                this.message = message;
                this.senderAddress = address;
                this.senderPortNum = portNum;
            }
    
            // 
            public void run() {

                // System.out.println(message); // Print the received message
    
                // If the message is stop change the atomic boolean flag running to false so that the peer starts to shutdown the udp server
                if (message.equals("stop")) {
                    // shutdown action
                    running.set(false);
                    System.out.println("Stopping");
                    return;
                }
    
                // If the message is not a stop message, parse it
                Pattern pattern1 = Pattern.compile("peer.*", Pattern.CASE_INSENSITIVE);
                Pattern pattern2 = Pattern.compile("snip.*", Pattern.CASE_INSENSITIVE);
                
                // Peer msg message
                Matcher match1 = pattern1.matcher(message);
                
                // Snippet message
                Matcher match2 = pattern2.matcher(message);
    
                if (match1.find()) { // Peer message handling
    
                    message = message.replaceFirst("peer", ""); // Contains information about a third peer
                    
                    // Extract the peer information sent and put it in appropriate variables
                    System.out.println(message);
                    String[] info = message.split(":");
                    String peerAddress = info[0];
                    int peerPortNum = Integer.parseInt(info[1].trim());
    
                    // Create a new peer with the message senders info so we can add it to the peerlist, activePeers and inactivePeers hashmaps
                    Peer tempPeer = new Peer();
                    tempPeer.address = senderAddress;
                    tempPeer.port = senderPortNum;

                    // System.out.println("Got " + peerAddress + ":" + peerPortNum + " From " + senderAddress + ":" + senderPortNum); // Show the peer in the message and who it came from
    
                    // Add sender to the list of peers and active peers
                    peerList.putIfAbsent(senderAddress + ":" + senderPortNum, tempPeer);
                    activePeers.putIfAbsent(senderAddress + ":" + senderPortNum, tempPeer);

                    // remove the peer from inactive peers list if its currently in it
                    if(inactivePeers.containsKey(senderAddress + ":" + senderPortNum)){
                        inactivePeers.remove(senderAddress + ":" + senderPortNum);
                        // Also remove the address from the timeOuts hashmap
                        timeOuts.remove(senderAddress + ":" + senderPortNum);
                    }
    
                    // Add the third peer to the list of peers that was sent by the sender peer as a peer message
                    Peer tempPeer2 = new Peer();
                    tempPeer2.address = peerAddress;
                    tempPeer2.port = peerPortNum;
                    peerList.putIfAbsent(peerAddress + ":" + peerPortNum, tempPeer2);
                    activePeers.putIfAbsent(peerAddress + ":" + peerPortNum, tempPeer2);



                    // Get the current date and time in the appropriate format
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");  
                    LocalDateTime now = LocalDateTime.now();
                    String date = dtf.format(now).toString();

                    // Format for storing peer messages recvd -> <source peer><space><received peer><space><date><newline>
                    // Add the above formatted string into the messagesRecvd concurrentlinkedqueue
                    messagesRecvd.add(senderAddress + ":" + senderPortNum + " " + peerAddress + ":" + peerPortNum + " " + date);
            
                } else if (match2.find()) { // Snippet message handling 

                    // remove from inactive peers list if present
                    if(inactivePeers.containsKey(senderAddress + ":" + senderPortNum)){
                        inactivePeers.remove(senderAddress + ":" + senderPortNum);
                        // Also remove the addr from timeouts
                        timeOuts.remove(senderAddress + ":" + senderPortNum);
                    }
                    
                    // reformat the message to remove snip
                    message = message.replaceFirst("snip", ""); 

                    // Spilt the message, parse it and appropirately assign the indiviual items to variables
                    String[] info = message.split(" ", 2);
                    int time = Integer.parseInt(info[0]);
                    String content = info[1];

                    // Get the max time between the timestamp atomic variable and the time sent by the peer message and update the timestamp with that value
                    timestamp.set(Math.max(timestamp.get(), time));
    
                    System.out.println(timestamp.get() + " " + content);

                    // snippet -> <timestamp><space><content><space><source peer><newline>  
                    snippetsRecvd.add(timestamp + " " + content + " " + senderAddress + ":" + senderPortNum);

                    // System.out.println("-----------");
                    // for(String elem : snippetsRecvd){
                    //     System.out.println(elem);
                    // }
                    // System.out.println("-----------");

                }
    
    
            }
        }
    }

}
