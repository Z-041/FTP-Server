package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

import java.io.File;

public class SizeCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        String filePath = session.resolvePath(argument);
        File file = new File(session.getRealPath(filePath));
        if (!file.exists() || !file.isFile()) {
            session.sendResponse("550 File not found");
            return;
        }
        session.sendResponse("213 " + file.length());
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
