package com.ftpserver.user;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserManagerSecurityTest {
    private UserManager userManager;
    private static final String TEST_USERS_FILE = "test_security_users.json";

    @BeforeEach
    void setUp() {
        userManager = new UserManager(TEST_USERS_FILE);
    }

    @AfterEach
    void tearDown() {
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(TEST_USERS_FILE));
        } catch (IOException e) {
            // ignore
        }
    }

    @Test
    @DisplayName("测试弱密码拒绝")
    void testWeakPasswordRejection() {
        assertFalse(userManager.validatePassword(null), "空密码应该被拒绝");
        assertFalse(userManager.validatePassword(""), "空字符串密码应该被拒绝");
        assertFalse(userManager.validatePassword("1234567"), "少于8位的密码应该被拒绝");
        assertFalse(userManager.validatePassword("password"), "无大写字母的密码应该被拒绝");
        assertFalse(userManager.validatePassword("PASSWORD"), "无小写字母的密码应该被拒绝");
        assertFalse(userManager.validatePassword("Password"), "无数字的密码应该被拒绝");
    }

    @Test
    @DisplayName("测试强密码接受")
    void testStrongPasswordAcceptance() {
        assertTrue(userManager.validatePassword("Password1"), "强密码应该被接受");
        assertTrue(userManager.validatePassword("MyP@ssw0rd"), "包含特殊字符的强密码应该被接受");
        assertTrue(userManager.validatePassword("SecurePass123"), "包含数字的密码应该被接受");
    }

    @Test
    @DisplayName("测试密码哈希升级")
    void testPasswordHashUpgrade() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("TestPassword123");
        user.setEnabled(true);
        
        userManager.addUser(user);
        
        Optional<User> retrievedUser = userManager.getUser("testuser");
        assertTrue(retrievedUser.isPresent());
        String password = retrievedUser.get().getPassword();
        assertNotNull(password);
        assertTrue(password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"), 
                  "密码应该使用BCrypt哈希");
    }

    @Test
    @DisplayName("测试旧密码格式兼容性")
    void testLegacyPasswordCompatibility() {
        User user = new User();
        user.setUsername("legacyuser");
        user.setPassword("SHA256SALT:1234567890abcdef:abc123def456");
        user.setEnabled(true);
        
        userManager.addUser(user);
        
        Optional<User> retrievedUser = userManager.getUser("legacyuser");
        assertTrue(retrievedUser.isPresent());
    }

    @Test
    @DisplayName("测试认证失败")
    void testAuthenticationFailure() {
        assertFalse(userManager.authenticate("nonexistent", "password").isPresent());
        assertFalse(userManager.authenticate("test", null).isPresent());
        assertFalse(userManager.authenticate(null, "password").isPresent());
    }

    @Test
    @DisplayName("测试密码验证")
    void testPasswordVerification() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("TestPassword123");
        user.setEnabled(true);
        
        userManager.addUser(user);
        
        assertTrue(userManager.authenticate("testuser", "TestPassword123").isPresent());
        assertFalse(userManager.authenticate("testuser", "WrongPassword").isPresent());
    }
}
