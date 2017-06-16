import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;

public class Server {
    protected ServerSocket listener;
    protected int port;
    protected int numClients;
    protected int clientId;
    protected int maxClients;
    protected HashMap<String, String> credens;
    protected HashMap<String, PrintWriter> clients;

    public Server(int port) {
        this.port = port;
        this.numClients = 0;
        this.clientId = 0;
        this.maxClients = 3;
        this.clients = new HashMap<>();
        this.credens = new HashMap<>();
        this.loadCredentials();
    }

    //load credentials from database
    public void loadCredentials() {
        try {
            BufferedReader db = new BufferedReader(new FileReader("db.txt"));
            String currLine;
            
            while ((currLine = db.readLine()) != null) {
                String tokens[] = currLine.split(" ");
                
                if (tokens.length >= 2) {
                    this.credens.put(tokens[0], tokens[1]);
                }
                
            }
            
            db.close();
            System.out.println("Database loaded.\n");
        }
        catch (IOException e) {
            System.out.println("Error: could not load credentials");
        }
    }

    //Start server socket on given port
    public void run() throws IOException {
        this.listener = new ServerSocket(this.port);
        
        try {
            while (true) {
                this.numClients++;
                this.clientId++;
                new Handler(this.listener.accept(), clientId).start();
            }
        }
        finally {
            this.numClients--;
            this.listener.close();
        }
    }

    //attempts to login the user
    public boolean login(String username, String password) {
        String p = this.credens.get(username);
        
        if (p != null && p.equals(password)) {
            send("System", "all", username + " has joined the ChatRoom.");
            
            return true;
        }
        
        return false;
    }

    //attempts to send message to whole chat room or specfifc user
    public boolean send(String user, String username, String message) {
        if (username.equals("all")) {
            System.out.println(user + " to all " + message);
            
            for (PrintWriter clientStream : this.clients.values()) {
                clientStream.println(user + ": " + message);
            }
            
            return true;
        }
        
        PrintWriter stream = this.clients.get(username);
        
        if (stream != null) {
            System.out.println(user + " to " + username + " " + message);
            stream.println(user + ": @" + username + " " + message);
            
            return true;
        }
        
        return false;
    }

    //attempts to log user out
    public boolean logout(String username) {
        if (clients.remove(username) != null) {
            send("System", "all", username + " has left the ChatRoom.");
            
            return true;
        }
        
        return false;
    }

    //attempts to register a new user
    public boolean register(String username, String password) {
        if (!this.credens.containsKey(username)) {
            this.credens.put(username, password);
            PrintWriter db;
            
            try {
                db = new PrintWriter(new BufferedWriter(new FileWriter("db.txt", true)));
                db.println(username + " " + password);
                db.close();
                
                System.out.println("System:" + username + " has joined ChatRoom.");
                
                return true;
            }
            catch (IOException e) {
                System.out.println("Error: Cannot add to the database");
            }
        }
        
        return false;
    }

    //starts server
    public static void main(String[] args) {
        Server server = new Server(14964);
        
        try {
            System.out.println("Welcome to ChatRoom 2.0 Server.");
            server.run();
        }
        catch (IOException e) {
            System.err.println("Error: Could not bind to port. Please close the port.\n");
            e.printStackTrace();
        }
    }

    private class Handler extends Thread {
        protected Socket socket;
        protected int clientID;
        protected String username;
        protected boolean authenticated;
        protected BufferedReader input;
        protected PrintWriter output;

        //Thread that manages requests from a client to server
        public Handler(Socket socket, int id) {
            this.socket = socket;
            this.clientID = id;
            this.authenticated = false;
        }

        public void run() {
            try {
                this.input = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                this.output = new PrintWriter(this.socket.getOutputStream(), true);

                String intro = "Welcome to ChatRoom 2.0.\n Enter help for a list of all commands.\n";
                this.output.println(intro);

                while (true) {
                    String input = this.input.readLine();

                    if (input == null) {
                        break;
                    }

                    String tokens[] = input.split("\\s+");

                    if (input.startsWith("register")) {
                        if (tokens.length != 3) {
                            this.output.println("Error: Invalid usage of register.");
                        }
                        else if (this.authenticated) {
                            this.output.println("Error: Account already exists.");
                        }
                        else if (tokens[1].length() > 32) {
                            this.output.println("Error: Username must be shorter than 32 characters.");
                        }
                        else if (tokens[2].length() > 8 || tokens[2].length() < 4) {
                            this.output.println("Error: Password must be between 4 and 8 characters.");
                        }
                        else {
                            if (register(tokens[1], tokens[2])) {
                                this.output.println("An account for " + tokens[1] + " has been created.");
                            }
                            else {
                                this.output.println("Error: Account not registered.");
                            }
                        }
                    }
                    else if (input.startsWith("login")) {
                        if (tokens.length != 3) {
                            this.output.println("Error: Invalid login usage.");
                        }
                        else if (clients.containsKey(tokens[1])) {
                            this.output.println("Error: User is logged in.");
                        }
                        else if (this.authenticated) {
                            this.output.println("Error: Already logged in.");
                        }
                        else {
                            boolean status = login(tokens[1], tokens[2]);

                            if (status) {
                                this.username = tokens[1];
                                this.authenticated = true;
                                this.output.println("Logged in. Welcome to ChatRoom.");
                                
                                clients.put(this.username, this.output);
                            }
                            else {
                                this.output.println("Error: Invalid credentials.");
                            }
                        }

                    }
                    else if (input.startsWith("who")) {
                        if (tokens.length != 1) {
                            this.output.println("Error: Invalid usage of who.");
                        }
                        else if (!this.authenticated) {
                            this.output.println("Please login.");
                        }
                        else {
                            this.output.println(clients.keySet().toString());
                        }
                        
                    }
                    else if (input.startsWith("send")) {
                        if (tokens.length < 3) {
                            this.output.println("Error: Invalid usage of send.");
                        }
                        else if (!this.authenticated) {
                            this.output.println("Please login.");
                        }
                        else {
                            String message = "";
                            
                            for (String part : Arrays.copyOfRange(tokens, 2, tokens.length)) {
                                message += part + " ";
                            }
                            if (!send(this.username, tokens[1], message)) {
                                this.output.println("Error: Client not found.");
                            }
                        }

                    }
                    else if (input.startsWith("logout")) {
                        if (tokens.length != 1) {
                            this.output.println("Error: Invalid usage of logout.");
                        }
                        else if (!this.authenticated) {
                            this.output.println("Please login.");
                        }
                        else {
                            System.out.println(this.username + " logged out");
                            this.output.println("Logging out...");
                            logout(this.username);
                            
                            break;
                        }

                    }
                    else if (input.startsWith("help")) {
                        this.output.println("register {username} {password}");
                        this.output.println("login {username} {password}");
                        this.output.println("logout");
                        this.output.println("send {user} {message}");
                        this.output.println("send all {message}");
                        this.output.println("who");
                        
                    }
                    else {
                        this.output.println("Command not found.  Try help.");
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                try {
                    this.socket.close();
                    System.out.println("Connection to client closed.");
                    logout(this.username);
                }
                catch (IOException e) {
                    System.out.println("Error: Cannot close socket.");
                }
            }
        }
    }
}
