package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class EpsvCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (!session.getConfig().isEnablePassiveMode()) {
            session.sendResponse("502 Passive mode disabled");
            return;
        }
        try {
            session.setPassiveDataConnection(session.createPassiveDataConnection());
            session.sendResponse("229 Entering Extended Passive Mode (|||" + 
                                session.getPassiveDataConnection().getPort() + "|)");
        } catch (Exception e) {
            session.logError("EPSV error", e);
            session.sendResponse("425 Can't open passive connection");
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
