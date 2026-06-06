package com.kgm.ui;

import com.kgm.StartupController;
import com.kgm.config.DatabaseConfig;
import com.kgm.config.DatabaseConnection;
import com.kgm.config.DatabaseConnectionFailure;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
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
import java.awt.Frame;
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
import java.io.PrintWriter;
import java.io.StringWriter;
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
    private static final Color WARNING_BACKGROUND = new Color(255, 248, 246);
    private static final Color WARNING_BORDER = new Color(255, 205, 196);
    private static final Color WARNING_TEXT = new Color(176, 43, 31);
    private static final int AUTO_RETRY_INITIAL_DELAY_MS = 3500;
    private static final int AUTO_RETRY_INTERVAL_MS = 10000;

    private static DatabaseSetupView activeView;

    private final JLabel statusLabel = new JLabel("Waiting for database setup");
    private final JTextArea errorText = new JTextArea();
    private final JTextArea technicalText = new JTextArea();
    private final JScrollPane technicalScroll = new JScrollPane(technicalText);
    private final JToggleButton technicalToggle = new JToggleButton("Show technical details");
    private final JProgressBar retryProgress = new JProgressBar();
    private final JButton retryButton = new ActionButton("Retry connection", true);
    private Runnable onConnected;
    private SwingWorker<Boolean, Void> retryWorker;
    private Timer autoRetryTimer;

    public DatabaseSetupView(RuntimeException startupFailure) {
        this(startupFailure, StartupController::showLoginWindow, true);
    }

    private DatabaseSetupView(RuntimeException startupFailure, Runnable onConnected, boolean exitOnClose) {
        this.onConnected = onConnected;
        activeView = this;
        setTitle(DatabaseConnectionFailure.TITLE);
        setSize(1040, 720);
        setMinimumSize(new Dimension(760, 560));
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(exitOnClose ? EXIT_ON_CLOSE : DISPOSE_ON_CLOSE);
        installFullScreenGuard();
        getContentPane().setBackground(BACKGROUND);
        setLayout(new BorderLayout());

        add(createRecoveryContent(startupFailure), BorderLayout.CENTER);
        updateError(startupFailure);
        startAutoRetry();
    }

    public static void showStartupFailure(RuntimeException failure) {
        showConnectionFailure(failure, StartupController::showLoginWindow, true);
    }

    public static void showConnectionFailure(Throwable failure) {
        showConnectionFailure(failure, null, false);
    }

    public static boolean showIfConnectionFailure(Throwable failure) {
        if (!DatabaseConnection.isConnectionFailure(failure)) {
            return false;
        }
        showConnectionFailure(failure);
        return true;
    }

    private static void showConnectionFailure(Throwable failure, Runnable onConnected, boolean exitOnClose) {
        Runnable show = () -> {
            RuntimeException runtimeFailure = runtimeFailure(failure);
            if (activeView != null && activeView.isDisplayable()) {
                boolean wasVisible = activeView.isVisible();
                activeView.setRecoveryAction(onConnected);
                activeView.updateError(runtimeFailure);
                activeView.setVisible(true);
                activeView.keepFullScreen();
                if (!wasVisible) {
                    activeView.toFront();
                    activeView.requestFocus();
                }
                return;
            }

            new DatabaseSetupView(runtimeFailure, onConnected, exitOnClose).setVisible(true);
        };

        if (SwingUtilities.isEventDispatchThread()) {
            show.run();
        } else {
            SwingUtilities.invokeLater(show);
        }
    }

    private static RuntimeException runtimeFailure(Throwable failure) {
        if (failure instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(DatabaseConnectionFailure.TITLE, failure);
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

    private JScrollPane createRecoveryContent(RuntimeException startupFailure) {
        JPanel page = new JPanel();
        page.setOpaque(false);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(new EmptyBorder(26, 34, 28, 34));

        page.add(createRecoveryHeader());
        page.add(Box.createVerticalStrut(18));
        page.add(createStatusPanel(startupFailure));
        page.add(Box.createVerticalStrut(14));
        page.add(createQuietCheckPanel());
        page.add(Box.createVerticalStrut(14));
        page.add(createChecklistPanel());
        page.add(Box.createVerticalStrut(14));
        page.add(createInlineFooter());

        JScrollPane scrollPane = new JScrollPane(page);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    private JPanel createRecoveryHeader() {
        JPanel header = new JPanel(new GridBagLayout());
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 128));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("KGM GUEST PORTAL");
        eyebrow.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        eyebrow.setForeground(PRIMARY);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel(DatabaseConnectionFailure.TITLE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Server is unavailable. The app is checking quietly and will continue when it reconnects.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleBlock.add(eyebrow);
        titleBlock.add(Box.createVerticalStrut(8));
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(subtitle);

        GridBagConstraints titleConstraints = new GridBagConstraints();
        titleConstraints.gridx = 0;
        titleConstraints.gridy = 0;
        titleConstraints.weightx = 1.0;
        titleConstraints.fill = GridBagConstraints.HORIZONTAL;
        titleConstraints.anchor = GridBagConstraints.WEST;
        titleConstraints.insets = new Insets(0, 0, 0, 16);
        header.add(titleBlock, titleConstraints);

        GridBagConstraints pillConstraints = new GridBagConstraints();
        pillConstraints.gridx = 1;
        pillConstraints.gridy = 0;
        pillConstraints.anchor = GridBagConstraints.NORTHEAST;
        header.add(new StatusPill("Auto-checking"), pillConstraints);
        return header;
    }

    private JPanel createQuietCheckPanel() {
        JPanel panel = cardPanel();
        panel.setLayout(new BorderLayout(14, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));
        panel.setBorder(new CompoundBorder(new RoundedBorder(8, new Color(191, 219, 254)), new EmptyBorder(16, 18, 16, 18)));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Automatic recovery is running");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(TEXT_PRIMARY);

        JLabel detail = new JLabel("No action is needed. The app checks the server in the background every few seconds.");
        detail.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detail.setForeground(TEXT_SECONDARY);

        text.add(title);
        text.add(Box.createVerticalStrut(4));
        text.add(detail);

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setPreferredSize(new Dimension(180, 8));
        progress.setBorder(BorderFactory.createEmptyBorder());
        progress.setForeground(PRIMARY);
        progress.setBackground(new Color(232, 244, 255));

        panel.add(text, BorderLayout.CENTER);
        panel.add(progress, BorderLayout.EAST);
        return panel;
    }

    private JPanel createChecklistPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 16));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Connection checklist");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_PRIMARY);

        JLabel detail = new JLabel("Use these actions only if the automatic check cannot reconnect.");
        detail.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detail.setForeground(TEXT_SECONDARY);

        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(detail);

        JPanel steps = new JPanel();
        steps.setOpaque(false);
        steps.setLayout(new BoxLayout(steps, BoxLayout.Y_AXIS));
        steps.add(stepCard(
                "1",
                "Confirm MySQL Server is running",
                "Check Server PC 516, LAN connectivity, and MySQL service status on port " + DatabaseConfig.port() + ".",
                DatabaseConfig.host() + ":" + DatabaseConfig.port(),
                "Download MySQL",
                MYSQL_DOWNLOAD_URL
        ));
        steps.add(Box.createVerticalStrut(12));
        steps.add(stepCard(
                "2",
                "Verify database access",
                "Run this SQL only if the database user is missing or the password changed.",
                SQL_SETUP,
                "Copy SQL",
                SQL_SETUP
        ));
        steps.add(Box.createVerticalStrut(12));
        steps.add(stepCard(
                "3",
                "Check the .env connection",
                "Make sure this computer points to the correct server, database, user, and password.",
                ENV_SETUP,
                "Copy .env values",
                ENV_SETUP
        ));

        wrapper.add(heading, BorderLayout.NORTH);
        wrapper.add(steps, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createInlineFooter() {
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel note = new JLabel("You can keep working when the server reconnects. Manual retry is optional.");
        note.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        note.setForeground(TEXT_SECONDARY);

        JButton close = HomeViewHelper.textButton("Close");
        close.addActionListener(event -> dispose());

        footer.add(note, BorderLayout.WEST);
        footer.add(close, BorderLayout.EAST);
        return footer;
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

        JLabel title = new JLabel(DatabaseConnectionFailure.TITLE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setForeground(TEXT_PRIMARY);

        JLabel subtitle = new JLabel("The app opened a safe recovery screen. Check the server connection, then retry.");
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
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        panel.setBorder(new CompoundBorder(new RoundedBorder(8, BORDER), new EmptyBorder(24, 24, 24, 24)));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("Current connection");
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        label.setForeground(TEXT_SECONDARY);

        JTextArea connection = plainText(connectionInfo(), new Font("Segoe UI", Font.BOLD, 18), TEXT_PRIMARY);
        connection.setFocusable(false);

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

        JLabel errorTitle = new JLabel(DatabaseConnectionFailure.TITLE);
        errorTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        errorTitle.setForeground(WARNING_TEXT);

        errorText.setEditable(false);
        errorText.setOpaque(false);
        errorText.setFocusable(false);
        errorText.setLineWrap(true);
        errorText.setWrapStyleWord(true);
        errorText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        errorText.setForeground(WARNING_TEXT);
        errorText.setBorder(BorderFactory.createEmptyBorder());
        errorText.setText(errorMessage(startupFailure));

        errorPanel.add(errorTitle, BorderLayout.NORTH);
        errorPanel.add(errorText, BorderLayout.CENTER);

        JPanel errorStack = new JPanel();
        errorStack.setOpaque(false);
        errorStack.setLayout(new BoxLayout(errorStack, BoxLayout.Y_AXIS));
        errorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorStack.add(errorPanel);
        errorStack.add(Box.createVerticalStrut(12));
        errorStack.add(createTechnicalDetailsPanel());

        JPanel actions = new JPanel(new GridBagLayout());
        actions.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 10, 0);
        actions.add(retryButton, gbc);

        retryProgress.setIndeterminate(true);
        retryProgress.setVisible(false);
        retryProgress.setPreferredSize(new Dimension(0, 6));
        retryProgress.setBorder(BorderFactory.createEmptyBorder());
        retryProgress.setBackground(new Color(232, 244, 255));
        retryProgress.setForeground(PRIMARY);
        gbc.gridy = 1;
        actions.add(retryProgress, gbc);

        JButton mysqlButton = new ActionButton("Download MySQL", false);
        mysqlButton.addActionListener(event -> openUrl(MYSQL_DOWNLOAD_URL));
        gbc.gridy = 2;
        actions.add(mysqlButton, gbc);

        retryButton.addActionListener(event -> retryConnection(true));

        panel.add(top, BorderLayout.NORTH);
        panel.add(errorStack, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createTechnicalDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        technicalToggle.setFocusPainted(false);
        technicalToggle.setContentAreaFilled(false);
        technicalToggle.setBorderPainted(false);
        technicalToggle.setOpaque(false);
        technicalToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        technicalToggle.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        technicalToggle.setForeground(PRIMARY_DARK);
        technicalToggle.setHorizontalAlignment(SwingConstants.LEFT);
        technicalToggle.setBorder(new EmptyBorder(2, 0, 2, 0));
        technicalToggle.addActionListener(event -> toggleTechnicalDetails());

        technicalText.setEditable(false);
        technicalText.setFocusable(false);
        technicalText.setLineWrap(true);
        technicalText.setWrapStyleWord(true);
        technicalText.setFont(new Font("Consolas", Font.PLAIN, 12));
        technicalText.setForeground(new Color(49, 58, 70));
        technicalText.setBackground(new Color(248, 250, 252));
        technicalText.setBorder(new EmptyBorder(10, 10, 10, 10));

        technicalScroll.setVisible(false);
        technicalScroll.setPreferredSize(new Dimension(0, 150));
        technicalScroll.setBorder(new RoundedBorder(8, new Color(226, 232, 240)));

        panel.add(technicalToggle, BorderLayout.NORTH);
        panel.add(technicalScroll, BorderLayout.CENTER);
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
                "Download MySQL",
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
        area.setFocusable(false);
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

    private void startAutoRetry() {
        autoRetryTimer = new Timer(AUTO_RETRY_INTERVAL_MS, event -> retryConnection(false));
        autoRetryTimer.setInitialDelay(AUTO_RETRY_INITIAL_DELAY_MS);
        autoRetryTimer.start();
    }

    private void retryConnection(boolean userInitiated) {
        if (retryWorker != null && !retryWorker.isDone()) {
            return;
        }

        statusLabel.setText(userInitiated ? "Checking MySQL connection..." : "Checking server quietly...");
        if (userInitiated) {
            retryButton.setEnabled(false);
            retryButton.setText("Checking...");
            retryProgress.setVisible(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }

        retryWorker = new SwingWorker<>() {
            private RuntimeException failure;

            @Override
            protected Boolean doInBackground() {
                try {
                    StartupController.reconnectDatabase();
                    return true;
                } catch (RuntimeException exception) {
                    failure = exception;
                    return false;
                }
            }

            @Override
            protected void done() {
                if (userInitiated) {
                    retryButton.setEnabled(true);
                    retryButton.setText("Retry connection");
                    retryProgress.setVisible(false);
                    setCursor(Cursor.getDefaultCursor());
                }
                if (Boolean.TRUE.equals(getResult())) {
                    statusLabel.setText("Database connected");
                    if (autoRetryTimer != null) {
                        autoRetryTimer.stop();
                    }
                    Runnable recovery = onConnected;
                    dispose();
                    if (recovery != null) {
                        recovery.run();
                    }
                } else {
                    statusLabel.setText("Server offline. Automatic checks continue.");
                    if (userInitiated) {
                        updateError(failure);
                    }
                }
                retryWorker = null;
            }

            private Boolean getResult() {
                try {
                    return get();
                } catch (Exception exception) {
                    return false;
                }
            }
        };
        retryWorker.execute();
    }

    private void updateError(RuntimeException failure) {
        errorText.setText(errorMessage(failure));
        errorText.setCaretPosition(0);
        technicalText.setText(technicalDetails(failure));
        technicalText.setCaretPosition(0);
    }

    private String errorMessage(Throwable failure) {
        return DatabaseConnection.userFriendlyConnectionMessage() + "\n" + hintFor(rootCause(failure));
    }

    private String technicalDetails(Throwable failure) {
        if (failure == null) {
            return "No technical error details were provided.";
        }

        StringWriter writer = new StringWriter();
        PrintWriter printer = new PrintWriter(writer);
        failure.printStackTrace(printer);
        printer.flush();
        return writer.toString();
    }

    private void toggleTechnicalDetails() {
        boolean showing = technicalToggle.isSelected();
        technicalToggle.setText(showing ? "Hide technical details" : "Show technical details");
        technicalScroll.setVisible(showing);
        revalidate();
        repaint();
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

    private void setRecoveryAction(Runnable recoveryAction) {
        if (recoveryAction != null) {
            this.onConnected = recoveryAction;
        }
    }

    @Override
    public void dispose() {
        if (autoRetryTimer != null) {
            autoRetryTimer.stop();
        }
        if (activeView == this) {
            activeView = null;
        }
        super.dispose();
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
