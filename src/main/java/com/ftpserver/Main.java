package com.ftpserver;

import com.ftpserver.ui.MainWindow;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        // 设置系统外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 设置全局字体，支持中文显示（在设置LookAndFeel之后）
        setGlobalFont();

        // 在事件调度线程中启动 GUI
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }

    private static void setGlobalFont() {
        // 使用支持中文的字体
        String fontName = "Microsoft YaHei";
        // 如果微软雅黑不可用，尝试其他中文字体
        if (!isFontAvailable(fontName)) {
            fontName = "SimHei";  // 黑体
            if (!isFontAvailable(fontName)) {
                fontName = "SimSun";  // 宋体
                if (!isFontAvailable(fontName)) {
                    fontName = "Dialog";  // 系统默认对话框字体
                }
            }
        }

        Font defaultFont = new Font(fontName, Font.PLAIN, 12);
        Font boldFont = new Font(fontName, Font.BOLD, 12);

        // 设置所有可能的字体键
        String[] fontKeys = {
            "Label.font",
            "Button.font",
            "ToggleButton.font",
            "RadioButton.font",
            "CheckBox.font",
            "ComboBox.font",
            "TextField.font",
            "TextArea.font",
            "TextPane.font",
            "EditorPane.font",
            "FormattedTextField.font",
            "List.font",
            "Tree.font",
            "Table.font",
            "TableHeader.font",
            "Menu.font",
            "MenuItem.font",
            "PopupMenu.font",
            "OptionPane.font",
            "Panel.font",
            "ProgressBar.font",
            "ScrollPane.font",
            "Slider.font",
            "Spinner.font",
            "TabbedPane.font",
            "TitledBorder.font",
            "ToolBar.font",
            "ToolTip.font",
            "Viewport.font",
            "InternalFrame.titleFont",
            "DesktopIcon.font",
            "ColorChooser.font",
            "FileChooser.listFont",
            "FileChooser.detailsViewFont"
        };

        for (String key : fontKeys) {
            Font oldFont = UIManager.getFont(key);
            if (oldFont != null) {
                Font newFont = new Font(fontName, oldFont.getStyle(), oldFont.getSize());
                UIManager.put(key, newFont);
            } else {
                UIManager.put(key, defaultFont);
            }
        }

        // 特别设置表头字体为粗体
        UIManager.put("TableHeader.font", boldFont);

        // 遍历所有默认值，替换所有字体
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof Font) {
                Font oldFont = (Font) value;
                // 保留原有样式和大小
                Font newFont = new Font(fontName, oldFont.getStyle(), oldFont.getSize());
                UIManager.put(key, newFont);
            }
        }
    }

    private static boolean isFontAvailable(String fontName) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = ge.getAvailableFontFamilyNames();
        for (String font : availableFonts) {
            if (font.equalsIgnoreCase(fontName)) {
                return true;
            }
        }
        return false;
    }
}
