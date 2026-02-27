package com.ftpserver.ui.sidebar;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;



public class Sidebar extends JPanel {

    private JButton navOverview;
    private JButton navClients;
    private JButton navUsers;
    private JButton navConfig;
    private JButton navLogs;
    private JPanel navItems;
    private Map<String, JButton> navButtons;
    private String activeNav = "overview";

    public Sidebar() {
        navButtons = new HashMap<>();
        initialize();
    }

    private void initialize() {
        setBackground(new Color(15, 23, 42));
        setPreferredSize(new Dimension(200, 0));
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(16, 12, 16, 12));

        // 标题区域
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        titlePanel.setBackground(new Color(15, 23, 42));
        titlePanel.setBorder(new EmptyBorder(0, 4, 12, 0));

        JLabel title = new JLabel("FTP Server");
        title.setForeground(new Color(248, 250, 252));

        JLabel subtitle = new JLabel("文件传输控制台");
        subtitle.setForeground(new Color(148, 163, 184));

        titlePanel.add(title);
        titlePanel.add(subtitle);

        // 导航按钮面板
        navItems = new JPanel();
        navItems.setLayout(new BoxLayout(navItems, BoxLayout.Y_AXIS));
        navItems.setBackground(new Color(15, 23, 42));
        navItems.setAlignmentX(Component.LEFT_ALIGNMENT);

        navOverview = createNavItem("总览", "overview", true);
        navClients = createNavItem("客户端", "clients", false);
        navUsers = createNavItem("用户管理", "users", false);
        navConfig = createNavItem("设置", "config", false);
        navLogs = createNavItem("日志", "logs", false);

        navItems.add(navOverview);
        navItems.add(Box.createVerticalStrut(2));
        navItems.add(navClients);
        navItems.add(Box.createVerticalStrut(2));
        navItems.add(navUsers);
        navItems.add(Box.createVerticalStrut(2));
        navItems.add(navConfig);
        navItems.add(Box.createVerticalStrut(2));
        navItems.add(navLogs);

        add(titlePanel, BorderLayout.NORTH);
        add(navItems, BorderLayout.CENTER);
    }

    private JButton createNavItem(String text, String key, boolean active) {
        JButton btn = new JButton(text);
        btn.setForeground(active ? new Color(248, 250, 252) : new Color(148, 163, 184));
        btn.setBackground(new Color(15, 23, 42));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        // 使用空边框作为内边距，左侧用颜色指示
        Color leftBorderColor = active ? new Color(59, 130, 246) : new Color(15, 23, 42);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, leftBorderColor),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        btn.setPreferredSize(new Dimension(0, 28));
        navButtons.put(key, btn);
        return btn;
    }

    public void setActiveNav(String key) {
        // 重置所有按钮样式
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            JButton btn = entry.getValue();
            boolean isActive = entry.getKey().equals(key);
            btn.setForeground(isActive ? new Color(248, 250, 252) : new Color(148, 163, 184));
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, isActive ? new Color(59, 130, 246) : new Color(15, 23, 42)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
        }
        activeNav = key;
    }

    public JButton getNavOverview() {
        return navOverview;
    }

    public JButton getNavClients() {
        return navClients;
    }

    public JButton getNavUsers() {
        return navUsers;
    }

    public JButton getNavConfig() {
        return navConfig;
    }

    public JButton getNavLogs() {
        return navLogs;
    }
}
