package com.ftpserver.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerConfig {
    private int port = 2121;
    private String rootDirectory = System.getProperty("user.home") + File.separator + "ftp_root";
    private int maxConnections = 50;
    private int connectionTimeout = 300;
    private int dataPortRangeStart = 50000;
    private int dataPortRangeEnd = 60000;
    private boolean enablePassiveMode = true;
    private boolean enableActiveMode = true;
    private String logDirectory = "logs";
    private String passiveAddress = null;

    public ServerConfig() {
        ensureRootDirectory();
    }

    public void load(String configPath) throws IOException {
        Path path = Paths.get(configPath);
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = replaceSystemProperties(parts[1].trim());
                        try {
                            setProperty(key, value);
                        } catch (Exception e) {
                            System.err.println("Warning: Invalid config at line " + lineNumber + ": " + e.getMessage());
                        }
                    }
                }
            }
        }
        ensureRootDirectory();
    }

    private void ensureRootDirectory() {
        if (rootDirectory != null && !rootDirectory.isEmpty()) {
            File rootDir = new File(rootDirectory);
            if (!rootDir.exists()) {
                rootDir.mkdirs();
            }
        }
    }

    private String replaceSystemProperties(String value) {
        String result = value;
        int startIndex;
        while ((startIndex = result.indexOf("${")) != -1) {
            int endIndex = result.indexOf("}", startIndex);
            if (endIndex == -1) break;
            String propName = result.substring(startIndex + 2, endIndex);
            String propValue = System.getProperty(propName, "");
            result = result.substring(0, startIndex) + propValue + result.substring(endIndex + 1);
        }
        return result;
    }

    private void setProperty(String key, String value) {
        try {
            switch (key) {
                case "port" -> {
                    int p = Integer.parseInt(value);
                    if (p > 0 && p <= 65535) {
                        port = p;
                    }
                }
                case "rootDirectory" -> rootDirectory = value;
                case "maxConnections" -> {
                    int m = Integer.parseInt(value);
                    if (m > 0) {
                        maxConnections = m;
                    }
                }
                case "connectionTimeout" -> {
                    int t = Integer.parseInt(value);
                    if (t >= 0) {
                        connectionTimeout = t;
                    }
                }
                case "dataPortRangeStart" -> {
                    int s = Integer.parseInt(value);
                    if (s > 0 && s <= 65535) {
                        if (s <= dataPortRangeEnd) {
                            dataPortRangeStart = s;
                        }
                    }
                }
                case "dataPortRangeEnd" -> {
                    int e = Integer.parseInt(value);
                    if (e > 0 && e <= 65535) {
                        if (e >= dataPortRangeStart) {
                            dataPortRangeEnd = e;
                        }
                    }
                }
                case "enablePassiveMode" -> enablePassiveMode = Boolean.parseBoolean(value);
                case "enableActiveMode" -> enableActiveMode = Boolean.parseBoolean(value);
                case "logDirectory" -> logDirectory = value;
                case "passiveAddress" -> passiveAddress = value.isEmpty() ? null : value;
            }
        } catch (NumberFormatException e) {
        }
    }

    public void save(String configPath) throws IOException {
        Path path = Paths.get(configPath);
        Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write("# FTP Server Configuration\n");
            writer.write("port=" + port + "\n");
            writer.write("rootDirectory=" + rootDirectory + "\n");
            writer.write("maxConnections=" + maxConnections + "\n");
            writer.write("connectionTimeout=" + connectionTimeout + "\n");
            writer.write("dataPortRangeStart=" + dataPortRangeStart + "\n");
            writer.write("dataPortRangeEnd=" + dataPortRangeEnd + "\n");
            writer.write("enablePassiveMode=" + enablePassiveMode + "\n");
            writer.write("enableActiveMode=" + enableActiveMode + "\n");
            writer.write("logDirectory=" + logDirectory + "\n");
            writer.write("passiveAddress=" + (passiveAddress != null ? passiveAddress : "") + "\n");
        }
    }

    public int getPort() { return port; }
    public void setPort(int port) { 
        if (port > 0 && port <= 65535) {
            this.port = port; 
        }
    }

    public String getRootDirectory() { return rootDirectory; }
    public void setRootDirectory(String rootDirectory) { this.rootDirectory = rootDirectory; }

    public int getMaxConnections() { return maxConnections; }
    public void setMaxConnections(int maxConnections) { 
        if (maxConnections > 0) {
            this.maxConnections = maxConnections; 
        }
    }

    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { 
        if (connectionTimeout >= 0) {
            this.connectionTimeout = connectionTimeout; 
        }
    }

    public int getDataPortRangeStart() { return dataPortRangeStart; }
    public void setDataPortRangeStart(int dataPortRangeStart) { 
        if (dataPortRangeStart > 0 && dataPortRangeStart <= 65535 && 
            dataPortRangeStart <= dataPortRangeEnd) {
            this.dataPortRangeStart = dataPortRangeStart; 
        }
    }

    public int getDataPortRangeEnd() { return dataPortRangeEnd; }
    public void setDataPortRangeEnd(int dataPortRangeEnd) { 
        if (dataPortRangeEnd > 0 && dataPortRangeEnd <= 65535 && 
            dataPortRangeEnd >= dataPortRangeStart) {
            this.dataPortRangeEnd = dataPortRangeEnd; 
        }
    }

    public boolean isEnablePassiveMode() { return enablePassiveMode; }
    public void setEnablePassiveMode(boolean enablePassiveMode) { this.enablePassiveMode = enablePassiveMode; }

    public boolean isEnableActiveMode() { return enableActiveMode; }
    public void setEnableActiveMode(boolean enableActiveMode) { this.enableActiveMode = enableActiveMode; }

    public String getLogDirectory() { return logDirectory; }
    public void setLogDirectory(String logDirectory) { this.logDirectory = logDirectory; }

    public String getPassiveAddress() { return passiveAddress; }
    public void setPassiveAddress(String passiveAddress) { this.passiveAddress = passiveAddress; }
}
