package com.kgm.ui.panel;

import com.kgm.ui.component.UniversalDateRangePicker;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class GuestFilterPanel extends JPanel {
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

        JPanel filters = new JPanel(new GridBagLayout());
        filters.setOpaque(false);
        filters.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 14, 14);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        JButton searchButton = HomeViewHelper.textButton("SEARCH");
        styleSearchButton(searchButton);

        searchField.addActionListener(e -> onSearch.run());
        searchField.setToolTipText("Search by guest name or CNIC");
        searchField.getAccessibleContext().setAccessibleName("Search by guest name or CNIC");
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

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        filters.add(createSearchFilter(searchButton), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.35;
        filters.add(HomeViewHelper.filterField("Status", statusFilter, 200), gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 14, 0);
        filters.add(createDateFilter(), gbc);

        body.add(filters);

        card.add(body, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
        updateClearButtonState();
    }

    private JPanel createSearchFilter(JButton searchButton) {
        JPanel block = new JPanel();
        block.setOpaque(false);
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));

        JLabel label = HomeViewHelper.label("Search Name or CNIC");
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

    private JPanel createDateFilter() {
        JPanel block = new JPanel();
        block.setOpaque(false);
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));

        JLabel label = HomeViewHelper.label("Date Range");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(dateRangeFilter);

        block.add(label);
        block.add(Box.createVerticalStrut(6));
        block.add(row);
        return block;
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
