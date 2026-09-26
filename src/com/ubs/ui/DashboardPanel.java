package com.ubs.ui;

import com.ubs.dao.BillDAO;
import com.ubs.dao.ResourceDAO;
import com.ubs.model.Bill;
import com.ubs.model.Resource;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;


public class DashboardPanel extends JPanel {

    private static final DecimalFormat INR = new DecimalFormat("\u20B90.00");

    private final ResourceDAO resourceDAO = new ResourceDAO();
    private final BillDAO billDAO = new BillDAO();

    private JPanel statsRow;
    private JPanel resourceGrid;

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 16));
        setBorder(new EmptyBorder(20, 24, 20, 24));
        setBackground(UITheme.BG);

        JLabel heading = new JLabel("Usage & Billing Dashboard");
        heading.setFont(UITheme.FONT_H1);
        add(heading, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        statsRow = new JPanel(new GridLayout(1, 4, 14, 0));
        statsRow.setOpaque(false);
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JLabel resourcesTitle = UITheme.sectionTitle("Resource Occupancy");
        resourcesTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        resourcesTitle.setBorder(new EmptyBorder(20, 0, 8, 0));

        resourceGrid = new JPanel(new GridLayout(0, 3, 14, 14));
        resourceGrid.setOpaque(false);
        resourceGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane scroll = new JScrollPane(wrapTop(resourceGrid));
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        center.add(statsRow);
        center.add(resourcesTitle);
        center.add(scroll);

        add(center, BorderLayout.CENTER);

        refresh();
    }

    private JPanel wrapTop(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(c, BorderLayout.NORTH);
        return p;
    }

    private JPanel statCard(String label, String value, Color accent) {
        JPanel card = UITheme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        valueLabel.setForeground(accent);
        JLabel textLabel = new JLabel(label);
        textLabel.setFont(UITheme.FONT_BODY);
        textLabel.setForeground(UITheme.TEXT_MUTED);
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(textLabel);
        return card;
    }

    private JPanel resourceCard(Resource r) {
        JPanel card = UITheme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(r.getName());
        name.setFont(UITheme.FONT_BODY_BOLD);

        JLabel cat = new JLabel(r.getCategory() == null ? "General" : r.getCategory());
        cat.setFont(UITheme.FONT_BODY);
        cat.setForeground(UITheme.TEXT_MUTED);

        JProgressBar bar = new JProgressBar(0, r.getCapacity());
        bar.setValue(r.getActiveCount());
        bar.setStringPainted(true);
        bar.setString(r.getActiveCount() + " / " + r.getCapacity() + " in use");
        bar.setForeground(r.isFull() ? UITheme.DANGER : UITheme.SUCCESS);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel status = new JLabel(r.isFull() ? "FULL - requests rejected" : r.getAvailableSlots() + " slot(s) available");
        status.setFont(UITheme.FONT_BODY);
        status.setForeground(r.isFull() ? UITheme.DANGER : UITheme.SUCCESS);

        card.add(name);
        card.add(cat);
        card.add(Box.createVerticalStrut(10));
        card.add(bar);
        card.add(Box.createVerticalStrut(6));
        card.add(status);
        return card;
    }

    public void refresh() {
        try {
            List<Resource> resources = resourceDAO.findAll();
            List<Bill> bills = billDAO.findAll();

            int totalResources = resources.size();
            int totalCapacity = resources.stream().mapToInt(Resource::getCapacity).sum();
            int activeUsers = resources.stream().mapToInt(Resource::getActiveCount).sum();
            double totalRevenue = bills.stream().mapToDouble(Bill::getAmount).sum();

            statsRow.removeAll();
            statsRow.add(statCard("Total Resources", String.valueOf(totalResources), UITheme.PRIMARY));
            statsRow.add(statCard("Currently In Use", activeUsers + " / " + totalCapacity, UITheme.WARNING));
            statsRow.add(statCard("Bills Generated", String.valueOf(bills.size()), UITheme.PRIMARY));
            statsRow.add(statCard("Total Revenue", INR.format(totalRevenue), UITheme.SUCCESS));

            resourceGrid.removeAll();
            if (resources.isEmpty()) {
                JLabel empty = new JLabel("No resources defined yet. Add one from the Resources tab.");
                empty.setFont(UITheme.FONT_BODY);
                empty.setForeground(UITheme.TEXT_MUTED);
                resourceGrid.add(empty);
            } else {
                for (Resource r : resources) {
                    resourceGrid.add(resourceCard(r));
                }
            }

            revalidate();
            repaint();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed to load dashboard: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
