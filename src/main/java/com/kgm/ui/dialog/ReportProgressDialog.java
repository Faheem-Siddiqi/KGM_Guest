package com.kgm.ui.dialog;

import com.kgm.ui.styling.HomeViewHelper;
import com.kgm.service.GuestReportService;

import javax.swing.*;
import java.awt.*;
public class ReportProgressDialog extends JDialog {
    private final JLabel state = new JLabel("Preparing report export...");
    private final JTextArea details = new JTextArea();

    public ReportProgressDialog(Window owner) {
        this(owner, null);
    }

    public ReportProgressDialog(Window owner, GuestReportService.ReportExportRequest request) {
        super(owner, "Generating Reports", ModalityType.MODELESS);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setContentPane(content(request));
        setSize(520, 236);
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
        root.setBackground(Color.WHITE);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HomeViewHelper.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JLabel title = new JLabel("Generating Reports");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        state.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        state.setForeground(HomeViewHelper.TEXT_SECONDARY);
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
        details.setForeground(HomeViewHelper.TEXT_SECONDARY);
        details.setBorder(BorderFactory.createEmptyBorder());
        details.setAlignmentX(Component.LEFT_ALIGNMENT);

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);
        progress.setPreferredSize(new Dimension(430, 16));
        body.add(state);
        body.add(Box.createVerticalStrut(10));
        body.add(details);
        body.add(Box.createVerticalStrut(14));
        body.add(progress);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
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
}
