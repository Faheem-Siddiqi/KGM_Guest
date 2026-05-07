package com.kgm.ui.panel;

import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;

public class UniversalGraphPanel extends JPanel {
    private final String title;
    private final String subtitle;
    private String[] categories;
    private Series[] series;

    public UniversalGraphPanel(String title, String subtitle, String[] categories, Series... series) {
        this.title = title;
        this.subtitle = subtitle;
        this.categories = categories.clone();
        this.series = series.clone();
        setOpaque(false);
        setPreferredSize(new Dimension(preferredGraphWidth(), 340));
        setMinimumSize(new Dimension(320, 300));
    }

    public void setGraphData(String[] categories, Series... series) {
        this.categories = categories.clone();
        this.series = series.clone();
        setPreferredSize(new Dimension(preferredGraphWidth(), 340));
        revalidate();
        repaint();
    }

    public boolean needsHorizontalScroll() {
        return categories.length > 5;
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int radius = 16;

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, width - 1, height - 1, radius, radius);
        g2.setColor(HomeViewHelper.BORDER);
        g2.drawRoundRect(0, 0, width - 1, height - 1, radius, radius);

        drawHeader(g2, width);
        drawBars(g2, width, height);

        g2.dispose();
    }

    private void drawHeader(Graphics2D g2, int width) {
        g2.setColor(HomeViewHelper.TEXT_PRIMARY);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 17));
        g2.drawString(title, 22, 30);

        g2.setColor(HomeViewHelper.TEXT_SECONDARY);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.drawString(subtitle, 22, 50);

        int legendX = width - 22;
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        for (int i = series.length - 1; i >= 0; i--) {
            Series item = series[i];
            FontMetrics metrics = g2.getFontMetrics();
            int textWidth = metrics.stringWidth(item.name);
            legendX -= textWidth + 24;
            GradientPaint paint = new GradientPaint(legendX, 18, item.start, legendX + 12, 30, item.end);
            g2.setPaint(paint);
            g2.fillRoundRect(legendX, 18, 12, 12, 6, 6);
            g2.setColor(HomeViewHelper.TEXT_SECONDARY);
            g2.drawString(item.name, legendX + 18, 29);
            legendX -= 14;
        }
    }

    private void drawBars(Graphics2D g2, int width, int height) {
        int plotX = 52;
        int plotY = plotTopInset();
        int plotW = Math.max(120, width - 80);
        int plotH = Math.max(120, height - plotY - 56);
        int baseY = plotY + plotH;
        int max = niceMax();

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        FontMetrics metrics = g2.getFontMetrics();

        for (int step = 0; step <= 4; step++) {
            int y = baseY - (plotH * step / 4);
            int value = max * step / 4;
            g2.setColor(new Color(232, 236, 240));
            g2.drawLine(plotX, y, plotX + plotW, y);
            g2.setColor(HomeViewHelper.TEXT_SECONDARY);
            String label = String.valueOf(value);
            g2.drawString(label, plotX - metrics.stringWidth(label) - 8, y + 4);
        }

        if (categories.length == 0 || series.length == 0) {
            return;
        }

        int groupW = plotW / categories.length;
        int seriesGap = 4;
        int barW = Math.max(8, Math.min(28, (groupW - 18) / series.length - seriesGap));
        int totalBarsW = series.length * barW + Math.max(0, series.length - 1) * seriesGap;

        for (int categoryIndex = 0; categoryIndex < categories.length; categoryIndex++) {
            int groupStart = plotX + categoryIndex * groupW + (groupW - totalBarsW) / 2;

            for (int seriesIndex = 0; seriesIndex < series.length; seriesIndex++) {
                Series item = series[seriesIndex];
                int value = categoryIndex < item.values.length ? item.values[categoryIndex] : 0;
                int barH = (int) ((value / (double) max) * plotH);
                int x = groupStart + seriesIndex * (barW + seriesGap);
                int y = baseY - barH;

                g2.setPaint(new GradientPaint(x, y, item.start, x, baseY, item.end));
                g2.fillRoundRect(x, y, barW, barH, 8, 8);
            }

            String label = categories[categoryIndex];
            g2.setColor(HomeViewHelper.TEXT_SECONDARY);
            drawCategoryLabel(g2, label, plotX + categoryIndex * groupW + groupW / 2, baseY + 18);
        }
    }

    private void drawCategoryLabel(Graphics2D g2, String label, int centerX, int startY) {
        String[] lines = label.split("\\R", -1);
        Font originalFont = g2.getFont();
        g2.setFont(new Font("Segoe UI", Font.PLAIN, lines.length > 1 ? 10 : 11));
        FontMetrics labelMetrics = g2.getFontMetrics();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            g2.drawString(line, centerX - labelMetrics.stringWidth(line) / 2, startY + i * 13);
        }
        g2.setFont(originalFont);
    }

    protected int plotTopInset() {
        return 92;
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
        int categoryWidth = series.length > 1 ? 92 : 78;
        return Math.max(360, 110 + Math.max(1, categories.length) * categoryWidth);
    }

    public static class Series {
        private final String name;
        private final int[] values;
        private final Color start;
        private final Color end;

        public Series(String name, int[] values, Color start, Color end) {
            this.name = name;
            this.values = values.clone();
            this.start = start;
            this.end = end;
        }
    }
}
