package com.kgm.ui.panel;

import com.kgm.dao.GuestDao;
import com.kgm.model.Guest;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
    public static final int REVIEW = 13;
    public static final int ID = 14;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final GuestDao guestDao = new GuestDao();
    private final List<Object[]> allData = new ArrayList<>();
    private final List<Object[]> visibleRecords = new ArrayList<>();
    private final Consumer<Object[]> onViewGuest;

    private final UniversalTablePanel guestTable = new UniversalTablePanel(
            new String[]{"Guest Name", "Arrival", "Departure", "Status", "Tenure", "Actions"},
            "No guest records found."
    );

    public GuestRecordPanel(Consumer<Object[]> onViewGuest) {
        this.onViewGuest = onViewGuest;
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel card = HomeViewHelper.sectionCard("Recent Guest Records", "Current guest movements and approvals.");
        guestTable.setActionColumn(5, "View", row -> showGuestDetails(row));
        guestTable.setStatusColumn(3);
        refreshFromDatabase();
        card.add(guestTable, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
    }

    public void refreshFromDatabase() {
        try {
            allData.clear();
            for (Guest guest : guestDao.findAll()) {
                allData.add(toRecord(guest));
            }
            reset();
        } catch (SQLException exception) {
            allData.clear();
            setVisibleRecords(new ArrayList<>());
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
        Object[] values = {
                record[NAME],
                dateTimeText(record[ARRIVAL]),
                dateTimeText(record[DEPARTURE]),
                statusText(record),
                tenureText(record)
        };
        for (Object value : values) {
            if (String.valueOf(value).toLowerCase().contains(query)) {
                return true;
            }
        }
        return false;
    }

    private boolean statusMatches(Object[] record, String status) {
        if (status.equalsIgnoreCase("All Status")) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime arrival = parseDateTime(record[ARRIVAL]);
        LocalDateTime departure = parseDateTime(record[DEPARTURE]);
        if (arrival == null || departure == null) {
            return false;
        }

        if (status.equalsIgnoreCase("Currently Staying")) {
            return !arrival.isAfter(now) && departure.isAfter(now);
        }
        if (status.equalsIgnoreCase("Departed")) {
            return !departure.isAfter(now);
        }
        if (status.equalsIgnoreCase("Upcoming")) {
            return arrival.isAfter(now);
        }
        return true;
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
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime arrival = parseDateTime(record[ARRIVAL]);
        LocalDateTime departure = parseDateTime(record[DEPARTURE]);
        if (arrival == null || departure == null) {
            return "Unknown";
        }
        if (!departure.isAfter(now)) {
            return "Departed";
        }
        if (arrival.isAfter(now)) {
            return "Upcoming";
        }
        return "Currently Staying";
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
                record[NAME],
                dateTimeText(record[ARRIVAL]),
                dateTimeText(record[DEPARTURE]),
                statusText(record),
                tenureText(record),
                "View"
        };
    }

    private void showGuestDetails(int row) {
        if (row < 0 || row >= visibleRecords.size()) {
            return;
        }
        onViewGuest.accept(visibleRecords.get(row));
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
                firstText(guest.getReview(), guest.getRemarks()),
                guest.getId()
        };
    }

    private String formatDateTime(java.util.Date date) {
        if (date == null) {
            return "";
        }
        return LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault()).format(DATE_TIME);
    }

    private String firstText(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return second == null ? "" : second;
    }
}
