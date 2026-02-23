package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class QuitCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        session.sendResponse("221 Goodbye");
        session.close();
    }

    @Override
    public boolean requiresAuthentication() {
        return false;
    }
}
