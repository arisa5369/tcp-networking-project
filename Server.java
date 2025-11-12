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
}