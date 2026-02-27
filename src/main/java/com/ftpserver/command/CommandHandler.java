package com.ftpserver.command;

import com.ftpserver.config.ServerConfig;
import com.ftpserver.session.FtpSession;
import com.ftpserver.user.UserManager;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * FTP命令处理器，负责处理客户端发送的FTP命令
 */
public class CommandHandler {
    private final FtpSession session; // FTP会话
    private static final Logger logger = Logger.getLogger(CommandHandler.class.getName());
    
    /**
     * 命令执行状态
     */
    public enum CommandStatus {
        PENDING,     // 等待执行
        EXECUTING,   // 执行中
        SUCCESS,     // 执行成功
        FAILED,      // 执行失败
        ERROR        // 执行错误
    }

    public CommandHandler(Socket controlSocket, ServerConfig config, UserManager userManager) throws IOException {
        this.session = new FtpSession(controlSocket, config, userManager);
        logger.log(Level.INFO, "New command handler created for client: " + controlSocket.getInetAddress());
    }

    /**
     * 处理客户端连接，接收和执行FTP命令
     */
    public void handle() {
        try {
            // 发送欢迎消息
            session.sendResponse("220 Welcome to Java FTP Server");
            logger.log(Level.INFO, "Welcome message sent to client: " + session.getRemoteAddress());
            
            String line;
            // 循环读取客户端命令
            while ((line = session.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;
                
                session.logDebug("Received: " + line);
                logger.log(Level.FINE, "Command received from " + session.getRemoteAddress() + ": " + line);
                
                // 解析命令和参数
                int spaceIndex = line.indexOf(' ');
                String command = (spaceIndex > 0 ? line.substring(0, spaceIndex) : line).toUpperCase();
                String argument = spaceIndex > 0 ? line.substring(spaceIndex + 1).trim() : "";
                
                // 执行命令并跟踪状态
                CommandStatus status = executeCommandWithStatus(command, argument);
                logger.log(Level.FINE, "Command " + command + " executed with status: " + status);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Connection error for client: " + session.getRemoteAddress(), e);
            session.logError("Connection error", e);
        } finally {
            // 关闭会话
            session.close();
            logger.log(Level.INFO, "Session closed for client: " + session.getRemoteAddress());
        }
    }

    /**
     * 执行命令并返回执行状态
     * @param command 命令名称
     * @param argument 命令参数
     * @return 命令执行状态
     */
    private CommandStatus executeCommandWithStatus(String command, String argument) {
        CommandStatus status = CommandStatus.PENDING;
        
        try {
            // 前置检查
            if (!preCommandCheck(command)) {
                return CommandStatus.FAILED;
            }
            
            status = CommandStatus.EXECUTING;
            
            // 执行命令
            boolean success = executeCommand(command, argument);
            status = success ? CommandStatus.SUCCESS : CommandStatus.FAILED;
            
            // 后置处理
            postCommandProcessing(command, status);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error executing command " + command, e);
            session.logError("Command execution error", e);
            session.sendResponse("500 Internal server error");
            status = CommandStatus.ERROR;
        }
        
        return status;
    }

    /**
     * 执行命令
     * @param command 命令名称
     * @param argument 命令参数
     * @return 是否执行成功
     */
    private boolean executeCommand(String command, String argument) {
        FtpCommand ftpCommand = CommandFactory.getCommand(command);
        if (ftpCommand == null) {
            session.sendResponse("502 Command not implemented: " + command);
            return false;
        }
        
        if (ftpCommand.requiresAuthentication() && !session.isAuthenticated()) {
            session.sendResponse("530 Not logged in");
            return false;
        }
        
        try {
            ftpCommand.execute(session, argument);
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Command execution failed: " + command, e);
            session.logError("Command execution error: " + command, e);
            session.sendResponse("500 Internal server error");
            return false;
        }
    }

    /**
     * 命令执行前的检查
     * @param command 命令名称
     * @return 是否通过检查
     */
    private boolean preCommandCheck(String command) {
        // 检查会话状态
        if (!session.isConnected()) {
            logger.log(Level.WARNING, "Command received but session is not connected: " + command);
            session.sendResponse("421 Service not available");
            return false;
        }
        
        // 可以添加更多的前置检查，例如：
        // - 命令速率限制
        // - 会话超时检查
        // - 命令白名单/黑名单检查
        
        return true;
    }

    /**
     * 命令执行后的处理
     * @param command 命令名称
     * @param status 执行状态
     */
    private void postCommandProcessing(String command, CommandStatus status) {
        // 可以添加命令执行后的处理逻辑，例如：
        // - 更新会话活动时间
        // - 记录命令执行统计
        // - 根据命令类型执行特定的后置处理
        
        session.updateLastActivityTime();
        
        if (status == CommandStatus.SUCCESS) {
            logger.log(Level.FINE, "Command executed successfully: " + command);
        } else if (status == CommandStatus.FAILED) {
            logger.log(Level.FINE, "Command execution failed: " + command);
        }
    }

    /**
     * 获取当前用户
     * @return 当前用户
     */
    public com.ftpserver.user.User getCurrentUser() {
        return session.getCurrentUser();
    }

    /**
     * 检查是否已认证
     * @return 是否已认证
     */
    public boolean isAuthenticated() {
        return session.isAuthenticated();
    }
}
