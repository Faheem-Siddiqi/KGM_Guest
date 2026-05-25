package com.kgm.ui.panel;

import com.kgm.dao.AccommodationDao;
import com.kgm.dao.AccommodationDao.AccommodationOccupancyFilter;
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
    private final Consumer<AccommodationRecord> onViewAccommodation;
    private final AccommodationDao accommodationDao = new AccommodationDao();
    private final AccommodationTablePanel accommodationTablePanel;
    private JPanel content;
    private SwingWorker<List<AccommodationRecord>, Void> loadWorker;

    public AccommodationListViewPanel(
            KPICategoryPanel.MetricSelection selection,
            Runnable onBack,
            Consumer<AccommodationRecord> onViewAccommodation
    ) {
        this.selection = selection;
        this.onViewAccommodation = onViewAccommodation;
        this.accommodationTablePanel = new AccommodationTablePanel(
                (row, accommodation) -> {
                },
                (row, accommodation) -> {
                    if (this.onViewAccommodation != null) {
                        this.onViewAccommodation.accept(accommodation);
                    }
                },
                false,
                false,
                "Accommodation List",
                "Click a room name to open its room details.",
                "No rooms match this KPI."
        );
        setLayout(new BorderLayout());
        setBackground(AccommodationManagementHelper.PAGE_BACKGROUND);
        add(scrollPane(createPage(onBack)), BorderLayout.CENTER);
        loadAccommodations();
    }

    public void refreshData() {
        loadAccommodations();
    }

    private JPanel createPage(Runnable onBack) {
        JPanel page = AccommodationManagementHelper.pagePanel();

        GridBagConstraints headerGbc = AccommodationManagementHelper.pageConstraints(0);
        page.add(AccommodationManagementHelper.screenHeader(
                selectionTitle(),
                selectionSubtitle(),
                onBack
        ), headerGbc);

        content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(loadingState(), BorderLayout.CENTER);

        GridBagConstraints tableGbc = AccommodationManagementHelper.pageConstraints(1);
        page.add(content, tableGbc);

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

    private JComponent loadingState() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(34, 0, 34, 0));
        JLabel label = new JLabel("Loading accommodations for " + categoryName() + "...", SwingConstants.CENTER);
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
            protected List<AccommodationRecord> doInBackground() throws Exception {
                return accommodationDao.getAccommodationsByCategory(categoryName(), selectedFilter());
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

    private void setAccommodations(List<AccommodationRecord> accommodations) {
        content.removeAll();
        content.add(accommodationTablePanel, BorderLayout.CENTER);
        accommodationTablePanel.setAccommodations(accommodations);
        content.revalidate();
        content.repaint();
    }

    private String categoryName() {
        return selection == null || selection.categoryName() == null || selection.categoryName().isBlank()
                ? "Selected Category"
                : selection.categoryName().trim();
    }

    private String selectionTitle() {
        return selection == null ? categoryName() + " Accommodations" : selection.displayTitle();
    }

    private String selectionSubtitle() {
        return switch (selectedFilter()) {
            case ALL -> "Showing every room in this category.";
            case OCCUPIED_ROOMS, OCCUPIED_BEDS -> "Showing rooms that are fully or partially occupied.";
            case VACANT_ROOMS -> "Showing rooms with no current guests.";
            case VACANT_BEDS -> "Showing rooms with available bed capacity.";
        };
    }

    private AccommodationOccupancyFilter selectedFilter() {
        if (selection == null || selection.type() == KPICategoryPanel.MetricType.TOTAL) {
            return AccommodationOccupancyFilter.ALL;
        }
        if (selection.group() == KPICategoryPanel.MetricGroup.ROOMS) {
            return selection.type() == KPICategoryPanel.MetricType.OCCUPIED
                    ? AccommodationOccupancyFilter.OCCUPIED_ROOMS
                    : AccommodationOccupancyFilter.VACANT_ROOMS;
        }
        return selection.type() == KPICategoryPanel.MetricType.OCCUPIED
                ? AccommodationOccupancyFilter.OCCUPIED_BEDS
                : AccommodationOccupancyFilter.VACANT_BEDS;
    }
}
