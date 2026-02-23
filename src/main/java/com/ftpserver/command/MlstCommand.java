package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class MlstCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        session.sendResponse("250-Start of list");
        session.sendResponse("250 End of list");
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
