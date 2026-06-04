package com.kgm.ui.dialog;

import com.kgm.ui.styling.HomeViewHelper;
import com.kgm.service.GuestReportService;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
public class ReportProgressDialog extends JDialog {
    private static final Color BACKGROUND = new Color(248, 250, 252);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color SELECTED_SURFACE = new Color(239, 246, 255);

    private final JLabel state = new JLabel("Preparing report export...");
    private final JTextArea details = new JTextArea();

    public ReportProgressDialog(Window owner) {
        this(owner, null);
    }

    public ReportProgressDialog(Window owner, GuestReportService.ReportExportRequest request) {
        super(owner, "Generating Reports", ModalityType.MODELESS);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setContentPane(content(request));
        setSize(560, 278);
        setLocationRelativeTo(owner);
    }

    public void open() {
        setVisible(true);
    }

    public void close() {
        dispose();
    }

    public void updateProgress(String message) {
        state.setText(message == null || message.isBlank() ? "Working on report export..." : message.trim());
    }

    private JPanel content(GuestReportService.ReportExportRequest request) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel card = new RoundedPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        JLabel icon = new BadgeLabel();
        icon.setPreferredSize(new Dimension(40, 40));
        JLabel title = new JLabel("Generating Reports");
        title.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 18));
        title.setForeground(HomeViewHelper.TEXT_PRIMARY);
        JLabel subtitle = new JLabel("Please keep this window open while the files are prepared.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(MUTED);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(title);
        copy.add(Box.createVerticalStrut(3));
        copy.add(subtitle);
        header.add(icon, BorderLayout.WEST);
        header.add(copy, BorderLayout.CENTER);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        state.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        state.setForeground(HomeViewHelper.TEXT_PRIMARY);
        state.setAlignmentX(Component.LEFT_ALIGNMENT);

        details.setText(detailsText(request));
        details.setEditable(false);
        details.setFocusable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(false);
        details.setRows(3);
        details.setColumns(48);
        details.setOpaque(false);
        details.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        details.setForeground(MUTED);
        details.setBorder(BorderFactory.createEmptyBorder());
        details.setAlignmentX(Component.LEFT_ALIGNMENT);

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);
        progress.setPreferredSize(new Dimension(500, 8));
        progress.setMinimumSize(new Dimension(320, 8));
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        progress.setBorder(BorderFactory.createEmptyBorder());
        progress.setOpaque(false);
        progress.setUI(new ModernProgressBarUI());
        body.add(state);
        body.add(Box.createVerticalStrut(12));
        body.add(details);
        body.add(Box.createVerticalStrut(18));
        body.add(progress);

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        root.add(card, BorderLayout.CENTER);
        return root;
    }

    private String detailsText(GuestReportService.ReportExportRequest request) {
        if (request == null || request.range() == null) {
            return "Preparing selected report files.";
        }
        GuestReportService.ReportRange range = request.range();
        return "Period: " + range.label()
                + " (" + range.startDate() + " to " + range.endDate() + ")\n"
                + "Formats: " + request.formatLabel() + "\n"
                + "Saving to: " + destinationText(request);
    }

    private String destinationText(GuestReportService.ReportExportRequest request) {
        if (request.saveTarget() == null) {
            return "Selected save location";
        }
        return request.saveTarget().getAbsolutePath();
    }

    private static class RoundedPanel extends JPanel {
        private RoundedPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(15, 23, 42, 10));
            g2.fillRoundRect(2, 3, getWidth() - 5, getHeight() - 6, 10, 10);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 3, 10, 10);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 2, getHeight() - 3, 10, 10);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class BadgeLabel extends JLabel {
        private BadgeLabel() {
            super("R", SwingConstants.CENTER);
            setForeground(HomeViewHelper.PRIMARY);
            setFont(new Font("Segoe UI", Font.BOLD, 16));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(SELECTED_SURFACE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.setColor(new Color(191, 219, 254));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class ModernProgressBarUI extends BasicProgressBarUI {
        @Override
        protected void paintDeterminate(Graphics graphics, JComponent component) {
            paintTrack(graphics, component);
        }

        @Override
        protected void paintIndeterminate(Graphics graphics, JComponent component) {
            paintTrack(graphics, component);
            Rectangle box = getBox(null);
            if (box == null) {
                return;
            }
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(HomeViewHelper.PRIMARY);
            g2.fillRoundRect(box.x, 0, box.width, component.getHeight(), 8, 8);
            g2.dispose();
        }

        private void paintTrack(Graphics graphics, JComponent component) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(226, 232, 240));
            g2.fillRoundRect(0, 0, component.getWidth(), component.getHeight(), 8, 8);
            g2.dispose();
        }
    }
}
