package com.kgm.ui;

import com.kgm.config.DatabaseConfig;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public class StartupLoadingView extends JFrame {
    private static final int SLOW_CONNECTION_THRESHOLD_MS = 1200;
    private static final Color BACKGROUND = new Color(246, 249, 252);
    private static final Color CARD = Color.WHITE;
    private static final Color BORDER = HomeViewHelper.BORDER;
    private static final Color PRIMARY = HomeViewHelper.PRIMARY;
    private static final Color PRIMARY_DARK = HomeViewHelper.PRIMARY_DARK;
    private static final Color TEXT_PRIMARY = HomeViewHelper.TEXT_PRIMARY;
    private static final Color TEXT_SECONDARY = HomeViewHelper.TEXT_SECONDARY;

    private final JLabel statusLabel = new JLabel("Checking database connection...");
    private final JLabel detailLabel = new JLabel("The app will continue automatically when the server responds.");
    private final Timer slowConnectionTimer;

    public StartupLoadingView() {
        setTitle("Connecting to KGM Server");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1040, 720);
        setMinimumSize(new Dimension(760, 560));
        setResizable(true);
        setLocationRelativeTo(null);
        installFullScreenGuard();
        setContentPane(createContent());

        slowConnectionTimer = new Timer(SLOW_CONNECTION_THRESHOLD_MS, event -> showSlowConnectionMessage());
        slowConnectionTimer.setRepeats(false);
    }

    public void finish() {
        if (SwingUtilities.isEventDispatchThread()) {
            finishOnEdt();
        } else {
            SwingUtilities.invokeLater(this::finishOnEdt);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible && !isVisible()) {
            slowConnectionTimer.restart();
        }
        super.setVisible(visible);
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(BACKGROUND);

        JPanel card = new JPanel(new BorderLayout(0, 26));
        card.setBackground(CARD);
        card.setBorder(new CompoundBorder(new RoundedBorder(8, BORDER), new EmptyBorder(34, 36, 34, 36)));
        card.setPreferredSize(new Dimension(650, 360));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("KGM GUEST PORTAL");
        eyebrow.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        eyebrow.setForeground(PRIMARY);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Connecting to server");
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Please wait while the database connection is checked.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleBlock.add(eyebrow);
        titleBlock.add(Box.createVerticalStrut(10));
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(6));
        titleBlock.add(subtitle);

        card.add(titleBlock, BorderLayout.NORTH);
        card.add(createStatusPanel(), BorderLayout.CENTER);
        card.add(createConnectionLine(), BorderLayout.SOUTH);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(28, 28, 28, 28);
        constraints.anchor = GridBagConstraints.CENTER;
        root.add(card, constraints);
        return root;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        statusLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 16));
        statusLabel.setForeground(TEXT_PRIMARY);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        detailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detailLabel.setForeground(TEXT_SECONDARY);
        detailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setBorder(BorderFactory.createEmptyBorder());
        progress.setPreferredSize(new Dimension(560, 8));
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        progress.setForeground(PRIMARY);
        progress.setBackground(new Color(232, 244, 255));
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(detailLabel);
        panel.add(Box.createVerticalStrut(18));
        panel.add(progress);
        return panel;
    }

    private JPanel createConnectionLine() {
        JPanel panel = new JPanel(new BorderLayout(14, 0));
        panel.setOpaque(false);

        JLabel label = new JLabel("Configured connection");
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setForeground(TEXT_SECONDARY);

        JLabel value = new JLabel(connectionInfo(), SwingConstants.RIGHT);
        value.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        value.setForeground(PRIMARY_DARK);

        panel.add(label, BorderLayout.WEST);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private void showSlowConnectionMessage() {
        statusLabel.setText("Database is taking longer than usual");
        detailLabel.setText("Checking server, LAN, and MySQL availability. The app is still responsive.");
    }

    private void installFullScreenGuard() {
        setExtendedState(Frame.MAXIMIZED_BOTH);
        addWindowStateListener(event -> {
            if ((event.getNewState() & Frame.ICONIFIED) == Frame.ICONIFIED) {
                SwingUtilities.invokeLater(this::keepFullScreen);
            }
        });
    }

    private void keepFullScreen() {
        setState(Frame.NORMAL);
        setExtendedState(Frame.MAXIMIZED_BOTH);
    }

    private void finishOnEdt() {
        slowConnectionTimer.stop();
        dispose();
    }

    private String connectionInfo() {
        return DatabaseConfig.username()
                + "@"
                + DatabaseConfig.host()
                + ":"
                + DatabaseConfig.port()
                + "/"
                + DatabaseConfig.databaseName();
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
}
