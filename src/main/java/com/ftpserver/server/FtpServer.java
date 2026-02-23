package com.ftpserver.server;

import com.ftpserver.command.CommandHandler;
import com.ftpserver.config.ServerConfig;
import com.ftpserver.user.User;
import com.ftpserver.user.UserManager;
import com.ftpserver.util.Logger;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FtpServer {
    private ServerConfig config;
    private UserManager userManager;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running;
    private final List<ClientSession> clientSessions;
    private final List<ServerListener> listeners;
    private final Logger logger;

    public interface ServerListener {
        void onServerStarted();
        void onServerStopped();
        void onClientConnected(ClientSession session);
        void onClientDisconnected(ClientSession session);
    }

    public static class ClientSession {
        public final Socket socket;
        public final LocalDateTime connectTime;
        public User user;
        public volatile boolean active;
        public Thread handlerThread;

        public ClientSession(Socket socket) {
            this.socket = socket;
            this.connectTime = LocalDateTime.now();
            this.active = true;
        }

        public String getClientAddress() {
            String address = socket.getInetAddress().getHostAddress();
            if (address.equals("0:0:0:0:0:0:0:1")) {
                return "127.0.0.1";
            }
            return address;
        }

        public int getClientPort() {
            return socket.getPort();
        }
    }

    public FtpServer(ServerConfig config, UserManager userManager) {
        this.config = config;
        this.userManager = userManager;
        this.clientSessions = new CopyOnWriteArrayList<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.logger = Logger.getInstance();
        this.running = false;
    }

    public void addListener(ServerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ServerListener listener) {
        listeners.remove(listener);
    }

    public synchronized void start() throws IOException {
        if (running) {
            throw new IllegalStateException("Server is already running");
        }
        File rootDir = new File(config.getRootDirectory());
        if (!rootDir.exists()) {
            boolean created = rootDir.mkdirs();
            if (created) {
                logger.info("Root directory created: " + rootDir.getAbsolutePath(), "FtpServer", "-");
            }
        }
        threadPool = Executors.newCachedThreadPool();
        serverSocket = new ServerSocket(config.getPort());
        serverSocket.setSoTimeout(5000);
        running = true;
        logger.info("FTP Server started on port " + config.getPort(), "FtpServer", "-");
        listeners.forEach(ServerListener::onServerStarted);
        new Thread(this::acceptLoop, "FTP-Server-Acceptor").start();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientIp = clientSocket.getInetAddress().getHostAddress();
                if (clientSessions.size() >= config.getMaxConnections()) {
                    clientSocket.close();
                    logger.warn("Connection rejected: max connections reached", "FtpServer", clientIp);
                    continue;
                }
                ClientSession session = new ClientSession(clientSocket);
                clientSessions.add(session);
                logger.info("Client connected: " + session.getClientAddress(), "FtpServer", session.getClientAddress());
                listeners.forEach(l -> l.onClientConnected(session));
                session.handlerThread = new Thread(() -> handleClient(session), "FTP-Client-" + session.getClientPort());
                session.handlerThread.start();
            } catch (IOException e) {
                if (running) {
                    if (!"Accept timed out".equals(e.getMessage())) {
                        logger.error("Accept error: " + e.getMessage(), "FtpServer", "-");
                    }
                }
            }
        }
    }

    private void handleClient(ClientSession session) {
        try {
            CommandHandler handler = new CommandHandler(session.socket, config, userManager);
            handler.handle();
        } catch (IOException e) {
            logger.error("Client handler error: " + e.getMessage(), "FtpServer", session.getClientAddress());
        } finally {
            session.active = false;
            clientSessions.remove(session);
            logger.info("Client disconnected: " + session.getClientAddress(), "FtpServer", session.getClientAddress());
            listeners.forEach(l -> l.onClientDisconnected(session));
        }
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("Error closing server socket: " + e.getMessage(), "FtpServer", "-");
        }
        for (ClientSession session : clientSessions) {
            try {
                if (!session.socket.isClosed()) {
                    session.socket.close();
                }
            } catch (IOException e) {
                logger.error("Error closing client socket: " + e.getMessage(), "FtpServer", session.getClientAddress());
            }
        }
        clientSessions.clear();
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdownNow();
            try {
                if (!threadPool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    logger.warn("Thread pool did not terminate within timeout", "FtpServer", "-");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Thread pool termination interrupted: " + e.getMessage(), "FtpServer", "-");
            }
        }
        logger.info("FTP Server stopped", "FtpServer", "-");
        listeners.forEach(ServerListener::onServerStopped);
    }

    public boolean isRunning() {
        return running;
    }

    public ServerConfig getConfig() {
        return config;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public List<ClientSession> getClientSessions() {
        return new ArrayList<>(clientSessions);
    }

    public int getConnectionCount() {
        return clientSessions.size();
    }
}
