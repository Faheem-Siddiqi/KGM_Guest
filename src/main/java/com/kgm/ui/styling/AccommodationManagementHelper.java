package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

public final class AccommodationManagementHelper {
    public static final int CONTENT_WIDTH = 860;
    public static final Color PAGE_BACKGROUND = Color.WHITE;
    public static final Color CARD_BACKGROUND = Color.WHITE;
    public static final Color TEXT_PRIMARY = new Color(35, 43, 54);
    public static final Color TEXT_SECONDARY = new Color(99, 115, 129);
    public static final Color BORDER = new Color(220, 226, 232);
    public static final Color TABLE_HEADER = Color.WHITE;
    public static final Color ROW_SELECTION = new Color(229, 242, 255);
    public static final Color PRIMARY = new Color(0, 112, 210);
    public static final Color DANGER = new Color(180, 60, 45);

    private AccommodationManagementHelper() {
    }

    public static JPanel pagePanel() {
        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(PAGE_BACKGROUND);
        page.setBorder(new EmptyBorder(40, 40, 40, 40));
        return page;
    }

    public static JPanel sectionCard(String title, String subtitle) {
        JPanel card = new JPanel(new BorderLayout(0, 18)) {
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = CONTENT_WIDTH;
                return size;
            }

            public Dimension getMinimumSize() {
                Dimension size = super.getMinimumSize();
                size.width = CONTENT_WIDTH;
                return size;
            }
        };
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(new CompoundBorder(
                new RoundedBorder(16),
                new EmptyBorder(24, 24, 24, 24)
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

    public static JPanel screenHeader(Runnable onBack) {
        return screenHeader(
                "Accommodation Management",
                "Manage categories, rooms, staff assignments, and amenity details.",
                onBack
        );
    }

    public static JPanel screenHeader(String titleText, String subtitleText, Runnable onBack) {
        JPanel header = new JPanel(new BorderLayout()) {
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = CONTENT_WIDTH;
                return size;
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 4, 0));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(titleText);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_PRIMARY);

        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_SECONDARY);

        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(subtitle);

        JButton back = textButton("BACK");
        back.addActionListener(e -> onBack.run());

