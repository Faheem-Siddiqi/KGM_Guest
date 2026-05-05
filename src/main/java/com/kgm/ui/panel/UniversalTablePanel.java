package com.kgm.ui.panel;

import com.kgm.ui.styling.AccommodationManagementHelper;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UniversalTablePanel extends JPanel {
    // UPDATE PAGINATION 
    private static final int PAGE_SIZE = 2;
    private static final int MIN_VIEWPORT_HEIGHT = 118;

    private final JTable table;
    private final DefaultTableModel model;
    private final List<Object[]> rows = new ArrayList<>();
    private final JPanel content = new JPanel(new BorderLayout());
    private final JLabel rangeLabel = new JLabel();
    private final JButton previousButton = new JButton("Previous");
    private final JButton nextButton = new JButton("Next");
    private final String emptyText;
    private int actionColumn = -1;
    private Consumer<Integer> onAction;
    private boolean hugRows = true;
    private boolean paginationEnabled = true;
    private int currentPage = 0;

    public UniversalTablePanel(String[] columns, String emptyText) {
        this.emptyText = emptyText;
        this.model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.table = new JTable(model);

        setLayout(new BorderLayout());
        setOpaque(false);
        content.setOpaque(false);

        AccommodationManagementHelper.styleTable(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                if (actionColumn < 0 || onAction == null) {
                    return;
                }

                int row = table.rowAtPoint(event.getPoint());
                int column = table.columnAtPoint(event.getPoint());
                if (row < 0 || column != actionColumn) {
                    return;
                }

                onAction.accept(toAbsoluteRow(row));
            }
        });
        table.addMouseMotionListener(new MouseAdapter() {
            public void mouseMoved(MouseEvent event) {
                int row = table.rowAtPoint(event.getPoint());
                int column = table.columnAtPoint(event.getPoint());
                boolean hoveringAction = actionColumn >= 0 && row >= 0 && column == actionColumn;
                table.setCursor(Cursor.getPredefinedCursor(hoveringAction ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });

        previousButton.addActionListener(e -> goToPage(currentPage - 1));
        nextButton.addActionListener(e -> goToPage(currentPage + 1));
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent event) {
                if (!rows.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        renderPage();
                        revalidate();
                        repaint();
                    });
                }
            }
        });

        add(content, BorderLayout.CENTER);
        refresh();
    }

    public void setActionColumn(int column, String text, Consumer<Integer> onAction) {
        this.actionColumn = column;
        this.onAction = onAction;
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                label.setText(text);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setForeground(AccommodationManagementHelper.PRIMARY);
                label.setBackground(isSelected ? AccommodationManagementHelper.ROW_SELECTION : Color.WHITE);
                label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
                label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(232, 236, 240)),
                        BorderFactory.createEmptyBorder(0, 14, 0, 14)
                ));
                return label;
            }
        };
        table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        configureColumnWidths();
    }

    public void setHugRows(boolean hugRows) {
        this.hugRows = hugRows;
        refresh();
    }

    public void setPaginationEnabled(boolean paginationEnabled) {
        this.paginationEnabled = paginationEnabled;
        currentPage = 0;
        refresh();
    }

    public void addRow(Object[] row) {
        rows.add(row);
        currentPage = lastPage();
        refresh();
    }

    public void setRows(List<Object[]> newRows) {
        rows.clear();
        rows.addAll(newRows);
        currentPage = 0;
        refresh();
    }

    public void clearRows() {
        rows.clear();
        currentPage = 0;
        refresh();
    }

    public void updateRow(int row, Object[] values) {
        rows.set(row, values);
        refresh();
    }

    public void removeRow(int row) {
        rows.remove(row);
        currentPage = Math.min(currentPage, lastPage());
        refresh();
    }

    public int getRowCount() {
        return rows.size();
    }

    public Object getValueAt(int row, int column) {
        return rows.get(row)[column];
    }

    public void clearSelection() {
        table.clearSelection();
    }

    public int getRenderedTableWidth() {
        return table.getPreferredScrollableViewportSize().width;
    }

    private void refresh() {
        content.removeAll();
        if (rows.isEmpty()) {
            content.add(AccommodationManagementHelper.emptyState(emptyText), BorderLayout.CENTER);
        } else {
            renderPage();
            content.add(createTableContainer(), BorderLayout.CENTER);
        }
        content.revalidate();
        content.repaint();
    }

    private JPanel createTableContainer() {
        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setOpaque(false);
        container.setBorder(new EmptyBorder(6, 0, 0, 0));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new RoundedTableBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.getViewport().setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(shouldShowHorizontalScroll()
                ? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
                : ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(table.getPreferredScrollableViewportSize());
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(96);

        container.add(scrollPane, BorderLayout.CENTER);
        if (paginationEnabled) {
            container.add(createPagination(), BorderLayout.SOUTH);
        }
        return container;
    }

    private JPanel createPagination() {
        JPanel pagination = new JPanel(new BorderLayout());
        pagination.setOpaque(false);
        pagination.setBorder(new EmptyBorder(2, 0, 0, 0));

        rangeLabel.setText(showingText());
        rangeLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        rangeLabel.setForeground(AccommodationManagementHelper.TEXT_SECONDARY);
        rangeLabel.setBorder(new EmptyBorder(8, 0, 8, 0));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        stylePagingButton(previousButton, currentPage > 0);
        stylePagingButton(nextButton, currentPage < lastPage());
        buttons.add(previousButton);
        buttons.add(nextButton);

        pagination.add(rangeLabel, BorderLayout.WEST);
        pagination.add(buttons, BorderLayout.EAST);
        return pagination;
    }

    private void renderPage() {
        model.setRowCount(0);
        int start = paginationEnabled ? currentPage * PAGE_SIZE : 0;
        int end = paginationEnabled ? Math.min(start + PAGE_SIZE, rows.size()) : rows.size();
        for (int index = start; index < end; index++) {
            model.addRow(rows.get(index));
        }
        configureColumnWidths();
        if (hugRows) {
            int headerHeight = table.getTableHeader().getPreferredSize().height;
            int horizontalScrollbarHeight = preferredTableWidth() > availableTableWidth()
                    ? UIManager.getInt("ScrollBar.width")
                    : 0;
            int contentHeight = headerHeight + table.getRowHeight() * Math.max(1, model.getRowCount()) + horizontalScrollbarHeight;
            int height = Math.max(MIN_VIEWPORT_HEIGHT, contentHeight);
            table.setPreferredScrollableViewportSize(new Dimension(availableTableWidth(), height));
        }
    }

    private void configureColumnWidths() {
        int columns = table.getColumnCount();
        int availableWidth = availableTableWidth();
        int totalWidth = 0;

        for (int column = 0; column < columns; column++) {
            int width = column == actionColumn ? 80 : measuredColumnWidth(column);
            table.getColumnModel().getColumn(column).setPreferredWidth(width);
            totalWidth += width;
        }

        if (totalWidth < availableWidth && columns > 0) {
            int extra = availableWidth - totalWidth;
            int baseExtra = extra / columns;
            int remainder = extra % columns;
            for (int column = 0; column < columns; column++) {
                int currentWidth = table.getColumnModel().getColumn(column).getPreferredWidth();
                int addedWidth = baseExtra + (column < remainder ? 1 : 0);
                table.getColumnModel().getColumn(column).setPreferredWidth(currentWidth + addedWidth);
            }
            totalWidth = availableWidth;
        }

        Dimension size = table.getPreferredScrollableViewportSize();
        table.setPreferredScrollableViewportSize(new Dimension(
                availableWidth,
                size.height
        ));
    }

    private int measuredColumnWidth(int column) {
        int padding = 34;
        FontMetrics headerMetrics = table.getTableHeader().getFontMetrics(table.getTableHeader().getFont());
        FontMetrics cellMetrics = table.getFontMetrics(table.getFont());
        int width = headerMetrics.stringWidth(table.getColumnName(column)) + padding;

        for (Object[] row : rows) {
            Object value = row[column];
            width = Math.max(width, cellMetrics.stringWidth(value == null ? "" : String.valueOf(value)) + padding);
        }

        return Math.max(72, width);
    }

    private int preferredTableWidth() {
        int width = 0;
        for (int column = 0; column < table.getColumnCount(); column++) {
            width += table.getColumnModel().getColumn(column).getPreferredWidth();
        }
        return Math.max(width, 1);
    }

    private boolean shouldShowHorizontalScroll() {
        return preferredTableWidth() > availableTableWidth();
    }

    private int availableTableWidth() {
        int width = getWidth();
        if (width <= 0 && getParent() != null) {
            width = getParent().getWidth();
        }
        return Math.max(240, width > 0 ? width : AccommodationManagementHelper.CONTENT_WIDTH - 80);
    }

    private String showingText() {
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, rows.size());
        return "Showing " + start + "-" + end + " / " + rows.size();
    }

    private int toAbsoluteRow(int pageRow) {
        int offset = paginationEnabled ? currentPage * PAGE_SIZE : 0;
        return offset + table.convertRowIndexToModel(pageRow);
    }

    private void goToPage(int page) {
        currentPage = Math.max(0, Math.min(page, lastPage()));
        refresh();
    }

    private int lastPage() {
        if (rows.isEmpty()) {
            return 0;
        }
        return paginationEnabled ? (rows.size() - 1) / PAGE_SIZE : 0;
    }

    private void stylePagingButton(JButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setBorder(new EmptyBorder(8, 14, 8, 14));
        button.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        button.setBackground(enabled ? AccommodationManagementHelper.PRIMARY : new Color(225, 225, 225));
        button.setForeground(enabled ? Color.WHITE : new Color(145, 145, 145));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
    }

    private static class RoundedTableBorder extends AbstractBorder {
        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(AccommodationManagementHelper.BORDER);
            g2.drawRoundRect(x, y, width - 1, height - 1, 4, 4);
        }

        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }
    }
}
