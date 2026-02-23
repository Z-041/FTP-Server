package com.ftpserver.session;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathResolver {
    private String rootDirectory;

    public PathResolver(String rootDirectory) {
        this.rootDirectory = normalizeSystemPath(rootDirectory);
        ensureRootDirectory();
    }

    private void ensureRootDirectory() {
        File rootDir = new File(rootDirectory);
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
    }

    public void setRootDirectory(String rootDirectory) {
        this.rootDirectory = normalizeSystemPath(rootDirectory);
        ensureRootDirectory();
    }

    public String resolvePath(String currentDirectory, String path) {
        if (path == null || path.isEmpty()) {
            return currentDirectory;
        }
        
        try {
            String result;
            
            String normalizedPath = path.replace("\\", "/");
            
            if (normalizedPath.startsWith("/")) {
                result = normalizedPath;
            } else {
                if (currentDirectory.equals("/")) {
                    result = "/" + normalizedPath;
                } else {
                    result = currentDirectory + "/" + normalizedPath;
                }
            }
            
            while (result.contains("//")) {
                result = result.replace("//", "/");
            }
            
            result = resolvePathSegments(result);
            
            return result;
        } catch (Exception e) {
            return currentDirectory;
        }
    }

    private String resolvePathSegments(String path) {
        String[] segments = path.split("/");
        java.util.List<String> result = new java.util.ArrayList<>();
        
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

    public String getRealPath(String ftpPath) {
        try {
            String normalizedFtpPath = ftpPath.replace("\\", "/");
            
            String normalized = normalizedFtpPath;
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            
            while (normalized.contains("//")) {
                normalized = normalized.replace("//", "/");
            }
            
            String[] segments = normalized.split("/");
            java.util.List<String> validSegments = new java.util.ArrayList<>();
            
            for (String segment : segments) {
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
            
            File rootDir = new File(rootDirectory);
            File targetFile = rootDir;
            
            for (String segment : validSegments) {
                targetFile = new File(targetFile, segment);
            }
            
            String absoluteRoot = rootDir.getAbsolutePath();
            String absoluteTarget = targetFile.getAbsolutePath();
            
            String normalizedRoot = absoluteRoot.replace("\\", "/");
            String normalizedTarget = absoluteTarget.replace("\\", "/");
            
            if (!normalizedTarget.startsWith(normalizedRoot + "/") && 
                !normalizedTarget.equals(normalizedRoot)) {
                return rootDirectory;
            }
            
            return absoluteTarget;
        } catch (Exception e) {
            return rootDirectory;
        }
    }

    private String normalizeSystemPath(String path) {
        if (path == null) {
            return "";
        }
        Path p = Paths.get(path);
        return p.normalize().toAbsolutePath().toString();
    }

    public String getRootDirectory() {
        return rootDirectory;
    }
}
