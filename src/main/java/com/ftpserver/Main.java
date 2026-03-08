package com.ftpserver;

import com.ftpserver.ui.MainWindow;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set global font for Chinese support (after setting LookAndFeel)
        setGlobalFont();

        // Launch GUI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }

    private static void setGlobalFont() {
        // Get system font for cross-platform compatibility
        Font systemFont = getSystemFont();
        Font defaultFont = systemFont.deriveFont(Font.PLAIN, 12);
        Font boldFont = systemFont.deriveFont(Font.BOLD, 12);

        String[] fontKeys = {
            "Label.font", "Button.font", "ToggleButton.font", "RadioButton.font",
            "CheckBox.font", "ComboBox.font", "TextField.font", "TextArea.font",
            "TextPane.font", "EditorPane.font", "FormattedTextField.font", "List.font",
            "Tree.font", "Table.font", "TableHeader.font", "Menu.font", "MenuItem.font",
            "PopupMenu.font", "OptionPane.font", "Panel.font", "ProgressBar.font",
            "ScrollPane.font", "Slider.font", "Spinner.font", "TabbedPane.font",
            "TitledBorder.font", "ToolBar.font", "ToolTip.font", "Viewport.font",
            "InternalFrame.titleFont", "DesktopIcon.font", "ColorChooser.font",
            "FileChooser.listFont", "FileChooser.detailsViewFont"
        };

        for (String key : fontKeys) {
            Font oldFont = UIManager.getFont(key);
            if (oldFont != null) {
                Font newFont = systemFont.deriveFont(oldFont.getStyle(), oldFont.getSize());
                UIManager.put(key, newFont);
            } else {
                UIManager.put(key, defaultFont);
            }
        }

        UIManager.put("TableHeader.font", boldFont);
    }

    private static Font getSystemFont() {
        // Try to use system fonts with Chinese support, in priority order
        String[] preferredFonts = {
            "Microsoft YaHei",     // Windows
            "PingFang SC",         // macOS
            "Hiragino Sans GB",    // macOS
            "Noto Sans CJK SC",    // Linux
            "WenQuanYi Micro Hei", // Linux
            "SimHei",              // Windows
            "SimSun",              // Windows
            "Dialog"               // System default
        };

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = ge.getAvailableFontFamilyNames();

        for (String preferred : preferredFonts) {
            for (String available : availableFonts) {
                if (available.equalsIgnoreCase(preferred)) {
                    return new Font(preferred, Font.PLAIN, 12);
                }
            }
        }

        // Fallback to system default font
        return new Font(Font.DIALOG, Font.PLAIN, 12);
    }
}
