package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

import java.io.File;

public class MdtmCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        String filePath = session.resolvePath(argument);
        File file = new File(session.getRealPath(filePath));
        if (!file.exists()) {
            session.sendResponse("550 File not found");
            return;
        }
        session.sendResponse("213 " + session.formatMlsdTime(file.lastModified()));
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
