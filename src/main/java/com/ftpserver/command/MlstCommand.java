package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

import java.io.File;

public class MlstCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (argument == null || argument.trim().isEmpty()) {
            session.sendResponse("501 Syntax error in parameters");
            return;
        }
        
        String filePath = session.resolvePath(argument);
        
        if (!session.getPathResolver().isPathSafe(filePath)) {
            session.sendResponse("550 Path not allowed");
            return;
        }
        
        File file = new File(session.getRealPath(filePath));
        if (!file.exists()) {
            session.sendResponse("550 File not found");
            return;
        }
        
        try {
            StringBuilder facts = new StringBuilder();
            
            if (file.isDirectory()) {
                facts.append("type=dir;");
            } else if (file.isFile()) {
                facts.append("type=file;");
            } else {
                facts.append("type=OS.unix=symlink;");
            }
            
            facts.append("size=").append(file.length()).append(";");
            
            long lastModified = file.lastModified();
            facts.append("modify=").append(session.formatMlsdTime(lastModified)).append(";");
            
            String filename = file.getName();
            session.sendResponse("250-" + facts + " " + filename);
            session.sendResponse("250 End");
        } catch (Exception e) {
            session.logError("MLST error", e);
            session.sendResponse("550 Requested action aborted");
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
