package com.kgm.ui.component;

import com.kgm.ui.styling.HomeViewHelper;
import com.toedter.calendar.JCalendar;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class UniversalDateRangePicker extends JPanel {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int FIELD_HEIGHT = 34;
    private static final int FIELD_WIDTH = 250;
    private static final int ICON_SIZE = 16;
    private static final Color BORDER_COLOR = new Color(200, 200, 200);
    private static final Color ICON_COLOR = HomeViewHelper.TEXT_SECONDARY;

    private final JLabel displayLabel = new JLabel();
    private final JLabel iconLabel = new CalendarIcon();
    private final JCalendar startCalendar = new JCalendar();
    private final JCalendar endCalendar = new JCalendar();
    private final JDialog rangeDialog;

    private LocalDate startDate;
    private LocalDate endDate;
    private Runnable rangeChangeListener;
    private boolean enabled = true;

    public UniversalDateRangePicker() {
        super(new BorderLayout(8, 0));
        setOpaque(true);
        setBackground(Color.WHITE);
        setBorder(fieldBorder());
        setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        setMinimumSize(new Dimension(220, FIELD_HEIGHT));
        setMaximumSize(new Dimension(300, FIELD_HEIGHT));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        displayLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        displayLabel.setForeground(HomeViewHelper.TEXT_PRIMARY);
        displayLabel.setBorder(new EmptyBorder(0, 0, 0, 0));
        add(displayLabel, BorderLayout.CENTER);

        iconLabel.setPreferredSize(new Dimension(26, FIELD_HEIGHT));
        iconLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add(iconLabel, BorderLayout.EAST);

        styleCalendar(startCalendar);
        styleCalendar(endCalendar);

        rangeDialog = new JDialog((Window) null);
        rangeDialog.setUndecorated(true);
        rangeDialog.setModal(false);
        rangeDialog.setLayout(new BorderLayout());
        rangeDialog.getRootPane().setBorder(new LineBorder(new Color(190, 190, 190), 1));
        rangeDialog.add(createPickerPanel(), BorderLayout.CENTER);
        rangeDialog.pack();

        MouseAdapter openListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                showCalendarDialog();
            }
        };
        addMouseListener(openListener);
        displayLabel.addMouseListener(openListener);
        iconLabel.addMouseListener(openListener);

        updateDisplay();
    }

    public DateRange getDateRange() {
        return new DateRange(startDate, endDate).normalized();
    }

    public String getFilterText() {
        return getDateRange().displayText();
    }

    public boolean hasSelection() {
        return getDateRange().hasSelection();
    }

    public void clearRange() {
        setDateRange(null, null);
    }

    public void addRangeChangeListener(Runnable onChange) {
        this.rangeChangeListener = onChange;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        super.setEnabled(enabled);
        displayLabel.setEnabled(enabled);
        iconLabel.setEnabled(enabled);
        setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        iconLabel.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        setBackground(enabled ? Color.WHITE : new Color(248, 248, 248));
        displayLabel.setForeground(enabled ? HomeViewHelper.TEXT_PRIMARY : new Color(150, 150, 150));
        if (!enabled) {
            rangeDialog.setVisible(false);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private JPanel createPickerPanel() {
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel calendars = new JPanel(new GridLayout(1, 2, 12, 0));
        calendars.setOpaque(false);
        calendars.add(calendarBlock("From", startCalendar));
        calendars.add(calendarBlock("To", endCalendar));
        content.add(calendars, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton clear = textButton("Clear", false);
        clear.addActionListener(event -> {
            clearRange();
            rangeDialog.setVisible(false);
        });

        JButton apply = textButton("Apply", true);
        apply.addActionListener(event -> {
            LocalDate start = toLocalDate(startCalendar.getDate());
            LocalDate end = toLocalDate(endCalendar.getDate());
            setDateRange(start, end);
            rangeDialog.setVisible(false);
        });

        actions.add(clear);
        actions.add(apply);
        content.add(actions, BorderLayout.SOUTH);
        return content;
    }

    private JPanel calendarBlock(String title, JCalendar calendar) {
        JPanel block = new JPanel(new BorderLayout(0, 6));
        block.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setForeground(HomeViewHelper.TEXT_SECONDARY);
        block.add(label, BorderLayout.NORTH);
        block.add(calendar, BorderLayout.CENTER);
        return block;
    }

    private JButton textButton(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (primary) {
            button.setForeground(Color.WHITE);
            button.setBackground(HomeViewHelper.PRIMARY);
            button.setBorder(new EmptyBorder(7, 16, 7, 16));
        } else {
            button.setForeground(HomeViewHelper.PRIMARY);
            button.setBackground(Color.WHITE);
            button.setBorder(new CompoundBorder(
                    new LineBorder(HomeViewHelper.BORDER),
                    new EmptyBorder(6, 14, 6, 14)
            ));
        }
        return button;
    }

    private void showCalendarDialog() {
        if (!enabled) {
            return;
        }

        LocalDate today = LocalDate.now();
        DateRange range = getDateRange();
        startCalendar.setDate(toDate(range.startOr(today)));
        endCalendar.setDate(toDate(range.endOr(range.startOr(today))));

        Point screenPoint = getLocationOnScreen();
        Dimension dialogSize = rangeDialog.getSize();
        Rectangle screen = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();

        int x = screenPoint.x;
        int y = screenPoint.y + getHeight() + 2;
        if (x + dialogSize.width > screen.x + screen.width) {
            x = screen.x + screen.width - dialogSize.width;
        }
        if (y + dialogSize.height > screen.y + screen.height) {
            y = screenPoint.y - dialogSize.height - 2;
        }

        rangeDialog.setLocation(Math.max(screen.x, x), Math.max(screen.y, y));
        rangeDialog.setVisible(true);
    }

    private void setDateRange(LocalDate start, LocalDate end) {
        DateRange normalized = new DateRange(start, end).normalized();
        boolean changed = !sameDate(startDate, normalized.startDate())
                || !sameDate(endDate, normalized.endDate());
        startDate = normalized.startDate();
        endDate = normalized.endDate();
        updateDisplay();
        if (changed && rangeChangeListener != null) {
            rangeChangeListener.run();
        }
    }

    private boolean sameDate(LocalDate first, LocalDate second) {
        return first == null ? second == null : first.equals(second);
    }

    private void updateDisplay() {
        DateRange range = getDateRange();
        displayLabel.setText(range.hasSelection() ? range.displayText() : "Any date");
        displayLabel.setToolTipText(range.hasSelection() ? range.displayText() : "Filter by arrival/departure range");
    }

    private CompoundBorder fieldBorder() {
        return new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1),
                new EmptyBorder(6, 8, 6, 8)
        );
    }

    private void styleCalendar(JCalendar calendar) {
        calendar.setLocale(Locale.ENGLISH);
        calendar.setBackground(Color.WHITE);
        calendar.setDecorationBackgroundColor(Color.WHITE);
        calendar.setWeekdayForeground(HomeViewHelper.TEXT_SECONDARY);
        calendar.setSundayForeground(HomeViewHelper.KPI_ROSE_DARK);
        calendar.setPreferredSize(new Dimension(250, 220));
        calendar.setMinimumSize(new Dimension(250, 220));
        calendar.setMaximumSize(new Dimension(250, 220));
        styleChildren(calendar);
    }

    private void styleChildren(Component component) {
        component.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                styleChildren(child);
            }
        }
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static class CalendarIcon extends JLabel {
        public void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x = (getWidth() - ICON_SIZE) / 2;
            int y = (getHeight() - ICON_SIZE) / 2;
            g2.setColor(ICON_COLOR);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(x, y + 4, ICON_SIZE, ICON_SIZE - 4, 2, 2);
            g2.fillRect(x, y + 4, ICON_SIZE, 4);
            g2.drawLine(x + 3, y, x + 3, y + 6);
            g2.drawLine(x + ICON_SIZE - 3, y, x + ICON_SIZE - 3, y + 6);
            g2.drawLine(x + 3, y + 10, x + ICON_SIZE - 3, y + 10);
            g2.drawLine(x + 3, y + 13, x + ICON_SIZE - 3, y + 13);
            g2.dispose();
        }
    }

    public record DateRange(LocalDate startDate, LocalDate endDate) {
        public static DateRange empty() {
            return new DateRange(null, null);
        }

        public static DateRange single(LocalDate date) {
            return new DateRange(date, date);
        }

        public boolean hasSelection() {
            return startDate != null || endDate != null;
        }

        public boolean isEmpty() {
            return !hasSelection();
        }

        public DateRange normalized() {
            if (startDate == null && endDate == null) {
                return this;
            }
            if (startDate == null) {
                return new DateRange(endDate, endDate);
            }
            if (endDate == null) {
                return new DateRange(startDate, startDate);
            }
            if (endDate.isBefore(startDate)) {
                return new DateRange(endDate, startDate);
            }
            return this;
        }

        public LocalDate startOr(LocalDate fallback) {
            DateRange normalized = normalized();
            return normalized.startDate == null ? fallback : normalized.startDate;
        }

        public LocalDate endOr(LocalDate fallback) {
            DateRange normalized = normalized();
            return normalized.endDate == null ? fallback : normalized.endDate;
        }

        public String displayText() {
            DateRange normalized = normalized();
            if (!normalized.hasSelection()) {
                return "";
            }
            String start = DATE_FORMAT.format(normalized.startDate);
            String end = DATE_FORMAT.format(normalized.endDate);
            return start.equals(end) ? start : start + " to " + end;
        }
    }
}
