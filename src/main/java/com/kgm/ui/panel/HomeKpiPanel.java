package com.kgm.ui.panel;

import com.kgm.dao.DashboardDao;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;

public class HomeKpiPanel extends JPanel {
    public HomeKpiPanel(DashboardDao.DashboardStats stats) {
        setLayout(new BorderLayout());
        setOpaque(false);
        updateStats(stats);
    }

    public void updateStats(DashboardDao.DashboardStats stats) {
        removeAll();
        JPanel kpiGrid = new JPanel(new GridLayout(2, 3, 16, 16));
        kpiGrid.setOpaque(false);

        kpiGrid.add(HomeViewHelper.kpiCard(
                "Total Beds",
                String.valueOf(stats.totalSeats()),
                "Accommodation capacity",
                HomeViewHelper.PRIMARY_DARK,
                HomeViewHelper.PRIMARY_LIGHT,
                true
        ));
        kpiGrid.add(HomeViewHelper.kpiCard(
                "Vacant Beds",
                String.valueOf(stats.vacantSeats()),
                "Ready for assignment",
                HomeViewHelper.VACANT_DARK,
                HomeViewHelper.VACANT_LIGHT,
                false
        ));
        kpiGrid.add(HomeViewHelper.kpiCard(
                "Occupied Beds",
                String.valueOf(stats.occupiedSeats()),
                buildOccupiedDetail(stats.occupancyPercent()),
                HomeViewHelper.OCCUPIED_DARK,
                HomeViewHelper.OCCUPIED_LIGHT,
                false
        ));
        kpiGrid.add(HomeViewHelper.kpiCard(
                "Upcoming Guests",
                String.valueOf(stats.upcomingGuests()),
                "Scheduled future arrivals",
                HomeViewHelper.KPI_AMBER_DARK,
                HomeViewHelper.KPI_AMBER_LIGHT,
                false
        ));
        kpiGrid.add(HomeViewHelper.kpiCard(
                "Avg Stay Duration",
                String.format("%.1f hrs", stats.averageStayHours()),
                "Guest stay average",
                HomeViewHelper.KPI_SKY_DARK,
                HomeViewHelper.KPI_SKY_LIGHT,
                false
        ));
        kpiGrid.add(HomeViewHelper.kpiCard(
                "Peak Arrival",
                stats.peakArrival(),
                "Highest check-in window",
                HomeViewHelper.KPI_ROSE_DARK,
                HomeViewHelper.KPI_ROSE_LIGHT,
                false
        ));

        add(kpiGrid, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private String buildOccupiedDetail(int occupancyPercent) {
        return "Current occupancy: " + occupancyPercent + "%";
    }
}
