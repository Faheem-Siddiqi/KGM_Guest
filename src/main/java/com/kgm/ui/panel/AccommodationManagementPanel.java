package com.kgm.ui.panel;

import com.kgm.ui.styling.AccommodationManagementHelper;

import javax.swing.*;
import java.awt.*;

public class AccommodationManagementPanel extends JPanel {
    private AccommodationFormPanel accommodationFormPanel;
    private AccommodationTablePanel accommodationTablePanel;

    public AccommodationManagementPanel(Runnable onBack) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel page = AccommodationManagementHelper.pagePanel();
        JPanel card = AccommodationManagementHelper.cardPanel();
        GridBagConstraints gbc = AccommodationManagementHelper.formConstraints();
        int y = 0;

        y = AccommodationManagementHelper.addFormHeader(card, gbc, y, onBack);

        accommodationTablePanel = new AccommodationTablePanel((row, accommodation) ->
                accommodationFormPanel.editAccommodation(row, accommodation)
        );
        accommodationFormPanel = new AccommodationFormPanel(
                accommodation -> accommodationTablePanel.addAccommodation(accommodation),
                (row, accommodation) -> accommodationTablePanel.updateAccommodation(row, accommodation)
        );
        AccommodationCategoryPanel categoryPanel = new AccommodationCategoryPanel(categories ->
                accommodationFormPanel.setCategories(categories)
        );

        y = AccommodationManagementHelper.addSectionTitle(card, gbc, y, "Accommodation Categories");
        gbc.gridx = 0;
        gbc.gridy = y++;
        gbc.gridwidth = 4;
        card.add(categoryPanel, gbc);

        y = AccommodationManagementHelper.addSectionTitle(card, gbc, y, "Accommodation Form");
        gbc.gridx = 0;
        gbc.gridy = y++;
        gbc.gridwidth = 4;
        card.add(accommodationFormPanel, gbc);

        y = AccommodationManagementHelper.addSectionTitle(card, gbc, y, "All Accommodations");
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        card.add(accommodationTablePanel, gbc);

        page.add(card);

        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> {
            scroll.getVerticalScrollBar().setValue(0);
            scroll.getHorizontalScrollBar().setValue(0);
        });
    }
}
