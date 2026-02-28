package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

import java.net.InetAddress;

public class PasvCommand implements FtpCommand {
    @Override
    public void execute(FtpSession session, String argument) {
        if (!session.getConfig().isEnablePassiveMode()) {
            session.sendResponse("502 Passive mode disabled");
            return;
        }
        try {
            InetAddress serverAddr;
            if (session.getConfig().getPassiveAddress() != null && 
                !session.getConfig().getPassiveAddress().isEmpty()) {
                serverAddr = InetAddress.getByName(session.getConfig().getPassiveAddress());
            } else {
                serverAddr = session.getControlSocket().getLocalAddress();
                if (serverAddr.isLoopbackAddress()) {
                    String localIp = java.net.InetAddress.getLocalHost().getHostAddress();
                    if (!localIp.equals("127.0.0.1")) {
                        serverAddr = InetAddress.getByName(localIp);
                    }
                }
            }
            byte[] addrBytes = serverAddr.getAddress();
            if (addrBytes.length != 4) {
                serverAddr = InetAddress.getByName("127.0.0.1");
                addrBytes = serverAddr.getAddress();
            }
            session.setPassiveDataConnection(session.createPassiveDataConnection());
            int p1 = session.getPassiveDataConnection().getPort() / 256;
            int p2 = session.getPassiveDataConnection().getPort() % 256;
            String response = String.format("227 Entering Passive Mode (%d,%d,%d,%d,%d,%d)",
                    addrBytes[0] & 0xff, addrBytes[1] & 0xff, 
                    addrBytes[2] & 0xff, addrBytes[3] & 0xff, p1, p2);
            session.sendResponse(response);
        } catch (Exception e) {
            session.logError("PASV error", e);
            session.sendResponse("425 Can't open passive connection");
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
