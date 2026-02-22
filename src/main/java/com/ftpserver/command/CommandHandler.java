package com.ftpserver.command;

import com.ftpserver.config.ServerConfig;
import com.ftpserver.data.ActiveDataConnection;
import com.ftpserver.data.DataConnection;
import com.ftpserver.data.PassiveDataConnection;
import com.ftpserver.user.User;
import com.ftpserver.user.UserManager;
import com.ftpserver.util.Logger;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class CommandHandler {
    private final Socket controlSocket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final ServerConfig config;
    private final UserManager userManager;
    private final Logger logger;
    private final String clientIp;

    private User currentUser;
    private String currentDirectory;
    private boolean authenticated;
    private DataConnection dataConnection;
    private boolean asciiMode;
    private boolean renameFromPending;
    private String renameFromPath;
    private InetAddress activeModeAddress;
    private int activeModePort;
    private PassiveDataConnection passiveDataConnection;
    private String pendingUsername;
    private String rootDirectory;

    public CommandHandler(Socket controlSocket, ServerConfig config, UserManager userManager) throws IOException {
        this.controlSocket = controlSocket;
        this.config = config;
        this.userManager = userManager;
        this.logger = Logger.getInstance();
        this.clientIp = controlSocket.getInetAddress().getHostAddress();
        this.reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream(), "UTF-8"));
        this.writer = new PrintWriter(new OutputStreamWriter(controlSocket.getOutputStream(), "UTF-8"), true);
        this.currentDirectory = "/";
        this.authenticated = false;
        this.asciiMode = true;
        this.renameFromPending = false;
        this.rootDirectory = config.getRootDirectory();
    }

    public void handle() {
        try {
            sendResponse("220 Welcome to Java FTP Server");
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                logger.debug("Received: " + line, "CommandHandler", clientIp);
                int spaceIndex = line.indexOf(' ');
                String command = (spaceIndex > 0 ? line.substring(0, spaceIndex) : line).toUpperCase();
                String argument = spaceIndex > 0 ? line.substring(spaceIndex + 1).trim() : "";
                executeCommand(command, argument);
            }
        } catch (IOException e) {
            logger.error("Connection error: " + e.getMessage(), "CommandHandler", clientIp);
        } finally {
            close();
        }
    }

    private void executeCommand(String command, String argument) throws IOException {
        switch (command) {
            case "USER" -> handleUser(argument);
            case "PASS" -> handlePass(argument);
            case "QUIT" -> handleQuit();
            case "SYST" -> handleSyst();
            case "FEAT" -> handleFeat();
            case "PWD" -> handlePwd();
            case "XPWD" -> handlePwd();
            case "CWD" -> handleCwd(argument);
            case "XCWD" -> handleCwd(argument);
            case "CDUP" -> handleCdup();
            case "XCUP" -> handleCdup();
            case "MKD" -> handleMkd(argument);
            case "XMKD" -> handleMkd(argument);
            case "RMD" -> handleRmd(argument);
            case "XRMD" -> handleRmd(argument);
            case "DELE" -> handleDele(argument);
            case "RNFR" -> handleRnfr(argument);
            case "RNTO" -> handleRnto(argument);
            case "LIST" -> handleList(argument);
            case "NLST" -> handleNlst(argument);
            case "MLSD" -> handleMlsd(argument);
            case "MLST" -> handleMlst(argument);
            case "RETR" -> handleRetr(argument);
            case "STOR" -> handleStor(argument);
            case "TYPE" -> handleType(argument);
            case "PORT" -> handlePort(argument);
            case "EPRT" -> handleEprt(argument);
            case "PASV" -> handlePasv();
            case "EPSV" -> handleEpsv();
            case "NOOP" -> handleNoop();
            case "OPTS" -> handleOpts(argument);
            case "SITE" -> handleSite(argument);
            case "SIZE" -> handleSize(argument);
            case "MDTM" -> handleMdtm(argument);
            case "REST" -> handleRest(argument);
            case "ABOR" -> handleAbor();
            default -> sendResponse("502 Command not implemented: " + command);
        }
    }

    private void handleUser(String username) {
        this.pendingUsername = username;
        sendResponse("331 Username okay, need password");
    }

    private void handlePass(String password) {
        if (pendingUsername == null) {
            sendResponse("503 Bad sequence of commands");
            return;
        }
        
        Optional<User> userOpt;
        if (pendingUsername.equalsIgnoreCase("anonymous") || pendingUsername.equalsIgnoreCase("ftp")) {
            userOpt = userManager.getUser("anonymous");
        } else {
            userOpt = userManager.authenticate(pendingUsername, password);
        }
        
        if (userOpt.isPresent() && userOpt.get().isEnabled()) {
            this.currentUser = userOpt.get();
            this.authenticated = true;
            String userHome = currentUser.getHomeDirectory();
            if (userHome != null && !userHome.isEmpty()) {
                this.currentDirectory = "/";
                this.rootDirectory = userHome;
            } else {
                this.currentDirectory = "/";
                this.rootDirectory = config.getRootDirectory();
            }
            logger.info("User " + currentUser.getUsername() + " logged in", "CommandHandler", clientIp);
            sendResponse("230 User logged in, proceed");
        } else {
            sendResponse("530 Login incorrect");
        }
        pendingUsername = null;
    }

    private void handleQuit() {
        sendResponse("221 Goodbye");
        close();
    }

    private void handleSyst() {
        sendResponse("215 UNIX Type: L8");
    }

    private void handleFeat() {
        sendResponse("211-Features:");
        sendResponse(" UTF8");
        sendResponse(" MLSD");
        sendResponse(" SIZE");
        sendResponse(" MDTM");
        sendResponse(" REST STREAM");
        sendResponse("211 End");
    }

    private void handlePwd() {
        if (!authenticated) {
            sendResponse("530 Not logged in");
            return;
        }
        sendResponse("257 \"" + currentDirectory + "\" is current directory");
    }

    private void handleCwd(String path) {
        if (!authenticated) {
            sendResponse("530 Not logged in");
            return;
        }
        String newDir = resolvePath(path);
        File dir = new File(getRealPath(newDir));
        if (dir.exists() && dir.isDirectory()) {
            currentDirectory = newDir;
            sendResponse("250 Directory changed to " + currentDirectory);
        } else {
            sendResponse("550 Failed to change directory");
        }
    }

    private void handleCdup() {
        if (!authenticated) {
            sendResponse("530 Not logged in");
            return;
        }
        if (!currentDirectory.equals("/")) {
            Path p = Paths.get(currentDirectory).getParent();
            currentDirectory = p != null ? p.toString().replace("\\", "/") : "/";
        }
        sendResponse("200 Directory changed to parent");
    }

    private void handleMkd(String path) {
        if (!authenticated) {
            sendResponse("530 Not logged in");
            return;
        }
        if (currentUser != null && !currentUser.hasPermission(User.Permission.CREATE_DIR)) {
            sendResponse("550 Permission denied");
            return;
        }
        String fullPath = resolvePath(path);
        File dir = new File(getRealPath(fullPath));
        if (dir.mkdirs()) {
            sendResponse("257 \"" + fullPath + "\" created");
        } else {
            sendResponse("550 Create directory operation failed");
        }
    }

    private void handleRmd(String path) {
        if (!authenticated) {
            sendResponse("530 Not logged in");
            return;
        }
        if (currentUser != null && !currentUser.hasPermission(User.Permission.DELETE_DIR)) {
            sendResponse("550 Permission denied");
            return;
        }
        String fullPath = resolvePath(path);
        File dir = new File(getRealPath(fullPath));
        if (dir.exists() && dir.isDirectory() && dir.delete()) {
            sendResponse("250 Directory deleted");
        } else {
            sendResponse("550 Delete directory operation failed");
        }
    }

    private void handleDele(String path) {
        if (!authenticated) {
            sendResponse("530 Not logged in");
            return;
        }
        if (currentUser != null && !currentUser.hasPermission(User.Permission.DELETE)) {
            sendResponse("550 Permission denied");
            return;
        }
        String fullPath = resolvePath(path);
        File file = new File(getRealPath(fullPath));
        if (file.exists() && file.isFile() && file.delete()) {
            sendResponse("250 File deleted");
        } else {
            sendResponse("550 Delete operation failed");
        }
    }

    private void handleRnfr(String path) {
        if (!authenticated) {
            sendResponse("530 Not logged in");
            return;
        }
        renameFromPath = resolvePath(path);
        renameFromPending = true;
        sendResponse("350 File exists, ready for destination name");
    }

    private void handleRnto(String path) {
        if (!authenticated) {
            sendResponse("530 Not logged in");
            return;
        }
        if (!renameFromPending) {
            sendResponse("503 Bad sequence of commands");
            return;
        }
        if (currentUser != null && !currentUser.hasPermission(User.Permission.RENAME)) {
            sendResponse("550 Permission denied");
            return;
        }
        String destPath = resolvePath(path);
        File from = new File(getRealPath(renameFromPath));
        File to = new File(getRealPath(destPath));
        if (from.renameTo(to)) {
            sendResponse("250 Rename successful");
        } else {
            sendResponse("550 Rename failed");
        }
        renameFromPending = false;
        renameFromPath = null;
    }

    private void handleList(String path) {
        if (!authenticated) {
            sendResponse("530 Not logged in");
            return;
        }
        if (currentUser != null && !currentUser.hasPermission(User.Permission.LIST)) {
            sendResponse("550 Permission denied");
            return;
        }
        String listPath = path;
        if (listPath.startsWith("-")) {
            listPath = "";
        }
        listPath = listPath.isEmpty() ? currentDirectory : resolvePath(listPath);
        File dir = new File(getRealPath(listPath));
        if (!dir.exists()) {
            sendResponse("550 Directory not found");
            return;
        }
        sendResponse("150 Opening ASCII mode data connection for file list");
        try (DataConnection dc = openDataConnection()) {
            dc.setAsciiMode(asciiMode);
            StringBuilder sb = new StringBuilder();
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    sb.append(formatFileInfo(file)).append("\r\n");
                }
            }
            dc.sendListing(sb.toString());
            sendResponse("226 Transfer complete");
        } catch (IOException e) {
            logger.error("LIST error: " + e.getMessage(), "CommandHandler", clientIp);
            sendResponse("425 Can't open data connection");
        }
    }

    private void handleNlst(String path) {
        if (!authenticated) {
            sendResponse("530 Not logged in");
            return;
        }
        String listPath = path;
        if (listPath.startsWith("-")) {
            listPath = "";
        }
        listPath = listPath.isEmpty() ? currentDirectory : resolvePath(listPath);
        File dir = new File(getRealPath(listPath));
        if (!dir.exists()) {
            sendResponse("550 Directory not found");
            return;
        }
        sendResponse("150 Opening ASCII mode data connection for file list");
        try (DataConnection dc = openDataConnection()) {
            dc.setAsciiMode(asciiMode);
            StringBuilder sb = new StringBuilder();
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    sb.append(file.getName()).append("\r\n");
                }
            }
            dc.sendListing(sb.toString());
            sendResponse("226 Transfer complete");
        } catch (IOException e) {
            sendResponse("425 Can't open data connection");
        }
    }

    private String formatFileInfo(File file) {
        String perms = file.isDirectory() ? "drwxr-xr-x" : "-rw-r--r--";
        String links = "1";
        String owner = "ftp";
        String group = "ftp";
        String size = String.format("%12d", file.length());
        
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                          "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(file.lastModified()));
        String month = months[cal.get(Calendar.MONTH)];
        String day = String.format("%2d", cal.get(Calendar.DAY_OF_MONTH));
        String hour = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY));
        String minute = String.format("%02d", cal.get(Calendar.MINUTE));
        String date = month + " " + day + " " + hour + ":" + minute;
        
        String name = file.getName();
        
        return String.format("%s %s %-8s %-8s %s %s %s", 
                            perms, links, owner, group, size, date, name);
    }

    private void handleMlst(String path) {
        if (!authenticated) {
            sendResponse("530 Not logged in");
            return;
        }
        sendResponse("250-Start of list");
        sendResponse("250 End of list");
    }

    private void handleRetr(String path) {
        if (!authenticated) {
            sendResponse("530 Not logged in");
            return;
        }
        if (currentUser != null && !currentUser.hasPermission(User.Permission.READ)) {
            sendResponse("550 Permission denied");
            return;
        }
        String filePath = resolvePath(path);
        File file = new File(getRealPath(filePath));
        if (!file.exists() || !file.isFile()) {
            sendResponse("550 File not found");
            return;
        }
        sendResponse("150 Opening " + (asciiMode ? "ASCII" : "BINARY") + " mode data connection for " + file.getName());
        try (DataConnection dc = openDataConnection()) {
            dc.setAsciiMode(asciiMode);
            dc.sendFile(file);
            sendResponse("226 Transfer complete");
            logger.info("File downloaded: " + filePath, "CommandHandler", clientIp);
        } catch (IOException e) {
            logger.error("RETR error: " + e.getMessage(), "CommandHandler", clientIp);
            sendResponse("425 Can't open data connection");
        }
    }

    private void handleStor(String path) {
        if (!authenticated) {
            sendResponse("530 Not logged in");
            return;
        }
        if (currentUser != null && !currentUser.hasPermission(User.Permission.WRITE)) {
            sendResponse("550 Permission denied");
            return;
        }
        String filePath = resolvePath(path);
        File file = new File(getRealPath(filePath));
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        sendResponse("150 Opening " + (asciiMode ? "ASCII" : "BINARY") + " mode data connection for " + file.getName());
        try (DataConnection dc = openDataConnection()) {
            dc.setAsciiMode(asciiMode);
            dc.receiveFile(file);
            sendResponse("226 Transfer complete");
            logger.info("File uploaded: " + filePath, "CommandHandler", clientIp);
        } catch (IOException e) {
            logger.error("STOR error: " + e.getMessage(), "CommandHandler", clientIp);
            sendResponse("425 Can't open data connection");
        }
    }

    private void handleType(String type) {
        if (type.equalsIgnoreCase("A")) {
            asciiMode = true;
            sendResponse("200 Type set to A");
        } else if (type.equalsIgnoreCase("I")) {
            asciiMode = false;
            sendResponse("200 Type set to I");
        } else {
            sendResponse("504 Command not implemented for that parameter");
        }
    }

    private void handlePort(String argument) {
        if (!config.isEnableActiveMode()) {
            sendResponse("502 Active mode disabled");
            return;
        }
        try {
            String[] parts = argument.split(",");
            byte[] addrBytes = new byte[4];
            for (int i = 0; i < 4; i++) {
                addrBytes[i] = (byte) Integer.parseInt(parts[i]);
            }
            activeModeAddress = InetAddress.getByAddress(addrBytes);
            activeModePort = Integer.parseInt(parts[4]) * 256 + Integer.parseInt(parts[5]);
            sendResponse("200 PORT command successful");
        } catch (Exception e) {
            sendResponse("501 Invalid PORT parameters");
        }
    }

    private void handlePasv() {
        if (!config.isEnablePassiveMode()) {
            sendResponse("502 Passive mode disabled");
            return;
        }
        try {
            InetAddress serverAddr = config.getPassiveAddress() != null 
                    ? InetAddress.getByName(config.getPassiveAddress())
                    : controlSocket.getLocalAddress();
            int port = findAvailablePort();
            passiveDataConnection = new PassiveDataConnection(serverAddr, port);
            byte[] addr = serverAddr.getAddress();
            int p1 = passiveDataConnection.getPort() / 256;
            int p2 = passiveDataConnection.getPort() % 256;
            String response = String.format("227 Entering Passive Mode (%d,%d,%d,%d,%d,%d)",
                    addr[0] & 0xff, addr[1] & 0xff, addr[2] & 0xff, addr[3] & 0xff, p1, p2);
            sendResponse(response);
        } catch (IOException e) {
            sendResponse("425 Can't open passive connection");
        }
    }

    private void handleEpsv() {
        if (!config.isEnablePassiveMode()) {
            sendResponse("502 Passive mode disabled");
            return;
        }
        try {
            InetAddress serverAddr = config.getPassiveAddress() != null 
                    ? InetAddress.getByName(config.getPassiveAddress())
                    : controlSocket.getLocalAddress();
            int port = findAvailablePort();
            passiveDataConnection = new PassiveDataConnection(serverAddr, port);
            sendResponse("229 Entering Extended Passive Mode (|||" + passiveDataConnection.getPort() + "|)");
        } catch (IOException e) {
            sendResponse("425 Can't open passive connection");
        }
    }

    private void handleNoop() {
        sendResponse("200 OK");
    }

    private void handleOpts(String argument) {
        String[] parts = argument.split(" ");
        if (parts.length >= 1) {
            String option = parts[0].toUpperCase();
            if ("UTF8".equals(option)) {
                sendResponse("200 OK");
                return;
            }
        }
        sendResponse("501 Invalid option");
    }

    private void handleSite(String argument) {
        String[] parts = argument.split(" ");
        if (parts.length >= 1) {
            String command = parts[0].toUpperCase();
            if ("HELP".equals(command)) {
                sendResponse("214-Site commands:");
                sendResponse("214 HELP");
                sendResponse("214 End");
                return;
            }
        }
        sendResponse("501 Invalid SITE command");
    }

    private int findAvailablePort() throws IOException {
        for (int port = config.getDataPortRangeStart(); port <= config.getDataPortRangeEnd(); port++) {
            try (ServerSocket ss = new ServerSocket(port, 1, controlSocket.getLocalAddress())) {
                return port;
            } catch (IOException ignored) {
            }
        }
        throw new IOException("No available port in range");
    }

    private DataConnection openDataConnection() throws IOException {
        if (passiveDataConnection != null) {
            passiveDataConnection.connect();
            DataConnection dc = passiveDataConnection;
            passiveDataConnection = null;
            return dc;
        } else if (activeModeAddress != null) {
            ActiveDataConnection dc = new ActiveDataConnection(activeModeAddress, activeModePort);
            dc.connect();
            activeModeAddress = null;
            return dc;
        }
        throw new IOException("No data connection established");
    }

    private String resolvePath(String path) {
        if (path == null || path.isEmpty()) {
            return currentDirectory;
        }
        try {
            String result;
            if (path.startsWith("/")) {
                result = path;
            } else {
                if (currentDirectory.equals("/")) {
                    result = "/" + path;
                } else {
                    result = currentDirectory + "/" + path;
                }
            }
            while (result.contains("//")) {
                result = result.replace("//", "/");
            }
            return result;
        } catch (Exception e) {
            logger.error("Invalid path: " + path, "CommandHandler", clientIp);
            return currentDirectory;
        }
    }

    private String getRealPath(String ftpPath) {
        try {
            String normalized = ftpPath;
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            while (normalized.contains("//")) {
                normalized = normalized.replace("//", "/");
            }
            if (normalized.startsWith("..")) {
                normalized = "";
            }
            return new File(rootDirectory, normalized).getAbsolutePath();
        } catch (Exception e) {
            logger.error("Invalid FTP path: " + ftpPath, "CommandHandler", clientIp);
            return rootDirectory;
        }
    }

    private void handleMlsd(String path) {
        if (!authenticated) {
            sendResponse("530 Not logged in");
            return;
        }
        if (currentUser != null && !currentUser.hasPermission(User.Permission.LIST)) {
            sendResponse("550 Permission denied");
            return;
        }
        String listPath = path;
        if (listPath.startsWith("-")) {
            listPath = "";
        }
        listPath = listPath.isEmpty() ? currentDirectory : resolvePath(listPath);
        File dir = new File(getRealPath(listPath));
        if (!dir.exists()) {
            sendResponse("550 Directory not found");
            return;
        }
        sendResponse("150 Opening data connection for MLSD");
        try (DataConnection dc = openDataConnection()) {
            dc.setAsciiMode(false);
            StringBuilder sb = new StringBuilder();
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    sb.append(formatMlsdEntry(file)).append("\r\n");
                }
            }
            dc.sendListing(sb.toString());
            sendResponse("226 Transfer complete");
        } catch (IOException e) {
            logger.error("MLSD error: " + e.getMessage(), "CommandHandler", clientIp);
            sendResponse("425 Can't open data connection");
        }
    }

    private String formatMlsdEntry(File file) {
        StringBuilder sb = new StringBuilder();
        sb.append("type=").append(file.isDirectory() ? "dir" : "file").append(";");
        sb.append("size=").append(file.length()).append(";");
        sb.append("modify=").append(formatMlsdTime(file.lastModified())).append(";");
        sb.append("perm=").append(file.isDirectory() ? "el" : "r").append(";");
        sb.append(" ").append(file.getName());
        return sb.toString();
    }

    private String formatMlsdTime(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(timestamp));
    }

    private void handleSize(String path) {
        if (!authenticated) {
            sendResponse("530 Not logged in");
            return;
        }
        String filePath = resolvePath(path);
        File file = new File(getRealPath(filePath));
        if (!file.exists() || !file.isFile()) {
            sendResponse("550 File not found");
            return;
        }
        sendResponse("213 " + file.length());
    }

    private void handleMdtm(String path) {
        if (!authenticated) {
            sendResponse("530 Not logged in");
            return;
        }
        String filePath = resolvePath(path);
        File file = new File(getRealPath(filePath));
        if (!file.exists()) {
            sendResponse("550 File not found");
            return;
        }
        sendResponse("213 " + formatMlsdTime(file.lastModified()));
    }

    private void handleRest(String argument) {
        if (!authenticated) {
            sendResponse("530 Not logged in");
            return;
        }
        sendResponse("350 Restart position accepted");
    }

    private void handleAbor() {
        sendResponse("226 Abort successful");
    }

    private void handleEprt(String argument) {
        sendResponse("522 Extended PORT not implemented, use EPSV instead");
    }

    private void sendResponse(String response) {
        writer.println(response);
        logger.debug("Sent: " + response, "CommandHandler", clientIp);
    }

    private void close() {
        try {
            if (dataConnection != null) {
                dataConnection.close();
            }
            if (passiveDataConnection != null) {
                passiveDataConnection.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (!controlSocket.isClosed()) {
                controlSocket.close();
            }
        } catch (IOException e) {
            logger.error("Close error: " + e.getMessage(), "CommandHandler", clientIp);
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
