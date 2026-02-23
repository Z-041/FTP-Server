package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class RnfrCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        session.setRenameFromPath(session.resolvePath(argument));
        session.setRenameFromPending(true);
        session.sendResponse("350 File exists, ready for destination name");
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
