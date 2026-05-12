package com.kgm.ui.panel;

import com.kgm.ui.styling.RoomDetailHelper;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class RoomDetailPagePanel extends JPanel {
    private final AccommodationRecord accommodation;
    private final RoomDetailKpiPanel kpiPanel;
    private final RoomDetailGuestActivityPanel guestActivityPanel;

    public RoomDetailPagePanel(
            AccommodationRecord accommodation,
            Runnable onBack,
            Consumer<Object[]> onViewGuest
    ) {
        this.accommodation = accommodation;
        this.kpiPanel = new RoomDetailKpiPanel(accommodation.getId());
        this.guestActivityPanel = new RoomDetailGuestActivityPanel(
                accommodation.getId(),
                onViewGuest,
                this::refreshStats
        );

        setLayout(new BorderLayout());
        setOpaque(false);
        add(RoomDetailHelper.scrollPane(createPage(onBack)), BorderLayout.CENTER);
        refreshData();
    }

    public void refreshData() {
        refreshStats();
        guestActivityPanel.refreshGuestRecords();
    }

    private void refreshStats() {
        kpiPanel.refreshStatsAsync();
    }

    private JPanel createPage(Runnable onBack) {
        JPanel page = RoomDetailHelper.pagePanel();

        GridBagConstraints constraints = RoomDetailHelper.pageConstraints(0);
        page.add(RoomDetailHelper.header(
                accommodation.getName(),
                accommodation.getCategory(),
                onBack
        ), constraints);

        constraints = RoomDetailHelper.kpiConstraints(1);
        page.add(kpiPanel, constraints);

        constraints = RoomDetailHelper.pageConstraints(2);
        page.add(guestActivityPanel, constraints);

        constraints = RoomDetailHelper.pageConstraints(3);
        constraints.weighty = 1.0;
        page.add(Box.createVerticalGlue(), constraints);

        return page;
    }
}
