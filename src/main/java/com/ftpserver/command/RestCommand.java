package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class RestCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (argument == null || argument.trim().isEmpty()) {
            session.sendResponse("501 Syntax error in parameters");
            return;
        }
        
        try {
            long offset = Long.parseLong(argument.trim());
            if (offset < 0) {
                session.sendResponse("501 Invalid restart offset");
                return;
            }
            
            session.setRestartOffset(offset);
            session.sendResponse("350 Restarting at " + offset + ". Send STORE or RETRIEVE to initiate transfer");
        } catch (NumberFormatException e) {
            session.sendResponse("501 Invalid restart offset");
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
