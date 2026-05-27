package com.kgm.ui.component;

import com.kgm.ui.styling.HomeViewHelper;
import com.toedter.calendar.JCalendar;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class UniversalDatePicker extends JPanel {

    private static final String DATE_FORMAT = "dd-MM-yyyy HH:mm";

    private static final Color BORDER_COLOR = new Color(200, 200, 200);
    private static final Color BORDER_FOCUS_COLOR = HomeViewHelper.PRIMARY;
    private static final Color BACKGROUND_COLOR = Color.WHITE;
    private static final Color DISABLED_BACKGROUND = new Color(248, 248, 248);
    private static final Color DISABLED_FOREGROUND = new Color(150, 150, 150);
    private static final Color ICON_COLOR = new Color(120, 120, 120);

    private static final Font DISPLAY_FONT =
            new Font("Segoe UI", Font.PLAIN, 13);

    private static final int FIELD_HEIGHT = 34;
    private static final int FIELD_WIDTH = 340;

    private static final int POPUP_WIDTH = 330;
    private static final int POPUP_HEIGHT = 330;

    private static final int ICON_SIZE = 16;

    private final JTextField displayField;
    private final JPanel iconPanel;

    private final JCalendar calendar;
    private final JPanel calendarContainer;
    private JDialog calendarDialog;

    private JSpinner hourSpinner;
    private JSpinner minuteSpinner;

    private Date selectedDate;

    private boolean enabled = true;

    private Runnable dateChangeListener;

    public UniversalDatePicker() {
        this(new Date());
    }

    public UniversalDatePicker(Date initialDate) {

        super(new BorderLayout());

        this.selectedDate =
                initialDate != null ? initialDate : new Date();

        setOpaque(false);

        setLayout(new BorderLayout());

        setBorder(createBorder());

        setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        setMinimumSize(new Dimension(260, FIELD_HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, FIELD_HEIGHT));

        displayField = new JTextField(formatDate(selectedDate));

        displayField.setFont(DISPLAY_FONT);

        displayField.setEditable(false);

        displayField.setFocusable(false);

        displayField.setBorder(null);

        displayField.setBackground(BACKGROUND_COLOR);

        displayField.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        add(displayField, BorderLayout.CENTER);

        iconPanel = new JPanel(new GridBagLayout());

        iconPanel.setOpaque(false);

        iconPanel.setPreferredSize(
                new Dimension(34, FIELD_HEIGHT)
        );

        iconPanel.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        iconPanel.add(new CalendarIcon(ICON_COLOR));

        add(iconPanel, BorderLayout.EAST);

        calendar = new JCalendar();

        calendar.setLocale(Locale.ENGLISH);

        calendar.setDate(selectedDate);

        calendar.setDecorationBackgroundColor(BACKGROUND_COLOR);

        calendar.setWeekdayForeground(
                new Color(60, 60, 60)
        );

        forceCompactCalendar(calendar);

        calendarContainer =
                new JPanel(new BorderLayout());

        calendarContainer.setBackground(BACKGROUND_COLOR);

        calendarContainer.setBorder(
                new EmptyBorder(8, 8, 8, 8)
        );

        calendarContainer.setPreferredSize(
                new Dimension(POPUP_WIDTH, POPUP_HEIGHT)
        );

        calendarContainer.setMinimumSize(
                new Dimension(POPUP_WIDTH, POPUP_HEIGHT)
        );

        calendarContainer.setMaximumSize(
                new Dimension(POPUP_WIDTH, POPUP_HEIGHT)
        );

        calendarContainer.add(calendar, BorderLayout.CENTER);

        calendarContainer.add(
                createTimePanel(),
                BorderLayout.SOUTH
        );

        MouseAdapter showCalendarListener =
                new MouseAdapter() {

                    @Override
                    public void mouseClicked(MouseEvent e) {

                        if (enabled) {
                            showCalendar();
                        }
                    }
                };

        addMouseListener(showCalendarListener);

        displayField.addMouseListener(showCalendarListener);

        iconPanel.addMouseListener(showCalendarListener);

        calendar.addPropertyChangeListener(
                "date",
                evt -> {

                    Date newDate =
                            (Date) evt.getNewValue();

                    if (newDate != null) {

                        Calendar current =
                                Calendar.getInstance();

                        current.setTime(selectedDate);

                        Calendar picked =
                                Calendar.getInstance();

                        picked.setTime(newDate);

                        current.set(
                                Calendar.YEAR,
                                picked.get(Calendar.YEAR)
                        );

                        current.set(
                                Calendar.MONTH,
                                picked.get(Calendar.MONTH)
                        );

                        current.set(
                                Calendar.DAY_OF_MONTH,
                                picked.get(Calendar.DAY_OF_MONTH)
                        );

                        setSelectedDate(current.getTime());

                        updateSpinners();
                    }
                }
        );
    }

    private void forceCompactCalendar(JCalendar calendar) {

        Dimension compactSize =
                new Dimension(310, 230);

        calendar.setPreferredSize(compactSize);

        calendar.setMinimumSize(compactSize);

        calendar.setMaximumSize(compactSize);

        calendar.setFont(
                new Font("Segoe UI", Font.PLAIN, 12)
        );

        for (Component component : calendar.getComponents()) {

            component.setFont(
                    new Font("Segoe UI", Font.PLAIN, 12)
            );
        }
    }

    private JPanel createTimePanel() {

        JPanel timePanel = new JPanel();

        timePanel.setLayout(
                new FlowLayout(FlowLayout.CENTER, 5, 5)
        );

        timePanel.setBackground(
                new Color(245, 245, 245)
        );

        timePanel.setBorder(
                new CompoundBorder(
                        new MatteBorder(
                                1,
                                0,
                                0,
                                0,
                                new Color(220, 220, 220)
                        ),
                        new EmptyBorder(4, 4, 4, 4)
                )
        );

        Calendar cal = Calendar.getInstance();

        cal.setTime(selectedDate);

        JLabel hourLabel = smallLabel("Hr");

        timePanel.add(hourLabel);

        hourSpinner = new JSpinner(
                new SpinnerNumberModel(
                        cal.get(Calendar.HOUR_OF_DAY),
                        0,
                        23,
                        1
                )
        );

        styleSpinner(hourSpinner);

        hourSpinner.setPreferredSize(
                new Dimension(44, 24)
        );

        hourSpinner.addChangeListener(
                e -> updateTimeFromSpinners()
        );

        timePanel.add(hourSpinner);

        JLabel minuteLabel = smallLabel("Min");

        timePanel.add(minuteLabel);

        minuteSpinner = new JSpinner(
                new SpinnerNumberModel(
                        cal.get(Calendar.MINUTE),
                        0,
                        59,
                        1
                )
        );

        styleSpinner(minuteSpinner);

        minuteSpinner.setPreferredSize(
                new Dimension(44, 24)
        );

        minuteSpinner.addChangeListener(
                e -> updateTimeFromSpinners()
        );

        timePanel.add(minuteSpinner);

        JButton nowButton =
                smallButton("Now", false);

        nowButton.setPreferredSize(
                new Dimension(56, 24)
        );

        nowButton.addActionListener(e -> {

            Date now = new Date();

            calendar.setDate(now);

            setSelectedDate(now);

            updateSpinners();
        });

        timePanel.add(nowButton);

        JButton doneButton =
                smallButton("OK", true);

        doneButton.setPreferredSize(
                new Dimension(52, 24)
        );

        doneButton.addActionListener(
                e -> confirmAndClose()
        );

        timePanel.add(doneButton);

        return timePanel;
    }

    private JLabel smallLabel(String text) {

        JLabel label = new JLabel(text);

        label.setFont(
                new Font("Segoe UI", Font.PLAIN, 11)
        );

        label.setForeground(
                new Color(60, 60, 60)
        );

        return label;
    }

    private void styleSpinner(JSpinner spinner) {

        spinner.setPreferredSize(
                new Dimension(48, 24)
        );

        spinner.setFont(
                new Font("Segoe UI", Font.PLAIN, 11)
        );

        JComponent editor = spinner.getEditor();

        if (editor instanceof JSpinner.DefaultEditor) {

            JTextField field =
                    ((JSpinner.DefaultEditor) editor)
                            .getTextField();

            field.setFont(
                    new Font("Segoe UI", Font.PLAIN, 11)
            );

            field.setHorizontalAlignment(
                    JTextField.CENTER
            );
        }
    }

    private JButton smallButton(
            String text,
            boolean primary
    ) {

        JButton button = new JButton(text);

        button.setFont(
                new Font("Segoe UI", Font.BOLD, 11)
        );

        button.setFocusPainted(false);

        if (primary) {

            button.setBackground(
                    HomeViewHelper.PRIMARY
            );

            button.setForeground(Color.WHITE);

            button.setBorder(
                    new EmptyBorder(4, 10, 4, 10)
            );

        } else {

            button.setBackground(
                    new Color(245, 245, 245)
            );

            button.setForeground(
                    new Color(80, 80, 80)
            );

            button.setBorder(
                    new CompoundBorder(
                            new LineBorder(
                                    new Color(170, 170, 170)
                            ),
                            new EmptyBorder(4, 10, 4, 10)
                    )
            );
        }

        return button;
    }

    private void updateTimeFromSpinners() {

        if (hourSpinner == null ||
                minuteSpinner == null) {
            return;
        }

        Calendar c = Calendar.getInstance();

        c.setTime(selectedDate);

        c.set(
                Calendar.HOUR_OF_DAY,
                (Integer) hourSpinner.getValue()
        );

        c.set(
                Calendar.MINUTE,
                (Integer) minuteSpinner.getValue()
        );

        c.set(Calendar.SECOND, 0);

        c.set(Calendar.MILLISECOND, 0);

        setSelectedDate(c.getTime());
    }

    private void confirmAndClose() {

        Date calendarDate = calendar.getDate();

        if (calendarDate != null) {

            Calendar finalDate =
                    Calendar.getInstance();

            finalDate.setTime(calendarDate);

            finalDate.set(
                    Calendar.HOUR_OF_DAY,
                    (Integer) hourSpinner.getValue()
            );

            finalDate.set(
                    Calendar.MINUTE,
                    (Integer) minuteSpinner.getValue()
            );

            finalDate.set(Calendar.SECOND, 0);

            finalDate.set(Calendar.MILLISECOND, 0);

            setSelectedDate(finalDate.getTime());
        }

        hideCalendarDialog();
    }

    private void updateSpinners() {

        if (hourSpinner == null ||
                minuteSpinner == null) {
            return;
        }

        Calendar cal = Calendar.getInstance();

        cal.setTime(selectedDate);

        hourSpinner.setValue(
                cal.get(Calendar.HOUR_OF_DAY)
        );

        minuteSpinner.setValue(
                cal.get(Calendar.MINUTE)
        );
    }

    private CompoundBorder createBorder() {

        return new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1),
                new EmptyBorder(6, 8, 6, 8)
        );
    }

    private String formatDate(Date date) {

        if (date == null) {
            return "";
        }

        return new SimpleDateFormat(
                DATE_FORMAT
        ).format(date);
    }

    private void showCalendar() {

        if (!enabled) {
            return;
        }

        ensureCalendarDialog();

        calendar.setDate(selectedDate);

        updateSpinners();

        Point screenPoint = getLocationOnScreen();

        Rectangle screen =
                GraphicsEnvironment
                        .getLocalGraphicsEnvironment()
                        .getMaximumWindowBounds();

        int x = screenPoint.x;

        int y = screenPoint.y + getHeight() + 2;

        if (x + POPUP_WIDTH >
                screen.x + screen.width) {

            x =
                    screen.x +
                            screen.width -
                            POPUP_WIDTH;
        }

        if (y + POPUP_HEIGHT >
                screen.y + screen.height) {

            y =
                    screenPoint.y -
                            POPUP_HEIGHT -
                            2;
        }

        calendarDialog.setLocation(x, y);

        calendarDialog.setVisible(true);

        calendarDialog.toFront();

        calendarDialog.requestFocus();
    }

    private void ensureCalendarDialog() {

        Window owner = SwingUtilities.getWindowAncestor(this);

        if (calendarDialog != null && calendarDialog.getOwner() == owner) {
            return;
        }

        if (calendarDialog != null) {
            calendarDialog.setVisible(false);
            calendarDialog.remove(calendarContainer);
            calendarDialog.dispose();
        }

        calendarDialog = new JDialog(owner);

        calendarDialog.setUndecorated(true);

        calendarDialog.setModal(false);

        calendarDialog.setModalityType(Dialog.ModalityType.MODELESS);

        calendarDialog.setAlwaysOnTop(false);

        calendarDialog.setLayout(new BorderLayout());

        calendarDialog.getRootPane().setBorder(
                new LineBorder(new Color(190, 190, 190), 1)
        );

        calendarDialog.add(calendarContainer, BorderLayout.CENTER);

        calendarDialog.setSize(POPUP_WIDTH, POPUP_HEIGHT);
    }

    private void hideCalendarDialog() {

        if (calendarDialog != null) {
            calendarDialog.setVisible(false);
        }
    }

    public Date getDate() {
        return selectedDate;
    }

    public void setDate(Date date) {

        setSelectedDate(date);

        calendar.setDate(this.selectedDate);

        updateSpinners();
    }

    private void setSelectedDate(Date date) {

        if (date == null) {
            date = new Date();
        }

        this.selectedDate = date;

        displayField.setText(
                formatDate(date)
        );

        if (dateChangeListener != null) {
            dateChangeListener.run();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {

        this.enabled = enabled;

        super.setEnabled(enabled);

        displayField.setEnabled(enabled);

        iconPanel.setEnabled(enabled);

        if (enabled) {

            displayField.setCursor(
                    Cursor.getPredefinedCursor(
                            Cursor.HAND_CURSOR
                    )
            );

            iconPanel.setCursor(
                    Cursor.getPredefinedCursor(
                            Cursor.HAND_CURSOR
                    )
            );

            displayField.setBackground(
                    BACKGROUND_COLOR
            );

            displayField.setForeground(HomeViewHelper.TEXT_PRIMARY);

            iconPanel.setVisible(true);

        } else {

            displayField.setCursor(
                    Cursor.getDefaultCursor()
            );

            iconPanel.setCursor(
                    Cursor.getDefaultCursor()
            );

            displayField.setBackground(
                    DISABLED_BACKGROUND
            );

            displayField.setForeground(
                    DISABLED_FOREGROUND
            );

            iconPanel.setVisible(false);

            hideCalendarDialog();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setFocus(boolean focus) {

        if (focus) {

            setBorder(
                    new CompoundBorder(
                            new LineBorder(
                                    BORDER_FOCUS_COLOR,
                                    2
                            ),
                            new EmptyBorder(
                                    5,
                                    7,
                                    5,
                                    7
                            )
                    )
            );

        } else {

            setBorder(createBorder());
        }
    }

    public void addDateChangeListener(
            Runnable onChange
    ) {

        this.dateChangeListener = onChange;
    }

    public void applyProjectStyling() {

        setPreferredSize(
                new Dimension(FIELD_WIDTH, FIELD_HEIGHT)
        );

        setMinimumSize(
                new Dimension(260, FIELD_HEIGHT)
        );

        setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        FIELD_HEIGHT
                )
        );

        setFont(DISPLAY_FONT);

        setBackground(BACKGROUND_COLOR);

        setBorder(
                new CompoundBorder(
                        new RoundedBorder(16),
                        new EmptyBorder(6, 8, 6, 8)
                )
        );
    }

    private static class CalendarIcon extends JLabel {

        private final Color iconColor;

        CalendarIcon(Color color) {

            this.iconColor = color;

            setPreferredSize(
                    new Dimension(ICON_SIZE, ICON_SIZE)
            );
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            Graphics2D g2 =
                    (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            int x =
                    (getWidth() - ICON_SIZE) / 2;

            int y =
                    (getHeight() - ICON_SIZE) / 2;

            int w = ICON_SIZE;

            int h = ICON_SIZE;

            g2.setColor(iconColor);

            g2.setStroke(
                    new BasicStroke(
                            1.5f,
                            BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND
                    )
            );

            g2.drawRoundRect(
                    x,
                    y + 4,
                    w,
                    h - 4,
                    2,
                    2
            );

            g2.fillRect(x, y + 4, w, 4);

            g2.drawLine(
                    x + 3,
                    y,
                    x + 3,
                    y + 6
            );

            g2.drawLine(
                    x + w - 3,
                    y,
                    x + w - 3,
                    y + 6
            );

            g2.drawLine(
                    x + w / 3,
                    y + 8,
                    x + w / 3,
                    y + h - 2
            );

            g2.drawLine(
                    x + 2 * w / 3,
                    y + 8,
                    x + 2 * w / 3,
                    y + h - 2
            );

            g2.drawLine(
                    x + 2,
                    y + 12,
                    x + w - 2,
                    y + 12
            );

            g2.dispose();
        }
    }

    public static class RoundedBorder
            extends AbstractBorder {

        private final int radius;

        public RoundedBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public void paintBorder(
                Component component,
                Graphics g,
                int x,
                int y,
                int width,
                int height
        ) {

            Graphics2D g2 =
                    (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(BORDER_COLOR);

            g2.drawRoundRect(
                    x,
                    y,
                    width - 1,
                    height - 1,
                    radius,
                    radius
            );

            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(
                Component component
        ) {

            return new Insets(4, 4, 4, 4);
        }
    }
}
