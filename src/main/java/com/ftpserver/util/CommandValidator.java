package com.ftpserver.util;

import java.util.regex.Pattern;

/**
 * 命令参数验证工具类
 * 用于验证和清理 FTP 命令参数，防止命令注入攻击
 */
public class CommandValidator {
    
    private static final Pattern DANGEROUS_CHAR_PATTERN = Pattern.compile("[;|&$`\\\\<>\"']");
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile("\\.\\.[\\\\/]");
    private static final Pattern VALID_FILENAME_PATTERN = Pattern.compile("^[\\w\\.\\-\\s\\u4e00-\\u9fa5]+$");
    
    private CommandValidator() {
    }
    
    /**
     * 验证命令参数是否安全
     * @param argument 命令参数
     * @return 是否安全
     */
    public static boolean isArgumentSafe(String argument) {
        if (argument == null || argument.isEmpty()) {
            return true;
        }
        
        if (DANGEROUS_CHAR_PATTERN.matcher(argument).find()) {
            return false;
        }
        
        if (PATH_TRAVERSAL_PATTERN.matcher(argument).find()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 清理命令参数，移除危险字符
     * @param argument 原始参数
     * @return 清理后的参数
     */
    public static String sanitizeArgument(String argument) {
        if (argument == null || argument.isEmpty()) {
            return argument;
        }
        
        String sanitized = DANGEROUS_CHAR_PATTERN.matcher(argument).replaceAll("");
        sanitized = sanitized.replaceAll("\\s+", " ").trim();
        
        if (sanitized.isEmpty()) {
            return null;
        }
        
        return sanitized;
    }
    
    /**
     * 验证文件名是否合法
     * @param filename 文件名
     * @return 是否合法
     */
    public static boolean isValidFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        
        if (filename.equals(".") || filename.equals("..")) {
            return false;
        }
        
        if (filename.length() > 255) {
            return false;
        }
        
        if (DANGEROUS_CHAR_PATTERN.matcher(filename).find()) {
            return false;
        }
        
        return VALID_FILENAME_PATTERN.matcher(filename).matches();
    }
    
    /**
     * 验证目录名是否合法
     * @param dirname 目录名
     * @return 是否合法
     */
    public static boolean isValidDirname(String dirname) {
        return isValidFilename(dirname);
    }
    
    /**
     * 验证路径是否合法
     * @param path 路径
     * @return 是否合法
     */
    public static boolean isValidPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        if (PATH_TRAVERSAL_PATTERN.matcher(path).find()) {
            return false;
        }
        
        String[] segments = path.split("[\\\\/]");
        for (String segment : segments) {
            if (!segment.isEmpty() && !isValidFilename(segment)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 清理路径
     * @param path 原始路径
     * @return 清理后的路径
     */
    public static String sanitizePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        
        String sanitized = DANGEROUS_CHAR_PATTERN.matcher(path).replaceAll("");
        sanitized = sanitized.replaceAll("\\.\\.", "");
        sanitized = sanitized.replaceAll("[\\\\/]+", "/");
        sanitized = sanitized.replaceAll("^/|/$", "");
        
        if (sanitized.isEmpty()) {
            return null;
        }
        
        return sanitized;
    }
}
