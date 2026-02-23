package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class SystCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        session.sendResponse("215 UNIX Type: L8");
    }

    @Override
    public boolean requiresAuthentication() {
        return false;
    }
}