        header.add(titleBlock, BorderLayout.WEST);
        header.add(back, BorderLayout.EAST);
        return header;
    }

    public static JPanel breadcrumb(String[] labels, Runnable[] actions) {
        JPanel breadcrumb = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)) {
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = CONTENT_WIDTH;
                return size;
            }
        };
        breadcrumb.setBackground(Color.WHITE);
        breadcrumb.setBorder(new EmptyBorder(0, 0, 2, 0));

        for (int index = 0; index < labels.length; index++) {
            JButton link = breadcrumbLink(labels[index]);
            if (index < actions.length && actions[index] != null) {
                int actionIndex = index;
                link.addActionListener(e -> actions[actionIndex].run());
            }
            breadcrumb.add(link);

            if (index < labels.length - 1) {
                breadcrumb.add(breadcrumbSlash());
            }
        }
        return breadcrumb;
    }

    public static JPanel returnToTop(Runnable action) {
        JPanel container = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)) {
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = CONTENT_WIDTH;
                return size;
            }
        };
        container.setBackground(Color.WHITE);

        JButton link = breadcrumbLink("Return to top");
        link.addActionListener(e -> action.run());
        container.add(link);
        return container;
    }

    private static JButton breadcrumbLink(String text) {
        JButton link = new JButton(text);
        link.setContentAreaFilled(false);
        link.setBorderPainted(false);
        link.setFocusPainted(false);
        link.setOpaque(false);
        link.setForeground(new Color(0, 112, 210));
        link.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.setBorder(new EmptyBorder(4, 0, 4, 0));
        return link;
    }

    private static JLabel breadcrumbSlash() {
        JLabel slash = new JLabel("/");
        slash.setForeground(new Color(0, 112, 210));
        slash.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        slash.setBorder(new EmptyBorder(4, 2, 4, 2));
        return slash;
    }

    public static GridBagConstraints pageConstraints(int y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.insets = new Insets(0, 0, 18, 0);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weightx = 0;
        return gbc;
    }

    public static GridBagConstraints formConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 16, 16);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        return gbc;
    }

    public static JPanel inlineField(String labelText, JComponent field) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        row.setOpaque(false);

        JLabel label = label(labelText);
        label.setPreferredSize(new Dimension(105, 34));
        label.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(label);
        row.add(styleField(field));
        return row;
    }

    public static JPanel fieldBlock(String labelText, JComponent field) {
        JPanel fieldBlock = new JPanel();
        fieldBlock.setLayout(new BoxLayout(fieldBlock, BoxLayout.Y_AXIS));
        fieldBlock.setOpaque(false);

        JLabel label = label(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComponent styledField = styleField(field);
        styledField.setAlignmentX(Component.LEFT_ALIGNMENT);

        fieldBlock.add(label);
        fieldBlock.add(Box.createVerticalStrut(6));
        fieldBlock.add(styledField);
        return fieldBlock;
    }

    public static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setForeground(new Color(70, 82, 96));
        return label;
    }

    public static JComponent styleField(JComponent component) {
        component.setPreferredSize(new Dimension(340, 34));
        component.setMinimumSize(new Dimension(280, 34));
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
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

    public static JComboBox<String> combo(String... items) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        styleComboBox(comboBox);
        return comboBox;
    }

    public static JTextField placeholderField(String placeholder) {
        return new PlaceholderTextField(placeholder);
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

    public static JButton dangerTextButton(String text) {
        JButton button = textButton(text);
        button.setForeground(DANGER);
        return button;
    }

    public static void setTextButtonEnabled(JButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setForeground(enabled ? PRIMARY : new Color(155, 155, 155));
        button.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    public static void setDangerTextButtonEnabled(JButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setForeground(enabled ? DANGER : new Color(155, 155, 155));
        button.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    public static JPanel textActionsPanel() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        return actions;
    }

    public static JPanel centeredTextActionsPanel() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        actions.setOpaque(false);
        return actions;
    }

    public static void styleTable(JTable table) {
        Color cellDivider = new Color(232, 236, 240);

        table.setRowHeight(44);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
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
        Font tableHeaderFont = new Font("Segoe UI", Font.BOLD, 12);
        header.setFont(tableHeaderFont);
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
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBackground(PRIMARY);
                label.setForeground(Color.WHITE);
                label.setFont(tableHeaderFont);
                label.setBorder(new CompoundBorder(
                        new MatteBorder(0, 0, 1, 1, Color.WHITE),
                        new EmptyBorder(0, 14, 0, 14)
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
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setBackground(isSelected ? ROW_SELECTION : Color.WHITE);
                label.setForeground(TEXT_PRIMARY);
                label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                label.setBorder(new CompoundBorder(
                        new MatteBorder(0, 0, 1, 1, cellDivider),
                        new EmptyBorder(0, 16, 0, 14)
                ));
                return label;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
    }

    public static JLabel emptyState(String text) {
        JLabel empty = new JLabel(text, SwingConstants.CENTER);
        empty.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        empty.setForeground(TEXT_SECONDARY);
        empty.setBorder(new CompoundBorder(
                new LineBorder(BORDER),
                new EmptyBorder(34, 20, 34, 20)
        ));
        return empty;
    }

    public static JPanel amenityChip(String text, Runnable onDelete) {
        JPanel chip = new JPanel(new BorderLayout(8, 0));
        chip.setBackground(Color.WHITE);
        chip.setBorder(new CompoundBorder(
                new RoundedBorder(12, BORDER),
                new EmptyBorder(6, 10, 6, 6)
        ));

        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(TEXT_PRIMARY);

        JButton delete = dangerTextButton("Delete");
        delete.setBorder(new EmptyBorder(2, 6, 2, 6));
        delete.addActionListener(e -> onDelete.run());

        chip.add(label, BorderLayout.CENTER);
        chip.add(delete, BorderLayout.EAST);
        return chip;
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

    public static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        public RoundedBorder(int radius) {
            this(radius, BORDER);
        }

        public RoundedBorder(int radius, Color color) {
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

    private static class PlaceholderTextField extends JTextField {
        private final String placeholder;

        private PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!getText().isEmpty() || isFocusOwner()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setFont(getFont());
            g2.setColor(new Color(145, 145, 145));
            Insets insets = getInsets();
            FontMetrics metrics = g2.getFontMetrics();
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(placeholder, insets.left, y);
            g2.dispose();
        }
    }
}
