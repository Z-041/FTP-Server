package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class AborCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        session.sendResponse("226 Abort successful");
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
