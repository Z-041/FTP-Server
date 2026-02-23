package com.ftpserver.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Logger {
    private static Logger instance;
    private final List<LogEntry> logEntries;
    private final List<LogListener> listeners;
    private String logDirectory;
    private boolean enableFileLogging;

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
        this.listeners = new ArrayList<>();
        this.logDirectory = "logs";
        this.enableFileLogging = true;
    }

    public static synchronized Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
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
            writeToFile(entry);
        }
    }

    private void writeToFile(LogEntry entry) {
        try {
            Path logDir = Paths.get(logDirectory);
            Files.createDirectories(logDir);
            String date = entry.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path logFile = logDir.resolve("ftp-server-" + date + ".log");
            
            // Create log entry with better formatting
            String formattedEntry = String.format("[%s] [%s] [%s] %s%n",
                    entry.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")),
                    entry.level,
                    entry.ip != null ? entry.ip : "-",
                    entry.message);
            
            Files.write(logFile, formattedEntry.getBytes("UTF-8"),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write log to file: " + e.getMessage());
        }
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
}
