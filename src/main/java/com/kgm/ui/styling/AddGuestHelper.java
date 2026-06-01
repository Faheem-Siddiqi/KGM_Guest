package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public final class AddGuestHelper {
    private static final int CONTENT_WIDTH = 860;
    private static final int FIELD_WIDTH = 300;
    private static final int FIELD_MIN_WIDTH = 220;
    private static final int PAGE_SECTION_GAP = 18;
    private static final int HEADER_TO_BREADCRUMB_GAP = 8;
    private static final Color DROPDOWN_BACKGROUND = new Color(247, 250, 255);
    private static final Color DROPDOWN_SELECTION = HomeViewHelper.ROW_SELECTION;
    private static final String REQUIRED_MARKER_COLOR = "#d92d20";
    private static final String AUTO_COMPLETE_ENABLED = "kgm.autoCompleteEnabled";
    private static final String AUTO_COMPLETE_EDITOR = "kgm.autoCompleteEditor";

    private AddGuestHelper() {
    }

    public static JPanel pagePanel() {
        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(Color.WHITE);
        page.setBorder(new EmptyBorder(40, 40, 40, 40));
        return page;
    }

    public static JPanel cardPanel() {
        JPanel card = new JPanel(new GridBagLayout()) {
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = responsiveWidth(this);
                return size;
            }

            public Dimension getMinimumSize() {
                Dimension size = super.getMinimumSize();
                size.width = Math.min(size.width, 320);
                return size;
            }
        };
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

    public static GridBagConstraints pageConstraints(int y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.insets = new Insets(0, 0, PAGE_SECTION_GAP, 0);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weightx = 0.0;
        return gbc;
    }

    public static GridBagConstraints headerPageConstraints(int y) {
        GridBagConstraints gbc = pageConstraints(y);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        if (y == 0) {
            gbc.insets = new Insets(0, 0, HEADER_TO_BREADCRUMB_GAP, 0);
        }
        return gbc;
    }

    public static JPanel screenHeader(Runnable onBack) {
        return screenHeader("Add Guest", "Enter guest information", onBack);
    }

    public static JPanel screenHeader(String titleText, String subtitleText, Runnable onBack) {
        JPanel header = new JPanel(new BorderLayout()) {
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = responsiveWidth(this);
                return size;
            }
        };
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(0, 0, 4, 0));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setBackground(Color.WHITE);

        JLabel title = new JLabel(titleText);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(HomeViewHelper.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(subtitle);

        JButton back = new JButton("BACK");
        styleBack(back);
        back.addActionListener(e -> onBack.run());

        header.add(titleBlock, BorderLayout.WEST);
        header.add(back, BorderLayout.EAST);
        return header;
    }

    public static JPanel breadcrumb(String[] labels, Runnable[] actions) {
        JPanel breadcrumb = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)) {
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = responsiveWidth(this);
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
                size.width = responsiveWidth(this);
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
        link.setForeground(HomeViewHelper.PRIMARY);
        link.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.setBorder(new EmptyBorder(4, 0, 4, 0));
        return link;
    }

    private static JLabel breadcrumbSlash() {
        JLabel slash = new JLabel("/");
        slash.setForeground(HomeViewHelper.PRIMARY);
        slash.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        slash.setBorder(new EmptyBorder(4, 2, 4, 2));
        return slash;
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

        JLabel title = new JLabel("Add Guest");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Enter guest information");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(HomeViewHelper.TEXT_SECONDARY);
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
        section.setForeground(HomeViewHelper.TEXT_PRIMARY);

        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 4;
        panel.add(section, gbc);
        gbc.gridwidth = 1;
        return y + 1;
    }

    public static JLabel addField(JPanel panel, GridBagConstraints gbc, int y, int xOffset, String labelText, JComponent field) {
        return addField(panel, gbc, y, xOffset, labelText, field, false);
    }

    public static JLabel addRequiredField(JPanel panel, GridBagConstraints gbc, int y, int xOffset, String labelText, JComponent field) {
        return addField(panel, gbc, y, xOffset, labelText, field, true);
    }

    private static JLabel addField(
            JPanel panel,
            GridBagConstraints gbc,
            int y,
            int xOffset,
            String labelText,
            JComponent field,
            boolean required
    ) {
        JPanel fieldBlock = new JPanel();
        fieldBlock.setLayout(new BoxLayout(fieldBlock, BoxLayout.Y_AXIS));
        fieldBlock.setBackground(Color.WHITE);

        JLabel label = label(labelText, required);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComponent styledField = styleField(field);
        styledField.setAlignmentX(Component.LEFT_ALIGNMENT);

        fieldBlock.add(label);
        fieldBlock.add(Box.createVerticalStrut(6));
        fieldBlock.add(styledField);

        gbc.gridx = xOffset;
        gbc.gridy = y;
        gbc.gridwidth = 2;
        panel.add(fieldBlock, gbc);
        gbc.gridwidth = 1;
        return label;
    }

    public static JLabel label(String text) {
        return label(text, false);
    }

    private static JLabel label(String text, boolean required) {
        JLabel label = new JLabel(text);
        if (required) {
            label.setText(requiredLabelText(text));
        }
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setForeground(new Color(70, 82, 96));
        return label;
    }

    private static String requiredLabelText(String text) {
        return "<html>" + html(text) + "<span style='color:" + REQUIRED_MARKER_COLOR + ";'> *</span></html>";
    }

    public static void setRequiredLabelText(JLabel label, String text) {
        if (label != null) {
            label.setText(requiredLabelText(text));
        }
    }

    private static String html(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    @SuppressWarnings("unchecked")
    public static JComponent styleField(JComponent component) {
        component.setPreferredSize(new Dimension(FIELD_WIDTH, 34));
        component.setMinimumSize(new Dimension(FIELD_MIN_WIDTH, 34));
        component.setMaximumSize(new Dimension(FIELD_WIDTH, 34));
        component.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        component.setBackground(Color.WHITE);
        component.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 200, 200)),
                new EmptyBorder(6, 8, 6, 8)
        ));
        if (component instanceof JComboBox<?>) {
            JComboBox<?> comboBox = (JComboBox<?>) component;
            styleComboBox(comboBox);
            if (comboBox.isEditable() && Boolean.TRUE.equals(comboBox.getClientProperty(AUTO_COMPLETE_ENABLED))) {
                enableAutoComplete((JComboBox<String>) comboBox);
            }
        }
        return component;
    }

    public static JComboBox<String> combo(String... items) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        styleComboBox(comboBox);
        return comboBox;
    }

    public static JComboBox<String> editableCombo(String... items) {
        JComboBox<String> comboBox = combo(items);
        comboBox.setEditable(true);
        comboBox.putClientProperty(AUTO_COMPLETE_ENABLED, true);
        styleComboBox(comboBox);
        enableAutoComplete(comboBox);
        return comboBox;
    }

    public static void stylePrimary(JButton button) {
        button.setBackground(HomeViewHelper.PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
    }

    public static void styleReset(JButton button) {
        button.setBackground(new Color(245, 245, 245));
        button.setForeground(new Color(80, 80, 80));
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setBorder(new CompoundBorder(
                new LineBorder(new Color(170, 170, 170)),
                new EmptyBorder(8, 16, 8, 16)
        ));
    }

    public static void styleBack(JButton button) {
        button.setForeground(HomeViewHelper.PRIMARY);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        button.setBorder(new EmptyBorder(7, 16, 7, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static JPanel actionsPanel() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(Color.WHITE);
        return actions;
    }

    public static JTextArea remarksArea(String text) {
        JTextArea remarks = new JTextArea(text);
        remarks.setLineWrap(true);
        remarks.setWrapStyleWord(true);
        remarks.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        remarks.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 200, 200)),
                new EmptyBorder(8, 8, 8, 8)
        ));
        return remarks;
    }

    private static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setUI(new BasicComboBoxUI() {
            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(Color.WHITE);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });
        comboBox.setFocusable(comboBox.isEditable());
        comboBox.setRequestFocusEnabled(comboBox.isEditable());
        if (comboBox.isEditable()) {
            Component editor = comboBox.getEditor().getEditorComponent();
            editor.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            editor.setBackground(Color.WHITE);
            editor.setForeground(HomeViewHelper.TEXT_PRIMARY);
            if (editor instanceof JComponent) {
                ((JComponent) editor).setBorder(new EmptyBorder(0, 0, 0, 0));
            }
        }
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
                label.setBackground(isSelected ? DROPDOWN_SELECTION : DROPDOWN_BACKGROUND);
                label.setForeground(HomeViewHelper.TEXT_PRIMARY);
                list.setBackground(DROPDOWN_BACKGROUND);
                list.setSelectionBackground(DROPDOWN_SELECTION);
                list.setSelectionForeground(HomeViewHelper.TEXT_PRIMARY);
                return label;
            }
        });
    }

    private static int responsiveWidth(Component component) {
        Container parent = component.getParent();
        int width = parent == null ? 0 : parent.getWidth();
        if (width <= 0) {
            return CONTENT_WIDTH;
        }
        return Math.max(320, Math.min(CONTENT_WIDTH, width));
    }

    private static void enableAutoComplete(JComboBox<String> comboBox) {
        JTextField editor = (JTextField) comboBox.getEditor().getEditorComponent();
        if (editor == comboBox.getClientProperty(AUTO_COMPLETE_EDITOR)) {
            return;
        }

        comboBox.putClientProperty(AUTO_COMPLETE_EDITOR, editor);
        final boolean[] updating = {false};
        comboBox.setMaximumRowCount(comboBox.getItemCount());

        editor.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() != KeyEvent.VK_ENTER || updating[0]) {
                    return;
                }

                String match = findComboMatch(comboBox, editor.getText());
                if (match == null) {
                    return;
                }

                updating[0] = true;
                comboBox.setSelectedItem(match);
                editor.setText(match);
                editor.setCaretPosition(match.length());
                comboBox.setPopupVisible(false);
                updating[0] = false;
                event.consume();
            }
        });

        editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                autoComplete();
            }

            public void removeUpdate(DocumentEvent e) {
                autoComplete();
            }

            public void changedUpdate(DocumentEvent e) {
                autoComplete();
            }

            private void autoComplete() {
                if (updating[0]) {
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    String typed = editor.getText();
                    int selectionStart = editor.getSelectionStart();
                    if (selectionStart > 0 && selectionStart < typed.length()) {
                        typed = typed.substring(0, selectionStart);
                    }
                    typed = typed.trim();

                    if (typed.length() < 2) {
                        comboBox.setPopupVisible(false);
                        return;
                    }

                    String match = findComboMatch(comboBox, typed);
                    if (match == null) {
                        comboBox.setPopupVisible(false);
                        return;
                    }

                    updating[0] = true;
                    comboBox.setSelectedItem(match);
                    editor.setText(typed);
                    editor.setCaretPosition(typed.length());
                    updating[0] = false;
                    if (comboBox.isDisplayable() && editor.hasFocus()) {
                        comboBox.setPopupVisible(true);
                    }
                });
            }
        });
    }

    private static String findComboMatch(JComboBox<String> comboBox, String typed) {
        String typedLower = typed.trim().toLowerCase();
        if (typedLower.length() < 2) {
            return null;
        }

        String closestMatch = null;
        int closestScore = Integer.MAX_VALUE;
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            String item = comboBox.getItemAt(i);
            if (isFreeTextOption(item)) {
                continue;
            }
            if (item != null && item.toLowerCase().startsWith(typedLower)) {
                return item;
            }
            if (item == null) {
                continue;
            }

            String itemLower = item.toLowerCase();
            int score = matchScore(itemLower, typedLower);
            if (score < closestScore) {
                closestScore = score;
                closestMatch = item;
            }
        }

        int allowedDistance = Math.max(1, typedLower.length() / 3);
        return closestScore <= allowedDistance ? closestMatch : null;
    }

    private static boolean isFreeTextOption(String item) {
        return item != null && item.trim().toLowerCase().startsWith("others");
    }

    private static int matchScore(String item, String typed) {
        if (item.contains(typed)) {
            return 0;
        }

        int compareLength = Math.min(item.length(), typed.length());
        String itemStart = item.substring(0, compareLength);
        return levenshteinDistance(itemStart, typed);
    }

    private static int levenshteinDistance(String left, String right) {
        int[] costs = new int[right.length() + 1];
        for (int j = 0; j < costs.length; j++) {
            costs[j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            int previous = costs[0];
            costs[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int current = costs[j];
                int replacementCost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                costs[j] = Math.min(
                        Math.min(costs[j] + 1, costs[j - 1] + 1),
                        previous + replacementCost
                );
                previous = current;
            }
        }
        return costs[right.length()];
    }

    public static class RoundedBorder extends AbstractBorder {
        private final int radius;

        public RoundedBorder(int radius) {
            this.radius = radius;
        }

        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(220, 220, 220));
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }

        public Insets getBorderInsets(Component component) {
            return new Insets(10, 10, 10, 10);
        }
    }
}
