package com.ftpserver.session;

import com.ftpserver.config.ServerConfig;
import com.ftpserver.data.DataConnection;
import com.ftpserver.data.PassiveDataConnection;
import com.ftpserver.user.User;
import com.ftpserver.user.UserManager;
import com.ftpserver.util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FtpSession implements AutoCloseable {
    private final Socket controlSocket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final ServerConfig config;
    private final UserManager userManager;
    private final Logger logger;
    private final String clientIp;
    private final PathResolver pathResolver;
    private long lastActivityTime;

    private User currentUser;
    private String currentDirectory;
    private boolean authenticated;
    private boolean asciiMode;
    private boolean renameFromPending;
    private String renameFromPath;
    private InetAddress activeModeAddress;
    private int activeModePort;
    private PassiveDataConnection passiveDataConnection;
    private String pendingUsername;
    private long restartOffset = 0;

    public FtpSession(Socket controlSocket, ServerConfig config, UserManager userManager) throws IOException {
        this.controlSocket = controlSocket;
        this.config = config;
        this.userManager = userManager;
        this.logger = Logger.getInstance();
        String rawIp = controlSocket.getInetAddress().getHostAddress();
        this.clientIp = rawIp.equals("0:0:0:0:0:0:0:1") ? "127.0.0.1" : rawIp;
        this.reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream(), "UTF-8"));
        this.writer = new PrintWriter(new OutputStreamWriter(controlSocket.getOutputStream(), "UTF-8"), true);
        this.pathResolver = new PathResolver(config.getRootDirectory());
        this.currentDirectory = "/";
        this.authenticated = false;
        this.asciiMode = true;
        this.renameFromPending = false;
        this.lastActivityTime = System.currentTimeMillis();
    }

    public void sendResponse(String response) {
        try {
            writer.println(response);
            logger.debug("Sent: " + response, "FtpSession", clientIp);
        } catch (Exception e) {
            logger.error("Failed to send response: " + e.getMessage(), "FtpSession", clientIp);
        }
    }

    public String readLine() throws IOException {
        try {
            String line = reader.readLine();
            updateLastActivityTime();
            return line;
        } catch (IOException e) {
            logger.error("Failed to read line: " + e.getMessage(), "FtpSession", clientIp);
            throw e;
        }
    }

    public void logDebug(String message) {
        logger.debug(message, "FtpSession", clientIp);
    }

    public void logInfo(String message) {
        logger.info(message, "FtpSession", clientIp);
    }

    public void logError(String message, Exception e) {
        logger.error(message + ": " + e.getMessage(), "FtpSession", clientIp);
    }

    public void logWarn(String message) {
        logger.warn(message, "FtpSession", clientIp);
    }

    public String resolvePath(String path) {
        return pathResolver.resolvePath(currentDirectory, path);
    }

    public String getRealPath(String ftpPath) {
        return pathResolver.getRealPath(ftpPath);
    }

    public String formatFileInfo(java.io.File file) {
        String perms = com.ftpserver.util.CrossPlatformUtil.getUnixPermissions(file);
        String links = String.valueOf(com.ftpserver.util.CrossPlatformUtil.getLinkCount(file));
        String owner = com.ftpserver.util.CrossPlatformUtil.getFileOwner(file);
        String group = com.ftpserver.util.CrossPlatformUtil.getFileGroup(file);
        String size = String.format("%12d", file.length());
        
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                          "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(new Date(file.lastModified()));
        String month = months[cal.get(java.util.Calendar.MONTH)];
        String day = String.format("%2d", cal.get(java.util.Calendar.DAY_OF_MONTH));
        String hour = String.format("%02d", cal.get(java.util.Calendar.HOUR_OF_DAY));
        String minute = String.format("%02d", cal.get(java.util.Calendar.MINUTE));
        String date = month + " " + day + " " + hour + ":" + minute;
        
        String name = file.getName();
        
        return String.format("%s %s %-8s %-8s %12s %s %s", 
                            perms, links, owner, group, size.trim(), date, name);
    }

    public String formatMlsdEntry(java.io.File file) {
        StringBuilder sb = new StringBuilder();
        sb.append("type=").append(file.isDirectory() ? "dir" : "file").append(";");
        sb.append("size=").append(file.length()).append(";");
        sb.append("modify=").append(formatMlsdTime(file.lastModified())).append(";");
        sb.append("perm=").append(file.isDirectory() ? "el" : "r").append(";");
        sb.append(" ").append(file.getName());
        return sb.toString();
    }

    public String formatMlsdTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(timestamp));
    }

    public PassiveDataConnection createPassiveDataConnection() throws IOException, com.ftpserver.data.DataConnectionException {
        Exception lastException = null;
        for (int port = config.getDataPortRangeStart(); port <= config.getDataPortRangeEnd(); port++) {
            try {
                return new PassiveDataConnection(InetAddress.getByName("0.0.0.0"), port);
            } catch (IOException e) {
                lastException = e;
            } catch (com.ftpserver.data.DataConnectionException e) {
                lastException = e;
            }
        }
        if (lastException instanceof IOException) {
            throw (IOException) lastException;
        } else if (lastException instanceof com.ftpserver.data.DataConnectionException) {
            throw (com.ftpserver.data.DataConnectionException) lastException;
        } else {
            throw new IOException("No available port in range");
        }
    }

    public DataConnection openDataConnection() throws IOException, com.ftpserver.data.DataConnectionException {
        if (passiveDataConnection != null) {
            passiveDataConnection.connect();
            DataConnection dc = passiveDataConnection;
            passiveDataConnection = null;
            return dc;
        } else if (activeModeAddress != null) {
            com.ftpserver.data.ActiveDataConnection dc = 
                new com.ftpserver.data.ActiveDataConnection(activeModeAddress, activeModePort);
            dc.connect();
            activeModeAddress = null;
            return dc;
        }
        throw new IOException("No data connection established");
    }

    public Socket getControlSocket() {
        return controlSocket;
    }

    public ServerConfig getConfig() {
        return config;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public String getClientIp() {
        return clientIp;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public boolean isAsciiMode() {
        return asciiMode;
    }

    public void setAsciiMode(boolean asciiMode) {
        this.asciiMode = asciiMode;
    }

    public boolean isRenameFromPending() {
        return renameFromPending;
    }

    public void setRenameFromPending(boolean renameFromPending) {
        this.renameFromPending = renameFromPending;
    }

    public String getRenameFromPath() {
        return renameFromPath;
    }

    public void setRenameFromPath(String renameFromPath) {
        this.renameFromPath = renameFromPath;
    }

    public InetAddress getActiveModeAddress() {
        return activeModeAddress;
    }

    public void setActiveModeAddress(InetAddress activeModeAddress) {
        this.activeModeAddress = activeModeAddress;
    }

    public int getActiveModePort() {
        return activeModePort;
    }

    public void setActiveModePort(int activeModePort) {
        this.activeModePort = activeModePort;
    }

    public PassiveDataConnection getPassiveDataConnection() {
        return passiveDataConnection;
    }

    public void setPassiveDataConnection(PassiveDataConnection passiveDataConnection) {
        this.passiveDataConnection = passiveDataConnection;
    }

    public String getPendingUsername() {
        return pendingUsername;
    }

    public void setPendingUsername(String pendingUsername) {
        this.pendingUsername = pendingUsername;
    }

    public PathResolver getPathResolver() {
        return pathResolver;
    }

    public long getRestartOffset() {
        return restartOffset;
    }

    public void setRestartOffset(long restartOffset) {
        this.restartOffset = restartOffset;
    }

    public void clearRestartOffset() {
        this.restartOffset = 0;
    }

    @Override
    public void close() {
        try {
            if (passiveDataConnection != null) {
                try {
                    passiveDataConnection.close();
                } catch (com.ftpserver.data.DataConnectionException e) {
                    logger.error("Data connection close error: " + e.getMessage(), "FtpSession", clientIp);
                }
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
            logger.error("Close error: " + e.getMessage(), "FtpSession", clientIp);
        }
    }

    /**
     * 检查会话是否连接
     * @return 是否已连接
     */
    public boolean isConnected() {
        return controlSocket != null && !controlSocket.isClosed();
    }

    /**
     * 更新最后活动时间
     */
    public void updateLastActivityTime() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    /**
     * 获取最后活动时间
     * @return 最后活动时间戳
     */
    public long getLastActivityTime() {
        return lastActivityTime;
    }

    /**
     * 获取远程地址
     * @return 远程地址
     */
    public InetAddress getRemoteAddress() {
        return controlSocket.getInetAddress();
    }
}
