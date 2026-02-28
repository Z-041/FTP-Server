package com.ftpserver.command;

import com.ftpserver.data.DataConnection;
import com.ftpserver.session.FtpSession;
import com.ftpserver.user.User;
import com.ftpserver.util.CrossPlatformUtil;

import java.io.File;

public class MlsdCommand implements FtpCommand {
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
        session.sendResponse("150 Opening data connection for MLSD");
        DataConnection dc = null;
        try {
            dc = session.openDataConnection();
            dc.setAsciiMode(false);
            StringBuilder sb = new StringBuilder();
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    sb.append(session.formatMlsdEntry(file)).append(CrossPlatformUtil.CRLF);
                }
            }
            dc.sendListing(sb.toString());
            dc.close();
            dc = null;
            session.sendResponse("226 Transfer complete");
        } catch (Exception e) {
            session.logError("MLSD error", e);
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
