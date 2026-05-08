package com.kgm.ui.dialog;

import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

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
        List<String> lines = messageLines(text);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        if (lines.size() > 1) {
            for (String line : lines) {
                JPanel row = messageRow(type, line);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(row);
                panel.add(Box.createVerticalStrut(8));
            }
        } else {
            panel.add(paragraph(text));
        }

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setPreferredSize(new Dimension(500, preferredMessageHeight(lines, text)));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scroll;
    }

    private static JTextArea paragraph(String message) {
        JTextArea text = new JTextArea(message);
        text.setEditable(false);
        text.setFocusable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        text.setForeground(HomeViewHelper.TEXT_PRIMARY);
        text.setBackground(Color.WHITE);
        text.setBorder(BorderFactory.createEmptyBorder());
        text.setAlignmentX(Component.LEFT_ALIGNMENT);
        return text;
    }

    private static JPanel messageRow(Type type, String message) {
        boolean error = type == Type.ERROR;
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(error ? ERROR_SURFACE : Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(error ? ERROR_BORDER : HomeViewHelper.BORDER),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)
        ));

        JLabel badge = new JLabel(error ? "!" : "-");
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setVerticalAlignment(SwingConstants.CENTER);
        badge.setPreferredSize(new Dimension(22, 22));
        badge.setOpaque(true);
        badge.setBackground(error ? Type.ERROR.accent : HomeViewHelper.PRIMARY);
        badge.setForeground(Color.WHITE);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JLabel label = new JLabel("<html><body style='width:360px'>"
                + htmlEscape(message)
                + "</body></html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(HomeViewHelper.TEXT_PRIMARY);

        row.add(badge, BorderLayout.WEST);
        row.add(label, BorderLayout.CENTER);
        return row;
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

    private static List<String> messageLines(String message) {
        List<String> lines = new ArrayList<>();
        for (String line : message.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        if (lines.isEmpty()) {
            lines.add("-");
        }
        return lines;
    }

    private static String htmlEscape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static int preferredMessageHeight(List<String> lines, String message) {
        if (lines.size() > 1) {
            return Math.min(340, 36 + lines.size() * 48);
        }
        int displayLines = Math.max(3, message.length() / 72 + message.split("\\R", -1).length);
        return Math.min(260, 42 + displayLines * 18);
    }

    private static Window owner(Component parent) {
        if (parent instanceof Window window) {
            return window;
        }
        return parent == null ? null : SwingUtilities.getWindowAncestor(parent);
    }
}
