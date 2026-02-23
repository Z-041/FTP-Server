package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public class FeatCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        session.sendResponse("211-Features:");
        session.sendResponse(" UTF8");
        session.sendResponse(" MLSD");
        session.sendResponse(" SIZE");
        session.sendResponse(" MDTM");
        session.sendResponse(" REST STREAM");
        session.sendResponse("211 End");
    }

    @Override
    public boolean requiresAuthentication() {
        return false;
    }
}
