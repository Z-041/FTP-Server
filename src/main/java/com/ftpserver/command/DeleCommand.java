package com.ftpserver.command;

import com.ftpserver.session.FtpSession;
import com.ftpserver.user.User;

import java.io.File;

public class DeleCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (session.getCurrentUser() != null && 
            !session.getCurrentUser().hasPermission(User.Permission.DELETE)) {
            session.sendResponse("550 Permission denied");
            return;
        }
        String fullPath = session.resolvePath(argument);
        
        // 检查路径安全性
        if (!session.getPathResolver().isPathSafe(fullPath)) {
            session.sendResponse("550 Path not allowed");
            return;
        }
        
        File file = new File(session.getRealPath(fullPath));
        if (file.exists() && file.isFile() && file.delete()) {
            session.sendResponse("250 File deleted");
        } else {
            session.sendResponse("550 Delete operation failed");
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
