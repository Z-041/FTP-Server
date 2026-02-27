package com.ftpserver.ui.tray;

import com.ftpserver.server.FtpServer;
import com.ftpserver.util.Logger;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.event.ActionListener;

public class SystemTrayManager {

    private Stage primaryStage;
    private FtpServer ftpServer;
    private Logger logger;
    private SystemTray systemTray;
    private TrayIcon trayIcon;
    private MenuItem startStopTrayItem;
    private volatile boolean trayInitialized = false;
    private volatile boolean pendingStatusUpdate = false;
    private volatile boolean pendingRunningState = false;

    public SystemTrayManager(Stage primaryStage, FtpServer ftpServer, Logger logger) {
        this.primaryStage = primaryStage;
        this.ftpServer = ftpServer;
        this.logger = logger;
    }

    public void initSystemTray() {
        if (!SystemTray.isSupported()) {
            logger.info("System tray is not supported on this platform", "UI", "-");
            return;
        }

        java.awt.EventQueue.invokeLater(() -> {
            try {
                systemTray = SystemTray.getSystemTray();
                PopupMenu popup = new PopupMenu();

                // 主菜单选项
                MenuItem showItem = new MenuItem("Show Window");
                showItem.addActionListener(e -> showFromTray());

                // 根据当前服务器状态初始化菜单项标签
                String initialLabel = ftpServer.isRunning() ? "Stop Server" : "Start Server";
                startStopTrayItem = new MenuItem(initialLabel);
                startStopTrayItem.addActionListener(e -> toggleServerFromTray());

                // 添加服务器状态子菜单
                Menu statusMenu = new Menu("Server Status");
                MenuItem statusInfoItem = new MenuItem("View Status");
                statusInfoItem.addActionListener(e -> showServerStatus());
                statusMenu.add(statusInfoItem);

                // 添加快速操作子菜单
                Menu actionsMenu = new Menu("Quick Actions");
                MenuItem restartItem = new MenuItem("Restart Server");
                restartItem.addActionListener(e -> restartServer());
                MenuItem viewLogsItem = new MenuItem("View Logs");
                viewLogsItem.addActionListener(e -> showLogs());
                actionsMenu.add(restartItem);
                actionsMenu.add(viewLogsItem);

                MenuItem exitItem = new MenuItem("Exit");
                exitItem.addActionListener(e -> exitApplication());

                // 构建菜单结构
                popup.add(showItem);
                popup.addSeparator();
                popup.add(startStopTrayItem);
                popup.add(statusMenu);
                popup.add(actionsMenu);
                popup.addSeparator();
                popup.add(exitItem);

                Image icon = createTrayIcon();
                trayIcon = new TrayIcon(icon, "FTP Server", popup);
                trayIcon.setImageAutoSize(true);
                trayIcon.addActionListener(e -> showFromTray());
                
                // 添加工具提示
                updateTrayTooltip();

                systemTray.add(trayIcon);
                trayInitialized = true;
                
                // 处理初始化期间pending的状态更新
                if (pendingStatusUpdate) {
                    updateTrayServerStatus(pendingRunningState);
                    pendingStatusUpdate = false;
                }
                
                // 初始化时同步当前服务器状态
                updateTrayTooltip();
                
                logger.info("System tray initialized", "UI", "-");
            } catch (AWTException e) {
                logger.error("Failed to initialize system tray: " + e.getMessage(), "UI", "-");
            }
        });
    }

