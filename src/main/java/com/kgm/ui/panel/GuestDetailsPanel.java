package com.kgm.ui.panel;

import com.kgm.dao.GuestDao;
import com.kgm.ui.styling.AddGuestHelper;
import com.kgm.ui.styling.DialogHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GuestDetailsPanel extends JPanel {
    private static final SimpleDateFormat DATE_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final GuestDao guestDao = new GuestDao();

    public GuestDetailsPanel(Object[] record, Runnable onBack) {
        this(record, onBack, () -> {
        });
    }

    public GuestDetailsPanel(Object[] record, Runnable onBack, Runnable onUpdated) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        add(createContent(record, onBack, onUpdated), BorderLayout.CENTER);
    }

    private JComponent createContent(Object[] record, Runnable onBack, Runnable onUpdated) {
        final JScrollPane[] scrollRef = new JScrollPane[1];
        JPanel page = AddGuestHelper.pagePanel();
        page.add(AddGuestHelper.screenHeader(
                "Viewing Guest",
                "Review guest details and update departure or remarks.",
                onBack
        ), AddGuestHelper.pageConstraints(0));

        Date arrivalValue = parseDate(value(record, GuestRecordPanel.ARRIVAL));
        Date departureValue = parseDate(value(record, GuestRecordPanel.DEPARTURE));
        boolean canEditStay = !isNaturallyDeparted(arrivalValue, departureValue);
        JSpinner arrivalDate = dateTimeSpinner(arrivalValue);
        JSpinner departureDate = dateTimeSpinner(departureValue);
        JTextField tenureField = lockedField("");
        JTextField statusField = lockedField("");
        String remarksText = value(record, GuestRecordPanel.REMARKS);
        JTextArea remarks = AddGuestHelper.remarksArea(remarksText);
        setRemarksEditable(remarks, canEditStay);

        arrivalDate.setEnabled(false);
        departureDate.setEnabled(canEditStay);
        updateStaySummary(arrivalDate, departureDate, tenureField, statusField);
        departureDate.addChangeListener(e -> updateStaySummary(arrivalDate, departureDate, tenureField, statusField));
        addDateTimeEditListener(departureDate, () -> updateStaySummary(arrivalDate, departureDate, tenureField, statusField));

        JPanel basicCard = AddGuestHelper.cardPanel();
        GridBagConstraints basicGbc = AddGuestHelper.formConstraints();
        int y = AddGuestHelper.addSectionTitle(basicCard, basicGbc, 0, "Basic Information");
        AddGuestHelper.addField(basicCard, basicGbc, y, 0, "Guest Name", lockedField(value(record, GuestRecordPanel.NAME)));
        AddGuestHelper.addField(basicCard, basicGbc, y++, 2, "Guest CNIC", lockedField(value(record, GuestRecordPanel.CNIC)));
        AddGuestHelper.addField(basicCard, basicGbc, y, 0, "Guest Nationality", lockedField(value(record, GuestRecordPanel.NATIONALITY)));
        AddGuestHelper.addField(basicCard, basicGbc, y++, 2, "Guest Category", lockedField(value(record, GuestRecordPanel.CATEGORY)));
        AddGuestHelper.addField(basicCard, basicGbc, y, 0, "Guest Address", lockedField(value(record, GuestRecordPanel.ADDRESS)));

        JPanel requestCard = AddGuestHelper.cardPanel();
        GridBagConstraints requestGbc = AddGuestHelper.formConstraints();
        y = AddGuestHelper.addSectionTitle(requestCard, requestGbc, 0, "Request Details");
        AddGuestHelper.addField(requestCard, requestGbc, y, 0, "Requested By", lockedField(value(record, GuestRecordPanel.REQUESTED_BY)));
        AddGuestHelper.addField(requestCard, requestGbc, y++, 2, "Requested Department", lockedField(value(record, GuestRecordPanel.DEPARTMENT)));
        AddGuestHelper.addField(requestCard, requestGbc, y, 0, "Approved By", lockedField(value(record, GuestRecordPanel.APPROVED_BY)));
        AddGuestHelper.addField(requestCard, requestGbc, y++, 2, "Accommodated By", lockedField(value(record, GuestRecordPanel.ACCOMMODATED_BY)));

        JPanel stayCard = AddGuestHelper.cardPanel();
        GridBagConstraints stayGbc = AddGuestHelper.formConstraints();
        y = AddGuestHelper.addSectionTitle(stayCard, stayGbc, 0, "Stay Details");
        AddGuestHelper.addField(stayCard, stayGbc, y, 0, "Arrival Date", arrivalDate);
        AddGuestHelper.addField(stayCard, stayGbc, y++, 2, "Departure Date", departureDate);
        AddGuestHelper.addField(stayCard, stayGbc, y, 0, "Tenure", tenureField);
        AddGuestHelper.addField(stayCard, stayGbc, y++, 2, "Accommodation Category", lockedField(value(record, GuestRecordPanel.ACCOMMODATION)));
        AddGuestHelper.addField(stayCard, stayGbc, y, 0, "Room", lockedField(value(record, GuestRecordPanel.ROOM)));
        AddGuestHelper.addField(stayCard, stayGbc, y++, 2, "Status", statusField);

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
        JButton back = new JButton("Back");
        AddGuestHelper.styleReset(back);
        back.addActionListener(e -> onBack.run());
        JButton update = new JButton("Update Guest");
        AddGuestHelper.stylePrimary(update);
        update.setEnabled(canEditStay);
        update.addActionListener(e -> {
            if (!remarks.isEditable() && !departureDate.isEnabled()) {
                DialogHelper.warning(this, "No editable fields", "This departed guest record cannot be changed.");
                return;
            }
            updateStaySummary(arrivalDate, departureDate, tenureField, statusField);
            if (tenureField.getText().startsWith("Departure must")) {
                DialogHelper.error(this, "Invalid departure", tenureField.getText());
                return;
            }
            try {
                String nextRemarks = remarks.getText().trim();
                if (!remarks.isEditable()) {
                    nextRemarks = value(record, GuestRecordPanel.REMARKS);
                }
                if (nextRemarks.isEmpty()) {
                    nextRemarks = "N/A";
                }
                guestDao.updateDepartureAndRemarks(recordId(record), (Date) departureDate.getValue(), nextRemarks);
                record[GuestRecordPanel.DEPARTURE] = DATE_TIME.format((Date) departureDate.getValue());
                record[GuestRecordPanel.REMARKS] = nextRemarks;
                updateStatus(arrivalDate, departureDate, statusField);
                if (isNaturallyDeparted((Date) arrivalDate.getValue(), (Date) departureDate.getValue())) {
                    departureDate.setEnabled(false);
                    setRemarksEditable(remarks, false);
                    update.setEnabled(false);
                }
                onUpdated.run();
                DialogHelper.success(this, "Guest details updated.");
            } catch (SQLException exception) {
                DialogHelper.error(this, "Guest not updated", exception.getMessage());
            }
        });
        actions.add(back);
        actions.add(update);

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
        Timer statusTimer = new Timer(1000, event -> {
            updateStatus(arrivalDate, departureDate, statusField);
            if (isNaturallyDeparted((Date) arrivalDate.getValue(), (Date) departureDate.getValue())) {
                departureDate.setEnabled(false);
                setRemarksEditable(remarks, false);
                update.setEnabled(false);
            }
        });
        statusTimer.start();
        scroll.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0 && !scroll.isDisplayable()) {
                statusTimer.stop();
            }
        });
        return scroll;
    }

    private String value(Object[] record, int index) {
        if (record == null || index < 0 || index >= record.length || record[index] == null) {
            return "";
        }
        return String.valueOf(record[index]);
    }

    private long recordId(Object[] record) {
        if (record == null || GuestRecordPanel.ID >= record.length || record[GuestRecordPanel.ID] == null) {
            return 0;
        }
        if (record[GuestRecordPanel.ID] instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(record[GuestRecordPanel.ID]));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private JTextField lockedField(String value) {
        JTextField field = new JTextField(value);
        field.setEditable(false);
        field.setFocusable(false);
        field.setForeground(new Color(70, 70, 70));
        field.setBackground(Color.WHITE);
        return field;
    }

    private static void setRemarksEditable(JTextArea remarks, boolean editable) {
        remarks.setEditable(editable);
        remarks.setFocusable(editable);
        remarks.setBackground(editable ? Color.WHITE : new Color(248, 248, 248));
    }

    private static boolean isDeparted(Date departure) {
        return departure != null && !departure.after(new Date());
    }

    private static boolean isNaturallyDeparted(Date arrival, Date departure) {
        return arrival != null
                && departure != null
                && !departure.before(arrival)
                && isDeparted(departure);
    }

    private static Date parseDate(String value) {
        try {
            return DATE_TIME.parse(value);
        } catch (ParseException ignored) {
            return new Date();
        }
    }

    private static JSpinner dateTimeSpinner(Date value) {
        JSpinner spinner = new JSpinner(new SpinnerDateModel(value, null, null, java.util.Calendar.MINUTE));
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

        tenureField.setText(tenureText(days, hours));
    }

    private static String tenureText(long days, long hours) {
        String dayText = days + (days == 1 ? " day" : " days");
        String hourText = hours + (hours == 1 ? " hour" : " hours");
        return dayText + " " + hourText;
    }

    private static void updateStaySummary(
            JSpinner arrivalDate,
            JSpinner departureDate,
            JTextField tenureField,
            JTextField statusField
    ) {
        updateTenure(arrivalDate, departureDate, tenureField);
        updateStatus(arrivalDate, departureDate, statusField);
    }

    private static void updateStatus(JSpinner arrivalDate, JSpinner departureDate, JTextField statusField) {
        Date arrival = (Date) arrivalDate.getValue();
        Date departure = (Date) departureDate.getValue();
        Date now = new Date();

        if (departure.before(arrival)) {
            statusField.setForeground(new Color(180, 60, 45));
            statusField.setText("Invalid Dates");
            return;
        }
        if (!departure.after(now)) {
            statusField.setForeground(new Color(180, 60, 45));
            statusField.setText("Departed");
            return;
        }
        if (arrival.after(now)) {
            statusField.setForeground(new Color(0, 112, 210));
            statusField.setText("Upcoming");
            return;
        }

        statusField.setForeground(new Color(38, 128, 64));
        statusField.setText("Currently Staying");
    }

    private static void addDateTimeEditListener(JSpinner spinner, Runnable onChange) {
        JFormattedTextField textField = ((JSpinner.DateEditor) spinner.getEditor()).getTextField();
        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                commitAndUpdate();
            }

            public void removeUpdate(DocumentEvent event) {
                commitAndUpdate();
            }

            public void changedUpdate(DocumentEvent event) {
                commitAndUpdate();
            }

            private void commitAndUpdate() {
                SwingUtilities.invokeLater(() -> {
                    try {
                        spinner.commitEdit();
                        onChange.run();
                    } catch (ParseException ignored) {
                    }
                });
            }
        });
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
}
