package com.ftpserver.command;

import com.ftpserver.data.DataConnection;
import com.ftpserver.session.FtpSession;

import java.io.IOException;

public class AborCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        try {
            if (session.getPassiveDataConnection() != null) {
                try {
                    session.getPassiveDataConnection().close();
                } catch (Exception e) {
                    session.logError("Error closing passive data connection during abort", e);
                }
                session.setPassiveDataConnection(null);
            }
            
            session.setActiveModeAddress(null);
            session.setActiveModePort(0);
            
            session.sendResponse("226 ABOR successful");
        } catch (Exception e) {
            session.logError("ABOR error", e);
            session.sendResponse("426 Abort failed");
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
