package com.ftpserver.ui.content;

import com.ftpserver.ui.model.UserRow;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class UsersContent extends JPanel {

    private DefaultTableModel tableModel;
    private JTable table;
    private JButton addBtn;
    private JButton editBtn;
    private JButton deleteBtn;

    public UsersContent() {
        initialize();
    }

    private void initialize() {
        setBackground(new Color(248, 250, 252));
        setLayout(new BorderLayout(0, 12));

        JPanel headerBar = createHeaderBar();
        table = createUsersTable();

        add(headerBar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private JPanel createHeaderBar() {
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(new Color(248, 250, 252));

        JLabel title = new JLabel("用户管理");
        title.setForeground(new Color(15, 23, 42));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(new Color(248, 250, 252));

        addBtn = createButton("添加用户", new Color(37, 99, 235), Color.WHITE);
        editBtn = createButton("编辑", new Color(241, 245, 249), new Color(71, 85, 105));
        deleteBtn = createButton("删除", new Color(254, 226, 226), new Color(220, 38, 38));

        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteBtn);

        headerBar.add(title, BorderLayout.WEST);
        headerBar.add(buttonPanel, BorderLayout.EAST);

        return headerBar;
    }

    private JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bg.darker()),
            new EmptyBorder(7, 14, 7, 14)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JTable createUsersTable() {
        String[] columnNames = {"用户名", "主目录", "启用状态", "权限"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int column) {
                return String.class;
            }
        };

        JTable table = new JTable(tableModel);
        table.setRowHeight(36);
        table.setGridColor(new Color(226, 232, 240));
        table.setSelectionBackground(new Color(239, 246, 255));
        table.setSelectionForeground(new Color(37, 99, 235));
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setForeground(new Color(71, 85, 105));
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.setAutoCreateRowSorter(true);
        table.setDoubleBuffered(true);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        return table;
    }

    public void clearData() {
        tableModel.setRowCount(0);
    }

    public void addUser(UserRow user) {
        Object[] row = {user.username, user.homeDir, user.enabled, user.permissions};
        tableModel.addRow(row);
    }
    
    /**
     * 批量更新数据，提高性能
     * @param users 用户列表
     */
    public void updateData(java.util.List<UserRow> users) {
        // 批量更新，先清空再添加所有行
        tableModel.setRowCount(0);
        for (UserRow user : users) {
            Object[] row = {user.username, user.homeDir, user.enabled, user.permissions};
            tableModel.addRow(row);
        }
    }

    public UserRow getSelectedUser() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            return new UserRow(
                (String) tableModel.getValueAt(row, 0),
                (String) tableModel.getValueAt(row, 1),
                (String) tableModel.getValueAt(row, 2),
                (String) tableModel.getValueAt(row, 3)
            );
        }
        return null;
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public JTable getTable() {
        return table;
    }

    public JButton getAddBtn() {
        return addBtn;
    }

    public JButton getEditBtn() {
        return editBtn;
    }

    public JButton getDeleteBtn() {
        return deleteBtn;
    }
}
