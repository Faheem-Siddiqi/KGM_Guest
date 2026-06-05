package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

public final class ButtonStateHelper {
    private static final String RADIUS_PROPERTY = "kgm.button.radius";
    private static final String HOVER_BACKGROUND_PROPERTY = "kgm.button.hoverBackground";
    private static final String PRESSED_BACKGROUND_PROPERTY = "kgm.button.pressedBackground";
    private static final Color DISABLED_BACKGROUND = new Color(148, 163, 184);

    private ButtonStateHelper() {
    }

    public static void installRounded(AbstractButton button, int radius) {
        button.putClientProperty(RADIUS_PROPERTY, radius);
        button.setRolloverEnabled(true);
        button.setUI(new RoundedButtonUI());
    }

    public static void setHoverBackground(AbstractButton button, Color hoverBackground, Color pressedBackground) {
        button.putClientProperty(HOVER_BACKGROUND_PROPERTY, hoverBackground);
        button.putClientProperty(PRESSED_BACKGROUND_PROPERTY, pressedBackground);
        button.repaint();
    }

    private static Color resolveBackground(AbstractButton button) {
        ButtonModel model = button.getModel();
        if (!model.isEnabled()) {
            return DISABLED_BACKGROUND;
        }
        if (model.isPressed() || model.isArmed()) {
            return getColor(button, PRESSED_BACKGROUND_PROPERTY, button.getBackground());
        }
        if (model.isRollover()) {
            return getColor(button, HOVER_BACKGROUND_PROPERTY, button.getBackground());
        }
        return button.getBackground();
    }

    private static Color getColor(AbstractButton button, String property, Color fallback) {
        Object value = button.getClientProperty(property);
        return value instanceof Color ? (Color) value : fallback;
    }

    private static int getRadius(AbstractButton button) {
        Object value = button.getClientProperty(RADIUS_PROPERTY);
        return value instanceof Integer ? (Integer) value : 6;
    }

    private static final class RoundedButtonUI extends BasicButtonUI {
        @Override
        public void installUI(JComponent component) {
            super.installUI(component);
            component.setOpaque(false);
        }

        @Override
        public void paint(Graphics graphics, JComponent component) {
            AbstractButton button = (AbstractButton) component;
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(resolveBackground(button));
            int radius = getRadius(button);
            g2.fillRoundRect(0, 0, component.getWidth(), component.getHeight(), radius, radius);
            g2.dispose();
            super.paint(graphics, component);
        }

        @Override
        protected void paintButtonPressed(Graphics graphics, AbstractButton button) {
            // Background state is painted in paint() so pressed text/icon layout remains unchanged.
        }
    }
}
