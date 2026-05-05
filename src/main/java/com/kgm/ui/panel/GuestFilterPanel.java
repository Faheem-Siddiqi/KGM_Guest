package com.kgm.ui.panel;

import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Date;

public class GuestFilterPanel extends JPanel {
    private final JTextField searchField = new JTextField();
    private final JComboBox<String> statusFilter = HomeViewHelper.combo(
            "All Status", "Currently Staying", "Departed", "Upcoming"
    );
    private final JSpinner dateFilter = HomeViewHelper.dateSpinner(new Date());
    private final JCheckBox useDateFilter = HomeViewHelper.checkBox("Use Date");
    private final JButton clearButton = HomeViewHelper.textButton("CLEAR");

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

        dateFilter.setEnabled(false);
        useDateFilter.addActionListener(e -> dateFilter.setEnabled(useDateFilter.isSelected()));

        searchField.addActionListener(e -> onSearch.run());
        searchButton.addActionListener(e -> onSearch.run());
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

        JLabel label = HomeViewHelper.label("Search Guest");
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

        JLabel label = HomeViewHelper.label("Date");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(useDateFilter);
        row.add(HomeViewHelper.styleField(dateFilter, 150));

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
        return useDateFilter.isSelected() ? HomeViewHelper.dateText(dateFilter) : "";
    }

    public void clearSearch() {
        searchField.setText("");
        statusFilter.setSelectedIndex(0);
        useDateFilter.setSelected(false);
        dateFilter.setValue(new Date());
        dateFilter.setEnabled(false);
        updateClearButtonState();
    }

    private void updateClearButtonState() {
        HomeViewHelper.setTextButtonEnabled(clearButton, !searchField.getText().trim().isEmpty());
    }
}
