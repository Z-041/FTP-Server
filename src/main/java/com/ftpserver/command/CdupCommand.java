package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CdupCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (!session.getCurrentDirectory().equals("/")) {
            Path p = Paths.get(session.getCurrentDirectory()).getParent();
            session.setCurrentDirectory(p != null ? p.toString().replace("\\", "/") : "/");
        }
        session.sendResponse("200 Directory changed to parent");
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
