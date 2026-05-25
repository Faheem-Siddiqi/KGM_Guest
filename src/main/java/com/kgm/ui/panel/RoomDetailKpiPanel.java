package com.kgm.ui.panel;

import com.kgm.dao.DashboardDao;
import com.kgm.ui.dialog.DelayedProgressDialog;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;

public class RoomDetailKpiPanel extends JPanel {
    private final long accommodationId;
    private final DashboardDao dashboardDao = new DashboardDao();
    private final HomeKpiPanel kpiPanel = new HomeKpiPanel();
    private SwingWorker<DashboardDao.DashboardStats, Void> statsWorker;

    public RoomDetailKpiPanel(long accommodationId) {
        this.accommodationId = accommodationId;
        setLayout(new BorderLayout());
        setOpaque(false);
        kpiPanel.showLoading("Loading room KPIs...");
        add(kpiPanel, BorderLayout.CENTER);
    }

    public void refreshStatsAsync() {
        if (statsWorker != null && !statsWorker.isDone()) {
            return;
        }

        DelayedProgressDialog.Handle progress = DelayedProgressDialog.showAfter(
                this,
                "Loading Room Details",
                "Database is taking longer than usual. Loading room dashboard..."
        );
        statsWorker = new SwingWorker<>() {
            protected DashboardDao.DashboardStats doInBackground() throws Exception {
                return dashboardDao.loadStatsForAccommodation(accommodationId);
            }

            protected void done() {
                try {
                    kpiPanel.updateRoomStats(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    DialogHelper.error(
                            RoomDetailKpiPanel.this,
                            "Room details not loaded",
                            cause == null ? exception.getMessage() : cause.getMessage()
                    );
                } finally {
                    progress.done();
                    statsWorker = null;
                }
            }
        };
        statsWorker.execute();
    }
}
