package com.kgm.ui.panel;

import com.kgm.dao.DashboardDao;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class KPICategoryPanel extends JPanel {
    private static final int METRIC_MIN_WIDTH = 176;
    private static final int METRIC_MIN_CARD_WIDTH = 148;
    private static final int METRIC_CARD_HEIGHT = 94;
    private static final int METRIC_GAP = 12;

    private final DashboardDao.CategoryKpiStats categoryStats;
    private final Consumer<MetricSelection> onMetricClicked;

    public KPICategoryPanel(
            DashboardDao.CategoryKpiStats categoryStats,
            Consumer<MetricSelection> onMetricClicked
    ) {
        this.categoryStats = categoryStats;
        this.onMetricClicked = onMetricClicked;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(18, 0, 18, 0));
        setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel categoryContainer = new JPanel();
        categoryContainer.setLayout(new BoxLayout(categoryContainer, BoxLayout.Y_AXIS));
        categoryContainer.setOpaque(false);

        JLabel headingLabel = new JLabel(categoryStats.categoryName());
        headingLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        headingLabel.setForeground(HomeViewHelper.TEXT_PRIMARY);
        headingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headingLabel.setToolTipText(categoryStats.categoryName());
        categoryContainer.add(headingLabel);
        categoryContainer.add(Box.createVerticalStrut(10));

        categoryContainer.add(createMetricRow(
                MetricGroup.ROOMS,
                categoryStats.totalRooms(),
                categoryStats.occupiedRooms(),
                categoryStats.vacantRooms()
        ));
        categoryContainer.add(Box.createVerticalStrut(8));
        categoryContainer.add(createMetricRow(
                MetricGroup.BEDS,
                categoryStats.totalBeds(),
                categoryStats.occupiedBeds(),
                categoryStats.vacantBeds()
        ));

        add(categoryContainer, BorderLayout.CENTER);
    }

    private JPanel createMetricRow(MetricGroup group, int total, int occupied, int vacant) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel rowLabel = new JLabel(group.label());
        rowLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        rowLabel.setForeground(HomeViewHelper.TEXT_SECONDARY);
        rowLabel.setPreferredSize(new Dimension(58, METRIC_CARD_HEIGHT));
        rowLabel.setHorizontalAlignment(SwingConstants.LEFT);
        rowLabel.setVerticalAlignment(SwingConstants.CENTER);
        row.add(rowLabel, BorderLayout.WEST);

        JPanel metricGrid = responsiveMetricGrid();
        metricGrid.setOpaque(false);
        metricGrid.add(createMetricCard(MetricType.TOTAL, group, total, HomeViewHelper.BLUE));
        metricGrid.add(createMetricCard(MetricType.OCCUPIED, group, occupied, HomeViewHelper.PURPLE));
        metricGrid.add(createMetricCard(MetricType.VACANT, group, vacant, HomeViewHelper.TEAL));
        row.add(metricGrid, BorderLayout.CENTER);
        return row;
    }

    private JPanel responsiveMetricGrid() {
        return new JPanel(new GridLayout(0, 3, METRIC_GAP, METRIC_GAP)) {
            public void doLayout() {
                updateResponsiveColumns(this);
                super.doLayout();
            }

            public Dimension getPreferredSize() {
                updateResponsiveColumns(this);
                Container parent = getParent();
                int width = parent == null || parent.getWidth() <= 0
                        ? super.getPreferredSize().width
                        : parent.getWidth();
                GridLayout layout = (GridLayout) getLayout();
                int columns = Math.max(1, layout.getColumns());
                int rows = (int) Math.ceil(getComponentCount() / (double) columns);
                int height = rows * METRIC_CARD_HEIGHT + Math.max(0, rows - 1) * METRIC_GAP;
                return new Dimension(width, height);
            }
        };
    }

    private void updateResponsiveColumns(JPanel panel) {
        int width = panel.getWidth();
        if (width <= 0 && panel.getParent() != null) {
            width = panel.getParent().getWidth();
        }
        int columns = Math.max(1, Math.min(3, Math.max(1, width / (METRIC_MIN_WIDTH + METRIC_GAP))));
        columns = Math.min(Math.max(1, panel.getComponentCount()), columns);
        GridLayout layout = (GridLayout) panel.getLayout();
        if (layout.getColumns() != columns) {
            layout.setColumns(columns);
        }
    }

    private JPanel createMetricCard(
            MetricType type,
            MetricGroup group,
            int value,
            Color accent
    ) {
        MetricSelection selection = new MetricSelection(categoryStats.categoryName(), group, type);
        String detail = cardDetail(type, group);
        JPanel card = HomeViewHelper.kpiCard(
                cardTitle(type, group),
                String.valueOf(value),
                detail,
                accent,
                accent,
                false
        );
        Dimension cardSize = new Dimension(METRIC_MIN_WIDTH, METRIC_CARD_HEIGHT);
        card.setPreferredSize(cardSize);
        card.setMinimumSize(new Dimension(METRIC_MIN_CARD_WIDTH, METRIC_CARD_HEIGHT));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("View " + selection.displayTitle());
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (onMetricClicked != null) {
                    onMetricClicked.accept(selection);
                }
            }
        });
        return card;
    }

    private String cardTitle(MetricType type, MetricGroup group) {
        return type.label() + " " + group.label();
    }

    private String cardDetail(MetricType type, MetricGroup group) {
        String categoryName = categoryStats.categoryName() == null || categoryStats.categoryName().isBlank()
                ? "this category"
                : categoryStats.categoryName().trim();
        if (group == MetricGroup.ROOMS) {
            return switch (type) {
                case TOTAL -> "All rooms in " + categoryName;
                case OCCUPIED -> "Fully or partially occupied";
                case VACANT -> "Vacant rooms in " + categoryName;
            };
        }
        return switch (type) {
            case TOTAL -> "All beds in " + categoryName;
            case OCCUPIED -> "Currently occupied beds";
            case VACANT -> "Available vacant beds";
        };
    }

    public record MetricSelection(String categoryName, MetricGroup group, MetricType type) {
        public String displayTitle() {
            return type.label() + " " + group.label() + " in " + categoryName;
        }
    }

    public enum MetricGroup {
        ROOMS("Rooms"),
        BEDS("Beds");

        private final String label;

        MetricGroup(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public enum MetricType {
        TOTAL("Total"),
        OCCUPIED("Occupied"),
        VACANT("Vacant");

        private final String label;

        MetricType(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
