package com.ftpserver.session;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 路径解析器，负责处理FTP路径与系统路径之间的转换和验证
 */
public class PathResolver {
    private String rootDirectory;

    /**
     * 构造函数
     * @param rootDirectory 根目录路径
     */
    public PathResolver(String rootDirectory) {
        this.rootDirectory = normalizeSystemPath(rootDirectory);
        ensureRootDirectoryExists();
    }

    /**
     * 确保根目录存在，如果不存在则创建
     */
    private void ensureRootDirectoryExists() {
        File rootDir = new File(rootDirectory);
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
    }

    /**
     * 设置根目录
     * @param rootDirectory 新的根目录路径
     */
    public void setRootDirectory(String rootDirectory) {
        this.rootDirectory = normalizeSystemPath(rootDirectory);
        ensureRootDirectoryExists();
    }

    /**
     * 解析FTP路径
     * @param currentDirectory 当前目录
     * @param path 要解析的路径
     * @return 解析后的规范化路径
     */
    public String resolvePath(String currentDirectory, String path) {
        if (path == null || path.isEmpty()) {
            return currentDirectory;
        }

        try {
            String normalizedPath = normalizePathSeparators(path);
            String resolvedPath;

            if (normalizedPath.startsWith("/")) {
                resolvedPath = normalizedPath;
            } else {
                resolvedPath = combinePaths(currentDirectory, normalizedPath);
            }

            resolvedPath = removeDuplicateSlashes(resolvedPath);
            resolvedPath = resolvePathSegments(resolvedPath);

            return resolvedPath;
        } catch (Exception e) {
            return currentDirectory;
        }
    }

    /**
     * 解析路径段，处理.和..等特殊路径
     * @param path 路径字符串
     * @return 解析后的路径
     */
    private String resolvePathSegments(String path) {
        String[] segments = path.split("/");
        List<String> result = new ArrayList<>();

        for (String segment : segments) {
            if (segment == null || segment.isEmpty() || segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                if (!result.isEmpty()) {
                    result.remove(result.size() - 1);
                }
            } else {
                result.add(segment);
            }
        }

        if (result.isEmpty()) {
            return "/";
        }

        return "/" + String.join("/", result);
    }

    /**
     * 获取FTP路径对应的实际系统路径
     * @param ftpPath FTP路径
     * @return 实际系统路径
     */
    public String getRealPath(String ftpPath) {
        try {
            String normalizedFtpPath = normalizePathSeparators(ftpPath);
            String normalized = normalizedFtpPath;
            
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }

            normalized = removeDuplicateSlashes(normalized);
            List<String> validSegments = extractValidPathSegments(normalized);

            File rootDir = new File(rootDirectory);
            File targetFile = buildTargetFile(rootDir, validSegments);

            String absoluteRoot = rootDir.getAbsolutePath();
            String absoluteTarget = targetFile.getAbsolutePath();

            String normalizedRoot = normalizePathSeparators(absoluteRoot);
            String normalizedTarget = normalizePathSeparators(absoluteTarget);

            if (!isPathWithinRoot(normalizedRoot, normalizedTarget)) {
                return rootDirectory;
            }

            return absoluteTarget;
        } catch (Exception e) {
            return rootDirectory;
        }
    }

    /**
     * 检查路径是否安全（不包含路径遍历攻击）
     * @param ftpPath FTP路径
     * @return 是否安全
     */
    public boolean isPathSafe(String ftpPath) {
        if (ftpPath == null || ftpPath.isEmpty()) {
            return false;
        }
        
        try {
            String normalizedFtpPath = normalizePathSeparators(ftpPath);
            String normalized = normalizedFtpPath;
            
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            
            normalized = removeDuplicateSlashes(normalized);
            
            List<String> validSegments = new ArrayList<>();
            boolean triedToEscape = false;
            
            for (String segment : normalized.split("/")) {
                if (segment == null || segment.isEmpty() || segment.equals(".")) {
                    continue;
                }
                if (segment.equals("..")) {
                    if (validSegments.isEmpty()) {
                        triedToEscape = true;
                    } else {
                        validSegments.remove(validSegments.size() - 1);
                    }
                } else {
                    validSegments.add(segment);
                }
            }
            
            if (triedToEscape) {
                return false;
            }
            
            File rootDir = new File(rootDirectory);
            File targetFile = buildTargetFile(rootDir, validSegments);
            
            String canonicalRoot = rootDir.getCanonicalPath();
            String canonicalPath = targetFile.getCanonicalPath();
            
            return canonicalPath.startsWith(canonicalRoot + File.separator) || 
                   canonicalPath.equals(canonicalRoot);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 清理文件名，移除或替换不安全的字符
     * @param fileName 原始文件名
     * @return 清理后的文件名
     */
    public String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return fileName;
        }

        // 替换特殊字符，保留点号
        String sanitized = fileName.replaceAll("[<>:\"\\|?*]", "_");
        sanitized = sanitized.replaceAll("\\.{2,}", ".");
        sanitized = sanitized.replaceAll("^\\.+|\\.+$", "");

        if (sanitized.isEmpty()) {
            sanitized = "unnamed";
        }

        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }

        return sanitized;
    }

    /**
     * 规范化系统路径
     * @param path 原始路径
     * @return 规范化后的路径
     */
    private String normalizeSystemPath(String path) {
        if (path == null) {
            return "";
        }
        Path p = Paths.get(path);
        return p.normalize().toAbsolutePath().toString();
    }

    /**
     * 规范化路径分隔符
     * @param path 原始路径
     * @return 规范化后的路径
     */
    private String normalizePathSeparators(String path) {
        return path.replace("\\", "/");
    }

    /**
     * 移除重复的斜杠
     * @param path 原始路径
     * @return 处理后的路径
     */
    private String removeDuplicateSlashes(String path) {
        while (path.contains("//")) {
            path = path.replace("//", "/");
        }
        return path;
    }

    /**
     * 合并路径
     * @param currentDirectory 当前目录
     * @param relativePath 相对路径
     * @return 合并后的路径
     */
    private String combinePaths(String currentDirectory, String relativePath) {
        if (currentDirectory.equals("/")) {
            return "/" + relativePath;
        } else {
            return currentDirectory + "/" + relativePath;
        }
    }

    /**
     * 提取有效的路径段
     * @param path 路径字符串
     * @return 有效的路径段列表
     */
    private List<String> extractValidPathSegments(String path) {
        List<String> validSegments = new ArrayList<>();
        
        for (String segment : path.split("/")) {
            if (segment == null || segment.isEmpty() || segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                if (!validSegments.isEmpty()) {
                    validSegments.remove(validSegments.size() - 1);
                }
            } else {
                validSegments.add(segment);
            }
        }
        
        return validSegments;
    }

    /**
     * 构建目标文件
     * @param rootDir 根目录
     * @param segments 路径段
     * @return 目标文件
     */
    private File buildTargetFile(File rootDir, List<String> segments) {
        File targetFile = rootDir;
        
        for (String segment : segments) {
            targetFile = new File(targetFile, segment);
        }
        
        return targetFile;
    }

    /**
     * 检查路径是否在根目录内
     * @param rootPath 根目录路径
     * @param targetPath 目标路径
     * @return 是否在根目录内
     */
    private boolean isPathWithinRoot(String rootPath, String targetPath) {
        return targetPath.startsWith(rootPath + "/") || targetPath.equals(rootPath);
    }

    /**
     * 获取根目录
     * @return 根目录路径
     */
    public String getRootDirectory() {
        return rootDirectory;
    }
}
