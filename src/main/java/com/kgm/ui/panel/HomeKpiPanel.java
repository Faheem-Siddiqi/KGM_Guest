package com.kgm.ui.panel;

import com.kgm.dao.DashboardDao;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class HomeKpiPanel extends JPanel {
    private static final int KPI_MIN_WIDTH = 220;

    public HomeKpiPanel(DashboardDao.DashboardStats stats) {
        setLayout(new BorderLayout());
        setOpaque(false);
        updateStats(stats);
    }

    public void updateStats(DashboardDao.DashboardStats stats) {
        removeAll();
        JPanel kpiGrid = responsiveGrid(6, KPI_MIN_WIDTH, 16, 16);
        kpiGrid.setOpaque(false);

        kpiGrid.add(HomeViewHelper.kpiCard(
                "Total Beds",
                String.valueOf(stats.totalSeats()),
                "Accommodation capacity",
                HomeViewHelper.PRIMARY_DARK,
                HomeViewHelper.PRIMARY_LIGHT,
                true
        ));
        kpiGrid.add(HomeViewHelper.kpiCard(
                "Vacant Beds",
                String.valueOf(stats.vacantSeats()),
                "Ready for assignment",
                HomeViewHelper.VACANT_DARK,
                HomeViewHelper.VACANT_LIGHT,
                false
        ));
        kpiGrid.add(HomeViewHelper.kpiCard(
                "Occupied Beds",
                String.valueOf(stats.occupiedSeats()),
                monthlyOccupancyDetail(stats.occupancyPercent()),
                HomeViewHelper.OCCUPIED_DARK,
                HomeViewHelper.OCCUPIED_LIGHT,
                false
        ));
        kpiGrid.add(HomeViewHelper.kpiCard(
                "Upcoming Guests",
                String.valueOf(stats.upcomingGuests()),
                "Scheduled future arrivals",
                HomeViewHelper.KPI_AMBER_DARK,
                HomeViewHelper.KPI_AMBER_LIGHT,
                false
        ));
        kpiGrid.add(HomeViewHelper.kpiCard(
                "Monthly Avg Stay",
                stayDurationText(stats.averageStayHours()),
                "Average stay this month",
                HomeViewHelper.KPI_SKY_DARK,
                HomeViewHelper.KPI_SKY_LIGHT,
                false
        ));
        kpiGrid.add(HomeViewHelper.kpiCard(
                "Avg Arrival Time",
                stats.averageArrivalTime(),
                "Arrived guests only",
                HomeViewHelper.KPI_ROSE_DARK,
                HomeViewHelper.KPI_ROSE_LIGHT,
                false
        ));

        add(kpiGrid, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void showCategoryLoading() {
        removeAll();
        add(messagePanel("Loading accommodation KPIs..."), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void updateCategoryStats(
            List<DashboardDao.CategoryKpiStats> categoryStats,
            Consumer<KPICategoryPanel.MetricSelection> onMetricClicked
    ) {
        removeAll();

        if (categoryStats == null || categoryStats.isEmpty()) {
            add(messagePanel("No active accommodation categories found."), BorderLayout.CENTER);
            revalidate();
            repaint();
            return;
        }

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (DashboardDao.CategoryKpiStats stat : categoryStats) {
            KPICategoryPanel categoryPanel = new KPICategoryPanel(stat, onMetricClicked);
            container.add(categoryPanel);
        }

        add(container, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JComponent messagePanel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(HomeViewHelper.TEXT_SECONDARY);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 0, 18, 0));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private String monthlyOccupancyDetail(int occupancyPercent) {
        return "This month occupancy rate: " + occupancyPercent + "%";
    }

    private String stayDurationText(double averageStayHours) {
        long totalHours = Math.max(0, Math.round(averageStayHours));
        long days = totalHours / 24;
        long hours = totalHours % 24;
        return days + " days " + hours + " hrs";
    }

    private JPanel responsiveGrid(int itemCount, int minItemWidth, int horizontalGap, int verticalGap) {
        return new JPanel(new GridLayout(0, 3, horizontalGap, verticalGap)) {
            public void doLayout() {
                updateResponsiveColumns(this, itemCount, minItemWidth);
                super.doLayout();
            }

            public Dimension getPreferredSize() {
                updateResponsiveColumns(this, itemCount, minItemWidth);
                return super.getPreferredSize();
            }
        };
    }

    private void updateResponsiveColumns(JPanel panel, int itemCount, int minItemWidth) {
        int width = panel.getWidth();
        if (width <= 0 && panel.getParent() != null) {
            width = panel.getParent().getWidth();
        }
        int columns = Math.max(1, Math.min(Math.max(1, itemCount), Math.max(1, width / minItemWidth)));
        GridLayout layout = (GridLayout) panel.getLayout();
        if (layout.getColumns() != columns) {
            layout.setColumns(columns);
        }
    }
}
