package com.kgm.ui.panel;

import com.kgm.ui.styling.AccommodationManagementHelper;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class AccommodationTablePanel extends JPanel {
    private final List<AccommodationRecord> records = new ArrayList<>();
    private final UniversalTablePanel tablePanel = new UniversalTablePanel(
            new String[]{"Category", "Name", "Capacity", "Available", "Status", "Assigned Staff", "Amenities", "Actions"},
            "No accommodation records yet. Save the form above to create one."
    );
    private final BiConsumer<Integer, AccommodationRecord> onEdit;

    public AccommodationTablePanel(BiConsumer<Integer, AccommodationRecord> onEdit) {
        this.onEdit = onEdit;
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel card = AccommodationManagementHelper.sectionCard(
                "Accommodation List",
                "All created accommodations appear here with quick edit access."
        );

        tablePanel.setActionColumn(7, "Edit", row -> this.onEdit.accept(row, records.get(row)));

        card.add(tablePanel, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
    }

    public void addAccommodation(AccommodationRecord accommodation) {
        records.add(accommodation);
        tablePanel.addRow(rowValues(accommodation));
    }

    public void setAccommodations(List<AccommodationRecord> accommodations) {
        records.clear();
        records.addAll(accommodations);
        List<Object[]> rows = new ArrayList<>();
        for (AccommodationRecord accommodation : records) {
            rows.add(rowValues(accommodation));
        }
        tablePanel.setRows(rows);
    }

    public AccommodationRecord getAccommodation(int row) {
        return records.get(row);
    }

    public void updateAccommodation(int row, AccommodationRecord accommodation) {
        records.set(row, accommodation);
        tablePanel.updateRow(row, rowValues(accommodation));
    }

    private Object[] rowValues(AccommodationRecord accommodation) {
        return new Object[]{
                accommodation.getCategory(),
                accommodation.getName(),
                accommodation.getCapacity(),
                accommodation.getAvailableSeats(),
                accommodation.getStatus(),
                accommodation.getAssignedStaff(),
                accommodation.getAmenities().size(),
                "Edit"
        };
    }
}
