package com.ftpserver.util;

import org.junit.jupiter.api.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CrossPlatformUtilTest {

    @Test
    @DisplayName("测试OS类型检测")
    void testOSTypeDetection() {
        CrossPlatformUtil.OSType osType = CrossPlatformUtil.getOSType();
        assertNotNull(osType, "应该检测到操作系统类型");
    }

    @Test
    @DisplayName("测试CRLF确保")
    void testEnsureCRLF() {
        String result1 = CrossPlatformUtil.ensureCRLF("line1\nline2\n");
        assertTrue(result1.contains("line1"), "应该包含line1");
        assertTrue(result1.contains("line2"), "应该包含line2");
        assertTrue(result1.contains("\r\n"), "应该包含CRLF");
        
        String result2 = CrossPlatformUtil.ensureCRLF("line1\r\nline2\r\n");
        assertTrue(result2.contains("line1"), "应该包含line1");
        assertTrue(result2.contains("line2"), "应该包含line2");
        assertTrue(result2.contains("\r\n"), "应该包含CRLF");
        
        String result3 = CrossPlatformUtil.ensureCRLF("line1\rline2\r");
        assertTrue(result3.contains("line1"), "应该包含line1");
        assertTrue(result3.contains("line2"), "应该包含line2");
        
        assertEquals("", CrossPlatformUtil.ensureCRLF(null));
        assertEquals("", CrossPlatformUtil.ensureCRLF(""));
    }

    @Test
    @DisplayName("测试FTP路径规范化")
    void testNormalizeFtpPath() {
        assertEquals("/test/path", CrossPlatformUtil.normalizeFtpPath("\\test\\path"));
        assertEquals("/test/path", CrossPlatformUtil.normalizeFtpPath("//test//path"));
        assertEquals("/test/path", CrossPlatformUtil.normalizeFtpPath("test/path"));
        assertEquals("/", CrossPlatformUtil.normalizeFtpPath(null));
        assertEquals("/", CrossPlatformUtil.normalizeFtpPath(""));
    }

    @Test
    @DisplayName("测试Unix权限获取")
    void testGetUnixPermissions() throws IOException {
        Path tempFile = Files.createTempFile("test", ".txt");
        File file = tempFile.toFile();
        
        String perms = CrossPlatformUtil.getUnixPermissions(file);
        assertTrue(perms.startsWith("-"), "文件权限应该以-开头");
        
        Files.delete(tempFile);
        
        Path tempDir = Files.createTempDirectory("testdir");
        File dir = tempDir.toFile();
        
        String dirPerms = CrossPlatformUtil.getUnixPermissions(dir);
        assertTrue(dirPerms.startsWith("d"), "目录权限应该以d开头");
        
        Files.delete(tempDir);
    }

    @Test
    @DisplayName("测试文件所有者获取")
    void testGetFileOwner() throws IOException {
        Path tempFile = Files.createTempFile("test", ".txt");
        File file = tempFile.toFile();
        
        String owner = CrossPlatformUtil.getFileOwner(file);
        assertNotNull(owner, "文件所有者不应为null");
        
        Files.delete(tempFile);
    }

    @Test
    @DisplayName("测试文件组获取")
    void testGetFileGroup() throws IOException {
        Path tempFile = Files.createTempFile("test", ".txt");
        File file = tempFile.toFile();
        
        String group = CrossPlatformUtil.getFileGroup(file);
        assertNotNull(group, "文件组不应为null");
        
        Files.delete(tempFile);
    }

    @Test
    @DisplayName("测试链接计数获取")
    void testGetLinkCount() throws IOException {
        Path tempFile = Files.createTempFile("test", ".txt");
        File file = tempFile.toFile();
        
        int links = CrossPlatformUtil.getLinkCount(file);
        assertEquals(1, links, "链接计数应该为1");
        
        Files.delete(tempFile);
    }
}
