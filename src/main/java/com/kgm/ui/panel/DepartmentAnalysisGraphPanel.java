package com.kgm.ui.panel;

import com.kgm.dao.DashboardDao;
import com.kgm.ui.styling.HomeViewHelper;

public class DepartmentAnalysisGraphPanel extends UniversalGraphPanel {
    public DepartmentAnalysisGraphPanel(DashboardDao.DepartmentChartData data) {
        super(
                "Top 5 Departments",
                "Guest records grouped by highest requesting departments",
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
