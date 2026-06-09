package com.kgm.ui.panel;

import com.kgm.dao.AccommodationCategoryDao;
import com.kgm.ui.DatabaseSetupView;
import com.kgm.ui.component.PendingButtonState;
import com.kgm.ui.styling.AccommodationManagementHelper;
import com.kgm.ui.styling.DialogHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class AccommodationCategoryPanel extends JPanel {
    private final JTextField categoryNameField = AccommodationManagementHelper.placeholderField("Category Name");
    private final UniversalTablePanel categoryTable = new UniversalTablePanel(
            new String[]{"Category Name", "Actions"},
            "No categories added yet."
    );
    private final Consumer<List<String>> onCategoriesChanged;
    private final AccommodationCategoryDao categoryDao = new AccommodationCategoryDao();
    private final JButton cancelButton = AccommodationManagementHelper.textButton("CANCEL");
    private final JButton saveButton = AccommodationManagementHelper.textButton("SAVE");
    private final JButton updateButton = AccommodationManagementHelper.textButton("UPDATE");
    private final JButton deleteButton = AccommodationManagementHelper.dangerTextButton("DELETE");
    private int editingRow = -1;
    private boolean actionInProgress;

    public AccommodationCategoryPanel(Consumer<List<String>> onCategoriesChanged) {
        this.onCategoriesChanged = onCategoriesChanged;
        setLayout(new GridBagLayout());
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

        GridBagConstraints cardConstraints = new GridBagConstraints();
        cardConstraints.gridx = 0;
        cardConstraints.gridy = 0;
        cardConstraints.anchor = GridBagConstraints.NORTH;
        add(card, cardConstraints);

        loadCategories();
    }

    private JPanel createInlineForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.X_AXIS));
        form.setOpaque(false);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);

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
        updateActionStates();
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
        runCategoryAction(saveButton, "SAVING...", "Category not saved", () -> categoryDao.save(category));
    }

    private void updateCategory() {
        if (editingRow < 0) {
            return;
        }
        String category = categoryNameField.getText().trim();
        if (category.isEmpty()) {
            return;
        }
        String oldCategory = String.valueOf(categoryTable.getValueAt(editingRow, 0));
        runCategoryAction(
                updateButton,
                "UPDATING...",
                "Category not updated",
                () -> categoryDao.updateName(oldCategory, category)
        );
    }

    private void deleteCategory() {
        if (editingRow < 0) {
            return;
        }
        String category = String.valueOf(categoryTable.getValueAt(editingRow, 0));
        runCategoryAction(deleteButton, "DELETING...", "Category not deleted", () -> categoryDao.deleteByName(category));
    }

    private void runCategoryAction(
            JButton sourceButton,
            String pendingText,
            String errorTitle,
            DatabaseAction action
    ) {
        if (actionInProgress) {
            return;
        }
        actionInProgress = true;
        updateActionStates();
        PendingButtonState pending = PendingButtonState.start(sourceButton, pendingText);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                action.run();
                return null;
            }

            @Override
            protected void done() {
                boolean success = false;
                try {
                    get();
                    success = true;
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    Throwable failure = cause == null ? exception : cause;
                    if (DatabaseSetupView.showIfConnectionFailure(failure)) {
                        return;
                    }
                    DialogHelper.error(AccommodationCategoryPanel.this, errorTitle, failure.getMessage());
                } finally {
                    actionInProgress = false;
                    pending.restore();
                    if (success) {
                        loadCategories();
                        clearForm();
                    } else {
                        updateActionStates();
                    }
                }
            }
        }.execute();
    }

    private void addCategory(String category) {
        categoryTable.addRow(new Object[]{category, "Edit"});
    }

    private void loadCategories() {
        try {
            categoryTable.clearRows();
            for (String category : categoryDao.findActiveNames()) {
                addCategory(category);
            }
            notifyCategoriesChanged();
        } catch (SQLException exception) {
            if (DatabaseSetupView.showIfConnectionFailure(exception)) {
                return;
            }
            DialogHelper.error(this, "Categories not loaded", exception.getMessage());
        }
    }

    private void clearForm() {
        editingRow = -1;
        categoryNameField.setText("");
        categoryTable.clearSelection();
        updateActionStates();
    }

    private void updateActionStates() {
        if (actionInProgress) {
            AccommodationManagementHelper.setTextButtonEnabled(saveButton, false);
            AccommodationManagementHelper.setTextButtonEnabled(updateButton, false);
            AccommodationManagementHelper.setTextButtonEnabled(cancelButton, false);
            AccommodationManagementHelper.setDangerTextButtonEnabled(deleteButton, false);
            return;
        }
        boolean hasCategoryName = !categoryNameField.getText().trim().isEmpty();
        boolean editing = editingRow >= 0;

        AccommodationManagementHelper.setTextButtonEnabled(saveButton, hasCategoryName && !editing);
        AccommodationManagementHelper.setTextButtonEnabled(updateButton, hasCategoryName && editing);
        AccommodationManagementHelper.setTextButtonEnabled(cancelButton, hasCategoryName || editing);
        AccommodationManagementHelper.setDangerTextButtonEnabled(deleteButton, editing);
    }

    private interface DatabaseAction {
        void run() throws SQLException;
    }

    private void notifyCategoriesChanged() {
        List<String> categories = new ArrayList<>();
        for (int i = 0; i < categoryTable.getRowCount(); i++) {
            categories.add(String.valueOf(categoryTable.getValueAt(i, 0)));
        }
        onCategoriesChanged.accept(categories);
    }
}
