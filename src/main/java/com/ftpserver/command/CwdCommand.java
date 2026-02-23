package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

import java.io.File;

public class CwdCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        String newDir = session.resolvePath(argument);
        File dir = new File(session.getRealPath(newDir));
        if (dir.exists() && dir.isDirectory()) {
            session.setCurrentDirectory(newDir);
            session.sendResponse("250 Directory changed to " + session.getCurrentDirectory());
        } else {
            session.sendResponse("550 Failed to change directory");
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
