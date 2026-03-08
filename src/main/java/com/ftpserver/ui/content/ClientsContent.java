package com.ftpserver.ui.content;

import com.ftpserver.ui.model.ClientRow;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class ClientsContent extends JPanel {

    private DefaultTableModel tableModel;
    private JTable table;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField searchField;
    private List<ClientRow> allClients;

    public ClientsContent() {
        allClients = new ArrayList<>();
        initialize();
    }

    private void initialize() {
        setBackground(new Color(248, 250, 252));
        setLayout(new BorderLayout(0, 12));

        JPanel headerPanel = new JPanel(new BorderLayout(0, 8));
        headerPanel.setBackground(new Color(248, 250, 252));

        JLabel title = new JLabel("已连接客户端");
        title.setForeground(new Color(15, 23, 42));

        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(200, 28));
        searchField.setToolTipText("搜索IP地址或端口");
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterClients();
            }
        });

        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(searchField, BorderLayout.EAST);

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
        table.setDoubleBuffered(true);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * 过滤客户端列表
     */
    private void filterClients() {
        String searchText = searchField.getText().trim().toLowerCase();
        tableModel.setRowCount(0);
        
        if (searchText.isEmpty()) {
            for (ClientRow client : allClients) {
                Object[] row = {client.ip, client.port, client.connectTime, client.status};
                tableModel.addRow(row);
            }
        } else {
            for (ClientRow client : allClients) {
                if (client.ip.toLowerCase().contains(searchText) || 
                    client.port.toLowerCase().contains(searchText) ||
                    client.status.toLowerCase().contains(searchText)) {
                    Object[] row = {client.ip, client.port, client.connectTime, client.status};
                    tableModel.addRow(row);
                }
            }
        }
    }

    public void clearData() {
        allClients.clear();
        tableModel.setRowCount(0);
    }

    public void addClient(ClientRow client) {
        allClients.add(client);
        filterClients();
    }
    
    /**
     * 批量更新数据，提高性能
     * @param clients 客户端列表
     */
    public void updateData(List<ClientRow> clients) {
        allClients = new ArrayList<>(clients);
        filterClients();
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public JTable getTable() {
        return table;
    }
    
    /**
     * 获取搜索字段
     * @return 搜索字段
     */
    public JTextField getSearchField() {
        return searchField;
    }
}
