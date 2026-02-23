package com.ftpserver.command;

import com.ftpserver.session.FtpSession;

public interface FtpCommand {
    void execute(FtpSession session, String argument) throws Exception;
    boolean requiresAuthentication();
}
