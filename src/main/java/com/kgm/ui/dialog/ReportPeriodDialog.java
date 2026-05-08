package com.kgm.ui.dialog;

import com.kgm.service.GuestReportService;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class ReportPeriodDialog extends JDialog {
    private final JToggleButton weeklyButton = periodButton("Weekly");
    private final JToggleButton monthlyButton = periodButton("Monthly");
    private final JToggleButton fortnightButton = periodButton("Fortnight");
    private final JToggleButton customButton = periodButton("Custom Range");
    private final JSpinner startDate = HomeViewHelper.dateSpinner(date(LocalDate.now().minusDays(6)));
    private final JSpinner endDate = HomeViewHelper.dateSpinner(date(LocalDate.now()));
    private GuestReportService.ReportRange selectedRange;

    public ReportPeriodDialog(Window owner) {
        super(owner, "Guest Report", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(content());
        pack();
        setMinimumSize(new Dimension(520, 440));
        setLocationRelativeTo(owner);
        weeklyButton.setSelected(true);
        updateCustomFields();
    }

    public GuestReportService.ReportRange getSelectedRange() {
        return selectedRange;
    }

    private JPanel content() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(dialogHeader(), BorderLayout.NORTH);
        root.add(dialogBody(), BorderLayout.CENTER);
        root.add(dialogFooter(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel dialogHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HomeViewHelper.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Download Guest Report");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Choose an A4 report period and generate a professional DOCX file.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(226, 239, 249));
        text.add(title);
        text.add(Box.createVerticalStrut(4));
        text.add(subtitle);

        header.add(text, BorderLayout.WEST);
        return header;
    }

    private JPanel dialogBody() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));

        JLabel periodLabel = fieldLabel("Report Period");
        periodLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(periodLabel);
        body.add(Box.createVerticalStrut(8));
        body.add(periodOptions());
        body.add(Box.createVerticalStrut(18));
        body.add(customDatePanel());
        return body;
    }

    private JPanel periodOptions() {
        ButtonGroup group = new ButtonGroup();
        group.add(weeklyButton);
        group.add(monthlyButton);
        group.add(fortnightButton);
        group.add(customButton);

        JPanel options = new JPanel(new GridLayout(2, 2, 10, 10));
        options.setOpaque(false);
        options.setAlignmentX(Component.LEFT_ALIGNMENT);
        options.add(weeklyButton);
        options.add(monthlyButton);
        options.add(fortnightButton);
        options.add(customButton);

        weeklyButton.addActionListener(event -> updateCustomFields());
        monthlyButton.addActionListener(event -> updateCustomFields());
        fortnightButton.addActionListener(event -> updateCustomFields());
        customButton.addActionListener(event -> updateCustomFields());
        return options;
    }

    private JPanel customDatePanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 12, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(dateField("Start Date", startDate));
        panel.add(dateField("End Date", endDate));
        return panel;
    }

    private JPanel dateField(String labelText, JSpinner spinner) {
        JPanel field = new JPanel();
        field.setOpaque(false);
        field.setLayout(new BoxLayout(field, BoxLayout.Y_AXIS));
        JLabel label = fieldLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        JComponent styled = HomeViewHelper.styleField(spinner, 210);
        styled.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.add(label);
        field.add(Box.createVerticalStrut(6));
        field.add(styled);
        return field;
    }

    private JPanel dialogFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setBackground(new Color(247, 249, 251));
        footer.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        JButton cancel = secondaryButton("Cancel");
        JButton generate = primaryButton("Generate Report");
        cancel.addActionListener(event -> dispose());
        generate.addActionListener(event -> selectRange());
        footer.add(cancel);
        footer.add(generate);
        return footer;
    }

    private void updateCustomFields() {
        boolean custom = customButton.isSelected();
        startDate.setEnabled(custom);
        endDate.setEnabled(custom);
    }

    private void selectRange() {
        LocalDate today = LocalDate.now();
        if (weeklyButton.isSelected()) {
            selectedRange = new GuestReportService.ReportRange("Weekly", today.minusDays(6), today);
        } else if (monthlyButton.isSelected()) {
            selectedRange = new GuestReportService.ReportRange("Monthly", today.withDayOfMonth(1), today);
        } else if (fortnightButton.isSelected()) {
            selectedRange = new GuestReportService.ReportRange("Fortnight", today.minusDays(13), today);
        } else {
            LocalDate start = localDate(startDate);
            LocalDate end = localDate(endDate);
            if (end.isBefore(start)) {
                DialogHelper.error(this, "Invalid date range", "End Date must be the same as or after Start Date.");
                return;
            }
            selectedRange = new GuestReportService.ReportRange("Custom Range", start, end);
        }
        dispose();
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setForeground(new Color(70, 82, 96));
        return label;
    }

    private static JToggleButton periodButton(String text) {
        JToggleButton button = new JToggleButton(text);
        button.setPreferredSize(new Dimension(210, 58));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        button.setForeground(HomeViewHelper.TEXT_PRIMARY);
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HomeViewHelper.BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        return button;
    }

    private static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(148, 34));
        button.setBackground(HomeViewHelper.PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(94, 34));
        button.setBackground(Color.WHITE);
        button.setForeground(HomeViewHelper.TEXT_SECONDARY);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(HomeViewHelper.BORDER));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static Date date(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate localDate(JSpinner spinner) {
        return ((Date) spinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
