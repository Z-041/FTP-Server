package com.ftpserver.command;

import com.ftpserver.data.DataConnection;
import com.ftpserver.session.FtpSession;
import com.ftpserver.user.User;

import java.io.File;
import java.io.IOException;

public class StorCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (session.getCurrentUser() != null && 
            !session.getCurrentUser().hasPermission(com.ftpserver.user.User.Permission.WRITE)) {
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
        File parentDir = file.getParentFile();
        if (parentDir != null) {
            if (!parentDir.exists()) {
                // 检查创建目录权限
                if (session.getCurrentUser() != null && 
                    !session.getCurrentUser().hasPermission(com.ftpserver.user.User.Permission.CREATE_DIR)) {
                    session.sendResponse("550 Permission denied");
                    return;
                }
                boolean created = parentDir.mkdirs();
                if (!created) {
                    session.sendResponse("550 Failed to create directory");
                    return;
                }
            } else if (!parentDir.canWrite()) {
                session.sendResponse("550 Permission denied");
                return;
            }
        }
        long restartOffset = session.getRestartOffset();
        if (restartOffset > 0 && file.exists() && file.length() >= restartOffset) {
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
            dc.receiveFile(file);
            session.sendResponse("226 Transfer complete");
            session.logInfo("File uploaded: " + filePath);
        } catch (com.ftpserver.data.DataConnectionException e) {
            session.logError("STOR data connection error", e);
            session.sendResponse("425 Can't open data connection");
        } catch (IOException e) {
            session.logError("STOR IO error", e);
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