    private Image createTrayIcon() {
        int size = 32;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // 绘制背景
        g2d.setColor(new Color(59, 130, 246));
        g2d.fillRoundRect(0, 0, size, size, 8, 8);
        
        // 绘制FTP图标 - 使用跨平台兼容的字体
        g2d.setColor(Color.WHITE);
        // 尝试使用系统字体，确保跨平台兼容性
        Font font = null;
        try {
            font = new Font("Segoe UI", Font.BOLD, 16);
        } catch (Exception e) {
            try {
                font = new Font("Arial", Font.BOLD, 16);
            } catch (Exception ex) {
                font = new Font(Font.SANS_SERIF, Font.BOLD, 16);
            }
        }
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        String text = "FTP";
        int x = (size - fm.stringWidth(text)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(text, x, y);
        
        // 添加发光效果
        g2d.setColor(new Color(255, 255, 255, 50));
        g2d.fillOval(size/4, size/4, size/2, size/2);
        
        g2d.dispose();
        return image;
    }

    public void hideToTray() {
        Platform.runLater(() -> {
            // 使用 setIconified 最小化到任务栏，而不是 hide()
            // 这样可以保持 JavaFX 事件循环正常运行
            primaryStage.setIconified(true);
            primaryStage.hide();
        });
        if (trayIcon != null) {
            java.awt.EventQueue.invokeLater(() -> {
                trayIcon.displayMessage("FTP Server", "Server is running in background", TrayIcon.MessageType.INFO);
            });
        }
    }

    public void showFromTray() {
        Platform.runLater(() -> {
            primaryStage.setIconified(false);
            primaryStage.show();
            primaryStage.toFront();
            primaryStage.requestFocus();
        });
    }

    public void toggleServerFromTray() {
        Platform.runLater(() -> {
            try {
                if (ftpServer.isRunning()) {
                    ftpServer.stop();
                } else {
                    ftpServer.start();
                }
            } catch (Exception e) {
                logger.error("Error toggling server from tray: " + e.getMessage(), "UI", "-");
            }
        });
    }

    public void exitApplication() {
        if (ftpServer.isRunning()) {
            ftpServer.stop();
        }
        if (trayInitialized && systemTray != null) {
            java.awt.EventQueue.invokeLater(() -> {
                systemTray.remove(trayIcon);
                Platform.runLater(() -> {
                    Platform.exit();
                    System.exit(0);
                });
            });
        } else {
            Platform.runLater(() -> {
                Platform.exit();
                System.exit(0);
            });
        }
    }

    public boolean isTrayInitialized() {
        return trayInitialized;
    }

    private void updateTrayTooltip() {
        if (trayIcon != null) {
            String status = ftpServer.isRunning() ? "Running" : "Stopped";
            trayIcon.setToolTip("FTP Server - " + status);
        }
    }

    private void showServerStatus() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Server Status");
            alert.setHeaderText("FTP Server Status");
            
            String status = ftpServer.isRunning() ? "Running" : "Stopped";
            String message = "Status: " + status + "\n" +
                           "Connections: " + (ftpServer.isRunning() ? ftpServer.getConnectionCount() : 0) + "\n" +
                           "Uptime: " + getServerUptime();
            
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private String getServerUptime() {
        if (!ftpServer.isRunning()) {
            return "N/A";
        }
        // 这里可以添加实际的运行时间计算
        return "Calculating...";
    }

    private void restartServer() {
        new Thread(() -> {
            try {
                if (ftpServer.isRunning()) {
                    ftpServer.stop();
                    Thread.sleep(1000); // 短暂延迟
                }
                ftpServer.start();
                if (trayIcon != null) {
                    java.awt.EventQueue.invokeLater(() -> {
                        trayIcon.displayMessage("FTP Server", "Server restarted successfully", TrayIcon.MessageType.INFO);
                    });
                }
            } catch (Exception e) {
                logger.error("Error restarting server: " + e.getMessage(), "UI", "-");
                if (trayIcon != null) {
                    java.awt.EventQueue.invokeLater(() -> {
                        trayIcon.displayMessage("FTP Server", "Failed to restart server: " + e.getMessage(), TrayIcon.MessageType.ERROR);
                    });
                }
            }
        }).start();
    }

    private void showLogs() {
        Platform.runLater(() -> {
            // 这里可以打开日志窗口
            // 暂时显示一个简单的消息
            trayIcon.displayMessage("FTP Server", "Logs will be displayed in the main window", TrayIcon.MessageType.INFO);
        });
    }

    public void updateTrayServerStatus(boolean running) {
        // 如果托盘尚未初始化，保存状态待后续更新
        if (!trayInitialized) {
            pendingStatusUpdate = true;
            pendingRunningState = running;
            return;
        }
        
        java.awt.EventQueue.invokeLater(() -> {
            if (startStopTrayItem != null) {
                startStopTrayItem.setLabel(running ? "Stop Server" : "Start Server");
            }
            updateTrayTooltip();
            
            // 显示状态变更消息
            if (trayIcon != null) {
                String message = running ? "Server started successfully" : "Server stopped";
                trayIcon.displayMessage("FTP Server", message, TrayIcon.MessageType.INFO);
            }
        });
    }
}