import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    protected Socket socket;
    protected String ip;
    protected int port;
    protected BufferedReader input;
    protected PrintWriter output;

    public Client(int port) {
        this.ip = "127.0.0.1";
        this.port = port;
    }

    //connects client to server
    public void connect() throws IOException {
        this.socket = new Socket(this.ip, this.port);
        this.input = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

        System.out.println(this.input.readLine());

        new Handler(this.socket).start();

        while (true) {
            try {
                String currResponse = this.input.readLine();
                
                if (currResponse != null && currResponse.length() > 0) {
                    System.out.println(currResponse);
                    System.out.print(">> ");
                }
            }
            catch (IOException e) {
                e.getStackTrace();
            }

        }
    }

    //runs the CLI
    public static void main(String[] args) {
        Client client = new Client(14964);
        
        try {
            client.connect();
        }
        catch (IOException e) {
            System.out.println("Error: Connection refused.");
            e.printStackTrace();
        }
    }

    //hanlder to allow keyboard input and print responses from the server
    private class Handler extends Thread {
        protected Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                output = new PrintWriter(this.socket.getOutputStream(), true);

                while (true) {
                    System.out.print(">> ");

                    Scanner scanner = new Scanner(System.in);
                    String userCommand = scanner.nextLine();

                    output.println(userCommand);

                    if (userCommand.equals("logout")) {
                        System.exit(1);
                    }
                }
            }
            catch (IOException e) {
                e.getStackTrace();
            }
        }
    }
}
