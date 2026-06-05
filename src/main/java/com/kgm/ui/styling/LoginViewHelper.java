package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

public final class LoginViewHelper {
    private static final String LOGIN_BACKGROUND_PATH = "images/LoginBG.png";
    private static final String LOGIN_LOGO_PATH = "images/LoginTransparent.png";
    private static final String LOGIN_IMAGE_CREDIT = "Made with \u2665 by Faheem Siddiqi";
    private static final int WINDOW_WIDTH = 1040;
    private static final int WINDOW_HEIGHT = 680;
    private static final int FORM_WIDTH = 340;
    private static final int FIELD_HEIGHT = 40;
    private static final int BUTTON_HEIGHT = 40;
    private static final int LOGO_MARGIN = 30;
    private static final int LOGO_WIDTH = 55;
    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(15, 23, 42);
    private static final Color HEADING_GREY = new Color(17, 24, 39);
    private static final Color TEXT_SECONDARY = new Color(100, 116, 139);
    private static final Color LABEL_TEXT = new Color(51, 65, 85);
    private static final Color PLACEHOLDER_TEXT = new Color(148, 163, 184);
    private static final Color FIELD_BACKGROUND = new Color(248, 250, 252);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color FOCUS_BORDER = new Color(37, 99, 235);
    private static final Color PRIMARY = new Color(37, 99, 235);
    private static final Color PRIMARY_HOVER = new Color(29, 78, 216);
    private static final Color PRIMARY_PRESSED = new Color(30, 64, 175);

    private LoginViewHelper() {
    }

    public static void applyFrame(JFrame frame) {
        frame.setTitle("KGM Login");
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setMinimumSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
        outer.setBorder(new EmptyBorder(48, 60, 48, 60));
        return outer;
    }

    public static JPanel createFormPanel() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(FORM_WIDTH, 360));
        form.setMaximumSize(new Dimension(FORM_WIDTH, 360));
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
        label.setFont(new Font("Segoe UI", Font.BOLD, 30));
        label.setForeground(HEADING_GREY);
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
        JButton button = new JButton(text);
        styleButton(button);
        return button;
    }

    public static JPanel createFieldBlock(String labelText, JTextField field) {
        JPanel block = new JPanel();
        block.setOpaque(false);
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setPreferredSize(new Dimension(FORM_WIDTH, 64));
        block.setMaximumSize(new Dimension(FORM_WIDTH, 64));

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setForeground(LABEL_TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(label);
        block.add(Box.createVerticalStrut(7));
        block.add(field);
        return block;
    }

    private static void styleField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(PRIMARY);
        field.setOpaque(true);
        field.setBackground(FIELD_BACKGROUND);
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
                new RoundedBorder(6, color),
                new EmptyBorder(0, 12, 0, 12));
    }

    private static void styleButton(JButton button) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setForeground(Color.WHITE);
        button.setBackground(PRIMARY);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        button.setBorder(new EmptyBorder(0, 14, 0, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setPreferredSize(new Dimension(FORM_WIDTH, BUTTON_HEIGHT));
        button.setMinimumSize(new Dimension(FORM_WIDTH, BUTTON_HEIGHT));
        button.setMaximumSize(new Dimension(FORM_WIDTH, BUTTON_HEIGHT));
        ButtonStateHelper.installRounded(button, 6);
        ButtonStateHelper.setHoverBackground(button, PRIMARY_HOVER, PRIMARY_PRESSED);
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
            setBackground(PAGE_BACKGROUND);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int width = getWidth();
            int height = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (image != null) {
                paintCoverImage(g2, image, width, height);
            } else {
                GradientPaint fallback = new GradientPaint(0, 0, PRIMARY, width, height, new Color(11, 31, 55));
                g2.setPaint(fallback);
                g2.fillRect(0, 0, width, height);
            }
            g2.setPaint(new GradientPaint(
                    0,
                    0,
                    new Color(15, 23, 42, 18),
                    width,
                    height,
                    new Color(15, 23, 42, 118)
            ));
            g2.fillRect(0, 0, width, height);
            g2.dispose();

            if (logo != null) {
                Graphics2D logoGraphics = (Graphics2D) g.create();
                logoGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                logoGraphics.drawImage(logo, LOGO_MARGIN, LOGO_MARGIN, logoWidth, logoHeight, this);
                logoGraphics.dispose();
            }

            Graphics2D text = (Graphics2D) g.create();
            text.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            text.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            text.setColor(Color.WHITE);
            FontMetrics fm = text.getFontMetrics();
            int textY = getHeight() - LOGO_MARGIN - fm.getDescent();
            text.drawString(LOGIN_IMAGE_CREDIT, LOGO_MARGIN, textY);
            text.dispose();
        }

        private void paintCoverImage(Graphics2D graphics, Image source, int width, int height) {
            int sourceWidth = source.getWidth(this);
            int sourceHeight = source.getHeight(this);
            if (sourceWidth <= 0 || sourceHeight <= 0 || width <= 0 || height <= 0) {
                return;
            }

            double scale = Math.max(width / (double) sourceWidth, height / (double) sourceHeight);
            int drawWidth = (int) Math.round(sourceWidth * scale);
            int drawHeight = (int) Math.round(sourceHeight * scale);
            int x = (width - drawWidth) / 2;
            int y = (height - drawHeight) / 2;
            graphics.drawImage(source, x, y, drawWidth, drawHeight, this);
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
            g2.setColor(PLACEHOLDER_TEXT);
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
                g2.setColor(PLACEHOLDER_TEXT);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(placeholder, getInsets().left, y);
                g2.dispose();
            }
        }
    }
}
