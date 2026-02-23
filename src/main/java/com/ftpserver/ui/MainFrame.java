package com.ftpserver.ui;

import com.ftpserver.config.ServerConfig;
import com.ftpserver.server.FtpServer;
import com.ftpserver.user.User;
import com.ftpserver.user.UserManager;
import com.ftpserver.util.Logger;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainFrame extends JFrame implements FtpServer.ServerListener, Logger.LogListener {
    private static final String CONFIG_PATH = "config" + File.separator + "server.properties";
    private static final String USERS_PATH = "config" + File.separator + "users.json";

    private static final Color PRIMARY_COLOR = new Color(33, 150, 243);
    private static final Color PRIMARY_DARK = new Color(25, 118, 210);
    private static final Color ACCENT_COLOR = new Color(0, 188, 212);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color ERROR_COLOR = new Color(244, 67, 54);
    private static final Color WARNING_COLOR = new Color(255, 193, 7);
    private static final Color BACKGROUND_COLOR = new Color(245, 247, 250);
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(33, 33, 33);
    private static final Color TEXT_SECONDARY = new Color(117, 117, 117);

    private FtpServer ftpServer;
    private ServerConfig config;
    private UserManager userManager;
    private Logger logger;

    private JLabel statusLabel;
    private JLabel statusIndicator;
    private JButton startStopButton;
    private JTabbedPane tabbedPane;
    private DefaultTableModel clientsTableModel;
    private DefaultTableModel usersTableModel;
    private DefaultTableModel logsTableModel;
    private JTextField portField;
    private JTextField rootDirField;
    private JTextField maxConnectionsField;
    
    // 概览面板统计卡片引用
    private JPanel portCard;
    private JPanel connectionCard;
    private JPanel maxConnectionsCard;
    private JPanel usersCard;
    
    // 定时更新器
    private javax.swing.Timer updateTimer;

    public MainFrame() {
        setTitle("FTP服务器管理器");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setMinimumSize(new Dimension(1000, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND_COLOR);

        loadConfig();
        initComponents();
        updateConfigFields();
        updateUsersTable();
        setupServer();
        initializeUpdateTimer(); // 初始化定时更新器

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
                if (ftpServer.isRunning()) {
                    ftpServer.stop();
                }
            }
        });
    }

    private void initComponents() {
        JPanel headerPanel = createHeaderPanel();
        tabbedPane = createTabbedPane();

        setLayout(new BorderLayout(0, 0));
        add(headerPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));
        panel.setBackground(PRIMARY_COLOR);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setOpaque(false);

        JLabel titleLabel = new JLabel("FTP服务器管理器");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        statusPanel.setOpaque(false);

        statusIndicator = new JLabel("●");
        statusIndicator.setFont(new Font("微软雅黑", Font.BOLD, 20));
        statusIndicator.setForeground(ERROR_COLOR);

        statusLabel = new JLabel("已停止");
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        statusLabel.setForeground(Color.WHITE);

        statusPanel.add(statusIndicator);
        statusPanel.add(statusLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);

        startStopButton = createPrimaryButton("启动服务器");
        startStopButton.setPreferredSize(new Dimension(160, 44));
        startStopButton.addActionListener(e -> toggleServer());
        buttonPanel.add(startStopButton);

        panel.add(titlePanel, BorderLayout.WEST);
        panel.add(statusPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    private JTabbedPane createTabbedPane() {
        JTabbedPane pane = new JTabbedPane(JTabbedPane.TOP);
        pane.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        pane.setBackground(BACKGROUND_COLOR);
        pane.setBorder(new EmptyBorder(16, 16, 16, 16));

        pane.addTab("概览", createOverviewPanel());
        pane.addTab("客户端", createClientsPanel());
        pane.addTab("用户", createUsersPanel());
        pane.addTab("配置", createConfigPanel());
        pane.addTab("日志", createLogsPanel());

        return pane;
    }

    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 24));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));

        // 创建统计卡片面板
        JPanel statsGrid = new JPanel(new GridLayout(2, 2, 20, 20));
        statsGrid.setBackground(BACKGROUND_COLOR);
        statsGrid.setBorder(new EmptyBorder(0, 0, 0, 0));

        // 创建统计卡片并保存引用
        portCard = createStatCard("服务器端口", String.valueOf(config.getPort()), "🔌");
        connectionCard = createStatCard("当前连接数", "0", "👥");
        maxConnectionsCard = createStatCard("最大连接数", String.valueOf(config.getMaxConnections()), "📊");
        usersCard = createStatCard("用户总数", String.valueOf(userManager.getAllUsers().size()), "👤");

        statsGrid.add(portCard);
        statsGrid.add(connectionCard);
        statsGrid.add(maxConnectionsCard);
        statsGrid.add(usersCard);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(BACKGROUND_COLOR);
        centerPanel.add(statsGrid, BorderLayout.NORTH);

        panel.add(centerPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatCard(String title, String value, String icon) {
        JPanel card = new JPanel(new BorderLayout(12, 12));
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 228, 235), 1),
                new EmptyBorder(24, 24, 24, 24)
        ));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel topRow = new JPanel(new BorderLayout(12, 0));
        topRow.setOpaque(false);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("微软雅黑", Font.PLAIN, 36));
        iconLabel.setForeground(PRIMARY_COLOR);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        titleLabel.setForeground(TEXT_SECONDARY);

        topRow.add(iconLabel, BorderLayout.WEST);
        topRow.add(titleLabel, BorderLayout.CENTER);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("微软雅黑", Font.BOLD, 32));
        valueLabel.setForeground(TEXT_PRIMARY);

        card.add(topRow, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.putClientProperty("valueLabel", valueLabel);

        return card;
    }

    private JPanel createClientsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));

        String[] columns = {"IP地址", "端口", "连接时间", "状态"};
        clientsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = createStyledTable(clientsTableModel);
        JScrollPane scrollPane = createStyledScrollPane(table);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));

        String[] columns = {"用户名", "主目录", "启用", "权限"};
        usersTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = createStyledTable(usersTableModel);
        JScrollPane scrollPane = createStyledScrollPane(table);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        buttonPanel.setBorder(new EmptyBorder(0, 0, 8, 0));

        JButton addButton = createPrimaryButton("添加用户");
        addButton.addActionListener(e -> showAddUserDialog());

        JButton editButton = createSecondaryButton("编辑用户");
        editButton.addActionListener(e -> showEditUserDialog(table.getSelectedRow()));

        JButton deleteButton = createDangerButton("删除用户");
        deleteButton.addActionListener(e -> deleteUser(table.getSelectedRow()));

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(CARD_BACKGROUND);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 228, 235), 1),
                new EmptyBorder(32, 32, 32, 32)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 0, 12, 20);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formCard.add(createFormLabel("端口:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        portField = createFormTextField();
        formCard.add(portField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        formCard.add(createFormLabel("根目录:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel rootDirPanel = new JPanel(new BorderLayout(8, 0));
        rootDirPanel.setOpaque(false);
        rootDirField = createFormTextField();
        JButton browseRootDirButton = createSecondaryButton("浏览");
        browseRootDirButton.setPreferredSize(new Dimension(100, 40));
        browseRootDirButton.addActionListener(e -> browseDirectory(rootDirField));
        rootDirPanel.add(rootDirField, BorderLayout.CENTER);
        rootDirPanel.add(browseRootDirButton, BorderLayout.EAST);
        formCard.add(rootDirPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        formCard.add(createFormLabel("最大连接数:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        maxConnectionsField = createFormTextField();
        formCard.add(maxConnectionsField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        buttonPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        JButton saveButton = createPrimaryButton("保存配置");
        saveButton.addActionListener(e -> saveConfig());
        buttonPanel.add(saveButton);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(BACKGROUND_COLOR);
        centerPanel.add(formCard, BorderLayout.NORTH);

        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void browseDirectory(JTextField textField) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("选择目录");
        
        String currentPath = textField.getText();
        if (!currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists()) {
                fileChooser.setCurrentDirectory(currentDir);
            }
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            textField.setText(selectedDir.getAbsolutePath());
        }
    }

    private JPanel createLogsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));

        JTextArea logTextArea = new JTextArea();
        logTextArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logTextArea.setBackground(Color.WHITE);
        logTextArea.setForeground(TEXT_PRIMARY);
        logTextArea.setEditable(false);
        logTextArea.setLineWrap(true);
        logTextArea.setWrapStyleWord(true);
        logTextArea.setBorder(new EmptyBorder(16, 16, 16, 16));

        logsTableModel = new DefaultTableModel(new String[]{"时间", "级别", "来源", "消息"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JScrollPane scrollPane = new JScrollPane(logTextArea);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 228, 235), 1),
                new EmptyBorder(0, 0, 0, 0)
        ));
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getViewport().setBackground(Color.WHITE);

        JPanel filterCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        filterCard.setBackground(CARD_BACKGROUND);
        filterCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 228, 235), 1),
                new EmptyBorder(16, 20, 16, 20)
        ));

        JLabel searchLabel = new JLabel("搜索:");
        searchLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        searchLabel.setForeground(TEXT_SECONDARY);

        JTextField searchField = new JTextField(30);
        searchField.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(200, 200, 200), 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        searchField.setBackground(Color.WHITE);

        JButton searchButton = createPrimaryButton("搜索");
        searchButton.addActionListener(e -> {
            List<Logger.LogEntry> entries = searchField.getText().isEmpty() 
                ? logger.getLogEntries() 
                : logger.searchLogEntries(searchField.getText());
            displayLogEntries(logTextArea, entries);
        });

        JButton clearButton = createSecondaryButton("清空日志");
        clearButton.addActionListener(e -> {
            logger.clearLogs();
            logTextArea.setText("");
        });

        filterCard.add(searchLabel);
        filterCard.add(searchField);
        filterCard.add(searchButton);
        filterCard.add(clearButton);

        panel.add(filterCard, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        logger.addListener(entry -> SwingUtilities.invokeLater(() -> {
            if (logsTableModel.getRowCount() > 1000) {
                logsTableModel.removeRow(0);
            }
            appendLogEntry(logTextArea, entry);
        }));

        return panel;
    }

    private void displayLogEntries(JTextArea textArea, List<Logger.LogEntry> entries) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Logger.LogEntry entry : entries) {
            sb.append(String.format("[%s] [%s] %s%n",
                    entry.timestamp.format(formatter),
                    entry.level,
                    entry.message));
        }
        textArea.setText(sb.toString());
    }

    private void appendLogEntry(JTextArea textArea, Logger.LogEntry entry) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String logLine = String.format("[%s] [%s] %s%n",
                entry.timestamp.format(formatter),
                entry.level,
                entry.message);
        textArea.append(logLine);
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    private JTextField createFormTextField() {
        JTextField field = new JTextField(25);
        field.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(200, 200, 200), 1),
                new EmptyBorder(10, 14, 10, 14)
        ));
        field.setBackground(Color.WHITE);
        return field;
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        table.setRowHeight(36);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setBackground(Color.WHITE);
        table.setForeground(TEXT_PRIMARY);
        table.setSelectionBackground(new Color(227, 242, 253));
        table.setSelectionForeground(TEXT_PRIMARY);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("微软雅黑", Font.BOLD, 13));
        header.setBackground(new Color(248, 250, 252));
        header.setForeground(TEXT_SECONDARY);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(220, 228, 235)),
                new EmptyBorder(0, 0, 0, 0)
        ));
        header.setReorderingAllowed(false);

        return table;
    }

    private JScrollPane createStyledScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 228, 235), 1),
                new EmptyBorder(0, 0, 0, 0)
        ));
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(PRIMARY_COLOR);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(140, 40));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(PRIMARY_DARK);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(PRIMARY_COLOR);
            }
        });

        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.BOLD, 13));
        button.setForeground(TEXT_PRIMARY);
        button.setBackground(new Color(240, 243, 247));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(140, 40));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(228, 233, 240));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(240, 243, 247));
            }
        });

        return button;
    }

    private JButton createDangerButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(ERROR_COLOR);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(140, 40));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(211, 47, 47));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(ERROR_COLOR);
            }
        });

        return button;
    }

    private void loadConfig() {
        config = new ServerConfig();
        try {
            config.load(CONFIG_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
        userManager = new UserManager(USERS_PATH);
        logger = Logger.getInstance();
        logger.setLogDirectory(config.getLogDirectory());
    }

    private void setupServer() {
        ftpServer = new FtpServer(config, userManager);
        ftpServer.addListener(this);
    }
    
    private void initializeUpdateTimer() {
        // 创建定时器，每2秒更新一次概览数据
        updateTimer = new javax.swing.Timer(2000, e -> {
            SwingUtilities.invokeLater(this::updateOverviewStats);
        });
        updateTimer.start();
    }
    
    private void cleanup() {
        // 清理定时器
        if (updateTimer != null && updateTimer.isRunning()) {
            updateTimer.stop();
        }
    }

    private void updateConfigFields() {
        portField.setText(String.valueOf(config.getPort()));
        rootDirField.setText(config.getRootDirectory());
        maxConnectionsField.setText(String.valueOf(config.getMaxConnections()));
    }

    private void saveConfig() {
        try {
            config.setPort(Integer.parseInt(portField.getText()));
            config.setRootDirectory(rootDirField.getText());
            config.setMaxConnections(Integer.parseInt(maxConnectionsField.getText()));
            config.save(CONFIG_PATH);
            JOptionPane.showMessageDialog(this, "配置保存成功！");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "保存配置时出错：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleServer() {
        try {
            if (ftpServer.isRunning()) {
                ftpServer.stop();
            } else {
                ftpServer.start();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "错误：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateStatus() {
        if (ftpServer.isRunning()) {
            statusLabel.setText("运行在端口 " + config.getPort());
            statusIndicator.setForeground(SUCCESS_COLOR);
            startStopButton.setText("停止服务器");
            startStopButton.setBackground(ERROR_COLOR);
            startStopButton.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    startStopButton.setBackground(new Color(211, 47, 47));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    startStopButton.setBackground(ERROR_COLOR);
                }
            });
        } else {
            statusLabel.setText("已停止");
            statusIndicator.setForeground(ERROR_COLOR);
            startStopButton.setText("启动服务器");
            startStopButton.setBackground(PRIMARY_COLOR);
            startStopButton.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    startStopButton.setBackground(PRIMARY_DARK);
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    startStopButton.setBackground(PRIMARY_COLOR);
                }
            });
        }
    }

    private void updateOverviewStats() {
        // 更新服务器端口
        updateCardValue(portCard, String.valueOf(config.getPort()));
        
        // 更新当前连接数
        int connectionCount = ftpServer != null ? ftpServer.getConnectionCount() : 0;
        updateCardValue(connectionCard, String.valueOf(connectionCount));
        
        // 更新最大连接数
        updateCardValue(maxConnectionsCard, String.valueOf(config.getMaxConnections()));
        
        // 更新用户总数
        int userCount = userManager != null ? userManager.getAllUsers().size() : 0;
        updateCardValue(usersCard, String.valueOf(userCount));
        
        // 更新连接状态颜色指示
        updateConnectionStatusIndicator(connectionCount);
    }
    
    private void updateCardValue(JPanel card, String value) {
        if (card != null) {
            JLabel valueLabel = (JLabel) card.getClientProperty("valueLabel");
            if (valueLabel != null) {
                valueLabel.setText(value);
            }
        }
    }
    
    private void updateConnectionStatusIndicator(int connectionCount) {
        if (connectionCard != null) {
            JLabel valueLabel = (JLabel) connectionCard.getClientProperty("valueLabel");
            if (valueLabel != null) {
                // 根据连接数设置颜色
                if (connectionCount == 0) {
                    valueLabel.setForeground(TEXT_SECONDARY);
                } else if (connectionCount < config.getMaxConnections() * 0.8) {
                    valueLabel.setForeground(SUCCESS_COLOR);
                } else if (connectionCount < config.getMaxConnections()) {
                    valueLabel.setForeground(WARNING_COLOR);
                } else {
                    valueLabel.setForeground(ERROR_COLOR);
                }
            }
        }
    }

    private void updateClientsTable() {
        clientsTableModel.setRowCount(0);
        if (ftpServer != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (FtpServer.ClientSession session : ftpServer.getClientSessions()) {
                Object[] row = {
                        session.getClientAddress(),
                        session.getClientPort(),
                        session.connectTime.format(formatter),
                        session.active ? "活跃" : "不活跃"
                };
                clientsTableModel.addRow(row);
            }
        }
    }

    private void updateUsersTable() {
        usersTableModel.setRowCount(0);
        for (User user : userManager.getAllUsers()) {
            Object[] row = {
                    user.getUsername(),
                    user.getHomeDirectory() != null ? user.getHomeDirectory() : "Default",
                    user.isEnabled(),
                    user.getPermissions().toString()
            };
            usersTableModel.addRow(row);
        }
        updateOverviewStats();
    }

    private void showAddUserDialog() {
        JDialog dialog = new JDialog(this, "添加用户", true);
        dialog.setSize(500, 580);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(0, 0));
        dialog.getContentPane().setBackground(BACKGROUND_COLOR);

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(CARD_BACKGROUND);
        formCard.setBorder(new EmptyBorder(32, 32, 32, 32));

        JScrollPane scrollPane = new JScrollPane(formCard);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(BACKGROUND_COLOR);
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 20);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField usernameField = createFormTextField();
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(200, 200, 200), 1),
                new EmptyBorder(10, 14, 10, 14)
        ));
        passwordField.setBackground(Color.WHITE);
        JTextField homeDirField = createFormTextField();
        JCheckBox enabledCheckbox = new JCheckBox("启用", true);
        enabledCheckbox.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        enabledCheckbox.setBackground(CARD_BACKGROUND);

        JCheckBox readCheck = new JCheckBox("读取", true);
        JCheckBox writeCheck = new JCheckBox("写入", true);
        JCheckBox deleteCheck = new JCheckBox("删除", true);
        JCheckBox createDirCheck = new JCheckBox("创建目录", true);
        JCheckBox deleteDirCheck = new JCheckBox("删除目录", true);
        JCheckBox renameCheck = new JCheckBox("重命名", true);
        JCheckBox listCheck = new JCheckBox("列表", true);

        for (JCheckBox check : new JCheckBox[]{readCheck, writeCheck, deleteCheck, createDirCheck, deleteDirCheck, renameCheck, listCheck}) {
            check.setFont(new Font("微软雅黑", Font.PLAIN, 13));
            check.setBackground(CARD_BACKGROUND);
            check.setForeground(TEXT_PRIMARY);
        }

        gbc.gridx = 0;
        gbc.gridy = 0;
        formCard.add(createFormLabel("用户名:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formCard.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        formCard.add(createFormLabel("密码:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formCard.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        formCard.add(createFormLabel("主目录:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formCard.add(homeDirField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        formCard.add(enabledCheckbox, gbc);

        gbc.gridy = 4;
        gbc.gridwidth = 2;
        JLabel permLabel = new JLabel("权限:");
        permLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        permLabel.setForeground(TEXT_PRIMARY);
        formCard.add(permLabel, gbc);

        gbc.gridy = 5;
        gbc.gridwidth = 1;
        formCard.add(readCheck, gbc);

        gbc.gridy = 6;
        formCard.add(writeCheck, gbc);

        gbc.gridy = 7;
        formCard.add(deleteCheck, gbc);

        gbc.gridy = 8;
        formCard.add(createDirCheck, gbc);

        gbc.gridy = 9;
        formCard.add(deleteDirCheck, gbc);

        gbc.gridy = 10;
        formCard.add(renameCheck, gbc);

        gbc.gridy = 11;
        formCard.add(listCheck, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        buttonPanel.setBorder(new EmptyBorder(20, 24, 24, 24));

        JButton cancelButton = createSecondaryButton("取消");
        cancelButton.addActionListener(e -> dialog.dispose());

        JButton okButton = createPrimaryButton("确定");
        okButton.addActionListener(e -> {
            User user = new User();
            user.setUsername(usernameField.getText());
            user.setPassword(new String(passwordField.getPassword()));
            user.setHomeDirectory(homeDirField.getText().isEmpty() ? null : homeDirField.getText());
            user.setEnabled(enabledCheckbox.isSelected());
            if (readCheck.isSelected()) user.addPermission(User.Permission.READ);
            if (writeCheck.isSelected()) user.addPermission(User.Permission.WRITE);
            if (deleteCheck.isSelected()) user.addPermission(User.Permission.DELETE);
            if (createDirCheck.isSelected()) user.addPermission(User.Permission.CREATE_DIR);
            if (deleteDirCheck.isSelected()) user.addPermission(User.Permission.DELETE_DIR);
            if (renameCheck.isSelected()) user.addPermission(User.Permission.RENAME);
            if (listCheck.isSelected()) user.addPermission(User.Permission.LIST);
            userManager.addUser(user);
            updateUsersTable();
            dialog.dispose();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showEditUserDialog(int row) {
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一个用户");
            return;
        }
        String username = (String) usersTableModel.getValueAt(row, 0);
        User user = userManager.getUser(username).orElse(null);
        if (user == null) return;

        JDialog dialog = new JDialog(this, "编辑用户", true);
        dialog.setSize(500, 580);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(0, 0));
        dialog.getContentPane().setBackground(BACKGROUND_COLOR);

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(CARD_BACKGROUND);
        formCard.setBorder(new EmptyBorder(32, 32, 32, 32));

        JScrollPane scrollPane = new JScrollPane(formCard);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(BACKGROUND_COLOR);
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 20);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField usernameField = createFormTextField();
        usernameField.setText(user.getUsername());
        usernameField.setEditable(false);
        usernameField.setBackground(new Color(245, 245, 245));
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setText(user.getPassword());
        passwordField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(200, 200, 200), 1),
                new EmptyBorder(10, 14, 10, 14)
        ));
        passwordField.setBackground(Color.WHITE);
        JTextField homeDirField = createFormTextField();
        homeDirField.setText(user.getHomeDirectory() != null ? user.getHomeDirectory() : "");
        JCheckBox enabledCheckbox = new JCheckBox("启用", user.isEnabled());
        enabledCheckbox.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        enabledCheckbox.setBackground(CARD_BACKGROUND);

        if (user.isAnonymous()) {
            passwordField.setEditable(false);
            passwordField.setBackground(new Color(245, 245, 245));
            homeDirField.setEditable(false);
            homeDirField.setBackground(new Color(245, 245, 245));
        }

        JCheckBox readCheck = new JCheckBox("读取", user.hasPermission(User.Permission.READ));
        JCheckBox writeCheck = new JCheckBox("写入", user.hasPermission(User.Permission.WRITE));
        JCheckBox deleteCheck = new JCheckBox("删除", user.hasPermission(User.Permission.DELETE));
        JCheckBox createDirCheck = new JCheckBox("创建目录", user.hasPermission(User.Permission.CREATE_DIR));
        JCheckBox deleteDirCheck = new JCheckBox("删除目录", user.hasPermission(User.Permission.DELETE_DIR));
        JCheckBox renameCheck = new JCheckBox("重命名", user.hasPermission(User.Permission.RENAME));
        JCheckBox listCheck = new JCheckBox("列表", user.hasPermission(User.Permission.LIST));

        for (JCheckBox check : new JCheckBox[]{readCheck, writeCheck, deleteCheck, createDirCheck, deleteDirCheck, renameCheck, listCheck}) {
            check.setFont(new Font("微软雅黑", Font.PLAIN, 13));
            check.setBackground(CARD_BACKGROUND);
            check.setForeground(TEXT_PRIMARY);
        }

        gbc.gridx = 0;
        gbc.gridy = 0;
        formCard.add(createFormLabel("用户名:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formCard.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        formCard.add(createFormLabel("密码:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formCard.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        formCard.add(createFormLabel("主目录:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formCard.add(homeDirField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        formCard.add(enabledCheckbox, gbc);

        gbc.gridy = 4;
        gbc.gridwidth = 2;
        JLabel permLabel = new JLabel("权限:");
        permLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        permLabel.setForeground(TEXT_PRIMARY);
        formCard.add(permLabel, gbc);

        gbc.gridy = 5;
        gbc.gridwidth = 1;
        formCard.add(readCheck, gbc);

        gbc.gridy = 6;
        formCard.add(writeCheck, gbc);

        gbc.gridy = 7;
        formCard.add(deleteCheck, gbc);

        gbc.gridy = 8;
        formCard.add(createDirCheck, gbc);

        gbc.gridy = 9;
        formCard.add(deleteDirCheck, gbc);

        gbc.gridy = 10;
        formCard.add(renameCheck, gbc);

        gbc.gridy = 11;
        formCard.add(listCheck, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        buttonPanel.setBorder(new EmptyBorder(20, 24, 24, 24));

        JButton cancelButton = createSecondaryButton("取消");
        cancelButton.addActionListener(e -> dialog.dispose());

        JButton okButton = createPrimaryButton("确定");
        okButton.addActionListener(e -> {
            if (!user.isAnonymous()) {
                user.setPassword(new String(passwordField.getPassword()));
                user.setHomeDirectory(homeDirField.getText().isEmpty() ? null : homeDirField.getText());
            }
            user.setEnabled(enabledCheckbox.isSelected());
            user.getPermissions().clear();
            if (readCheck.isSelected()) user.addPermission(User.Permission.READ);
            if (writeCheck.isSelected()) user.addPermission(User.Permission.WRITE);
            if (deleteCheck.isSelected()) user.addPermission(User.Permission.DELETE);
            if (createDirCheck.isSelected()) user.addPermission(User.Permission.CREATE_DIR);
            if (deleteDirCheck.isSelected()) user.addPermission(User.Permission.DELETE_DIR);
            if (renameCheck.isSelected()) user.addPermission(User.Permission.RENAME);
            if (listCheck.isSelected()) user.addPermission(User.Permission.LIST);
            userManager.updateUser(user);
            updateUsersTable();
            dialog.dispose();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void deleteUser(int row) {
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一个用户");
            return;
        }
        String username = (String) usersTableModel.getValueAt(row, 0);
        User user = userManager.getUser(username).orElse(null);
        if (user != null && user.isAnonymous()) {
            JOptionPane.showMessageDialog(this, "匿名用户不可删除", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "确定要删除用户 '" + username + "' 吗？", "确认", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            userManager.removeUser(username);
            updateUsersTable();
        }
    }

    @Override
    public void onServerStarted() {
        SwingUtilities.invokeLater(() -> {
            updateStatus();
            updateOverviewStats();
        });
    }

    @Override
    public void onServerStopped() {
        SwingUtilities.invokeLater(() -> {
            updateStatus();
            updateOverviewStats();
            updateClientsTable();
        });
    }

    @Override
    public void onClientConnected(FtpServer.ClientSession session) {
        SwingUtilities.invokeLater(() -> {
            updateClientsTable();
            updateOverviewStats(); // 立即更新连接数
        });
    }

    @Override
    public void onClientDisconnected(FtpServer.ClientSession session) {
        SwingUtilities.invokeLater(() -> {
            updateClientsTable();
            updateOverviewStats(); // 立即更新连接数
        });
    }

    @Override
    public void onLogEntry(Logger.LogEntry entry) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            Font font = new Font("微软雅黑", Font.PLAIN, 12);
            UIManager.put("Label.font", font);
            UIManager.put("Button.font", font);
            UIManager.put("TextField.font", font);
            UIManager.put("TextArea.font", font);
            UIManager.put("Table.font", font);
            UIManager.put("TableHeader.font", font);
            UIManager.put("TabbedPane.font", font);
            UIManager.put("CheckBox.font", font);
            UIManager.put("RadioButton.font", font);
            UIManager.put("ComboBox.font", font);
            UIManager.put("List.font", font);
            UIManager.put("Tree.font", font);
            UIManager.put("OptionPane.font", font);
            UIManager.put("TitledBorder.font", font);
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
