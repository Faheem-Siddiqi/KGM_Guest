package com.kgm.ui.panel;

import com.kgm.ui.component.UniversalDateRangePicker;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
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
        row.add(HomeViewHelper.styleField(searchField, 320));
        row.add(Box.createHorizontalStrut(10));
        row.add(searchButton);
        row.add(Box.createHorizontalStrut(10));
        row.add(clearButton);

        block.add(label);
        block.add(Box.createVerticalStrut(6));
        block.add(row);
        return block;
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
