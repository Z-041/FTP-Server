package com.ftpserver.command;

import com.ftpserver.session.FtpSession;
import com.ftpserver.user.User;

import java.io.File;
import java.util.Optional;

public class PassCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (session.getPendingUsername() == null) {
            session.sendResponse("503 Bad sequence of commands");
            return;
        }
        
        Optional<User> userOpt;
        String pendingUsername = session.getPendingUsername();
        if (pendingUsername.equalsIgnoreCase("anonymous") || pendingUsername.equalsIgnoreCase("ftp")) {
            userOpt = session.getUserManager().getUser("anonymous");
        } else {
            userOpt = session.getUserManager().authenticate(pendingUsername, argument);
        }
        
        if (userOpt.isPresent() && userOpt.get().isEnabled()) {
            User user = userOpt.get();
            session.setCurrentUser(user);
            session.setAuthenticated(true);
            String userHome = user.getHomeDirectory();
            
            if (userHome == null || userHome.isEmpty()) {
                userHome = session.getConfig().getRootDirectory();
            }
            
            File homeDir = new File(userHome);
            if (!homeDir.exists()) {
                homeDir.mkdirs();
            }
            
            session.setCurrentDirectory("/");
            session.getPathResolver().setRootDirectory(userHome);
            
            session.logInfo("User " + user.getUsername() + " logged in");
            session.sendResponse("230 User logged in, proceed");
        } else {
            session.sendResponse("530 Login incorrect");
        }
        session.setPendingUsername(null);
    }

    @Override
    public boolean requiresAuthentication() {
        return false;
    }
}
