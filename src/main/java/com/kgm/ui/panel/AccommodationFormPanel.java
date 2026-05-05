package com.kgm.ui.panel;

import com.kgm.ui.styling.AccommodationManagementHelper;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AccommodationFormPanel extends JPanel {
    private final JTextField nameField = new JTextField("");
    private final JComboBox<String> categoryCombo = AccommodationManagementHelper.combo();
    private final JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(2, 1, 100, 1));
    private final JComboBox<String> statusCombo = AccommodationManagementHelper.combo(
            "Ready for Assignment", "Temporarily Unavailable", "Under Maintenance", "Reserved"
    );
    private final JTextField assignedStaffField = new JTextField("");
    private final JTextField amenityField = new JTextField("");
    private final JPanel amenitiesListPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    private final List<String> amenities = new ArrayList<>();

    private final Consumer<AccommodationRecord> onSave;
    private final BiConsumer<Integer, AccommodationRecord> onUpdate;
    private int editingRow = -1;

    public AccommodationFormPanel(Consumer<AccommodationRecord> onSave, BiConsumer<Integer, AccommodationRecord> onUpdate) {
        this.onSave = onSave;
        this.onUpdate = onUpdate;
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel card = AccommodationManagementHelper.sectionCard(
                "Accommodation Form",
                "Create a new accommodation or update the selected record."
        );
        card.add(createFormBody(), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
        refreshAmenities();
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
        assignedStaffField.setText(accommodation.getAssignedStaff());
        amenities.clear();
        amenities.addAll(accommodation.getAmenities());
        refreshAmenities();
    }

    private JPanel createFormBody() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints gbc = AccommodationManagementHelper.formConstraints();

        addField(body, gbc, 0, 0, "Name", nameField);
        addField(body, gbc, 0, 1, "Category", categoryCombo);
        addField(body, gbc, 1, 0, "Capacity", capacitySpinner);
        addField(body, gbc, 1, 1, "Status", statusCombo);
        addField(body, gbc, 2, 0, "Assigned Staff", assignedStaffField);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        body.add(createAmenitiesPanel(), gbc);

        JPanel actions = AccommodationManagementHelper.textActionsPanel();
        JButton cancel = AccommodationManagementHelper.textButton("CANCEL");
        JButton update = AccommodationManagementHelper.textButton("UPDATE");
        JButton save = AccommodationManagementHelper.textButton("SAVE");
        actions.add(cancel);
        actions.add(update);
        actions.add(save);

        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(4, 0, 0, 0);
        body.add(actions, gbc);

        cancel.addActionListener(e -> clearForm());
        save.addActionListener(e -> saveAccommodation());
        update.addActionListener(e -> updateAccommodation());
        return body;
    }

    private void addField(JPanel panel, GridBagConstraints gbc, int y, int x, String label, JComponent field) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 0, 16, x == 0 ? 18 : 0);
        panel.add(AccommodationManagementHelper.fieldBlock(label, field), gbc);
    }

    private JPanel createAmenitiesPanel() {
        JPanel amenitiesPanel = new JPanel(new BorderLayout(0, 10));
        amenitiesPanel.setOpaque(false);

        JPanel addRow = new JPanel(new BorderLayout(10, 0));
        addRow.setOpaque(false);
        addRow.add(AccommodationManagementHelper.fieldBlock("Amenities", amenityField), BorderLayout.CENTER);
        JButton addAmenity = AccommodationManagementHelper.textButton("ADD");
        addAmenity.addActionListener(e -> addAmenity());
        addRow.add(addAmenity, BorderLayout.EAST);

        amenitiesListPanel.setOpaque(false);
        amenitiesListPanel.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 226, 232)),
                new EmptyBorder(8, 8, 8, 8)
        ));

        amenitiesPanel.add(addRow, BorderLayout.NORTH);
        amenitiesPanel.add(amenitiesListPanel, BorderLayout.CENTER);
        return amenitiesPanel;
    }

    private void addAmenity() {
        String amenity = amenityField.getText().trim();
        if (amenity.isEmpty()) {
            return;
        }
        amenities.add(amenity);
        amenityField.setText("");
        refreshAmenities();
    }

    private void removeAmenity(String amenity) {
        amenities.remove(amenity);
        refreshAmenities();
    }

    private void refreshAmenities() {
        amenitiesListPanel.removeAll();
        if (amenities.isEmpty()) {
            JLabel empty = new JLabel("No amenities added yet");
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            empty.setForeground(AccommodationManagementHelper.TEXT_SECONDARY);
            empty.setBorder(new EmptyBorder(6, 4, 6, 4));
            amenitiesListPanel.add(empty);
        } else {
            for (String amenity : amenities) {
                amenitiesListPanel.add(AccommodationManagementHelper.amenityChip(amenity, () -> removeAmenity(amenity)));
            }
        }
        amenitiesListPanel.revalidate();
        amenitiesListPanel.repaint();
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
        Object selectedCategory = categoryCombo.getSelectedItem();
        String name = nameField.getText().trim();
        String category = selectedCategory == null ? "" : String.valueOf(selectedCategory).trim();
        int capacity = (Integer) capacitySpinner.getValue();
        String status = String.valueOf(statusCombo.getSelectedItem()).trim();
        String assignedStaff = assignedStaffField.getText().trim();

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
        if (assignedStaff.isEmpty()) {
            errors.append("Assigned Staff is required.\n");
        }
        if (errors.length() > 0) {
            JOptionPane.showMessageDialog(this, errors.toString(), "Please complete required fields", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        return new AccommodationRecord(name, category, capacity, status, assignedStaff, amenities);
    }

    private void clearForm() {
        editingRow = -1;
        nameField.setText("");
        if (categoryCombo.getItemCount() > 0) {
            categoryCombo.setSelectedIndex(0);
        }
        capacitySpinner.setValue(1);
        statusCombo.setSelectedIndex(0);
        assignedStaffField.setText("");
        amenityField.setText("");
        amenities.clear();
        refreshAmenities();
    }
}
