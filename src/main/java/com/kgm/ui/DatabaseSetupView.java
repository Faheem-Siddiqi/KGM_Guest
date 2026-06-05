package com.kgm.ui;

import com.kgm.StartupController;
import com.kgm.config.DatabaseConfig;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.net.URI;
import java.sql.SQLException;

public class DatabaseSetupView extends JFrame {
    private static final String MYSQL_DOWNLOAD_URL = "https://dev.mysql.com/downloads/windows/installer/";
    private static final String SQL_SETUP = """
            CREATE USER IF NOT EXISTS 'kgm_user'@'localhost' IDENTIFIED BY 'change_me';
            GRANT CREATE ON *.* TO 'kgm_user'@'localhost';
            GRANT ALL PRIVILEGES ON `kgm_guest`.* TO 'kgm_user'@'localhost';
            FLUSH PRIVILEGES;
            """;
    private static final String ENV_SETUP = """
            KGM_DB_HOST=127.0.0.1
            KGM_DB_PORT=3306
            KGM_DB_NAME=kgm_guest
            KGM_DB_USER=kgm_user
            KGM_DB_PASSWORD=change_me

            KGM_LOGIN_USERNAME=admin
            KGM_LOGIN_PASSWORD=change_this_password
            """;
    private static final String RUN_COMMANDS = """
            Copy-Item .env.sample .env
            mvn package
            java -jar target\\my-java-app-1.0.0.jar
            """;
    private static final Color BACKGROUND = new Color(246, 249, 252);
    private static final Color PANEL = Color.WHITE;
    private static final Color TEXT_PRIMARY = HomeViewHelper.TEXT_PRIMARY;
    private static final Color TEXT_SECONDARY = HomeViewHelper.TEXT_SECONDARY;
    private static final Color BORDER = HomeViewHelper.BORDER;
    private static final Color PRIMARY = HomeViewHelper.PRIMARY;
    private static final Color PRIMARY_DARK = HomeViewHelper.PRIMARY_DARK;
    private static final Color TEAL = HomeViewHelper.TEAL;
    private static final Color WARNING_BACKGROUND = new Color(255, 247, 237);
    private static final Color WARNING_BORDER = new Color(253, 186, 116);
    private static final Color WARNING_TEXT = new Color(154, 52, 18);

    private final JLabel statusLabel = new JLabel("Waiting for database setup");
    private final JTextArea errorText = new JTextArea();
    private final JButton retryButton = new ActionButton("Retry connection", true);

