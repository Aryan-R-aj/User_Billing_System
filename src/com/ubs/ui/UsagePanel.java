package com.ubs.ui;

import com.ubs.dao.ResourceDAO;
import com.ubs.dao.ServiceDAO;
import com.ubs.dao.UsageSessionDAO;
import com.ubs.model.Bill;
import com.ubs.model.Resource;
import com.ubs.model.ServicePlan;
import com.ubs.model.UsageSession;
import com.ubs.service.BillingException;
import com.ubs.service.BillingService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsagePanel extends JPanel {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd MMM, HH:mm:ss");
    private static final java.text.DecimalFormat INR = new java.text.DecimalFormat("\u20B90.00");

    private final ResourceDAO resourceDAO = new ResourceDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final UsageSessionDAO usageSessionDAO = new UsageSessionDAO();
    private final BillingService billingService = new BillingService();
    private final MainFrame mainFrame;

    private JComboBox<Resource> resourceCombo;
    private JComboBox<ServicePlan> serviceCombo;
    private JTextField userNameField;
    private JLabel capacityHintLabel;
    private JLabel statusLabel;

    private DefaultTableModel tableModel;
    private JTable table;

    // sessionId -> plan / start time, used to recompute the live columns every tick
    // without hitting the database once per second.
    private final Map<Integer, UsageSession> activeSessions = new HashMap<>();
    private final Map<Integer, ServicePlan> planCache = new HashMap<>();

    private Timer tickTimer;

    public UsagePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(0, 16));
        setBorder(new EmptyBorder(20, 24, 20, 24));
        setBackground(UITheme.BG);

        add(buildForm(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);

        refresh();
        startTicker();
    }

    private JComponent buildForm() {
        JPanel card = UITheme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = UITheme.sectionTitle("Start Usage");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        resourceCombo = new JComboBox<>();
        resourceCombo.addActionListener(e -> onResourceSelected());
        serviceCombo = new JComboBox<>();
        userNameField = new JTextField(16);

        row.add(labeled("Resource", resourceCombo));
        row.add(labeled("Service / Pricing plan", serviceCombo));
        row.add(labeled("User name", userNameField));

        JButton startBtn = UITheme.successButton("\u25B6 Start Usage");
        startBtn.addActionListener(e -> onStart());
        row.add(Box.createHorizontalStrut(8));
        row.add(startBtn);

        capacityHintLabel = new JLabel(" ");
        capacityHintLabel.setFont(UITheme.FONT_BODY);
        capacityHintLabel.setForeground(UITheme.TEXT_MUTED);
        capacityHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(UITheme.FONT_BODY_BOLD);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(title);
        card.add(Box.createVerticalStrut(8));
        card.add(row);
        card.add(capacityHintLabel);
        card.add(statusLabel);
        return card;
    }

    private JPanel labeled(String label, JComponent field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(label);
        l.setFont(UITheme.FONT_BODY);
        l.setForeground(UITheme.TEXT_MUTED);
        field.setMaximumSize(new Dimension(220, 30));
        p.add(l);
        p.add(field);
        return p;
    }

    private JComponent buildTable() {
        JPanel card = UITheme.card();
        card.setLayout(new BorderLayout(0, 10));

        JLabel title = UITheme.sectionTitle("Active Usage (live)");
        card.add(title, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new Object[]{"Session ID", "Resource", "Service", "User", "Started At", "Elapsed", "Running Est."}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(UITheme.FONT_BODY);
        table.getTableHeader().setFont(UITheme.FONT_BODY_BOLD);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        card.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);
        JButton stopBtn = UITheme.dangerButton("\u25A0 Stop Selected & Generate Bill");
        stopBtn.addActionListener(e -> onStop());
        JButton refreshBtn = UITheme.secondaryButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());
        footer.add(refreshBtn);
        footer.add(stopBtn);
        card.add(footer, BorderLayout.SOUTH);

        return card;
    }

    private void onResourceSelected() {
        Resource r = (Resource) resourceCombo.getSelectedItem();
        serviceCombo.removeAllItems();
        if (r == null) {
            capacityHintLabel.setText(" ");
            return;
        }
        try {
            List<ServicePlan> plans = serviceDAO.findByResourceId(r.getId());
            for (ServicePlan p : plans) {
                serviceCombo.addItem(p);
            }
            if (plans.isEmpty()) {
                capacityHintLabel.setForeground(UITheme.WARNING);
                capacityHintLabel.setText("No service/pricing plan defined for this resource yet - add one in the Services tab.");
            } else {
                Resource fresh = resourceDAO.findById(r.getId());
                capacityHintLabel.setForeground(fresh.isFull() ? UITheme.DANGER : UITheme.TEXT_MUTED);
                capacityHintLabel.setText("Availability: " + fresh.getActiveCount() + " / " + fresh.getCapacity() + " in use"
                        + (fresh.isFull() ? "  \u2014  FULL, new requests will be rejected" : ""));
            }
        } catch (SQLException ex) {
            capacityHintLabel.setForeground(UITheme.DANGER);
            capacityHintLabel.setText("Error loading services: " + ex.getMessage());
        }
    }

    private void onStart() {
        Resource resource = (Resource) resourceCombo.getSelectedItem();
        ServicePlan plan = (ServicePlan) serviceCombo.getSelectedItem();
        String userName = userNameField.getText().trim();

        if (resource == null) {
            showStatus("Please add a resource first.", UITheme.DANGER);
            return;
        }
        if (plan == null) {
            showStatus("Please define a service/pricing plan for this resource first.", UITheme.DANGER);
            return;
        }
        if (userName.isEmpty()) {
            showStatus("User name is required.", UITheme.DANGER);
            return;
        }
        try {
            UsageSession session = billingService.startUsage(resource.getId(), plan.getId(), userName);
            showStatus("Usage started for " + userName + " on \"" + resource.getName() + "\" (session #" + session.getId() + ").", UITheme.SUCCESS);
            userNameField.setText("");
            refresh();
            mainFrame.refreshAll();
        } catch (BillingException be) {
            showStatus(be.getMessage(), UITheme.DANGER);
        } catch (SQLException ex) {
            showStatus("Database error: " + ex.getMessage(), UITheme.DANGER);
        }
    }

    private void onStop() {
        int row = table.getSelectedRow();
        if (row < 0) {
            showStatus("Select an active session to stop first.", UITheme.WARNING);
            return;
        }
        int sessionId = (Integer) tableModel.getValueAt(row, 0);
        String userName = (String) tableModel.getValueAt(row, 3);
        String resourceName = (String) tableModel.getValueAt(row, 1);

        try {
            Bill bill = billingService.stopUsage(sessionId);
            refresh();
            mainFrame.refreshAll();
            showStatus("Session stopped. Bill generated for " + userName + ".", UITheme.SUCCESS);
            showBillDialog(bill);
        } catch (BillingException be) {
            showStatus(be.getMessage(), UITheme.DANGER);
        } catch (SQLException ex) {
            showStatus("Database error: " + ex.getMessage(), UITheme.DANGER);
        }
    }

    private void showBillDialog(Bill bill) {
        String html = "<html><div style='font-family:SansSerif; font-size:12px;'>"
                + "<h2 style='margin-bottom:4px;'>Bill Generated</h2>"
                + "<b>Resource:</b> " + bill.getResourceName() + "<br>"
                + "<b>Service:</b> " + bill.getServiceName() + "<br>"
                + "<b>User:</b> " + bill.getUserName() + "<br>"
                + "<b>Start:</b> " + bill.getStartTime() + "<br>"
                + "<b>End:</b> " + bill.getEndTime() + "<br>"
                + "<b>Duration:</b> " + bill.getDurationMinutes() + " minute(s)<br>"
                + "<b>Billed hours (rounded up):</b> " + bill.getBilledHours() + "<br>"
                + "<hr><h3>Total Amount: " + INR.format(bill.getAmount()) + "</h3>"
                + "</div></html>";
        JOptionPane.showMessageDialog(this, new JLabel(html), "Usage Stopped - Bill #" + bill.getId(), JOptionPane.INFORMATION_MESSAGE);
    }

    public void refresh() {
        try {
            List<Resource> resources = resourceDAO.findAll();
            Object prevSelected = resourceCombo.getSelectedItem();
            resourceCombo.removeAllItems();
            for (Resource r : resources) {
                resourceCombo.addItem(r);
            }
            if (prevSelected != null) {
                for (int i = 0; i < resourceCombo.getItemCount(); i++) {
                    Resource item = resourceCombo.getItemAt(i);
                    if (item.getId() == ((Resource) prevSelected).getId()) {
                        resourceCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
            onResourceSelected();

            List<UsageSession> sessions = usageSessionDAO.findAllActive();
            activeSessions.clear();
            planCache.clear();
            for (UsageSession s : sessions) {
                activeSessions.put(s.getId(), s);
            }
            renderActiveTable();
        } catch (SQLException ex) {
            showStatus("Failed to refresh: " + ex.getMessage(), UITheme.DANGER);
        }
    }

    private void renderActiveTable() {
        int selectedId = table.getSelectedRow() >= 0 ? (Integer) tableModel.getValueAt(table.getSelectedRow(), 0) : -1;
        tableModel.setRowCount(0);
        LocalDateTime now = LocalDateTime.now();
        for (UsageSession s : activeSessions.values()) {
            Duration elapsed = Duration.between(s.getStartTime(), now);
            String elapsedStr = formatDuration(elapsed);
            String estimate = estimateCost(s, elapsed);
            tableModel.addRow(new Object[]{
                    s.getId(), s.getResourceName(), s.getServiceName(), s.getUserName(),
                    s.getStartTime().format(DISPLAY_FMT), elapsedStr, estimate
            });
        }
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if ((Integer) tableModel.getValueAt(i, 0) == selectedId) {
                table.setRowSelectionInterval(i, i);
                break;
            }
        }
    }

    private String estimateCost(UsageSession s, Duration elapsed) {
        try {
            ServicePlan plan = planCache.computeIfAbsent(s.getServiceId(), id -> {
                try {
                    return serviceDAO.findById(id);
                } catch (SQLException e) {
                    return null;
                }
            });
            if (plan == null) {
                return "-";
            }
            long seconds = Math.max(elapsed.getSeconds(), 0);
            int billedHours = seconds == 0 ? 1 : (int) Math.ceil(seconds / 3600.0);
            double amount = plan.getFirstHourPrice();
            if (billedHours > 1) {
                amount += (billedHours - 1) * plan.getAdditionalHourPrice();
            }
            return INR.format(amount) + " (" + billedHours + "h billed)";
        } catch (Exception e) {
            return "-";
        }
    }

    private String formatDuration(Duration d) {
        long totalSeconds = Math.max(d.getSeconds(), 0);
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private void startTicker() {
        tickTimer = new Timer(1000, e -> {
            if (!activeSessions.isEmpty()) {
                renderActiveTable();
            }
        });
        tickTimer.start();
    }

    private void showStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }
}
