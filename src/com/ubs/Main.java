package com.ubs;

import com.ubs.dao.DatabaseManager;
import com.ubs.ui.MainFrame;

import javax.swing.*;

/**
 * Application entry point.
 *
 * Usage:
 *   java -cp "bin:lib/sqlite-jdbc.jar" com.ubs.Main
 *
 * On first run this creates usage_billing.db (SQLite) in the current
 * working directory and initializes all required tables.
 */
public class Main {
    public static void main(String[] args) {
        DatabaseManager.initializeSchema();

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // fall back to default look and feel
            }
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
