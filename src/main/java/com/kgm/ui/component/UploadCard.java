package com.kgm.ui.component;

import com.kgm.ui.styling.HomeViewHelper;
import com.kgm.ui.util.FileDialogHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.function.Consumer;

public class UploadCard extends JPanel {
    private static final int CARD_RADIUS = 8;
    private static final Color HOVER_COLOR = new Color(240, 245, 250);
    private static final Color NORMAL_COLOR = Color.WHITE;

    private final FileDialogHandler.FileDialogConfig config;
    private final Consumer<File[]> onFilesSelected;
    private JPanel cardPanel;
    private JButton selectButton;
    private boolean hovered;

    public UploadCard(
            FileDialogHandler.FileDialogConfig config,
            Consumer<File[]> onFilesSelected
    ) {
        this.config = config == null ? new FileDialogHandler.FileDialogConfig() : config.copy();
        this.onFilesSelected = onFilesSelected;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        cardPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? HOVER_COLOR : NORMAL_COLOR);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CARD_RADIUS, CARD_RADIUS);
                g2.setColor(HomeViewHelper.BORDER);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CARD_RADIUS, CARD_RADIUS);
                g2.dispose();
                super.paintComponent(graphics);
            }
        };
        cardPanel.setOpaque(false);
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBorder(BorderFactory.createEmptyBorder(24, 26, 24, 26));
        cardPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        installCardMouseHandlers();

        FileGlyph glyph = new FileGlyph();
        glyph.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(glyph);
        cardPanel.add(Box.createVerticalStrut(18));

        JLabel titleLabel = new JLabel(uploadTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setForeground(HomeViewHelper.TEXT_PRIMARY);
        cardPanel.add(titleLabel);
        cardPanel.add(Box.createVerticalStrut(8));

        JLabel descLabel = new JLabel(uploadDescription());
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        descLabel.setForeground(HomeViewHelper.TEXT_SECONDARY);
        cardPanel.add(descLabel);
        cardPanel.add(Box.createVerticalStrut(18));

        selectButton = new JButton(config.isSelectMultipleFiles() ? "Select Files" : "Select File");
        selectButton.setPreferredSize(new Dimension(128, 36));
        selectButton.setMaximumSize(new Dimension(136, 36));
        selectButton.setBackground(HomeViewHelper.PRIMARY_DARK);
        selectButton.setForeground(Color.WHITE);
        selectButton.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        selectButton.setFocusPainted(false);
        selectButton.setBorderPainted(false);
        selectButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        selectButton.addActionListener(event -> openFileDialog());
        selectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(selectButton);

        add(cardPanel, BorderLayout.CENTER);
    }

    private String uploadTitle() {
        String title = config.getUploadTitle();
        return title == null || title.isBlank() ? "Upload File" : title;
    }

    private String uploadDescription() {
        String description = config.getUploadDescription();
        return description == null || description.isBlank()
                ? FileDialogHandler.fileTypeDescription(config.getFileType())
                : description;
    }

    private void installCardMouseHandlers() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                hovered = true;
                cardPanel.repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hovered = false;
                cardPanel.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                openFileDialog();
            }
        };
        cardPanel.addMouseListener(mouseAdapter);
    }

    private void openFileDialog() {
        if (!isEnabled()) {
            return;
        }
        FileDialogHandler.FileDialogConfig dialogConfig = config.copy()
                .withParent(SwingUtilities.getWindowAncestor(this));
        FileDialogHandler.openFileDialog(dialogConfig, onFilesSelected);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (selectButton != null) {
            selectButton.setEnabled(enabled);
        }
        if (cardPanel != null) {
            cardPanel.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        }
    }

    private static class FileGlyph extends JComponent {
        private FileGlyph() {
            setPreferredSize(new Dimension(58, 58));
            setMaximumSize(new Dimension(58, 58));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(230, 243, 255));
            g2.fillRoundRect(7, 5, 44, 50, 8, 8);
            g2.setColor(HomeViewHelper.PRIMARY);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(7, 5, 44, 50, 8, 8);
            g2.drawLine(18, 23, 40, 23);
            g2.drawLine(18, 32, 40, 32);
            g2.drawLine(18, 41, 32, 41);
            g2.dispose();
        }
    }
}
