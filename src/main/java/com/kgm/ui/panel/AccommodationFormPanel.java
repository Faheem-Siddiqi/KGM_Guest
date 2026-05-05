package com.kgm.ui.panel;

import com.kgm.ui.styling.AccommodationManagementHelper;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AccommodationFormPanel extends JPanel {
    private final JTextField nameField = new JTextField("");
    private final JComboBox<String> categoryCombo = AccommodationManagementHelper.combo("Rooms", "Suites", "Guest House");
    private final JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(2, 1, 100, 1));
    private final JComboBox<String> statusCombo = AccommodationManagementHelper.combo(
            "Available", "Under Maintenance", "Occupied", "Reserved"
    );
    private final JTextField locationField = new JTextField("");
    private final JTextArea descriptionArea = AccommodationManagementHelper.descriptionArea("N/A");

    private final Consumer<AccommodationRecord> onSave;
    private final BiConsumer<Integer, AccommodationRecord> onUpdate;
    private int editingRow = -1;

    public AccommodationFormPanel(Consumer<AccommodationRecord> onSave, BiConsumer<Integer, AccommodationRecord> onUpdate) {
        this.onSave = onSave;
        this.onUpdate = onUpdate;
        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);
        buildForm();
    }

    public void setCategories(List<String> categories) {
        Object selected = categoryCombo.getSelectedItem();
        categoryCombo.removeAllItems();
        for (String category : categories) {
            categoryCombo.addItem(category);
        }
        if (selected != null) {
            categoryCombo.setSelectedItem(selected);
        }
        if (categoryCombo.getSelectedIndex() < 0 && categoryCombo.getItemCount() > 0) {
            categoryCombo.setSelectedIndex(0);
        }
    }

    public void editAccommodation(int row, AccommodationRecord accommodation) {
        editingRow = row;
        nameField.setText(accommodation.getName());
        categoryCombo.setSelectedItem(accommodation.getCategory());
        capacitySpinner.setValue(accommodation.getCapacity());
        statusCombo.setSelectedItem(accommodation.getStatus());
        locationField.setText(accommodation.getLocation());
        descriptionArea.setText(accommodation.getDescription());
    }

    private void buildForm() {
        GridBagConstraints gbc = AccommodationManagementHelper.formConstraints();

        AccommodationManagementHelper.addField(this, gbc, 0, 0, "Name", nameField);
        AccommodationManagementHelper.addField(this, gbc, 0, 2, "Category", categoryCombo);
        AccommodationManagementHelper.addField(this, gbc, 1, 0, "Capacity", capacitySpinner);
        AccommodationManagementHelper.addField(this, gbc, 1, 2, "Status", statusCombo);
        AccommodationManagementHelper.addField(this, gbc, 2, 0, "Location", locationField);

        JLabel descriptionLabel = AccommodationManagementHelper.label("Description");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 4;
        add(descriptionLabel, gbc);

        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipady = 70;
        add(descriptionArea, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.ipady = 0;

        JPanel actions = AccommodationManagementHelper.actionsPanel();
        JButton cancel = new JButton("Cancel");
        JButton update = new JButton("Update");
        JButton save = new JButton("Save");
        AccommodationManagementHelper.styleSecondary(cancel);
        AccommodationManagementHelper.styleSecondary(update);
        AccommodationManagementHelper.stylePrimary(save);
        actions.add(cancel);
        actions.add(update);
        actions.add(save);

        gbc.gridy = 5;
        gbc.gridwidth = 4;
        add(actions, gbc);
        gbc.gridwidth = 1;

        cancel.addActionListener(e -> clearForm());
        save.addActionListener(e -> saveAccommodation());
        update.addActionListener(e -> updateAccommodation());
    }

    private void saveAccommodation() {
        AccommodationRecord accommodation = collectAccommodation();
        if (accommodation == null) {
            return;
        }
        onSave.accept(accommodation);
        clearForm();
    }

    private void updateAccommodation() {
        if (editingRow < 0) {
            JOptionPane.showMessageDialog(this, "Select Edit from the table before updating.", "No row selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        AccommodationRecord accommodation = collectAccommodation();
        if (accommodation == null) {
            return;
        }
        onUpdate.accept(editingRow, accommodation);
        clearForm();
    }

    private AccommodationRecord collectAccommodation() {
        String name = nameField.getText().trim();
        String category = String.valueOf(categoryCombo.getSelectedItem()).trim();
        int capacity = (Integer) capacitySpinner.getValue();
        String status = String.valueOf(statusCombo.getSelectedItem()).trim();
        String location = locationField.getText().trim();
        String description = descriptionArea.getText().trim();

        StringBuilder errors = new StringBuilder();
        if (name.isEmpty()) {
            errors.append("Name is required.\n");
        }
        if (category.isEmpty()) {
            errors.append("Category is required.\n");
        }
        if (status.isEmpty()) {
            errors.append("Status is required.\n");
        }
        if (location.isEmpty()) {
            errors.append("Location is required.\n");
        }
        if (description.isEmpty()) {
            errors.append("Description is required.\n");
        }
        if (errors.length() > 0) {
            JOptionPane.showMessageDialog(this, errors.toString(), "Please complete required fields", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        return new AccommodationRecord(name, category, capacity, status, location, description);
    }

    private void clearForm() {
        editingRow = -1;
        nameField.setText("");
        if (categoryCombo.getItemCount() > 0) {
            categoryCombo.setSelectedIndex(0);
        }
        capacitySpinner.setValue(1);
        statusCombo.setSelectedIndex(0);
        locationField.setText("");
        descriptionArea.setText("N/A");
    }
}
