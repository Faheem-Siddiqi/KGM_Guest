package com.kgm.ui.dialog;

import com.kgm.service.GuestReportService;
import com.kgm.ui.component.UniversalDatePicker;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

public class ReportPeriodDialog extends JDialog {
    private static final Color BACKGROUND = new Color(248, 250, 252);
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color SELECTED_SURFACE = new Color(239, 246, 255);
    private static final Color SELECTED_BORDER = new Color(147, 197, 253);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color SUBTLE_TEXT = new Color(100, 116, 139);
    private static final int RADIUS = 10;

    private final JToggleButton weeklyButton = periodButton("Weekly");
    private final JToggleButton monthlyButton = periodButton("Monthly");
    private final JToggleButton fortnightButton = periodButton("Fortnight");
    private final JToggleButton customButton = periodButton("Custom Range");
    private final UniversalDatePicker startDate = new UniversalDatePicker(date(LocalDate.now().minusDays(6)));
    private final UniversalDatePicker endDate = new UniversalDatePicker(date(LocalDate.now()));
    private final JButton generateButton = primaryButton("Generate Reports");
    private final JCheckBox pdfCheck = formatCheck("PDF", true);
    private final JCheckBox excelCheck = formatCheck("Excel", false);
    private GuestReportService.ReportRange selectedRange;
    private GuestReportService.ReportExportRequest selectedRequest;

    public ReportPeriodDialog(Window owner) {
        super(owner, "Guest Report", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(content());
        pack();
        setMinimumSize(new Dimension(660, 560));
        setResizable(true);
        setLocationRelativeTo(owner);
        weeklyButton.setSelected(true);
        installDateValidation(startDate);
        installDateValidation(endDate);
        updateCustomFields();
    }

    public GuestReportService.ReportRange getSelectedRange() {
        return selectedRange;
    }

    public GuestReportService.ReportExportRequest getSelectedRequest() {
        return selectedRequest;
    }

    private JPanel content() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        root.add(dialogHeader(), BorderLayout.NORTH);
        root.add(dialogBody(), BorderLayout.CENTER);
        root.add(dialogFooter(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel dialogHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CARD_BACKGROUND);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(20, 24, 18, 24)
        ));

