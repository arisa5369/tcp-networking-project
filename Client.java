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

                while (true) {
                    System.out.print("Dergo mesazh/komande: ");
                    String message = scanner.nextLine().trim();
                    if (message.equalsIgnoreCase("exit")) break;

                    if (!isAdmin && (message.startsWith("/upload") || message.startsWith("/delete"))) {
                        System.out.println("️ Pa qasje (vetëm admin).");
                        continue;
                    }

                    if (message.startsWith("/upload")) {
                        String[] parts = message.split(" ", 2);
                        if (parts.length < 2) {
                            System.out.println("Përdorimi: /upload emri.txt");
                            continue;
                        }
                        out.println(message);
                        String signal = in.readLine();
                        if ("OK_UPLOAD".equals(signal)) {
                            System.out.println("Shkruaj përmbajtjen e file-it (shkruaj 'EOF' për fund):");
                            while (true) {
                                String line = scanner.nextLine();
                                out.println(line);
                                if ("EOF".equals(line)) break;
                            }
                        }
                        System.out.println("Pergjigje nga server:\n" + readFullResponse(in));
                    }

                    else if (message.startsWith("/download")) {
                        String[] parts = message.split(" ", 2);
                        if (parts.length < 2) {
                            System.out.println("Përdorimi: /download emri.txt");
                            continue;
                        }
                        String fileName = parts[1].trim();
                        out.println(message);
                        File localFile = new File("downloaded_" + fileName);
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(localFile))) {
                            String line;
                            boolean fileFound = false;
                            while ((line = in.readLine()) != null) {
                                if ("EOF".equals(line)) break;
                                if (line.equals("FILE_NOT_FOUND")) {
                                    System.out.println(" File-i nuk ekziston në server.");
                                    fileFound = false;
                                    break;
                                }
                                writer.write(line);
                                writer.newLine();
                                fileFound = true;
                            }
                            if (fileFound) {
                                System.out.println(" File-i u shkarkua me sukses si: " + localFile.getName());
                            }
                        } catch (IOException e) {
                            System.out.println(" Gabim gjatë ruajtjes së file-it: " + e.getMessage());
                        }
                    }

                    else {
                        out.println(message);
                        System.out.println("Pergjigje nga server:\n" + readFullResponse(in));
                    }
                }

                System.out.println("Po dilni nga klienti...");
                break;

            } catch (IOException e) {
                System.out.println("Lidhja deshtoi: " + e.getMessage());
                System.out.println("Po provoj përsëri pas 5 sekondash...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private static String readFullResponse(BufferedReader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            if ("EOF".equals(line)) break;
            sb.append(line).append("\n");
        }
        return sb.toString().trim();
    }
}