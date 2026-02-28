package com.ftpserver.command;

import com.ftpserver.session.FtpSession;
import com.ftpserver.user.User;
import com.ftpserver.util.CrossPlatformUtil;
import com.ftpserver.util.FileListUtils;

import java.io.File;

public class ListCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (session.getCurrentUser() != null && 
            !session.getCurrentUser().hasPermission(User.Permission.LIST)) {
            session.sendResponse("550 Permission denied");
            return;
        }
        String listPath = argument;
        if (listPath.startsWith("-")) {
            listPath = "";
        }
        listPath = listPath.isEmpty() ? session.getCurrentDirectory() : session.resolvePath(listPath);
        
        if (!session.getPathResolver().isPathSafe(listPath)) {
            session.sendResponse("550 Path not allowed");
            return;
        }
        
        File dir = new File(session.getRealPath(listPath));
        if (!dir.exists()) {
            session.sendResponse("550 Directory not found");
            return;
        }
        if (!dir.isDirectory()) {
            session.sendResponse("550 Not a directory");
            return;
        }
        
        FileListUtils.executeFileListOperation(
            session, 
            dir, 
            file -> session.formatFileInfo(file),
            "LIST"
        );
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
