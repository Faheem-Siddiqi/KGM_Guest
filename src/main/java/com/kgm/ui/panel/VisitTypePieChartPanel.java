package com.kgm.ui.panel;

import com.kgm.dao.DashboardDao;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;

public class VisitTypePieChartPanel extends JPanel implements Scrollable {
    private static final int GRAPH_HEIGHT = 400;
    private static final int CARD_PADDING_X = 24;
    private static final int HEADER_TITLE_Y = 30;
    private static final int HEADER_SUBTITLE_Y = 51;
    private static final int CHART_TOP = 86;
    private static final int LEGEND_ROW_HEIGHT = 24;
    private static final int PIE_MAX_DIAMETER = 230;
    private static final int PIE_MIN_DIAMETER = 170;
    private static final int PIE_SLICE_ALPHA = 220;
    private static final int PIE_SLICE_HOVER_ALPHA = 245;
    private static final int PIE_SLICE_MUTED_ALPHA = 118;
    private static final int PIE_LEGEND_HOVER_ALPHA = 18;
    private static final int PIE_LEGEND_DOT_SIZE = 10;
    private static final int PIE_LEGEND_DOT_GAP = 6;
    private static final int PIE_LEGEND_VALUE_GAP = 8;
    private static final double SLICE_GAP_DEGREES = 0.35;
    private static final Color[] SLICE_COLORS = {
            HomeViewHelper.TEAL,
            HomeViewHelper.BLUE,
            HomeViewHelper.PURPLE,
            new Color(34, 197, 94),
            new Color(245, 158, 11),
            new Color(14, 165, 233),
            new Color(99, 102, 241),
            new Color(236, 72, 153)
    };

    private final DashboardDao.BreakdownChartData data;
    private int hoveredIndex = -1;
    private SliceSelectionListener sliceSelectionListener;

    public VisitTypePieChartPanel(DashboardDao.BreakdownChartData data) {
        this.data = data == null
                ? new DashboardDao.BreakdownChartData(new String[0], new int[0])
                : data;
        setOpaque(false);
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(520, preferredGraphHeight()));
        setMinimumSize(new Dimension(320, 300));
        installHoverCursor();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int width = getWidth();
        int height = getHeight();
        drawHeader(g2, width);

        int total = totalValue();
        if (total <= 0 || data.labels().length == 0) {
            drawEmptyState(g2, width, height);
            g2.dispose();
            return;
        }

