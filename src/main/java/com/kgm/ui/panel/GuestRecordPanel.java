package com.kgm.ui.panel;

import com.kgm.dao.GuestDao;
import com.kgm.model.Guest;
import com.kgm.service.GuestValidationService;
import com.kgm.ui.dialog.DelayedProgressDialog;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class GuestRecordPanel extends JPanel {
    public static final int NAME = 0;
    public static final int CNIC = 1;
    public static final int NATIONALITY = 2;
    public static final int CATEGORY = 3;
    public static final int ADDRESS = 4;
    public static final int REQUESTED_BY = 5;
    public static final int DEPARTMENT = 6;
    public static final int APPROVED_BY = 7;
    public static final int ACCOMMODATED_BY = 8;
    public static final int ARRIVAL = 9;
    public static final int DEPARTURE = 10;
    public static final int ACCOMMODATION = 11;
    public static final int ROOM = 12;
    public static final int REMARKS = 13;
    public static final int REVIEW = 14;
    public static final int ID = 15;
    private static final int GUEST_NAME_TABLE_LIMIT = 13;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final GuestDao guestDao = new GuestDao();
    private final List<Object[]> allData = new ArrayList<>();
    private final List<Object[]> visibleRecords = new ArrayList<>();
    private final Consumer<Object[]> onViewGuest;
    private final RefreshIcon refreshIcon = new RefreshIcon(16, HomeViewHelper.PRIMARY);
    private JButton refreshButton;
    private Timer refreshAnimation;
    private SwingWorker<List<Object[]>, Void> refreshWorker;
    private final Runnable onReportRequest;
    private final Long accommodationId;
    private final Runnable onDataChanged;

    private final UniversalTablePanel guestTable = new UniversalTablePanel(
            new String[]{"Guest Name", "Arrival", "Departure", "Accommodation", "Requested Department", "Status", "Actions"},
            "No guest records found."
    );

    public GuestRecordPanel(Consumer<Object[]> onViewGuest) {
        this(onViewGuest, null, null, null);
    }

    public GuestRecordPanel(Consumer<Object[]> onViewGuest, Runnable onReportRequest) {
        this(onViewGuest, onReportRequest, null, null);
    }

    public GuestRecordPanel(Consumer<Object[]> onViewGuest, Runnable onReportRequest, Long accommodationId) {
        this(onViewGuest, onReportRequest, accommodationId, null);
    }

    public GuestRecordPanel(
            Consumer<Object[]> onViewGuest,
            Runnable onReportRequest,
            Long accommodationId,
            Runnable onDataChanged
    ) {
        this.onViewGuest = onViewGuest;
        this.onReportRequest = onReportRequest;
        this.accommodationId = accommodationId;
        this.onDataChanged = onDataChanged;
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel card = HomeViewHelper.sectionCard(
                "Guest Records",
                "Current, upcoming, and departed guest movements.",
                headerActions()
        );
        guestTable.setActionColumn(6, "View", row -> showGuestDetails(row));
        guestTable.setStatusColumn(5, this::confirmDeleteBooking, this::isUpcomingGuest);
        guestTable.setHugColumn(0); // Guest Name
        guestTable.setColumnAlignment(1, SwingConstants.CENTER); // Arrival
        guestTable.setColumnAlignment(2, SwingConstants.CENTER); // Departure
        guestTable.setColumnAlignment(3, SwingConstants.CENTER); // Accommodation
        guestTable.setColumnAlignment(4, SwingConstants.CENTER); // Requested Department
        card.add(guestTable, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
        refreshFromDatabaseAsync(false);
    }

    public void refreshFromDatabase() {
        try {
            setGuestRecords(loadGuestRecords());
        } catch (SQLException exception) {
            allData.clear();
            setVisibleRecords(new ArrayList<>());
        }
    }

    private JButton refreshButton() {
        JButton button = new JButton(refreshIcon);
        refreshButton = button;
        button.setPreferredSize(new Dimension(34, 34));
        button.setMinimumSize(new Dimension(34, 34));
        button.setMaximumSize(new Dimension(34, 34));
        button.setToolTipText("Refresh Data");
        button.getAccessibleContext().setAccessibleName("Refresh Data");
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setDisabledIcon(refreshIcon);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(event -> refreshFromDatabaseAsync(true));
        return button;
    }

    private JPanel headerActions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);
        if (onReportRequest != null) {
            actions.add(reportLabel());
        }
        actions.add(refreshButton());
        return actions;
    }

    private JLabel reportLabel() {
        JLabel label = new JLabel("Download Report");
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setForeground(HomeViewHelper.PRIMARY);
        label.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                onReportRequest.run();
            }

            public void mouseEntered(MouseEvent event) {
                label.setForeground(HomeViewHelper.PRIMARY_DARK);
            }

            public void mouseExited(MouseEvent event) {
                label.setForeground(HomeViewHelper.PRIMARY);
            }
        });
        return label;
    }

    public void refreshFromDatabaseAsync(boolean showSuccess) {
        if (refreshWorker != null && !refreshWorker.isDone()) {
            return;
        }

        startRefreshAnimation();
        DelayedProgressDialog.Handle progress = DelayedProgressDialog.showAfter(
                this,
                "Loading Guest Records",
                "Database is taking longer than usual. Loading guest records..."
        );
        refreshWorker = new SwingWorker<>() {
            protected List<Object[]> doInBackground() throws Exception {
                return loadGuestRecords();
            }

            protected void done() {
                try {
                    setGuestRecords(get());
                    if (showSuccess) {
                        DialogHelper.success(GuestRecordPanel.this, "Data refreshed successfully.");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    allData.clear();
                    setVisibleRecords(new ArrayList<>());
                    Throwable cause = exception.getCause();
                    String message = cause == null ? exception.getMessage() : cause.getMessage();
                    System.err.println("Guest records refresh failed: " + message);
                    DialogHelper.error(GuestRecordPanel.this, "Refresh failed", message);
                } finally {
                    progress.done();
                    stopRefreshAnimation();
                    refreshWorker = null;
                }
            }
        };
        refreshWorker.execute();
    }

    private List<Object[]> loadGuestRecords() throws SQLException {
        List<Object[]> records = new ArrayList<>();
        List<Guest> guests = accommodationId == null
                ? guestDao.findAll()
                : guestDao.findByAccommodationId(accommodationId);
        for (Guest guest : guests) {
            records.add(toRecord(guest));
        }
        return records;
    }

    private void setGuestRecords(List<Object[]> records) {
        allData.clear();
        allData.addAll(records);
        reset();
    }

    private void startRefreshAnimation() {
        refreshIcon.setAngle(0);
        if (refreshButton != null) {
            refreshButton.setEnabled(false);
            refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        if (refreshAnimation != null) {
            refreshAnimation.stop();
        }
        refreshAnimation = new Timer(60, event -> {
            refreshIcon.rotate(28);
            if (refreshButton != null) {
                refreshButton.repaint();
            }
        });
        refreshAnimation.start();
    }

    private void stopRefreshAnimation() {
        if (refreshAnimation != null) {
            refreshAnimation.stop();
            refreshAnimation = null;
        }
        refreshIcon.setAngle(0);
        if (refreshButton != null) {
            refreshButton.setEnabled(true);
            refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            refreshButton.repaint();
        }
    }

    public void search(String query) {
        search(query, "All Status", "");
    }

    public void search(String query, String status, String date) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        String normalizedStatus = status == null ? "All Status" : status.trim();
        String normalizedDate = date == null ? "" : date.trim();

        boolean hasStatus = !normalizedStatus.equalsIgnoreCase("All Status");
        boolean hasDate = !normalizedDate.isEmpty();

        if (normalizedQuery.isEmpty() && !hasStatus && !hasDate) {
            reset();
            return;
        }

        List<Object[]> filteredRecords = new ArrayList<>();
        for (Object[] record : allData) {
            if (recordMatches(record, normalizedQuery)
                    && statusMatches(record, normalizedStatus)
                    && dateMatches(record, normalizedDate)) {
                filteredRecords.add(record);
            }
        }
        setVisibleRecords(filteredRecords);
    }

    public void reset() {
        setVisibleRecords(allRows());
    }

    private boolean recordMatches(Object[] record, String query) {
        if (cnicMatches(record[CNIC], query)) {
            return true;
        }
        Object[] values = {
                record[NAME]
        };
        for (Object value : values) {
            if (String.valueOf(value).toLowerCase().contains(query)) {
                return true;
            }
        }
        return false;
    }

    private boolean cnicMatches(Object cnic, String query) {
        String cnicText = String.valueOf(cnic).toLowerCase();
        if (cnicText.contains(query)) {
            return true;
        }
        String cnicDigits = digitsOnly(cnicText);
        String queryDigits = digitsOnly(query);
        return !queryDigits.isEmpty() && cnicDigits.contains(queryDigits);
    }

    private String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private boolean statusMatches(Object[] record, String status) {
        if (status.equalsIgnoreCase("All Status")) {
            return true;
        }

        return statusText(record).equalsIgnoreCase(status);
    }

    private boolean dateMatches(Object[] record, String date) {
        return date.isEmpty()
                || String.valueOf(record[ARRIVAL]).startsWith(date)
                || String.valueOf(record[DEPARTURE]).startsWith(date);
    }

    private String dateTimeText(Object value) {
        return String.valueOf(value);
    }

    private String statusText(Object[] record) {
        LocalDateTime arrival = parseDateTime(record[ARRIVAL]);
        LocalDateTime departure = parseDateTime(record[DEPARTURE]);
        if (arrival == null || departure == null) {
            return "Unknown";
        }
        return GuestValidationService.stayStatus(toDate(arrival), toDate(departure));
    }

    private Date toDate(LocalDateTime value) {
        return Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
    }

    private LocalDateTime parseDateTime(Object value) {
        try {
            String text = String.valueOf(value);
            if (text.length() >= 16) {
                text = text.substring(0, 16);
            }
            return LocalDateTime.parse(text, DATE_TIME);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String tenureText(Object[] record) {
        LocalDateTime arrival = parseDateTime(record[ARRIVAL]);
        LocalDateTime departure = parseDateTime(record[DEPARTURE]);
        if (arrival == null || departure == null) {
            return "-";
        }
        if (departure.isBefore(arrival)) {
            return "-";
        }
        long totalHours = java.time.Duration.between(arrival, departure).toHours();
        long days = totalHours / 24;
        long hours = totalHours % 24;
        return tenureText(days, hours);
    }

    private String tenureText(long days, long hours) {
        String dayText = days + (days == 1 ? " day" : " days");
        String hourText = hours + (hours == 1 ? " hour" : " hours");
        return dayText + " " + hourText;
    }

    private List<Object[]> allRows() {
        return new ArrayList<>(allData);
    }

    private void setVisibleRecords(List<Object[]> records) {
        visibleRecords.clear();
        visibleRecords.addAll(records);
        guestTable.setRows(toTableRows(records));
    }

    private List<Object[]> toTableRows(List<Object[]> source) {
        List<Object[]> rows = new ArrayList<>();
        for (Object[] record : source) {
            rows.add(toTableRow(record));
        }
        return rows;
    }

    private Object[] toTableRow(Object[] record) {
        return new Object[]{
                guestNameTableText(record[NAME]),
                dateTimeText(record[ARRIVAL]),
                dateTimeText(record[DEPARTURE]),
                accommodationText(record),
                record[DEPARTMENT],
                statusText(record),
                "View"
        };
    }

    private String guestNameTableText(Object value) {
        String text = value == null ? "" : String.valueOf(value).trim();
        if (text.length() <= GUEST_NAME_TABLE_LIMIT) {
            return text;
        }
        return text.substring(0, GUEST_NAME_TABLE_LIMIT - 2) + "..";
    }

    private String accommodationText(Object[] record) {
        String category = String.valueOf(record[ACCOMMODATION]);
        String room = String.valueOf(record[ROOM]);
        if ((category == null || category.isEmpty() || category.equals("null")) &&
            (room == null || room.isEmpty() || room.equals("null"))) {
            return "-";
        }
        if (category == null || category.isEmpty() || category.equals("null")) {
            return room;
        }
        if (room == null || room.isEmpty() || room.equals("null")) {
            return category;
        }
        return category + " : " + room;
    }

    private void showGuestDetails(int row) {
        if (row < 0 || row >= visibleRecords.size()) {
            return;
        }
        onViewGuest.accept(visibleRecords.get(row));
    }

    private boolean isUpcomingGuest(int row) {
        if (row < 0 || row >= visibleRecords.size()) {
            return false;
        }
        Object[] record = visibleRecords.get(row);
        String status = statusText(record);
        return "Upcoming".equalsIgnoreCase(status);
    }

    private void confirmDeleteBooking(int row) {
        if (row < 0 || row >= visibleRecords.size()) {
            return;
        }
        Object[] record = visibleRecords.get(row);
        String guestName = String.valueOf(record[NAME]);
        String arrivalDate = String.valueOf(record[ARRIVAL]);

        int result = DialogHelper.option(
                this,
                "Cancel Upcoming Booking",
                "Are you sure you want to cancel the upcoming booking for \"" + guestName + "\" (Arrival: " + arrivalDate + ")? This action cannot be undone and the reserved room will be released back to available inventory.",
                "Yes, Cancel Booking",
                "Keep Booking"
        );

        if (result == 0) { // Primary option (Yes) was selected
            deleteBooking(row, record);
        }
    }

    private void deleteBooking(int row, Object[] record) {
        long guestId = (Long) record[ID];
        
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                guestDao.delete(guestId);
                return true;
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        // Remove from visible records and refresh
                        visibleRecords.remove(row);
                        allData.remove(record);
                        guestTable.setRows(toTableRows(visibleRecords));
                        if (onDataChanged != null) {
                            onDataChanged.run();
                        }
                        DialogHelper.success(GuestRecordPanel.this, "Booking cancelled successfully. The room has been released.");
                    }
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    DialogHelper.error(GuestRecordPanel.this, "Cancellation Failed", cause.getMessage());
                }
            }
        }.execute();
    }

    private Object[] toRecord(Guest guest) {
        return new Object[]{
                guest.getGuestName(),
                guest.getCnic(),
                guest.getNationality(),
                guest.getGuestCategory(),
                guest.getAddress(),
                guest.getRequestedBy(),
                guest.getRequestedDepartment(),
                guest.getApprovedBy(),
                guest.getAccommodatedBy(),
                formatDateTime(guest.getArrivalAt()),
                formatDateTime(guest.getDepartureAt()),
                guest.getAccommodation(),
                guest.getRoomName(),
                guest.getRemarks(),
                guest.getReview(),
                guest.getId()
        };
    }

    private String formatDateTime(java.util.Date date) {
        if (date == null) {
            return "";
        }
        return LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault()).format(DATE_TIME);
    }

    private static class RefreshIcon implements Icon {
        private final int size;
        private final Color color;
        private int angle;

        private RefreshIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        public int getIconWidth() {
            return size;
        }

        public int getIconHeight() {
            return size;
        }

        private void setAngle(int angle) {
            this.angle = angle % 360;
        }

        private void rotate(int degrees) {
            setAngle(angle + degrees);
        }

        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.rotate(Math.toRadians(angle), x + size / 2.0, y + size / 2.0);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int inset = 3;
            int diameter = size - inset * 2;
            g2.drawArc(x + inset, y + inset, diameter, diameter, 35, 280);

            int arrowX = x + size - 4;
            int arrowY = y + 5;
            Polygon arrow = new Polygon();
            arrow.addPoint(arrowX, arrowY);
            arrow.addPoint(arrowX - 7, arrowY);
            arrow.addPoint(arrowX - 3, arrowY + 6);
            g2.fillPolygon(arrow);
            g2.dispose();
        }
    }
}
