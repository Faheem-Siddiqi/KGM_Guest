package com.kgm.ui;

import com.kgm.dao.AccommodationCategoryDao;
import com.kgm.dao.AccommodationDao;
import com.kgm.dao.GuestDao;
import com.kgm.database.DatabaseInitializer;
import com.kgm.model.Guest;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.component.UniversalDatePicker;
import com.kgm.ui.styling.AddGuestHelper;
import com.kgm.ui.styling.DialogHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AddGuest extends JFrame {
    private static final String ALL_ROOMS_OCCUPIED = "All rooms occupied";
    private static final String NO_ROOMS_AVAILABLE = "No rooms available";
    private static final Color ROOM_BLOCKED_COLOR = new Color(180, 60, 45);

    private static final GuestDao GUEST_DAO = new GuestDao();
    private static final AccommodationDao ACCOMMODATION_DAO = new AccommodationDao();
    private static final AccommodationCategoryDao ACCOMMODATION_CATEGORY_DAO = new AccommodationCategoryDao();

    public AddGuest() {
        setTitle("Add Guest");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        Runnable onBack = () -> {
            new HomeView();
            dispose();
        };

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(new HeaderPanel("Add Guest"), BorderLayout.NORTH);
        root.add(createContent(onBack), BorderLayout.CENTER);
        root.add(new FooterPanel(), BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(900, 650));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    public static JComponent createContent(Runnable onBack) {
        DatabaseInitializer.init();
        final JScrollPane[] scrollRef = new JScrollPane[1];
        JPanel page = AddGuestHelper.pagePanel();
        page.add(AddGuestHelper.screenHeader(onBack), AddGuestHelper.pageConstraints(0));

        JTextField guestNameField = new JTextField("");
        JTextField guestCnicField = new JTextField("");
        JComboBox<String> guestNationalityCombo = AddGuestHelper.editableCombo(
                "Pakistani", "Afghan", "Chinese", "Turkish", "Other (Specify)"
        );
        guestNationalityCombo.setSelectedItem("Pakistani");
        JComboBox<String> guestCategoryCombo = AddGuestHelper.combo("Family", "Non-Family");
        JTextField guestAddressField = new JTextField("");

        JTextField requestedByField = new JTextField("");
        JComboBox<String> requestedDepartmentCombo = AddGuestHelper.editableCombo(
               
            "HR", "Admin", "Finance", "Spinning", "Power House", "IT"
        );
        requestedDepartmentCombo.setSelectedItem("");
        JTextField approvedByField = new JTextField("");
        JTextField accommodatedByField = new JTextField("");

        Date initialArrivalDate = dateTimeValue(0, 0);
        Date initialDepartureDate = dateTimeValue(1, 0);
        UniversalDatePicker arrivalDate = new UniversalDatePicker(initialArrivalDate);
        UniversalDatePicker departureDate = new UniversalDatePicker(initialDepartureDate);
        JTextField tenureField = new JTextField();
        tenureField.setEditable(false);
        tenureField.setFocusable(false);
        tenureField.setBackground(Color.WHITE);
        JComboBox<String> accommodationCombo = AddGuestHelper.combo(accommodationCategoryItems());
        JComboBox<String> roomCombo = AddGuestHelper.combo();
        installRoomComboRenderer(roomCombo);
        updateRoomCombo(accommodationCombo, roomCombo);
        accommodationCombo.addActionListener(event -> updateRoomCombo(accommodationCombo, roomCombo));
        JTextArea remarks = AddGuestHelper.remarksArea("N/A");

        updateTenure(arrivalDate, departureDate, tenureField);
        arrivalDate.addDateChangeListener(() -> updateTenure(arrivalDate, departureDate, tenureField));
        departureDate.addDateChangeListener(() -> updateTenure(arrivalDate, departureDate, tenureField));

        JPanel basicCard = AddGuestHelper.cardPanel();
        GridBagConstraints basicGbc = AddGuestHelper.formConstraints();
        int y = AddGuestHelper.addSectionTitle(basicCard, basicGbc, 0, "Basic Information");
        AddGuestHelper.addField(basicCard, basicGbc, y, 0, "Guest Name", guestNameField);
        AddGuestHelper.addField(basicCard, basicGbc, y++, 2, "Guest CNIC", guestCnicField);
        AddGuestHelper.addField(basicCard, basicGbc, y, 0, "Guest Nationality", guestNationalityCombo);
        AddGuestHelper.addField(basicCard, basicGbc, y++, 2, "Guest Category", guestCategoryCombo);
        AddGuestHelper.addField(basicCard, basicGbc, y, 0, "Guest Address", guestAddressField);

        JPanel requestCard = AddGuestHelper.cardPanel();
        GridBagConstraints requestGbc = AddGuestHelper.formConstraints();
        y = AddGuestHelper.addSectionTitle(requestCard, requestGbc, 0, "Request Details");
        AddGuestHelper.addField(requestCard, requestGbc, y, 0, "Requested By", requestedByField);
        AddGuestHelper.addField(requestCard, requestGbc, y++, 2, "Requested Department", requestedDepartmentCombo);
        AddGuestHelper.addField(requestCard, requestGbc, y, 0, "Approved By", approvedByField);
        AddGuestHelper.addField(requestCard, requestGbc, y++, 2, "Accommodated By", accommodatedByField);

        JPanel stayCard = AddGuestHelper.cardPanel();
        GridBagConstraints stayGbc = AddGuestHelper.formConstraints();
        y = AddGuestHelper.addSectionTitle(stayCard, stayGbc, 0, "Stay Details");
        AddGuestHelper.addField(stayCard, stayGbc, y, 0, "Arrival Date", arrivalDate);
        AddGuestHelper.addField(stayCard, stayGbc, y++, 2, "Departure Date", departureDate);
        AddGuestHelper.addField(stayCard, stayGbc, y, 0, "Tenure", tenureField);
        AddGuestHelper.addField(stayCard, stayGbc, y++, 2, "Accommodation Category", accommodationCombo);
        AddGuestHelper.addField(stayCard, stayGbc, y++, 0, "Room", roomCombo);

        JLabel remarksLabel = AddGuestHelper.label("Remarks");
        stayGbc.gridx = 0;
        stayGbc.gridy = y++;
        stayGbc.gridwidth = 4;
        stayCard.add(remarksLabel, stayGbc);

        stayGbc.gridx = 0;
        stayGbc.gridy = y++;
        stayGbc.gridwidth = 4;
        stayGbc.fill = GridBagConstraints.BOTH;
        stayGbc.ipady = 80;
        stayCard.add(remarks, stayGbc);
        stayGbc.fill = GridBagConstraints.HORIZONTAL;
        stayGbc.ipady = 0;
        stayGbc.gridwidth = 1;

        JPanel actions = AddGuestHelper.actionsPanel();
        JButton reset = new JButton("Reset");
        AddGuestHelper.styleReset(reset);
        reset.addActionListener(e -> clearForm(
                guestNameField,
                guestCnicField,
                guestNationalityCombo,
                guestCategoryCombo,
                guestAddressField,
                requestedByField,
                requestedDepartmentCombo,
                approvedByField,
                accommodatedByField,
                accommodationCombo,
                roomCombo,
                arrivalDate,
                departureDate,
                tenureField,
                remarks
        ));

        JButton submit = new JButton("Add Guest");
        AddGuestHelper.stylePrimary(submit);
        submit.addActionListener(e -> submitGuest(
                page,
                guestNameField,
                guestCnicField,
                guestNationalityCombo,
                guestCategoryCombo,
                guestAddressField,
                requestedByField,
                requestedDepartmentCombo,
                approvedByField,
                accommodatedByField,
                tenureField,
                accommodationCombo,
                roomCombo,
                arrivalDate,
                departureDate,
                remarks
        ));
        actions.add(reset);
        actions.add(submit);

        stayGbc.gridx = 0;
        stayGbc.gridy = y;
        stayGbc.gridwidth = 4;
        stayCard.add(actions, stayGbc);

        page.add(AddGuestHelper.breadcrumb(
                new String[]{"Basic Information", "Request Details", "Stay Details"},
                new Runnable[]{
                        () -> scrollToSection(scrollRef[0], basicCard),
                        () -> scrollToSection(scrollRef[0], requestCard),
                        () -> scrollToSection(scrollRef[0], stayCard)
                }
        ), AddGuestHelper.pageConstraints(1));
        page.add(basicCard, AddGuestHelper.pageConstraints(2));
        page.add(requestCard, AddGuestHelper.pageConstraints(3));
        page.add(stayCard, AddGuestHelper.pageConstraints(4));
        page.add(AddGuestHelper.returnToTop(() -> scrollToTop(scrollRef[0])), AddGuestHelper.pageConstraints(5));

        JScrollPane scroll = new JScrollPane(page);
        scrollRef[0] = scroll;
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(Color.WHITE);
        SwingUtilities.invokeLater(() -> {
            scroll.getVerticalScrollBar().setValue(0);
            scroll.getHorizontalScrollBar().setValue(0);
        });
        return scroll;
    }

    private static void scrollToSection(JScrollPane scroll, JComponent section) {
        if (scroll == null || section == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            Rectangle bounds = section.getBounds();
            bounds.y = Math.max(0, bounds.y - 12);
            bounds.height = Math.min(section.getHeight() + 24, scroll.getViewport().getHeight());
            if (section.getParent() instanceof JComponent) {
                ((JComponent) section.getParent()).scrollRectToVisible(bounds);
            }
        });
    }

    private static void scrollToTop(JScrollPane scroll) {
        if (scroll == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            scroll.getVerticalScrollBar().setValue(0);
            scroll.getHorizontalScrollBar().setValue(0);
        });
    }

    public static void main(String[] args) {
        DatabaseInitializer.init();
        SwingUtilities.invokeLater(() -> new AddGuest().setVisible(true));
    }

    private static void submitGuest(
            Component parent,
            JTextField guestNameField,
            JTextField guestCnicField,
            JComboBox<String> guestNationalityCombo,
            JComboBox<String> guestCategoryCombo,
            JTextField guestAddressField,
            JTextField requestedByField,
            JComboBox<String> requestedDepartmentCombo,
            JTextField approvedByField,
            JTextField accommodatedByField,
            JTextField tenureField,
            JComboBox<String> accommodationCombo,
            JComboBox<String> roomCombo,
            UniversalDatePicker arrivalDate,
            UniversalDatePicker departureDate,
            JTextArea remarks
    ) {
        List<String> missingFields = new ArrayList<>();
        List<String> fieldIssues = new ArrayList<>();
        String dateIssue = null;
        String roomIssue = null;
        String guestName = guestNameField.getText().trim();
        String guestCnic = guestCnicField.getText().trim();
        String guestNationality = String.valueOf(guestNationalityCombo.getEditor().getItem()).trim();
        String guestCategory = selectedText(guestCategoryCombo);
        String guestAddress = guestAddressField.getText().trim();
        String requestedBy = requestedByField.getText().trim();
        String requestedDepartment = String.valueOf(requestedDepartmentCombo.getEditor().getItem()).trim();
        String approvedBy = approvedByField.getText().trim();
        String accommodatedBy = accommodatedByField.getText().trim();
        String accommodation = selectedText(accommodationCombo);
        String room = selectedText(roomCombo);
        String remarksText = remarks.getText().trim();

        if (guestName.isEmpty()) {
            missingFields.add("Guest Name");
        }
        if (guestCnic.isEmpty()) {
            missingFields.add("Guest CNIC");
        } else if (!guestCnic.matches("\\d{13}")) {
            fieldIssues.add("Guest CNIC must contain exactly 13 digits.");
        }
        if (guestNationality.isEmpty()) {
            missingFields.add("Guest Nationality");
        }
        if (guestCategory.isEmpty()) {
            missingFields.add("Guest Category");
        }
        if (guestAddress.isEmpty()) {
            missingFields.add("Guest Address");
        }
        if (requestedBy.isEmpty()) {
            missingFields.add("Requested By");
        }
        if (requestedDepartment.isEmpty()) {
            missingFields.add("Requested Department");
        }
        if (approvedBy.isEmpty()) {
            missingFields.add("Approved By");
        }
        if (accommodatedBy.isEmpty()) {
            missingFields.add("Accommodated By");
        }
        if (tenureField.getText().startsWith("Departure must")) {
            dateIssue = tenureField.getText() + ".";
        }
        if (accommodation.isEmpty()) {
            missingFields.add("Accommodation Category");
        }
        if (!accommodation.isEmpty() && (room.isEmpty() || isRoomPlaceholder(room) || !roomCombo.isEnabled())) {
            roomIssue = "No ready room is available for " + accommodation
                    + ". Select another accommodation category or mark a room ready in Accommodation Management.";
        }
        List<String> validationSections = validationSections(missingFields, fieldIssues, dateIssue, roomIssue);
        if (!validationSections.isEmpty()) {
            DialogHelper.errorSections(
                    parent,
                    "Guest Details Need Attention",
                    validationSections.toArray(new String[0])
            );
            return;
        }

        Guest guest = new Guest();
        guest.setGuestName(guestName);
        guest.setCnic(guestCnic);
        guest.setNationality(guestNationality);
        guest.setGuestCategory(guestCategory);
        guest.setAddress(guestAddress);
        guest.setRequestedBy(requestedBy);
        guest.setRequestedDepartment(requestedDepartment);
        guest.setApprovedBy(approvedBy);
        guest.setAccommodatedBy(accommodatedBy);
        guest.setArrivalAt(arrivalDate.getDate());
        guest.setDepartureAt(departureDate.getDate());
        guest.setAccommodation(accommodation);
        guest.setRoomName(room);
        guest.setRemarks(remarksText.isEmpty() ? "N/A" : remarksText);

        try {
            GUEST_DAO.save(guest);
            DialogHelper.success(parent, "Guest added successfully. Record ID: " + guest.getId());
            clearForm(
                    guestNameField,
                    guestCnicField,
                    guestNationalityCombo,
                    guestCategoryCombo,
                    guestAddressField,
                    requestedByField,
                    requestedDepartmentCombo,
                    approvedByField,
                    accommodatedByField,
                    accommodationCombo,
                    roomCombo,
                    arrivalDate,
                    departureDate,
                    tenureField,
                    remarks
            );
        } catch (SQLException exception) {
            DialogHelper.error(parent, "Guest not saved", exception.getMessage());
        }
    }

    private static List<String> validationSections(
            List<String> missingFields,
            List<String> fieldIssues,
            String dateIssue,
            String roomIssue
    ) {
        List<String> sections = new ArrayList<>();
        if (!missingFields.isEmpty()) {
            sections.add(missingFieldsMessage(missingFields));
        }
        if (!fieldIssues.isEmpty()) {
            sections.add("Check field format\n" + String.join("\n", fieldIssues));
        }
        if (dateIssue != null) {
            sections.add("Check stay dates\n" + dateIssue);
        }
        if (roomIssue != null) {
            sections.add("Room availability\n" + roomIssue);
        }
        return sections;
    }

    private static String missingFieldsMessage(List<String> missingFields) {
        if (missingFields.size() >= 8) {
            return """
                    Missing required information
                    Please complete the required Guest Details, Request Details, Approval Details, and Stay Details before saving.
                    """;
        }
        return "Missing required information\nComplete: " + String.join(", ", missingFields) + ".";
    }

    private static String[] accommodationCategoryItems() {
        try {
            return itemsOrFallback(
                    ACCOMMODATION_CATEGORY_DAO.findActiveNames(),
                    new String[0]
            );
        } catch (SQLException exception) {
            return new String[0];
        }
    }

    private static String[] roomItems(String accommodationCategory) {
        if (accommodationCategory == null || accommodationCategory.trim().isEmpty()) {
            return new String[0];
        }
        try {
            return itemsOrFallback(
                    ACCOMMODATION_DAO.findReadyNamesByCategory(accommodationCategory),
                    new String[0]
            );
        } catch (SQLException exception) {
            return new String[0];
        }
    }

    private static void updateRoomCombo(JComboBox<String> accommodationCombo, JComboBox<String> roomCombo) {
        String accommodation = selectedText(accommodationCombo);
        if (accommodation.isEmpty()) {
            setComboItems(roomCombo, new String[0]);
            setRoomComboState(roomCombo, false, false);
            return;
        }

        String[] readyRooms = roomItems(accommodation);
        if (readyRooms.length > 0) {
            setComboItems(roomCombo, readyRooms);
            setRoomComboState(roomCombo, true, false);
            return;
        }

        if (hasRoomsInCategory(accommodation)) {
            setComboItems(roomCombo, new String[]{ALL_ROOMS_OCCUPIED});
            setRoomComboState(roomCombo, false, true);
            return;
        }

        setComboItems(roomCombo, new String[]{NO_ROOMS_AVAILABLE});
        setRoomComboState(roomCombo, false, false);
    }

    private static void clearForm(
            JTextField guestNameField,
            JTextField guestCnicField,
            JComboBox<String> guestNationalityCombo,
            JComboBox<String> guestCategoryCombo,
            JTextField guestAddressField,
            JTextField requestedByField,
            JComboBox<String> requestedDepartmentCombo,
            JTextField approvedByField,
            JTextField accommodatedByField,
            JComboBox<String> accommodationCombo,
            JComboBox<String> roomCombo,
            UniversalDatePicker arrivalDate,
            UniversalDatePicker departureDate,
            JTextField tenureField,
            JTextArea remarks
    ) {
        guestNameField.setText("");
        guestCnicField.setText("");
        guestNationalityCombo.setSelectedItem("Pakistani");
        if (guestCategoryCombo.getItemCount() > 0) {
            guestCategoryCombo.setSelectedIndex(0);
        }
        guestAddressField.setText("");
        requestedByField.setText("");
        requestedDepartmentCombo.setSelectedItem("");
        approvedByField.setText("");
        accommodatedByField.setText("");
        arrivalDate.setDate(dateTimeValue(0, 0));
        departureDate.setDate(dateTimeValue(1, 0));
        selectFirstItem(accommodationCombo);
        updateRoomCombo(accommodationCombo, roomCombo);
        remarks.setText("N/A");
        updateTenure(arrivalDate, departureDate, tenureField);
    }

    private static String[] itemsOrFallback(List<String> values, String... fallback) {
        List<String> cleanValues = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                cleanValues.add(value.trim());
            }
        }
        return cleanValues.isEmpty() ? fallback : cleanValues.toArray(new String[0]);
    }

    private static boolean hasRoomsInCategory(String accommodationCategory) {
        try {
            return !ACCOMMODATION_DAO.findActiveNamesByCategory(accommodationCategory).isEmpty();
        } catch (SQLException exception) {
            return false;
        }
    }

    private static void setRoomComboState(JComboBox<String> roomCombo, boolean enabled, boolean allOccupied) {
        roomCombo.setEnabled(enabled);
        roomCombo.setForeground(allOccupied ? ROOM_BLOCKED_COLOR : Color.BLACK);
        roomCombo.putClientProperty("kgm.allRoomsOccupied", allOccupied);
        roomCombo.repaint();
    }

    private static void installRoomComboRenderer(JComboBox<String> roomCombo) {
        ListCellRenderer<? super String> defaultRenderer = roomCombo.getRenderer();
        roomCombo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            Component component = defaultRenderer.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    isSelected,
                    cellHasFocus
            );
            if (component instanceof JLabel label) {
                if (ALL_ROOMS_OCCUPIED.equals(value)) {
                    label.setForeground(ROOM_BLOCKED_COLOR);
                } else if (NO_ROOMS_AVAILABLE.equals(value)) {
                    label.setForeground(new Color(145, 145, 145));
                }
            }
            return component;
        });
    }

    private static void setComboItems(JComboBox<String> comboBox, String[] items) {
        Object selected = comboBox.getSelectedItem();
        comboBox.removeAllItems();
        for (String item : items) {
            comboBox.addItem(item);
        }
        if (selected != null && containsItem(comboBox, String.valueOf(selected))) {
            comboBox.setSelectedItem(selected);
        }
        if (comboBox.getSelectedIndex() < 0 && comboBox.getItemCount() > 0) {
            comboBox.setSelectedIndex(0);
        }
    }

    private static void selectFirstItem(JComboBox<String> comboBox) {
        if (comboBox.getItemCount() > 0) {
            comboBox.setSelectedIndex(0);
        }
    }

    private static String selectedText(JComboBox<String> comboBox) {
        Object selected = comboBox.getSelectedItem();
        return selected == null ? "" : String.valueOf(selected).trim();
    }

    private static boolean isRoomPlaceholder(String value) {
        return ALL_ROOMS_OCCUPIED.equals(value) || NO_ROOMS_AVAILABLE.equals(value);
    }

    private static boolean containsItem(JComboBox<String> comboBox, String value) {
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            if (String.valueOf(comboBox.getItemAt(i)).equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static Date dateTimeValue(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hour, minute, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private static Date dateTimeValue(int daysFromToday, int hour) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, daysFromToday);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private static JSpinner dateTimeSpinner(Date value) {
        JSpinner spinner = new JSpinner(new SpinnerDateModel(value, null, null, Calendar.MINUTE));
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "yyyy-MM-dd HH:mm");
        spinner.setEditor(editor);
        spinner.setBackground(Color.WHITE);
        spinner.setOpaque(false);
        editor.setOpaque(false);
        editor.getTextField().setOpaque(false);
        editor.getTextField().setBackground(Color.WHITE);
        editor.getTextField().setBorder(null);
        return spinner;
    }

    private static void updateTenure(UniversalDatePicker arrivalDate, UniversalDatePicker departureDate, JTextField tenureField) {
        Date arrival = arrivalDate.getDate();
        Date departure = departureDate.getDate();
        if (departure.before(arrival)) {
            tenureField.setForeground(new Color(180, 60, 45));
            tenureField.setText("Departure must be after arrival");
            return;
        }

        tenureField.setForeground(new Color(30, 30, 30));
        long diffMillis = Math.max(0, departure.getTime() - arrival.getTime());
        long totalHours = diffMillis / (1000 * 60 * 60);
        long days = totalHours / 24;
        long hours = totalHours % 24;

        tenureField.setText(tenureText(days, hours));
    }

    private static String tenureText(long days, long hours) {
        String dayText = days + (days == 1 ? " day" : " days");
        String hourText = hours + (hours == 1 ? " hour" : " hours");
        return dayText + " " + hourText;
    }

    private static void addDateTimeEditListener(JSpinner spinner, Runnable onChange) {
        JFormattedTextField textField = ((JSpinner.DateEditor) spinner.getEditor()).getTextField();
        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                commitAndUpdate();
            }

            public void removeUpdate(DocumentEvent e) {
                commitAndUpdate();
            }

            public void changedUpdate(DocumentEvent e) {
                commitAndUpdate();
            }

            private void commitAndUpdate() {
                SwingUtilities.invokeLater(() -> {
                    try {
                        spinner.commitEdit();
                        onChange.run();
                    } catch (java.text.ParseException ignored) {
                    }
                });
            }
        });
    }
}
