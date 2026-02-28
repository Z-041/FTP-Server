package com.ftpserver.command;

import com.ftpserver.data.DataConnection;
import com.ftpserver.session.FtpSession;
import com.ftpserver.user.User;

import java.io.File;
import java.io.IOException;

public class RetrCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (session.getCurrentUser() != null && 
            !session.getCurrentUser().hasPermission(com.ftpserver.user.User.Permission.READ)) {
            session.sendResponse("550 Permission denied");
            return;
        }
        String filePath = session.resolvePath(argument);
        
        // 检查路径安全性
        if (!session.getPathResolver().isPathSafe(filePath)) {
            session.sendResponse("550 Path not allowed");
            return;
        }
        
        File file = new File(session.getRealPath(filePath));
        if (!file.exists()) {
            session.sendResponse("550 File not found");
            return;
        }
        if (!file.isFile()) {
            session.sendResponse("550 Not a file");
            return;
        }
        if (!file.canRead()) {
            session.sendResponse("550 Permission denied");
            return;
        }
        
        long restartOffset = session.getRestartOffset();
        if (restartOffset > 0) {
            if (restartOffset >= file.length()) {
                session.sendResponse("450 Restart offset beyond file size");
                session.clearRestartOffset();
                return;
            }
            session.sendResponse("150 Opening " + (session.isAsciiMode() ? "ASCII" : "BINARY") + 
                               " mode data connection for " + file.getName() + 
                               " (resuming at " + restartOffset + ")");
        } else {
            session.sendResponse("150 Opening " + (session.isAsciiMode() ? "ASCII" : "BINARY") + 
                               " mode data connection for " + file.getName());
        }
        
        try (DataConnection dc = session.openDataConnection()) {
            dc.setAsciiMode(session.isAsciiMode());
            dc.setRestartOffset(restartOffset);
            dc.sendFile(file);
            session.sendResponse("226 Transfer complete");
            session.logInfo("File downloaded: " + filePath);
        } catch (com.ftpserver.data.DataConnectionException e) {
            session.logError("RETR data connection error", e);
            session.sendResponse("425 Can't open data connection");
        } catch (IOException e) {
            session.logError("RETR IO error", e);
            session.sendResponse("425 Can't open data connection");
        } finally {
            session.clearRestartOffset();
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
