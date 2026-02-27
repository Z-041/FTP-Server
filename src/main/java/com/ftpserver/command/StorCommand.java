package com.ftpserver.command;

import com.ftpserver.data.DataConnection;
import com.ftpserver.session.FtpSession;
import com.ftpserver.user.User;

import java.io.File;

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
        session.sendResponse("150 Opening " + (session.isAsciiMode() ? "ASCII" : "BINARY") + 
                           " mode data connection for " + file.getName());
        DataConnection dc = null;
        try {
            dc = session.openDataConnection();
            dc.setAsciiMode(session.isAsciiMode());
            dc.receiveFile(file);
            dc.close();
            dc = null;
            session.sendResponse("226 Transfer complete");
            session.logInfo("File uploaded: " + filePath);
        } catch (Exception e) {
            session.logError("STOR error", e);
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