        JPanel text = new JPanel(new BorderLayout());
        text.setOpaque(false);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Download Guest Report");
        title.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 20));
        title.setForeground(HomeViewHelper.TEXT_PRIMARY);
        JLabel subtitle = new JLabel("Select a report period and export PDF, Excel, or both.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(SUBTLE_TEXT);
        copy.add(title);
        copy.add(Box.createVerticalStrut(4));
        copy.add(subtitle);

        text.add(copy, BorderLayout.CENTER);
        header.add(text, BorderLayout.WEST);
        return header;
    }

    private JComponent dialogBody() {
        JPanel body = new JPanel();
        body.setBackground(BACKGROUND);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 18, 24));

        body.add(sectionCard("Report Period", periodPanel()));
        body.add(Box.createVerticalStrut(14));
        body.add(sectionCard("Export Format", formatPanel()));

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BACKGROUND);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel sectionCard(String title, JComponent content) {
        JPanel card = new RoundedPanel(CARD_BACKGROUND, BORDER, RADIUS);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel label = fieldLabel(title);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        card.add(label, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel periodPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(periodOptions());
        panel.add(Box.createVerticalStrut(14));
        panel.add(customDatePanel());
        return panel;
    }

    private JPanel periodOptions() {
        ButtonGroup group = new ButtonGroup();
        group.add(weeklyButton);
        group.add(monthlyButton);
        group.add(fortnightButton);
        group.add(customButton);

        JPanel options = new JPanel(new GridLayout(2, 2, 12, 12));
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

    private JPanel formatPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel options = new JPanel(new GridLayout(1, 2, 12, 0));
        options.setOpaque(false);
        options.setAlignmentX(Component.LEFT_ALIGNMENT);
        options.add(formatCard(pdfCheck, "Portable PDF report"));
        options.add(formatCard(excelCheck, "Editable Excel workbook"));

        panel.add(options);
        return panel;
    }

    private JPanel formatCard(JCheckBox checkBox, String description) {
        JPanel card = new RoundedPanel(CARD_BACKGROUND, BORDER, RADIUS);
        card.setLayout(new BorderLayout(8, 4));
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.add(checkBox, BorderLayout.NORTH);

        JLabel help = new JLabel(description);
        help.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        help.setForeground(SUBTLE_TEXT);
        card.add(help, BorderLayout.CENTER);
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getSource() == checkBox) {
                    return;
                }
                checkBox.setSelected(!checkBox.isSelected());
                updateGenerateButtonState();
            }
        });
        checkBox.addItemListener(event -> {
            styleFormatCard(card, checkBox.isSelected());
            updateGenerateButtonState();
        });
        styleFormatCard(card, checkBox.isSelected());
        return card;
    }

    private static JCheckBox formatCheck(String text, boolean selected) {
        JCheckBox checkBox = new JCheckBox(text, selected);
        checkBox.setOpaque(false);
        checkBox.setFocusPainted(false);
        checkBox.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        checkBox.setForeground(HomeViewHelper.TEXT_PRIMARY);
        checkBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return checkBox;
    }

    private JPanel dateField(String labelText, UniversalDatePicker picker) {
        JPanel field = new JPanel();
        field.setOpaque(false);
        field.setLayout(new BoxLayout(field, BoxLayout.Y_AXIS));
        JLabel label = fieldLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        picker.setPreferredSize(new Dimension(240, 38));
        picker.setMinimumSize(new Dimension(210, 38));
        picker.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        picker.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.add(label);
        field.add(Box.createVerticalStrut(7));
        field.add(picker);
        return field;
    }

    private JPanel dialogFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setBackground(CARD_BACKGROUND);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                BorderFactory.createEmptyBorder(14, 24, 14, 24)
        ));

        JButton cancel = secondaryButton("Cancel");
        cancel.addActionListener(event -> dispose());
        generateButton.addActionListener(event -> selectRange());
        footer.add(cancel);
        footer.add(generateButton);
        getRootPane().setDefaultButton(generateButton);
        return footer;
    }

    private void updateCustomFields() {
        boolean custom = customButton.isSelected();
        GuestReportService.ReportRange presetRange = selectedPresetRange();
        if (presetRange != null) {
            startDate.setDate(date(presetRange.startDate()));
            endDate.setDate(date(presetRange.endDate()));
        }
        startDate.setEnabled(custom);
        endDate.setEnabled(custom);
        refreshPeriodButtonStyles();
        updateGenerateButtonState();
    }

    private void selectRange() {
        if (!hasSelectedFormat()) {
            DialogHelper.error(
                    this,
                    "Report format needed",
                    "Choose PDF, Excel, or both before generating the report."
            );
            return;
        }
        GuestReportService.ReportRange presetRange = selectedPresetRange();
        if (presetRange != null) {
            selectedRange = presetRange;
        } else {
            LocalDate start = inputDate(startDate);
            LocalDate end = inputDate(endDate);
            if (start == null || end == null || end.isBefore(start)) {
                DialogHelper.error(
                        this,
                        "Report period needs attention",
                        "Choose a valid custom date range. The end date cannot be before the start date."
                );
                return;
            }
            startDate.setDate(date(start));
            endDate.setDate(date(end));
            selectedRange = new GuestReportService.ReportRange("Custom Range", start, end);
        }
        selectedRequest = new GuestReportService.ReportExportRequest(
                selectedRange,
                pdfCheck.isSelected(),
                excelCheck.isSelected(),
                null
        );
        dispose();
    }

    private GuestReportService.ReportRange selectedPresetRange() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (weeklyButton.isSelected()) {
            return new GuestReportService.ReportRange(
                    "Weekly",
                    weekStart,
                    today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            );
        }
        if (monthlyButton.isSelected()) {
            return new GuestReportService.ReportRange(
                    "Monthly",
                    today.withDayOfMonth(1),
                    today.with(TemporalAdjusters.lastDayOfMonth())
            );
        }
        if (fortnightButton.isSelected()) {
            return new GuestReportService.ReportRange("Fortnight", weekStart, weekStart.plusDays(13));
        }
        return null;
    }

    private void updateGenerateButtonState() {
        generateButton.setEnabled(true);
        generateButton.setBackground(HomeViewHelper.PRIMARY);
        generateButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private boolean hasSelectedFormat() {
        return pdfCheck.isSelected() || excelCheck.isSelected();
    }

    private boolean customDateRangeValid() {
        LocalDate start = inputDate(startDate);
        LocalDate end = inputDate(endDate);
        return start != null && end != null && !end.isBefore(start);
    }

    private LocalDate inputDate(UniversalDatePicker picker) {
        Date value = picker == null ? null : picker.getDate();
        return value == null ? null : localDate(value);
    }

    private void installDateValidation(UniversalDatePicker picker) {
        picker.addDateChangeListener(this::updateGenerateButtonState);
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setForeground(HomeViewHelper.TEXT_PRIMARY);
        return label;
    }

    private void refreshPeriodButtonStyles() {
        stylePeriodButton(weeklyButton);
        stylePeriodButton(monthlyButton);
        stylePeriodButton(fortnightButton);
        stylePeriodButton(customButton);
    }

    private static void stylePeriodButton(JToggleButton button) {
        boolean selected = button.isSelected();
        button.setForeground(selected ? HomeViewHelper.PRIMARY : HomeViewHelper.TEXT_PRIMARY);
        button.repaint();
    }

    private static void styleFormatCard(JPanel card, boolean selected) {
        card.putClientProperty("selected", selected);
        card.repaint();
    }

    private static JToggleButton periodButton(String text) {
        JToggleButton button = new PeriodToggleButton(text);
        button.setPreferredSize(new Dimension(210, 58));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        button.setForeground(HomeViewHelper.TEXT_PRIMARY);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        return button;
    }

    private static JButton primaryButton(String text) {
        JButton button = new RoundedButton(text, HomeViewHelper.PRIMARY, HomeViewHelper.PRIMARY_DARK, Color.WHITE);
        button.setPreferredSize(new Dimension(158, 38));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static JButton secondaryButton(String text) {
        JButton button = new RoundedButton(text, Color.WHITE, BORDER, HomeViewHelper.TEXT_SECONDARY);
        button.setPreferredSize(new Dimension(96, 38));
        button.setForeground(HomeViewHelper.TEXT_SECONDARY);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static class RoundedPanel extends JPanel {
        private final Color background;
        private final Color border;
        private final int radius;

        private RoundedPanel(Color background, Color border, int radius) {
            this.background = background;
            this.border = border;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean selected = Boolean.TRUE.equals(getClientProperty("selected"));
            Color fill = selected ? SELECTED_SURFACE : background;
            Color stroke = selected ? SELECTED_BORDER : border;

            g2.setColor(new Color(15, 23, 42, selected ? 10 : 6));
            g2.fillRoundRect(1, 2, getWidth() - 3, getHeight() - 4, radius, radius);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 3, radius, radius);
            g2.setColor(stroke);
            g2.drawRoundRect(0, 0, getWidth() - 2, getHeight() - 3, radius, radius);

            if (selected) {
                g2.setColor(HomeViewHelper.PRIMARY);
                g2.fillRoundRect(0, 0, 4, getHeight() - 3, 4, 4);
            }
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class PeriodToggleButton extends JToggleButton {
        private PeriodToggleButton(String text) {
            super(text);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean selected = isSelected();
            g2.setColor(selected ? SELECTED_SURFACE : Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS);
            g2.setColor(selected ? SELECTED_BORDER : BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS);

            if (selected) {
                g2.setColor(HomeViewHelper.PRIMARY);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
            }

            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class RoundedButton extends JButton {
        private final Color fill;
        private final Color stroke;
        private final Color text;

        private RoundedButton(String label, Color fill, Color stroke, Color text) {
            super(label);
            this.fill = fill;
            this.stroke = stroke;
            this.text = text;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.setColor(stroke);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.dispose();
            setForeground(text);
            super.paintComponent(graphics);
        }
    }

    private static Date date(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate localDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
