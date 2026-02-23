package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class OptsCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        String[] parts = argument.split(" ");
        if (parts.length >= 1) {
            String option = parts[0].toUpperCase();
            if ("UTF8".equals(option)) {
                session.sendResponse("200 OK");
                return;
            }
        }
        session.sendResponse("501 Invalid option");
    }

    @Override
    public boolean requiresAuthentication() {
        return false;
    }
}
