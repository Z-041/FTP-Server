package com.ftpserver.ui.content;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ConfigContent extends JPanel {

    private JTextField portField;
    private JTextField rootDirField;
    private JButton browseBtn;
    private JTextField maxConnField;
    private JTextField timeoutField;
    private JTextField passiveStartField;
    private JTextField passiveEndField;
    private JCheckBox passiveModeCheck;
    private JCheckBox activeModeCheck;
    private JTextField logDirField;
    private JTextField passiveAddrField;
    private JButton saveBtn;

    public ConfigContent() {
        initialize();
    }

    private void initialize() {
        setBackground(new Color(248, 250, 252));
        setLayout(new BorderLayout());

        JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240)),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel cardTitle = new JLabel("服务器配置");
        cardTitle.setForeground(new Color(15, 23, 42));

        JPanel form = createForm();
        JPanel buttonBar = createButtonBar();

        JPanel centerPanel = new JPanel(new BorderLayout(0, 16));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(form, BorderLayout.CENTER);
        centerPanel.add(buttonBar, BorderLayout.SOUTH);

        card.add(cardTitle, BorderLayout.NORTH);
        card.add(centerPanel, BorderLayout.CENTER);

        add(card, BorderLayout.NORTH);
    }

    private JPanel createForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 0, 6, 14);
        gbc.anchor = GridBagConstraints.WEST;

        // Port
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel portLabel = new JLabel("端口:");
        portLabel.setForeground(new Color(71, 85, 105));
        form.add(portLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        portField = new JTextField(20);
        portField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)),
            new EmptyBorder(8, 10, 8, 10)
        ));
        form.add(portField, gbc);

        // Root Directory
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel rootDirLabel = new JLabel("根目录:");
        rootDirLabel.setForeground(new Color(71, 85, 105));
        form.add(rootDirLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JPanel rootDirPanel = new JPanel(new BorderLayout(8, 0));
        rootDirPanel.setBackground(Color.WHITE);
        rootDirField = new JTextField(20);
        rootDirField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)),
            new EmptyBorder(8, 10, 8, 10)
        ));
        browseBtn = new JButton("浏览");
        browseBtn.setBackground(new Color(241, 245, 249));
        browseBtn.setForeground(new Color(71, 85, 105));
        browseBtn.setFocusPainted(false);
        browseBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)),
            new EmptyBorder(7, 12, 7, 12)
        ));
        browseBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rootDirPanel.add(rootDirField, BorderLayout.CENTER);
        rootDirPanel.add(browseBtn, BorderLayout.EAST);
        form.add(rootDirPanel, gbc);

        // Max Connections
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel maxConnLabel = new JLabel("最大连接数:");
        maxConnLabel.setForeground(new Color(71, 85, 105));
        form.add(maxConnLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        maxConnField = new JTextField(20);
        maxConnField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)),
            new EmptyBorder(8, 10, 8, 10)
        ));
        form.add(maxConnField, gbc);

        // Connection Timeout
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel timeoutLabel = new JLabel("连接超时(秒):");
        timeoutLabel.setForeground(new Color(71, 85, 105));
        form.add(timeoutLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        timeoutField = new JTextField(20);
        timeoutField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)),
            new EmptyBorder(8, 10, 8, 10)
        ));
        form.add(timeoutField, gbc);

        // Passive Port Range Start
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel passiveStartLabel = new JLabel("被动端口起始:");
        passiveStartLabel.setForeground(new Color(71, 85, 105));
        form.add(passiveStartLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        passiveStartField = new JTextField(20);
        passiveStartField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)),
            new EmptyBorder(8, 10, 8, 10)
        ));
        form.add(passiveStartField, gbc);

        // Passive Port Range End
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel passiveEndLabel = new JLabel("被动端口结束:");
        passiveEndLabel.setForeground(new Color(71, 85, 105));
        form.add(passiveEndLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        passiveEndField = new JTextField(20);
        passiveEndField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)),
            new EmptyBorder(8, 10, 8, 10)
        ));
        form.add(passiveEndField, gbc);

        // Passive Mode
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel passiveModeLabel = new JLabel("被动模式:");
        passiveModeLabel.setForeground(new Color(71, 85, 105));
        form.add(passiveModeLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        passiveModeCheck = new JCheckBox("启用");
        passiveModeCheck.setBackground(Color.WHITE);
        form.add(passiveModeCheck, gbc);

        // Active Mode
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel activeModeLabel = new JLabel("主动模式:");
        activeModeLabel.setForeground(new Color(71, 85, 105));
        form.add(activeModeLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        activeModeCheck = new JCheckBox("启用");
        activeModeCheck.setBackground(Color.WHITE);
        form.add(activeModeCheck, gbc);

        // Log Directory
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel logDirLabel = new JLabel("日志目录:");
        logDirLabel.setForeground(new Color(71, 85, 105));
        form.add(logDirLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        logDirField = new JTextField(20);
        logDirField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)),
            new EmptyBorder(8, 10, 8, 10)
        ));
        form.add(logDirField, gbc);

        // Passive Address
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel passiveAddrLabel = new JLabel("被动模式地址:");
        passiveAddrLabel.setForeground(new Color(71, 85, 105));
        form.add(passiveAddrLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        passiveAddrField = new JTextField(20);
        passiveAddrField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)),
            new EmptyBorder(8, 10, 8, 10)
        ));
        form.add(passiveAddrField, gbc);

        return form;
    }

    private JPanel createButtonBar() {
        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonBar.setBackground(Color.WHITE);

        saveBtn = new JButton("保存配置");
        saveBtn.setBackground(new Color(37, 99, 235));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(),
            new EmptyBorder(9, 18, 9, 18)
        ));
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        buttonBar.add(saveBtn);
        return buttonBar;
    }

    public JTextField getPortField() {
        return portField;
    }

    public JTextField getRootDirField() {
        return rootDirField;
    }

    public JButton getBrowseBtn() {
        return browseBtn;
    }

    public JTextField getMaxConnField() {
        return maxConnField;
    }

    public JTextField getTimeoutField() {
        return timeoutField;
    }

    public JTextField getPassiveStartField() {
        return passiveStartField;
    }

    public JTextField getPassiveEndField() {
        return passiveEndField;
    }

    public JCheckBox getPassiveModeCheck() {
        return passiveModeCheck;
    }

    public JCheckBox getActiveModeCheck() {
        return activeModeCheck;
    }

    public JTextField getLogDirField() {
        return logDirField;
    }

    public JTextField getPassiveAddrField() {
        return passiveAddrField;
    }

    public JButton getSaveBtn() {
        return saveBtn;
    }
}
