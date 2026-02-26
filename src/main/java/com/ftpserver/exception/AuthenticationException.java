package com.ftpserver.exception;

public class AuthenticationException extends FtpException {
    public AuthenticationException(String message) {
        super(530, message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(530, message, cause);
    }
}
