package com.kgm.ui.panel;

import com.kgm.dao.DashboardDao;
import com.kgm.ui.styling.HomeViewHelper;

import java.util.ArrayList;
import java.util.List;

public class CompanyAnalysisGraphPanel extends UniversalGraphPanel {
    private final DashboardDao.BreakdownChartData data;

    public CompanyAnalysisGraphPanel(DashboardDao.BreakdownChartData data) {
        super(
                "Top 5 Companies Visit",
                subtitle(data),
                labels(data),
                new UniversalGraphPanel.Series(
                        "Guest Arrivals",
                        values(data),
                        HomeViewHelper.PURPLE,
                        HomeViewHelper.PURPLE_DARK
                )
        );
        this.data = safeData(data);
    }

    private static String subtitle(DashboardDao.BreakdownChartData data) {
        DashboardDao.BreakdownChartData safeData = safeData(data);
        if (safeData.labels().length == 0) {
            return "No saved company or organization names yet";
        }
        int shownGuests = sum(safeData.values());
        int allGuests = Math.max(shownGuests, safeData.totalGuestRequests());
        if (safeData.labels().length < 5) {
            return "Showing " + safeData.labels().length + " saved organization"
                    + plural(safeData.labels().length) + " from guest records";
        }
        return "Leading organizations account for " + shownGuests + " of " + allGuests + " guest records";
    }

    private static DashboardDao.BreakdownChartData safeData(DashboardDao.BreakdownChartData data) {
        return data == null
                ? new DashboardDao.BreakdownChartData(new String[0], new int[0])
                : data;
    }

    private static String[] labels(DashboardDao.BreakdownChartData data) {
        return safeData(data).labels();
    }

    private static int[] values(DashboardDao.BreakdownChartData data) {
        return safeData(data).values();
    }

    private static String plural(int count) {
        return count == 1 ? "" : "s";
    }

    @Override
    protected List<String> additionalStatLines(int categoryIndex) {
        List<String> lines = new ArrayList<>();
        if (categoryIndex < 0 || categoryIndex >= data.values().length) {
            return lines;
        }
        int guests = Math.max(0, data.values()[categoryIndex]);
        int total = Math.max(guests, data.totalGuestRequests());
        if (total > 0) {
            lines.add(String.format("Share of all guest records: %.1f%%", guests * 100.0 / total));
        }
        return lines;
    }

    private static int sum(int[] values) {
        int total = 0;
        for (int value : values) {
            total += Math.max(0, value);
        }
        return total;
    }
}
