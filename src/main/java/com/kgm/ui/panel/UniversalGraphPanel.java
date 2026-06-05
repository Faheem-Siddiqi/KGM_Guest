package com.kgm.ui.panel;

import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class UniversalGraphPanel extends JPanel implements Scrollable {
    private static final int MIN_VISIBLE_BAR_HEIGHT = 5;
    private static final int MIN_SKEWED_BAR_HEIGHT = 9;
    private static final int BAR_RADIUS = 2;
    private static final int GRAPH_HEIGHT = 400;
    private static final int CARD_PADDING_X = 24;
    private static final int PLOT_LEFT_PADDING = 54;
    private static final int PLOT_RIGHT_PADDING = 24;
    private static final int LABEL_TOP_MARGIN = 22;
    private static final int BOTTOM_CONTENT_PADDING = 96;
    private static final double LINEAR_SCALE_EXPONENT = 1.0;
    private static final double COMPRESSED_SCALE_EXPONENT = 0.48;
    private static final double SCALE_COMPRESSION_THRESHOLD = 6.0;
    private static final int MULTI_SERIES_GROUP_WIDTH = 82;
    private static final int SINGLE_SERIES_GROUP_WIDTH = 96;

    private final String title;
    private final String subtitle;
    private String[] categories;
    private Series[] series;
    private HoverBar hoveredBar;
    private CategorySelectionListener categorySelectionListener;

    public UniversalGraphPanel(String title, String subtitle, String[] categories, Series... series) {
        this.title = title;
        this.subtitle = subtitle;
        this.categories = categories == null ? new String[0] : categories.clone();
        this.series = series == null ? new Series[0] : series.clone();
        setOpaque(false);
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(preferredGraphWidth(), GRAPH_HEIGHT));
        setMinimumSize(new Dimension(320, 300));
        installHoverCursor();
    }

    public void setGraphData(String[] categories, Series... series) {
        this.categories = categories == null ? new String[0] : categories.clone();
        this.series = series == null ? new Series[0] : series.clone();
        hoveredBar = null;
        setPreferredSize(new Dimension(preferredGraphWidth(), GRAPH_HEIGHT));
        revalidate();
        repaint();
    }

    public boolean needsHorizontalScroll() {
        return categories.length > 5;
    }

    public void setCategorySelectionListener(CategorySelectionListener categorySelectionListener) {
        this.categorySelectionListener = categorySelectionListener;
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        drawHeader(g2, width);
        if (hasChartData()) {
            drawBars(g2, width, height);
            drawLegend(g2, width, height);
            drawHoverOverlay(g2, width, height);
        } else {
            drawEmptyState(g2, width, height);
        }

        g2.dispose();
    }

    private void installHoverCursor() {
        addMouseMotionListener(new MouseAdapter() {
            public void mouseMoved(MouseEvent event) {
                HoverBar nextHover = hoverBarAt(event.getPoint());
                if (nextHover == null ? hoveredBar != null : !nextHover.equals(hoveredBar)) {
                    hoveredBar = nextHover;
                    repaint();
                }
                setCursor(Cursor.getPredefinedCursor(
                        nextHover == null ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR
                ));
            }
        });
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                HoverBar clickedBar = hoverBarAt(event.getPoint());
                if (clickedBar != null && categorySelectionListener != null) {
                    categorySelectionListener.categorySelected(categoryLabel(clickedBar.categoryIndex()));
                }
            }

            public void mouseExited(MouseEvent event) {
                hoveredBar = null;
                repaint();
                setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    private void drawHeader(Graphics2D g2, int width) {
        g2.setColor(HomeViewHelper.TEXT_PRIMARY);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
        drawTruncatedString(g2, title, CARD_PADDING_X, 30, width - CARD_PADDING_X * 2);

        g2.setColor(HomeViewHelper.TEXT_SECONDARY);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        drawTruncatedString(g2, subtitleText(), CARD_PADDING_X, 51, width - CARD_PADDING_X * 2);
    }

    private String subtitleText() {
        String base = subtitle == null ? "" : subtitle.trim();
        if (!usesCompressedScale()) {
            return base;
        }
        String scaleNote = "Scale adjusted for readability";
        return base.isEmpty() ? scaleNote : base + " | " + scaleNote;
    }

    private int legendWidth(Graphics2D g2) {
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        FontMetrics metrics = g2.getFontMetrics();
        int width = 0;
        for (Series item : series) {
            width += metrics.stringWidth(item.name) + 30;
        }
        return width + Math.max(0, series.length - 1) * 8;
    }

    private void drawLegend(Graphics2D g2, int width, int height) {
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        FontMetrics metrics = g2.getFontMetrics();
        int legendWidth = legendWidth(g2);
        Rectangle visible = getVisibleRect();
        int visibleX = visible.width > 0 ? visible.x : 0;
        int visibleWidth = visible.width > 0 ? visible.width : width;
        int legendX = visibleX + Math.max(0, (visibleWidth - legendWidth) / 2);
        int legendY = height - 36;

        for (Series item : series) {
            int pillWidth = metrics.stringWidth(item.name) + 30;

            g2.setColor(new Color(248, 250, 252));
            g2.fillRoundRect(legendX, legendY, pillWidth, 24, 8, 8);
            g2.setColor(new Color(226, 232, 240));
            g2.drawRoundRect(legendX, legendY, pillWidth, 24, 8, 8);

            GradientPaint paint = new GradientPaint(legendX + 10, legendY + 7, item.start, legendX + 18, legendY + 15, item.end);
            g2.setPaint(paint);
            g2.fillRoundRect(legendX + 10, legendY + 7, 9, 9, 5, 5);
            g2.setColor(HomeViewHelper.TEXT_SECONDARY);
            g2.drawString(item.name, legendX + 24, legendY + 16);
            legendX += pillWidth + 8;
        }
    }

    private void drawBars(Graphics2D g2, int width, int height) {
        GraphLayout layout = graphLayout(width, height);
        BarLayout bars = barLayout(layout);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        FontMetrics metrics = g2.getFontMetrics();

        for (int step = 0; step <= 4; step++) {
            double normalized = step / 4.0;
            int y = layout.baseY() - (int) Math.round(layout.plotH() * normalized);
            int value = axisValueFor(normalized, layout);
            g2.setColor(new Color(241, 245, 249));
            g2.drawLine(layout.plotX(), y, layout.plotX() + layout.plotW(), y);
            g2.setColor(HomeViewHelper.TEXT_SECONDARY);
            String label = String.valueOf(value);
            g2.drawString(label, layout.plotX() - metrics.stringWidth(label) - 9, y + 4);
        }

        g2.setColor(new Color(226, 232, 240));
        g2.drawLine(layout.plotX(), layout.baseY(), layout.plotX() + layout.plotW(), layout.baseY());

        for (int categoryIndex = 0; categoryIndex < categories.length; categoryIndex++) {
            int groupX = bars.chartX() + categoryIndex * bars.groupW();
            int groupStart = groupX + (bars.groupW() - bars.totalBarsW()) / 2;

            if (hoveredBar != null && hoveredBar.categoryIndex() == categoryIndex) {
                g2.setColor(new Color(15, 23, 42, 7));
                g2.fillRoundRect(groupX + 4, layout.plotY(), Math.max(12, bars.groupW() - 8), layout.plotH(), 8, 8);
            }

            for (int seriesIndex = 0; seriesIndex < series.length; seriesIndex++) {
                Series item = series[seriesIndex];
                int value = categoryIndex < item.values.length ? Math.max(0, item.values[categoryIndex]) : 0;
                if (value <= 0) {
                    continue;
                }

                int scaledBarH = scaledBarHeight(value, layout);
                int barH = Math.min(layout.plotH(), scaledBarH);
                int x = groupStart + seriesIndex * (bars.barW() + bars.seriesGap());
                int y = layout.baseY() - barH;

                g2.setPaint(new GradientPaint(x, y, item.start, x, layout.baseY(), item.end));
                g2.fillRoundRect(x, y, bars.barW(), barH, BAR_RADIUS, BAR_RADIUS);
                if (hoveredBar != null
                        && hoveredBar.categoryIndex() == categoryIndex
                        && hoveredBar.seriesIndex() == seriesIndex) {
                    Stroke originalStroke = g2.getStroke();
                    g2.setStroke(new BasicStroke(2f));
                    g2.setColor(new Color(15, 23, 42, 150));
                    g2.drawRoundRect(x - 2, y - 2, bars.barW() + 4, barH + 4, BAR_RADIUS + 3, BAR_RADIUS + 3);
                    g2.setStroke(originalStroke);
                }
            }

            String label = categories[categoryIndex];
            g2.setColor(HomeViewHelper.TEXT_SECONDARY);
            drawCategoryLabel(
                    g2,
                    label,
                    groupX + bars.groupW() / 2,
                    layout.baseY() + LABEL_TOP_MARGIN,
                    Math.max(52, bars.groupW() - 10)
            );
        }
    }

    private HoverBar hoverBarAt(Point point) {
        if (point == null || !hasChartData()) {
            return null;
        }

        GraphLayout layout = graphLayout(getWidth(), getHeight());
        BarLayout bars = barLayout(layout);

        for (int categoryIndex = 0; categoryIndex < categories.length; categoryIndex++) {
            int groupX = bars.chartX() + categoryIndex * bars.groupW();
            int groupStart = groupX + (bars.groupW() - bars.totalBarsW()) / 2;
            for (int seriesIndex = 0; seriesIndex < series.length; seriesIndex++) {
                int value = categoryIndex < series[seriesIndex].values.length
                        ? Math.max(0, series[seriesIndex].values[categoryIndex])
                        : 0;
                if (value <= 0) {
                    continue;
                }
                int scaledBarH = scaledBarHeight(value, layout);
                int barH = Math.min(layout.plotH(), scaledBarH);
                int x = groupStart + seriesIndex * (bars.barW() + bars.seriesGap());
                int y = layout.baseY() - barH;
                Rectangle bounds = new Rectangle(x - 5, y - 5, bars.barW() + 10, barH + 10);
                if (bounds.contains(point)) {
                    return new HoverBar(categoryIndex, seriesIndex);
                }
            }
        }
        return null;
    }

    private void drawHoverOverlay(Graphics2D g2, int width, int height) {
        if (hoveredBar == null) {
            return;
        }

        String category = categoryLabel(hoveredBar.categoryIndex());
        List<String> extraLines = additionalStatLines(hoveredBar.categoryIndex());

        Font titleFont = new Font("Segoe UI Semibold", Font.PLAIN, 12);
        Font categoryFont = new Font("Segoe UI", Font.BOLD, 13);
        Font rowFont = new Font("Segoe UI", Font.PLAIN, 12);
        Font valueFont = new Font("Segoe UI Semibold", Font.PLAIN, 12);

        g2.setFont(categoryFont);
        int boxWidth = g2.getFontMetrics().stringWidth(category) + 36;
        g2.setFont(titleFont);
        boxWidth = Math.max(boxWidth, g2.getFontMetrics().stringWidth(title) + 36);
        g2.setFont(rowFont);
        FontMetrics rowMetrics = g2.getFontMetrics();
        g2.setFont(valueFont);
        FontMetrics valueMetrics = g2.getFontMetrics();
        for (Series item : series) {
            int value = hoveredBar.categoryIndex() < item.values.length
                    ? Math.max(0, item.values[hoveredBar.categoryIndex()])
                    : 0;
            boxWidth = Math.max(
                    boxWidth,
                    42 + rowMetrics.stringWidth(item.name) + valueMetrics.stringWidth(String.valueOf(value)) + 34
            );
        }
        g2.setFont(rowFont);
        for (String line : extraLines) {
            boxWidth = Math.max(boxWidth, rowMetrics.stringWidth(line) + 36);
        }
        boxWidth = Math.min(Math.max(boxWidth, 220), Math.max(220, width - 28));
        int boxHeight = 52 + series.length * 24 + extraLines.size() * 18;

        Point mouse = getMousePosition();
        int x = mouse == null ? width - boxWidth - 18 : mouse.x + 18;
        int y = mouse == null ? 70 : mouse.y - boxHeight - 14;
        x = clamp(x, 14, Math.max(14, width - boxWidth - 14));
        y = clamp(y, 62, Math.max(62, height - boxHeight - 14));

        g2.setColor(new Color(15, 23, 42, 22));
        g2.fillRoundRect(x + 2, y + 4, boxWidth, boxHeight, 10, 10);

        Composite originalComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.98f));
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(x, y, boxWidth, boxHeight, 10, 10);
        g2.setComposite(originalComposite);
        g2.setColor(new Color(226, 232, 240));
        g2.drawRoundRect(x, y, boxWidth, boxHeight, 10, 10);

        g2.setFont(titleFont);
        g2.setColor(HomeViewHelper.TEXT_SECONDARY);
        g2.drawString(title, x + 14, y + 18);

        g2.setFont(categoryFont);
        g2.setColor(HomeViewHelper.TEXT_PRIMARY);
        drawTruncatedString(g2, category, x + 14, y + 37, boxWidth - 28);

        int rowY = y + 59;
        for (int seriesIndex = 0; seriesIndex < series.length; seriesIndex++) {
            Series item = series[seriesIndex];
            int value = hoveredBar.categoryIndex() < item.values.length
                    ? Math.max(0, item.values[hoveredBar.categoryIndex()])
                    : 0;

            if (seriesIndex == hoveredBar.seriesIndex()) {
                g2.setColor(new Color(item.start.getRed(), item.start.getGreen(), item.start.getBlue(), 18));
                g2.fillRoundRect(x + 10, rowY - 15, boxWidth - 20, 21, 7, 7);
            }

            g2.setColor(item.start);
            g2.fillRoundRect(x + 16, rowY - 8, 8, 8, 5, 5);
            g2.setFont(rowFont);
            g2.setColor(HomeViewHelper.TEXT_SECONDARY);
            drawTruncatedString(g2, item.name, x + 30, rowY, boxWidth - 88);
            g2.setFont(valueFont);
            g2.setColor(HomeViewHelper.TEXT_PRIMARY);
            String valueText = String.valueOf(value);
            g2.drawString(valueText, x + boxWidth - 16 - valueMetrics.stringWidth(valueText), rowY);
            rowY += 24;
        }

        g2.setFont(rowFont);
        g2.setColor(HomeViewHelper.TEXT_SECONDARY);
        for (String line : extraLines) {
            drawTruncatedString(g2, line, x + 16, rowY, boxWidth - 32);
            rowY += 18;
        }
    }

    protected List<String> additionalStatLines(int categoryIndex) {
        List<String> lines = new ArrayList<>();
        Integer availableBeds = availableBeds(categoryIndex);
        if (availableBeds != null) {
            lines.add("Available Beds: " + availableBeds);
        }
        return lines;
    }

    private Integer availableBeds(int categoryIndex) {
        Integer capacity = null;
        Integer occupied = null;
        for (Series item : series) {
            if (categoryIndex >= item.values.length) {
                continue;
            }
            if ("Capacity".equalsIgnoreCase(item.name)) {
                capacity = Math.max(0, item.values[categoryIndex]);
            } else if ("Occupied".equalsIgnoreCase(item.name)) {
                occupied = Math.max(0, item.values[categoryIndex]);
            }
        }
        return capacity == null || occupied == null ? null : Math.max(0, capacity - occupied);
    }

    private String categoryLabel(int categoryIndex) {
        return categoryIndex < categories.length && categories[categoryIndex] != null
                ? categories[categoryIndex].replaceAll("\\R", " / ")
                : "";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private GraphLayout graphLayout(int width, int height) {
        ScaleConfig scale = scaleConfig();
        int plotX = PLOT_LEFT_PADDING;
        int plotY = plotTopInset();
        int plotW = Math.max(120, width - PLOT_LEFT_PADDING - PLOT_RIGHT_PADDING);
        int plotH = Math.max(120, height - plotY - BOTTOM_CONTENT_PADDING);
        int baseY = plotY + plotH;
        return new GraphLayout(
                plotX,
                plotY,
                plotW,
                plotH,
                baseY,
                scale.max(),
                scale.exponent(),
                scale.minVisibleBarHeight()
        );
    }

    private int scaledBarHeight(int value, GraphLayout layout) {
        if (value <= 0) {
            return 0;
        }
        double normalized = Math.min(1.0, value / (double) layout.max());
        double scaled = Math.pow(normalized, layout.scaleExponent());
        int scaledHeight = (int) Math.round(scaled * layout.plotH());
        return Math.max(layout.minVisibleBarHeight(), scaledHeight);
    }

    private int axisValueFor(double normalizedPosition, GraphLayout layout) {
        if (normalizedPosition <= 0) {
            return 0;
        }
        if (normalizedPosition >= 1) {
            return layout.max();
        }
        double valueRatio = Math.pow(normalizedPosition, 1.0 / layout.scaleExponent());
        return (int) Math.round(layout.max() * valueRatio);
    }

    private ScaleConfig scaleConfig() {
        int minPositive = Integer.MAX_VALUE;
        int maxPositive = 0;
        for (Series item : series) {
            for (int value : item.values) {
                if (value > 0) {
                    minPositive = Math.min(minPositive, value);
                    maxPositive = Math.max(maxPositive, value);
                }
            }
        }
        int max = niceMax();
        boolean compressed = minPositive != Integer.MAX_VALUE
                && maxPositive > minPositive
                && maxPositive / (double) minPositive >= SCALE_COMPRESSION_THRESHOLD;
        return new ScaleConfig(
                max,
                compressed ? COMPRESSED_SCALE_EXPONENT : LINEAR_SCALE_EXPONENT,
                compressed ? MIN_SKEWED_BAR_HEIGHT : MIN_VISIBLE_BAR_HEIGHT
        );
    }

    private boolean usesCompressedScale() {
        return scaleConfig().exponent() < LINEAR_SCALE_EXPONENT;
    }

    private BarLayout barLayout(GraphLayout layout) {
        int categoryCount = Math.max(1, categories.length);
        int maxGroupW = series.length > 1 ? MULTI_SERIES_GROUP_WIDTH : singleSeriesGroupWidth();
        int chartW = categories.length == 0
                ? layout.plotW()
                : Math.min(layout.plotW(), Math.max(maxGroupW, categoryCount * maxGroupW));
        int chartX = layout.plotX() + Math.min(12, Math.max(0, layout.plotW() - chartW));
        int groupW = Math.max(1, chartW / categoryCount);
        int seriesGap = series.length > 1 ? 5 : 0;
        int barSlotPadding = series.length > 1 ? 20 : 22;
        int rawBarW = (groupW - barSlotPadding - Math.max(0, series.length - 1) * seriesGap)
                / Math.max(1, series.length);
        int maxBarW = series.length > 1 ? 26 : 32;
        int barW = Math.max(8, Math.min(maxBarW, rawBarW));
        int totalBarsW = series.length * barW + Math.max(0, series.length - 1) * seriesGap;
        return new BarLayout(chartX, chartW, groupW, barW, seriesGap, totalBarsW);
    }

    private int singleSeriesGroupWidth() {
        int labelAwareWidth = longestCategoryLineLength() * 5 + 34;
        return Math.max(72, Math.min(84, labelAwareWidth));
    }

    private boolean hasChartData() {
        if (categories.length == 0 || series.length == 0) {
            return false;
        }

        for (Series item : series) {
            for (int value : item.values) {
                if (value > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    private void drawEmptyState(Graphics2D g2, int width, int height) {
        GraphLayout layout = graphLayout(width, height);
        int boxWidth = Math.min(360, Math.max(220, layout.plotW() - 32));
        int boxHeight = 124;
        int x = layout.plotX() + (layout.plotW() - boxWidth) / 2;
        int y = layout.plotY() + Math.max(0, (layout.plotH() - boxHeight) / 2);

        g2.setColor(new Color(248, 250, 252));
        g2.fillRoundRect(x, y, boxWidth, boxHeight, 10, 10);
        g2.setColor(new Color(226, 232, 240));
        g2.drawRoundRect(x, y, boxWidth, boxHeight, 10, 10);

        int iconX = x + boxWidth / 2 - 18;
        int iconY = y + 18;
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(iconX, iconY, 36, 36, 10, 10);
        g2.setColor(new Color(226, 232, 240));
        g2.drawRoundRect(iconX, iconY, 36, 36, 10, 10);
        g2.setColor(new Color(148, 163, 184));
        g2.fillRoundRect(iconX + 10, iconY + 22, 4, 7, 3, 3);
        g2.fillRoundRect(iconX + 16, iconY + 16, 4, 13, 3, 3);
        g2.fillRoundRect(iconX + 22, iconY + 10, 4, 19, 3, 3);

        g2.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        FontMetrics titleMetrics = g2.getFontMetrics();
        String emptyTitle = truncateToWidth(g2, "No data available", boxWidth - 32);
        g2.setColor(HomeViewHelper.TEXT_PRIMARY);
        g2.drawString(emptyTitle, x + (boxWidth - titleMetrics.stringWidth(emptyTitle)) / 2, y + 74);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        FontMetrics subtitleMetrics = g2.getFontMetrics();
        String emptySubtitle = truncateToWidth(g2, "This graph will update once records are available.", boxWidth - 32);
        g2.setColor(HomeViewHelper.TEXT_SECONDARY);
        g2.drawString(emptySubtitle, x + (boxWidth - subtitleMetrics.stringWidth(emptySubtitle)) / 2, y + 96);
    }

    private void drawCategoryLabel(Graphics2D g2, String label, int centerX, int startY, int maxWidth) {
        String[] lines = label.split("\\R", -1);
        Font originalFont = g2.getFont();
        g2.setFont(new Font("Segoe UI", Font.PLAIN, lines.length > 1 ? 10 : 11));
        FontMetrics labelMetrics = g2.getFontMetrics();
        for (int i = 0; i < lines.length; i++) {
            String line = truncateToWidth(g2, lines[i], maxWidth);
            g2.drawString(line, centerX - labelMetrics.stringWidth(line) / 2, startY + i * 13);
        }
        g2.setFont(originalFont);
    }

    private void drawTruncatedString(Graphics2D g2, String text, int x, int y, int maxWidth) {
        g2.drawString(truncateToWidth(g2, text, maxWidth), x, y);
    }

    private String truncateToWidth(Graphics2D g2, String text, int maxWidth) {
        String value = text == null ? "" : text;
        if (maxWidth <= 0 || g2.getFontMetrics().stringWidth(value) <= maxWidth) {
            return value;
        }

        String ellipsis = "...";
        FontMetrics metrics = g2.getFontMetrics();
        int ellipsisWidth = metrics.stringWidth(ellipsis);
        int end = value.length();
        while (end > 0 && metrics.stringWidth(value.substring(0, end)) + ellipsisWidth > maxWidth) {
            end--;
        }
        return end <= 0 ? ellipsis : value.substring(0, end).trim() + ellipsis;
    }

    protected int plotTopInset() {
        return 86;
    }

    private int niceMax() {
        int max = 1;
        for (Series item : series) {
            for (int value : item.values) {
                max = Math.max(max, value);
            }
        }
        int padded = (int) Math.ceil(max * 1.2);
        int interval = padded <= 20 ? 4 : 10;
        return Math.max(interval, ((padded + interval - 1) / interval) * interval);
    }

    private int preferredGraphWidth() {
        int categoryWidth = series.length > 1
                ? MULTI_SERIES_GROUP_WIDTH
                : Math.max(88, Math.min(132, longestCategoryLineLength() * 6 + 28));
        return Math.max(360, PLOT_LEFT_PADDING + PLOT_RIGHT_PADDING + Math.max(1, categories.length) * categoryWidth);
    }

    private int longestCategoryLineLength() {
        int longest = 8;
        for (String category : categories) {
            if (category == null || category.isBlank()) {
                continue;
            }
            for (String line : category.split("\\R")) {
                longest = Math.max(longest, line.trim().length());
            }
        }
        return longest;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 96;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        Container parent = getParent();
        if (parent instanceof JViewport viewport) {
            return getPreferredSize().width <= viewport.getWidth();
        }
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        Container parent = getParent();
        if (parent instanceof JViewport viewport) {
            return getPreferredSize().height <= viewport.getHeight();
        }
        return false;
    }

    private record ScaleConfig(int max, double exponent, int minVisibleBarHeight) {
    }

    private record GraphLayout(
            int plotX,
            int plotY,
            int plotW,
            int plotH,
            int baseY,
            int max,
            double scaleExponent,
            int minVisibleBarHeight
    ) {
    }

    private record BarLayout(int chartX, int chartW, int groupW, int barW, int seriesGap, int totalBarsW) {
    }

    private record HoverBar(int categoryIndex, int seriesIndex) {
    }

    public static class Series {
        private final String name;
        private final int[] values;
        private final Color start;
        private final Color end;

        public Series(String name, int[] values, Color start, Color end) {
            this.name = name;
            this.values = values == null ? new int[0] : values.clone();
            this.start = start;
            this.end = end;
        }
    }

    @FunctionalInterface
    public interface CategorySelectionListener {
        void categorySelected(String category);
    }
}
