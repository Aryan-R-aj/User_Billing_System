package com.ubs.ui;

import com.ubs.dao.ResourceDAO;
import com.ubs.dao.ServiceDAO;
import com.ubs.model.Resource;
import com.ubs.model.ServicePlan;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;


public class ServicePanel extends JPanel {

    private final ResourceDAO resourceDAO = new ResourceDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final MainFrame mainFrame;
    private static final DecimalFormat INR = new DecimalFormat("\u20B90.00");

    private JComboBox<Resource> resourceCombo;
    private JTextField serviceNameField;
    private JSpinner firstHourSpinner;
    private JSpinner additionalHourSpinner;
    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel statusLabel;

    public ServicePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(0, 16));
        setBorder(new EmptyBorder(20, 24, 20, 24));
        setBackground(UITheme.BG);

        add(buildForm(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);

        refresh();
    }

    private JComponent buildForm() {
        JPanel card = UITheme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = UITheme.sectionTitle("Add Pricing Plan (Service) for a Resource");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        resourceCombo = new JComboBox<>();
        serviceNameField = new JTextField(14);
        firstHourSpinner = new JSpinner(new SpinnerNumberModel(30.0, 0.0, 1000000.0, 5.0));
        additionalHourSpinner = new JSpinner(new SpinnerNumberModel(10.0, 0.0, 1000000.0, 5.0));

        row.add(labeled("Resource", resourceCombo));
        row.add(labeled("Service name", serviceNameField));
        row.add(labeled("First hour price (\u20B9)", firstHourSpinner));
        row.add(labeled("Additional hour price (\u20B9)", additionalHourSpinner));

        JButton addBtn = UITheme.primaryButton("+ Add Service");
        addBtn.addActionListener(e -> onAdd());
        row.add(Box.createHorizontalStrut(8));
        row.add(addBtn);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(UITheme.FONT_BODY);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(title);
        card.add(Box.createVerticalStrut(8));
        card.add(row);
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
        field.setMaximumSize(new Dimension(190, 30));
        p.add(l);
        p.add(field);
        return p;
    }

    private JComponent buildTable() {
        JPanel card = UITheme.card();
        card.setLayout(new BorderLayout(0, 10));

        JLabel title = UITheme.sectionTitle("Defined Services & Pricing");
        card.add(title, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Resource", "Service", "First Hour", "Additional Hour"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(UITheme.FONT_BODY);
        table.getTableHeader().setFont(UITheme.FONT_BODY_BOLD);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        card.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);
        JButton deleteBtn = UITheme.dangerButton("Delete Selected");
        deleteBtn.addActionListener(e -> onDelete());
        JButton refreshBtn = UITheme.secondaryButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());
        footer.add(refreshBtn);
        footer.add(deleteBtn);
        card.add(footer, BorderLayout.SOUTH);

        return card;
    }

    private void onAdd() {
        Resource selected = (Resource) resourceCombo.getSelectedItem();
        String serviceName = serviceNameField.getText().trim();
        double firstHour = (Double) firstHourSpinner.getValue();
        double additionalHour = (Double) additionalHourSpinner.getValue();

        if (selected == null) {
            showStatus("Add a resource first (see Resources tab).", UITheme.DANGER);
            return;
        }
        if (serviceName.isEmpty()) {
            showStatus("Service name is required.", UITheme.DANGER);
            return;
        }
        try {
            serviceDAO.insert(selected.getId(), serviceName, firstHour, additionalHour);
            serviceNameField.setText("");
            firstHourSpinner.setValue(30.0);
            additionalHourSpinner.setValue(10.0);
            showStatus("Service \"" + serviceName + "\" added for " + selected.getName() + ".", UITheme.SUCCESS);
            refresh();
            mainFrame.refreshAll();
        } catch (SQLException ex) {
            showStatus("Database error: " + ex.getMessage(), UITheme.DANGER);
        }
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) {
            showStatus("Select a service to delete first.", UITheme.WARNING);
            return;
        }
        int id = (Integer) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 2);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete service \"" + name + "\"?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            serviceDAO.delete(id);
            showStatus("Service deleted.", UITheme.SUCCESS);
            refresh();
            mainFrame.refreshAll();
        } catch (SQLException ex) {
            showStatus("Could not delete: " + ex.getMessage(), UITheme.DANGER);
        }
    }

    public void refresh() {
        try {
            List<Resource> resources = resourceDAO.findAll();
            resourceCombo.removeAllItems();
            for (Resource r : resources) {
                resourceCombo.addItem(r);
            }

            List<ServicePlan> plans = serviceDAO.findAll();
            tableModel.setRowCount(0);
            for (ServicePlan p : plans) {
                tableModel.addRow(new Object[]{
                        p.getId(), p.getResourceName(), p.getServiceName(),
                        INR.format(p.getFirstHourPrice()), INR.format(p.getAdditionalHourPrice())
                });
            }
        } catch (SQLException ex) {
            showStatus("Failed to load services: " + ex.getMessage(), UITheme.DANGER);
        }
    }

    private void showStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }
}
