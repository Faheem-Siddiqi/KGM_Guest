package com.kgm.ui.panel;

import com.kgm.dao.DashboardDao;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HomeKpiPanel extends JPanel {
    private static final int KPI_MIN_CARD_WIDTH = 176;
    private static final int KPI_CARD_HEIGHT = 94;
    private static final int FEATURED_KPI_CARD_HEIGHT = 100;
    private static final int KPI_GAP = 12;
    private static final int KPI_MAX_COLUMNS = 4;
    private static final String ALL_CATEGORIES = "All";
    private static final String SECURITY_CATEGORY = "Security Block";

    private final List<DashboardDao.CategoryKpiStats> visibleCategoryStats = new ArrayList<>();
    private String selectedCategory = ALL_CATEGORIES;

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
        visibleCategoryStats.clear();

        if (categoryStats == null || categoryStats.isEmpty()) {
            add(messagePanel("No active accommodation categories found."), BorderLayout.CENTER);
            revalidate();
            repaint();
            return;
        }

        for (DashboardDao.CategoryKpiStats stat : categoryStats) {
            if (stat != null && !isSecurityCategory(stat.categoryName())) {
                visibleCategoryStats.add(stat);
            }
        }

        if (visibleCategoryStats.isEmpty()) {
            selectedCategory = ALL_CATEGORIES;
            add(messagePanel("No active accommodation categories found."), BorderLayout.CENTER);
            revalidate();
            repaint();
            return;
        }

        if (!ALL_CATEGORIES.equals(selectedCategory) && selectedCategoryStats() == null) {
            selectedCategory = ALL_CATEGORIES;
        }

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setAlignmentX(Component.LEFT_ALIGNMENT);

        container.add(categoryTabsPanel());
        container.add(Box.createVerticalStrut(10));
        container.add(new KPICategoryPanel(selectedStats()));

        add(container, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JPanel categoryTabsPanel() {
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tabs.setOpaque(false);
        tabs.setAlignmentX(Component.LEFT_ALIGNMENT);

        ButtonGroup group = new ButtonGroup();
        JToggleButton allButton = categoryTab(ALL_CATEGORIES, ALL_CATEGORIES);
        group.add(allButton);
        tabs.add(allButton);

        for (DashboardDao.CategoryKpiStats stat : visibleCategoryStats) {
            JToggleButton tab = categoryTab(stat.categoryName(), stat.categoryName());
            group.add(tab);
            tabs.add(tab);
        }
        return tabs;
    }

    private JToggleButton categoryTab(String text, String value) {
        JToggleButton tab = new CategoryTabButton(text);
        tab.setSelected(valueMatchesSelected(value));
        styleCategoryTab(tab);
        tab.addActionListener(event -> {
            selectedCategory = value == null || value.isBlank() ? ALL_CATEGORIES : value.trim();
            updateCategoryStats(new ArrayList<>(visibleCategoryStats));
        });
        return tab;
    }

    private boolean valueMatchesSelected(String value) {
        String normalizedValue = value == null || value.isBlank() ? ALL_CATEGORIES : value.trim();
        return selectedCategory.equalsIgnoreCase(normalizedValue);
    }

    private void styleCategoryTab(JToggleButton tab) {
        tab.setForeground(tab.isSelected() ? HomeViewHelper.PRIMARY : HomeViewHelper.TEXT_SECONDARY);
        tab.repaint();
    }

    private DashboardDao.CategoryKpiStats selectedStats() {
        DashboardDao.CategoryKpiStats selected = selectedCategoryStats();
        return selected == null ? aggregateStats() : selected;
    }

    private DashboardDao.CategoryKpiStats selectedCategoryStats() {
        if (ALL_CATEGORIES.equalsIgnoreCase(selectedCategory)) {
            return null;
        }
        for (DashboardDao.CategoryKpiStats stat : visibleCategoryStats) {
            if (stat.categoryName() != null && stat.categoryName().trim().equalsIgnoreCase(selectedCategory)) {
                return stat;
            }
        }
        return null;
    }

    private DashboardDao.CategoryKpiStats aggregateStats() {
        int totalRooms = 0;
        int occupiedRooms = 0;
        int vacantRooms = 0;
        int fullyOccupiedRooms = 0;
        int partiallyOccupiedRooms = 0;
        int fullyVacantRooms = 0;
        int partiallyVacantRooms = 0;
        int totalBeds = 0;
        int occupiedBeds = 0;
        int vacantBeds = 0;

        for (DashboardDao.CategoryKpiStats stat : visibleCategoryStats) {
            totalRooms += stat.totalRooms();
            occupiedRooms += stat.occupiedRooms();
            vacantRooms += stat.vacantRooms();
            fullyOccupiedRooms += stat.fullyOccupiedRooms();
            partiallyOccupiedRooms += stat.partiallyOccupiedRooms();
            fullyVacantRooms += stat.fullyVacantRooms();
            partiallyVacantRooms += stat.partiallyVacantRooms();
            totalBeds += stat.totalBeds();
            occupiedBeds += stat.occupiedBeds();
            vacantBeds += stat.vacantBeds();
        }

        return new DashboardDao.CategoryKpiStats(
                "All Categories",
                totalRooms,
                occupiedRooms,
                vacantRooms,
                fullyOccupiedRooms,
                partiallyOccupiedRooms,
                fullyVacantRooms,
                partiallyVacantRooms,
                totalBeds,
                occupiedBeds,
                vacantBeds
        );
    }

    private boolean isSecurityCategory(String categoryName) {
        return categoryName != null && SECURITY_CATEGORY.equalsIgnoreCase(categoryName.trim());
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

    private static class CategoryTabButton extends JToggleButton {
        private CategoryTabButton(String text) {
            super(text == null || text.isBlank() ? ALL_CATEGORIES : text.trim());
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean selected = isSelected();
            g2.setColor(selected ? new Color(239, 246, 255) : Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.setColor(selected ? new Color(191, 219, 254) : new Color(226, 232, 240));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }
}
