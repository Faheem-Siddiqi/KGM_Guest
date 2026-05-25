package com.kgm.ui.dialog;

import com.kgm.service.ExcelImportService;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.io.File;

public class ImportProgressDialog extends JDialog {
    private static final Color SURFACE = new Color(247, 250, 252);
    private static final Color PROGRESS_TRACK = new Color(226, 234, 244);
    private static final Color BADGE_BACKGROUND = new Color(235, 245, 255);
    private static final int BODY_WIDTH = 470;

    private final JLabel stateLabel = new JLabel("Preparing import");
    private final JLabel detailLabel = new JLabel("Waiting for the workbook scan to start.");
    private final JLabel rowLabel = new JLabel("Rows --/--");
    private final JLabel percentLabel = new JLabel("--");
    private final JProgressBar progressBar = new JProgressBar();
    private boolean closed;

    public ImportProgressDialog(Window owner, File file, String importMode) {
        super(owner, "Importing Excel", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setContentPane(content(file, importMode));
        pack();
        setMinimumSize(new Dimension(540, 270));
        setLocationRelativeTo(owner);
    }

    public void open() {
        if (!closed) {
            setVisible(true);
        }
    }

    public void close() {
        closed = true;
        setVisible(false);
        dispose();
    }

    public void updateProgress(ExcelImportService.ImportProgress progress) {
        if (progress == null) {
            return;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> updateProgress(progress));
            return;
        }

        stateLabel.setText(progress.state());
        detailLabel.setText(progress.detail());
        if (progress.determinate()) {
            progressBar.setIndeterminate(false);
            progressBar.setMaximum(Math.max(1, progress.total()));
            progressBar.setValue(Math.min(progress.current(), progress.total()));
            rowLabel.setText("Rows " + progress.current() + "/" + progress.total());
            percentLabel.setText(progress.percentage() + "%");
        } else {
            progressBar.setIndeterminate(true);
            rowLabel.setText("Rows --/--");
            percentLabel.setText("Working");
        }
    }

    private JPanel content(File file, String importMode) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(header(importMode), BorderLayout.NORTH);
        root.add(body(file), BorderLayout.CENTER);
        return root;
    }

    private JPanel header(String importMode) {
        JPanel header = new JPanel(new BorderLayout(12, 2));
        header.setBackground(HomeViewHelper.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));

        JLabel title = new JLabel("Importing Excel");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel(importMode == null || importMode.isBlank() ? "Guest import" : importMode);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(223, 239, 255));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(title);
        text.add(Box.createVerticalStrut(2));
        text.add(subtitle);
        header.add(text, BorderLayout.WEST);
        return header;
    }

    private JPanel body(File file) {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(22, 24, 24, 24));

        stateLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 16));
        stateLabel.setForeground(HomeViewHelper.TEXT_PRIMARY);
        stateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        detailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        detailLabel.setForeground(HomeViewHelper.TEXT_SECONDARY);
        detailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComponent filePanel = filePanel(file);
        filePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel progressMeta = new JPanel(new BorderLayout());
        progressMeta.setOpaque(false);
        progressMeta.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressMeta.setMaximumSize(new Dimension(BODY_WIDTH, 24));
        rowLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        rowLabel.setForeground(HomeViewHelper.PRIMARY_DARK);
        percentLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        percentLabel.setForeground(HomeViewHelper.TEXT_SECONDARY);
        progressMeta.add(rowLabel, BorderLayout.WEST);
        progressMeta.add(percentLabel, BorderLayout.EAST);

        progressBar.setPreferredSize(new Dimension(BODY_WIDTH, 16));
        progressBar.setMaximumSize(new Dimension(BODY_WIDTH, 16));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setForeground(HomeViewHelper.PRIMARY);
        progressBar.setBackground(PROGRESS_TRACK);
        progressBar.setBorderPainted(false);
        progressBar.setIndeterminate(true);

        body.add(stateLabel);
        body.add(Box.createVerticalStrut(6));
        body.add(detailLabel);
        body.add(Box.createVerticalStrut(18));
        body.add(filePanel);
        body.add(Box.createVerticalStrut(18));
        body.add(progressMeta);
        body.add(Box.createVerticalStrut(8));
        body.add(progressBar);
        return body;
    }

    private JComponent filePanel(File file) {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(8, HomeViewHelper.BORDER),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        panel.setMaximumSize(new Dimension(BODY_WIDTH, 58));
        panel.setPreferredSize(new Dimension(BODY_WIDTH, 58));

        JLabel badge = new JLabel("XLS", SwingConstants.CENTER);
        badge.setOpaque(true);
        badge.setBackground(BADGE_BACKGROUND);
        badge.setForeground(HomeViewHelper.PRIMARY_DARK);
        badge.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));
        badge.setPreferredSize(new Dimension(42, 30));
        badge.setBorder(new RoundedBorder(8, new Color(194, 222, 255)));

        String nameText = file == null ? "Selected workbook" : file.getName();
        JLabel fileName = new JLabel(compactText(nameText, 42));
        fileName.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        fileName.setForeground(HomeViewHelper.TEXT_PRIMARY);
        fileName.setToolTipText(nameText);

        String pathText = file == null || file.getParent() == null ? "" : file.getParent();
        JLabel filePath = new JLabel(compactText(pathText, 66));
        filePath.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        filePath.setForeground(HomeViewHelper.TEXT_SECONDARY);
        filePath.setToolTipText(pathText);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(fileName);
        text.add(Box.createVerticalStrut(2));
        text.add(filePath);

        panel.add(badge, BorderLayout.WEST);
        panel.add(text, BorderLayout.CENTER);
        return panel;
    }

    private static String compactText(String text, int maxLength) {
        String value = text == null ? "" : text;
        if (value.length() <= maxLength) {
            return value;
        }
        if (maxLength <= 3) {
            return value.substring(0, maxLength);
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        private RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        public Insets getBorderInsets(Component component) {
            return new Insets(8, 8, 8, 8);
        }
    }
}
