package com.ftpserver.session;

import org.junit.jupiter.api.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class PathResolverSecurityTest {
    private static final String TEST_ROOT = "test_security_root";
    private PathResolver pathResolver;

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(Paths.get(TEST_ROOT));
        pathResolver = new PathResolver(TEST_ROOT);
    }

    @AfterEach
    void tearDown() throws Exception {
        deleteDirectory(Paths.get(TEST_ROOT));
    }

    private void deleteDirectory(Path path) throws Exception {
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (Exception e) {
                            // ignore
                        }
                    });
        }
    }

    @Test
    @DisplayName("测试路径遍历攻击防护")
    void testPathTraversalAttack() throws Exception {
        String[] maliciousPaths = {
                "../../../etc/passwd",
                "..\\..\\..\\windows\\system32",
                "/../../../etc/shadow",
                "test/../../../tmp",
                "./.././../secret"
        };

        for (String path : maliciousPaths) {
            String realPath = pathResolver.getRealPath(path);
            String canonicalRoot = new File(TEST_ROOT).getCanonicalPath();
            String canonicalPath = new File(realPath).getCanonicalPath();

            assertTrue(canonicalPath.startsWith(canonicalRoot + File.separator) ||
                    canonicalPath.equals(canonicalRoot),
                    "路径 " + path + " 应该被阻止访问根目录外的文件");
        }
    }

    @Test
    @DisplayName("测试符号链接攻击防护")
    void testSymlinkAttack() throws Exception {
        Path targetDir = Files.createTempDirectory("target");
        Path symlinkPath = Paths.get(TEST_ROOT).resolve("symlink");

        try {
            Files.createSymbolicLink(symlinkPath, targetDir);

            String realPath = pathResolver.getRealPath("/symlink/test.txt");
            String canonicalRoot = new File(TEST_ROOT).getCanonicalPath();
            String canonicalPath = new File(realPath).getCanonicalPath();

            assertTrue(canonicalPath.startsWith(canonicalRoot + File.separator) ||
                    canonicalPath.equals(canonicalRoot),
                    "符号链接应该被正确处理");
        } finally {
            Files.deleteIfExists(symlinkPath);
            deleteDirectory(targetDir);
        }
    }

    @Test
    @DisplayName("测试空路径处理")
    void testEmptyPath() {
        assertFalse(pathResolver.isPathSafe(null));
        assertFalse(pathResolver.isPathSafe(""));
    }

    @Test
    @DisplayName("测试路径规范化")
    void testPathNormalization() {
        String path = "/test/../test2/./file.txt";
        String resolved = pathResolver.resolvePath("/", path);
        assertEquals("/test2/file.txt", resolved);
    }

    @Test
    @DisplayName("测试路径安全验证")
    void testPathSafetyValidation() throws Exception {
        assertTrue(pathResolver.isPathSafe("/test.txt"));

        String[] unsafePaths = {
                "../../../etc/passwd",
                "..\\..\\..\\windows",
                "/../../secret"
        };

        for (String path : unsafePaths) {
            assertFalse(pathResolver.isPathSafe(path), "路径 " + path + " 应该被标记为不安全");
        }
    }

    @Test
    @DisplayName("测试文件名清理")
    void testFileNameSanitization() {
        String[] invalidFilenames = {
                "test<file>.txt",
                "file:with\"quotes.txt",
                "file|with?asterisk.txt",
                "file*with*stars.txt",
                "file...with...dots.txt"
        };

        for (String filename : invalidFilenames) {
            String sanitized = pathResolver.sanitizeFileName(filename);
            assertNotEquals(filename, sanitized, "文件名应该被清理");
            assertTrue(sanitized.length() <= 255, "文件名长度应该被限制");
        }
    }

    @Test
    @DisplayName("测试长文件名限制")
    void testLongFilenameLimit() {
        String longName = "a".repeat(300);
        String sanitized = pathResolver.sanitizeFileName(longName);
        assertEquals(255, sanitized.length(), "文件名应该被截断到255字符");
    }

    @Test
    @DisplayName("测试路径边界条件")
    void testPathEdgeCases() {
        assertEquals("/", pathResolver.resolvePath("/", ".."));
        assertEquals("/test", pathResolver.resolvePath("/test", "."));
        assertEquals("/test/test", pathResolver.resolvePath("/test", "./test"));
    }

    @Test
    @DisplayName("测试Windows路径分隔符")
    void testWindowsPathSeparators() throws Exception {
        String path = "..\\..\\..\\windows\\system32";
        String realPath = pathResolver.getRealPath(path);
        String canonicalRoot = new File(TEST_ROOT).getCanonicalPath();
        String canonicalPath = new File(realPath).getCanonicalPath();

        assertTrue(canonicalPath.startsWith(canonicalRoot + File.separator) ||
                canonicalPath.equals(canonicalRoot),
                "Windows路径分隔符应该被正确处理");
    }

    @Test
    @DisplayName("测试混合路径分隔符")
    void testMixedPathSeparators() {
        String path = "test\\../test2/./file.txt";
        String resolved = pathResolver.resolvePath("/", path);
        assertEquals("/test2/file.txt", resolved);
    }
}
