package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class RestCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        session.sendResponse("350 Restart position accepted");
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
