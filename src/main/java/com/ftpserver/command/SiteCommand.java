package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class SiteCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        String[] parts = argument.split(" ");
        if (parts.length >= 1) {
            String command = parts[0].toUpperCase();
            if ("HELP".equals(command)) {
                session.sendResponse("214-Site commands:");
                session.sendResponse("214 HELP");
                session.sendResponse("214 End");
                return;
            }
        }
        session.sendResponse("501 Invalid SITE command");
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
