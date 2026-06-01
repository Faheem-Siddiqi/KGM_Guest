package com.kgm.ui;

import com.kgm.service.AuthService;
import com.kgm.ui.styling.HomeViewHelper;
import com.kgm.util.SessionManager;
import com.kgm.util.SessionWatcher;

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

public class LoginView extends JFrame {
    private static final String LOGIN_BACKGROUND_PATH = "images/LoginBG.png";
    private static final String LOGIN_LOGO_PATH = "images/LoginTransparent.png";
private static final String LOGIN_IMAGE_CREDIT = "Made with ♥ by Faheem Siddiqi";
    private static final int WINDOW_WIDTH = 920;
    private static final int WINDOW_HEIGHT = 640;
    private static final int FORM_WIDTH = 300;
    private static final int FIELD_HEIGHT = 42;
    private static final int BUTTON_HEIGHT = 44;
    private static final int LOGO_MARGIN = 24;
    // Change this width if the login logo needs to be resized later.
    private static final int LOGO_WIDTH = 70;
    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color TEXT_PRIMARY = HomeViewHelper.TEXT_PRIMARY;
  private static final Color HEADING_GREY = HomeViewHelper.TEXT_PRIMARY;
    private static final Color TEXT_SECONDARY = HomeViewHelper.TEXT_SECONDARY;
    private static final Color BORDER = HomeViewHelper.BORDER;
    private static final Color FOCUS_BORDER = HomeViewHelper.PRIMARY;
    private static final Color PRIMARY = HomeViewHelper.PRIMARY;
    private static final Color PRIMARY_HOVER = HomeViewHelper.PRIMARY_DARK;

    public LoginView() {
        setTitle("KGM Login");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setMinimumSize(new Dimension(760, 560));
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(PAGE_BACKGROUND);

        JPanel root = new JPanel(new GridLayout(1, 2, 0, 0));
        root.setBackground(PAGE_BACKGROUND);
        add(root, BorderLayout.CENTER);

        ImagePanel imagePanel = createImagePanel();
        root.add(imagePanel);
        root.add(createLoginPanel());

        setLocationRelativeTo(null);
    }

    private ImagePanel createImagePanel() {
        return new ImagePanel(LOGIN_BACKGROUND_PATH, LOGIN_LOGO_PATH);
    }

    private JPanel createLoginPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(PAGE_BACKGROUND);
        outer.setBorder(new EmptyBorder(40, 54, 40, 54));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(FORM_WIDTH, 344));
        form.setMaximumSize(new Dimension(FORM_WIDTH, 344));

        JLabel eyebrow = new JLabel("KGM GUEST PORTAL: update ");
        eyebrow.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        eyebrow.setForeground(PRIMARY);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel welcome = new JLabel("Welcome Back");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 32));
        welcome.setForeground(HEADING_GREY);
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Sign in to continue to guest management.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField userField = new PlaceholderTextField("Enter username");
        JPasswordField passField = new PlaceholderPasswordField("Enter password");
        styleField(userField);
        styleField(passField);

        JButton loginBtn = new PrimaryButton("Sign In");
        styleButton(loginBtn);

        form.add(eyebrow);
        form.add(Box.createVerticalStrut(14));
        form.add(welcome);
        form.add(Box.createVerticalStrut(4));
        form.add(subtitle);
        form.add(Box.createVerticalStrut(30));
        form.add(createFieldBlock("Username", userField));
        form.add(Box.createVerticalStrut(14));
        form.add(createFieldBlock("Password", passField));
        form.add(Box.createVerticalStrut(20));
        form.add(loginBtn);

        outer.add(form, new GridBagConstraints());
        getRootPane().setDefaultButton(loginBtn);

        loginBtn.addActionListener(e -> {
            String user = userField.getText();
            String pass = new String(passField.getPassword());
            if (AuthService.login(user, pass)) {
                SessionManager.startSession(user);
                SessionWatcher.start();
                SessionWatcher.closeAllWindows();
                new HomeView();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password", "Login Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        return outer;
    }

    private JPanel createFieldBlock(String labelText, JTextField field) {
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

    private void styleField(JTextField field) {
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

    private CompoundBorder createFieldBorder(Color color) {
        return new CompoundBorder(
                new RoundedBorder(8, color),
                new EmptyBorder(0, 16, 0, 16));
    }

    private void styleButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 15));
        btn.setBorder(new EmptyBorder(0, 16, 0, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setPreferredSize(new Dimension(FORM_WIDTH, BUTTON_HEIGHT));
        btn.setMinimumSize(new Dimension(FORM_WIDTH, BUTTON_HEIGHT));
        btn.setMaximumSize(new Dimension(FORM_WIDTH, BUTTON_HEIGHT));
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
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int width = getWidth();
            int height = getHeight();
            if (image != null) {
                g.drawImage(image, 0, 0, this);
            } else {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint fallback = new GradientPaint(0, 0, HomeViewHelper.PRIMARY_LIGHT,
                        width, height, HomeViewHelper.PRIMARY_DARK);
                g2.setPaint(fallback);
                g2.fillRect(0, 0, width, height);
                g2.dispose();
            }

            if (logo != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.drawImage(logo, LOGO_MARGIN, LOGO_MARGIN, logoWidth, logoHeight, this);
                g2.dispose();
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
    }

    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.draw(new RoundRectangle2D.Float(x, y, width - 1, height - 1, radius, radius));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
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
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color start = HomeViewHelper.PRIMARY_LIGHT;
            Color end = PRIMARY;
            if (isFocusOwner()) {
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
            super.paintComponent(g);
        }
    }

    private static class PlaceholderTextField extends JTextField {
        private final String placeholder;

        PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            paintPlaceholder(g, getText().isEmpty());
        }

        protected void paintPlaceholder(Graphics g, boolean visible) {
            if (!visible || isFocusOwner()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
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
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getPassword().length == 0 && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
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
