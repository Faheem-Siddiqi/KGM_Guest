package com.kgm.ui.panel;

import com.kgm.dao.AccommodationDao;
import com.kgm.dao.AccommodationDao.AccommodationKpiRecord;
import com.kgm.dao.AccommodationDao.AccommodationOccupancyFilter;
import com.kgm.model.Guest;
import com.kgm.ui.dialog.DelayedProgressDialog;
import com.kgm.ui.styling.AccommodationManagementHelper;
import com.kgm.ui.styling.DialogHelper;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class AccommodationListViewPanel extends JPanel {
    private final KPICategoryPanel.MetricSelection selection;
    private final AccommodationOccupancyFilter filter;
    private final Consumer<Object[]> onViewGuest;
    private final AccommodationDao accommodationDao = new AccommodationDao();
    private final List<AccommodationKpiRecord> records = new ArrayList<>();
    private final UniversalTablePanel tablePanel = new UniversalTablePanel(
            new String[]{
                    "Category", "Room", "Capacity", "Available", "Status",
                    "Guest Name", "CNIC", "Arrival", "Departure", "Company", "Visit Type", "Actions"
            },
            "No accommodation records match this KPI."
    );
    private JPanel card;
    private SwingWorker<List<AccommodationKpiRecord>, Void> loadWorker;

    public AccommodationListViewPanel(
            KPICategoryPanel.MetricSelection selection,
            AccommodationOccupancyFilter filter,
            Runnable onBack,
            Consumer<Object[]> onViewGuest
    ) {
        this.selection = selection;
        this.filter = filter == null ? AccommodationOccupancyFilter.ALL : filter;
        this.onViewGuest = onViewGuest;
        setLayout(new BorderLayout());
        setBackground(AccommodationManagementHelper.PAGE_BACKGROUND);
        configureTable();
        add(scrollPane(createPage(onBack)), BorderLayout.CENTER);
        loadAccommodations();
    }

    private JPanel createPage(Runnable onBack) {
        JPanel page = AccommodationManagementHelper.pagePanel();

        GridBagConstraints headerGbc = AccommodationManagementHelper.pageConstraints(0);
        page.add(AccommodationManagementHelper.screenHeader(
                selection.displayTitle(),
                "Accommodation list filtered from the dashboard KPI.",
                onBack
        ), headerGbc);

        card = AccommodationManagementHelper.sectionCard(
                "Accommodation List",
                "Rooms, capacity, availability, and current guest detail for the selected KPI."
        );
        card.add(loadingState(), BorderLayout.CENTER);

        GridBagConstraints tableGbc = AccommodationManagementHelper.pageConstraints(1);
        page.add(card, tableGbc);

        GridBagConstraints spacerGbc = AccommodationManagementHelper.pageConstraints(2);
        spacerGbc.weighty = 1.0;
        page.add(Box.createVerticalGlue(), spacerGbc);
        return page;
    }

    private JScrollPane scrollPane(JComponent page) {
        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        SwingUtilities.invokeLater(() -> {
            scroll.getVerticalScrollBar().setValue(0);
            scroll.getHorizontalScrollBar().setValue(0);
        });
        return scroll;
    }

    private void configureTable() {
        tablePanel.setLinkColumn(11, this::viewGuest);
        tablePanel.setHugColumn(1);
        tablePanel.setHugColumn(5);
        tablePanel.setHugColumn(11);
        tablePanel.setPreferredColumnWidthLimit(1, 160);
        tablePanel.setPreferredColumnWidthLimit(5, 180);
        tablePanel.setColumnAlignment(2, SwingConstants.CENTER);
        tablePanel.setColumnAlignment(3, SwingConstants.CENTER);
        tablePanel.setColumnAlignment(4, SwingConstants.CENTER);
        tablePanel.setColumnAlignment(6, SwingConstants.CENTER);
        tablePanel.setColumnAlignment(7, SwingConstants.CENTER);
        tablePanel.setColumnAlignment(8, SwingConstants.CENTER);
        tablePanel.setColumnAlignment(10, SwingConstants.CENTER);
    }

    private JComponent loadingState() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(34, 0, 34, 0));
        JLabel label = new JLabel("Loading KPI accommodation details...", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(AccommodationManagementHelper.TEXT_SECONDARY);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private void loadAccommodations() {
        if (loadWorker != null && !loadWorker.isDone()) {
            return;
        }

        DelayedProgressDialog.Handle progress = DelayedProgressDialog.showAfter(
                this,
                "Loading Accommodations",
                "Database is taking longer than usual. Loading KPI accommodations..."
        );
        loadWorker = new SwingWorker<>() {
            @Override
            protected List<AccommodationKpiRecord> doInBackground() throws Exception {
                return accommodationDao.findKpiDrilldownRows(selection.categoryName(), filter);
            }

            @Override
            protected void done() {
                try {
                    setAccommodations(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    DialogHelper.error(
                            AccommodationListViewPanel.this,
                            "Accommodations not loaded",
                            cause == null ? exception.getMessage() : cause.getMessage()
                    );
                    setAccommodations(new ArrayList<>());
                } finally {
                    progress.done();
                    loadWorker = null;
                }
            }
        };
        loadWorker.execute();
    }

    private void setAccommodations(List<AccommodationKpiRecord> accommodations) {
        records.clear();
        records.addAll(accommodations);
        List<Object[]> rows = new ArrayList<>();
        for (AccommodationKpiRecord accommodation : accommodations) {
            rows.add(rowValues(accommodation));
        }
        card.removeAll();
        card.add(tablePanel, BorderLayout.CENTER);
        tablePanel.setRows(rows);
        card.revalidate();
        card.repaint();
    }

    private Object[] rowValues(AccommodationKpiRecord accommodation) {
        Guest guest = accommodation.guest();
        Object[] guestRecord = guest == null ? null : GuestRecordPanel.recordFromGuest(guest);
        return new Object[]{
                accommodation.category(),
                accommodation.room(),
                accommodation.capacity(),
                accommodation.availableSeats(),
                accommodation.status(),
                guest == null ? "-" : emptyToDash(guest.getGuestName()),
                guest == null ? "-" : emptyToDash(guest.getCnic()),
                guestRecord == null ? "-" : guestRecord[GuestRecordPanel.ARRIVAL],
                guestRecord == null ? "-" : guestRecord[GuestRecordPanel.DEPARTURE],
                guest == null ? "-" : emptyToDash(guest.getCompanyName()),
                guest == null ? "-" : emptyToDash(guest.getVisitType()),
                guest == null ? "-" : "View Guest"
        };
    }

    private void viewGuest(int row) {
        if (row < 0 || row >= tablePanel.getRowCount()) {
            return;
        }
        Object actionValue = tablePanel.getValueAt(row, 11);
        if (!"View Guest".equals(String.valueOf(actionValue))) {
            DialogHelper.warning(this, "No guest to view", "This room does not currently have a guest for the selected KPI.");
            return;
        }
        if (row >= records.size() || records.get(row).guest() == null || onViewGuest == null) {
            return;
        }
        onViewGuest.accept(GuestRecordPanel.recordFromGuest(records.get(row).guest()));
    }

    private String emptyToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
