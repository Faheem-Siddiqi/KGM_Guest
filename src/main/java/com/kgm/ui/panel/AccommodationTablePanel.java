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
    private final JPanel card;
    private final JPanel content = new JPanel(new BorderLayout());
    private final BiConsumer<Integer, AccommodationRecord> onEdit;

    public AccommodationTablePanel(BiConsumer<Integer, AccommodationRecord> onEdit) {
        this.onEdit = onEdit;
        setLayout(new BorderLayout());
        setOpaque(false);

        card = AccommodationManagementHelper.sectionCard(
                "Accommodation List",
                "All created accommodations appear here with quick edit access."
        );
        content.setOpaque(false);

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

        card.add(content, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
        refreshContent();
    }

    public void addAccommodation(AccommodationRecord accommodation) {
        tableModel.addAccommodation(accommodation);
        refreshContent();
    }

    public void updateAccommodation(int row, AccommodationRecord accommodation) {
        tableModel.updateAccommodation(row, accommodation);
        refreshContent();
    }

    private void refreshContent() {
        content.removeAll();
        if (tableModel.getRowCount() == 0) {
            content.add(AccommodationManagementHelper.emptyState("No accommodation records yet. Save the form above to create one."), BorderLayout.CENTER);
        } else {
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.getViewport().setBackground(Color.WHITE);
            content.add(scrollPane, BorderLayout.CENTER);
        }
        content.revalidate();
        content.repaint();
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
        table.getColumnModel().getColumn(table.getColumnCount() - 1).setPreferredWidth(80);
    }
}
