package com.ftpserver.command;

import com.ftpserver.session.FtpSession;
import com.ftpserver.user.User;

import java.io.File;

public class RmdCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (session.getCurrentUser() != null && 
            !session.getCurrentUser().hasPermission(User.Permission.DELETE_DIR)) {
            session.sendResponse("550 Permission denied");
            return;
        }
        String fullPath = session.resolvePath(argument);
        File dir = new File(session.getRealPath(fullPath));
        if (dir.exists() && dir.isDirectory() && dir.delete()) {
            session.sendResponse("250 Directory deleted");
        } else {
            session.sendResponse("550 Delete directory operation failed");
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
