package com.kgm.ui.panel;

import com.kgm.dao.DashboardDao;
import com.kgm.ui.styling.HomeViewHelper;

import java.util.ArrayList;
import java.util.List;

public class DepartmentAnalysisGraphPanel extends UniversalGraphPanel {
    private final DashboardDao.DepartmentChartData data;

    public DepartmentAnalysisGraphPanel(DashboardDao.DepartmentChartData data) {
        super(
                "Top 5 Departments Requesting Guests",
                subtitle(data),
                data.labels(),
                new UniversalGraphPanel.Series(
                        "Guests",
                        data.guests(),
                        HomeViewHelper.KPI_ROSE_LIGHT,
                        HomeViewHelper.KPI_ROSE_DARK
                )
        );
        this.data = data;
    }

    private static String subtitle(DashboardDao.DepartmentChartData data) {
        if (data.labels().length == 0) {
            return "No guest records available";
        }
        int totalGuests = 0;
        for (int value : data.guests()) {
            totalGuests += Math.max(0, value);
        }
        int allRequests = Math.max(totalGuests, data.totalGuestRequests());
        return totalGuests + " of " + allRequests + " guest requests from DB and legacy records";
    }

    @Override
    protected List<String> additionalStatLines(int categoryIndex) {
        List<String> lines = new ArrayList<>();
        if (categoryIndex < 0 || categoryIndex >= data.guests().length) {
            return lines;
        }
        int guestCount = Math.max(0, data.guests()[categoryIndex]);
        int total = Math.max(guestCount, data.totalGuestRequests());
        if (total > 0) {
            double share = guestCount * 100.0 / total;
            lines.add(String.format("Share of all requests: %.1f%%", share));
        }
        return lines;
    }
}
