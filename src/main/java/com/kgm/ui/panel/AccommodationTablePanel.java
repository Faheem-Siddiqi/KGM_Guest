package com.kgm.ui.panel;

import com.kgm.ui.styling.AccommodationManagementHelper;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AccommodationTablePanel extends JPanel {
    private static final String ALL_CATEGORIES = "All";
    private static final String[] COLUMNS = {
            "Category", "Name", "Capacity", "Available", "Status", "Assigned Staff", "Amenities", "Actions"
    };
    private static final String[] READ_ONLY_COLUMNS = {
            "Category", "Name", "Capacity", "Available", "Status", "Assigned Staff", "Amenities"
    };
    private static final int ACTION_COLUMN = 7;
    private static final String DEFAULT_TITLE = "Accommodation List";
    private static final String DEFAULT_SUBTITLE = "All created accommodations appear here with quick edit access.";
    private static final String DEFAULT_EMPTY_TEXT = "No accommodation records yet. Save the form above to create one.";
    private final List<AccommodationRecord> records = new ArrayList<>();
    private final List<Integer> visibleIndexes = new ArrayList<>();
    private final CategoryTabsPanel categoryTabs = new CategoryTabsPanel();
    private final UniversalTablePanel tablePanel;
    private final BiConsumer<Integer, AccommodationRecord> onEdit;
    private final BiConsumer<Integer, AccommodationRecord> onView;
    private final boolean showCategoryTabs;
    private final boolean showActionColumn;
    private List<String> availableCategories = new ArrayList<>();
    private String selectedCategory = "";

    public AccommodationTablePanel(BiConsumer<Integer, AccommodationRecord> onEdit) {
        this(onEdit, (row, accommodation) -> {
        });
    }

    public AccommodationTablePanel(
            BiConsumer<Integer, AccommodationRecord> onEdit,
            BiConsumer<Integer, AccommodationRecord> onView
    ) {
        this(onEdit, onView, true, true, DEFAULT_TITLE, DEFAULT_SUBTITLE, DEFAULT_EMPTY_TEXT);
    }

    public AccommodationTablePanel(
            BiConsumer<Integer, AccommodationRecord> onEdit,
            BiConsumer<Integer, AccommodationRecord> onView,
            boolean showCategoryTabs,
            boolean showActionColumn,
            String title,
            String subtitle,
            String emptyText
    ) {
        this.onEdit = onEdit;
        this.onView = onView;
        this.showCategoryTabs = showCategoryTabs;
        this.showActionColumn = showActionColumn;
        this.tablePanel = new UniversalTablePanel(showActionColumn ? COLUMNS : READ_ONLY_COLUMNS, emptyText);
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel card = AccommodationManagementHelper.sectionCard(title, subtitle);

        if (showActionColumn) {
            tablePanel.setActionColumn(ACTION_COLUMN, "Edit", row -> {
                int recordIndex = visibleIndexes.get(row);
                this.onEdit.accept(recordIndex, records.get(recordIndex));
            });
        }
        tablePanel.setLinkColumn(1, row -> {
            int recordIndex = visibleIndexes.get(row);
            this.onView.accept(recordIndex, records.get(recordIndex));
        });

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        if (showCategoryTabs) {
            body.add(categoryTabs, BorderLayout.NORTH);
        }
        body.add(tablePanel, BorderLayout.CENTER);

        card.add(body, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
        if (showCategoryTabs) {
            refreshCategoryTabs();
        }
    }

    public void addAccommodation(AccommodationRecord accommodation) {
        records.add(accommodation);
        if (showCategoryTabs && !containsCategory(availableCategories, accommodation.getCategory())) {
            availableCategories = categoriesFromRecords();
            refreshCategoryTabs();
        }
        applyCategoryFilter();
    }

    public void setAccommodations(List<AccommodationRecord> accommodations) {
        records.clear();
        records.addAll(accommodations);
        if (!showCategoryTabs) {
            selectedCategory = "";
        } else if (availableCategories.isEmpty()) {
            availableCategories = categoriesFromRecords();
            refreshCategoryTabs();
        }
        applyCategoryFilter();
    }

    public void setCategories(List<String> categories) {
        availableCategories = cleanCategories(categories);
        if (!selectedCategory.isEmpty() && !containsCategory(availableCategories, selectedCategory)) {
            selectedCategory = "";
        }
        if (showCategoryTabs) {
            refreshCategoryTabs();
        }
        applyCategoryFilter();
    }

    public AccommodationRecord getAccommodation(int row) {
        return records.get(row);
    }

    public void updateAccommodation(int row, AccommodationRecord accommodation) {
        records.set(row, accommodation);
        if (showCategoryTabs && !containsCategory(availableCategories, accommodation.getCategory())) {
            availableCategories = categoriesFromRecords();
            refreshCategoryTabs();
        }
        applyCategoryFilter();
    }

    private Object[] rowValues(AccommodationRecord accommodation) {
        if (!showActionColumn) {
            return new Object[]{
                    accommodation.getCategory(),
                    accommodation.getName(),
                    accommodation.getCapacity(),
                    accommodation.getAvailableSeats(),
                    accommodation.getStatus(),
                    accommodation.getAssignedStaff(),
                    accommodation.getAmenities().size()
            };
        }
        return new Object[]{
                accommodation.getCategory(),
                accommodation.getName(),
                accommodation.getCapacity(),
                accommodation.getAvailableSeats(),
                accommodation.getStatus(),
                accommodation.getAssignedStaff(),
                accommodation.getAmenities().size(),
                "Edit"
        };
    }

    private void applyCategoryFilter() {
        List<Object[]> rows = new ArrayList<>();
        visibleIndexes.clear();
        for (int index = 0; index < records.size(); index++) {
            AccommodationRecord accommodation = records.get(index);
            if (matchesSelectedCategory(accommodation)) {
                visibleIndexes.add(index);
                rows.add(rowValues(accommodation));
            }
        }
        tablePanel.setRows(rows);
    }

    private boolean matchesSelectedCategory(AccommodationRecord accommodation) {
        return selectedCategory.isEmpty()
                || selectedCategory.equalsIgnoreCase(cleanCategory(accommodation.getCategory()));
    }

    private void selectCategory(String category) {
        selectedCategory = category == null ? "" : category;
        categoryTabs.selectValue(selectedCategory);
        applyCategoryFilter();
    }

    private void refreshCategoryTabs() {
        categoryTabs.removeAllTabs();
        categoryTabs.addTab(ALL_CATEGORIES, "", this::selectCategory);
        for (String category : availableCategories) {
            categoryTabs.addTab(category, category, this::selectCategory);
        }
        categoryTabs.selectValue(selectedCategory);
        categoryTabs.revalidate();
        categoryTabs.repaint();
    }

    private List<String> categoriesFromRecords() {
        Set<String> categories = new LinkedHashSet<>();
        for (AccommodationRecord record : records) {
            String category = cleanCategory(record.getCategory());
            if (!category.isEmpty()) {
                categories.add(category);
            }
        }
        return new ArrayList<>(categories);
    }

    private List<String> cleanCategories(List<String> categories) {
        Set<String> cleaned = new LinkedHashSet<>();
        for (String category : categories) {
            String value = cleanCategory(category);
            if (!value.isEmpty()) {
                cleaned.add(value);
            }
        }
        return new ArrayList<>(cleaned);
    }

    private String cleanCategory(String category) {
        return category == null ? "" : category.trim();
    }

    private boolean containsCategory(List<String> categories, String category) {
        String value = cleanCategory(category);
        for (String item : categories) {
            if (item.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static class CategoryTabsPanel extends JPanel {
        private static final int HORIZONTAL_GAP = 14;
        private static final int VERTICAL_GAP = 4;
        private static final int ROW_HEIGHT = 28;
        private final List<CategoryTabButton> buttons = new ArrayList<>();

        private CategoryTabsPanel() {
            setOpaque(false);
            setLayout(null);
            setPreferredSize(new Dimension(0, ROW_HEIGHT));
        }

        private void addTab(String text, String value, Consumer<String> onSelect) {
            CategoryTabButton button = new CategoryTabButton(text, value);
            button.addActionListener(event -> onSelect.accept(value));
            buttons.add(button);
            add(button);
        }

        private void removeAllTabs() {
            buttons.clear();
            removeAll();
            setPreferredSize(new Dimension(0, ROW_HEIGHT));
        }

        private void selectValue(String value) {
            for (CategoryTabButton button : buttons) {
                button.setActive(button.hasValue(value));
            }
        }

        public void doLayout() {
            int height = calculateLayout(getWidth(), true);
            setPreferredSize(new Dimension(0, height));
        }

        public Dimension getPreferredSize() {
            int width = getWidth() > 0 ? getWidth() : AccommodationManagementHelper.CONTENT_WIDTH - 96;
            return new Dimension(width, calculateLayout(width, false));
        }

        private int calculateLayout(int width, boolean applyBounds) {
            int maxWidth = Math.max(1, width);
            int x = 0;
            int y = 0;
            int rows = 1;
            for (CategoryTabButton button : buttons) {
                Dimension size = button.getPreferredSize();
                int buttonWidth = Math.min(size.width, maxWidth);
                if (x > 0 && x + buttonWidth > maxWidth) {
                    x = 0;
                    y += ROW_HEIGHT + VERTICAL_GAP;
                    rows++;
                }
                if (applyBounds) {
                    button.setBounds(x, y, buttonWidth, ROW_HEIGHT);
                }
                x += buttonWidth + HORIZONTAL_GAP;
            }
            return rows * ROW_HEIGHT + Math.max(0, rows - 1) * VERTICAL_GAP;
        }
    }

    private static class CategoryTabButton extends JButton {
        private final String value;
        private boolean active;

        private CategoryTabButton(String text, String value) {
            super(text);
            this.value = value;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
            setForeground(AccommodationManagementHelper.TEXT_SECONDARY);
            setBorder(BorderFactory.createEmptyBorder(4, 0, 7, 8));
        }

        private boolean hasValue(String otherValue) {
            return value.equals(otherValue);
        }

        private void setActive(boolean active) {
            this.active = active;
            setForeground(active ? AccommodationManagementHelper.PRIMARY : AccommodationManagementHelper.TEXT_SECONDARY);
            repaint();
        }

        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!active) {
                return;
            }
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FontMetrics metrics = g2.getFontMetrics(getFont());
            int underlineWidth = Math.max(22, Math.min(getWidth() - 12, metrics.stringWidth(getText()) + 4));
            int underlineX = (getWidth() - underlineWidth) / 2;
            g2.setColor(AccommodationManagementHelper.PRIMARY);
            g2.fillRoundRect(underlineX, getHeight() - 4, underlineWidth, 3, 3, 3);
            g2.dispose();
        }
    }
}
