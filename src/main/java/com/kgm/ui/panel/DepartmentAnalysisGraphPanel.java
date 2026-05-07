package com.kgm.ui.panel;

import com.kgm.dao.DashboardDao;
import com.kgm.ui.styling.HomeViewHelper;

public class DepartmentAnalysisGraphPanel extends UniversalGraphPanel {
    public DepartmentAnalysisGraphPanel(DashboardDao.DepartmentChartData data) {
        super(
                "Department Analysis",
                "Guest records grouped by department",
                data.labels(),
                new UniversalGraphPanel.Series(
                        "Guests",
                        data.guests(),
                        HomeViewHelper.GRAPH_PLUM_LIGHT,
                        HomeViewHelper.GRAPH_PLUM_DARK
                )
        );
    }
}
