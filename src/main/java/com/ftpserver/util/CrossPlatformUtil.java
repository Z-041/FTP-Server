package com.ftpserver.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class CrossPlatformUtil {
    
    public static final String CRLF = "\r\n";
    public static final String LF = "\n";
    
    public enum OSType {
        WINDOWS,
        LINUX,
        MACOS,
        UNKNOWN
    }
    
    private static OSType osType;
    
    static {
        detectOS();
    }
    
    private static void detectOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            osType = OSType.WINDOWS;
        } else if (os.contains("mac")) {
            osType = OSType.MACOS;
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            osType = OSType.LINUX;
        } else {
            osType = OSType.UNKNOWN;
        }
    }
    
    public static OSType getOSType() {
        return osType;
    }
    
    public static boolean isWindows() {
        return osType == OSType.WINDOWS;
    }
    
    public static boolean isLinux() {
        return osType == OSType.LINUX;
    }
    
    public static boolean isMacOS() {
        return osType == OSType.MACOS;
    }
    
    public static String getUnixPermissions(File file) {
        if (file.isDirectory()) {
            return "drwxr-xr-x";
        } else {
            return "-rw-r--r--";
        }
    }
    
    public static String getFileOwner(File file) {
        try {
            if (isWindows()) {
                return "ftp";
            }
            Path path = file.toPath();
            return Files.getOwner(path).getName();
        } catch (Exception e) {
            return "ftp";
        }
    }
    
    public static String getFileGroup(File file) {
        try {
            if (isWindows()) {
                return "ftp";
            }
            return "ftp";
        } catch (Exception e) {
            return "ftp";
        }
    }
    
    public static int getLinkCount(File file) {
        return 1;
    }
    
    public static String ensureCRLF(String text) {
        if (text == null) {
            return "";
        }
        return text.replace(LF, CRLF).replace("\r" + CRLF, CRLF);
    }
    
    public static String normalizeFtpPath(String path) {
        if (path == null) {
            return "/";
        }
        String normalized = path.replace("\\", "/");
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }
}
