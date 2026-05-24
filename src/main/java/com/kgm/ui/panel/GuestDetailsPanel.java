package com.kgm.ui.panel;

import com.kgm.dao.GuestDao;
import com.kgm.ui.component.UniversalDatePicker;
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

        /*
         * Lock departure/update only based on DB/original record state when screen opens.
         * Do not lock again if the live calculated status changes to Departed on screen.
         */
        boolean dbDeparted = isNaturallyDeparted(arrivalValue, departureValue);

        String originalRemarks = value(record, GuestRecordPanel.REMARKS);
        boolean remarksCanEdit = isEmptyRemark(originalRemarks);

        UniversalDatePicker arrivalDate = new UniversalDatePicker(arrivalValue);
        UniversalDatePicker departureDate = new UniversalDatePicker(departureValue);

        JTextField tenureField = lockedField("");
        JTextField statusField = lockedField("");

        JTextArea remarks = AddGuestHelper.remarksArea(originalRemarks);
        setRemarksEditable(remarks, remarksCanEdit);

        arrivalDate.setEnabled(false);
        departureDate.setEnabled(!dbDeparted);

        updateStaySummary(arrivalDate, departureDate, tenureField, statusField);
        departureDate.addDateChangeListener(() -> updateStaySummary(arrivalDate, departureDate, tenureField, statusField));

        JPanel basicCard = AddGuestHelper.cardPanel();
        GridBagConstraints basicGbc = AddGuestHelper.formConstraints();
        int y = AddGuestHelper.addSectionTitle(basicCard, basicGbc, 0, "Basic Information");

        AddGuestHelper.addField(basicCard, basicGbc, y, 0, "Guest Name", lockedField(value(record, GuestRecordPanel.NAME)));
        AddGuestHelper.addField(basicCard, basicGbc, y++, 2, "Guest CNIC", lockedField(value(record, GuestRecordPanel.CNIC)));
        AddGuestHelper.addField(basicCard, basicGbc, y, 0, "Guest Nationality", lockedField(value(record, GuestRecordPanel.NATIONALITY)));
        AddGuestHelper.addField(basicCard, basicGbc, y++, 2, "Guest Category", lockedField(value(record, GuestRecordPanel.CATEGORY)));
        AddGuestHelper.addField(basicCard, basicGbc, y, 0, "Company Name", lockedField(value(record, GuestRecordPanel.COMPANY_NAME)));
        AddGuestHelper.addField(basicCard, basicGbc, y++, 2, "Visit Type", lockedField(value(record, GuestRecordPanel.VISIT_TYPE)));
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
        update.setEnabled(!dbDeparted || remarksCanEdit);

        update.addActionListener(e -> {
            if (dbDeparted && !remarks.isEditable()) {
                DialogHelper.warning(this, "No editable fields", "This departed guest record cannot be changed.");
                return;
            }

            updateStaySummary(arrivalDate, departureDate, tenureField, statusField);

            if (tenureField.getText().startsWith("Departure must")) {
                DialogHelper.error(this, "Invalid departure", tenureField.getText());
                return;
            }

            try {
                String nextRemarks;

                if (remarks.isEditable()) {
                    nextRemarks = remarks.getText() == null ? "" : remarks.getText().trim();

                    if (nextRemarks.isEmpty()) {
                        nextRemarks = "N/A";
                    }
                } else {
                    nextRemarks = originalRemarks;

                    if (isEmptyRemark(nextRemarks)) {
                        nextRemarks = "N/A";
                    }
                }

                Date nextDeparture = dbDeparted
                        ? departureValue
                        : departureDate.getDate();

                guestDao.updateDepartureAndRemarks(
                        recordId(record),
                        nextDeparture,
                        nextRemarks
                );

                // Show success dialog first
                DialogHelper.success(this, "Guest Updated Successfully");

                // Reload data from database and refresh UI
                reloadFromDatabase(record, arrivalDate, departureDate, remarks, tenureField, statusField);

                onUpdated.run();
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

            /*
             * Do not disable departure/update here.
             * Live calculated "Departed" should only update text color/status.
             */
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

    private static boolean isEmptyRemark(String remark) {
        if (remark == null) {
            return true;
        }

        String cleaned = remark.trim();

        return cleaned.isEmpty() || cleaned.equalsIgnoreCase("N/A");
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

    private static void updateTenure(
            UniversalDatePicker arrivalDate,
            UniversalDatePicker departureDate,
            JTextField tenureField
    ) {
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

    private static void updateStaySummary(
            UniversalDatePicker arrivalDate,
            UniversalDatePicker departureDate,
            JTextField tenureField,
            JTextField statusField
    ) {
        updateTenure(arrivalDate, departureDate, tenureField);
        updateStatus(arrivalDate, departureDate, statusField);
    }

    private static void updateStatus(
            UniversalDatePicker arrivalDate,
            UniversalDatePicker departureDate,
            JTextField statusField
    ) {
        Date arrival = arrivalDate.getDate();
        Date departure = departureDate.getDate();
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

    private void reloadFromDatabase(
            Object[] record,
            UniversalDatePicker arrivalDate,
            UniversalDatePicker departureDate,
            JTextArea remarks,
            JTextField tenureField,
            JTextField statusField
    ) {
        SwingUtilities.invokeLater(() -> {
            try {
                com.kgm.model.Guest guest = guestDao.findById(recordId(record));
                String arrivalStr = formatDateTime(guest.getArrivalAt());
                String departureStr = formatDateTime(guest.getDepartureAt());

                // Update the record array
                record[GuestRecordPanel.DEPARTURE] = departureStr;
                record[GuestRecordPanel.REMARKS] = guest.getRemarks() != null ? guest.getRemarks() : "";

                // Update departure date picker
                Date newDeparture = parseDate(departureStr);
                departureDate.setDate(newDeparture);

                // Update remarks
                String newRemarks = guest.getRemarks() != null ? guest.getRemarks() : "";
                remarks.setText(newRemarks);

                // Recalculate dbDeparted based on new values
                boolean newDbDeparted = isNaturallyDeparted(arrivalDate.getDate(), newDeparture);

                // Update departure field enabled state
                departureDate.setEnabled(!newDbDeparted);

                // Update remarks editability
                boolean newRemarksCanEdit = isEmptyRemark(newRemarks);
                setRemarksEditable(remarks, newRemarksCanEdit);

                // Update the update button state
                Container parent = getParent();
                if (parent != null) {
                    Component[] components = parent.getComponents();
                    for (Component comp : components) {
                        if (comp instanceof JScrollPane) {
                            JViewport viewport = ((JScrollPane) comp).getViewport();
                            if (viewport.getView() instanceof JPanel) {
                                JPanel page = (JPanel) viewport.getView();
                                for (Component child : page.getComponents()) {
                                    if (child instanceof JPanel) {
                                        JPanel card = (JPanel) child;
                                        for (Component cardChild : card.getComponents()) {
                                            if (cardChild instanceof JPanel && ((JPanel) cardChild).getComponentCount() > 0) {
                                                JPanel actionsPanel = (JPanel) cardChild;
                                                for (Component actionComp : actionsPanel.getComponents()) {
                                                    if (actionComp instanceof JButton && "Update Guest".equals(((JButton) actionComp).getText())) {
                                                        ((JButton) actionComp).setEnabled(!newDbDeparted || newRemarksCanEdit);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Update stay summary (tenure and status)
                updateStaySummary(arrivalDate, departureDate, tenureField, statusField);

            } catch (SQLException exception) {
                DialogHelper.error(this, "Reload failed", "Could not reload guest data: " + exception.getMessage());
            }
        });
    }

    private String formatDateTime(Date date) {
        if (date == null) {
            return "";
        }
        return DATE_TIME.format(date);
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
