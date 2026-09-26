package com.ubs.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class UITheme {
    public static final Color BG = new Color(0xF4F6FA);
    public static final Color CARD_BG = Color.WHITE;
    public static final Color PRIMARY = new Color(0x2F6FED);
    public static final Color PRIMARY_DARK = new Color(0x1E4FBE);
    public static final Color SUCCESS = new Color(0x1E9E5A);
    public static final Color DANGER = new Color(0xE2453C);
    public static final Color WARNING = new Color(0xE6A317);
    public static final Color TEXT_MUTED = new Color(0x6B7280);
    public static final Color BORDER = new Color(0xDFE3EA);

    public static final Font FONT_H1 = new Font("SansSerif", Font.BOLD, 22);
    public static final Font FONT_H2 = new Font("SansSerif", Font.BOLD, 16);
    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_MONO = new Font("Monospaced", Font.PLAIN, 13);

    private UITheme() {
    }

    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        styleButton(b, PRIMARY, Color.WHITE);
        return b;
    }

    public static JButton dangerButton(String text) {
        JButton b = new JButton(text);
        styleButton(b, DANGER, Color.WHITE);
        return b;
    }

    public static JButton successButton(String text) {
        JButton b = new JButton(text);
        styleButton(b, SUCCESS, Color.WHITE);
        return b;
    }

    public static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(FONT_BODY_BOLD);
        b.setFocusPainted(false);
        b.setBackground(Color.WHITE);
        b.setForeground(PRIMARY_DARK);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY, 1, true),
                new EmptyBorder(8, 16, 8, 16)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static void styleButton(JButton b, Color bg, Color fg) {
        b.setFont(FONT_BODY_BOLD);
        b.setFocusPainted(false);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(9, 18, 9, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)));
        return p;
    }

    public static JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_H2);
        l.setForeground(new Color(0x1F2937));
        return l;
    }
}
