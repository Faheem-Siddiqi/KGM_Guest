package com.kgm.ui.panel;

import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;

public class GuestFilterPanel extends JPanel {
    private final JTextField searchField = new JTextField();

    public GuestFilterPanel(Runnable onSearch, Runnable onClear, Runnable onAddGuest, Runnable onAccommodation) {
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel card = HomeViewHelper.sectionCard("Guest Filters", "Search and narrow guest activity quickly.");
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 12, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;

        JComboBox<String> statusFilter = HomeViewHelper.combo("All Status", "Vacant", "Occupied");
        JComboBox<String> departmentFilter = HomeViewHelper.combo("All Departments", "IT", "HR", "Ops", "Sales", "Finance");
        JTextField dateFilter = new JTextField();
        JButton searchButton = HomeViewHelper.textButton("SEARCH");
        JButton clearButton = HomeViewHelper.textButton("CLEAR");
        JPanel searchActions = HomeViewHelper.inlineActionsPanel();

        searchField.addActionListener(e -> onSearch.run());
        searchButton.addActionListener(e -> onSearch.run());
        clearButton.addActionListener(e -> onClear.run());

        searchActions.add(searchButton);
        searchActions.add(clearButton);

        gbc.gridx = 0;
        gbc.gridy = 0;
        body.add(HomeViewHelper.filterField("Search Guest", searchField), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        body.add(searchActions, gbc);

        gbc.gridx = 2;
        gbc.weightx = 1.0;
        body.add(HomeViewHelper.filterField("Status", statusFilter), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        body.add(HomeViewHelper.filterField("Department", departmentFilter), gbc);

        gbc.gridx = 1;
        body.add(HomeViewHelper.filterField("Date", dateFilter), gbc);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 24));
        actions.setOpaque(false);

        JButton addGuest = HomeViewHelper.textButton("ADD GUEST");
        JButton accommodation = HomeViewHelper.textButton("ACCOMMODATIONS");
        addGuest.addActionListener(e -> onAddGuest.run());
        accommodation.addActionListener(e -> onAccommodation.run());

        actions.add(addGuest);
        actions.add(accommodation);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        body.add(actions, gbc);

        card.add(body, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
    }

    public String getSearchText() {
        return searchField.getText().trim();
    }

    public void clearSearch() {
        searchField.setText("");
    }
}
