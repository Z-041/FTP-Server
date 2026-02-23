package com.ftpserver.command;

import com.ftpserver.config.ServerConfig;
import com.ftpserver.session.FtpSession;
import com.ftpserver.user.UserManager;

import java.io.IOException;
import java.net.Socket;

public class CommandHandler {
    private final FtpSession session;

    public CommandHandler(Socket controlSocket, ServerConfig config, UserManager userManager) throws IOException {
        this.session = new FtpSession(controlSocket, config, userManager);
    }

    public void handle() {
        try {
            session.sendResponse("220 Welcome to Java FTP Server");
            String line;
            while ((line = session.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                session.logDebug("Received: " + line);
                int spaceIndex = line.indexOf(' ');
                String command = (spaceIndex > 0 ? line.substring(0, spaceIndex) : line).toUpperCase();
                String argument = spaceIndex > 0 ? line.substring(spaceIndex + 1).trim() : "";
                executeCommand(command, argument);
            }
        } catch (IOException e) {
            session.logError("Connection error", e);
        } finally {
            session.close();
        }
    }

    private void executeCommand(String command, String argument) {
        try {
            FtpCommand ftpCommand = CommandFactory.getCommand(command);
            if (ftpCommand == null) {
                session.sendResponse("502 Command not implemented: " + command);
                return;
            }
            if (ftpCommand.requiresAuthentication() && !session.isAuthenticated()) {
                session.sendResponse("530 Not logged in");
                return;
            }
            ftpCommand.execute(session, argument);
        } catch (Exception e) {
            session.logError("Command execution error", e);
            session.sendResponse("500 Internal server error");
        }
    }

    public com.ftpserver.user.User getCurrentUser() {
        return session.getCurrentUser();
    }

    public boolean isAuthenticated() {
        return session.isAuthenticated();
    }
}
