package com.ubs.ui;

import com.ubs.dao.BillDAO;
import com.ubs.model.Bill;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Tab showing the full, immutable billing history with search and CSV export. */
public class BillHistoryPanel extends JPanel {

    private static final DecimalFormat INR = new DecimalFormat("\u20B90.00");

    private final BillDAO billDAO = new BillDAO();

    private DefaultTableModel tableModel;
    private JTable table;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField searchField;
    private JLabel summaryLabel;
    private JLabel statusLabel;

    public BillHistoryPanel() {
        setLayout(new BorderLayout(0, 16));
        setBorder(new EmptyBorder(20, 24, 20, 24));
        setBackground(UITheme.BG);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);

        refresh();
    }

    private JComponent buildHeader() {
        JPanel card = UITheme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = UITheme.sectionTitle("Bill History");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchField = new JTextField(24);
        searchField.addCaretListener(e -> applyFilter());
        row.add(labeled("Search by resource, service or user", searchField));

        JButton exportBtn = UITheme.secondaryButton("\u2B07 Export CSV");
        exportBtn.addActionListener(e -> onExport());
        row.add(Box.createHorizontalStrut(8));
        row.add(exportBtn);

        JButton refreshBtn = UITheme.secondaryButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());
        row.add(refreshBtn);

        summaryLabel = new JLabel(" ");
        summaryLabel.setFont(UITheme.FONT_BODY_BOLD);
        summaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(UITheme.FONT_BODY);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(title);
        card.add(Box.createVerticalStrut(8));
        card.add(row);
        card.add(summaryLabel);
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
        field.setMaximumSize(new Dimension(320, 30));
        p.add(l);
        p.add(field);
        return p;
    }

    private JComponent buildTable() {
        JPanel card = UITheme.card();
        card.setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(
                new Object[]{"Bill #", "Resource", "Service", "User", "Start", "End", "Duration (min)", "Billed Hrs", "Amount"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(UITheme.FONT_BODY);
        table.getTableHeader().setFont(UITheme.FONT_BODY_BOLD);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private void applyFilter() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }
        try {
            // Filter across Resource(1), Service(2) and User(3) columns, case-insensitive.
            RowFilterCombo filter = new RowFilterCombo(text);
            sorter.setRowFilter(filter);
        } catch (PatternSyntaxException ex) {
            sorter.setRowFilter(null);
        }
        updateSummaryForVisibleRows();
    }

    /** Simple case-insensitive substring filter across resource/service/user columns. */
    private static class RowFilterCombo extends javax.swing.RowFilter<DefaultTableModel, Integer> {
        private final Pattern pattern;

        RowFilterCombo(String text) {
            this.pattern = Pattern.compile(Pattern.quote(text), Pattern.CASE_INSENSITIVE);
        }

        @Override
        public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
            for (int col : new int[]{1, 2, 3}) {
                Object val = entry.getValue(col);
                if (val != null && pattern.matcher(val.toString()).find()) {
                    return true;
                }
            }
            return false;
        }
    }

    private void onExport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("bill_history.csv"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        java.io.File file = chooser.getSelectedFile();
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Bill#,Resource,Service,User,Start,End,DurationMinutes,BilledHours,Amount");
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                int modelRow = table.convertRowIndexToModel(i);
                StringBuilder sb = new StringBuilder();
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    if (col > 0) sb.append(',');
                    sb.append(csvEscape(String.valueOf(tableModel.getValueAt(modelRow, col))));
                }
                writer.println(sb);
            }
            showStatus("Exported to " + file.getAbsolutePath(), UITheme.SUCCESS);
        } catch (IOException ex) {
            showStatus("Export failed: " + ex.getMessage(), UITheme.DANGER);
        }
    }

    private String csvEscape(String value) {
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public void refresh() {
        try {
            List<Bill> bills = billDAO.findAll();
            tableModel.setRowCount(0);
            for (Bill b : bills) {
                tableModel.addRow(new Object[]{
                        b.getId(), b.getResourceName(), b.getServiceName(), b.getUserName(),
                        b.getStartTime(), b.getEndTime(), b.getDurationMinutes(), b.getBilledHours(),
                        INR.format(b.getAmount())
                });
            }
            searchField.setText("");
            sorter.setRowFilter(null);
            updateSummary(bills);
        } catch (SQLException ex) {
            showStatus("Failed to load bill history: " + ex.getMessage(), UITheme.DANGER);
        }
    }

    private void updateSummary(List<Bill> bills) {
        double total = bills.stream().mapToDouble(Bill::getAmount).sum();
        summaryLabel.setText(bills.size() + " bill(s) \u2014 total revenue " + INR.format(total));
    }

    private void updateSummaryForVisibleRows() {
        double total = 0;
        int count = table.getRowCount();
        for (int i = 0; i < count; i++) {
            int modelRow = table.convertRowIndexToModel(i);
            String amountStr = String.valueOf(tableModel.getValueAt(modelRow, 8)).replace("\u20B9", "");
            try {
                total += Double.parseDouble(amountStr);
            } catch (NumberFormatException ignored) {
            }
        }
        summaryLabel.setText(count + " bill(s) shown \u2014 total \u20B9" + String.format("%.2f", total));
    }

    private void showStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }
}
