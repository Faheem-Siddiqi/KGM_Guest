package com.kgm.ui.panel;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class RoomDetailGuestActivityPanel extends JPanel {
    private final GuestFilterPanel guestFilterPanel;
    private final GuestRecordPanel guestRecordPanel;

    public RoomDetailGuestActivityPanel(
            long accommodationId,
            Consumer<Object[]> onViewGuest,
            Runnable onDataChanged
    ) {
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        guestFilterPanel = new GuestFilterPanel(this::performSearch, this::clearSearch);
        guestRecordPanel = new GuestRecordPanel(onViewGuest, null, accommodationId, onDataChanged);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 18, 0);

        constraints.gridy = 0;
        content.add(guestFilterPanel, constraints);

        constraints.gridy = 1;
        content.add(guestRecordPanel, constraints);

        add(content, BorderLayout.CENTER);
    }

    public void refreshGuestRecords() {
        guestRecordPanel.refreshFromDatabaseAsync(false);
    }

    private void performSearch() {
        guestRecordPanel.search(
                guestFilterPanel.getSearchText(),
                guestFilterPanel.getStatusText(),
                guestFilterPanel.getDateText()
        );
    }

    private void clearSearch() {
        guestFilterPanel.clearSearch();
        guestRecordPanel.reset();
    }
}
