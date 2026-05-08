package com.kgm.ui.panel;

import com.kgm.ui.styling.AccommodationManagementHelper;
import com.kgm.ui.styling.DialogHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;

public class AccommodationFormPanel extends JPanel {
    private static final String ROOM_PREFIX = "Room-";
    private static final String DEFAULT_STATUS = "Ready for Assignment";

    private final JTextField nameField = new JTextField(ROOM_PREFIX);
    private final JComboBox<String> categoryCombo = AccommodationManagementHelper.combo();
    private final JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
    private final JComboBox<String> statusCombo = AccommodationManagementHelper.combo(
            DEFAULT_STATUS, "Temporarily Unavailable", "Under Maintenance", "Reserved"
    );
    private final JTextField assignedStaffField = new JTextField("");
    private final JTextField amenityField = new JTextField("");
    private final JPanel amenitiesListPanel = new AmenitiesListPanel();
    private final List<String> amenities = new ArrayList<>();
    private final JButton cancelButton = AccommodationManagementHelper.textButton("CANCEL");
    private final JButton updateButton = AccommodationManagementHelper.textButton("UPDATE");
    private final JButton saveButton = AccommodationManagementHelper.textButton("SAVE");
    private JScrollPane amenitiesScrollPane;

    private final SaveHandler onSave;
    private final UpdateHandler onUpdate;
    private String currentNamePrefix = ROOM_PREFIX;
    private int editingRow = -1;

