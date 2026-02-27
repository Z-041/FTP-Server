package com.ftpserver.data;

/**
 * 数据连接异常类，用于表示数据连接过程中的各种错误
 */
public class DataConnectionException extends Exception {
    
    /**
     * 异常类型枚举
     */
    public enum ErrorType {
        CONNECTION_ERROR,      // 连接错误
        TRANSFER_ERROR,        // 传输错误
        AUTHENTICATION_ERROR,  // 认证错误
        RESOURCE_ERROR,        // 资源错误
        TIMEOUT_ERROR,         // 超时错误
        UNKNOWN_ERROR          // 未知错误
    }
    
    private final ErrorType errorType;
    
    /**
     * 构造函数
     * @param message 错误信息
     * @param errorType 错误类型
     */
    public DataConnectionException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }
    
    /**
     * 构造函数
     * @param message 错误信息
     * @param errorType 错误类型
     * @param cause 原始异常
     */
    public DataConnectionException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }
    
    /**
     * 获取错误类型
     * @return 错误类型
     */
    public ErrorType getErrorType() {
        return errorType;
    }
    
    /**
     * 获取错误代码
     * @return 错误代码
     */
    public int getErrorCode() {
        switch (errorType) {
            case CONNECTION_ERROR:
                return 425; // Can't open data connection
            case TRANSFER_ERROR:
                return 426; // Connection closed; transfer aborted
            case AUTHENTICATION_ERROR:
                return 530; // Not logged in
            case RESOURCE_ERROR:
                return 550; // Requested action not taken
            case TIMEOUT_ERROR:
                return 421; // Service not available
            default:
                return 500; // Internal server error
        }
    }
}