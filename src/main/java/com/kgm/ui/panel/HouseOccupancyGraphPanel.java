package com.kgm.ui.panel;

import com.kgm.dao.DashboardDao;
import com.kgm.ui.styling.HomeViewHelper;

import java.awt.*;

public class HouseOccupancyGraphPanel extends UniversalGraphPanel {
    public HouseOccupancyGraphPanel(DashboardDao.OccupancyChartData data) {
        super(
                "House Occupancy",
                "Capacity compared with occupied seats",
                data.labels(),
                new UniversalGraphPanel.Series(
                        "Capacity",
                        data.capacity(),
                        HomeViewHelper.PRIMARY_LIGHT,
                        HomeViewHelper.PRIMARY
                ),
                new UniversalGraphPanel.Series(
                        "Occupied",
                        data.occupied(),
                        HomeViewHelper.PRIMARY,
                        HomeViewHelper.PRIMARY_DARK
                )
        );
    }
}
