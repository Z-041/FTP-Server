package com.ftpserver.command;

import com.ftpserver.data.DataConnection;
import com.ftpserver.session.FtpSession;
import com.ftpserver.user.User;
import com.ftpserver.util.CrossPlatformUtil;

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
        File dir = new File(session.getRealPath(listPath));
        if (!dir.exists()) {
            session.sendResponse("550 Directory not found");
            return;
        }
        session.sendResponse("150 Opening ASCII mode data connection for file list");
        DataConnection dc = null;
        try {
            dc = session.openDataConnection();
            dc.setAsciiMode(session.isAsciiMode());
            StringBuilder sb = new StringBuilder();
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    sb.append(session.formatFileInfo(file)).append(CrossPlatformUtil.CRLF);
                }
            }
            dc.sendListing(sb.toString());
            dc.close();
            dc = null;
            session.sendResponse("226 Transfer complete");
        } catch (Exception e) {
            session.logError("LIST error", e);
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
