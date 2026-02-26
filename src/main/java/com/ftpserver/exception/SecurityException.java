package com.ftpserver.exception;

public class SecurityException extends FtpException {
    public SecurityException(String message) {
        super(550, message);
    }

    public SecurityException(String message, Throwable cause) {
        super(550, message, cause);
    }
}
