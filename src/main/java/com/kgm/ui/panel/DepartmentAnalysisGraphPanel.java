package com.kgm.ui.panel;

import com.kgm.dao.DashboardDao;
import com.kgm.ui.styling.HomeViewHelper;

public class DepartmentAnalysisGraphPanel extends UniversalGraphPanel {
    private static final int TOP_DEPARTMENT_SLOTS = 5;

    public DepartmentAnalysisGraphPanel(DashboardDao.DepartmentChartData data) {
        super(
                "Top 5 Departments",
                "Guest records grouped by highest requesting departments",
                topFiveLabels(data.labels()),
                new UniversalGraphPanel.Series(
                        "Guests",
                        topFiveValues(data.guests()),
                        HomeViewHelper.KPI_ROSE_LIGHT,
                        HomeViewHelper.KPI_ROSE_DARK
                )
        );
    }

    private static String[] topFiveLabels(String[] labels) {
        String[] padded = new String[TOP_DEPARTMENT_SLOTS];
        for (int i = 0; i < TOP_DEPARTMENT_SLOTS; i++) {
            padded[i] = i < labels.length && labels[i] != null && !labels[i].isBlank()
                    ? labels[i]
                    : "Dept " + (i + 1);
        }
        return padded;
    }

    private static int[] topFiveValues(int[] values) {
        int[] padded = new int[TOP_DEPARTMENT_SLOTS];
        for (int i = 0; i < TOP_DEPARTMENT_SLOTS && i < values.length; i++) {
            padded[i] = values[i];
        }
        return padded;
    }
}
