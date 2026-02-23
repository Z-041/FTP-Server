package com.ftpserver;

import com.ftpserver.config.ServerConfig;
import com.ftpserver.server.FtpServer;
import com.ftpserver.user.User;
import com.ftpserver.user.UserManager;
import com.ftpserver.util.Logger;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class FtpServerTest {
    private static FtpServer ftpServer;
    private static ServerConfig config;
    private static UserManager userManager;
    private static final int TEST_PORT = 2122;
    private static final String TEST_USER = "testuser";
    private static final String TEST_PASSWORD = "testpass";
    
    @BeforeAll
    static void setUp() throws IOException {
        // Setup test configuration
        config = new ServerConfig();
        config.setPort(TEST_PORT);
        config.setRootDirectory("test_ftp_root");
        config.setMaxConnections(10);
        config.setEnablePassiveMode(true);
        config.setEnableActiveMode(true);
        
        // Create test root directory
        Path rootDir = Paths.get(config.getRootDirectory());
        Files.createDirectories(rootDir);
        
        // Setup user manager
        userManager = new UserManager("test_users.json");
        
        // Add test user
        User testUser = new User();
        testUser.setUsername(TEST_USER);
        testUser.setPassword(TEST_PASSWORD);
        testUser.setEnabled(true);
        testUser.addPermission(User.Permission.READ);
        testUser.addPermission(User.Permission.WRITE);
        testUser.addPermission(User.Permission.LIST);
        userManager.addUser(testUser);
        
        // Setup server
        ftpServer = new FtpServer(config, userManager);
        ftpServer.start();
        
        // Wait for server to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @AfterAll
    static void tearDown() {
        if (ftpServer != null && ftpServer.isRunning()) {
            ftpServer.stop();
        }
        
        // Clean up test files
        try {
            Files.deleteIfExists(Paths.get("test_users.json"));
            deleteDirectory(Paths.get("test_ftp_root"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        }
    }
    
    @Test
    @DisplayName("测试FTP服务器启动")
    void testServerStart() {
        assertTrue(ftpServer.isRunning(), "FTP服务器应该正在运行");
        assertEquals(TEST_PORT, config.getPort(), "端口应该正确设置");
    }
    
    @Test
    @DisplayName("测试基本FTP连接")
    void testBasicConnection() throws IOException {
        try (Socket socket = new Socket("localhost", TEST_PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            
            // Read welcome message
            String response = reader.readLine();
            assertNotNull(response, "应该收到欢迎消息");
            assertTrue(response.startsWith("220"), "欢迎消息应该以220开头");
            
            // Send USER command
            writer.println("USER " + TEST_USER);
            response = reader.readLine();
            assertNotNull(response, "应该收到USER响应");
            assertTrue(response.startsWith("331"), "USER响应应该以331开头");
            
            // Send PASS command
            writer.println("PASS " + TEST_PASSWORD);
            response = reader.readLine();
            assertNotNull(response, "应该收到PASS响应");
            assertTrue(response.startsWith("230"), "PASS响应应该以230开头");
            
            // Send QUIT command
            writer.println("QUIT");
            response = reader.readLine();
            assertNotNull(response, "应该收到QUIT响应");
            assertTrue(response.startsWith("221"), "QUIT响应应该以221开头");
        }
    }
    
    @Test
    @DisplayName("测试目录操作")
    void testDirectoryOperations() throws IOException {
        try (Socket socket = new Socket("localhost", TEST_PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            
            // Login
            reader.readLine(); // Welcome message
            writer.println("USER " + TEST_USER);
            reader.readLine(); // 331 response
            writer.println("PASS " + TEST_PASSWORD);
            String response = reader.readLine(); // 230 response
            assertTrue(response.startsWith("230"), "登录应该成功");
            
            // Test PWD command
            writer.println("PWD");
            response = reader.readLine();
            assertTrue(response.startsWith("257"), "PWD应该返回当前目录");
            
            // Test SYST command
            writer.println("SYST");
            response = reader.readLine();
            assertTrue(response.startsWith("215"), "SYST应该返回系统信息");
            
            // Test QUIT
            writer.println("QUIT");
            reader.readLine(); // 221 response
        }
    }
    
    @Test
    @DisplayName("测试用户管理")
    void testUserManagement() {
        // Test user exists
        assertTrue(userManager.getUser(TEST_USER).isPresent(), "测试用户应该存在");
        
        // Test user authentication
        assertTrue(userManager.authenticate(TEST_USER, TEST_PASSWORD).isPresent(), 
                  "应该能成功验证测试用户");
        
        // Test user permissions
        User user = userManager.getUser(TEST_USER).get();
        assertTrue(user.hasPermission(User.Permission.READ), "用户应该有读权限");
        assertTrue(user.hasPermission(User.Permission.WRITE), "用户应该有写权限");
        assertTrue(user.hasPermission(User.Permission.LIST), "用户应该有列表权限");
    }
    
    @Test
    @DisplayName("测试配置管理")
    void testConfiguration() {
        assertEquals(TEST_PORT, config.getPort(), "端口配置应该正确");
        assertEquals("test_ftp_root", config.getRootDirectory(), "根目录配置应该正确");
        assertEquals(10, config.getMaxConnections(), "最大连接数配置应该正确");
        assertTrue(config.isEnablePassiveMode(), "被动模式应该启用");
        assertTrue(config.isEnableActiveMode(), "主动模式应该启用");
    }
}