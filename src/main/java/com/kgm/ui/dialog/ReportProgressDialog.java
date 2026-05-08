package com.kgm.ui.dialog;

import com.kgm.ui.styling.HomeViewHelper;
import com.kgm.service.GuestReportService;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class ReportProgressDialog extends JDialog {
    public ReportProgressDialog(Window owner) {
        this(owner, null, null);
    }

    public ReportProgressDialog(Window owner, GuestReportService.ReportRange range, File target) {
        super(owner, "Generating Report", ModalityType.MODELESS);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setContentPane(content(range, target));
        setSize(500, progressDialogHeight(target));
        setLocationRelativeTo(owner);
    }

    private JPanel content(GuestReportService.ReportRange range, File target) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HomeViewHelper.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JLabel title = new JLabel("Generating Report");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        JLabel state = new JLabel("Preparing report and updating the document...");
        state.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        state.setForeground(HomeViewHelper.TEXT_SECONDARY);
        state.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea details = new JTextArea(detailsText(range, target));
        details.setEditable(false);
        details.setFocusable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(false);
        details.setRows(target == null ? 2 : Math.max(3, Math.min(6, target.getAbsolutePath().length() / 68 + 2)));
        details.setColumns(48);
        details.setOpaque(false);
        details.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        details.setForeground(HomeViewHelper.TEXT_SECONDARY);
        details.setBorder(BorderFactory.createEmptyBorder());
        details.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.setVisible(target != null);

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);
        progress.setPreferredSize(new Dimension(430, 16));
        body.add(state);
        if (target != null) {
            body.add(Box.createVerticalStrut(10));
            body.add(details);
        }
        body.add(Box.createVerticalStrut(14));
        body.add(progress);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        return root;
    }

    private String detailsText(GuestReportService.ReportRange range, File target) {
        if (range == null || target == null) {
            return "";
        }
        return "Period: " + range.label()
                + " (" + range.startDate() + " to " + range.endDate() + ")\n"
                + "Saving to: " + target.getAbsolutePath();
    }

    private int progressDialogHeight(File target) {
        if (target == null) {
            return 176;
        }
        int pathRows = Math.max(1, Math.min(4, target.getAbsolutePath().length() / 68 + 1));
        return 220 + pathRows * 18;
    }
}