    public AccommodationFormPanel(SaveHandler onSave, UpdateHandler onUpdate) {
        this.onSave = onSave;
        this.onUpdate = onUpdate;
        setLayout(new BorderLayout());
        setOpaque(false);
        nameField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent event) {
                nameField.setText(accommodationNameValue(nameField.getText()));
                updateActionStates();
            }
        });
        categoryCombo.addActionListener(e -> {
            syncNamePrefix();
            updateActionStates();
        });
        statusCombo.addActionListener(e -> updateActionStates());
        capacitySpinner.addChangeListener(e -> updateActionStates());
        addDocumentListener(nameField, this::updateActionStates);
        addDocumentListener(assignedStaffField, this::updateActionStates);
        amenityField.addActionListener(e -> addAmenity());

        JPanel card = AccommodationManagementHelper.sectionCard(
                "Accommodation Form",
                "Create a new accommodation or update the selected record."
        );
        card.add(createFormBody(), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
        refreshAmenities();
        setDefaultStatus();
    }

    public void setCategories(List<String> categories) {
        Object selected = categoryCombo.getSelectedItem();
        categoryCombo.removeAllItems();
        for (String category : categories) {
            categoryCombo.addItem(category);
        }
        if (selected != null && containsItem(categoryCombo, String.valueOf(selected))) {
            categoryCombo.setSelectedItem(selected);
        }
        if (categoryCombo.getSelectedIndex() < 0 && categoryCombo.getItemCount() > 0) {
            categoryCombo.setSelectedIndex(0);
        }
        syncNamePrefix();
        updateActionStates();
    }

    public void editAccommodation(int row, AccommodationRecord accommodation) {
        editingRow = row;
        categoryCombo.setSelectedItem(accommodation.getCategory());
        currentNamePrefix = ROOM_PREFIX;
        nameField.setText(accommodationNameValue(accommodation.getName()));
        capacitySpinner.setValue(accommodation.getCapacity());
        statusCombo.setSelectedItem(accommodation.getStatus());
        assignedStaffField.setText(accommodation.getAssignedStaff());
        amenities.clear();
        amenities.addAll(accommodation.getAmenities());
        refreshAmenities();
        updateActionStates();
    }

    private JPanel createFormBody() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints gbc = AccommodationManagementHelper.formConstraints();

        addField(body, gbc, 0, 0, "Category", categoryCombo);
        addField(body, gbc, 0, 1, "Name", nameField);
        addField(body, gbc, 1, 0, "Capacity", capacitySpinner);
        addField(body, gbc, 1, 1, "Status", statusCombo);
        addField(body, gbc, 2, 0, "Assigned Staff", assignedStaffField);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        body.add(createAmenitiesPanel(), gbc);

        JPanel actions = AccommodationManagementHelper.textActionsPanel();
        actions.add(cancelButton);
        actions.add(updateButton);
        actions.add(saveButton);

        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(4, 0, 0, 0);
        body.add(actions, gbc);

        cancelButton.addActionListener(e -> clearForm());
        saveButton.addActionListener(e -> saveAccommodation());
        updateButton.addActionListener(e -> updateAccommodation());
        updateActionStates();
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
        JPanel amenitiesPanel = new JPanel();
        amenitiesPanel.setLayout(new BoxLayout(amenitiesPanel, BoxLayout.Y_AXIS));
        amenitiesPanel.setOpaque(false);

        JLabel amenitiesLabel = AccommodationManagementHelper.label("Amenities");
        amenitiesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel addRow = new JPanel();
        addRow.setLayout(new BoxLayout(addRow, BoxLayout.X_AXIS));
        addRow.setOpaque(false);
        addRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComponent styledAmenityField = AccommodationManagementHelper.styleField(amenityField);
        styledAmenityField.setAlignmentX(Component.LEFT_ALIGNMENT);
        addRow.add(styledAmenityField);
        addRow.add(Box.createHorizontalStrut(10));
        JButton addAmenity = AccommodationManagementHelper.textButton("ADD");
        addAmenity.addActionListener(e -> addAmenity());
        addRow.add(addAmenity);

        amenitiesListPanel.setOpaque(false);
        amenitiesListPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        amenitiesScrollPane = new JScrollPane(amenitiesListPanel);
        amenitiesScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        amenitiesScrollPane.setPreferredSize(new Dimension(704, 112));
        amenitiesScrollPane.setMinimumSize(new Dimension(620, 82));
        amenitiesScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
        amenitiesScrollPane.setBorder(new LineBorder(new Color(220, 226, 232)));
        amenitiesScrollPane.setWheelScrollingEnabled(false);
        amenitiesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        amenitiesScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        amenitiesScrollPane.getVerticalScrollBar().setUnitIncrement(12);
        amenitiesScrollPane.getViewport().setBackground(Color.WHITE);
        amenitiesScrollPane.addMouseWheelListener(this::forwardAmenitiesMouseWheel);
        amenitiesScrollPane.getViewport().addMouseWheelListener(this::forwardAmenitiesMouseWheel);
        amenitiesListPanel.addMouseWheelListener(this::forwardAmenitiesMouseWheel);

        amenitiesPanel.add(amenitiesLabel);
        amenitiesPanel.add(Box.createVerticalStrut(6));
        amenitiesPanel.add(addRow);
        amenitiesPanel.add(Box.createVerticalStrut(10));
        amenitiesPanel.add(amenitiesScrollPane);
        return amenitiesPanel;
    }

    private void forwardAmenitiesMouseWheel(MouseWheelEvent event) {
        if (scrollAmenitiesList(event)) {
            return;
        }

        JScrollPane pageScroll = findPageScrollPane();
        if (pageScroll == null) {
            return;
        }

        MouseWheelEvent pageEvent = new MouseWheelEvent(
                pageScroll,
                event.getID(),
                event.getWhen(),
                event.getModifiersEx(),
                0,
                0,
                event.getXOnScreen(),
                event.getYOnScreen(),
                event.getClickCount(),
                event.isPopupTrigger(),
                event.getScrollType(),
                event.getScrollAmount(),
                event.getWheelRotation(),
                event.getPreciseWheelRotation()
        );
        pageScroll.dispatchEvent(pageEvent);
        event.consume();
    }

    private boolean scrollAmenitiesList(MouseWheelEvent event) {
        JScrollBar verticalBar = amenitiesScrollPane.getVerticalScrollBar();
        if (verticalBar == null || !verticalBar.isVisible()) {
            return false;
        }

        int direction = event.getWheelRotation() < 0 ? -1 : 1;
        int max = verticalBar.getMaximum() - verticalBar.getVisibleAmount();
        if (direction < 0 && verticalBar.getValue() <= verticalBar.getMinimum()) {
            return false;
        }
        if (direction > 0 && verticalBar.getValue() >= max) {
            return false;
        }

        scrollBar(verticalBar, event);
        event.consume();
        return true;
    }

    private JScrollPane findPageScrollPane() {
        Container parent = getParent();
        while (parent != null) {
            if (parent instanceof JScrollPane scrollPane && scrollPane != amenitiesScrollPane) {
                return scrollPane;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private void scrollBar(JScrollBar scrollBar, MouseWheelEvent event) {
        int direction = event.getWheelRotation() < 0 ? -1 : 1;
        int amount = event.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL
                ? event.getUnitsToScroll() * scrollBar.getUnitIncrement(direction)
                : event.getWheelRotation() * scrollBar.getBlockIncrement(direction);
        int max = scrollBar.getMaximum() - scrollBar.getVisibleAmount();
        int value = Math.max(scrollBar.getMinimum(), Math.min(max, scrollBar.getValue() + amount));
        scrollBar.setValue(value);
    }

    private void addAmenity() {
        String amenity = amenityField.getText().trim();
        if (amenity.isEmpty()) {
            return;
        }
        amenities.add(amenity);
        amenityField.setText("");
        refreshAmenities();
        updateActionStates();
    }

    private void removeAmenity(String amenity) {
        amenities.remove(amenity);
        refreshAmenities();
        updateActionStates();
    }

    private void refreshAmenities() {
        amenitiesListPanel.removeAll();
        if (amenities.isEmpty()) {
            JLabel empty = new JLabel("No amenities added yet");
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            empty.setForeground(AccommodationManagementHelper.TEXT_SECONDARY);
            empty.setBorder(new EmptyBorder(6, 4, 6, 4));
            amenitiesListPanel.add(empty);
            installAmenitiesWheelForwarding(empty);
        } else {
            for (String amenity : amenities) {
                JPanel chip = AccommodationManagementHelper.amenityChip(amenity, () -> removeAmenity(amenity));
                installAmenitiesWheelForwarding(chip);
                amenitiesListPanel.add(chip);
            }
        }
        amenitiesListPanel.revalidate();
        amenitiesListPanel.repaint();
        if (amenitiesScrollPane != null) {
            amenitiesScrollPane.revalidate();
            amenitiesScrollPane.repaint();
        }
    }

    private void installAmenitiesWheelForwarding(Component component) {
        component.addMouseWheelListener(this::forwardAmenitiesMouseWheel);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                installAmenitiesWheelForwarding(child);
            }
        }
    }

    private void saveAccommodation() {
        AccommodationRecord accommodation = collectAccommodation();
        if (accommodation == null) {
            return;
        }
        if (onSave.save(accommodation)) {
            clearForm();
        }
    }

    private void updateAccommodation() {
        if (editingRow < 0) {
            DialogHelper.warning(this, "No row selected", "Select Edit from the table before updating.");
            return;
        }
        AccommodationRecord accommodation = collectAccommodation();
        if (accommodation == null) {
            return;
        }
        if (onUpdate.update(editingRow, accommodation)) {
            clearForm();
        }
    }

    private AccommodationRecord collectAccommodation() {
        String category = selectedCategory();
        String name = accommodationNameValue(nameField.getText().trim());
        nameField.setText(name);
        int capacity = (Integer) capacitySpinner.getValue();
        String status = String.valueOf(statusCombo.getSelectedItem()).trim();
        String assignedStaff = assignedStaffField.getText().trim();

        StringBuilder errors = new StringBuilder();
        if (name.equals(ROOM_PREFIX)) {
            errors.append("Name is required after Room-.\n");
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
            DialogHelper.error(this, "Please complete required fields", errors.toString());
            return null;
        }

        return new AccommodationRecord(name, category, capacity, status, assignedStaff, amenities);
    }

    private void clearForm() {
        editingRow = -1;
        if (categoryCombo.getItemCount() > 0) {
            categoryCombo.setSelectedIndex(0);
        }
        currentNamePrefix = ROOM_PREFIX;
        nameField.setText(currentNamePrefix);
        capacitySpinner.setValue(1);
        setDefaultStatus();
        assignedStaffField.setText("");
        amenityField.setText("");
        amenities.clear();
        refreshAmenities();
        updateActionStates();
    }

    private void updateActionStates() {
        boolean editing = editingRow >= 0;
        boolean validName = isValidRoomName(nameField.getText());
        boolean validCategory = !selectedCategory().isEmpty();
        boolean hasAssignedStaff = !assignedStaffField.getText().trim().isEmpty();
        boolean canSubmit = validName && validCategory && hasAssignedStaff;

        AccommodationManagementHelper.setTextButtonEnabled(saveButton, canSubmit && !editing);
        AccommodationManagementHelper.setTextButtonEnabled(updateButton, canSubmit && editing);
        AccommodationManagementHelper.setTextButtonEnabled(cancelButton, editing || isFormDirty());
    }

    private boolean isFormDirty() {
        return !ROOM_PREFIX.equals(nameField.getText().trim())
                || !assignedStaffField.getText().trim().isEmpty()
                || !amenities.isEmpty()
                || capacitySpinner.getValue() instanceof Integer && (Integer) capacitySpinner.getValue() != 1
                || statusCombo.getSelectedIndex() > 0
                || categoryCombo.getSelectedIndex() > 0;
    }

    private void setDefaultStatus() {
        statusCombo.setSelectedItem(DEFAULT_STATUS);
        if (statusCombo.getSelectedItem() == null && statusCombo.getItemCount() > 0) {
            statusCombo.setSelectedIndex(0);
        }
    }

    private boolean isValidRoomName(String value) {
        String name = accommodationNameValue(value);
        return !ROOM_PREFIX.equals(name);
    }

    private void syncNamePrefix() {
        String nextPrefix = ROOM_PREFIX;
        String text = nameField.getText().trim();
        if (text.isEmpty() || text.equals(currentNamePrefix)) {
            nameField.setText(nextPrefix);
        } else {
            nameField.setText(accommodationNameValue(text));
        }
        currentNamePrefix = nextPrefix;
    }

    private String selectedCategory() {
        Object selectedCategory = categoryCombo.getSelectedItem();
        return selectedCategory == null ? "" : String.valueOf(selectedCategory).trim();
    }

    private boolean containsItem(JComboBox<String> comboBox, String value) {
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            if (String.valueOf(comboBox.getItemAt(i)).equals(value)) {
                return true;
            }
        }
        return false;
    }

    private String accommodationNameValue(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return ROOM_PREFIX;
        }
        if (startsWithIgnoreCase(text, ROOM_PREFIX)) {
            return ROOM_PREFIX + text.substring(ROOM_PREFIX.length()).trim();
        }
        if (text.equalsIgnoreCase("Room")) {
            return ROOM_PREFIX;
        }
        if (text.toLowerCase().startsWith("room ")) {
            return ROOM_PREFIX + text.substring("room ".length()).trim();
        }
        return ROOM_PREFIX + text;
    }

    private boolean startsWithIgnoreCase(String text, String prefix) {
        return text.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private void addDocumentListener(JTextField field, Runnable onChange) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                onChange.run();
            }

            public void removeUpdate(DocumentEvent event) {
                onChange.run();
            }

            public void changedUpdate(DocumentEvent event) {
                onChange.run();
            }
        });
    }

    public interface SaveHandler {
        boolean save(AccommodationRecord accommodation);
    }

    public interface UpdateHandler {
        boolean update(int row, AccommodationRecord accommodation);
    }

    private static class AmenitiesListPanel extends JPanel implements Scrollable {
        private AmenitiesListPanel() {
            super(new FlowLayout(FlowLayout.LEFT, 8, 8));
        }

        public Dimension getPreferredSize() {
            Container parent = getParent();
            int width = parent == null || parent.getWidth() <= 0
                    ? AccommodationManagementHelper.CONTENT_WIDTH - 156
                    : parent.getWidth();
            return wrappedPreferredSize(width);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(AccommodationManagementHelper.CONTENT_WIDTH - 156, 112);
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(16, visibleRect.height - 16);
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        private Dimension wrappedPreferredSize(int targetWidth) {
            Insets insets = getInsets();
            FlowLayout flowLayout = (FlowLayout) getLayout();
            int maxWidth = Math.max(1, targetWidth - insets.left - insets.right);
            int rowWidth = 0;
            int rowHeight = 0;
            int height = insets.top + flowLayout.getVgap();

            for (Component component : getComponents()) {
                if (!component.isVisible()) {
                    continue;
                }
                Dimension size = component.getPreferredSize();
                if (rowWidth > 0 && rowWidth + flowLayout.getHgap() + size.width > maxWidth) {
                    height += rowHeight + flowLayout.getVgap();
                    rowWidth = 0;
                    rowHeight = 0;
                }
                rowWidth += (rowWidth == 0 ? 0 : flowLayout.getHgap()) + size.width;
                rowHeight = Math.max(rowHeight, size.height);
            }

            height += rowHeight + flowLayout.getVgap() + insets.bottom;
            return new Dimension(targetWidth, height);
        }
    }
}
