package com.kgm.ui.panel;

import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;

public class HomeKpiPanel extends JPanel {
    public HomeKpiPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel kpiGrid = new JPanel(new GridLayout(2, 3, 16, 16));
        kpiGrid.setOpaque(false);

        kpiGrid.add(HomeViewHelper.kpiCard(
                "Total Seats",
                "120",
                "Accommodation capacity",
                HomeViewHelper.PRIMARY_DARK,
                HomeViewHelper.PRIMARY_LIGHT,
                true
        ));
        kpiGrid.add(HomeViewHelper.kpiCard(
                "Vacant Seats",
                "38",
                "Ready for assignment",
                HomeViewHelper.VACANT_DARK,
                HomeViewHelper.VACANT_LIGHT,
                false
        ));
        kpiGrid.add(HomeViewHelper.kpiCard(
                "Occupied Seats",
                "82",
                "Currently in use",
                HomeViewHelper.OCCUPIED_DARK,
                HomeViewHelper.OCCUPIED_LIGHT,
                false
        ));
        kpiGrid.add(HomeViewHelper.kpiCard(
                "Monthly Occupancy",
                "82%",
                "Across active accommodation",
                HomeViewHelper.KPI_AMBER_DARK,
                HomeViewHelper.KPI_AMBER_LIGHT,
                false
        ));
        kpiGrid.add(HomeViewHelper.kpiCard(
                "Avg Stay Duration",
                "2.4 hrs",
                "Guest stay average",
                HomeViewHelper.KPI_SKY_DARK,
                HomeViewHelper.KPI_SKY_LIGHT,
                false
        ));
        kpiGrid.add(HomeViewHelper.kpiCard(
                "Peak Arrival",
                "11:00 AM",
                "Highest check-in window",
                HomeViewHelper.KPI_ROSE_DARK,
                HomeViewHelper.KPI_ROSE_LIGHT,
                false
        ));

        add(kpiGrid, BorderLayout.CENTER);
    }
}
