package com.kgm.ui;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.styling.AddGuestHelper;
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

        JPanel page = AddGuestHelper.pagePanel();
        JPanel card = AddGuestHelper.cardPanel();
        GridBagConstraints gbc = AddGuestHelper.formConstraints();
        int y = 0;

        y = AddGuestHelper.addFormHeader(card, gbc, y, () -> {
            new HomeView();
            dispose();
        });

        // Basic Info
        y = AddGuestHelper.addSectionTitle(card, gbc, y, "Basic Information");
        JTextField guestNameField = new JTextField("");
        JTextField guestCnicField = new JTextField("1234512345671");
        JComboBox<String> guestAssociationCombo = AddGuestHelper.combo("Government", "Private", "Other");
        JComboBox<String> guestCategoryCombo = AddGuestHelper.combo("Family", "Non-Family");
        JTextField guestAddressField = new JTextField("");
        AddGuestHelper.addField(card, gbc, y, 0, "Guest Name", guestNameField);
        AddGuestHelper.addField(card, gbc, y++, 2, "Guest CNIC", guestCnicField);
        AddGuestHelper.addField(card, gbc, y, 0, "Guest Association", guestAssociationCombo);
        AddGuestHelper.addField(card, gbc, y++, 2, "Guest Category", guestCategoryCombo);
        AddGuestHelper.addField(card, gbc, y, 0, "Guest Address", guestAddressField);
        y++;
        // Request Details
        y = AddGuestHelper.addSectionTitle(card, gbc, y, "Request Details");
        JTextField requestedByField = new JTextField("");
        JComboBox<String> requestedDepartmentCombo = AddGuestHelper.editableCombo("HR", "Admin", "Finance", "Spinning",
                "Others (Specify)");
        requestedDepartmentCombo.setSelectedItem("");
        JTextField approvedByField = new JTextField("");
        JTextField accommodatedByField = new JTextField("");
        AddGuestHelper.addField(card, gbc, y, 0, "Requested By", requestedByField);
        AddGuestHelper.addField(card, gbc, y++, 2, "Requested Department", requestedDepartmentCombo);
        AddGuestHelper.addField(card, gbc, y, 0, "Approved By", approvedByField);
        AddGuestHelper.addField(card, gbc, y++, 2, "Accommodated By", accommodatedByField);
        // Stay Details
        y = AddGuestHelper.addSectionTitle(card, gbc, y, "Stay Details");
        Date initialArrivalDate = dateTimeValue(2026, Calendar.MAY, 10, 9, 0);
        Date initialDepartureDate = dateTimeValue(2026, Calendar.MAY, 15, 9, 0);
        JSpinner arrivalDate = dateTimeSpinner(initialArrivalDate);
        JSpinner departureDate = dateTimeSpinner(initialDepartureDate);
        JTextField tenureField = new JTextField();
        tenureField.setEditable(false);
        tenureField.setFocusable(false);
        tenureField.setBackground(Color.WHITE);
        JComboBox<String> accommodationCombo = AddGuestHelper.combo("Guest Room", "Guest House");
        JComboBox<String> roomCombo = AddGuestHelper.combo("Room I", "Room II", "Room III", "Room IV", "Room V",
                "Room VI", "Room VII");
        AddGuestHelper.addField(card, gbc, y, 0, "Arrival Date", arrivalDate);
        AddGuestHelper.addField(card, gbc, y++, 2, "Departure Date", departureDate);
        updateTenure(arrivalDate, departureDate, tenureField);
        arrivalDate.addChangeListener(e -> updateTenure(arrivalDate, departureDate, tenureField));
        departureDate.addChangeListener(e -> updateTenure(arrivalDate, departureDate, tenureField));
        addDateTimeEditListener(arrivalDate, () -> updateTenure(arrivalDate, departureDate, tenureField));
        addDateTimeEditListener(departureDate, () -> updateTenure(arrivalDate, departureDate, tenureField));
        AddGuestHelper.addField(card, gbc, y, 0, "Tenure", tenureField);
        AddGuestHelper.addField(card, gbc, y, 2, "Accommodation", accommodationCombo);
        y++;
        AddGuestHelper.addField(card, gbc, y, 0, "Room", roomCombo);
        y++;
        // Remarks
        JLabel remarksLabel = AddGuestHelper.label("Remarks");
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 4;
        card.add(remarksLabel, gbc);
        y++;
        JTextArea remarks = AddGuestHelper.remarksArea("N/A");
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipady = 80;
        card.add(remarks, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.ipady = 0;
        gbc.gridwidth = 1;
        y++;
        // Buttons
        JPanel actions = AddGuestHelper.actionsPanel();
        JButton reset = new JButton("Reset");
        AddGuestHelper.styleReset(reset);
        reset.addActionListener(e -> {
            // guestNameField.setText("");
            guestCnicField.setText("1234512345671");
            guestAssociationCombo.setSelectedIndex(0);
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
        submit.addActionListener(e -> {
            StringBuilder errors = new StringBuilder();
            String guestName = guestNameField.getText().trim();
            String guestCnic = guestCnicField.getText().trim();
            String guestAssociation = String.valueOf(guestAssociationCombo.getSelectedItem()).trim();
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
            if (guestAssociation.isEmpty()) {
                errors.append("Guest Association is required.\n");
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
                JOptionPane.showMessageDialog(
                        this,
                        errors.toString(),
                        "Please complete required fields",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            System.out.println("Guest Name: " + guestName);
            System.out.println("Guest CNIC: " + guestCnic);
            System.out.println("Guest Association: " + guestAssociation);
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
            JOptionPane.showMessageDialog(
                    this,
                    "Guest added successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        });
        actions.add(reset);
        actions.add(submit);
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 4;
        card.add(actions, gbc);
        page.add(card);
        // SCROLL ENABLED
        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(new HeaderPanel("Add Guest"), BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(900, 650));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        SwingUtilities.invokeLater(() -> {
            scroll.getVerticalScrollBar().setValue(0);
            scroll.getHorizontalScrollBar().setValue(0);
        });
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AddGuest().setVisible(true));
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
