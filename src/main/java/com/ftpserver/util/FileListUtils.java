package com.ftpserver.util;

import com.ftpserver.data.DataConnection;
import com.ftpserver.session.FtpSession;

import java.io.File;

public class FileListUtils {
    
    /**
     * 执行文件列表操作
     * @param session FTP 会话
     * @param dir 目录
     * @param entryFormatter 条目格式化器
     * @param operationName 操作名称（用于日志）
     */
    public static boolean executeFileListOperation(FtpSession session, File dir, FileEntryFormatter entryFormatter, String operationName) {
        String responseMsg = "150 Opening data connection for " + operationName;
        session.sendResponse(responseMsg);
        
        try (DataConnection dc = session.openDataConnection()) {
            dc.setAsciiMode(false);
            StringBuilder sb = new StringBuilder();
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    sb.append(entryFormatter.format(file)).append(CrossPlatformUtil.CRLF);
                }
            }
            dc.sendListing(sb.toString());
            session.sendResponse("226 Transfer complete");
            return true;
        } catch (com.ftpserver.data.DataConnectionException e) {
            session.logError(operationName + " data connection error", e);
            session.sendResponse("425 Can't open data connection");
            return false;
        } catch (Exception e) {
            session.logError(operationName + " error", e);
            session.sendResponse("425 Can't open data connection");
            return false;
        }
    }
    
    /**
     * 文件条目格式化接口
     */
    public interface FileEntryFormatter {
        String format(File file);
    }
}