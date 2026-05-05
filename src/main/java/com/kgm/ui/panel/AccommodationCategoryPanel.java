package com.kgm.ui.panel;

import com.kgm.ui.styling.AccommodationManagementHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AccommodationCategoryPanel extends JPanel {
    private final JTextField categoryNameField = AccommodationManagementHelper.placeholderField("Category Name");
    private final UniversalTablePanel categoryTable = new UniversalTablePanel(
            new String[]{"Category Name", "Actions"},
            "No categories added yet."
    );
    private final Consumer<List<String>> onCategoriesChanged;
    private final JButton saveButton = AccommodationManagementHelper.textButton("SAVE");
    private final JButton updateButton = AccommodationManagementHelper.textButton("UPDATE");
    private final JButton deleteButton = AccommodationManagementHelper.dangerTextButton("DELETE");
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

        categoryTable.setHugRows(true);
        categoryTable.setPaginationEnabled(false);
        categoryTable.setActionColumn(1, "Edit", this::selectCategory);

        body.add(createInlineForm(), BorderLayout.NORTH);
        body.add(categoryTable, BorderLayout.CENTER);

        card.add(body, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        addCategory("Rooms");
        addCategory("Suites");
        addCategory("Guest House");
        clearForm();
        notifyCategoriesChanged();
    }

    private JPanel createInlineForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.X_AXIS));
        form.setOpaque(false);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton cancelButton = AccommodationManagementHelper.textButton("CANCEL");

        form.add(AccommodationManagementHelper.styleField(categoryNameField));
        form.add(Box.createHorizontalStrut(12));
        form.add(cancelButton);
        form.add(Box.createHorizontalStrut(2));
        form.add(saveButton);
        form.add(Box.createHorizontalStrut(2));
        form.add(updateButton);
        form.add(Box.createHorizontalStrut(2));
        form.add(deleteButton);

        cancelButton.addActionListener(e -> clearForm());
        saveButton.addActionListener(e -> saveCategory());
        updateButton.addActionListener(e -> updateCategory());
        deleteButton.addActionListener(e -> deleteCategory());
        categoryNameField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                updateActionStates();
            }

            public void removeUpdate(DocumentEvent event) {
                updateActionStates();
            }

            public void changedUpdate(DocumentEvent event) {
                updateActionStates();
            }
        });
        return form;
    }

    private void selectCategory(int row) {
        editingRow = row;
        categoryNameField.setText(String.valueOf(categoryTable.getValueAt(row, 0)));
        updateActionStates();
    }

    private void saveCategory() {
        String category = categoryNameField.getText().trim();
        if (category.isEmpty()) {
            return;
        }
        addCategory(category);
        clearForm();
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
        categoryTable.updateRow(editingRow, new Object[]{category, "Edit"});
        clearForm();
        notifyCategoriesChanged();
    }

    private void deleteCategory() {
        if (editingRow < 0) {
            return;
        }
        categoryTable.removeRow(editingRow);
        clearForm();
        notifyCategoriesChanged();
    }

    private void addCategory(String category) {
        categoryTable.addRow(new Object[]{category, "Edit"});
    }

    private void clearForm() {
        editingRow = -1;
        categoryNameField.setText("");
        categoryTable.clearSelection();
        updateActionStates();
    }

    private void updateActionStates() {
        boolean hasCategoryName = !categoryNameField.getText().trim().isEmpty();
        boolean editing = editingRow >= 0;

        AccommodationManagementHelper.setTextButtonEnabled(saveButton, hasCategoryName && !editing);
        AccommodationManagementHelper.setTextButtonEnabled(updateButton, hasCategoryName && editing);
        AccommodationManagementHelper.setDangerTextButtonEnabled(deleteButton, editing);
    }

    private void notifyCategoriesChanged() {
        List<String> categories = new ArrayList<>();
        for (int i = 0; i < categoryTable.getRowCount(); i++) {
            categories.add(String.valueOf(categoryTable.getValueAt(i, 0)));
        }
        onCategoriesChanged.accept(categories);
    }
}
