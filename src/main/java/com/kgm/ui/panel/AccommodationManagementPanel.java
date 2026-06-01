package com.kgm.ui.panel;

import com.kgm.dao.AccommodationDao;
import com.kgm.database.DatabaseInitializer;
import com.kgm.ui.dialog.DelayedProgressDialog;
import com.kgm.ui.styling.AccommodationManagementHelper;
import com.kgm.ui.styling.DialogHelper;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AccommodationManagementPanel extends JPanel {
    private static final String MANAGEMENT_SCREEN = "management";
    private static final String ROOM_DETAILS_SCREEN = "roomDetails";
    private static final String GUEST_DETAILS_SCREEN = "guestDetails";

    private final AccommodationDao accommodationDao = new AccommodationDao();
    private AccommodationFormPanel accommodationFormPanel;
    private AccommodationTablePanel accommodationTablePanel;
    private AccommodationCategoryPanel categoryPanel;
    private JScrollPane scroll;
    private AccommodationRecord selectedAccommodation;
    private RoomDetailPagePanel roomDetailsPanel;
    private Component guestDetailsScreen;
    private SwingWorker<List<AccommodationRecord>, Void> loadWorker;

    public AccommodationManagementPanel(Runnable onBack) {
        DatabaseInitializer.init();
        setLayout(new CardLayout());
        setBackground(AccommodationManagementHelper.PAGE_BACKGROUND);

        JPanel page = AccommodationManagementHelper.pagePanel();

        accommodationTablePanel = new AccommodationTablePanel(
                (row, accommodation) -> {
                    accommodationFormPanel.editAccommodation(row, accommodation);
                    scrollToSection(accommodationFormPanel);
                },
                (row, accommodation) -> showRoomDetails(accommodation)
        );
        accommodationFormPanel = new AccommodationFormPanel(
                this::saveAccommodation,
                this::updateAccommodation
        );
        categoryPanel = new AccommodationCategoryPanel(categories -> {
            accommodationFormPanel.setCategories(categories);
            accommodationTablePanel.setCategories(categories);
        });

        GridBagConstraints headerGbc = AccommodationManagementHelper.headerPageConstraints(0);
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
        add(scroll, MANAGEMENT_SCREEN);

        SwingUtilities.invokeLater(() -> {
            scroll.getVerticalScrollBar().setValue(0);
            scroll.getHorizontalScrollBar().setValue(0);
        });

        loadAccommodations();
    }

    private void showRoomDetails(AccommodationRecord accommodation) {
        selectedAccommodation = accommodation;
        if (roomDetailsPanel != null) {
            remove(roomDetailsPanel);
        }
        roomDetailsPanel = new RoomDetailPagePanel(
                accommodation,
                this::showManagementScreen,
                this::showGuestDetails
        );
        add(roomDetailsPanel, ROOM_DETAILS_SCREEN);
        showScreen(ROOM_DETAILS_SCREEN);
    }

    private void showGuestDetails(Object[] guestRecord) {
        if (guestDetailsScreen != null) {
            remove(guestDetailsScreen);
        }
        guestDetailsScreen = new GuestDetailsPanel(
                guestRecord,
                this::showSelectedRoomDetails,
                this::refreshSelectedRoomDetails
        );
        add(guestDetailsScreen, GUEST_DETAILS_SCREEN);
        showScreen(GUEST_DETAILS_SCREEN);
    }

    private void showSelectedRoomDetails() {
        showScreen(ROOM_DETAILS_SCREEN);
    }

    private void refreshSelectedRoomDetails() {
        if (roomDetailsPanel != null) {
            roomDetailsPanel.refreshData();
        } else if (selectedAccommodation != null) {
            showRoomDetails(selectedAccommodation);
        }
    }

    private void showManagementScreen() {
        showScreen(MANAGEMENT_SCREEN);
    }

    private void showScreen(String name) {
        CardLayout layout = (CardLayout) getLayout();
        layout.show(this, name);
        revalidate();
        repaint();
    }

    private void loadAccommodations() {
        if (loadWorker != null && !loadWorker.isDone()) {
            return;
        }

        DelayedProgressDialog.Handle progress = DelayedProgressDialog.showAfter(
                this,
                "Loading Accommodations",
                "Database is taking longer than usual. Loading accommodation records..."
        );
        loadWorker = new SwingWorker<>() {
            protected List<AccommodationRecord> doInBackground() throws Exception {
                return accommodationDao.findAll();
            }

            protected void done() {
                try {
                    accommodationTablePanel.setAccommodations(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    DialogHelper.error(
                            AccommodationManagementPanel.this,
                            "Accommodations not loaded",
                            cause == null ? exception.getMessage() : cause.getMessage()
                    );
                } finally {
                    progress.done();
                    loadWorker = null;
                }
            }
        };
        loadWorker.execute();
    }

    private boolean saveAccommodation(AccommodationRecord accommodation) {
        try {
            AccommodationRecord saved = accommodationDao.save(accommodation);
            accommodationTablePanel.addAccommodation(saved);
            DialogHelper.success(this, "Accommodation saved successfully.");
            return true;
        } catch (SQLException exception) {
            DialogHelper.error(this, "Accommodation not saved", exception.getMessage());
            return false;
        }
    }

    private boolean updateAccommodation(int row, AccommodationRecord accommodation) {
        try {
            accommodation.setId(accommodationTablePanel.getAccommodation(row).getId());
            AccommodationRecord updated = accommodationDao.update(accommodation);
            accommodationTablePanel.updateAccommodation(row, updated);
            DialogHelper.success(this, "Accommodation updated successfully.");
            return true;
        } catch (SQLException exception) {
            DialogHelper.error(this, "Accommodation not updated", exception.getMessage());
            return false;
        }
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
