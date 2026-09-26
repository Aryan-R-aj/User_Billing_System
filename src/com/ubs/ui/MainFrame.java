package com.ubs.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Top-level application window. Hosts a tabbed layout for every functional
 * area of the system: Dashboard, Resources, Services (pricing), Usage
 * (start/stop -> billing), and Bill History.
 */
public class MainFrame extends JFrame {

    private final DashboardPanel dashboardPanel;
    private final ResourcePanel resourcePanel;
    private final ServicePanel servicePanel;
    private final UsagePanel usagePanel;
    private final BillHistoryPanel billHistoryPanel;
    private final JTabbedPane tabs;

    public MainFrame() {
        super("Usage & Billing System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setMinimumSize(new Dimension(960, 620));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG);

        dashboardPanel = new DashboardPanel();
        resourcePanel = new ResourcePanel(this);
        servicePanel = new ServicePanel(this);
        usagePanel = new UsagePanel(this);
        billHistoryPanel = new BillHistoryPanel();

        tabs = new JTabbedPane();
        tabs.setFont(UITheme.FONT_BODY_BOLD);
        tabs.addTab("  Dashboard  ", dashboardPanel);
        tabs.addTab("  Resources  ", resourcePanel);
        tabs.addTab("  Services & Pricing  ", servicePanel);
        tabs.addTab("  Start / Stop Usage  ", usagePanel);
        tabs.addTab("  Bill History  ", billHistoryPanel);

        tabs.addChangeListener(e -> refreshAll());

        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
    }

    /** Re-pulls fresh data into every tab; called after any add/start/stop/delete action. */
    public void refreshAll() {
        dashboardPanel.refresh();
        resourcePanel.refresh();
        servicePanel.refresh();
        usagePanel.refresh();
        billHistoryPanel.refresh();
    }
}
