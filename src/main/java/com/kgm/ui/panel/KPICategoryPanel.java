package com.kgm.ui.panel;

import com.kgm.dao.DashboardDao;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class KPICategoryPanel extends JPanel {
    private static final int CARD_RADIUS = 8;
    private static final int METRIC_MIN_WIDTH = 118;

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
        setBorder(BorderFactory.createEmptyBorder(0, 0, 22, 0));
        setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel categoryContainer = new JPanel();
        categoryContainer.setLayout(new BoxLayout(categoryContainer, BoxLayout.Y_AXIS));
        categoryContainer.setOpaque(false);

        JLabel headingLabel = new JLabel(categoryStats.categoryName());
        headingLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        headingLabel.setForeground(HomeViewHelper.TEXT_PRIMARY);
        headingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        categoryContainer.add(headingLabel);
        categoryContainer.add(Box.createVerticalStrut(10));

        categoryContainer.add(createMetricRow(
                MetricGroup.ROOMS,
                categoryStats.totalRooms(),
                categoryStats.occupiedRooms(),
                categoryStats.vacantRooms()
        ));
        categoryContainer.add(Box.createVerticalStrut(10));
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
        rowLabel.setPreferredSize(new Dimension(58, 68));
        rowLabel.setHorizontalAlignment(SwingConstants.LEFT);
        rowLabel.setVerticalAlignment(SwingConstants.CENTER);
        row.add(rowLabel, BorderLayout.WEST);

        JPanel metricGrid = responsiveMetricGrid();
        metricGrid.setOpaque(false);
        metricGrid.add(createMetricCard(MetricType.TOTAL, group, total,
                HomeViewHelper.PRIMARY_DARK, HomeViewHelper.PRIMARY_LIGHT));
        metricGrid.add(createMetricCard(MetricType.OCCUPIED, group, occupied,
                HomeViewHelper.OCCUPIED_DARK, HomeViewHelper.OCCUPIED_LIGHT));
        metricGrid.add(createMetricCard(MetricType.VACANT, group, vacant,
                HomeViewHelper.VACANT_DARK, HomeViewHelper.VACANT_LIGHT));
        row.add(metricGrid, BorderLayout.CENTER);
        return row;
    }

    private JPanel responsiveMetricGrid() {
        return new JPanel(new GridLayout(0, 3, 12, 8)) {
            public void doLayout() {
                updateResponsiveColumns(this);
                super.doLayout();
            }

            public Dimension getPreferredSize() {
                updateResponsiveColumns(this);
                return super.getPreferredSize();
            }
        };
    }

    private void updateResponsiveColumns(JPanel panel) {
        int width = panel.getWidth();
        if (width <= 0 && panel.getParent() != null) {
            width = panel.getParent().getWidth();
        }
        int columns = Math.max(1, Math.min(3, Math.max(1, width / METRIC_MIN_WIDTH)));
        GridLayout layout = (GridLayout) panel.getLayout();
        if (layout.getColumns() != columns) {
            layout.setColumns(columns);
        }
    }

    private JPanel createMetricCard(
            MetricType type,
            MetricGroup group,
            int value,
            Color start,
            Color end
    ) {
        MetricSelection selection = new MetricSelection(categoryStats.categoryName(), group, type);
        JPanel card = new JPanel(new BorderLayout(0, 4)) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, start, getWidth(), getHeight(), end));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), CARD_RADIUS, CARD_RADIUS);
                g2.dispose();
                super.paintComponent(graphics);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        card.setPreferredSize(new Dimension(150, 68));
        card.setMinimumSize(new Dimension(96, 68));
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

        JLabel labelComp = new JLabel(type.label());
        labelComp.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        labelComp.setForeground(new Color(238, 247, 255));

        JLabel valueComp = new JLabel(String.valueOf(value));
        valueComp.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueComp.setForeground(Color.WHITE);

        JLabel detailComp = new JLabel(group.detailLabel());
        detailComp.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        detailComp.setForeground(new Color(226, 239, 249));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(labelComp);
        text.add(Box.createVerticalStrut(2));
        text.add(valueComp);
        text.add(Box.createVerticalStrut(1));
        text.add(detailComp);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    public record MetricSelection(String categoryName, MetricGroup group, MetricType type) {
        public String displayTitle() {
            return type.label() + " " + group.label() + " in " + categoryName;
        }
    }

    public enum MetricGroup {
        ROOMS("Rooms", "room records"),
        BEDS("Beds", "bed capacity");

        private final String label;
        private final String detailLabel;

        MetricGroup(String label, String detailLabel) {
            this.label = label;
            this.detailLabel = detailLabel;
        }

        public String label() {
            return label;
        }

        public String detailLabel() {
            return detailLabel;
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
