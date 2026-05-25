package com.kgm.ui.panel;

import com.kgm.dao.DashboardDao;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class HomeKpiPanel extends JPanel {
    private static final int KPI_MIN_CARD_WIDTH = 176;
    private static final int KPI_CARD_HEIGHT = 94;
    private static final int FEATURED_KPI_CARD_HEIGHT = 100;
    private static final int KPI_GAP = 12;
    private static final int KPI_MAX_COLUMNS = 4;

    public HomeKpiPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
    }

    public HomeKpiPanel(DashboardDao.DashboardStats stats) {
        this();
        updateStats(stats);
    }

    public void updateStats(DashboardDao.DashboardStats stats) {
        updateStats(stats, true);
    }

    public void updateRoomStats(DashboardDao.DashboardStats stats) {
        updateStats(stats, false);
    }

    private void updateStats(DashboardDao.DashboardStats stats, boolean includeAverages) {
        removeAll();
        JPanel kpiGrid = responsiveKpiGrid();
        kpiGrid.setOpaque(false);

        kpiGrid.add(kpiCard(
                "Total Beds",
                String.valueOf(stats.totalSeats()),
                "Accommodation capacity",
                HomeViewHelper.BLUE,
                HomeViewHelper.BLUE,
                true
        ));
        kpiGrid.add(kpiCard(
                "Vacant Beds",
                String.valueOf(stats.vacantSeats()),
                "Ready for assignment",
                HomeViewHelper.TEAL,
                HomeViewHelper.TEAL,
                false
        ));
        kpiGrid.add(kpiCard(
                "Occupied Beds",
                String.valueOf(stats.occupiedSeats()),
                monthlyOccupancyDetail(stats.occupancyPercent()),
                HomeViewHelper.PURPLE,
                HomeViewHelper.PURPLE,
                false
        ));
        kpiGrid.add(kpiCard(
                "Upcoming Guests",
                String.valueOf(stats.upcomingGuests()),
                "Scheduled future arrivals",
                HomeViewHelper.BLUE,
                HomeViewHelper.BLUE,
                false
        ));
        if (includeAverages) {
            kpiGrid.add(kpiCard(
                    "Monthly Avg Stay",
                    stayDurationText(stats.averageStayHours()),
                    "Average stay this month",
                    HomeViewHelper.TEAL,
                    HomeViewHelper.TEAL,
                    false
            ));
            kpiGrid.add(kpiCard(
                    "Avg Arrival Time",
                    stats.averageArrivalTime(),
                    "Arrived guests only",
                    HomeViewHelper.PURPLE,
                    HomeViewHelper.PURPLE,
                    false
            ));
        }

        add(kpiGrid, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void showCategoryLoading() {
        showLoading("Loading accommodation KPIs...");
    }

    public void showLoading(String text) {
        removeAll();
        add(messagePanel(text), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void updateCategoryStats(List<DashboardDao.CategoryKpiStats> categoryStats) {
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
            KPICategoryPanel categoryPanel = new KPICategoryPanel(stat);
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

    private JPanel kpiCard(String title, String value, String detail, Color start, Color end, boolean featured) {
        JPanel card = HomeViewHelper.kpiCard(title, value, detail, start, end, featured);
        int height = featured ? FEATURED_KPI_CARD_HEIGHT : KPI_CARD_HEIGHT;
        Dimension compactSize = new Dimension(KPI_MIN_CARD_WIDTH, height);

        card.setPreferredSize(compactSize);
        card.setMinimumSize(new Dimension(148, height));

        return card;
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

    private JPanel responsiveKpiGrid() {
        return new JPanel(new GridLayout(0, 3, KPI_GAP, KPI_GAP)) {
            public void doLayout() {
                updateResponsiveColumns(this);
                super.doLayout();
            }

            public Dimension getPreferredSize() {
                updateResponsiveColumns(this);
                Container parent = getParent();
                int availableWidth = parent == null || parent.getWidth() <= 0
                        ? super.getPreferredSize().width
                        : parent.getWidth();
                if (availableWidth <= 0) {
                    return super.getPreferredSize();
                }

                GridLayout layout = (GridLayout) getLayout();
                int columns = Math.max(1, layout.getColumns());
                int rows = (int) Math.ceil(getComponentCount() / (double) columns);
                int height = rows * FEATURED_KPI_CARD_HEIGHT + Math.max(0, rows - 1) * KPI_GAP;

                return new Dimension(availableWidth, height);
            }
        };
    }

    private void updateResponsiveColumns(JPanel panel) {
        int width = panel.getWidth();
        if (width <= 0 && panel.getParent() != null) {
            width = panel.getParent().getWidth();
        }
        int columns = Math.max(1, Math.min(KPI_MAX_COLUMNS, width / (KPI_MIN_CARD_WIDTH + KPI_GAP)));
        columns = Math.min(Math.max(1, panel.getComponentCount()), columns);
        GridLayout layout = (GridLayout) panel.getLayout();
        if (layout.getColumns() != columns) {
            layout.setColumns(columns);
        }
    }
}
