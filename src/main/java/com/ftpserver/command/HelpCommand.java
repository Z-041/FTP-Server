package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class HelpCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (argument == null || argument.trim().isEmpty()) {
            session.sendResponse("214-The following commands are supported:");
            session.sendResponse("214-USER PASS QUIT SYST FEAT PWD CWD CDUP");
            session.sendResponse("214-MKD RMD DELE RNFR RNTO LIST NLST");
            session.sendResponse("214-MLSD MLST RETR STOR TYPE PORT PASV");
            session.sendResponse("214-EPSV NOOP OPTS SITE SIZE MDTM");
            session.sendResponse("214-REST ABOR HELP STAT EPRT");
            session.sendResponse("214 End of help");
        } else {
            String cmd = argument.trim().toUpperCase();
            session.sendResponse("214 Help for " + cmd + ": Command available");
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return false;
    }
}
