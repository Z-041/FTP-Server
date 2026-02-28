package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class StatCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (argument == null || argument.trim().isEmpty()) {
            session.sendResponse("211-FTP server status:");
            session.sendResponse("211  Connected to " + session.getClientIp());
            session.sendResponse("211  Logged in as " + 
                               (session.getCurrentUser() != null ? session.getCurrentUser().getUsername() : "unknown"));
            session.sendResponse("211  Current directory: " + session.getCurrentDirectory());
            session.sendResponse("211  Transfer type: " + (session.isAsciiMode() ? "ASCII" : "BINARY"));
            session.sendResponse("211  Data connection type: " + 
                               (session.getPassiveDataConnection() != null ? "PASSIVE" : "ACTIVE"));
            session.sendResponse("211 End of status");
        } else {
            session.sendResponse("501 Syntax error in parameters");
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
