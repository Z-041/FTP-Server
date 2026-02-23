package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class HelpCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        session.sendResponse("214-Commands supported:");
        session.sendResponse("214-USER PASS QUIT SYST FEAT PWD CWD CDUP MKD");
        session.sendResponse("214-RMD DELE RNFR RNTO LIST NLST MLSD RETR STOR");
        session.sendResponse("214-TYPE PORT PASV EPSV NOOP OPTS SITE SIZE MDTM");
        session.sendResponse("214-REST ABOR HELP STAT");
        session.sendResponse("214 Direct comments to server admin");
    }

    @Override
    public boolean requiresAuthentication() {
        return false;
    }
}
