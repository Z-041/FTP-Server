package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class SiteCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (argument == null || argument.trim().isEmpty()) {
            session.sendResponse("501 Syntax error in parameters");
            return;
        }
        
        String[] parts = argument.trim().split("\\s+");
        if (parts.length == 0) {
            session.sendResponse("501 Syntax error in parameters");
            return;
        }
        
        String command = parts[0].toUpperCase();
        
        switch (command) {
            case "HELP":
                session.sendResponse("214-SITE commands supported:");
                session.sendResponse("214 HELP");
                session.sendResponse("214 End of help");
                break;
                
            default:
                session.sendResponse("502 SITE command not implemented: " + command);
                break;
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
