package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public final class ModernScrollBarUI extends BasicScrollBarUI {
    public static final int THICKNESS = 8;

    private static final int TRACK_INSET = 2;
    private static final Color TRACK = new Color(248, 250, 252);
    private static final Color THUMB = new Color(203, 213, 225);
    private static final Color THUMB_HOVER = new Color(148, 163, 184);
    private static final Color THUMB_ACTIVE = new Color(100, 116, 139);
    private static final Dimension ZERO_BUTTON_SIZE = new Dimension(0, 0);

    public static void applyHorizontal(JScrollPane scrollPane) {
        if (scrollPane != null) {
            applyTo(scrollPane.getHorizontalScrollBar());
        }
    }

    public static void applyTo(JScrollBar scrollBar) {
        if (scrollBar == null) {
            return;
        }

        scrollBar.setUI(new ModernScrollBarUI());
        scrollBar.setOpaque(false);
        scrollBar.setFocusable(false);
        scrollBar.setBorder(BorderFactory.createEmptyBorder());
        if (scrollBar.getOrientation() == Adjustable.HORIZONTAL) {
            scrollBar.setPreferredSize(new Dimension(0, THICKNESS));
            scrollBar.setMinimumSize(new Dimension(0, THICKNESS));
            scrollBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, THICKNESS));
        } else {
            scrollBar.setPreferredSize(new Dimension(THICKNESS, 0));
            scrollBar.setMinimumSize(new Dimension(THICKNESS, 0));
            scrollBar.setMaximumSize(new Dimension(THICKNESS, Integer.MAX_VALUE));
        }
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return invisibleButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return invisibleButton();
    }

    @Override
    protected void paintTrack(Graphics graphics, JComponent component, Rectangle trackBounds) {
        if (trackBounds.isEmpty()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(TRACK);
        Rectangle track = slimBounds(trackBounds);
        g2.fillRoundRect(track.x, track.y, track.width, track.height, track.height, track.height);
        g2.dispose();
    }

    @Override
    protected void paintThumb(Graphics graphics, JComponent component, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(thumbColor(thumbBounds));
        Rectangle thumb = slimBounds(thumbBounds);
        g2.fillRoundRect(thumb.x, thumb.y, thumb.width, thumb.height, thumb.height, thumb.height);
        g2.dispose();
    }

    private Color thumbColor(Rectangle thumbBounds) {
        Point mouse = scrollbar.getMousePosition();
        if (isDragging) {
            return THUMB_ACTIVE;
        }
        if (mouse != null && thumbBounds.contains(mouse)) {
            return THUMB_HOVER;
        }
        return THUMB;
    }

    private Rectangle slimBounds(Rectangle bounds) {
        if (scrollbar.getOrientation() == Adjustable.HORIZONTAL) {
            int y = bounds.y + TRACK_INSET;
            int height = Math.max(4, bounds.height - TRACK_INSET * 2);
            return new Rectangle(bounds.x + TRACK_INSET, y, Math.max(4, bounds.width - TRACK_INSET * 2), height);
        }

        int x = bounds.x + TRACK_INSET;
        int width = Math.max(4, bounds.width - TRACK_INSET * 2);
        return new Rectangle(x, bounds.y + TRACK_INSET, width, Math.max(4, bounds.height - TRACK_INSET * 2));
    }

    private JButton invisibleButton() {
        JButton button = new JButton();
        button.setPreferredSize(ZERO_BUTTON_SIZE);
        button.setMinimumSize(ZERO_BUTTON_SIZE);
        button.setMaximumSize(ZERO_BUTTON_SIZE);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusable(false);
        return button;
    }
}
