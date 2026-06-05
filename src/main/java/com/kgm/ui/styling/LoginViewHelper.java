package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public final class LoginViewHelper {
    private static final String LOGIN_BACKGROUND_PATH = "images/LoginBG.png";
    private static final String LOGIN_LOGO_PATH = "images/LoginTransparent.png";
    private static final String LOGIN_IMAGE_CREDIT = "Made with \u2665 by Faheem Siddiqi";
    private static final int WINDOW_WIDTH = 920;
    private static final int WINDOW_HEIGHT = 640;
    private static final int FORM_WIDTH = 300;
    private static final int FORM_HEIGHT = 344;
    private static final int FIELD_HEIGHT = 42;
    private static final int BUTTON_HEIGHT = 44;
    private static final int LOGO_MARGIN = 24;
    private static final int LOGO_WIDTH = 50;
    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color TEXT_PRIMARY = HomeViewHelper.TEXT_PRIMARY;
    private static final Color TEXT_SECONDARY = HomeViewHelper.TEXT_SECONDARY;
    private static final Color BORDER = HomeViewHelper.BORDER;
    private static final Color FOCUS_BORDER = HomeViewHelper.PRIMARY;
    private static final Color PRIMARY = HomeViewHelper.PRIMARY;
    private static final Color PRIMARY_HOVER = HomeViewHelper.PRIMARY_DARK;

    private LoginViewHelper() {
    }

    public static void applyFrame(JFrame frame) {
        frame.setTitle("KGM Login");
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setMinimumSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(PAGE_BACKGROUND);
    }

    public static JPanel createRootPanel() {
        JPanel root = new JPanel(new GridLayout(1, 2, 0, 0));
        root.setBackground(PAGE_BACKGROUND);
        return root;
    }

    public static JPanel createImagePanel() {
        return new ImagePanel(LOGIN_BACKGROUND_PATH, LOGIN_LOGO_PATH);
    }

    public static JPanel createOuterPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(PAGE_BACKGROUND);
        outer.setBorder(new EmptyBorder(40, 54, 40, 54));
        return outer;
    }

    public static JPanel createFormPanel() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(FORM_WIDTH, FORM_HEIGHT));
        form.setMaximumSize(new Dimension(FORM_WIDTH, FORM_HEIGHT));
        return form;
    }

    public static JLabel createEyebrowLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setForeground(PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JLabel createWelcomeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 32));
        label.setForeground(TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JLabel createSubtitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JTextField createTextField(String placeholder) {
        JTextField field = new PlaceholderTextField(placeholder);
        styleField(field);
        return field;
    }

    public static JPasswordField createPasswordField(String placeholder) {
        JPasswordField field = new PlaceholderPasswordField(placeholder);
        styleField(field);
        return field;
    }

    public static JButton createPrimaryButton(String text) {
        JButton button = new PrimaryButton(text);
        styleButton(button);
        return button;
    }

    public static JPanel createFieldBlock(String labelText, JTextField field) {
        JPanel block = new JPanel();
        block.setOpaque(false);
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setPreferredSize(new Dimension(FORM_WIDTH, 68));
        block.setMaximumSize(new Dimension(FORM_WIDTH, 68));

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setForeground(new Color(64, 76, 92));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(label);
        block.add(Box.createVerticalStrut(8));
        block.add(field);
        return block;
    }

    private static void styleField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(PRIMARY);
        field.setOpaque(false);
        field.setBackground(PAGE_BACKGROUND);
        field.setBorder(createFieldBorder(BORDER));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(createFieldBorder(FOCUS_BORDER));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(createFieldBorder(BORDER));
            }
        });
        field.setPreferredSize(new Dimension(FORM_WIDTH, FIELD_HEIGHT));
        field.setMinimumSize(new Dimension(FORM_WIDTH, FIELD_HEIGHT));
        field.setMaximumSize(new Dimension(FORM_WIDTH, FIELD_HEIGHT));
    }

    private static CompoundBorder createFieldBorder(Color color) {
        return new CompoundBorder(
                new RoundedBorder(8, color),
                new EmptyBorder(0, 16, 0, 16));
    }

    private static void styleButton(JButton button) {
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 15));
        button.setBorder(new EmptyBorder(0, 16, 0, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setPreferredSize(new Dimension(FORM_WIDTH, BUTTON_HEIGHT));
        button.setMinimumSize(new Dimension(FORM_WIDTH, BUTTON_HEIGHT));
        button.setMaximumSize(new Dimension(FORM_WIDTH, BUTTON_HEIGHT));
    }

    private static class ImagePanel extends JPanel {
        private final Image image;
        private final Image logo;
        private final int logoWidth;
        private final int logoHeight;

        ImagePanel(String imagePath, String logoPath) {
            ImageIcon icon = new ImageIcon(imagePath);
            image = icon.getIconWidth() > 0 ? icon.getImage() : null;

            ImageIcon logoIcon = new ImageIcon(logoPath);
            logo = logoIcon.getIconWidth() > 0 ? logoIcon.getImage() : null;
            logoWidth = logo != null ? LOGO_WIDTH : 0;
            logoHeight = logo != null
                    ? Math.round(LOGO_WIDTH * (logoIcon.getIconHeight() / (float) logoIcon.getIconWidth()))
                    : 0;
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            int width = getWidth();
            int height = getHeight();
            if (image != null) {
                graphics.drawImage(image, 0, 0, this);
            } else {
                Graphics2D g2 = (Graphics2D) graphics.create();
                GradientPaint fallback = new GradientPaint(
                        0,
                        0,
                        HomeViewHelper.PRIMARY_LIGHT,
                        width,
                        height,
                        HomeViewHelper.PRIMARY_DARK
                );
                g2.setPaint(fallback);
                g2.fillRect(0, 0, width, height);
                g2.dispose();
            }

            if (logo != null) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.drawImage(logo, LOGO_MARGIN, LOGO_MARGIN, logoWidth, logoHeight, this);
                g2.dispose();
            }

            Graphics2D text = (Graphics2D) graphics.create();
            text.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            text.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            text.setColor(Color.WHITE);
            FontMetrics fm = text.getFontMetrics();
            int textY = getHeight() - LOGO_MARGIN - fm.getDescent();
            text.drawString(LOGIN_IMAGE_CREDIT, LOGO_MARGIN, textY);
            text.dispose();
        }
    }

    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.draw(new RoundRectangle2D.Float(x, y, width - 1, height - 1, radius, radius));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    private static class PrimaryButton extends JButton {
        private boolean hovered;

        PrimaryButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color start = HomeViewHelper.PRIMARY_LIGHT;
            Color end = PRIMARY;
            if (!isEnabled()) {
                start = new Color(148, 163, 184);
                end = new Color(100, 116, 139);
            } else if (isFocusOwner()) {
                start = HomeViewHelper.OCCUPIED_LIGHT;
                end = HomeViewHelper.PRIMARY_DARK;
            } else if (hovered) {
                start = HomeViewHelper.OCCUPIED_LIGHT;
                end = PRIMARY_HOVER;
            }

            g2.setPaint(new GradientPaint(0, 0, start, getWidth(), getHeight(), end));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            if (isFocusOwner()) {
                g2.setColor(new Color(255, 255, 255, 80));
                g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 13, 13);
            }
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class PlaceholderTextField extends JTextField {
        private final String placeholder;

        PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            paintPlaceholder(graphics, getText().isEmpty());
        }

        private void paintPlaceholder(Graphics graphics, boolean visible) {
            if (!visible || isFocusOwner()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(new Color(132, 145, 162));
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(placeholder, getInsets().left, y);
            g2.dispose();
        }
    }

    private static class PlaceholderPasswordField extends JPasswordField {
        private final String placeholder;

        PlaceholderPasswordField(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getPassword().length == 0 && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(132, 145, 162));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(placeholder, getInsets().left, y);
                g2.dispose();
            }
        }
    }
}
