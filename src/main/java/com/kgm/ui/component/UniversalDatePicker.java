package com.kgm.ui.component;

import com.toedter.calendar.JCalendar;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * A universal date picker component that wraps JCalendar with project-consistent styling.
 * Maintains the date format "yyyy-MM-dd HH:mm" for database compatibility.
 * 
 * Usage:
 * <pre>
 * UniversalDatePicker datePicker = new UniversalDatePicker(new Date());
 * // Get the selected date as java.util.Date
 * Date selectedDate = datePicker.getDate();
 * // Set a date programmatically
 * datePicker.setDate(new Date());
 * </pre>
 */
public class UniversalDatePicker extends JPanel {
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";
    private static final Color BORDER_COLOR = new Color(200, 200, 200);
    private static final Color BORDER_FOCUS_COLOR = new Color(0, 112, 210);
    private static final Color BACKGROUND_COLOR = Color.WHITE;
    private static final Font DISPLAY_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final int COMPONENT_HEIGHT = 34;
    private static final int CALENDAR_WIDTH = 280;
    private static final int CALENDAR_HEIGHT = 250;

    private final JTextField displayField;
    private final JCalendar calendar;
    private final JDialog calendarDialog;
    private JSpinner hourSpinner;
    private JSpinner minuteSpinner;
    private Date selectedDate;
    private boolean isFocused = false;
    private Runnable dateChangeListener;

    public UniversalDatePicker() {
        this(new Date());
    }

