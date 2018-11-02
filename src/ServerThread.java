import java.net.*;
import java.io.*;

public class ServerThread extends Thread {
    
    private int port;
    
    public ServerThread(int port) {
        this.port = port;
    }
    
    public static void main(String[] args) {
        loadApplicationData();
        new ServerThread(Utilities.PORT).start();
    }
    
    public void run() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
        }
        catch (IOException ioe) {
            Utilities.communicateError("Error binding to port \"" + port + "\"", true);
            return;
        }
        while (true) {
            try {
                Socket client = serverSocket.accept();
                new PlayerThread(client).start(); 
            }
            catch (IOException ioe) {
                Utilities.communicateError("Error while receiving a client connection", false);
            }
        }
    }
    
    private static void loadApplicationData() {
        if (!User.loadUsersFromFile()) {
            Utilities.communicateError("Proceeding after troubled loading of user data", false);
        }
        Clue.loadCluesFromFile();
        if (!Clue.getDeckIterator().hasNext()) {
            Utilities.communicateError("Unable to proceed after failing to load any clues", true);
            System.exit(1);
        }
    }
    
}