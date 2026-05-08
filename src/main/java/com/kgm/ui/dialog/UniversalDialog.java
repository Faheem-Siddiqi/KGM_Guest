package com.kgm.ui.dialog;

import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class UniversalDialog {
    public enum Type {
        INFO(HomeViewHelper.PRIMARY),
        SUCCESS(new Color(28, 137, 85)),
        WARNING(new Color(176, 76, 19)),
        ERROR(new Color(217, 45, 32));

        private final Color accent;

        Type(Color accent) {
            this.accent = accent;
        }
    }

    private static final Color FOOTER = new Color(247, 249, 251);
    private static final Color ERROR_SURFACE = new Color(255, 246, 245);
    private static final Color ERROR_BORDER = new Color(254, 205, 202);
    private static final int BODY_WIDTH = 500;
    private static final int MESSAGE_BOX_WIDTH = 456;
    private static final int MESSAGE_TEXT_WIDTH = 400;
    private static final int WRAP_COLUMNS = 54;
    private static final int SCROLLABLE_SECTION_ROWS = 10;
    public static final String SECTION_SEPARATOR = "\n\n::kgm-dialog-section::\n\n";

    private UniversalDialog() {
    }

    public static void message(Component parent, Type type, String title, String message) {
        option(parent, type, title, message, "OK");
    }

    public static int option(
            Component parent,
            Type type,
            String title,
            String message,
            String primaryOption,
            String... secondaryOptions
    ) {
        Window owner = owner(parent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        int[] selected = {-1};

        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(content(dialog, selected, type, title, message, primaryOption, secondaryOptions));
        dialog.pack();
        dialog.setMinimumSize(new Dimension(430, 220));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return selected[0];
    }

    private static JPanel content(
            JDialog dialog,
            int[] selected,
            Type type,
            String title,
            String message,
            String primaryOption,
            String[] secondaryOptions
    ) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(header(title, type.accent), BorderLayout.NORTH);
        root.add(body(type, message), BorderLayout.CENTER);
        root.add(footer(dialog, selected, type.accent, primaryOption, secondaryOptions), BorderLayout.SOUTH);
        return root;
    }

    private static JPanel header(String title, Color accent) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(accent);
        header.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));

        JLabel titleLabel = new JLabel(title == null || title.isBlank() ? "Message" : title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLabel.setForeground(Color.WHITE);
        header.add(titleLabel, BorderLayout.WEST);
        return header;
    }

    private static JComponent body(Type type, String message) {
        String text = message == null || message.isBlank() ? "-" : message.trim();
        List<String> sections = messageSections(text);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        for (int index = 0; index < sections.size(); index++) {
            JPanel messageBox = messageBox(type, sections.get(index));
            messageBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(messageBox);
            if (index < sections.size() - 1) {
                panel.add(Box.createVerticalStrut(10));
            }
        }

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setPreferredSize(new Dimension(BODY_WIDTH, preferredMessageHeight(sections)));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scroll;
    }

    private static JPanel messageBox(Type type, String message) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(surface(type));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border(type)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel badge = new JLabel(badgeText(type));
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setVerticalAlignment(SwingConstants.CENTER);
        badge.setPreferredSize(new Dimension(24, 24));
        badge.setOpaque(true);
        badge.setBackground(type.accent);
        badge.setForeground(Color.WHITE);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));

        String[] parts = headingAndBody(message);
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        if (!parts[0].isEmpty()) {
            JLabel heading = new JLabel(parts[0]);
            heading.setFont(new Font("Segoe UI", Font.BOLD, 13));
            heading.setForeground(HomeViewHelper.TEXT_PRIMARY);
            heading.setAlignmentX(Component.LEFT_ALIGNMENT);
            textPanel.add(heading);
            textPanel.add(Box.createVerticalStrut(4));
        }

        int contentRows = Math.max(1, wrappedRows(parts[1], WRAP_COLUMNS));
        boolean scrollableText = isScrollableSection(parts[0]) && contentRows > SCROLLABLE_SECTION_ROWS;
        int visibleRows = scrollableText ? SCROLLABLE_SECTION_ROWS : contentRows;

        JTextArea text = new JTextArea(parts[1]);
        text.setEditable(false);
        text.setFocusable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(false);
        text.setRows(contentRows);
        text.setColumns(0);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        text.setForeground(HomeViewHelper.TEXT_PRIMARY);
        text.setBackground(row.getBackground());
        text.setBorder(BorderFactory.createEmptyBorder());
        text.setAlignmentX(Component.LEFT_ALIGNMENT);

        int lineHeight = text.getFontMetrics(text.getFont()).getHeight();
        int textHeight = Math.max(42, visibleRows * lineHeight + 4);
        int contentHeight = Math.max(textHeight, contentRows * lineHeight + 4);
        int headingHeight = parts[0].isEmpty() ? 0 : 22;
        Dimension textSize = new Dimension(MESSAGE_TEXT_WIDTH, contentHeight);
        text.setPreferredSize(textSize);
        text.setMinimumSize(textSize);
        text.setMaximumSize(textSize);

        if (scrollableText) {
            JScrollPane sectionScroll = new JScrollPane(text);
            sectionScroll.setBorder(BorderFactory.createLineBorder(HomeViewHelper.BORDER));
            sectionScroll.getViewport().setBackground(row.getBackground());
            sectionScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            sectionScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            Dimension scrollSize = new Dimension(MESSAGE_TEXT_WIDTH, textHeight);
            sectionScroll.setPreferredSize(scrollSize);
            sectionScroll.setMinimumSize(scrollSize);
            sectionScroll.setMaximumSize(scrollSize);
            sectionScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
            textPanel.add(sectionScroll);
        } else {
            Dimension visibleTextSize = new Dimension(MESSAGE_TEXT_WIDTH, textHeight);
            text.setPreferredSize(visibleTextSize);
            text.setMinimumSize(visibleTextSize);
            text.setMaximumSize(visibleTextSize);
            textPanel.add(text);
        }
        Dimension panelSize = new Dimension(MESSAGE_TEXT_WIDTH, textHeight + headingHeight);
        textPanel.setPreferredSize(panelSize);
        textPanel.setMinimumSize(panelSize);
        textPanel.setMaximumSize(panelSize);

        int boxHeight = textHeight + headingHeight + 22;
        Dimension boxSize = new Dimension(MESSAGE_BOX_WIDTH, boxHeight);
        row.setPreferredSize(boxSize);
        row.setMinimumSize(boxSize);
        row.setMaximumSize(boxSize);

        row.add(badge, BorderLayout.WEST);
        row.add(textPanel, BorderLayout.CENTER);
        return row;
    }

    private static Color surface(Type type) {
        return type == Type.ERROR ? ERROR_SURFACE : Color.WHITE;
    }

    private static Color border(Type type) {
        return type == Type.ERROR ? ERROR_BORDER : HomeViewHelper.BORDER;
    }

    private static String badgeText(Type type) {
        return switch (type) {
            case SUCCESS -> "OK";
            case WARNING, ERROR -> "!";
            case INFO -> "i";
        };
    }

    private static JPanel footer(
            JDialog dialog,
            int[] selected,
            Color accent,
            String primaryOption,
            String[] secondaryOptions
    ) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setBackground(FOOTER);
        footer.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        for (int index = secondaryOptions.length - 1; index >= 0; index--) {
            String option = secondaryOptions[index];
            int optionIndex = index + 1;
            JButton secondary = secondaryButton(option);
            secondary.addActionListener(event -> {
                selected[0] = optionIndex;
                dialog.dispose();
            });
            footer.add(secondary);
        }

        JButton primary = primaryButton(primaryOption, accent);
        primary.addActionListener(event -> {
            selected[0] = 0;
            dialog.dispose();
        });
        footer.add(primary);
        dialog.getRootPane().setDefaultButton(primary);
        return footer;
    }

    private static JButton primaryButton(String text, Color accent) {
        JButton button = new JButton(buttonText(text));
        button.setPreferredSize(buttonSize(button.getText(), 92, 34));
        button.setBackground(accent);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static JButton secondaryButton(String text) {
        JButton button = new JButton(buttonText(text));
        button.setPreferredSize(buttonSize(button.getText(), 82, 30));
        button.setBackground(Color.WHITE);
        button.setForeground(HomeViewHelper.TEXT_SECONDARY);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(HomeViewHelper.BORDER));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static Dimension buttonSize(String text, int minimumWidth, int padding) {
        return new Dimension(Math.max(minimumWidth, text.length() * 9 + padding), 34);
    }

    private static String buttonText(String text) {
        return text == null || text.isBlank() ? "OK" : text;
    }

    private static int preferredMessageHeight(List<String> sections) {
        int height = 36;
        for (String section : sections) {
            String[] parts = headingAndBody(section);
            int headingHeight = parts[0].isEmpty() ? 0 : 22;
            int rows = visibleRows(parts[0], parts[1]);
            height += 22 + headingHeight + Math.max(42, rows * 19);
        }
        height += Math.max(0, sections.size() - 1) * 10;
        return Math.min(420, height);
    }

    private static int visibleRows(String heading, String body) {
        int rows = Math.max(1, wrappedRows(body, WRAP_COLUMNS));
        if (isScrollableSection(heading)) {
            return Math.min(SCROLLABLE_SECTION_ROWS, rows);
        }
        return rows;
    }

    private static boolean isScrollableSection(String heading) {
        return "Skipped rows".equalsIgnoreCase(heading)
                || "Rows to review".equalsIgnoreCase(heading);
    }

    private static int wrappedRows(String message, int columns) {
        int rows = 0;
        for (String line : message.split("\\R", -1)) {
            rows += Math.max(1, line.length() / columns + 1);
        }
        return rows;
    }

    private static List<String> messageSections(String message) {
        List<String> sections = new ArrayList<>();
        for (String section : message.split(Pattern.quote(SECTION_SEPARATOR), -1)) {
            String trimmed = section.trim();
            if (!trimmed.isEmpty()) {
                sections.add(trimmed);
            }
        }
        if (sections.isEmpty()) {
            sections.add("-");
        }
        return sections;
    }

    private static String[] headingAndBody(String message) {
        String[] lines = message.split("\\R", 2);
        if (lines.length < 2 || lines[1].trim().isEmpty()) {
            return new String[]{"", message};
        }
        return new String[]{lines[0].trim(), lines[1].trim()};
    }

    private static Window owner(Component parent) {
        if (parent instanceof Window window) {
            return window;
        }
        return parent == null ? null : SwingUtilities.getWindowAncestor(parent);
    }
}
