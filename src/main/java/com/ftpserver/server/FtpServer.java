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
import java.util.concurrent.TimeUnit;

/**
 * FTP服务器类，负责处理客户端连接和管理FTP会话
 */
public class FtpServer {
    // 使用配置中的连接超时设置（秒）
    private ServerConfig config; // 服务器配置
    private UserManager userManager; // 用户管理器
    private ServerSocket serverSocket; // 服务器套接字
    private ExecutorService threadPool; // 线程池，用于处理客户端连接
    private volatile boolean running; // 服务器运行状态
    private final List<ClientSession> clientSessions; // 客户端会话列表
    private final List<ServerListener> listeners; // 服务器事件监听器列表
    private final Logger logger; // 日志记录器

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

/**
 * 启动FTP服务器
 * @throws IOException 如果启动过程中出现IO错误
 */
    public synchronized void start() throws IOException {
        if (running) {
            throw new IllegalStateException("Server is already running");
        }
        // 确保根目录存在
        File rootDir = new File(config.getRootDirectory());
        if (!rootDir.exists()) {
            boolean created = rootDir.mkdirs();
            if (created) {
                logger.info("Root directory created: " + rootDir.getAbsolutePath(), "FtpServer", "-");
            }
        }
        int maxThreads = Math.max(config.getMaxConnections() * 2, 20);
        int coreThreads = Math.max(config.getMaxConnections(), 10);
        int queueCapacity = Math.max(config.getMaxConnections() * 2, 100);
        
        threadPool = new java.util.concurrent.ThreadPoolExecutor(
            coreThreads,
            maxThreads,
            30L, TimeUnit.SECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>(queueCapacity),
            new java.util.concurrent.ThreadFactory() {
                private int count = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("FTP-Worker-" + (++count));
                    t.setDaemon(true);
                    return t;
                }
            },
            new java.util.concurrent.ThreadPoolExecutor.DiscardPolicy()
        );
        
        // 预启动核心线程，提高响应速度
        ((java.util.concurrent.ThreadPoolExecutor) threadPool).prestartAllCoreThreads();
        // 创建服务器套接字并开始监听
        serverSocket = new ServerSocket(config.getPort());
        serverSocket.setSoTimeout(5000);
        running = true;
        logger.info("FTP Server started on port " + config.getPort(), "FtpServer", "-");
        // 通知监听器服务器已启动
        listeners.forEach(ServerListener::onServerStarted);
        // 启动接受连接的线程
        new Thread(this::acceptLoop, "FTP-Server-Acceptor").start();
    }

/**
 * 接受客户端连接的循环
 */
    private void acceptLoop() {
        while (running) {
            try {
                // 接受客户端连接
                Socket clientSocket = serverSocket.accept();
                // 设置连接超时
                clientSocket.setSoTimeout(config.getConnectionTimeout() * 1000);
                String clientIp = clientSocket.getInetAddress().getHostAddress();
                
                // 检查是否达到最大连接数
                if (clientSessions.size() >= config.getMaxConnections()) {
                    logger.warn("Connection rejected: max connections reached", "FtpServer", clientIp);
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        logger.error("Error closing rejected client socket: " + e.getMessage(), "FtpServer", clientIp);
                    }
                    continue;
                }
                
                // 创建客户端会话
                ClientSession session = new ClientSession(clientSocket);
                clientSessions.add(session);
                logger.info("Client connected: " + session.getClientAddress(), "FtpServer", session.getClientAddress());
                
                // 通知监听器客户端已连接
                listeners.forEach(l -> l.onClientConnected(session));
                
                // 启动客户端处理线程
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

    /**
     * 检查并清理超时的客户端连接
     */
    public void checkIdleClients() {
        long currentTime = System.currentTimeMillis();
        long timeoutMillis = config.getConnectionTimeout() * 1000L;
        
        for (ClientSession session : clientSessions) {
            if (!session.active) {
                continue;
            }
            
            long idleTime = currentTime - session.connectTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000;
            if (idleTime > timeoutMillis) {
                logger.warn("Client timeout: " + session.getClientAddress(), "FtpServer", session.getClientAddress());
                try {
                    if (!session.socket.isClosed()) {
                        session.socket.close();
                    }
                } catch (IOException e) {
                    logger.error("Error closing idle client socket: " + e.getMessage(), "FtpServer", session.getClientAddress());
                }
            }
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
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
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
