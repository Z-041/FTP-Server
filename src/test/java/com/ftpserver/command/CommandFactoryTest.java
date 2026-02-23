package com.ftpserver.command;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class CommandFactoryTest {

    @Test
    @DisplayName("测试获取支持的命令")
    void testGetSupportedCommand() {
        assertNotNull(CommandFactory.getCommand("USER"));
        assertNotNull(CommandFactory.getCommand("PASS"));
        assertNotNull(CommandFactory.getCommand("QUIT"));
        assertNotNull(CommandFactory.getCommand("SYST"));
        assertNotNull(CommandFactory.getCommand("FEAT"));
        assertNotNull(CommandFactory.getCommand("PWD"));
        assertNotNull(CommandFactory.getCommand("CWD"));
        assertNotNull(CommandFactory.getCommand("CDUP"));
        assertNotNull(CommandFactory.getCommand("MKD"));
        assertNotNull(CommandFactory.getCommand("RMD"));
        assertNotNull(CommandFactory.getCommand("DELE"));
        assertNotNull(CommandFactory.getCommand("RNFR"));
        assertNotNull(CommandFactory.getCommand("RNTO"));
        assertNotNull(CommandFactory.getCommand("LIST"));
        assertNotNull(CommandFactory.getCommand("NLST"));
        assertNotNull(CommandFactory.getCommand("MLSD"));
        assertNotNull(CommandFactory.getCommand("MLST"));
        assertNotNull(CommandFactory.getCommand("RETR"));
        assertNotNull(CommandFactory.getCommand("STOR"));
        assertNotNull(CommandFactory.getCommand("TYPE"));
        assertNotNull(CommandFactory.getCommand("PORT"));
        assertNotNull(CommandFactory.getCommand("PASV"));
        assertNotNull(CommandFactory.getCommand("EPSV"));
        assertNotNull(CommandFactory.getCommand("NOOP"));
        assertNotNull(CommandFactory.getCommand("OPTS"));
        assertNotNull(CommandFactory.getCommand("SITE"));
        assertNotNull(CommandFactory.getCommand("SIZE"));
        assertNotNull(CommandFactory.getCommand("MDTM"));
        assertNotNull(CommandFactory.getCommand("REST"));
        assertNotNull(CommandFactory.getCommand("ABOR"));
        assertNotNull(CommandFactory.getCommand("HELP"));
        assertNotNull(CommandFactory.getCommand("STAT"));
    }

    @Test
    @DisplayName("测试命令大小写不敏感")
    void testCommandCaseInsensitive() {
        FtpCommand cmd1 = CommandFactory.getCommand("user");
        FtpCommand cmd2 = CommandFactory.getCommand("USER");
        FtpCommand cmd3 = CommandFactory.getCommand("User");
        assertNotNull(cmd1);
        assertNotNull(cmd2);
        assertNotNull(cmd3);
        assertEquals(cmd1.getClass(), cmd2.getClass());
        assertEquals(cmd2.getClass(), cmd3.getClass());
    }

    @Test
    @DisplayName("测试不支持的命令")
    void testGetUnsupportedCommand() {
        assertNull(CommandFactory.getCommand("INVALID"));
        assertNull(CommandFactory.getCommand("UNKNOWN"));
    }

    @Test
    @DisplayName("测试检查命令是否支持")
    void testIsCommandSupported() {
        assertTrue(CommandFactory.isCommandSupported("USER"));
        assertTrue(CommandFactory.isCommandSupported("PASS"));
        assertTrue(CommandFactory.isCommandSupported("QUIT"));
        assertFalse(CommandFactory.isCommandSupported("INVALID"));
        assertFalse(CommandFactory.isCommandSupported(""));
    }

    @Test
    @DisplayName("测试命令的认证要求")
    void testCommandAuthenticationRequirement() {
        assertFalse(CommandFactory.getCommand("USER").requiresAuthentication());
        assertFalse(CommandFactory.getCommand("PASS").requiresAuthentication());
        assertFalse(CommandFactory.getCommand("QUIT").requiresAuthentication());
        assertFalse(CommandFactory.getCommand("SYST").requiresAuthentication());
        assertFalse(CommandFactory.getCommand("FEAT").requiresAuthentication());
        assertFalse(CommandFactory.getCommand("NOOP").requiresAuthentication());
        assertFalse(CommandFactory.getCommand("OPTS").requiresAuthentication());
        assertFalse(CommandFactory.getCommand("HELP").requiresAuthentication());
        
        assertTrue(CommandFactory.getCommand("PWD").requiresAuthentication());
        assertTrue(CommandFactory.getCommand("CWD").requiresAuthentication());
        assertTrue(CommandFactory.getCommand("CDUP").requiresAuthentication());
        assertTrue(CommandFactory.getCommand("MKD").requiresAuthentication());
        assertTrue(CommandFactory.getCommand("RMD").requiresAuthentication());
        assertTrue(CommandFactory.getCommand("DELE").requiresAuthentication());
        assertTrue(CommandFactory.getCommand("LIST").requiresAuthentication());
        assertTrue(CommandFactory.getCommand("RETR").requiresAuthentication());
        assertTrue(CommandFactory.getCommand("STOR").requiresAuthentication());
    }
}
