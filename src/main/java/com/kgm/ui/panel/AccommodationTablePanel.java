package com.kgm.ui.panel;

import com.kgm.ui.styling.AccommodationManagementHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;

public class AccommodationTablePanel extends JPanel {
    private final AccommodationTableModel tableModel = new AccommodationTableModel();
    private final JTable table = new JTable(tableModel);
    private final BiConsumer<Integer, AccommodationRecord> onEdit;

    public AccommodationTablePanel(BiConsumer<Integer, AccommodationRecord> onEdit) {
        this.onEdit = onEdit;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        seedDummyData();
        AccommodationManagementHelper.styleTable(table);
        styleActionsColumn();
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                int row = table.rowAtPoint(event.getPoint());
                int column = table.columnAtPoint(event.getPoint());
                if (row < 0 || column != table.getColumnCount() - 1) {
                    return;
                }
                int modelRow = table.convertRowIndexToModel(row);
                onEdit.accept(modelRow, tableModel.getAccommodation(modelRow));
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void addAccommodation(AccommodationRecord accommodation) {
        tableModel.addAccommodation(accommodation);
    }

    public void updateAccommodation(int row, AccommodationRecord accommodation) {
        tableModel.updateAccommodation(row, accommodation);
    }

    private void seedDummyData() {
        tableModel.addAccommodation(new AccommodationRecord(
                "Room I", "Rooms", 2, "Available", "Ground Floor", "Standard guest room near reception"
        ));
        tableModel.addAccommodation(new AccommodationRecord(
                "Executive Suite", "Suites", 4, "Reserved", "First Floor", "Large suite for senior visitors"
        ));
        tableModel.addAccommodation(new AccommodationRecord(
                "Guest House A", "Guest House", 8, "Under Maintenance", "North Block", "Independent unit with lounge"
        ));
    }

    private void styleActionsColumn() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setText("Edit");
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setForeground(AccommodationManagementHelper.PRIMARY);
                label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
                label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return label;
            }
        };
        table.getColumnModel().getColumn(table.getColumnCount() - 1).setCellRenderer(renderer);
        table.getColumnModel().getColumn(table.getColumnCount() - 1).setPreferredWidth(90);
    }
}
