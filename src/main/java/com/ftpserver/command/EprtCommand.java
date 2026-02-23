package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class EprtCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        session.sendResponse("522 Extended PORT not implemented, use EPSV instead");
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
