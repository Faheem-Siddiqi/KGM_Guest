package com.kgm.ui.panel;

import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;
import java.util.Date;

public class GuestFilterPanel extends JPanel {
    private final JTextField searchField = new JTextField();
    private final JComboBox<String> statusFilter = HomeViewHelper.combo(
            "All Status", "Currently Staying", "Departed", "Upcoming"
    );
    private final JComboBox<String> departmentFilter = HomeViewHelper.combo(
            "All Departments", "IT", "HR", "Ops", "Sales", "Finance"
    );
    private final JSpinner dateFilter = HomeViewHelper.dateSpinner(new Date());
    private final JCheckBox useDateFilter = HomeViewHelper.checkBox("Use Date");

    public GuestFilterPanel(Runnable onSearch, Runnable onClear, Runnable onAddGuest, Runnable onAccommodation) {
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
        JButton clearButton = HomeViewHelper.textButton("CLEAR");

        dateFilter.setEnabled(false);
        useDateFilter.addActionListener(e -> dateFilter.setEnabled(useDateFilter.isSelected()));

        searchField.addActionListener(e -> onSearch.run());
        searchButton.addActionListener(e -> onSearch.run());
        clearButton.addActionListener(e -> onClear.run());

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        filters.add(HomeViewHelper.filterField("Search Guest", searchField, 320), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.35;
        filters.add(HomeViewHelper.filterField("Status", statusFilter, 200), gbc);

        gbc.gridx = 2;
        gbc.insets = new Insets(0, 0, 14, 0);
        filters.add(HomeViewHelper.filterField("Department", departmentFilter, 200), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 14, 0);
        filters.add(createDateFilter(), gbc);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel searchActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchActions.setOpaque(false);
        searchActions.add(searchButton);
        searchActions.add(clearButton);

        JPanel navigationActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        navigationActions.setOpaque(false);
        JButton addGuest = HomeViewHelper.textButton("ADD GUEST");
        JButton accommodation = HomeViewHelper.textButton("ACCOMMODATIONS");
        addGuest.addActionListener(e -> onAddGuest.run());
        accommodation.addActionListener(e -> onAccommodation.run());

        navigationActions.add(addGuest);
        navigationActions.add(accommodation);
        actions.add(searchActions, BorderLayout.WEST);
        actions.add(navigationActions, BorderLayout.EAST);

        body.add(filters);
        body.add(actions);

        card.add(body, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
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

    public String getDepartmentText() {
        return String.valueOf(departmentFilter.getSelectedItem());
    }

    public String getDateText() {
        return useDateFilter.isSelected() ? HomeViewHelper.dateText(dateFilter) : "";
    }

    public void clearSearch() {
        searchField.setText("");
        statusFilter.setSelectedIndex(0);
        departmentFilter.setSelectedIndex(0);
        useDateFilter.setSelected(false);
        dateFilter.setValue(new Date());
        dateFilter.setEnabled(false);
    }
}
