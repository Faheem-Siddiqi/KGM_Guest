package com.kgm.ui.panel;

import com.kgm.dao.DashboardDao;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;

public class KPICategoryPanel extends JPanel {
    private static final int METRIC_MIN_WIDTH = 176;
    private static final int METRIC_MIN_CARD_WIDTH = 148;
    private static final int METRIC_CARD_HEIGHT = 94;
    private static final int METRIC_GAP = 12;
    private static final int ROW_LABEL_WIDTH = 58;
    private static final int ROW_LABEL_GAP = 14;

    private final DashboardDao.CategoryKpiStats categoryStats;

    public KPICategoryPanel(DashboardDao.CategoryKpiStats categoryStats) {
        this.categoryStats = categoryStats;
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
        categoryContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

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
        JPanel metricGrid = responsiveMetricGrid();
        metricGrid.setOpaque(false);

        metricGrid.add(createMetricCard(MetricType.TOTAL, group, total, HomeViewHelper.BLUE));
        metricGrid.add(createMetricCard(MetricType.OCCUPIED, group, occupied, HomeViewHelper.TEAL));
        metricGrid.add(createMetricCard(MetricType.VACANT, group, vacant, HomeViewHelper.PURPLE));

        JPanel row = new JPanel(new BorderLayout(ROW_LABEL_GAP, 0)) {
            @Override
            public Dimension getPreferredSize() {
                int parentWidth = getParent() == null ? 0 : getParent().getWidth();

                int gridWidth = parentWidth > 0
                        ? Math.max(METRIC_MIN_CARD_WIDTH, parentWidth - ROW_LABEL_WIDTH - ROW_LABEL_GAP)
                        : metricGrid.getPreferredSize().width;

                updateResponsiveColumns(metricGrid, gridWidth);

                Dimension labelSize = getComponent(0).getPreferredSize();
                Dimension gridSize = metricGrid.getPreferredSize();

                int width = parentWidth > 0
                        ? parentWidth
                        : labelSize.width + ROW_LABEL_GAP + gridSize.width;

                int height = Math.max(labelSize.height, gridSize.height);
                return new Dimension(width, height);
            }

            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }

            @Override
            public Dimension getMaximumSize() {
                Dimension preferred = getPreferredSize();
                return new Dimension(Integer.MAX_VALUE, preferred.height);
            }

            @Override
            public void doLayout() {
                int gridWidth = Math.max(
                        METRIC_MIN_CARD_WIDTH,
                        getWidth() - ROW_LABEL_WIDTH - ROW_LABEL_GAP
                );

                updateResponsiveColumns(metricGrid, gridWidth);
                super.doLayout();
            }
        };

        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel rowLabel = new JLabel(group.label());
        rowLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        rowLabel.setForeground(HomeViewHelper.TEXT_SECONDARY);
        rowLabel.setPreferredSize(new Dimension(ROW_LABEL_WIDTH, METRIC_CARD_HEIGHT));
        rowLabel.setMinimumSize(new Dimension(ROW_LABEL_WIDTH, METRIC_CARD_HEIGHT));
        rowLabel.setHorizontalAlignment(SwingConstants.LEFT);
        rowLabel.setVerticalAlignment(SwingConstants.CENTER);

        row.add(rowLabel, BorderLayout.WEST);
        row.add(metricGrid, BorderLayout.CENTER);

        return row;
    }

    private JPanel responsiveMetricGrid() {
        return new JPanel(new GridLayout(0, 3, METRIC_GAP, METRIC_GAP)) {
            @Override
            public void doLayout() {
                updateResponsiveColumns(this, getWidth());
                super.doLayout();
            }

            @Override
            public Dimension getPreferredSize() {
                int width = getWidth();

                if (width <= 0 && getParent() != null) {
                    width = getParent().getWidth() - ROW_LABEL_WIDTH - ROW_LABEL_GAP;
                }

                if (width <= 0) {
                    width = (METRIC_MIN_WIDTH * 3) + (METRIC_GAP * 2);
                }

                updateResponsiveColumns(this, width);

                GridLayout layout = (GridLayout) getLayout();
                int columns = Math.max(1, layout.getColumns());
                int rows = (int) Math.ceil(getComponentCount() / (double) columns);

                int height = rows * METRIC_CARD_HEIGHT + Math.max(0, rows - 1) * METRIC_GAP;
                return new Dimension(width, height);
            }

            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }

            @Override
            public Dimension getMaximumSize() {
                Dimension preferred = getPreferredSize();
                return new Dimension(Integer.MAX_VALUE, preferred.height);
            }
        };
    }

    private void updateResponsiveColumns(JPanel panel, int width) {
        if (width <= 0) {
            width = panel.getWidth();
        }

        if (width <= 0 && panel.getParent() != null) {
            width = panel.getParent().getWidth() - ROW_LABEL_WIDTH - ROW_LABEL_GAP;
        }

        int columns = Math.max(
                1,
                Math.min(
                        3,
                        (width + METRIC_GAP) / (METRIC_MIN_CARD_WIDTH + METRIC_GAP)
                )
        );

        columns = Math.min(Math.max(1, panel.getComponentCount()), columns);

        GridLayout layout = (GridLayout) panel.getLayout();

        if (layout.getColumns() != columns) {
            layout.setColumns(columns);
            panel.revalidate();
        }
    }

    private JPanel createMetricCard(
            MetricType type,
            MetricGroup group,
            int value,
            Color accent
    ) {
        String detail = cardDetail(type, group);

        JPanel card = HomeViewHelper.kpiCard(
                cardTitle(type, group),
                String.valueOf(value),
                detail,
                accent,
                accent,
                false
        );

        Dimension preferredSize = new Dimension(METRIC_MIN_WIDTH, METRIC_CARD_HEIGHT);
        Dimension minimumSize = new Dimension(METRIC_MIN_CARD_WIDTH, METRIC_CARD_HEIGHT);

        card.setPreferredSize(preferredSize);
        card.setMinimumSize(minimumSize);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, METRIC_CARD_HEIGHT));

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
                case OCCUPIED -> roomBreakdown(
                        categoryStats.occupiedRooms(),
                        categoryStats.fullyOccupiedRooms(),
                        categoryStats.partiallyOccupiedRooms(),
                        "occupied"
                );
                case VACANT -> roomBreakdown(
                        categoryStats.vacantRooms(),
                        categoryStats.fullyVacantRooms(),
                        categoryStats.partiallyVacantRooms(),
                        "vacant"
                );
            };
        }

        return switch (type) {
            case TOTAL -> "All beds in " + categoryName;
            case OCCUPIED -> "Currently occupied beds";
            case VACANT -> "Available vacant beds";
        };
    }

    private String roomBreakdown(int total, int fully, int partially, String state) {
        if (total <= 0) {
            return "No " + state + " rooms";
        }
        if (fully > 0 && partially > 0) {
            return "Full " + fully + " / Partial " + partially;
        }
        if (partially > 0) {
            return partially + " partially " + state + " room" + plural(partially);
        }
        return total + " " + state + " room" + plural(total);
    }

    private String plural(int count) {
        return count == 1 ? "" : "s";
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
