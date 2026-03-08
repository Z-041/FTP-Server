package com.ftpserver.ui.content;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class OverviewContent extends JPanel {

    private JLabel statPortValue;
    private JLabel statConnectionsValue;
    private JLabel statMaxConnectionsValue;
    private JLabel statUsersValue;
    private JTextArea logTextArea;

    public OverviewContent() {
        initialize();
    }

    private void initialize() {
        setBackground(new Color(248, 250, 252));
        setLayout(new BorderLayout(0, 20));

        JPanel statsGrid = createStatsGrid();
        JPanel recentActivity = createRecentActivity();

        add(statsGrid, BorderLayout.NORTH);
        add(recentActivity, BorderLayout.CENTER);
    }

    private JPanel createStatsGrid() {
        JPanel statsGrid = new JPanel(new GridLayout(2, 2, 16, 16));
        statsGrid.setBackground(new Color(248, 250, 252));

        statPortValue = new JLabel("-");
        statConnectionsValue = new JLabel("0");
        statMaxConnectionsValue = new JLabel("-");
        statUsersValue = new JLabel("-");

        statsGrid.add(createStatCard("服务器端口", statPortValue, new Color(59, 130, 246)));
        statsGrid.add(createStatCard("活跃连接", statConnectionsValue, new Color(16, 185, 129)));
        statsGrid.add(createStatCard("最大连接数", statMaxConnectionsValue, new Color(139, 92, 246)));
        statsGrid.add(createStatCard("用户总数", statUsersValue, new Color(245, 158, 11)));

        return statsGrid;
    }

    private JPanel createStatCard(String label, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240)),
            new EmptyBorder(20, 20, 20, 20)
        ));

        valueLabel.setForeground(new Color(15, 23, 42));

        JLabel labelText = new JLabel(label);
        labelText.setForeground(new Color(100, 116, 139));

        // 顶部装饰条
        JPanel accentBar = new JPanel();
        accentBar.setBackground(accentColor);
        accentBar.setPreferredSize(new Dimension(0, 3));

        card.add(accentBar, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(labelText, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createRecentActivity() {
        JPanel recentActivity = new JPanel(new BorderLayout(0, 12));
        recentActivity.setBackground(new Color(248, 250, 252));

        JLabel activityTitle = new JLabel("最近活动");
        activityTitle.setForeground(new Color(15, 23, 42));

        logTextArea = new JTextArea();
        logTextArea.setBackground(new Color(255, 255, 255));
        logTextArea.setForeground(new Color(71, 85, 105));
        logTextArea.setEditable(false);
        logTextArea.setLineWrap(true);
        logTextArea.setWrapStyleWord(true);
        logTextArea.setBorder(new EmptyBorder(12, 12, 12, 12));
        logTextArea.setDoubleBuffered(true);

        JScrollPane scrollPane = new JScrollPane(logTextArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        scrollPane.setPreferredSize(new Dimension(0, 180));

        recentActivity.add(activityTitle, BorderLayout.NORTH);
        recentActivity.add(scrollPane, BorderLayout.CENTER);

        return recentActivity;
    }

    public JLabel getStatPortValue() {
        return statPortValue;
    }

    public JLabel getStatConnectionsValue() {
        return statConnectionsValue;
    }

    public JLabel getStatMaxConnectionsValue() {
        return statMaxConnectionsValue;
    }

    public JLabel getStatUsersValue() {
        return statUsersValue;
    }

    public JTextArea getLogTextArea() {
        return logTextArea;
    }
}
