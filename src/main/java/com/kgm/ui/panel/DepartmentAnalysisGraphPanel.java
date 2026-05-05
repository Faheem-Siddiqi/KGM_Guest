package com.kgm.ui.panel;

import com.kgm.ui.styling.HomeViewHelper;

public class DepartmentAnalysisGraphPanel extends UniversalGraphPanel {
    public DepartmentAnalysisGraphPanel() {
        super(
                "Department Analysis",
                "Guest records grouped by department",
                new String[]{"IT", "HR", "Ops", "Sales", "Finance"},
                new UniversalGraphPanel.Series(
                        "Guests",
                        new int[]{12, 7, 10, 9, 6},
                        HomeViewHelper.VACANT_DARK,
                        HomeViewHelper.VACANT_LIGHT
                )
        );
    }
}
