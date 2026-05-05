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
    public static final Color PAGE_BACKGROUND = Color.WHITE;
    public static final Color TEXT_PRIMARY = new Color(35, 35, 35);
    public static final Color TEXT_SECONDARY = new Color(100, 100, 100);
    public static final Color BORDER = new Color(220, 220, 220);
    public static final Color TABLE_HEADER = new Color(246, 248, 250);
    public static final Color ROW_SELECTION = new Color(229, 242, 255);
    public static final Color PRIMARY = new Color(0, 112, 210);
    public static final Color DANGER = new Color(180, 60, 45);

    private AccommodationManagementHelper() {
    }

    public static JPanel pagePanel() {
        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(PAGE_BACKGROUND);
        page.setBorder(new EmptyBorder(40, 60, 40, 60));
        return page;
    }

    public static JPanel cardPanel() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new RoundedBorder(16),
                new EmptyBorder(24, 24, 24, 24)
        ));
        return card;
    }

    public static GridBagConstraints formConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 12, 10, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        return gbc;
    }

    public static int addFormHeader(JPanel panel, GridBagConstraints gbc, int y, Runnable onBack) {
        JPanel formHeader = new JPanel(new BorderLayout());
        formHeader.setBackground(Color.WHITE);
        formHeader.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                new EmptyBorder(4, 0, 12, 0)
        ));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setBackground(Color.WHITE);

        JLabel title = new JLabel("Accommodation Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Create, update, and manage accommodation inventory");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(subtitle);

        JButton back = new JButton("Back");
        styleBack(back);
        back.addActionListener(e -> onBack.run());

        formHeader.add(titleBlock, BorderLayout.WEST);
        formHeader.add(back, BorderLayout.EAST);

        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 4;
        panel.add(formHeader, gbc);
        gbc.gridwidth = 1;
        return y + 1;
    }

    public static int addSectionTitle(JPanel panel, GridBagConstraints gbc, int y, String text) {
        JLabel section = new JLabel(text);
        section.setFont(new Font("Segoe UI", Font.BOLD, 14));
        section.setForeground(new Color(60, 60, 60));

        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 4;
        panel.add(section, gbc);
        gbc.gridwidth = 1;
        return y + 1;
    }

    public static void addField(JPanel panel, GridBagConstraints gbc, int y, int xOffset, String labelText, JComponent field) {
        JPanel fieldBlock = fieldBlock(labelText, field);

        gbc.gridx = xOffset;
        gbc.gridy = y;
        gbc.gridwidth = 2;
        panel.add(fieldBlock, gbc);
        gbc.gridwidth = 1;
    }

    public static JPanel fieldBlock(String labelText, JComponent field) {
        JPanel fieldBlock = new JPanel();
        fieldBlock.setLayout(new BoxLayout(fieldBlock, BoxLayout.Y_AXIS));
        fieldBlock.setBackground(Color.WHITE);

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
        label.setForeground(new Color(70, 70, 70));
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

    public static JTextArea descriptionArea(String text) {
        JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 200, 200)),
                new EmptyBorder(8, 8, 8, 8)
        ));
        return area;
    }

    public static JPanel actionsPanel() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(Color.WHITE);
        return actions;
    }

    public static void stylePrimary(JButton button) {
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleSecondary(JButton button) {
        button.setBackground(new Color(245, 245, 245));
        button.setForeground(new Color(80, 80, 80));
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setBorder(new CompoundBorder(
                new LineBorder(new Color(170, 170, 170)),
                new EmptyBorder(8, 16, 8, 16)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleBack(JButton button) {
        button.setForeground(PRIMARY);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        button.setBorder(new EmptyBorder(7, 16, 7, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(ROW_SELECTION);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        header.setForeground(new Color(80, 80, 80));
        header.setBackground(TABLE_HEADER);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setBorder(new EmptyBorder(0, 12, 0, 12));
        renderer.setBackground(Color.WHITE);
        renderer.setForeground(TEXT_PRIMARY);
        table.setDefaultRenderer(Object.class, renderer);
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

        public RoundedBorder(int radius) {
            this.radius = radius;
        }

        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BORDER);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }

        public Insets getBorderInsets(Component component) {
            return new Insets(10, 10, 10, 10);
        }
    }
}