    public UniversalDatePicker(Date initialDate) {
        super(new BorderLayout());
        this.selectedDate = initialDate != null ? initialDate : new Date();
        
        setOpaque(false);
        setBorder(createBorder());
        setPreferredSize(new Dimension(340, COMPONENT_HEIGHT));
        setMinimumSize(new Dimension(280, COMPONENT_HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, COMPONENT_HEIGHT));
        setBackground(BACKGROUND_COLOR);

        // Create display field
        displayField = new JTextField(formatDate(selectedDate));
        displayField.setFont(DISPLAY_FONT);
        displayField.setBackground(BACKGROUND_COLOR);
        displayField.setEditable(false);
        displayField.setFocusable(false);
        displayField.setBorder(null);
        displayField.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add(displayField, BorderLayout.CENTER);

        // Create calendar
        calendar = new JCalendar();
        calendar.setLocale(Locale.ENGLISH);
        calendar.setDecorationBackgroundColor(BACKGROUND_COLOR);
        calendar.setWeekdayForeground(new Color(60, 60, 60));
        calendar.setDate(selectedDate);
        
        // Add time spinner panel
        JPanel timePanel = createTimePanel();
        calendar.add(timePanel, BorderLayout.SOUTH);

        // Create dialog for calendar
        JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(this);
        calendarDialog = new JDialog(owner != null ? owner : new JFrame());
        calendarDialog.setUndecorated(true);
        calendarDialog.setBackground(BACKGROUND_COLOR);
        calendarDialog.getContentPane().setBackground(BACKGROUND_COLOR);
        calendarDialog.add(calendar);
        calendarDialog.pack();
        calendarDialog.setSize(CALENDAR_WIDTH, CALENDAR_HEIGHT + 60);

        // Add click listener to show calendar
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showCalendar();
            }
        });

        displayField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showCalendar();
            }
        });

        // Listen for date changes from calendar
        calendar.addPropertyChangeListener("date", evt -> {
            Date newDate = (Date) evt.getNewValue();
            if (newDate != null) {
                // Preserve the time from the current selectedDate
                Calendar cal = Calendar.getInstance();
                cal.setTime(selectedDate);
                Calendar newCal = Calendar.getInstance();
                newCal.setTime(newDate);
                // Keep the hour and minute from the original date
                cal.set(Calendar.YEAR, newCal.get(Calendar.YEAR));
                cal.set(Calendar.MONTH, newCal.get(Calendar.MONTH));
                cal.set(Calendar.DAY_OF_MONTH, newCal.get(Calendar.DAY_OF_MONTH));
                setSelectedDate(cal.getTime());
                // Update the time spinners to reflect current time
                updateSpinners();
            }
        });
    }

    private JPanel createTimePanel() {
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        timePanel.setBackground(new Color(245, 245, 245));
        timePanel.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
                new EmptyBorder(8, 8, 8, 8)
        ));

        // Hour spinner
        JLabel hourLabel = new JLabel("Hour:");
        hourLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hourLabel.setForeground(new Color(60, 60, 60));
        timePanel.add(hourLabel);

        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedDate);
        
        SpinnerNumberModel hourModel = new SpinnerNumberModel(cal.get(Calendar.HOUR_OF_DAY), 0, 23, 1);
        hourSpinner = new JSpinner(hourModel);
        hourSpinner.setPreferredSize(new Dimension(50, 25));
        hourSpinner.addChangeListener(e -> {
            Calendar c = Calendar.getInstance();
            c.setTime(selectedDate);
            c.set(Calendar.HOUR_OF_DAY, (Integer) hourSpinner.getValue());
            setSelectedDate(c.getTime());
        });
        timePanel.add(hourSpinner);

        // Minute spinner
        JLabel minuteLabel = new JLabel("Min:");
        minuteLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        minuteLabel.setForeground(new Color(60, 60, 60));
        timePanel.add(minuteLabel);

        SpinnerNumberModel minuteModel = new SpinnerNumberModel(cal.get(Calendar.MINUTE), 0, 59, 1);
        minuteSpinner = new JSpinner(minuteModel);
        minuteSpinner.setPreferredSize(new Dimension(50, 25));
        minuteSpinner.addChangeListener(e -> {
            Calendar c = Calendar.getInstance();
            c.setTime(selectedDate);
            c.set(Calendar.MINUTE, (Integer) minuteSpinner.getValue());
            setSelectedDate(c.getTime());
        });
        timePanel.add(minuteSpinner);

        // Done button
        JButton doneButton = new JButton("Done");
        doneButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
        doneButton.setBackground(new Color(0, 112, 210));
        doneButton.setForeground(Color.WHITE);
        doneButton.setFocusPainted(false);
        doneButton.setBorder(new EmptyBorder(4, 12, 4, 12));
        doneButton.addActionListener(e -> {
            // Update the selected date from the calendar and spinners
            Date calendarDate = calendar.getDate();
            if (calendarDate != null) {
                Calendar cal2 = Calendar.getInstance();
                cal2.setTime(selectedDate);
                Calendar newCal = Calendar.getInstance();
                newCal.setTime(calendarDate);
                // Take date from calendar
                cal2.set(Calendar.YEAR, newCal.get(Calendar.YEAR));
                cal2.set(Calendar.MONTH, newCal.get(Calendar.MONTH));
                cal2.set(Calendar.DAY_OF_MONTH, newCal.get(Calendar.DAY_OF_MONTH));
                // Take time from spinners
                cal2.set(Calendar.HOUR_OF_DAY, (Integer) hourSpinner.getValue());
                cal2.set(Calendar.MINUTE, (Integer) minuteSpinner.getValue());
                setSelectedDate(cal2.getTime());
            }
            calendarDialog.dispose();
        });
        timePanel.add(doneButton);

        return timePanel;
    }

    private void updateSpinners() {
        if (hourSpinner != null && minuteSpinner != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(selectedDate);
            hourSpinner.setValue(cal.get(Calendar.HOUR_OF_DAY));
            minuteSpinner.setValue(cal.get(Calendar.MINUTE));
        }
    }

    private CompoundBorder createBorder() {
        return new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1),
                new EmptyBorder(6, 8, 6, 8)
        );
    }

    private String formatDate(Date date) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        return sdf.format(date);
    }

    private void showCalendar() {
        // Update spinners to reflect current time
        updateSpinners();
        
        // Position dialog near the component
        Point location = getLocationOnScreen();
        int x = location.x;
        int y = location.y + COMPONENT_HEIGHT + 2;
        
        // Ensure dialog doesn't go off screen
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle screenBounds = ge.getMaximumWindowBounds();
        if (x + CALENDAR_WIDTH > screenBounds.x + screenBounds.width) {
            x = screenBounds.x + screenBounds.width - CALENDAR_WIDTH;
        }
        if (y + calendarDialog.getHeight() > screenBounds.y + screenBounds.height) {
            y = location.y - calendarDialog.getHeight();
        }
        
        calendarDialog.setLocation(x, y);
        calendar.setDate(selectedDate);
        calendarDialog.setVisible(true);
    }

    public Date getDate() {
        return selectedDate;
    }

    public void setDate(Date date) {
        setSelectedDate(date);
    }

    private void setSelectedDate(Date date) {
        if (date == null) {
            date = new Date();
        }
        this.selectedDate = date;
        displayField.setText(formatDate(date));
        
        // Fire the date change listener
        if (dateChangeListener != null) {
            dateChangeListener.run();
        }
    }

    public void setFocus(boolean focus) {
        this.isFocused = focus;
        if (focus) {
            setBorder(new CompoundBorder(
                    new LineBorder(BORDER_FOCUS_COLOR, 2),
                    new EmptyBorder(6, 8, 6, 8)
            ));
        } else {
            setBorder(createBorder());
        }
    }

    public void addDateChangeListener(Runnable onChange) {
        this.dateChangeListener = onChange;
    }

    // Style the date picker to match AddGuestHelper.styleField
    public void applyProjectStyling() {
        setPreferredSize(new Dimension(340, COMPONENT_HEIGHT));
        setMinimumSize(new Dimension(280, COMPONENT_HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, COMPONENT_HEIGHT));
        setFont(DISPLAY_FONT);
        setBackground(BACKGROUND_COLOR);
        setBorder(new CompoundBorder(
                new RoundedBorder(16),
                new EmptyBorder(6, 8, 6, 8)
        ));
    }

    // Inner class for rounded border matching project style
    public static class RoundedBorder extends AbstractBorder {
        private final int radius;

        public RoundedBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BORDER_COLOR);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(4, 4, 4, 4);
        }
    }
}