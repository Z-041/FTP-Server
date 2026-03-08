package com.ftpserver.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Logger {
    private static Logger instance;
    private final List<LogEntry> logEntries;
    private final List<LogListener> listeners;
    private String logDirectory;
    private boolean enableFileLogging;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final long MAX_LOG_FILE_SIZE = 10 * 1024 * 1024;
    private static final int MAX_LOG_FILES = 5;
    private static final int LOG_QUEUE_CAPACITY = 10000;
    private static final int BATCH_SIZE = 100;
    private static final long FLUSH_INTERVAL_MS = 1000;

    private final BlockingQueue<LogEntry> logQueue;
    private final AtomicBoolean running;
    private final AtomicBoolean shutdownRequested;
    private Thread logWriterThread;
    private volatile boolean initialized = false;

    public interface LogListener {
        void onLogEntry(LogEntry entry);
    }

    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    public static class LogEntry {
        public final LocalDateTime timestamp;
        public final LogLevel level;
        public final String message;
        public final String source;
        public final String ip;

        public LogEntry(LogLevel level, String message, String source) {
            this(level, message, source, null);
        }

        public LogEntry(LogLevel level, String message, String source, String ip) {
            this.timestamp = LocalDateTime.now();
            this.level = level;
            this.message = message;
            this.source = source;
            this.ip = ip;
        }

        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String ipPart = (ip != null && !ip.isEmpty()) ? ip : "-";
            return String.format("%s-%s-%s-%s",
                    timestamp.format(formatter), level, ipPart, message);
        }
    }

    private Logger() {
        this.logEntries = new CopyOnWriteArrayList<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.logDirectory = "logs";
        this.enableFileLogging = true;
        this.logQueue = new LinkedBlockingQueue<>(LOG_QUEUE_CAPACITY);
        this.running = new AtomicBoolean(true);
        this.shutdownRequested = new AtomicBoolean(false);
        initializeAsyncWriter();
    }

    private static class LoggerHolder {
        private static final Logger INSTANCE = new Logger();
    }

    public static Logger getInstance() {
        return LoggerHolder.INSTANCE;
    }

    /**
     * 初始化异步日志写入器
     */
    private void initializeAsyncWriter() {
        logWriterThread = new Thread(() -> {
            List<LogEntry> batch = new ArrayList<>(BATCH_SIZE);
            long lastFlushTime = System.currentTimeMillis();
            
            while (running.get() || !logQueue.isEmpty()) {
                try {
                    LogEntry entry = logQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (entry != null) {
                        batch.add(entry);
                    }
                    
                    long currentTime = System.currentTimeMillis();
                    boolean shouldFlush = batch.size() >= BATCH_SIZE || 
                                       (currentTime - lastFlushTime >= FLUSH_INTERVAL_MS) ||
                                       (shutdownRequested.get() && !batch.isEmpty());
                    
                    if (shouldFlush && !batch.isEmpty()) {
                        writeBatchToFile(batch);
                        batch.clear();
                        lastFlushTime = currentTime;
                    }
                } catch (InterruptedException e) {
                    if (running.get()) {
                        System.err.println("Log writer thread interrupted: " + e.getMessage());
                    }
                    break;
                }
            }
            
            if (!batch.isEmpty()) {
                writeBatchToFile(batch);
            }
            
            initialized = true;
        }, "AsyncLogWriter");
        
        logWriterThread.setDaemon(true);
        logWriterThread.start();
    }

    public void setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
    }

    public void setEnableFileLogging(boolean enable) {
        this.enableFileLogging = enable;
    }

    public void addListener(LogListener listener) {
        listeners.add(listener);
    }

    public void removeListener(LogListener listener) {
        listeners.remove(listener);
    }

    public void log(LogLevel level, String message, String source) {
        log(level, message, source, null);
    }

    public void log(LogLevel level, String message, String source, String ip) {
        LogEntry entry = new LogEntry(level, message, source, ip);
        logEntries.add(entry);
        if (logEntries.size() > 10000) {
            logEntries.remove(0);
        }
        listeners.forEach(listener -> listener.onLogEntry(entry));
        
        if (enableFileLogging) {
            if (!logQueue.offer(entry)) {
                System.err.println("Log queue full, dropping log entry: " + message);
            }
        }
    }
    
    /**
     * 批量写入日志到文件
     * @param entries 日志条目列表
     */
    private void writeBatchToFile(List<LogEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        
        try {
            Path logDir = Paths.get(logDirectory);
            Files.createDirectories(logDir);
            
            String date = entries.get(0).timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path logFile = logDir.resolve("ftp-server-" + date + ".log");
            
            if (Files.exists(logFile) && Files.size(logFile) > MAX_LOG_FILE_SIZE) {
                rotateLogFile(logFile);
            }
            
            StringBuilder sb = new StringBuilder(entries.size() * 100);
            for (LogEntry entry : entries) {
                String maskedIp = maskIpAddress(entry.ip);
                sb.append(String.format("[%s] [%s] [%s] %s%n",
                        entry.timestamp.format(TIMESTAMP_FORMATTER),
                        entry.level,
                        maskedIp,
                        entry.message));
            }
            
            Files.write(logFile, sb.toString().getBytes("UTF-8"),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write log batch to file: " + e.getMessage());
        }
    }
    
    /**
     * 对 IP 地址进行脱敏处理
     * @param ip 原始 IP 地址
     * @return 脱敏后的 IP 地址
     */
    private String maskIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "-";
        }
        
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return "localhost";
        }
        
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + "." + parts[2] + ".*";
        }
        
        return ip.substring(0, Math.min(ip.length(), 8)) + "***";
    }

    private void rotateLogFile(Path logFile) throws IOException {
        if (!Files.exists(logFile)) {
            return;
        }
        
        for (int i = MAX_LOG_FILES - 1; i >= 1; i--) {
            Path currentFile = logFile.resolveSibling(logFile.getFileName() + "." + i);
            Path nextFile = logFile.resolveSibling(logFile.getFileName() + "." + (i + 1));
            
            if (Files.exists(currentFile)) {
                Files.move(currentFile, nextFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        
        Path rotatedFile = logFile.resolveSibling(logFile.getFileName() + ".1");
        Files.move(logFile, rotatedFile, StandardCopyOption.REPLACE_EXISTING);
    }

    public void debug(String message, String source) {
        log(LogLevel.DEBUG, message, source);
    }

    public void debug(String message, String source, String ip) {
        log(LogLevel.DEBUG, message, source, ip);
    }

    public void info(String message, String source) {
        log(LogLevel.INFO, message, source);
    }

    public void info(String message, String source, String ip) {
        log(LogLevel.INFO, message, source, ip);
    }

    public void warn(String message, String source) {
        log(LogLevel.WARN, message, source);
    }

    public void warn(String message, String source, String ip) {
        log(LogLevel.WARN, message, source, ip);
    }

    public void error(String message, String source) {
        log(LogLevel.ERROR, message, source);
    }

    public void error(String message, String source, String ip) {
        log(LogLevel.ERROR, message, source, ip);
    }

    public List<LogEntry> getLogEntries() {
        return new ArrayList<>(logEntries);
    }

    public List<LogEntry> getLogEntries(LogLevel level) {
        return logEntries.stream().filter(e -> e.level == level).toList();
    }

    public List<LogEntry> searchLogEntries(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return logEntries.stream()
                .filter(e -> e.message.toLowerCase().contains(lowerKeyword)
                        || e.source.toLowerCase().contains(lowerKeyword))
                .toList();
    }

    public void clearLogs() {
        logEntries.clear();
    }
    
    /**
     * 优雅关闭日志系统
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }
        
        shutdownRequested.set(true);
        running.set(false);
        
        if (logWriterThread != null && logWriterThread.isAlive()) {
            try {
                logWriterThread.interrupt();
                logWriterThread.join(5000);
            } catch (InterruptedException e) {
                System.err.println("Interrupted while waiting for log writer thread to shutdown: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 获取日志队列大小
     * @return 队列中待处理的日志条目数
     */
    public int getQueueSize() {
        return logQueue.size();
    }
    
    /**
     * 检查日志系统是否已初始化
     * @return 是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
}
