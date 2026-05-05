package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public final class HomeViewHelper {
    public static final Color PAGE_BACKGROUND = Color.WHITE;
    public static final Color PRIMARY = new Color(0, 112, 210);
    public static final Color PRIMARY_DARK = new Color(0, 62, 122);
    public static final Color PRIMARY_LIGHT = new Color(0, 157, 225);
    public static final Color SECONDARY_TAB = new Color(238, 246, 253);
    public static final Color VACANT_DARK = new Color(0, 136, 112);
    public static final Color VACANT_LIGHT = new Color(0, 188, 212);
    public static final Color OCCUPIED_DARK = new Color(73, 76, 162);
    public static final Color OCCUPIED_LIGHT = new Color(106, 139, 218);
    public static final Color KPI_GREEN_DARK = new Color(26, 112, 91);
    public static final Color KPI_GREEN_LIGHT = new Color(94, 182, 128);
    public static final Color KPI_STEEL_DARK = new Color(30, 103, 150);
    public static final Color KPI_STEEL_LIGHT = new Color(74, 190, 204);
    public static final Color KPI_WARM_DARK = new Color(174, 74, 94);
    public static final Color KPI_WARM_LIGHT = new Color(239, 122, 106);
    public static final Color TEXT_PRIMARY = new Color(35, 43, 54);
    public static final Color TEXT_SECONDARY = new Color(99, 115, 129);
    public static final Color BORDER = new Color(220, 226, 232);
    public static final Color ROW_SELECTION = new Color(229, 242, 255);
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final SimpleDateFormat FILTER_DATE_FORMAT = new SimpleDateFormat(DATE_PATTERN);

    private HomeViewHelper() {
    }

    public static JPanel pagePanel() {
        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(PAGE_BACKGROUND);
        page.setBorder(new EmptyBorder(28, 40, 32, 40));
        return page;
    }

    public static GridBagConstraints pageConstraints(int y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 18, 0);
        return gbc;
    }

    public static void styleTabs(JTabbedPane tabs) {
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setBackground(SECONDARY_TAB);
        tabs.setForeground(TEXT_SECONDARY);
        tabs.setOpaque(true);
        tabs.setBorder(new MatteBorder(1, 0, 0, 0, BORDER));
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setUI(new BasicTabbedPaneUI() {
            protected void installDefaults() {
                super.installDefaults();
                tabInsets = new Insets(12, 26, 11, 26);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
                contentBorderInsets = new Insets(18, 0, 0, 0);
                tabAreaInsets = new Insets(8, 28, 18, 28);
            }

            protected void paintTabBackground(
                    Graphics graphics,
                    int tabPlacement,
                    int tabIndex,
                    int x,
                    int y,
                    int width,
                    int height,
                    boolean isSelected
            ) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelected ? PRIMARY : SECONDARY_TAB);
                g2.fillRoundRect(x + 3, y + 4, width - 6, height - 10, 10, 10);
                g2.dispose();
            }

            protected void paintTabBorder(
                    Graphics graphics,
                    int tabPlacement,
                    int tabIndex,
                    int x,
                    int y,
                    int width,
                    int height,
                    boolean isSelected
            ) {
            }

            protected void paintTabArea(Graphics graphics, int tabPlacement, int selectedIndex) {
                super.paintTabArea(graphics, tabPlacement, selectedIndex);
                int lineY = 0;
                if (rects != null) {
                    for (Rectangle rect : rects) {
                        if (rect != null && rect.height > 0) {
                            lineY = Math.max(lineY, rect.y + rect.height - 2);
                        }
                    }
                }
                if (lineY <= 0) {
                    lineY = 42;
                }

                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BORDER);
                g2.drawLine(tabAreaInsets.left, lineY, tabPane.getWidth() - tabAreaInsets.right, lineY);

                if (selectedIndex >= 0 && rects != null && selectedIndex < rects.length) {
                    Rectangle selected = rects[selectedIndex];
                    if (selected != null) {
                        int underlineWidth = Math.max(24, selected.width - 32);
                        g2.setColor(PRIMARY);
                        g2.fillRoundRect(selected.x + 16, lineY - 1, underlineWidth, 3, 3, 3);
                    }
                }
                g2.dispose();
            }

            protected void paintContentBorder(Graphics graphics, int tabPlacement, int selectedIndex) {
            }

            protected void paintFocusIndicator(
                    Graphics graphics,
                    int tabPlacement,
                    Rectangle[] rectangles,
                    int tabIndex,
                    Rectangle iconRect,
                    Rectangle textRect,
                    boolean isSelected
            ) {
            }

            protected void paintText(
                    Graphics graphics,
                    int tabPlacement,
                    Font font,
                    FontMetrics metrics,
                    int tabIndex,
                    String title,
                    Rectangle textRect,
                    boolean isSelected
            ) {
                graphics.setFont(font);
                graphics.setColor(isSelected ? Color.WHITE : PRIMARY_DARK);
                graphics.drawString(title, textRect.x, textRect.y + metrics.getAscent());
            }
        });
    }

    public static JPanel sectionCard(String title, String subtitle) {
        JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new RoundedBorder(16, BORDER),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        heading.add(titleLabel);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitleLabel);
        card.add(heading, BorderLayout.NORTH);
        return card;
    }

    public static JPanel navigationPlaceholder(String text) {
        JPanel page = pagePanel();
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(TEXT_SECONDARY);

        GridBagConstraints gbc = pageConstraints(0);
        gbc.weighty = 1.0;
        page.add(label, gbc);
        return page;
    }

    public static JPanel kpiCard(String title, String value, String detail, Color start, Color end, boolean featured) {
        JPanel card = new GradientCard(start, end, 18);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        card.setPreferredSize(new Dimension(220, featured ? 148 : 130));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLabel.setForeground(new Color(238, 247, 255));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, featured ? 34 : 30));
        valueLabel.setForeground(Color.WHITE);

        JLabel detailLabel = new JLabel(detail);
        detailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        detailLabel.setForeground(new Color(226, 239, 249));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(titleLabel);
        text.add(Box.createVerticalStrut(featured ? 14 : 10));
        text.add(valueLabel);
        text.add(Box.createVerticalStrut(4));
        text.add(detailLabel);

        card.add(text, BorderLayout.CENTER);
        return card;
    }

    public static JPanel filterField(String labelText, JComponent field) {
        return filterField(labelText, field, 190);
    }

    public static JPanel filterField(String labelText, JComponent field, int width) {
        JPanel block = new JPanel();
        block.setOpaque(false);
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));

        JLabel label = label(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComponent styledField = styleField(field, width);
        styledField.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(label);
        block.add(Box.createVerticalStrut(6));
        block.add(styledField);
        return block;
    }

    public static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setForeground(new Color(70, 82, 96));
        return label;
    }

    public static JComponent styleField(JComponent component) {
        return styleField(component, 190);
    }

    public static JComponent styleField(JComponent component, int width) {
        component.setPreferredSize(new Dimension(width, 34));
        component.setMinimumSize(new Dimension(Math.min(width, 150), 34));
        component.setMaximumSize(new Dimension(Math.max(width, 260), 34));
        component.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        component.setBackground(Color.WHITE);
        component.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 200, 200)),
                new EmptyBorder(6, 8, 6, 8)
        ));
        if (component instanceof JComboBox<?>) {
            styleComboBox((JComboBox<?>) component);
        }
        return component;
    }

    public static JSpinner dateSpinner(Date value) {
        JSpinner spinner = new JSpinner(new SpinnerDateModel(value, null, null, Calendar.DAY_OF_MONTH));
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, DATE_PATTERN);
        spinner.setEditor(editor);
        editor.getTextField().setFont(new Font("Segoe UI", Font.PLAIN, 13));
        editor.getTextField().setHorizontalAlignment(JTextField.LEFT);
        return spinner;
    }

    public static String dateText(JSpinner spinner) {
        Object value = spinner.getValue();
        if (!(value instanceof Date)) {
            return "";
        }
        return FILTER_DATE_FORMAT.format((Date) value);
    }

    public static JCheckBox checkBox(String text) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setOpaque(false);
        checkBox.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        checkBox.setForeground(TEXT_SECONDARY);
        checkBox.setFocusPainted(false);
        checkBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return checkBox;
    }

    public static JComboBox<String> combo(String... items) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        styleComboBox(comboBox);
        return comboBox;
    }

    public static JButton textButton(String text) {
        JButton button = new JButton(text);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setForeground(PRIMARY);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(8, 10, 8, 10));
        return button;
    }

    public static JPanel inlineActionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 24));
        panel.setOpaque(false);
        return panel;
    }

    public static void setTextButtonEnabled(JButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setForeground(enabled ? PRIMARY : new Color(155, 155, 155));
        button.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    public static void styleDashboardTable(JTable table) {
        Color cellDivider = new Color(232, 236, 240);

        table.setRowHeight(42);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setBackground(Color.WHITE);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(ROW_SELECTION);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setFillsViewportHeight(true);
        table.setDefaultEditor(Object.class, null);
        table.setCellSelectionEnabled(false);
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFocusable(false);
        table.getTableHeader().setReorderingAllowed(false);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 42));
        Font headerFont = new Font("Segoe UI", Font.BOLD, 12);
        header.setFont(headerFont);
        header.setForeground(Color.WHITE);
        header.setBackground(PRIMARY);
        header.setBorder(new LineBorder(new Color(190, 204, 218)));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setBackground(PRIMARY);
                label.setForeground(Color.WHITE);
                label.setFont(headerFont);
                label.setBorder(new CompoundBorder(
                        new MatteBorder(0, 0, 1, 1, Color.WHITE),
                        new EmptyBorder(0, 16, 0, 14)
                ));
                label.setOpaque(true);
                return label;
            }
        });

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
                label.setHorizontalAlignment(column == table.getColumnCount() - 1 ? SwingConstants.CENTER : SwingConstants.LEFT);
                label.setBackground(isSelected ? ROW_SELECTION : Color.WHITE);
                label.setForeground(column == table.getColumnCount() - 1 ? PRIMARY : TEXT_PRIMARY);
                label.setFont(new Font(column == table.getColumnCount() - 1 ? "Segoe UI Semibold" : "Segoe UI", Font.PLAIN, 13));
                label.setBorder(new CompoundBorder(
                        new MatteBorder(0, 0, 1, 1, cellDivider),
                        new EmptyBorder(0, 16, 0, 14)
                ));
                return label;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
    }

    public static JScrollPane tableScroll(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(BORDER));
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setPreferredSize(new Dimension(0, 230));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    private static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setUI(new BasicComboBoxUI() {
            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(Color.WHITE);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });
        comboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(new EmptyBorder(0, 0, 0, 0));
                label.setBackground(isSelected ? new Color(224, 224, 224) : new Color(245, 245, 245));
                label.setForeground(Color.BLACK);
                list.setBackground(new Color(245, 245, 245));
                list.setSelectionBackground(new Color(224, 224, 224));
                list.setSelectionForeground(Color.BLACK);
                return label;
            }
        });
    }

    private static class GradientCard extends JPanel {
        private final Color start;
        private final Color end;
        private final int radius;

        private GradientCard(Color start, Color end, int radius) {
            this.start = start;
            this.end = end;
            this.radius = radius;
            setOpaque(false);
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, start, getWidth(), getHeight(), end));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        private RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }

        public Insets getBorderInsets(Component component) {
            return new Insets(10, 10, 10, 10);
        }
    }
}
