package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class TypeCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (argument.equalsIgnoreCase("A")) {
            session.setAsciiMode(true);
            session.sendResponse("200 Type set to A");
        } else if (argument.equalsIgnoreCase("I")) {
            session.setAsciiMode(false);
            session.sendResponse("200 Type set to I");
        } else {
            session.sendResponse("504 Command not implemented for that parameter");
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