    public DatabaseSetupView(RuntimeException startupFailure) {
        setTitle("KGM Database Setup");
        setSize(1040, 720);
        setMinimumSize(new Dimension(860, 620));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(BACKGROUND);
        setLayout(new BorderLayout());

        add(createHeader(), BorderLayout.NORTH);
        add(createBody(startupFailure), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
        updateError(startupFailure);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(24, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(26, 34, 12, 34));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("KGM GUEST PORTAL");
        eyebrow.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        eyebrow.setForeground(PRIMARY);

        JLabel title = new JLabel("Database setup required");
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setForeground(TEXT_PRIMARY);

        JLabel subtitle = new JLabel("The app opened safely. Complete these steps, then retry the connection.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(TEXT_SECONDARY);

        titleBlock.add(eyebrow);
        titleBlock.add(Box.createVerticalStrut(8));
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(subtitle);

        StatusPill pill = new StatusPill("Database offline");
        header.add(titleBlock, BorderLayout.WEST);
        header.add(pill, BorderLayout.EAST);
        return header;
    }

    private JPanel createBody(RuntimeException startupFailure) {
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(8, 34, 18, 34));

        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = 0;
        left.weightx = 0.34;
        left.weighty = 1.0;
        left.fill = GridBagConstraints.BOTH;
        left.insets = new Insets(0, 0, 0, 18);
        body.add(createStatusPanel(startupFailure), left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = 0;
        right.weightx = 0.66;
        right.weighty = 1.0;
        right.fill = GridBagConstraints.BOTH;
        body.add(createStepsPanel(), right);

        return body;
    }

    private JPanel createStatusPanel(RuntimeException startupFailure) {
        JPanel panel = cardPanel();
        panel.setLayout(new BorderLayout(0, 20));
        panel.setBorder(new CompoundBorder(new RoundedBorder(8, BORDER), new EmptyBorder(24, 24, 24, 24)));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("Current connection");
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        label.setForeground(TEXT_SECONDARY);

        JLabel connection = new JLabel(connectionInfo());
        connection.setFont(new Font("Segoe UI", Font.BOLD, 18));
        connection.setForeground(TEXT_PRIMARY);

        statusLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        statusLabel.setForeground(WARNING_TEXT);

        top.add(label);
        top.add(Box.createVerticalStrut(8));
        top.add(connection);
        top.add(Box.createVerticalStrut(14));
        top.add(statusLabel);

        JPanel errorPanel = new JPanel(new BorderLayout(0, 10));
        errorPanel.setBackground(WARNING_BACKGROUND);
        errorPanel.setBorder(new CompoundBorder(new RoundedBorder(8, WARNING_BORDER), new EmptyBorder(16, 16, 16, 16)));

        JLabel errorTitle = new JLabel("What happened");
        errorTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        errorTitle.setForeground(WARNING_TEXT);

        errorText.setEditable(false);
        errorText.setOpaque(false);
        errorText.setLineWrap(true);
        errorText.setWrapStyleWord(true);
        errorText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        errorText.setForeground(WARNING_TEXT);
        errorText.setBorder(BorderFactory.createEmptyBorder());
        errorText.setText(errorMessage(startupFailure));

        errorPanel.add(errorTitle, BorderLayout.NORTH);
        errorPanel.add(errorText, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridBagLayout());
        actions.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 10, 0);
        actions.add(retryButton, gbc);

        JButton mysqlButton = new ActionButton("Open MySQL download", false);
        mysqlButton.addActionListener(event -> openUrl(MYSQL_DOWNLOAD_URL));
        gbc.gridy = 1;
        actions.add(mysqlButton, gbc);

        retryButton.addActionListener(event -> retryConnection());

        panel.add(top, BorderLayout.NORTH);
        panel.add(errorPanel, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JScrollPane createStepsPanel() {
        JPanel steps = new JPanel();
        steps.setOpaque(false);
        steps.setLayout(new BoxLayout(steps, BoxLayout.Y_AXIS));

        steps.add(stepCard(
                "1",
                "Install and start MySQL Server",
                "Use MySQL Server 8.0 or newer. Keep the default port 3306 unless your PC uses a custom port.",
                MYSQL_DOWNLOAD_URL,
                "Open download",
                MYSQL_DOWNLOAD_URL
        ));
        steps.add(Box.createVerticalStrut(14));
        steps.add(stepCard(
                "2",
                "Create the KGM database user",
                "Run this SQL in MySQL Workbench or the MySQL command line. You can change the password, but keep .env the same.",
                SQL_SETUP,
                "Copy SQL",
                SQL_SETUP
        ));
        steps.add(Box.createVerticalStrut(14));
        steps.add(stepCard(
                "3",
                "Create the local .env file",
                "From the project root, copy .env.sample to .env, then place these values in the new file.",
                ENV_SETUP,
                "Copy .env values",
                ENV_SETUP
        ));
        steps.add(Box.createVerticalStrut(14));
        steps.add(stepCard(
                "4",
                "Build and run after setup",
                "Run these commands from the cloned project folder. Maven downloads the Java libraries automatically.",
                RUN_COMMANDS,
                "Copy commands",
                RUN_COMMANDS
        ));

        JScrollPane scrollPane = new JScrollPane(steps);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private JPanel stepCard(
            String number,
            String title,
            String detail,
            String codeText,
            String actionText,
            String copyText
    ) {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout(16, 14));
        card.setBorder(new CompoundBorder(new RoundedBorder(8, BORDER), new EmptyBorder(18, 18, 18, 18)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        JPanel heading = new JPanel(new BorderLayout(14, 0));
        heading.setOpaque(false);

        JLabel numberLabel = new StepNumber(number);
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLabel.setForeground(TEXT_PRIMARY);

        JTextArea detailText = plainText(detail, new Font("Segoe UI", Font.PLAIN, 13), TEXT_SECONDARY);
        detailText.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));

        titleBlock.add(titleLabel);
        titleBlock.add(detailText);
        heading.add(numberLabel, BorderLayout.WEST);
        heading.add(titleBlock, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.setOpaque(false);
        bottom.add(codeBlock(codeText), BorderLayout.CENTER);

        JButton action = new SmallButton(actionText);
        action.addActionListener(event -> {
            if (copyText.startsWith("https://")) {
                openUrl(copyText);
            } else {
                copyToClipboard(copyText);
            }
        });
        bottom.add(action, BorderLayout.EAST);

        card.add(heading, BorderLayout.NORTH);
        card.add(bottom, BorderLayout.CENTER);
        return card;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(0, 34, 26, 34));

        JLabel note = new JLabel("After MySQL and .env are ready, click Retry connection to continue to login.");
        note.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        note.setForeground(TEXT_SECONDARY);

        JButton close = HomeViewHelper.textButton("Close");
        close.addActionListener(event -> dispose());

        JPanel closeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeWrap.setOpaque(false);
        closeWrap.add(close);

        footer.add(note, BorderLayout.WEST);
        footer.add(closeWrap, BorderLayout.EAST);
        return footer;
    }

    private JPanel cardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL);
        panel.setOpaque(true);
        return panel;
    }

    private JTextArea codeBlock(String text) {
        JTextArea area = new JTextArea(text.trim());
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        area.setForeground(new Color(49, 58, 70));
        area.setBackground(new Color(248, 250, 252));
        area.setBorder(new CompoundBorder(new RoundedBorder(8, new Color(226, 232, 240)), new EmptyBorder(12, 12, 12, 12)));
        return area;
    }

    private JTextArea plainText(String text, Font font, Color color) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(font);
        area.setForeground(color);
        area.setBorder(BorderFactory.createEmptyBorder());
        return area;
    }

