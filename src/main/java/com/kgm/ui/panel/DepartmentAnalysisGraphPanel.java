package com.kgm.ui.panel;

import com.kgm.dao.DashboardDao;
import com.kgm.ui.styling.HomeViewHelper;

public class DepartmentAnalysisGraphPanel extends UniversalGraphPanel {

    public DepartmentAnalysisGraphPanel(DashboardDao.DepartmentChartData data) {
        super(
                "Total Departments",
                subtitle(data),
                data.labels(),
                new UniversalGraphPanel.Series(
                        "Guests",
                        data.guests(),
                        HomeViewHelper.KPI_ROSE_LIGHT,
                        HomeViewHelper.KPI_ROSE_DARK
                )
        );
    }

    private static String subtitle(DashboardDao.DepartmentChartData data) {
        if (data.labels().length == 0) {
            return "No guest records available";
        }
        int totalGuests = 0;
        for (int value : data.guests()) {
            totalGuests += Math.max(0, value);
        }
        return totalGuests + " guest records across " + data.labels().length + " requesting departments";
    }
}
