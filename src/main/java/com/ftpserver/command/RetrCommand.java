package com.ftpserver.command;

import com.ftpserver.data.DataConnection;
import com.ftpserver.session.FtpSession;
import com.ftpserver.user.User;

import java.io.File;

public class RetrCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (session.getCurrentUser() != null && 
            !session.getCurrentUser().hasPermission(User.Permission.READ)) {
            session.sendResponse("550 Permission denied");
            return;
        }
        String filePath = session.resolvePath(argument);
        File file = new File(session.getRealPath(filePath));
        if (!file.exists() || !file.isFile()) {
            session.sendResponse("550 File not found");
            return;
        }
        session.sendResponse("150 Opening " + (session.isAsciiMode() ? "ASCII" : "BINARY") + 
                           " mode data connection for " + file.getName());
        DataConnection dc = null;
        try {
            dc = session.openDataConnection();
            dc.setAsciiMode(session.isAsciiMode());
            dc.sendFile(file);
            dc.close();
            dc = null;
            session.sendResponse("226 Transfer complete");
            session.logInfo("File downloaded: " + filePath);
        } catch (Exception e) {
            session.logError("RETR error", e);
            session.sendResponse("425 Can't open data connection");
        } finally {
            if (dc != null) {
                try {
                    dc.close();
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
