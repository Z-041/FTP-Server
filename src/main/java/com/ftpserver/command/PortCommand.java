package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

import java.net.InetAddress;

public class PortCommand implements FtpCommand {
    @Override
    @SuppressWarnings("DuplicatedCode")
    public void execute(FtpSession session, String argument) {
        if (!session.getConfig().isEnableActiveMode()) {
            session.sendResponse("502 Active mode disabled");
            return;
        }
        try {
            String[] parts = argument.split(",");
            byte[] addrBytes = new byte[4];
            for (int i = 0; i < 4; i++) {
                addrBytes[i] = (byte) Integer.parseInt(parts[i]);
            }
            session.setActiveModeAddress(InetAddress.getByAddress(addrBytes));
            session.setActiveModePort(Integer.parseInt(parts[4]) * 256 + Integer.parseInt(parts[5]));
            session.sendResponse("200 PORT command successful");
        } catch (Exception e) {
            session.sendResponse("501 Invalid PORT parameters");
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