    private void retryConnection() {
        statusLabel.setText("Checking MySQL connection...");
        retryButton.setEnabled(false);
        retryButton.setText("Checking...");

        new SwingWorker<Boolean, Void>() {
            private RuntimeException failure;

            @Override
            protected Boolean doInBackground() {
                try {
                    StartupController.initializeDatabase();
                    return true;
                } catch (RuntimeException exception) {
                    failure = exception;
                    return false;
                }
            }

            @Override
            protected void done() {
                retryButton.setEnabled(true);
                retryButton.setText("Retry connection");
                if (Boolean.TRUE.equals(getResult())) {
                    statusLabel.setText("Database connected");
                    dispose();
                    StartupController.showLoginWindow();
                } else {
                    statusLabel.setText("Still waiting for database setup");
                    updateError(failure);
                }
            }

            private Boolean getResult() {
                try {
                    return get();
                } catch (Exception exception) {
                    return false;
                }
            }
        }.execute();
    }

    private void updateError(RuntimeException failure) {
        errorText.setText(errorMessage(failure));
        errorText.setCaretPosition(0);
    }

    private String errorMessage(Throwable failure) {
        Throwable root = rootCause(failure);
        String message = root == null ? "Database configuration is incomplete." : root.getMessage();
        if (message == null || message.isBlank()) {
            message = failure == null ? "Database configuration is incomplete." : failure.getMessage();
        }

        String hint = hintFor(root);
        return message + "\n\n" + hint;
    }

    private String hintFor(Throwable failure) {
        if (failure instanceof SQLException sqlException) {
            if (sqlException.getErrorCode() == 1045) {
                return "Check KGM_DB_USER and KGM_DB_PASSWORD in .env, then retry.";
            }
            if (sqlException.getMessage() != null && sqlException.getMessage().toLowerCase().contains("communications link failure")) {
                return "Start MySQL Server and confirm it is listening on " + DatabaseConfig.host() + ":" + DatabaseConfig.port() + ".";
            }
        }
        return "Follow the setup checklist on the right, then retry the connection.";
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
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

    private void openUrl(String url) {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                copyToClipboard(url);
                return;
            }
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception exception) {
            copyToClipboard(url);
        }
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text.trim()), null);
        statusLabel.setText("Copied to clipboard");
    }

    private static class ActionButton extends JButton {
        private final boolean primary;
        private boolean hovered;

        ActionButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
            setForeground(primary ? Color.WHITE : PRIMARY);
            setBorder(new EmptyBorder(12, 16, 12, 16));
            setHorizontalAlignment(SwingConstants.CENTER);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
                    repaint();
                }
            });
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = primary
                    ? (hovered && isEnabled() ? PRIMARY_DARK : PRIMARY)
                    : (hovered && isEnabled() ? new Color(232, 244, 255) : Color.WHITE);
            g2.setColor(isEnabled() ? fill : new Color(230, 235, 240));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.setColor(primary ? fill : BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class SmallButton extends JButton {
        SmallButton(String text) {
            super(text);
            setFocusPainted(false);
            setForeground(PRIMARY);
            setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
            setBackground(Color.WHITE);
            setBorder(new CompoundBorder(new RoundedBorder(8, BORDER), new EmptyBorder(8, 12, 8, 12)));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    private static class StatusPill extends JLabel {
        StatusPill(String text) {
            super(text);
            setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
            setForeground(PRIMARY_DARK);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(new EmptyBorder(8, 14, 8, 14));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(232, 244, 255));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class StepNumber extends JLabel {
        StepNumber(String number) {
            super(number, SwingConstants.CENTER);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setPreferredSize(new Dimension(34, 34));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint paint = new GradientPaint(0, 0, PRIMARY, getWidth(), getHeight(), TEAL);
            g2.setPaint(paint);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.dispose();
            super.paintComponent(graphics);
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
}
