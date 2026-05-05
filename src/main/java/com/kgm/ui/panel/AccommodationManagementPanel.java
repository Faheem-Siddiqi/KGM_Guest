package com.kgm.ui.panel;

import com.kgm.ui.styling.AccommodationManagementHelper;

import javax.swing.*;
import java.awt.*;

public class AccommodationManagementPanel extends JPanel {
    private AccommodationFormPanel accommodationFormPanel;
    private AccommodationTablePanel accommodationTablePanel;
    private AccommodationCategoryPanel categoryPanel;
    private JScrollPane scroll;

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
        categoryPanel = new AccommodationCategoryPanel(categories ->
                accommodationFormPanel.setCategories(categories)
        );

        GridBagConstraints headerGbc = AccommodationManagementHelper.pageConstraints(0);
        page.add(AccommodationManagementHelper.screenHeader(onBack), headerGbc);

        GridBagConstraints breadcrumbGbc = AccommodationManagementHelper.pageConstraints(1);
        page.add(AccommodationManagementHelper.breadcrumb(
                new String[]{"Accommodation Categories", "Accommodation Form", "Accommodation List"},
                new Runnable[]{
                        () -> scrollToSection(categoryPanel),
                        () -> scrollToSection(accommodationFormPanel),
                        () -> scrollToSection(accommodationTablePanel)
                }
        ), breadcrumbGbc);

        GridBagConstraints categoryGbc = AccommodationManagementHelper.pageConstraints(2);
        page.add(categoryPanel, categoryGbc);

        GridBagConstraints formGbc = AccommodationManagementHelper.pageConstraints(3);
        page.add(accommodationFormPanel, formGbc);

        GridBagConstraints tableGbc = AccommodationManagementHelper.pageConstraints(4);
        page.add(accommodationTablePanel, tableGbc);

        GridBagConstraints returnTopGbc = AccommodationManagementHelper.pageConstraints(5);
        page.add(AccommodationManagementHelper.returnToTop(this::scrollToTop), returnTopGbc);

        scroll = new JScrollPane(page);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> {
            scroll.getVerticalScrollBar().setValue(0);
            scroll.getHorizontalScrollBar().setValue(0);
        });
    }

    private void scrollToSection(JComponent section) {
        if (section == null || scroll == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            Rectangle bounds = section.getBounds();
            bounds.y = Math.max(0, bounds.y - 12);
            bounds.height = Math.min(section.getHeight() + 24, scroll.getViewport().getHeight());
            if (section.getParent() instanceof JComponent) {
                ((JComponent) section.getParent()).scrollRectToVisible(bounds);
            }
        });
    }

    private void scrollToTop() {
        if (scroll == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            scroll.getVerticalScrollBar().setValue(0);
            scroll.getHorizontalScrollBar().setValue(0);
        });
    }
}
