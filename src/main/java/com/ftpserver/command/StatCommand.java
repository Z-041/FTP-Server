package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class StatCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        session.sendResponse("211-FTP server status:");
        session.sendResponse("211  Connected to " + session.getClientIp());
        session.sendResponse("211  Logged in as " + 
                           (session.getCurrentUser() != null ? session.getCurrentUser().getUsername() : "unknown"));
        session.sendResponse("211  Current directory: " + session.getCurrentDirectory());
        session.sendResponse("211  Type: " + (session.isAsciiMode() ? "ASCII" : "BINARY"));
        session.sendResponse("211  Data connection: " + 
                           (session.getPassiveDataConnection() != null ? "Passive" : "Active"));
        session.sendResponse("211 End of status");
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
