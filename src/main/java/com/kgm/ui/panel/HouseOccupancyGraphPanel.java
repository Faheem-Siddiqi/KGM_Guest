package com.kgm.ui.panel;

import com.kgm.dao.DashboardDao;
import com.kgm.ui.dialog.DelayedProgressDialog;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class HouseOccupancyGraphPanel extends UniversalGraphPanel {
    private static final String ALL_CATEGORIES = "All Categories";

    /*
     * UX SPACING CONTROLS
     *
     * HEADER_LEFT:
     * Controls left/right horizontal margin of the tabs row.
     *
     * CATEGORY_TABS_Y:
     * Controls vertical position of tabs row.
     * Decrease value = tabs move up.
     * Increase value = tabs move down.
     *
     * CATEGORY_TABS_BOTTOM_MARGIN:
     * Controls spacing between tabs row and graph/chart area.
     * Increase value = more space after tabs before graph starts.
     * Decrease value = graph starts closer to tabs.
     */
    private static final int HEADER_LEFT = 26;
    private static final int CATEGORY_TABS_Y = 66;
    private static final int CATEGORY_TABS_BOTTOM_MARGIN = 32;

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
                "Capacity compared with occupied beds",
                data.labels(),
                capacitySeries(data.capacity()),
                occupiedSeries(data.occupied())
        );

        this.dashboardDao = dashboardDao;

        setLayout(null);
        configureCategoryTabs(categories);
    }

    private static UniversalGraphPanel.Series capacitySeries(int[] values) {
        return new UniversalGraphPanel.Series(
                "Capacity",
                values,
                HomeViewHelper.BLUE,
                HomeViewHelper.BLUE_DARK
        );
    }

    private static UniversalGraphPanel.Series occupiedSeries(int[] values) {
        return new UniversalGraphPanel.Series(
                "Occupied",
                values,
                HomeViewHelper.PURPLE,
                HomeViewHelper.PURPLE_DARK
        );
    }

    @Override
    public void doLayout() {
        super.doLayout();
        layoutCategoryTabs();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        layoutCategoryTabs();
        super.paintComponent(graphics);
    }

    @Override
    protected int plotTopInset() {
        int tabsBottom = categoryTabs.getY() + categoryTabs.getHeight();

        return tabsBottom > 0
                ? tabsBottom + CATEGORY_TABS_BOTTOM_MARGIN
                : 120;
    }

    private void layoutCategoryTabs() {
        Rectangle visible = getVisibleRect();

        int availableWidth = visible.width > 0
                ? visible.width - (HEADER_LEFT * 2)
                : getWidth() - (HEADER_LEFT * 2);

        int x = visible.width > 0
                ? visible.x + HEADER_LEFT
                : HEADER_LEFT;

        int tabWidth = Math.max(180, availableWidth);
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
        DelayedProgressDialog.Handle progress = DelayedProgressDialog.showAfter(
                this,
                "Loading Graph Data",
                "Database is taking longer than usual. Loading occupancy data..."
        );

        new SwingWorker<DashboardDao.OccupancyChartData, Void>() {
            @Override
            protected DashboardDao.OccupancyChartData doInBackground() throws Exception {
                return dashboardDao.loadOccupancyChart(selectedCategory);
            }

            @Override
            protected void done() {
                try {
                    DashboardDao.OccupancyChartData data = get();

                    setGraphData(
                            data.labels(),
                            capacitySeries(data.capacity()),
                            occupiedSeries(data.occupied())
                    );
                } catch (InterruptedException | ExecutionException exception) {
                    setGraphData(
                            new String[0],
                            capacitySeries(new int[0]),
                            occupiedSeries(new int[0])
                    );
                } finally {
                    progress.done();
                }
            }
        }.execute();
    }

    private static class CategoryTabsPanel extends JPanel {
        /*
         * TAB ROW STYLE CONTROLS
         *
         * HORIZONTAL_GAP:
         * Space between tabs in same row.
         *
         * VERTICAL_GAP:
         * Space between tab rows when tabs wrap to next line.
         *
         * ROW_HEIGHT:
         * Height of each tab row.
         * Increase only if text feels clipped or click area feels small.
         */
        private static final int HORIZONTAL_GAP = 18;
        private static final int VERTICAL_GAP = 8;
        private static final int ROW_HEIGHT = 34;

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

        @Override
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

            /*
             * TAB LABEL PADDING
             *
             * top:
             * Controls space above tab text.
             *
             * left/right:
             * Controls text breathing space inside each tab.
             *
             * bottom:
             * Controls space below tab text and underline.
             */
            setMargin(new Insets(0, 0, 0, 0));
            setBorder(BorderFactory.createEmptyBorder(8, 4, 10, 10));
        }

        private boolean hasValue(String otherValue) {
            return value.equals(otherValue);
        }

        private void setActive(boolean active) {
            this.active = active;
            setForeground(active ? HomeViewHelper.PRIMARY : HomeViewHelper.TEXT_SECONDARY);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            if (!active) {
                return;
            }

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            FontMetrics metrics = g2.getFontMetrics(getFont());

            int underlineWidth = Math.max(
                    24,
                    Math.min(getWidth() - 14, metrics.stringWidth(getText()) + 8)
            );

            int underlineX = (getWidth() - underlineWidth) / 2;

            g2.setColor(HomeViewHelper.PRIMARY);
            g2.fillRoundRect(
                    underlineX,
                    getHeight() - 5,
                    underlineWidth,
                    3,
                    3,
                    3
            );

            g2.dispose();
        }
    }
}