package com.ubs.ui;

import com.ubs.dao.ResourceDAO;
import com.ubs.model.Resource;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Tab for defining resources/entities (meeting rooms, gym machines,
 * workstations...) and their capacity. Includes a full "Add Resource"
 * form plus a live table showing current occupancy for every resource.
 */
public class ResourcePanel extends JPanel {

    private final ResourceDAO resourceDAO = new ResourceDAO();
    private final MainFrame mainFrame;

    private JTextField nameField;
    private JTextField categoryField;
    private JSpinner capacitySpinner;
    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel statusLabel;

    public ResourcePanel(MainFrame mainFrame) {
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

        JLabel title = UITheme.sectionTitle("Add New Resource");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        nameField = new JTextField(16);
        categoryField = new JTextField(14);
        capacitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));

        row.add(labeled("Resource name", nameField));
        row.add(labeled("Category (optional)", categoryField));
        row.add(labeled("Capacity", capacitySpinner));

        JButton addBtn = UITheme.primaryButton("+ Add Resource");
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
        field.setMaximumSize(new Dimension(220, 30));
        p.add(l);
        p.add(field);
        return p;
    }

    private JComponent buildTable() {
        JPanel card = UITheme.card();
        card.setLayout(new BorderLayout(0, 10));

        JLabel title = UITheme.sectionTitle("Resources & Live Capacity");
        card.add(title, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Name", "Category", "Capacity", "In Use", "Available", "Status"}, 0) {
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
        String name = nameField.getText().trim();
        String category = categoryField.getText().trim();
        int capacity = (Integer) capacitySpinner.getValue();

        if (name.isEmpty()) {
            showStatus("Resource name is required.", UITheme.DANGER);
            return;
        }
        try {
            if (resourceDAO.existsByName(name)) {
                showStatus("A resource named \"" + name + "\" already exists.", UITheme.DANGER);
                return;
            }
            resourceDAO.insert(name, category.isEmpty() ? null : category, capacity);
            nameField.setText("");
            categoryField.setText("");
            capacitySpinner.setValue(1);
            showStatus("Resource \"" + name + "\" added successfully.", UITheme.SUCCESS);
            refresh();
            mainFrame.refreshAll();
        } catch (SQLException ex) {
            showStatus("Database error: " + ex.getMessage(), UITheme.DANGER);
        }
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) {
            showStatus("Select a resource to delete first.", UITheme.WARNING);
            return;
        }
        int id = (Integer) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete resource \"" + name + "\"? This also removes its services, usage sessions and bill history.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            resourceDAO.delete(id);
            showStatus("Resource \"" + name + "\" deleted.", UITheme.SUCCESS);
            refresh();
            mainFrame.refreshAll();
        } catch (SQLException ex) {
            showStatus("Could not delete: " + ex.getMessage(), UITheme.DANGER);
        }
    }

    public void refresh() {
        try {
            List<Resource> resources = resourceDAO.findAll();
            tableModel.setRowCount(0);
            for (Resource r : resources) {
                String status = r.isFull() ? "FULL" : "AVAILABLE";
                tableModel.addRow(new Object[]{
                        r.getId(), r.getName(),
                        r.getCategory() == null ? "-" : r.getCategory(),
                        r.getCapacity(), r.getActiveCount(), r.getAvailableSlots(), status
                });
            }
        } catch (SQLException ex) {
            showStatus("Failed to load resources: " + ex.getMessage(), UITheme.DANGER);
        }
    }

    private void showStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }
}
