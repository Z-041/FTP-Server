package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class NoopCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        session.sendResponse("200 OK");
    }

    @Override
    public boolean requiresAuthentication() {
        return false;
    }
}
