package com.kgm.ui.panel;

import com.kgm.ui.component.UniversalDateRangePicker;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class GuestFilterPanel extends JPanel {
    private static final int FILTER_FIELD_GAP = 14;
    private static final int STATUS_ARROW_AND_PADDING_WIDTH = 36;
    private static final int FILTER_FIELD_HEIGHT = 34;

    private final JTextField searchField = new JTextField();
    private final JComboBox<String> statusFilter = HomeViewHelper.combo(
            "All Status", "Currently Staying", "Departed", "Upcoming"
    );
    private final UniversalDateRangePicker dateRangeFilter = new UniversalDateRangePicker();
    private final JButton clearButton = HomeViewHelper.textButton("CLEAR");
    private boolean suppressFilterEvents;

    public GuestFilterPanel(Runnable onSearch, Runnable onClear) {
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel card = HomeViewHelper.sectionCard("Guest Filters", "Search and narrow guest activity quickly.");
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

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

        JPanel filters = responsiveFilters(createSearchFilter(searchButton), createRightAlignedFilters());

        body.add(filters);

        card.add(body, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
        updateClearButtonState();
    }

    private JPanel responsiveFilters(JComponent searchFilter, JComponent rightFilters) {
        JPanel filters = new JPanel(new GridBagLayout());
        filters.setOpaque(false);
        filters.setAlignmentX(Component.LEFT_ALIGNMENT);
        Runnable layoutUpdater = () -> {
            boolean stacked = filters.getWidth() > 0 && filters.getWidth() < 780;
            filters.removeAll();
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(0, 0, 14, stacked ? 0 : 14);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            filters.add(searchFilter, gbc);

            gbc.gridx = stacked ? 0 : 1;
            gbc.gridy = stacked ? 1 : 0;
            gbc.weightx = stacked ? 1.0 : 0;
            gbc.fill = stacked ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
            gbc.anchor = stacked ? GridBagConstraints.NORTHWEST : GridBagConstraints.NORTHEAST;
            gbc.insets = new Insets(0, 0, 14, 0);
            filters.add(rightFilters, gbc);
            filters.revalidate();
            filters.repaint();
        };
        filters.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent event) {
                layoutUpdater.run();
            }
        });
        layoutUpdater.run();
        return filters;
    }

    private JPanel createSearchFilter(JButton searchButton) {
        JPanel block = new JPanel();
        block.setOpaque(false);
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));

        JLabel label = HomeViewHelper.label("Search Name / CNIC / Passport");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(createSearchFieldWithClearButton());
        row.add(Box.createHorizontalStrut(10));
        row.add(searchButton);

        block.add(label);
        block.add(Box.createVerticalStrut(6));
        block.add(row);
        return block;
    }

    private JComponent createSearchFieldWithClearButton() {
        JPanel field = new JPanel(new BorderLayout(6, 0));
        field.setOpaque(true);
        HomeViewHelper.styleField(field, 320);

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

    private JPanel createRightAlignedFilters() {
        JPanel filters = new JPanel();
        filters.setLayout(new BoxLayout(filters, BoxLayout.X_AXIS));
        filters.setOpaque(false);
        filters.add(createStatusFilter());
        filters.add(Box.createHorizontalStrut(FILTER_FIELD_GAP));
        filters.add(createDateFilter());
        return filters;
    }

    private JPanel createStatusFilter() {
        JPanel block = new JPanel();
        block.setOpaque(false);
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));

        // JLabel label = HomeViewHelper.label("Status");
        // label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(styleHugStatusFilter());

        block.add(createHiddenFilterLabelSpace());
        block.add(row);
        return block;
    }

    private JPanel createDateFilter() {
        JPanel block = new JPanel();
        block.setOpaque(false);
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));

        // JLabel label = HomeViewHelper.label("Date Range");
        // label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(lockComponentToPreferredWidth(dateRangeFilter));

        block.add(createHiddenFilterLabelSpace());
        block.add(row);
        return block;
    }

    private Component createHiddenFilterLabelSpace() {
        return Box.createVerticalStrut(HomeViewHelper.label("Filter").getPreferredSize().height + 6);
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
}
