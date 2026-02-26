package com.ftpserver.exception;

public class FtpException extends Exception {
    private final int responseCode;
    private final String responseMessage;

    public FtpException(int responseCode, String message) {
        super(message);
        this.responseCode = responseCode;
        this.responseMessage = message;
    }

    public FtpException(int responseCode, String message, Throwable cause) {
        super(message, cause);
        this.responseCode = responseCode;
        this.responseMessage = message;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }
}
