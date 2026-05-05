package com.kgm.ui.panel;

import com.kgm.ui.styling.AccommodationManagementHelper;

import javax.swing.*;
import java.awt.*;

public class AccommodationManagementPanel extends JPanel {
    private AccommodationFormPanel accommodationFormPanel;
    private AccommodationTablePanel accommodationTablePanel;

    public AccommodationManagementPanel(Runnable onBack) {
        setLayout(new BorderLayout());
        setBackground(AccommodationManagementHelper.PAGE_BACKGROUND);

        JPanel page = AccommodationManagementHelper.pagePanel();

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

        GridBagConstraints headerGbc = AccommodationManagementHelper.pageConstraints(0);
        page.add(AccommodationManagementHelper.screenHeader(onBack), headerGbc);

        GridBagConstraints categoryGbc = AccommodationManagementHelper.pageConstraints(1);
        page.add(categoryPanel, categoryGbc);

        GridBagConstraints formGbc = AccommodationManagementHelper.pageConstraints(2);
        page.add(accommodationFormPanel, formGbc);

        GridBagConstraints tableGbc = AccommodationManagementHelper.pageConstraints(3);
        page.add(accommodationTablePanel, tableGbc);

        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> {
            scroll.getVerticalScrollBar().setValue(0);
            scroll.getHorizontalScrollBar().setValue(0);
        });
    }
}
