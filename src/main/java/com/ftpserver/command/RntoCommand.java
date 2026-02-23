package com.ftpserver.command;

import com.ftpserver.session.FtpSession;
import com.ftpserver.user.User;

import java.io.File;

public class RntoCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (!session.isRenameFromPending()) {
            session.sendResponse("503 Bad sequence of commands");
            return;
        }
        if (session.getCurrentUser() != null && 
            !session.getCurrentUser().hasPermission(User.Permission.RENAME)) {
            session.sendResponse("550 Permission denied");
            return;
        }
        String destPath = session.resolvePath(argument);
        File from = new File(session.getRealPath(session.getRenameFromPath()));
        File to = new File(session.getRealPath(destPath));
        if (from.renameTo(to)) {
            session.sendResponse("250 Rename successful");
        } else {
            session.sendResponse("550 Rename failed");
        }
        session.setRenameFromPending(false);
        session.setRenameFromPath(null);
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
