package com.kgm.ui.dialog;

import com.kgm.service.GuestReportService;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

public class ReportPeriodDialog extends JDialog {
    private static final DateTimeFormatter INPUT_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final JToggleButton weeklyButton = periodButton("Weekly");
    private final JToggleButton monthlyButton = periodButton("Monthly");
    private final JToggleButton fortnightButton = periodButton("Fortnight");
    private final JToggleButton customButton = periodButton("Custom Range");
    private final JSpinner startDate = HomeViewHelper.dateSpinner(date(LocalDate.now().minusDays(6)));
    private final JSpinner endDate = HomeViewHelper.dateSpinner(date(LocalDate.now()));
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
        setMinimumSize(new Dimension(620, 500));
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
        JLabel subtitle = new JLabel("Select a report period and export PDF, Excel, or both.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(226, 239, 249));
        text.add(title);
        text.add(Box.createVerticalStrut(4));
        text.add(subtitle);

        header.add(text, BorderLayout.WEST);
        return header;
    }

    private JComponent dialogBody() {
        JPanel body = new JPanel();
        body.setBackground(new Color(247, 249, 251));
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));

        body.add(sectionCard("Report Period", periodPanel()));
        body.add(Box.createVerticalStrut(14));
        body.add(sectionCard("Export Format", formatPanel()));

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(247, 249, 251));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel sectionCard(String title, JComponent content) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HomeViewHelper.BORDER),
                BorderFactory.createEmptyBorder(14, 16, 16, 16)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel label = fieldLabel(title);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
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
        JPanel card = new JPanel(new BorderLayout(8, 2));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HomeViewHelper.BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.add(checkBox, BorderLayout.NORTH);

        JLabel help = new JLabel(description);
        help.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        help.setForeground(HomeViewHelper.TEXT_SECONDARY);
        card.add(help, BorderLayout.CENTER);
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent event) {
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
        cancel.addActionListener(event -> dispose());
        generateButton.addActionListener(event -> selectRange());
        footer.add(cancel);
        footer.add(generateButton);
        return footer;
    }

    private void updateCustomFields() {
        boolean custom = customButton.isSelected();
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
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (weeklyButton.isSelected()) {
            selectedRange = new GuestReportService.ReportRange(
                    "Weekly",
                    weekStart,
                    today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            );
        } else if (monthlyButton.isSelected()) {
            selectedRange = new GuestReportService.ReportRange(
                    "Monthly",
                    today.withDayOfMonth(1),
                    today.with(TemporalAdjusters.lastDayOfMonth())
            );
        } else if (fortnightButton.isSelected()) {
            selectedRange = new GuestReportService.ReportRange("Fortnight", weekStart, weekStart.plusDays(13));
        } else {
            LocalDate start = inputDate(startDate);
            LocalDate end = inputDate(endDate);
            if (start == null || end == null || end.isBefore(start)) {
                DialogHelper.error(
                        this,
                        "Report period needs attention",
                        "Enter a valid custom date range in dd-MM-yyyy format. The end date cannot be before the start date."
                );
                return;
            }
            startDate.setValue(date(start));
            endDate.setValue(date(end));
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

    private LocalDate inputDate(JSpinner spinner) {
        JFormattedTextField field = dateTextField(spinner);
        if (field == null) {
            return null;
        }
        String value = field.getText().trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value, INPUT_DATE);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private void installDateValidation(JSpinner spinner) {
        spinner.addChangeListener(event -> updateGenerateButtonState());
        JFormattedTextField field = dateTextField(spinner);
        if (field == null) {
            return;
        }
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                updateGenerateButtonState();
            }

            public void removeUpdate(DocumentEvent event) {
                updateGenerateButtonState();
            }

            public void changedUpdate(DocumentEvent event) {
                updateGenerateButtonState();
            }
        });
    }

    private JFormattedTextField dateTextField(JSpinner spinner) {
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            return editor.getTextField();
        }
        return null;
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setForeground(new Color(70, 82, 96));
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
        button.setBackground(selected ? new Color(232, 245, 240) : Color.WHITE);
        button.setForeground(selected ? HomeViewHelper.PRIMARY : HomeViewHelper.TEXT_PRIMARY);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? HomeViewHelper.PRIMARY : HomeViewHelper.BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
    }

    private static void styleFormatCard(JPanel card, boolean selected) {
        card.setBackground(selected ? new Color(232, 245, 240) : Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? HomeViewHelper.PRIMARY : HomeViewHelper.BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
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

    private static LocalDate localDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
