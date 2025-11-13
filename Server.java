import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;

public class Server {
    private static final int PORT = 8080;
    private static final String HOST = "0.0.0.0";
    private static final int MAX_CONNECTIONS = 5;
    private static final int TIMEOUT_MS = 300000; // 5 min
    private static final ExecutorService executor = Executors.newFixedThreadPool(MAX_CONNECTIONS);
    private static final List<ClientHandler> activeClients = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, Integer> messagesPerClient = new ConcurrentHashMap<>();

    private static long totalBytesSent = 0;
    private static long totalBytesReceived = 0;

    private static final String LOG_DIR = "server_files";
    private static final String MESSAGE_LOG = "messages_log.txt";
    private static final String STATS_LOG = "server_stats.txt";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        File logDir = new File(LOG_DIR);
        if (!logDir.exists()) logDir.mkdirs();

        startStatsLogger();

        try (ServerSocket serverSocket = new ServerSocket(PORT, 0, InetAddress.getByName(HOST))) {
            System.out.println(" Serveri po dëgjon në portin " + PORT);
            while (true) {
                if (activeClients.size() >= MAX_CONNECTIONS) {
                    System.out.println(" Prag lidhjesh arritur. Duke pritur...");
                    Thread.sleep(1000);
                    continue;
                }
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                executor.execute(handler);
                activeClients.add(handler);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println(" Gabim në server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }
    private static void startStatsLogger() {
        Timer statsTimer = new Timer(true);
        statsTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                logServerStats();
            }
        }, 0, 30000);
    }
    private static void logServerStats() {
        synchronized (activeClients) {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Server Stats @ ").append(DATE_FORMAT.format(new Date())).append(" ===\n");
            sb.append("Lidhje aktive: ").append(activeClients.size()).append("/").append(MAX_CONNECTIONS).append("\n");
            sb.append("Klientë aktivë: ");
            for (ClientHandler c : activeClients) sb.append(c.clientIP).append(" ");
            sb.append("\nMesazhe për klient:\n");
            for (Map.Entry<String, Integer> e : messagesPerClient.entrySet()) {
                sb.append("  ").append(e.getKey()).append(" → ").append(e.getValue()).append(" mesazhe\n");
            }
}
        static class ClientHandler implements Runnable {
            private final Socket socket;
            private final BufferedReader in;
            private final PrintWriter out;
            private final String clientIP;
            private boolean isAdmin = false;
            private Timer timeoutTimer;

            public ClientHandler(Socket socket) throws IOException {
                this.socket = socket;
                this.clientIP = socket.getInetAddress().getHostAddress();
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.out = new PrintWriter(socket.getOutputStream(), true);
                socket.setSoTimeout(TIMEOUT_MS);
                resetTimeout();
                log(" Lidhje e re nga: " + clientIP);
            }
            @Override
            public void run() {
                try {
                    // Login
                    String login = in.readLine();
                    incrementBytesReceived(login);
                    if ("admin:password".equals(login)) {
                        isAdmin = true;
                        out.println("ADMIN_GRANTED");
                        incrementBytesSent("ADMIN_GRANTED");
                        log(" Admin u identifikua: " + clientIP);
                    } else {
                        out.println("READ_ONLY");
                        incrementBytesSent("READ_ONLY");
                        log(" Përdorues i rregullt: " + clientIP);
                    }

                    String message;
                    while ((message = in.readLine()) != null) {
                        incrementBytesReceived(message);
                        resetTimeout();
                        messagesPerClient.put(clientIP, messagesPerClient.getOrDefault(clientIP, 0) + 1);
                        log(" Mesazh nga " + clientIP + ": " + message);
                        processCommand(message.trim());
                        if (!isAdmin) Thread.sleep(500);
                    }
                } catch (SocketTimeoutException e) {
                    log(" Timeout për klientin: " + clientIP);
                } catch (IOException e) {
                    log(" Gabim komunikimi me " + clientIP + ": " + e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    closeConnection();
                }
            }
            private void processCommand(String command) {
                String[] parts = command.split(" ", 2);
                String cmd = parts[0];
                String arg = parts.length > 1 ? parts[1].trim() : null;
                File dir = new File(LOG_DIR);
                if (!dir.exists()) dir.mkdirs();
                try {
                    switch (cmd) {
                        case "/list":
                            String[] files = dir.list();
                            sendResponse(files == null || files.length == 0 ? "[]" : Arrays.toString(files));
                            break;

                        case "/read":
                            if (arg == null) {
                                sendResponse("ERROR: Specifiko emrin e skedarit.");
                                return;
                            }
                            File file = new File(dir, arg);
                            sendResponse(file.exists() ? readFile(file) : "ERROR: Skedari nuk ekziston.");
                            break;

                        case "/info":
                            if (arg == null) {
                                sendResponse("ERROR: Specifiko emrin e skedarit.");
                                return;
                            }

                            File f = new File(dir, arg);
                            if (f.exists()) {
                                sendResponse(String.format("File: %s | Madhësia: %d bytes | Modifikuar: %s",
                                        f.getName(), f.length(), DATE_FORMAT.format(new Date(f.lastModified()))));
                            } else sendResponse("ERROR: Skedari nuk ekziston.");
                            break;

                        case "/search":
                            if (arg == null) {
                                sendResponse("ERROR: Specifiko fjalën për kërkim.");
                                return;
                            }
                            List<String> results = new ArrayList<>();
                            for (String fileName : Objects.requireNonNull(dir.list())) {
                                String content = readFile(new File(dir, fileName));
                                if (content.toLowerCase().contains(arg.toLowerCase())) {
                                    results.add(fileName);
                                }
                            }
                            sendResponse(results.isEmpty() ? "NOT_FOUND: \"" + arg + "\"" : results.toString());
                            break;
                        case "/upload":
                            if (!isAdmin) {
                                sendResponse("ERROR: Pa qasje (admin required).");
                                return;
                            }
                            out.println("OK_UPLOAD");
                            incrementBytesSent("OK_UPLOAD");
                            StringBuilder content = new StringBuilder();
                            String line;
                            while ((line = in.readLine()) != null) {
                                incrementBytesReceived(line);
                                if ("EOF".equals(line)) break;
                                content.append(line).append("\n");
                            }
                            writeFile(new File(dir, arg), content.toString());
                            log(" Upload nga " + clientIP + ": " + arg + " (" + content.length() + " bytes)");
                            sendResponse("SUCCESS: Upload complete - " + arg);
                            break;

                        case "/delete":
                            if (!isAdmin) {
                                sendResponse("ERROR: Pa qasje.");
                                return;
                            }
                            File fileToDelete = new File(dir, arg);
                            sendResponse(fileToDelete.exists() && fileToDelete.delete()
                                    ? "SUCCESS: Fshirë - " + arg
                                    : "ERROR: Dështoi fshirja.");
                            break;

                        case "/download":
                            sendResponse(readFile(new File(dir, arg)));
                            break;

                        case "/stats":
                            sendResponse(String.format(
                                    "Aktivë: %d/%d | Mesazhe nga ty: %d | Bytes dërguar: %d | Bytes pranuar: %d",
                                    activeClients.size(), MAX_CONNECTIONS,
                                    messagesPerClient.getOrDefault(clientIP, 0),
                                    totalBytesSent, totalBytesReceived));
                            break;

                        default:
                            sendResponse("ERROR: Komandë e panjohur.");
                    }
                } catch (Exception e) {
                    log("Gabim në procesim për " + clientIP + ": " + e.getMessage());
                    sendResponse("SERVER_ERROR: " + e.getMessage());
                }
            }

            private void sendResponse(String response) {
                incrementBytesSent(response);
                out.println(response);
                out.println("EOF");
                incrementBytesSent("EOF");
            }

            private void incrementBytesSent(String msg) {
                totalBytesSent += msg.getBytes().length;
            }

            private void incrementBytesReceived(String msg) {
                totalBytesReceived += msg.getBytes().length;
            }
            private String readFile(File file) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line).append("\n");
                    return sb.toString().trim();
                } catch (IOException e) {
                    return "ERROR: Gabim gjatë leximit.";
                }
            }

            private void writeFile(File file, String content) {
                try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                    pw.print(content);
                } catch (IOException e) {
                    log("Gabim shkrimi: " + e.getMessage());
                }
            }

            private void resetTimeout() {
                if (timeoutTimer != null) timeoutTimer.cancel();
                timeoutTimer = new Timer(true);
                timeoutTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        log("Timeout: Mbyllje e detyruar për " + clientIP);
                        closeConnection();
                    }
                }, TIMEOUT_MS);
            }

            private void closeConnection() {
                try {
                    if (timeoutTimer != null) timeoutTimer.cancel();
                    if (socket != null && !socket.isClosed()) socket.close();
                    activeClients.remove(this);
                    log(" Lidhja u mbyll për: " + clientIP);
                } catch (IOException e) {
                    log("Gabim gjatë mbylljes: " + e.getMessage());
                }
            }
            private void log(String message) {
                String logEntry = String.format("[%s] %s", DATE_FORMAT.format(new Date()), message);
                System.out.println(logEntry);
                try (PrintWriter pw = new PrintWriter(new FileWriter(MESSAGE_LOG, true))) {
                    pw.println(logEntry);
                } catch (IOException e) {
                    System.err.println("Gabim logimi: " + e.getMessage());
                }
            }
        }
    }