        PieLayout layout = pieLayout(width, height);
        drawPie(g2, layout, total);
        drawLegend(g2, layout, total);
        drawHoverSummary(g2, width, height, total);
        g2.dispose();
    }

    private void installHoverCursor() {
        addMouseMotionListener(new MouseAdapter() {
            public void mouseMoved(MouseEvent event) {
                int nextHover = itemAt(event.getPoint());
                if (nextHover != hoveredIndex) {
                    hoveredIndex = nextHover;
                    repaint();
                }
                setCursor(Cursor.getPredefinedCursor(nextHover >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                int selectedIndex = itemAt(event.getPoint());
                if (selectedIndex >= 0
                        && selectedIndex < data.labels().length
                        && sliceSelectionListener != null) {
                    sliceSelectionListener.sliceSelected(data.labels()[selectedIndex]);
                }
            }

            public void mouseExited(MouseEvent event) {
                hoveredIndex = -1;
                repaint();
                setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    public void setSliceSelectionListener(SliceSelectionListener sliceSelectionListener) {
        this.sliceSelectionListener = sliceSelectionListener;
    }

    private void drawHeader(Graphics2D g2, int width) {
        g2.setColor(HomeViewHelper.TEXT_PRIMARY);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
        drawTruncatedString(g2, "Visit Type Mix", CARD_PADDING_X, HEADER_TITLE_Y, width - CARD_PADDING_X * 2);

        g2.setColor(HomeViewHelper.TEXT_SECONDARY);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        drawTruncatedString(g2, "Guest records grouped by visit purpose", CARD_PADDING_X, HEADER_SUBTITLE_Y, width - CARD_PADDING_X * 2);
    }

    private void drawPie(Graphics2D g2, PieLayout layout, int total) {
        double startAngle = 90.0;
        Rectangle bounds = layout.pieBounds();

        for (int index = 0; index < data.values().length; index++) {
            int value = Math.max(0, data.values()[index]);
            if (value <= 0) {
                continue;
            }
            double sweep = value * 360.0 / total;
            double gap = sweep > SLICE_GAP_DEGREES * 3 ? SLICE_GAP_DEGREES : 0;
            double sliceStart = startAngle - gap / 2.0;
            double visibleSweep = Math.max(0.6, sweep - gap);
            Rectangle sliceBounds = sliceBounds(bounds, startAngle, sweep, index == hoveredIndex);
            Arc2D.Double slice = new Arc2D.Double(
                    sliceBounds.x,
                    sliceBounds.y,
                    sliceBounds.width,
                    sliceBounds.height,
                    sliceStart,
                    -visibleSweep,
                    Arc2D.PIE
            );
            boolean hovered = index == hoveredIndex;
            if (hovered) {
                g2.setColor(new Color(15, 23, 42, 24));
                g2.fill(shadowSlice(slice));
            }
            g2.setColor(slicePaint(index, hovered));
            g2.fill(slice);
            g2.setColor(new Color(255, 255, 255, hovered ? 245 : 220));
            g2.setStroke(new BasicStroke(hovered ? 1.45f : 0.75f));
            g2.draw(slice);
            startAngle -= sweep;
        }

        int innerDiameter = layout.innerDiameter();
        int innerX = bounds.x + (bounds.width - innerDiameter) / 2;
        int innerY = bounds.y + (bounds.height - innerDiameter) / 2;
        g2.setColor(Color.WHITE);
        g2.fillOval(innerX, innerY, innerDiameter, innerDiameter);
        g2.setStroke(new BasicStroke(0.6f));
        g2.setColor(new Color(226, 232, 240, 130));
        g2.drawOval(innerX, innerY, innerDiameter, innerDiameter);

        int centerValue = hoveredSliceValue(total);
        String totalText = String.valueOf(centerValue);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
        FontMetrics totalMetrics = g2.getFontMetrics();
        g2.setColor(HomeViewHelper.TEXT_PRIMARY);
        g2.drawString(
                totalText,
                (int) bounds.getCenterX() - totalMetrics.stringWidth(totalText) / 2,
                (int) bounds.getCenterY() + 2
        );

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        FontMetrics labelMetrics = g2.getFontMetrics();
        g2.setColor(HomeViewHelper.TEXT_SECONDARY);
        String centerLabel = hoveredIndex >= 0 ? percentText(centerValue, total) : "Total";
        g2.drawString(
                centerLabel,
                (int) bounds.getCenterX() - labelMetrics.stringWidth(centerLabel) / 2,
                (int) bounds.getCenterY() + 19
        );

    }

    private void drawLegend(Graphics2D g2, PieLayout layout, int total) {
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        FontMetrics metrics = g2.getFontMetrics();
        int rowY = layout.legendY();
        int maxTextWidth = Math.max(80, layout.legendWidth() - 126);

        for (int index = 0; index < data.labels().length; index++) {
            int value = index < data.values().length ? Math.max(0, data.values()[index]) : 0;
            if (value <= 0) {
                continue;
            }
            boolean hovered = index == hoveredIndex;
            if (hovered) {
                g2.setColor(withAlpha(colorFor(index), PIE_LEGEND_HOVER_ALPHA));
                g2.fillRoundRect(layout.legendX() - 8, rowY - 14, layout.legendWidth(), 22, 8, 8);
            }

            g2.setColor(hovered ? hoverColor(colorFor(index)) : colorFor(index));
            g2.fillOval(layout.legendX(), rowY - 10, PIE_LEGEND_DOT_SIZE, PIE_LEGEND_DOT_SIZE);

            g2.setFont(new Font("Segoe UI", hovered ? Font.BOLD : Font.PLAIN, 12));
            metrics = g2.getFontMetrics();
            g2.setColor(HomeViewHelper.TEXT_PRIMARY);
            String label = truncated(data.labels()[index], metrics, maxTextWidth);
            g2.drawString(label, layout.legendX() + PIE_LEGEND_DOT_SIZE + PIE_LEGEND_DOT_GAP, rowY);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            FontMetrics valueMetrics = g2.getFontMetrics();
            g2.setColor(hovered ? HomeViewHelper.TEXT_PRIMARY : HomeViewHelper.TEXT_SECONDARY);
            String valueText = value + " - " + percentText(value, total);
            g2.drawString(
                    valueText,
                    layout.legendX() + layout.legendWidth() - PIE_LEGEND_VALUE_GAP - valueMetrics.stringWidth(valueText),
                    rowY
            );
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            metrics = g2.getFontMetrics();
            rowY += LEGEND_ROW_HEIGHT;
        }
    }

    private void drawHoverSummary(Graphics2D g2, int width, int height, int total) {
        if (hoveredIndex < 0 || hoveredIndex >= data.labels().length || hoveredIndex >= data.values().length) {
            return;
        }
        int value = Math.max(0, data.values()[hoveredIndex]);
        String title = data.labels()[hoveredIndex];
        String detail = value + " guest records | " + String.format("%.1f", value * 100.0 / total) + "% of visit mix";

        int boxWidth = Math.min(320, Math.max(220, width - CARD_PADDING_X * 2));
        int boxHeight = 58;
        Point mouse = getMousePosition();
        int x = mouse == null ? (width - boxWidth) / 2 : mouse.x + 16;
        int y = mouse == null ? Math.max(CHART_TOP + 12, height - boxHeight - 20) : mouse.y - boxHeight - 14;
        x = clamp(x, 12, Math.max(12, width - boxWidth - 12));
        y = clamp(y, 62, Math.max(62, height - boxHeight - 12));

        g2.setColor(new Color(15, 23, 42, 22));
        g2.fillRoundRect(x + 2, y + 3, boxWidth, boxHeight, 12, 12);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(x, y, boxWidth, boxHeight, 12, 12);
        g2.setColor(new Color(226, 232, 240));
        g2.drawRoundRect(x, y, boxWidth, boxHeight, 12, 12);

        g2.setColor(hoverColor(colorFor(hoveredIndex)));
        g2.fillRoundRect(x + 14, y + 16, 12, 12, 4, 4);

        g2.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        g2.setColor(HomeViewHelper.TEXT_PRIMARY);
        drawTruncatedString(g2, title, x + 34, y + 22, boxWidth - 48);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(HomeViewHelper.TEXT_SECONDARY);
        drawTruncatedString(g2, detail, x + 34, y + 40, boxWidth - 48);
    }

    private void drawEmptyState(Graphics2D g2, int width, int height) {
        g2.setColor(new Color(248, 250, 252));
        int boxWidth = Math.min(280, Math.max(180, width - CARD_PADDING_X * 2));
        int x = (width - boxWidth) / 2;
        int y = Math.max(CHART_TOP, height / 2 - 30);
        g2.fillRoundRect(x, y, boxWidth, 58, 12, 12);
        g2.setColor(HomeViewHelper.TEXT_SECONDARY);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        String text = "No visit type records available";
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(text, (width - metrics.stringWidth(text)) / 2, y + 35);
    }

    private int itemAt(Point point) {
        int sliceIndex = sliceAt(point);
        return sliceIndex >= 0 ? sliceIndex : legendIndexAt(point);
    }

    private int sliceAt(Point point) {
        if (point == null || totalValue() <= 0) {
            return -1;
        }
        PieLayout layout = pieLayout(getWidth(), getHeight());
        Rectangle bounds = layout.pieBounds();
        double centerX = bounds.getCenterX();
        double centerY = bounds.getCenterY();
        double dx = point.x - centerX;
        double dy = point.y - centerY;
        double radius = bounds.width / 2.0;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance > radius || distance < layout.innerDiameter() / 2.0) {
            return -1;
        }

        double angle = normalizeAngle(Math.toDegrees(Math.atan2(centerY - point.y, point.x - centerX)));
        double startAngle = 90.0;
        int total = totalValue();
        for (int index = 0; index < data.values().length; index++) {
            int value = Math.max(0, data.values()[index]);
            if (value <= 0) {
                continue;
            }
            double sweep = value * 360.0 / total;
            double clockwiseDistance = normalizeAngle(startAngle - angle);
            if (clockwiseDistance <= sweep) {
                return index;
            }
            startAngle -= sweep;
        }
        return -1;
    }

    private int legendIndexAt(Point point) {
        if (point == null || totalValue() <= 0) {
            return -1;
        }
        PieLayout layout = pieLayout(getWidth(), getHeight());
        int rowY = layout.legendY();
        for (int index = 0; index < data.labels().length; index++) {
            int value = index < data.values().length ? Math.max(0, data.values()[index]) : 0;
            if (value <= 0) {
                continue;
            }
            Rectangle rowBounds = new Rectangle(
                    layout.legendX() - 8,
                    rowY - 15,
                    layout.legendWidth(),
                    23
            );
            if (rowBounds.contains(point)) {
                return index;
            }
            rowY += LEGEND_ROW_HEIGHT;
        }
        return -1;
    }

    private Rectangle sliceBounds(Rectangle bounds, double startAngle, double sweep, boolean expanded) {
        if (!expanded) {
            return bounds;
        }
        double midAngle = Math.toRadians(startAngle - sweep / 2.0);
        int offsetX = (int) Math.round(Math.cos(midAngle) * 8);
        int offsetY = (int) Math.round(-Math.sin(midAngle) * 8);
        return new Rectangle(bounds.x + offsetX - 3, bounds.y + offsetY - 3, bounds.width + 6, bounds.height + 6);
    }

    private Arc2D.Double shadowSlice(Arc2D.Double slice) {
        return new Arc2D.Double(
                slice.x + 1,
                slice.y + 3,
                slice.width,
                slice.height,
                slice.getAngleStart(),
                slice.getAngleExtent(),
                Arc2D.PIE
        );
    }

    private PieLayout pieLayout(int width, int height) {
        boolean wide = width >= 500;
        int usableHeight = Math.max(220, height - CHART_TOP - 78);
        int diameter = Math.min(PIE_MAX_DIAMETER, Math.max(PIE_MIN_DIAMETER, usableHeight));
        diameter = Math.min(diameter, Math.max(PIE_MIN_DIAMETER, wide ? width / 2 - 52 : width - CARD_PADDING_X * 2));

        int pieX;
        int pieY = CHART_TOP + 16;
        int legendX;
        int legendY;
        int legendWidth;
        if (wide) {
            pieX = CARD_PADDING_X + Math.max(0, (width / 2 - diameter) / 2);
            legendX = Math.max(width / 2 + 8, pieX + diameter + 32);
            legendY = CHART_TOP + 50;
            legendWidth = Math.max(150, width - legendX - CARD_PADDING_X);
        } else {
            pieX = Math.max(CARD_PADDING_X, (width - diameter) / 2);
            legendX = CARD_PADDING_X;
            legendY = pieY + diameter + 28;
            legendWidth = width - CARD_PADDING_X * 2;
        }
        int innerDiameter = Math.round(diameter * 0.34f);
        return new PieLayout(new Rectangle(pieX, pieY, diameter, diameter), innerDiameter, legendX, legendY, legendWidth);
    }

    private int totalValue() {
        int total = 0;
        for (int value : data.values()) {
            total += Math.max(0, value);
        }
        return total;
    }

    private int hoveredSliceValue(int total) {
        if (hoveredIndex < 0 || hoveredIndex >= data.values().length) {
            return total;
        }
        return Math.max(0, data.values()[hoveredIndex]);
    }

    private int preferredGraphHeight() {
        int legendRows = Math.max(0, data.labels().length - 5);
        return Math.max(GRAPH_HEIGHT, GRAPH_HEIGHT + legendRows * LEGEND_ROW_HEIGHT);
    }

    private Color colorFor(int index) {
        return SLICE_COLORS[Math.floorMod(index, SLICE_COLORS.length)];
    }

    private Color slicePaint(int index, boolean hovered) {
        Color base = hovered ? hoverColor(colorFor(index)) : colorFor(index);
        int alpha = hovered
                ? PIE_SLICE_HOVER_ALPHA
                : hoveredIndex >= 0 ? PIE_SLICE_MUTED_ALPHA : PIE_SLICE_ALPHA;
        return withAlpha(base, alpha);
    }

    private Color hoverColor(Color color) {
        int r = Math.max(0, color.getRed() - 18);
        int g = Math.max(0, color.getGreen() - 18);
        int b = Math.max(0, color.getBlue() - 18);
        return new Color(r, g, b);
    }

    private Color withAlpha(Color color, int alpha) {
        return new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                Math.max(0, Math.min(255, alpha))
        );
    }

    private String percentText(int value, int total) {
        if (total <= 0) {
            return "0%";
        }
        return Math.round(value * 100.0 / total) + "%";
    }

    private double normalizeAngle(double angle) {
        double normalized = angle % 360.0;
        return normalized < 0 ? normalized + 360.0 : normalized;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void drawTruncatedString(Graphics2D g2, String value, int x, int y, int maxWidth) {
        g2.drawString(truncated(value, g2.getFontMetrics(), maxWidth), x, y);
    }

    private String truncated(String value, FontMetrics metrics, int maxWidth) {
        String text = value == null || value.isBlank() ? "-" : value.trim();
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && metrics.stringWidth(text.substring(0, end)) + metrics.stringWidth(ellipsis) > maxWidth) {
            end--;
        }
        return end <= 0 ? ellipsis : text.substring(0, end).trim() + ellipsis;
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

    private record PieLayout(Rectangle pieBounds, int innerDiameter, int legendX, int legendY, int legendWidth) {
    }

    @FunctionalInterface
    public interface SliceSelectionListener {
        void sliceSelected(String label);
    }
}
