package com.kgm.ui.panel;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class AccommodationTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {
            "Category", "Name", "Capacity", "Available", "Status", "Assigned Staff", "Amenities", "Actions"
    };
    private final List<AccommodationRecord> accommodations = new ArrayList<>();

    public void addAccommodation(AccommodationRecord accommodation) {
        accommodations.add(accommodation);
        int row = accommodations.size() - 1;
        fireTableRowsInserted(row, row);
    }

    public void updateAccommodation(int row, AccommodationRecord accommodation) {
        accommodations.set(row, accommodation);
        fireTableRowsUpdated(row, row);
    }

    public AccommodationRecord getAccommodation(int row) {
        return accommodations.get(row);
    }

    public int getRowCount() {
        return accommodations.size();
    }

    public int getColumnCount() {
        return COLUMNS.length;
    }

    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        AccommodationRecord accommodation = accommodations.get(rowIndex);
        if (columnIndex == 0) {
            return accommodation.getCategory();
        }
        if (columnIndex == 1) {
            return accommodation.getName();
        }
        if (columnIndex == 2) {
            return accommodation.getCapacity();
        }
        if (columnIndex == 3) {
            return accommodation.getAvailableSeats();
        }
        if (columnIndex == 4) {
            return accommodation.getStatus();
        }
        if (columnIndex == 5) {
            return accommodation.getAssignedStaff();
        }
        if (columnIndex == 6) {
            return accommodation.getAmenities().size();
        }
        return "Edit";
    }
}
