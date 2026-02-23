package com.ftpserver.command;

import com.ftpserver.session.FtpSession;
import com.ftpserver.user.User;

import java.io.File;

public class RnfrCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (session.getCurrentUser() != null && 
            !session.getCurrentUser().hasPermission(User.Permission.RENAME)) {
            session.sendResponse("550 Permission denied");
            return;
        }
        if (argument == null || argument.trim().isEmpty()) {
            session.sendResponse("501 Syntax error in parameters or arguments");
            return;
        }
        String path = session.resolvePath(argument);
        File file = new File(session.getRealPath(path));
        if (!file.exists()) {
            session.sendResponse("550 File not found");
            return;
        }
        session.setRenameFromPath(path);
        session.setRenameFromPending(true);
        session.sendResponse("350 File exists, ready for destination name");
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
