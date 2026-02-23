package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class UserCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        session.setPendingUsername(argument);
        session.sendResponse("331 Username okay, need password");
    }

    @Override
    public boolean requiresAuthentication() {
        return false;
    }
}
