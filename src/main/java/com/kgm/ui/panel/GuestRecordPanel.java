package com.kgm.ui.panel;

import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GuestRecordPanel extends JPanel {
    public static final int NAME = 0;
    public static final int CNIC = 1;
    public static final int ASSOCIATION = 2;
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

    private final Object[][] allData = {
            {"Ali Khan", "3520112345671", "Government", "Family", "Islamabad", "Mate", "IT", "Mate", "Admin Office", "2026-05-01", "2026-05-01", "Guest Room", "Room I", "Comfortable stay."},
            {"Sara Ahmed", "3520112345672", "Private", "Non-Family", "Rawalpindi", "HR Desk", "HR", "Mate", "Admin Office", "2026-05-02", "2026-05-02", "Guest House", "Room II", "Arrived on schedule."},
            {"Usman Tariq", "3520112345673", "Government", "Family", "Lahore", "Ops Coordinator", "Ops", "Adnan Latif", "Admin Office", "2026-05-02", "2026-05-03", "Guest Room", "Room III", "Requires transport follow-up."},
            {"Hassan Raza", "3520112345674", "Private", "Family", "Faisalabad", "Finance Desk", "Finance", "Adnan Latif", "Admin Office", "2026-05-03", "2026-05-04", "Guest House", "Room IV", "No issues reported."},
            {"Bilal Khan", "3520112345675", "Government", "Non-Family", "Karachi", "Sales Desk", "Sales", "Adnan Latif", "Admin Office", "2026-05-04", "2026-05-04", "Guest Room", "Room V", "Short stay completed."},
            {"Ayesha Noor", "3520112345676", "Private", "Family", "Multan", "IT Desk", "IT", "Adnan Latif", "Admin Office", "2026-05-04", "2026-05-05", "Guest House", "Room VI", "Extended checkout requested."},
            {"Zain Ali", "3520112345677", "Government", "Non-Family", "Peshawar", "HR Desk", "HR", "Adnan Latif", "Admin Office", "2026-05-05", "2026-05-05", "Guest Room", "Room VII", "Guest checked in smoothly."},
            {"Noman", "3520112345678", "Private", "Non-Family", "Quetta", "Ops Desk", "Ops", "Adnan Latif", "Admin Office", "2026-05-05", "2026-05-06", "Guest House", "Room I", "Monitor departure timing."}
    };
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
        setVisibleRecords(allRows());
        card.add(guestTable, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
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

        LocalDate today = LocalDate.now();
        LocalDate arrival = parseDate(record[ARRIVAL]);
        LocalDate departure = parseDate(record[DEPARTURE]);
        if (arrival == null || departure == null) {
            return false;
        }

        if (status.equalsIgnoreCase("Currently Staying")) {
            return !today.isBefore(arrival) && !today.isAfter(departure);
        }
        if (status.equalsIgnoreCase("Departed")) {
            return departure.isBefore(today);
        }
        if (status.equalsIgnoreCase("Upcoming")) {
            return arrival.isAfter(today);
        }
        return true;
    }

    private boolean dateMatches(Object[] record, String date) {
        return date.isEmpty()
                || String.valueOf(record[ARRIVAL]).equals(date)
                || String.valueOf(record[DEPARTURE]).equals(date);
    }

    private LocalDate parseDate(Object value) {
        try {
            return LocalDate.parse(String.valueOf(value));
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String dateTimeText(Object value) {
        return String.valueOf(value) + " 09:00";
    }

    private String statusText(Object[] record) {
        LocalDate today = LocalDate.now();
        LocalDate arrival = parseDate(record[ARRIVAL]);
        LocalDate departure = parseDate(record[DEPARTURE]);
        if (arrival == null || departure == null) {
            return "Unknown";
        }
        if (departure.isBefore(today)) {
            return "Departed";
        }
        if (arrival.isAfter(today)) {
            return "Upcoming";
        }
        return "Currently Staying";
    }

    private String tenureText(Object[] record) {
        LocalDate arrival = parseDate(record[ARRIVAL]);
        LocalDate departure = parseDate(record[DEPARTURE]);
        if (arrival == null || departure == null) {
            return "-";
        }
        long days = Math.max(1, ChronoUnit.DAYS.between(arrival, departure) + 1);
        return days + (days == 1 ? " day" : " days");
    }

    private List<Object[]> allRows() {
        List<Object[]> rows = new ArrayList<>();
        for (Object[] row : allData) {
            rows.add(row);
        }
        return rows;
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
}
