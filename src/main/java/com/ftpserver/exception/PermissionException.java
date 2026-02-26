package com.ftpserver.exception;

public class PermissionException extends FtpException {
    public PermissionException(String message) {
        super(550, message);
    }

    public PermissionException(String message, Throwable cause) {
        super(550, message, cause);
    }
}
