package com.ftpserver.ui.content;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LogsContent extends JPanel {

    private JTextArea logsArea;
    private JTextField searchField;
    private JButton searchBtn;
    private JButton clearBtn;

    public LogsContent() {
        initialize();
    }

    private void initialize() {
        setBackground(new Color(248, 250, 252));
        setLayout(new BorderLayout(0, 12));

        JLabel title = new JLabel("服务器日志");
        title.setForeground(new Color(15, 23, 42));

        logsArea = new JTextArea();
        logsArea.setBackground(new Color(255, 255, 255));
        logsArea.setForeground(new Color(71, 85, 105));
        logsArea.setEditable(false);
        logsArea.setLineWrap(true);
        logsArea.setWrapStyleWord(true);
        logsArea.setBorder(new EmptyBorder(12, 12, 12, 12));
        logsArea.setDoubleBuffered(true);

        JScrollPane scrollPane = new JScrollPane(logsArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        scrollPane.setPreferredSize(new Dimension(0, 350));

        JPanel controls = createControls();

        // 使用一个面板来包含标题、控制栏和日志区域
        JPanel contentPanel = new JPanel(new BorderLayout(0, 12));
        contentPanel.setBackground(new Color(248, 250, 252));
        contentPanel.add(controls, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        add(title, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createControls() {
        JPanel controls = new JPanel(new BorderLayout(10, 0));
        controls.setBackground(new Color(248, 250, 252));

        searchField = new JTextField();
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)),
            new EmptyBorder(8, 12, 8, 12)
        ));

        searchBtn = new JButton("搜索");
        searchBtn.setBackground(new Color(241, 245, 249));
        searchBtn.setForeground(new Color(71, 85, 105));
        searchBtn.setFocusPainted(false);
        searchBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)),
            new EmptyBorder(7, 14, 7, 14)
        ));
        searchBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        clearBtn = new JButton("清空");
        clearBtn.setBackground(new Color(254, 226, 226));
        clearBtn.setForeground(new Color(220, 38, 38));
        clearBtn.setFocusPainted(false);
        clearBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(252, 165, 165)),
            new EmptyBorder(7, 14, 7, 14)
        ));
        clearBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setBackground(new Color(248, 250, 252));
        buttonPanel.add(searchBtn);
        buttonPanel.add(clearBtn);

        controls.add(searchField, BorderLayout.CENTER);
        controls.add(buttonPanel, BorderLayout.EAST);

        return controls;
    }

    public void appendLog(String text) {
        logsArea.append(text);
        logsArea.setCaretPosition(logsArea.getDocument().getLength());
    }

    public void clearLogs() {
        logsArea.setText("");
    }

    public JTextArea getLogsArea() {
        return logsArea;
    }

    public JTextField getSearchField() {
        return searchField;
    }

    public JButton getSearchBtn() {
        return searchBtn;
    }

    public JButton getClearBtn() {
        return clearBtn;
    }
}
