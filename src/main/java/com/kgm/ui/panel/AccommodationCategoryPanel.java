package com.kgm.ui.panel;

import com.kgm.ui.styling.AccommodationManagementHelper;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AccommodationCategoryPanel extends JPanel {
    private final JTextField categoryNameField = new JTextField("");
    private final DefaultListModel<String> categoryModel = new DefaultListModel<>();
    private final JList<String> categoryList = new JList<>(categoryModel);
    private final Consumer<List<String>> onCategoriesChanged;

    public AccommodationCategoryPanel(Consumer<List<String>> onCategoriesChanged) {
        this.onCategoriesChanged = onCategoriesChanged;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        categoryModel.addElement("Rooms");
        categoryModel.addElement("Suites");
        categoryModel.addElement("Guest House");

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints gbc = AccommodationManagementHelper.formConstraints();

        AccommodationManagementHelper.addField(form, gbc, 0, 0, "Category Name", categoryNameField);

        JPanel actions = AccommodationManagementHelper.actionsPanel();
        JButton add = new JButton("Save Category");
        JButton clear = new JButton("Cancel");
        AccommodationManagementHelper.styleSecondary(clear);
        AccommodationManagementHelper.stylePrimary(add);
        actions.add(clear);
        actions.add(add);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        form.add(actions, gbc);

        AccommodationManagementHelper.styleField(categoryList);
        categoryList.setVisibleRowCount(4);
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        add.addActionListener(e -> saveCategory());
        clear.addActionListener(e -> categoryNameField.setText(""));

        add(form, BorderLayout.NORTH);
        add(new JScrollPane(categoryList), BorderLayout.CENTER);
        notifyCategoriesChanged();
    }

    private void saveCategory() {
        String category = categoryNameField.getText().trim();
        if (category.isEmpty()) {
            return;
        }
        categoryModel.addElement(category);
        categoryNameField.setText("");
        notifyCategoriesChanged();
    }

    private void notifyCategoriesChanged() {
        List<String> categories = new ArrayList<>();
        for (int i = 0; i < categoryModel.size(); i++) {
            categories.add(categoryModel.getElementAt(i));
        }
        onCategoriesChanged.accept(categories);
    }
}
