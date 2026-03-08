package com.ftpserver.ui.content;

import com.ftpserver.ui.model.ClientRow;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ClientsContent extends JPanel {

    private DefaultTableModel tableModel;
    private JTable table;

    public ClientsContent() {
        initialize();
    }

    private void initialize() {
        setBackground(new Color(248, 250, 252));
        setLayout(new BorderLayout(0, 12));

        JLabel title = new JLabel("已连接客户端");
        title.setForeground(new Color(15, 23, 42));

        String[] columnNames = {"IP地址", "端口", "连接时间", "状态"};
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
        table = new JTable(tableModel);
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

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));

        add(title, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void clearData() {
        tableModel.setRowCount(0);
    }

    public void addClient(ClientRow client) {
        Object[] row = {client.ip, client.port, client.connectTime, client.status};
        tableModel.addRow(row);
    }
    
    /**
     * 批量更新数据，提高性能
     * @param clients 客户端列表
     */
    public void updateData(java.util.List<ClientRow> clients) {
        // 批量更新，先清空再添加所有行
        tableModel.setRowCount(0);
        for (ClientRow client : clients) {
            Object[] row = {client.ip, client.port, client.connectTime, client.status};
            tableModel.addRow(row);
        }
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public JTable getTable() {
        return table;
    }
}
