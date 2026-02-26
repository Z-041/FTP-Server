package com.ftpserver.session;

import org.junit.jupiter.api.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class PathResolverTest {
    private static final String TEST_ROOT = "test_path_root";
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
    @DisplayName("测试根目录创建")
    void testRootDirectoryCreation() {
        File rootDir = new File(TEST_ROOT);
        assertTrue(rootDir.exists(), "根目录应该存在");
        assertTrue(rootDir.isDirectory(), "根目录应该是目录");
    }

    @Test
    @DisplayName("测试相对路径解析")
    void testResolveRelativePath() {
        assertEquals("/test", pathResolver.resolvePath("/", "test"));
        assertEquals("/dir/test", pathResolver.resolvePath("/dir", "test"));
    }

    @Test
    @DisplayName("测试Windows反斜杠路径")
    void testWindowsBackslashPath() {
        assertEquals("/test/path", pathResolver.resolvePath("/", "test\\path"));
        assertEquals("/dir/test/path", pathResolver.resolvePath("/dir", "test\\path"));
    }

    @Test
    @DisplayName("测试路径段解析（.和..）")
    void testResolvePathSegments() {
        assertEquals("/test", pathResolver.resolvePath("/", "./test"));
        assertEquals("/", pathResolver.resolvePath("/test", ".."));
        assertEquals("/test2", pathResolver.resolvePath("/test1", "../test2"));
        assertEquals("/", pathResolver.resolvePath("/", ".."));
        assertEquals("/dir1/dir3", pathResolver.resolvePath("/dir1/dir2", "../dir3"));
    }

    @Test
    @DisplayName("测试绝对路径解析")
    void testResolveAbsolutePath() {
        assertEquals("/absolute/path", pathResolver.resolvePath("/current", "/absolute/path"));
    }

    @Test
    @DisplayName("测试空路径解析")
    void testResolveEmptyPath() {
        assertEquals("/", pathResolver.resolvePath("/", ""));
        assertEquals("/current", pathResolver.resolvePath("/current", null));
    }

    @Test
    @DisplayName("测试双斜杠处理")
    void testResolveDoubleSlash() {
        assertEquals("/path/to/dir", pathResolver.resolvePath("/", "path//to//dir"));
    }

    @Test
    @DisplayName("测试真实路径转换")
    void testGetRealPath() {
        String realPath = pathResolver.getRealPath("/test/file.txt");
        assertTrue(realPath.contains(TEST_ROOT), "真实路径应该包含根目录");
        assertTrue(realPath.endsWith("test" + File.separator + "file.txt"), "路径格式应该正确");
    }

    @Test
    @DisplayName("测试父目录路径处理")
    void testGetRealPathWithParent() {
        String realPath = pathResolver.getRealPath("/../test/../file.txt");
        assertTrue(realPath.contains(TEST_ROOT), "真实路径应该包含根目录");
    }

    @Test
    @DisplayName("测试设置新的根目录")
    void testSetRootDirectory() throws Exception {
        String newRoot = "new_test_root";
        Path newRootPath = Paths.get(newRoot).normalize().toAbsolutePath();
        Files.createDirectories(newRootPath);

        pathResolver.setRootDirectory(newRoot);
        assertEquals(newRootPath.toString(), pathResolver.getRootDirectory(), "根目录应该已更新");

        String realPath = pathResolver.getRealPath("/test.txt");
        assertTrue(realPath.contains(newRoot), "真实路径应该包含新根目录");

        deleteDirectory(newRootPath);
    }
}
