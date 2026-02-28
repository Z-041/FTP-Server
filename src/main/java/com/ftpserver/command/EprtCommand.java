package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

import java.net.InetAddress;

public class EprtCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (!session.getConfig().isEnableActiveMode()) {
            session.sendResponse("502 Active mode disabled");
            return;
        }
        
        if (argument == null || argument.trim().isEmpty()) {
            session.sendResponse("501 Syntax error in parameters");
            return;
        }
        
        try {
            String[] parts = argument.split("\\|");
            if (parts.length < 4) {
                session.sendResponse("501 Invalid EPRT parameters");
                return;
            }
            
            int networkProtocol = Integer.parseInt(parts[1]);
            if (networkProtocol != 1 && networkProtocol != 2) {
                session.sendResponse("522 Unsupported network protocol");
                return;
            }
            
            String address = parts[2];
            int port = Integer.parseInt(parts[3]);
            
            if (port <= 0 || port > 65535) {
                session.sendResponse("501 Invalid port number");
                return;
            }
            
            InetAddress addr = InetAddress.getByName(address);
            session.setActiveModeAddress(addr);
            session.setActiveModePort(port);
            
            session.sendResponse("200 EPRT command successful");
        } catch (NumberFormatException e) {
            session.sendResponse("501 Invalid EPRT parameters");
        } catch (Exception e) {
            session.logError("EPRT error", e);
            session.sendResponse("501 Syntax error in parameters");
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
