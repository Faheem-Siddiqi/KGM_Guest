package com.kgm.ui.panel;

import com.kgm.dao.DashboardDao;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HouseOccupancyGraphPanel extends UniversalGraphPanel {
    private static final String ALL_CATEGORIES = "All Categories";
    private static final int HEADER_LEFT = 22;
    private static final int CATEGORY_TABS_Y = 58;
    private static final int CATEGORY_TABS_BOTTOM_MARGIN = 12;
    private final DashboardDao dashboardDao;
    private final CategoryTabsPanel categoryTabs = new CategoryTabsPanel();
    private String selectedCategory = "";

    public HouseOccupancyGraphPanel(
            DashboardDao dashboardDao,
            DashboardDao.OccupancyChartData data,
            String[] categories
    ) {
        super(
                "House Capacity",
                "Capacity compared with occupied seats",
                data.labels(),
                new UniversalGraphPanel.Series(
                        "Capacity",
                        data.capacity(),
                        HomeViewHelper.PRIMARY_LIGHT,
                        HomeViewHelper.PRIMARY
                ),
                new UniversalGraphPanel.Series(
                        "Occupied",
                        data.occupied(),
                        HomeViewHelper.PRIMARY,
                        HomeViewHelper.PRIMARY_DARK
                )
        );
        this.dashboardDao = dashboardDao;
        setLayout(null);
        configureCategoryTabs(categories);
    }

    public void doLayout() {
        super.doLayout();
        layoutCategoryTabs();
    }

    protected void paintComponent(Graphics graphics) {
        layoutCategoryTabs();
        super.paintComponent(graphics);
    }

    protected int plotTopInset() {
        int tabsBottom = categoryTabs.getY() + categoryTabs.getHeight();
        return tabsBottom > 0 ? tabsBottom + CATEGORY_TABS_BOTTOM_MARGIN : 90;
    }

    private void layoutCategoryTabs() {
        Rectangle visible = getVisibleRect();
        int width = visible.width > 0 ? visible.width - (HEADER_LEFT * 2) : getWidth() - (HEADER_LEFT * 2);
        int x = visible.width > 0 ? visible.x + HEADER_LEFT : HEADER_LEFT;
        int tabWidth = Math.max(160, width);
        int tabHeight = categoryTabs.preferredHeight(tabWidth);
        categoryTabs.setBounds(x, CATEGORY_TABS_Y, tabWidth, tabHeight);
        categoryTabs.doLayout();
    }

    private void configureCategoryTabs(String[] categories) {
        boolean hasCategory = false;
        for (String category : categories) {
            if (category != null && !category.trim().isEmpty()) {
                String categoryName = category.trim();
                if (!hasCategory) {
                    selectedCategory = categoryName;
                }
                categoryTabs.addTab(categoryName, categoryName, this::selectCategory);
                hasCategory = true;
            }
        }
        categoryTabs.addTab(ALL_CATEGORIES, "", this::selectCategory);
        if (!hasCategory) {
            selectedCategory = "";
        }
        categoryTabs.selectValue(selectedCategory);
        add(categoryTabs);
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        categoryTabs.selectValue(category);
        loadSelectedCategory();
    }

    private void loadSelectedCategory() {
        try {
            DashboardDao.OccupancyChartData data = dashboardDao.loadOccupancyChart(selectedCategory);
            setGraphData(
                    data.labels(),
                    new UniversalGraphPanel.Series(
                            "Capacity",
                            data.capacity(),
                            HomeViewHelper.PRIMARY_LIGHT,
                            HomeViewHelper.PRIMARY
                    ),
                    new UniversalGraphPanel.Series(
                            "Occupied",
                            data.occupied(),
                            HomeViewHelper.PRIMARY,
                            HomeViewHelper.PRIMARY_DARK
                    )
            );
        } catch (SQLException exception) {
            setGraphData(
                    new String[0],
                    new UniversalGraphPanel.Series(
                            "Capacity",
                            new int[0],
                            HomeViewHelper.PRIMARY_LIGHT,
                            HomeViewHelper.PRIMARY
                    ),
                    new UniversalGraphPanel.Series(
                            "Occupied",
                            new int[0],
                            HomeViewHelper.PRIMARY,
                            HomeViewHelper.PRIMARY_DARK
                    )
            );
        }
    }

    private static class CategoryTabsPanel extends JPanel {
        private static final int HORIZONTAL_GAP = 14;
        private static final int VERTICAL_GAP = 4;
        private static final int ROW_HEIGHT = 28;
        private final List<CategoryTabButton> buttons = new ArrayList<>();
        private int rowCount = 1;

        private CategoryTabsPanel() {
            setOpaque(false);
            setLayout(null);
        }

        private void addTab(String text, String value, Consumer<String> onSelect) {
            CategoryTabButton button = new CategoryTabButton(text, value);
            button.addActionListener(event -> onSelect.accept(value));
            buttons.add(button);
            add(button);
        }

        private void selectValue(String value) {
            for (CategoryTabButton button : buttons) {
                button.setActive(button.hasValue(value));
            }
        }

        private int getRowCount() {
            return rowCount;
        }

        private int preferredHeight(int width) {
            return calculateLayout(width, false);
        }

        public void doLayout() {
            calculateLayout(getWidth(), true);
        }

        private int calculateLayout(int width, boolean applyBounds) {
            int maxWidth = Math.max(1, width);
            int x = 0;
            int y = 0;
            int rows = 1;
            for (CategoryTabButton button : buttons) {
                Dimension size = button.getPreferredSize();
                int buttonWidth = Math.min(size.width, maxWidth);
                if (x > 0 && x + buttonWidth > maxWidth) {
                    x = 0;
                    y += ROW_HEIGHT + VERTICAL_GAP;
                    rows++;
                }
                if (applyBounds) {
                    button.setBounds(x, y, buttonWidth, ROW_HEIGHT);
                }
                x += buttonWidth + HORIZONTAL_GAP;
            }
            rowCount = rows;
            return rows * ROW_HEIGHT + Math.max(0, rows - 1) * VERTICAL_GAP;
        }
    }

    private static class CategoryTabButton extends JButton {
        private final String value;
        private boolean active;

        private CategoryTabButton(String text, String value) {
            super(text);
            this.value = value;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
            setForeground(HomeViewHelper.TEXT_SECONDARY);
            setBorder(BorderFactory.createEmptyBorder(4, 0, 7, 8));
        }

        private boolean hasValue(String otherValue) {
            return value.equals(otherValue);
        }

        private void setActive(boolean active) {
            this.active = active;
            setForeground(active ? HomeViewHelper.PRIMARY : HomeViewHelper.TEXT_SECONDARY);
            repaint();
        }

        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!active) {
                return;
            }
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FontMetrics metrics = g2.getFontMetrics(getFont());
            int underlineWidth = Math.max(22, Math.min(getWidth() - 12, metrics.stringWidth(getText()) + 4));
            int underlineX = (getWidth() - underlineWidth) / 2;
            g2.setColor(HomeViewHelper.PRIMARY);
            g2.fillRoundRect(underlineX, getHeight() - 4, underlineWidth, 3, 3, 3);
            g2.dispose();
        }
    }
}
