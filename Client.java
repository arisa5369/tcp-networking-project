import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String HOST = "10.11.66.4";
    private static final int PORT = 8080;

    public static void main(String[] args) {
        reconnectLoop();
    }

    private static void reconnectLoop() {
        while (true) {
            try (Socket socket = new Socket(HOST, PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 Scanner scanner = new Scanner(System.in)) {

                System.out.print("Username:password (per admin): ");
                String login = scanner.nextLine();
                out.println(login);
                String response = in.readLine();
                System.out.println(response);
                boolean isAdmin = response.contains("ADMIN_GRANTED");