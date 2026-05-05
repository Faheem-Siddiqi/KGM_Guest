package com.kgm.ui.panel;

import com.kgm.ui.styling.AccommodationManagementHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AccommodationCategoryPanel extends JPanel {
    private final JTextField categoryNameField = new JTextField("");
    private final DefaultTableModel categoryModel = new DefaultTableModel(new Object[]{"Category Name", "Actions"}, 0);
    private final JTable categoryTable = new JTable(categoryModel);
    private final Consumer<List<String>> onCategoriesChanged;
    private int editingRow = -1;

    public AccommodationCategoryPanel(Consumer<List<String>> onCategoriesChanged) {
        this.onCategoriesChanged = onCategoriesChanged;
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel card = AccommodationManagementHelper.sectionCard(
                "Accommodation Categories",
                "Create, update, and delete accommodation categories."
        );
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);

        body.add(createInlineForm(), BorderLayout.NORTH);
        body.add(createTable(), BorderLayout.CENTER);

        card.add(body, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        addCategory("Rooms");
        addCategory("Suites");
        addCategory("Guest House");
        notifyCategoriesChanged();
    }

    private JPanel createInlineForm() {
        JPanel form = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        form.setOpaque(false);

        JButton cancel = AccommodationManagementHelper.textButton("CANCEL");
        JButton save = AccommodationManagementHelper.textButton("SAVE");
        JButton update = AccommodationManagementHelper.textButton("UPDATE");
        JButton delete = AccommodationManagementHelper.dangerTextButton("DELETE");

        form.add(AccommodationManagementHelper.label("Category Name"));
        form.add(AccommodationManagementHelper.styleField(categoryNameField));
        form.add(cancel);
        form.add(save);
        form.add(update);
        form.add(delete);

        cancel.addActionListener(e -> clearForm());
        save.addActionListener(e -> saveCategory());
        update.addActionListener(e -> updateCategory());
        delete.addActionListener(e -> deleteCategory());
        return form;
    }

    private JScrollPane createTable() {
        AccommodationManagementHelper.styleTable(categoryTable);
        styleActionsColumn();
        categoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        updateTableHeight();
        categoryTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                int row = categoryTable.rowAtPoint(event.getPoint());
                int column = categoryTable.columnAtPoint(event.getPoint());
                if (row < 0) {
                    return;
                }

                editingRow = categoryTable.convertRowIndexToModel(row);
                categoryNameField.setText(String.valueOf(categoryModel.getValueAt(editingRow, 0)));
                if (column == categoryTable.getColumnCount() - 1) {
                    categoryTable.setRowSelectionInterval(row, row);
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(categoryTable);
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    private void saveCategory() {
        String category = categoryNameField.getText().trim();
        if (category.isEmpty()) {
            return;
        }
        addCategory(category);
        clearForm();
        updateTableHeight();
        notifyCategoriesChanged();
    }

    private void updateCategory() {
        if (editingRow < 0) {
            return;
        }
        String category = categoryNameField.getText().trim();
        if (category.isEmpty()) {
            return;
        }
        categoryModel.setValueAt(category, editingRow, 0);
        clearForm();
        notifyCategoriesChanged();
    }

    private void deleteCategory() {
        if (editingRow < 0) {
            return;
        }
        categoryModel.removeRow(editingRow);
        clearForm();
        updateTableHeight();
        notifyCategoriesChanged();
    }

    private void addCategory(String category) {
        categoryModel.addRow(new Object[]{category, "Edit"});
        updateTableHeight();
    }

    private void clearForm() {
        editingRow = -1;
        categoryNameField.setText("");
        categoryTable.clearSelection();
    }

    private void notifyCategoriesChanged() {
        List<String> categories = new ArrayList<>();
        for (int i = 0; i < categoryModel.getRowCount(); i++) {
            categories.add(String.valueOf(categoryModel.getValueAt(i, 0)));
        }
        onCategoriesChanged.accept(categories);
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
                return label;
            }
        };
        categoryTable.getColumnModel().getColumn(1).setCellRenderer(renderer);
        categoryTable.getColumnModel().getColumn(1).setPreferredWidth(90);
    }

    private void updateTableHeight() {
        int rows = Math.max(1, categoryModel.getRowCount());
        int headerHeight = categoryTable.getTableHeader().getPreferredSize().height;
        int height = headerHeight + categoryTable.getRowHeight() * rows;
        categoryTable.setPreferredScrollableViewportSize(new Dimension(
                AccommodationManagementHelper.CONTENT_WIDTH - 80,
                height
        ));
        revalidate();
    }
}
