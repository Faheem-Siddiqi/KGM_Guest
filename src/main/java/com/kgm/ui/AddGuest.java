package com.kgm.ui;

import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.styling.AddGuestHelper;
import com.kgm.ui.styling.DialogHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Calendar;
import java.util.Date;

public class AddGuest extends JFrame {
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
        final JScrollPane[] scrollRef = new JScrollPane[1];
        JPanel page = AddGuestHelper.pagePanel();
        page.add(AddGuestHelper.screenHeader(onBack), AddGuestHelper.pageConstraints(0));

        JTextField guestNameField = new JTextField("");
        JTextField guestCnicField = new JTextField("1234512345671");
        JComboBox<String> guestNationalityCombo = AddGuestHelper.editableCombo(
                "Pakistani", "Afghan", "Chinese", "Turkish", "Other (Specify)"
        );
        guestNationalityCombo.setSelectedItem("Pakistani");
        JComboBox<String> guestCategoryCombo = AddGuestHelper.combo("Family", "Non-Family");
        JTextField guestAddressField = new JTextField("");

        JTextField requestedByField = new JTextField("");
        JComboBox<String> requestedDepartmentCombo = AddGuestHelper.editableCombo(
                "HR", "Admin", "Finance", "Spinning", "Others (Specify)"
        );
        requestedDepartmentCombo.setSelectedItem("");
        JTextField approvedByField = new JTextField("");
        JTextField accommodatedByField = new JTextField("");

        Date initialArrivalDate = dateTimeValue(2026, Calendar.MAY, 10, 9, 0);
        Date initialDepartureDate = dateTimeValue(2026, Calendar.MAY, 15, 9, 0);
        JSpinner arrivalDate = dateTimeSpinner(initialArrivalDate);
        JSpinner departureDate = dateTimeSpinner(initialDepartureDate);
        JTextField tenureField = new JTextField();
        tenureField.setEditable(false);
        tenureField.setFocusable(false);
        tenureField.setBackground(Color.WHITE);
        JComboBox<String> accommodationCombo = AddGuestHelper.combo("Guest Room", "Guest House");
        JComboBox<String> roomCombo = AddGuestHelper.combo(
                "Room I", "Room II", "Room III", "Room IV", "Room V", "Room VI", "Room VII"
        );
        JTextArea remarks = AddGuestHelper.remarksArea("N/A");

        updateTenure(arrivalDate, departureDate, tenureField);
        arrivalDate.addChangeListener(e -> updateTenure(arrivalDate, departureDate, tenureField));
        departureDate.addChangeListener(e -> updateTenure(arrivalDate, departureDate, tenureField));
        addDateTimeEditListener(arrivalDate, () -> updateTenure(arrivalDate, departureDate, tenureField));
        addDateTimeEditListener(departureDate, () -> updateTenure(arrivalDate, departureDate, tenureField));

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
        AddGuestHelper.addField(stayCard, stayGbc, y++, 2, "Accommodation", accommodationCombo);
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
        reset.addActionListener(e -> {
            guestNameField.setText("");
            guestCnicField.setText("1234512345671");
            guestNationalityCombo.setSelectedItem("Pakistani");
            guestCategoryCombo.setSelectedIndex(0);
            guestAddressField.setText("");
            requestedByField.setText("");
            requestedDepartmentCombo.setSelectedItem("");
            approvedByField.setText("");
            accommodatedByField.setText("");
            arrivalDate.setValue(initialArrivalDate);
            departureDate.setValue(initialDepartureDate);
            accommodationCombo.setSelectedIndex(0);
            roomCombo.setSelectedIndex(0);
            remarks.setText("N/A");
            updateTenure(arrivalDate, departureDate, tenureField);
        });

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
            JSpinner arrivalDate,
            JSpinner departureDate,
            JTextArea remarks
    ) {
        StringBuilder errors = new StringBuilder();
        String guestName = guestNameField.getText().trim();
        String guestCnic = guestCnicField.getText().trim();
        String guestNationality = String.valueOf(guestNationalityCombo.getEditor().getItem()).trim();
        String guestCategory = String.valueOf(guestCategoryCombo.getSelectedItem()).trim();
        String guestAddress = guestAddressField.getText().trim();
        String requestedBy = requestedByField.getText().trim();
        String requestedDepartment = String.valueOf(requestedDepartmentCombo.getEditor().getItem()).trim();
        String approvedBy = approvedByField.getText().trim();
        String accommodatedBy = accommodatedByField.getText().trim();
        String accommodation = String.valueOf(accommodationCombo.getSelectedItem()).trim();
        String room = String.valueOf(roomCombo.getSelectedItem()).trim();
        String remarksText = remarks.getText().trim();

        if (guestName.isEmpty()) {
            errors.append("Guest Name is required.\n");
            
        }
        if (guestCnic.isEmpty()) {
            errors.append("Guest CNIC is required.\n");
        }
        if (guestNationality.isEmpty()) {
            errors.append("Guest Nationality is required.\n");
        }
        if (guestCategory.isEmpty()) {
            errors.append("Guest Category is required.\n");
        }
        if (guestAddress.isEmpty()) {
            errors.append("Guest Address is required.\n");
        }
        if (requestedBy.isEmpty()) {
            errors.append("Requested By is required.\n");
        }
        if (requestedDepartment.isEmpty()) {
            errors.append("Requested Department is required.\n");
        }
        if (approvedBy.isEmpty()) {
            errors.append("Approved By is required.\n");
        }
        if (accommodatedBy.isEmpty()) {
            errors.append("Accommodated By is required.\n");
        }
        if (tenureField.getText().startsWith("Departure must")) {
            errors.append(tenureField.getText()).append(".\n");
        }
        if (accommodation.isEmpty()) {
            errors.append("Accommodation is required.\n");
        }
        if (room.isEmpty()) {
            errors.append("Room is required.\n");
        }
        if (remarksText.isEmpty()) {
            errors.append("Remarks are required.\n");
        }
        if (errors.length() > 0) {
            DialogHelper.error(parent, "Please complete required fields", errors.toString());
            return;
        }

        System.out.println("Guest Name: " + guestName);
        System.out.println("Guest CNIC: " + guestCnic);
        System.out.println("Guest Nationality: " + guestNationality);
        System.out.println("Guest Category: " + guestCategory);
        System.out.println("Guest Address: " + guestAddress);
        System.out.println("Requested By: " + requestedBy);
        System.out.println("Requested Department: " + requestedDepartment);
        System.out.println("Approved By: " + approvedBy);
        System.out.println("Accommodated By: " + accommodatedBy);
        System.out.println("Arrival Date: " + arrivalDate.getValue());
        System.out.println("Departure Date: " + departureDate.getValue());
        System.out.println("Tenure: " + tenureField.getText());
        System.out.println("Accommodation: " + accommodation);
        System.out.println("Room: " + room);
        System.out.println("Remarks: " + remarksText);

        DialogHelper.success(parent, "Guest added successfully.");
    }

    private static Date dateTimeValue(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hour, minute, 0);
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

    private static void updateTenure(JSpinner arrivalDate, JSpinner departureDate, JTextField tenureField) {
        Date arrival = (Date) arrivalDate.getValue();
        Date departure = (Date) departureDate.getValue();
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

        tenureField.setText(days + " days " + hours + " hours");
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
