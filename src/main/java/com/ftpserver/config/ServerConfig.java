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

    public ServerConfig() {}

    public void load(String configPath) throws IOException {
        Path path = Paths.get(configPath);
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = replaceSystemProperties(parts[1].trim());
                        setProperty(key, value);
                    }
                }
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
        switch (key) {
            case "port" -> port = Integer.parseInt(value);
            case "rootDirectory" -> rootDirectory = value;
            case "maxConnections" -> maxConnections = Integer.parseInt(value);
            case "connectionTimeout" -> connectionTimeout = Integer.parseInt(value);
            case "dataPortRangeStart" -> dataPortRangeStart = Integer.parseInt(value);
            case "dataPortRangeEnd" -> dataPortRangeEnd = Integer.parseInt(value);
            case "enablePassiveMode" -> enablePassiveMode = Boolean.parseBoolean(value);
            case "enableActiveMode" -> enableActiveMode = Boolean.parseBoolean(value);
            case "logDirectory" -> logDirectory = value;
            case "passiveAddress" -> passiveAddress = value.isEmpty() ? null : value;
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
    public void setPort(int port) { this.port = port; }

    public String getRootDirectory() { return rootDirectory; }
    public void setRootDirectory(String rootDirectory) { this.rootDirectory = rootDirectory; }

    public int getMaxConnections() { return maxConnections; }
    public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }

    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

    public int getDataPortRangeStart() { return dataPortRangeStart; }
    public void setDataPortRangeStart(int dataPortRangeStart) { this.dataPortRangeStart = dataPortRangeStart; }

    public int getDataPortRangeEnd() { return dataPortRangeEnd; }
    public void setDataPortRangeEnd(int dataPortRangeEnd) { this.dataPortRangeEnd = dataPortRangeEnd; }

    public boolean isEnablePassiveMode() { return enablePassiveMode; }
    public void setEnablePassiveMode(boolean enablePassiveMode) { this.enablePassiveMode = enablePassiveMode; }

    public boolean isEnableActiveMode() { return enableActiveMode; }
    public void setEnableActiveMode(boolean enableActiveMode) { this.enableActiveMode = enableActiveMode; }

    public String getLogDirectory() { return logDirectory; }
    public void setLogDirectory(String logDirectory) { this.logDirectory = logDirectory; }

    public String getPassiveAddress() { return passiveAddress; }
    public void setPassiveAddress(String passiveAddress) { this.passiveAddress = passiveAddress; }
}
