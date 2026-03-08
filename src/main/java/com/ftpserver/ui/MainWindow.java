package com.ftpserver.ui;

import com.ftpserver.config.ServerConfig;
import com.ftpserver.server.FtpServer;
import com.ftpserver.ui.content.*;
import com.ftpserver.ui.model.ClientRow;
import com.ftpserver.ui.model.UserRow;
import com.ftpserver.ui.sidebar.Sidebar;
import com.ftpserver.ui.tray.SystemTrayManager;
import com.ftpserver.user.User;
import com.ftpserver.user.UserManager;
import com.ftpserver.util.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class MainWindow extends JFrame implements FtpServer.ServerListener, Logger.LogListener {

    private static final String CONFIG_PATH = "config" + File.separator + "server.properties";
    private static final String USERS_PATH = "config" + File.separator + "users.json";

    private FtpServer ftpServer;
    private ServerConfig config;
    private UserManager userManager;
    private Logger logger;

    private JLabel statusIndicator;
    private JLabel statusText;
    private JButton startStopButton;

    private OverviewContent overviewContent;
    private ClientsContent clientsContent;
    private UsersContent usersContent;
    private ConfigContent configContent;
    private LogsContent logsContent;
    private Sidebar sidebar;
    private SystemTrayManager systemTrayManager;

    private JPanel contentPanel;
    private CardLayout cardLayout;
    private javax.swing.Timer updateTimer;
    private boolean logsContentInitialized = false;
    private boolean configContentInitialized = false;
    
    private int lastConnectionCount = -1;
    private int lastUserCount = -1;
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private java.util.List<FtpServer.ClientSession> lastSessions = new java.util.ArrayList<>();
    private int overviewLogLineCount = 0;
    private static final int MAX_OVERVIEW_LOG_LINES = 50;
    
    // 批量日志更新
    private final StringBuilder logBatchBuffer = new StringBuilder(4096);
    private javax.swing.Timer logBatchTimer;
    private static final int LOG_BATCH_INTERVAL = 100; // 100ms批量更新一次
    private final Object logBufferLock = new Object();
    
    // 用户数据缓存
    private java.util.List<User> lastUsers = new java.util.ArrayList<>();

    public MainWindow() {
        loadConfig();
        setupServer();
        initUI();
        initSystemTray();
        startUpdateTimer();
    }

    private void loadConfig() {
        config = new ServerConfig();
        logger = Logger.getInstance();
        try {
            config.load(CONFIG_PATH);
        } catch (IOException e) {
            logger.error("Failed to load config: " + e.getMessage(), "UI", "-");
        }
        userManager = new UserManager(USERS_PATH);
        logger.setLogDirectory(config.getLogDirectory());
        logger.addListener(this);

        // 设置GUI日志监听器
        setupGUILogListener();
        initLogBatchTimer();
    }

    private void initLogBatchTimer() {
        logBatchTimer = new javax.swing.Timer(LOG_BATCH_INTERVAL, e -> flushLogBatch());
        logBatchTimer.setRepeats(true);
        logBatchTimer.start();
    }

    private void flushLogBatch() {
        String batch;
        synchronized (logBufferLock) {
            if (logBatchBuffer.length() > 0) {
                batch = logBatchBuffer.toString();
                logBatchBuffer.setLength(0);
            } else {
                return;
            }
        }
        
        final String finalBatch = batch;
        SwingUtilities.invokeLater(() -> {
            try {
                // 批量更新日志页面
                logsContent.appendLog(finalBatch);

                // 批量更新概览页面的最近活动区域
                JTextArea overviewLogArea = overviewContent.getLogTextArea();
                overviewLogArea.append(finalBatch);
                overviewLogArea.setCaretPosition(overviewLogArea.getDocument().getLength());
                
                // 计算新增行数并限制显示
                int newLines = countNewLines(finalBatch);
                overviewLogLineCount += newLines;
                
                if (overviewLogLineCount > MAX_OVERVIEW_LOG_LINES) {
                    try {
                        int linesToRemove = overviewLogLineCount - MAX_OVERVIEW_LOG_LINES;
                        int end = overviewLogArea.getLineStartOffset(0);
                        int start = overviewLogArea.getLineStartOffset(linesToRemove);
                        overviewLogArea.replaceRange("", end, start);
                        overviewLogLineCount = MAX_OVERVIEW_LOG_LINES;
                    } catch (Exception ex) {
                        logger.error("Failed to trim log buffer: " + ex.getMessage(), "MainWindow", "-");
                    }
                }
            } catch (Exception ex) {
                logger.error("Error updating log display: " + ex.getMessage(), "MainWindow", "-");
            }
        });
    }

    private int countNewLines(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') count++;
        }
        return count;
    }

    private void setupGUILogListener() {
        logger.addListener(entry -> {
            String line = String.format("[%s] [%s] %s%n",
                    entry.timestamp.format(dateTimeFormatter), entry.level, entry.message);
            
            synchronized (logBufferLock) {
                logBatchBuffer.append(line);
            }
        });
    }

    private void setupServer() {
        ftpServer = new FtpServer(config, userManager);
        ftpServer.addListener(this);
    }

    private void startUpdateTimer() {
        updateTimer = new javax.swing.Timer(3000, e -> updateStats());
        updateTimer.setRepeats(true);
        updateTimer.start();
    }

    private void initUI() {
        setTitle("FTP Server Console");
        setSize(1200, 800);
        setMinimumSize(new Dimension(1000, 700));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(249, 250, 251));

        // 侧边栏
        sidebar = new Sidebar();
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(new Color(15, 23, 42));

        // 内容区域
        JPanel contentArea = createContentArea();

        mainPanel.add(sidebar, BorderLayout.WEST);
        mainPanel.add(contentArea, BorderLayout.CENTER);

        setContentPane(mainPanel);

        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (systemTrayManager != null && systemTrayManager.isTrayInitialized()) {
                    systemTrayManager.hideToTray();
                } else {
                    cleanup();
                    if (ftpServer.isRunning()) {
                        ftpServer.stop();
                    }
                    System.exit(0);
                }
            }
        });

        setupNavigation();
    }

    private JPanel createContentArea() {
        JPanel contentArea = new JPanel(new BorderLayout(0, 16));
        contentArea.setBackground(new Color(248, 250, 252));
        contentArea.setBorder(new EmptyBorder(0, 20, 20, 20));

        // 头部
        JPanel header = createHeader();
        contentArea.add(header, BorderLayout.NORTH);

        // 内容面板（使用 CardLayout）
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(new Color(249, 250, 251));

        overviewContent = new OverviewContent();
        clientsContent = new ClientsContent();
        usersContent = new UsersContent();
        configContent = new ConfigContent();
        logsContent = new LogsContent();

        contentPanel.add(overviewContent, "overview");
        contentPanel.add(clientsContent, "clients");
        contentPanel.add(usersContent, "users");
        contentPanel.add(configContent, "config");
        contentPanel.add(logsContent, "logs");

        contentArea.add(contentPanel, BorderLayout.CENTER);

        return contentArea;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(248, 250, 252));
        header.setBorder(new EmptyBorder(20, 0, 16, 0));

        JLabel headerTitle = new JLabel("控制台");
        headerTitle.setForeground(new Color(15, 23, 42));

        // 状态和控制按钮面板
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(new Color(248, 250, 252));

        // 状态指示器
        JPanel statusBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        statusBadge.setBackground(new Color(241, 245, 249));
        statusBadge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240)),
            new EmptyBorder(6, 14, 6, 14)
        ));

        statusIndicator = new JLabel("●");
        statusIndicator.setForeground(new Color(239, 68, 68));

        statusText = new JLabel("服务器离线");
        statusText.setForeground(new Color(71, 85, 105));

        statusBadge.add(statusIndicator);
        statusBadge.add(statusText);

        // 启动/停止按钮
        startStopButton = new JButton("启动服务器");
        startStopButton.setBackground(new Color(37, 99, 235));
        startStopButton.setForeground(Color.WHITE);
        startStopButton.setFocusPainted(false);
        startStopButton.setBorder(new EmptyBorder(8, 18, 8, 18));
        startStopButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        startStopButton.addActionListener(e -> toggleServer());

        rightPanel.add(statusBadge);
        rightPanel.add(startStopButton);

        header.add(headerTitle, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    private void setupNavigation() {
        sidebar.getNavOverview().addActionListener(e -> {
            sidebar.setActiveNav("overview");
            cardLayout.show(contentPanel, "overview");
        });
        sidebar.getNavClients().addActionListener(e -> {
            sidebar.setActiveNav("clients");
            cardLayout.show(contentPanel, "clients");
        });
        sidebar.getNavUsers().addActionListener(e -> {
            sidebar.setActiveNav("users");
            cardLayout.show(contentPanel, "users");
            setupUsersContent();
        });
        sidebar.getNavConfig().addActionListener(e -> {
            sidebar.setActiveNav("config");
            cardLayout.show(contentPanel, "config");
            setupConfigContent();
        });
        sidebar.getNavLogs().addActionListener(e -> {
            sidebar.setActiveNav("logs");
            cardLayout.show(contentPanel, "logs");
            setupLogsContent();
        });
    }

    private void setupUsersContent() {
        usersContent.getAddBtn().addActionListener(e -> showAddUserDialog());
        usersContent.getEditBtn().addActionListener(e -> {
            UserRow selected = usersContent.getSelectedUser();
            if (selected != null) {
                showEditUserDialog(selected.username);
            }
        });
        usersContent.getDeleteBtn().addActionListener(e -> {
            UserRow selected = usersContent.getSelectedUser();
            if (selected != null) {
                deleteUser(selected.username);
            }
        });
        refreshUsersTable();
    }

    private void setupConfigContent() {
        // 懒加载：只在配置值变化时更新UI
        String currentPort = String.valueOf(config.getPort());
        String currentRootDir = config.getRootDirectory();
        String currentMaxConn = String.valueOf(config.getMaxConnections());
        
        if (!currentPort.equals(configContent.getPortField().getText())) {
            configContent.getPortField().setText(currentPort);
        }
        if (!currentRootDir.equals(configContent.getRootDirField().getText())) {
            configContent.getRootDirField().setText(currentRootDir);
        }
        if (!currentMaxConn.equals(configContent.getMaxConnField().getText())) {
            configContent.getMaxConnField().setText(currentMaxConn);
        }

        if (configContentInitialized) {
            return;
        }
        configContentInitialized = true;

        configContent.getBrowseBtn().addActionListener(e -> {
            JFileChooser dirChooser = new JFileChooser();
            dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            dirChooser.setDialogTitle("Select Root Directory");
            if (dirChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                configContent.getRootDirField().setText(dirChooser.getSelectedFile().getAbsolutePath());
            }
        });

        configContent.getSaveBtn().addActionListener(e -> {
            try {
                config.setPort(Integer.parseInt(configContent.getPortField().getText()));
                config.setRootDirectory(configContent.getRootDirField().getText());
                config.setMaxConnections(Integer.parseInt(configContent.getMaxConnField().getText()));
                config.save(CONFIG_PATH);
                updateStats();
                showSuccessMessage("Configuration saved successfully!");
            } catch (Exception ex) {
                showErrorMessage("Failed to save: " + ex.getMessage());
            }
        });
    }

    private void setupLogsContent() {
        if (logsContentInitialized) {
            return;
        }
        logsContentInitialized = true;

        logsContent.getClearBtn().addActionListener(e -> {
            logger.clearLogs();
            logsContent.clearLogs();
            overviewContent.getLogTextArea().setText("");
        });
    }

    private void toggleServer() {
        try {
            if (ftpServer.isRunning()) {
                ftpServer.stop();
            } else {
                ftpServer.start();
            }
        } catch (IOException e) {
            showErrorMessage(e.getMessage());
        }
    }

    private void updateStats() {
        if (ftpServer != null && ftpServer.isRunning()) {
            ftpServer.checkIdleClients();
        }
        
        int connCount = ftpServer != null ? ftpServer.getConnectionCount() : 0;
        int userCount = userManager.getAllUsers().size();
        
        boolean needsUpdate = connCount != lastConnectionCount || userCount != lastUserCount;
        
        if (needsUpdate) {
            lastConnectionCount = connCount;
            lastUserCount = userCount;
            
            SwingUtilities.invokeLater(() -> {
                overviewContent.getStatPortValue().setText(String.valueOf(config.getPort()));
                overviewContent.getStatConnectionsValue().setText(String.valueOf(connCount));
                overviewContent.getStatMaxConnectionsValue().setText(String.valueOf(config.getMaxConnections()));
                overviewContent.getStatUsersValue().setText(String.valueOf(userCount));
            });
        }
        
        refreshClientsTable();
    }

    private void refreshClientsTable() {
        java.util.List<FtpServer.ClientSession> currentSessions = ftpServer != null ? 
            new java.util.ArrayList<>(ftpServer.getClientSessions()) : new java.util.ArrayList<>();
        
        boolean sessionsChanged = currentSessions.size() != lastSessions.size();
        if (!sessionsChanged) {
            for (int i = 0; i < currentSessions.size(); i++) {
                FtpServer.ClientSession current = currentSessions.get(i);
                FtpServer.ClientSession last = lastSessions.get(i);
                if (!current.getClientAddress().equals(last.getClientAddress()) ||
                    current.getClientPort() != last.getClientPort() ||
                    current.active != last.active) {
                    sessionsChanged = true;
                    break;
                }
            }
        }
        
        if (sessionsChanged) {
            lastSessions = currentSessions;
            SwingUtilities.invokeLater(() -> {
                clientsContent.clearData();
                for (FtpServer.ClientSession session : currentSessions) {
                    clientsContent.addClient(new ClientRow(
                            session.getClientAddress(),
                            String.valueOf(session.getClientPort()),
                            session.connectTime.format(dateTimeFormatter),
                            session.active ? "Active" : "Idle"
                    ));
                }
            });
        }
    }

    private void refreshUsersTable() {
        java.util.List<User> currentUsers = new java.util.ArrayList<>(userManager.getAllUsers());
        
        // 检查用户数据是否变化
        boolean usersChanged = currentUsers.size() != lastUsers.size();
        if (!usersChanged) {
            for (int i = 0; i < currentUsers.size(); i++) {
                User current = currentUsers.get(i);
                User last = lastUsers.get(i);
                if (!current.getUsername().equals(last.getUsername()) ||
                    !java.util.Objects.equals(current.getHomeDirectory(), last.getHomeDirectory()) ||
                    current.isEnabled() != last.isEnabled() ||
                    !current.getPermissions().equals(last.getPermissions())) {
                    usersChanged = true;
                    break;
                }
            }
        }
        
        if (usersChanged) {
            lastUsers = currentUsers;
            SwingUtilities.invokeLater(() -> {
                usersContent.clearData();
                for (User user : currentUsers) {
                    usersContent.addUser(new UserRow(
                            user.getUsername(),
                            user.getHomeDirectory() != null ? user.getHomeDirectory() : "Default",
                            user.isEnabled() ? "Yes" : "No",
                            user.getPermissions().toString()
                    ));
                }
            });
        }
    }

    private void showAddUserDialog() {
        JTextField userField = new JTextField(20);
        JPasswordField passField = new JPasswordField(20);
        JTextField homeField = new JTextField(20);
        JCheckBox enabledBox = new JCheckBox("启用", true);

        // 权限复选框
        JCheckBox readBox = new JCheckBox("读取", true);
        JCheckBox writeBox = new JCheckBox("写入", true);
        JCheckBox deleteBox = new JCheckBox("删除", true);
        JCheckBox createDirBox = new JCheckBox("创建目录", true);
        JCheckBox deleteDirBox = new JCheckBox("删除目录", true);
        JCheckBox renameBox = new JCheckBox("重命名", true);
        JCheckBox listBox = new JCheckBox("列表", true);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        
        JPanel basicPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        basicPanel.add(new JLabel("用户名:"));
        basicPanel.add(userField);
        basicPanel.add(new JLabel("密码:"));
        basicPanel.add(passField);
        basicPanel.add(new JLabel("主目录 (可选):"));
        basicPanel.add(homeField);
        basicPanel.add(new JLabel("启用状态:"));
        basicPanel.add(enabledBox);
        
        JPanel permPanel = new JPanel(new GridLayout(2, 4, 5, 5));
        permPanel.setBorder(BorderFactory.createTitledBorder("权限设置"));
        permPanel.add(readBox);
        permPanel.add(writeBox);
        permPanel.add(deleteBox);
        permPanel.add(createDirBox);
        permPanel.add(deleteDirBox);
        permPanel.add(renameBox);
        permPanel.add(listBox);
        
        panel.add(basicPanel, BorderLayout.NORTH);
        panel.add(permPanel, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, panel, "添加用户",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            User user = new User();
            user.setUsername(userField.getText());
            user.setPassword(new String(passField.getPassword()));
            user.setHomeDirectory(homeField.getText().isEmpty() ? null : homeField.getText());
            user.setEnabled(enabledBox.isSelected());
            
            if (readBox.isSelected()) user.addPermission(User.Permission.READ);
            if (writeBox.isSelected()) user.addPermission(User.Permission.WRITE);
            if (deleteBox.isSelected()) user.addPermission(User.Permission.DELETE);
            if (createDirBox.isSelected()) user.addPermission(User.Permission.CREATE_DIR);
            if (deleteDirBox.isSelected()) user.addPermission(User.Permission.DELETE_DIR);
            if (renameBox.isSelected()) user.addPermission(User.Permission.RENAME);
            if (listBox.isSelected()) user.addPermission(User.Permission.LIST);
            
            userManager.addUser(user);
            refreshUsersTable();
            updateStats();
        }
    }

    private void showEditUserDialog(String username) {
        User user = userManager.getUser(username).orElse(null);
        if (user == null) return;

        JTextField userField = new JTextField(user.getUsername(), 20);
        userField.setEditable(false);
        JPasswordField passField = new JPasswordField(user.getPassword(), 20);
        JTextField homeField = new JTextField(
            user.getHomeDirectory() != null ? user.getHomeDirectory() : "", 20);
        JCheckBox enabledBox = new JCheckBox("启用", user.isEnabled());

        // 权限复选框 - 根据用户当前权限设置选中状态
        JCheckBox readBox = new JCheckBox("读取", user.hasPermission(User.Permission.READ));
        JCheckBox writeBox = new JCheckBox("写入", user.hasPermission(User.Permission.WRITE));
        JCheckBox deleteBox = new JCheckBox("删除", user.hasPermission(User.Permission.DELETE));
        JCheckBox createDirBox = new JCheckBox("创建目录", user.hasPermission(User.Permission.CREATE_DIR));
        JCheckBox deleteDirBox = new JCheckBox("删除目录", user.hasPermission(User.Permission.DELETE_DIR));
        JCheckBox renameBox = new JCheckBox("重命名", user.hasPermission(User.Permission.RENAME));
        JCheckBox listBox = new JCheckBox("列表", user.hasPermission(User.Permission.LIST));

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        
        JPanel basicPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        basicPanel.add(new JLabel("用户名:"));
        basicPanel.add(userField);
        basicPanel.add(new JLabel("密码:"));
        basicPanel.add(passField);
        basicPanel.add(new JLabel("主目录:"));
        basicPanel.add(homeField);
        basicPanel.add(new JLabel("启用状态:"));
        basicPanel.add(enabledBox);
        
        JPanel permPanel = new JPanel(new GridLayout(2, 4, 5, 5));
        permPanel.setBorder(BorderFactory.createTitledBorder("权限设置"));
        permPanel.add(readBox);
        permPanel.add(writeBox);
        permPanel.add(deleteBox);
        permPanel.add(createDirBox);
        permPanel.add(deleteDirBox);
        permPanel.add(renameBox);
        permPanel.add(listBox);
        
        panel.add(basicPanel, BorderLayout.NORTH);
        panel.add(permPanel, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, panel, "编辑用户",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            user.setPassword(new String(passField.getPassword()));
            user.setHomeDirectory(homeField.getText().isEmpty() ? null : homeField.getText());
            user.setEnabled(enabledBox.isSelected());
            
            // 更新权限
            if (readBox.isSelected()) user.addPermission(User.Permission.READ);
            else user.removePermission(User.Permission.READ);
            if (writeBox.isSelected()) user.addPermission(User.Permission.WRITE);
            else user.removePermission(User.Permission.WRITE);
            if (deleteBox.isSelected()) user.addPermission(User.Permission.DELETE);
            else user.removePermission(User.Permission.DELETE);
            if (createDirBox.isSelected()) user.addPermission(User.Permission.CREATE_DIR);
            else user.removePermission(User.Permission.CREATE_DIR);
            if (deleteDirBox.isSelected()) user.addPermission(User.Permission.DELETE_DIR);
            else user.removePermission(User.Permission.DELETE_DIR);
            if (renameBox.isSelected()) user.addPermission(User.Permission.RENAME);
            else user.removePermission(User.Permission.RENAME);
            if (listBox.isSelected()) user.addPermission(User.Permission.LIST);
            else user.removePermission(User.Permission.LIST);
            
            refreshUsersTable();
        }
    }

    private void deleteUser(String username) {
        int result = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete user '" + username + "'?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            userManager.removeUser(username);
            refreshUsersTable();
            updateStats();
        }
    }

    private void showSuccessMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void initSystemTray() {
        systemTrayManager = new SystemTrayManager(this, ftpServer, logger);
        systemTrayManager.initSystemTray();
    }

    private void cleanup() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
        if (logBatchTimer != null) {
            logBatchTimer.stop();
        }
        if (logger != null) {
            logger.removeListener(this);
        }
    }

    @Override
    public void onServerStarted() {
        SwingUtilities.invokeLater(() -> {
            statusIndicator.setForeground(new Color(34, 197, 94));
            statusText.setText("运行中 - 端口 " + config.getPort());
            startStopButton.setText("停止服务器");
            startStopButton.setBackground(new Color(239, 68, 68));
        });
        if (systemTrayManager != null) {
            systemTrayManager.updateTrayServerStatus(true);
        }
    }

    @Override
    public void onServerStopped() {
        SwingUtilities.invokeLater(() -> {
            statusIndicator.setForeground(new Color(239, 68, 68));
            statusText.setText("服务器离线");
            startStopButton.setText("启动服务器");
            startStopButton.setBackground(new Color(37, 99, 235));
        });
        if (systemTrayManager != null) {
            systemTrayManager.updateTrayServerStatus(false);
        }
    }

    @Override
    public void onClientConnected(FtpServer.ClientSession session) {
        refreshClientsTable();
    }

    @Override
    public void onClientDisconnected(FtpServer.ClientSession session) {
        refreshClientsTable();
    }

    @Override
    public void onLogEntry(Logger.LogEntry entry) {
        // 日志通过 listener 直接处理
    }
}
