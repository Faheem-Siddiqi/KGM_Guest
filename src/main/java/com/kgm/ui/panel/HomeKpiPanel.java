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
                monthlyOccupancyDetail(stats.occupancyPercent()),
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
                "Monthly Avg Stay",
                stayDurationText(stats.averageStayHours()),
                "Average stay this month",
                HomeViewHelper.KPI_SKY_DARK,
                HomeViewHelper.KPI_SKY_LIGHT,
                false
        ));
        kpiGrid.add(HomeViewHelper.kpiCard(
                "Monthly Peak Arrival Time",
                stats.peakArrival(),
                "Highest check-in window this month",
                HomeViewHelper.KPI_ROSE_DARK,
                HomeViewHelper.KPI_ROSE_LIGHT,
                false
        ));

        add(kpiGrid, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private String monthlyOccupancyDetail(int occupancyPercent) {
        return "This month occupancy rate: " + occupancyPercent + "%";
    }

    private String stayDurationText(double averageStayHours) {
        long totalHours = Math.max(0, Math.round(averageStayHours));
        long days = totalHours / 24;
        long hours = totalHours % 24;
        return days + " days " + hours + " hrs";
    }
}
