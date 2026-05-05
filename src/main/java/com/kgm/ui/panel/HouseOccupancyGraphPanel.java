package com.kgm.ui.panel;

import com.kgm.ui.styling.HomeViewHelper;

import java.awt.*;

public class HouseOccupancyGraphPanel extends UniversalGraphPanel {
    public HouseOccupancyGraphPanel() {
        super(
                "House Occupancy",
                "Capacity compared with occupied seats",
                new String[]{"A", "B", "C", "D", "E", "F", "G", "H"},
                new UniversalGraphPanel.Series(
                        "Capacity",
                        new int[]{10, 14, 8, 16, 12, 9, 11, 13},
                        HomeViewHelper.PRIMARY_LIGHT,
                        HomeViewHelper.PRIMARY
                ),
                new UniversalGraphPanel.Series(
                        "Occupied",
                        new int[]{7, 10, 5, 13, 8, 6, 9, 10},
                        HomeViewHelper.PRIMARY,
                        HomeViewHelper.PRIMARY_DARK
                )
        );
    }
}
