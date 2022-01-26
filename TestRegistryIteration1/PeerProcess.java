import java.io.*;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.HashMap;




public class PeerProcess {

    Socket socket;
    BufferedReader in;
    PrintWriter out; 
    HashMap<String,Peer> peerLog = new HashMap<String,Peer>();
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
                        // Report handling goes here
                        break;
                    
                    case "close":
                        System.out.println(request);
                        connected = false;
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
     * and then sending it to the server through the output stream
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
    
            }

        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Printing out peers and testing duplicates. DELETE LATER
        // for (String add : peerLog.keySet()) {
        //     System.out.println("Address: " + add + " " + "PortNum: " + peerLog.get(add).port);
        // }

        // Peer something = peerLog.putIfAbsent("192.168.1.83:52857", new Peer());
        // System.out.println("Test: " + something);

    }

    public void handleReportRequest() {

    }

    public static void main(String[] args) {

        if (args.length != 3) {
			System.out.println("Please give provide three command-line arguments in the following order: IP address, port number, team name");
			System.exit(0);
		}

        PeerProcess process = new PeerProcess();

        String address = args[0];

		int port = Integer.parseInt(args[1]);

		String teamName = args[2];

        process.runProcess(address, port, teamName);

    }

    
}