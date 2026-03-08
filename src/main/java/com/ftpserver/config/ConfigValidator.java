package com.ftpserver.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConfigValidator {
    
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        
        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public List<String> getWarnings() {
            return warnings;
        }
        
        public String getErrorMessage() {
            return String.join("\n", errors);
        }
        
        public String getWarningMessage() {
            return String.join("\n", warnings);
        }
    }
    
    public static ValidationResult validate(ServerConfig config) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        validatePort(config, errors, warnings);
        validateRootDirectory(config, errors, warnings);
        validateMaxConnections(config, errors, warnings);
        validateConnectionTimeout(config, errors, warnings);
        validateDataPortRange(config, errors, warnings);
        validateLogDirectory(config, errors, warnings);
        validatePassiveAddress(config, errors, warnings);
        validateModes(config, errors, warnings);
        
        boolean valid = errors.isEmpty();
        return new ValidationResult(valid, errors, warnings);
    }
    
    private static void validatePort(ServerConfig config, List<String> errors, List<String> warnings) {
        int port = config.getPort();
        if (port <= 0 || port > 65535) {
            errors.add("端口号必须在 1-65535 之间");
        } else if (port < 1024) {
            warnings.add("端口号 " + port + " 小于 1024，可能需要管理员权限");
        } else if (port == 21) {
            warnings.add("使用标准FTP端口 21 可能需要管理员权限");
        }
    }
    
    private static void validateRootDirectory(ServerConfig config, List<String> errors, List<String> warnings) {
        String rootDir = config.getRootDirectory();
        if (rootDir == null || rootDir.trim().isEmpty()) {
            errors.add("根目录不能为空");
            return;
        }
        
        File dir = new File(rootDir);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                errors.add("无法创建根目录: " + rootDir);
            } else {
                warnings.add("根目录已自动创建: " + rootDir);
            }
        } else if (!dir.isDirectory()) {
            errors.add("根目录路径不是目录: " + rootDir);
        } else if (!dir.canRead()) {
            errors.add("无法读取根目录: " + rootDir);
        } else if (!dir.canWrite()) {
            errors.add("无法写入根目录: " + rootDir);
        }
    }
    
    private static void validateMaxConnections(ServerConfig config, List<String> errors, List<String> warnings) {
        int maxConn = config.getMaxConnections();
        if (maxConn <= 0) {
            errors.add("最大连接数必须大于 0");
        } else if (maxConn > 1000) {
            warnings.add("最大连接数 " + maxConn + " 过大，可能影响性能");
        }
    }
    
    private static void validateConnectionTimeout(ServerConfig config, List<String> errors, List<String> warnings) {
        int timeout = config.getConnectionTimeout();
        if (timeout < 0) {
            errors.add("连接超时不能为负数");
        } else if (timeout == 0) {
            warnings.add("连接超时设置为 0，表示永不超时");
        } else if (timeout < 30) {
            warnings.add("连接超时 " + timeout + " 秒过短，可能导致连接中断");
        } else if (timeout > 3600) {
            warnings.add("连接超时 " + timeout + " 秒过长，可能导致资源浪费");
        }
    }
    
    private static void validateDataPortRange(ServerConfig config, List<String> errors, List<String> warnings) {
        int start = config.getDataPortRangeStart();
        int end = config.getDataPortRangeEnd();
        
        if (start <= 0 || start > 65535) {
            errors.add("数据端口范围起始端口必须在 1-65535 之间");
        }
        
        if (end <= 0 || end > 65535) {
            errors.add("数据端口范围结束端口必须在 1-65535 之间");
        }
        
        if (start > 0 && end > 0 && start > end) {
            errors.add("数据端口范围起始端口不能大于结束端口");
        }
        
        if (start > 0 && end > 0 && start < 1024) {
            warnings.add("数据端口范围包含小于 1024 的端口，可能需要管理员权限");
        }
        
        if (start > 0 && end > 0 && (end - start) < 10) {
            warnings.add("数据端口范围过小（" + (end - start + 1) + " 个端口），可能导致并发传输失败");
        }
    }
    
    private static void validateLogDirectory(ServerConfig config, List<String> errors, List<String> warnings) {
        String logDir = config.getLogDirectory();
        if (logDir == null || logDir.trim().isEmpty()) {
            errors.add("日志目录不能为空");
            return;
        }
        
        File dir = new File(logDir);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                errors.add("无法创建日志目录: " + logDir);
            }
        } else if (!dir.isDirectory()) {
            errors.add("日志目录路径不是目录: " + logDir);
        } else if (!dir.canWrite()) {
            errors.add("无法写入日志目录: " + logDir);
        }
    }
    
    private static void validatePassiveAddress(ServerConfig config, List<String> errors, List<String> warnings) {
        String passiveAddr = config.getPassiveAddress();
        if (passiveAddr != null && !passiveAddr.trim().isEmpty()) {
            if (!isValidIPAddress(passiveAddr)) {
                errors.add("被动模式地址格式无效: " + passiveAddr);
            }
        }
    }
    
    private static void validateModes(ServerConfig config, List<String> errors, List<String> warnings) {
        if (!config.isEnablePassiveMode() && !config.isEnableActiveMode()) {
            errors.add("必须启用至少一种传输模式（主动或被动）");
        }
    }
    
    private static boolean isValidIPAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        
        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            return false;
        }
        
        return true;
    }
}
