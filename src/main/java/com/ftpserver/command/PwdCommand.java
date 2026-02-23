package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class PwdCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        String dir = session.getCurrentDirectory();
        session.sendResponse("257 \"" + dir + "\" is current directory");
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
