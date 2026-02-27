package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

import java.io.File;

public class CwdCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (session.getCurrentUser() != null && 
            !session.getCurrentUser().hasPermission(com.ftpserver.user.User.Permission.READ)) {
            session.sendResponse("550 Permission denied");
            return;
        }
        String newDir = session.resolvePath(argument);
        
        // 检查路径安全性
        if (!session.getPathResolver().isPathSafe(newDir)) {
            session.sendResponse("550 Path not allowed");
            return;
        }
        
        File dir = new File(session.getRealPath(newDir));
        if (!dir.exists()) {
            session.sendResponse("550 Directory not found");
            return;
        }
        if (!dir.isDirectory()) {
            session.sendResponse("550 Not a directory");
            return;
        }
        if (!dir.canRead()) {
            session.sendResponse("550 Permission denied");
            return;
        }
        session.setCurrentDirectory(newDir);
        session.sendResponse("250 Directory changed to " + session.getCurrentDirectory());
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
