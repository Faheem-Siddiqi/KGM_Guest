package com.kgm.ui.panel;

import com.kgm.ui.component.UniversalDateRangePicker;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class GuestFilterPanel extends JPanel {
    private static final int FILTER_FIELD_GAP = 14;
    private static final int STATUS_ARROW_AND_PADDING_WIDTH = 36;
    private static final int FILTER_FIELD_HEIGHT = 34;
    private static final int SEARCH_FIELD_WIDTH = 320;
    private static final int SEARCH_FIELD_MIN_WIDTH = 240;

    private final JTextField searchField = new PlaceholderTextField("Search by Name");
    private final JComboBox<String> statusFilter = HomeViewHelper.combo(
            "All Status", "Currently Staying", "Departed", "Upcoming"
    );
    private final UniversalDateRangePicker dateRangeFilter = new UniversalDateRangePicker();
    private final JButton clearButton = HomeViewHelper.textButton("CLEAR");
    private final JButton chartFilterClearButton = new DangerPillButton("Clear Filter");
    private boolean suppressFilterEvents;
    private Runnable onChartFilterClear;

    public GuestFilterPanel(Runnable onSearch, Runnable onClear) {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 12, 0));

        JButton searchButton = HomeViewHelper.textButton("SEARCH");
        styleSearchButton(searchButton);

        searchField.addActionListener(e -> onSearch.run());
        searchField.setToolTipText("Search by guest name, CNIC, or passport");
        searchField.getAccessibleContext().setAccessibleName("Search by guest name, CNIC, or passport");
        searchButton.addActionListener(e -> onSearch.run());
        statusFilter.addActionListener(e -> {
            updateClearButtonState();
            runFilter(onSearch);
        });
        dateRangeFilter.addRangeChangeListener(() -> {
            updateClearButtonState();
            runFilter(onSearch);
        });
        clearButton.addActionListener(e -> {
            onClear.run();
            updateClearButtonState();
        });
        styleInlineClearButton();
        styleChartFilterClearButton();
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                updateClearButtonState();
            }

            public void removeUpdate(DocumentEvent event) {
                updateClearButtonState();
            }

            public void changedUpdate(DocumentEvent event) {
                updateClearButtonState();
            }
        });

        add(inlineFilters(searchButton), BorderLayout.CENTER);
        updateClearButtonState();
    }

    private JPanel inlineFilters(JButton searchButton) {
        JPanel filters = new JPanel(new GridBagLayout());
        filters.setOpaque(false);
        filters.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 10);
        filters.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        filters.add(createSearchFieldWithClearButton(), gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, FILTER_FIELD_GAP);
        filters.add(searchButton, gbc);

        gbc.gridx = 3;
        filters.add(styleHugStatusFilter(), gbc);

        gbc.gridx = 4;
        gbc.insets = new Insets(0, 0, 0, FILTER_FIELD_GAP);
        filters.add(lockComponentToPreferredWidth(dateRangeFilter), gbc);

        gbc.gridx = 5;
        gbc.insets = new Insets(0, 0, 0, 0);
        filters.add(chartFilterClearButton, gbc);

        return filters;
    }

    private JComponent createSearchFieldWithClearButton() {
        JPanel field = new JPanel(new BorderLayout(6, 0));
        field.setOpaque(true);
        HomeViewHelper.styleField(field, SEARCH_FIELD_WIDTH);
        Dimension preferred = new Dimension(SEARCH_FIELD_WIDTH, FILTER_FIELD_HEIGHT);
        field.setPreferredSize(preferred);
        field.setMinimumSize(new Dimension(SEARCH_FIELD_MIN_WIDTH, FILTER_FIELD_HEIGHT));
        field.setMaximumSize(preferred);

        searchField.setBorder(null);
        searchField.setOpaque(false);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.add(searchField, BorderLayout.CENTER);
        field.add(clearButton, BorderLayout.EAST);
        return field;
    }

    private void styleInlineClearButton() {
        clearButton.setText("Clear");
        clearButton.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
        clearButton.setBorder(new EmptyBorder(2, 6, 2, 2));
        clearButton.setMargin(new Insets(0, 0, 0, 0));
        clearButton.setPreferredSize(new Dimension(42, 24));
        clearButton.setMinimumSize(new Dimension(42, 24));
        clearButton.setMaximumSize(new Dimension(42, 24));
    }

    private void styleSearchButton(JButton button) {
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setBackground(HomeViewHelper.PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setBorder(new EmptyBorder(0, 18, 0, 18));
        button.setPreferredSize(new Dimension(92, 34));
        button.setMinimumSize(new Dimension(92, 34));
        button.setMaximumSize(new Dimension(92, 34));
    }

    private void styleChartFilterClearButton() {
        chartFilterClearButton.setVisible(false);
        chartFilterClearButton.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        chartFilterClearButton.setForeground(Color.WHITE);
        chartFilterClearButton.setFocusPainted(false);
        chartFilterClearButton.setBorderPainted(false);
        chartFilterClearButton.setContentAreaFilled(false);
        chartFilterClearButton.setOpaque(false);
        chartFilterClearButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chartFilterClearButton.setBorder(new EmptyBorder(0, 14, 0, 14));
        chartFilterClearButton.setPreferredSize(new Dimension(164, FILTER_FIELD_HEIGHT));
        chartFilterClearButton.setMinimumSize(new Dimension(120, FILTER_FIELD_HEIGHT));
        chartFilterClearButton.setMaximumSize(new Dimension(340, FILTER_FIELD_HEIGHT));
        chartFilterClearButton.addActionListener(event -> {
            if (onChartFilterClear != null) {
                onChartFilterClear.run();
            }
        });
    }

    private JComponent styleHugStatusFilter() {
        HomeViewHelper.styleField(statusFilter, 150);
        styleStatusSelectedValueBackground();
        int width = calculateStatusFilterWidth();
        Dimension size = new Dimension(width, FILTER_FIELD_HEIGHT);
        statusFilter.setPreferredSize(size);
        statusFilter.setMinimumSize(size);
        statusFilter.setMaximumSize(size);
        return statusFilter;
    }

    private int calculateStatusFilterWidth() {
        FontMetrics metrics = statusFilter.getFontMetrics(statusFilter.getFont());
        int widestText = 0;
        for (int index = 0; index < statusFilter.getItemCount(); index++) {
            String value = String.valueOf(statusFilter.getItemAt(index));
            widestText = Math.max(widestText, metrics.stringWidth(value));
        }
        Insets insets = statusFilter.getInsets();
        return widestText + insets.left + insets.right + STATUS_ARROW_AND_PADDING_WIDTH;
    }

    private void styleStatusSelectedValueBackground() {
        statusFilter.setOpaque(true);
        statusFilter.setBackground(Color.WHITE);
        statusFilter.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list,
                        value,
                        index,
                        isSelected,
                        cellHasFocus
                );
                label.setBorder(new EmptyBorder(0, 0, 0, 0));
                label.setBackground(index < 0 ? Color.WHITE
                        : isSelected ? HomeViewHelper.ROW_SELECTION : new Color(247, 250, 255));
                label.setForeground(HomeViewHelper.TEXT_PRIMARY);
                list.setBackground(new Color(247, 250, 255));
                list.setSelectionBackground(HomeViewHelper.ROW_SELECTION);
                list.setSelectionForeground(HomeViewHelper.TEXT_PRIMARY);
                return label;
            }
        });
    }

    private JComponent lockComponentToPreferredWidth(JComponent component) {
        Dimension preferred = component.getPreferredSize();
        Dimension minimum = component.getMinimumSize();
        component.setPreferredSize(preferred);
        component.setMinimumSize(new Dimension(Math.min(minimum.width, preferred.width), preferred.height));
        component.setMaximumSize(preferred);
        return component;
    }

    public String getSearchText() {
        return searchField.getText().trim();
    }

    public String getStatusText() {
        return String.valueOf(statusFilter.getSelectedItem());
    }

    public String getDateText() {
        return dateRangeFilter.getFilterText();
    }

    public UniversalDateRangePicker.DateRange getDateRange() {
        return dateRangeFilter.getDateRange();
    }

    public void showChartFilterClearAction(String filterText, Runnable onClear) {
        String text = filterText == null || filterText.isBlank() ? "Filter" : filterText.trim();
        String buttonText = "Clear Filter: " + text;
        chartFilterClearButton.setText(buttonText);
        chartFilterClearButton.setToolTipText(buttonText);
        updateChartFilterClearButtonSize(buttonText);
        onChartFilterClear = onClear;
        chartFilterClearButton.setVisible(true);
        revalidate();
        repaint();
    }

    public void clearChartFilterClearAction() {
        chartFilterClearButton.setVisible(false);
        chartFilterClearButton.setText("Clear Filter");
        chartFilterClearButton.setToolTipText(null);
        onChartFilterClear = null;
        revalidate();
        repaint();
    }

    public void clearSearch() {
        suppressFilterEvents = true;
        try {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            dateRangeFilter.clearRange();
            updateClearButtonState();
        } finally {
            suppressFilterEvents = false;
        }
    }

    private void runFilter(Runnable onSearch) {
        if (!suppressFilterEvents) {
            onSearch.run();
        }
    }

    private void updateClearButtonState() {
        boolean hasSearchText = !searchField.getText().trim().isEmpty();
        boolean hasStatusFilter = statusFilter.getSelectedIndex() > 0;
        HomeViewHelper.setTextButtonEnabled(clearButton, hasSearchText || hasStatusFilter || dateRangeFilter.hasSelection());
    }

    private void updateChartFilterClearButtonSize(String text) {
        FontMetrics metrics = chartFilterClearButton.getFontMetrics(chartFilterClearButton.getFont());
        int width = Math.max(164, Math.min(340, metrics.stringWidth(text) + 34));
        Dimension size = new Dimension(width, FILTER_FIELD_HEIGHT);
        chartFilterClearButton.setPreferredSize(size);
        chartFilterClearButton.setMinimumSize(new Dimension(Math.min(width, 150), FILTER_FIELD_HEIGHT));
        chartFilterClearButton.setMaximumSize(size);
    }

    private static class DangerPillButton extends JButton {
        private static final Color RED = new Color(220, 38, 38);
        private static final Color RED_HOVER = new Color(185, 28, 28);
        private static final Color RED_PRESSED = new Color(153, 27, 27);

        private DangerPillButton(String text) {
            super(text);
            setRolloverEnabled(true);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ButtonModel model = getModel();
            Color fill = model.isPressed() ? RED_PRESSED : model.isRollover() ? RED_HOVER : RED;
            g2.setColor(fill);
            g2.fillRoundRect(0, 1, Math.max(0, getWidth() - 1), Math.max(0, getHeight() - 2), 8, 8);
            g2.setColor(new Color(127, 29, 29, 90));
            g2.drawRoundRect(0, 1, Math.max(0, getWidth() - 1), Math.max(0, getHeight() - 2), 8, 8);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class PlaceholderTextField extends JTextField {
        private final String placeholder;

        private PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
            addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent event) {
                    repaint();
                }

                public void focusLost(FocusEvent event) {
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (isFocusOwner() || !getText().isEmpty()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setFont(getFont());
            g2.setColor(new Color(
                    HomeViewHelper.TEXT_SECONDARY.getRed(),
                    HomeViewHelper.TEXT_SECONDARY.getGreen(),
                    HomeViewHelper.TEXT_SECONDARY.getBlue(),
                    150
            ));
            FontMetrics metrics = g2.getFontMetrics();
            Insets insets = getInsets();
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(placeholder, insets.left, y);
            g2.dispose();
        }
    }
}
