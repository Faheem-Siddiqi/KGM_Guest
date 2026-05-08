package com.kgm.ui.styling;

import javax.swing.*;
import java.awt.*;

public final class DialogHelper {
    private static final Color FOOTER = new Color(247, 249, 251);
    private static final Color SUCCESS = new Color(28, 137, 85);
    private static final Color WARNING = new Color(176, 76, 19);
    private static final Color ERROR = new Color(180, 60, 45);

    private DialogHelper() {
    }

    public static void success(Component parent, String message) {
        show(parent, "Success", message, "OK", SUCCESS);
    }

    public static void info(Component parent, String title, String message) {
        show(parent, title, message, "OK", HomeViewHelper.PRIMARY);
    }

    public static void warning(Component parent, String title, String message) {
        show(parent, title, message, "OK", WARNING);
    }

    public static void error(Component parent, String title, String message) {
        show(parent, title, message, "OK", ERROR);
    }

    public static int option(
            Component parent,
            String title,
            String message,
            String primaryOption,
            String secondaryOption
    ) {
        return showOption(parent, title, message, HomeViewHelper.PRIMARY, primaryOption, secondaryOption);
    }

    private static void show(Component parent, String title, String message, String buttonText, Color accent) {
        showOption(parent, title, message, accent, buttonText, null);
    }

    private static int showOption(
            Component parent,
            String title,
            String message,
            Color accent,
            String primaryOption,
            String secondaryOption
    ) {
        Window owner = owner(parent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        int[] selected = {-1};

        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(content(dialog, selected, title, message, accent, primaryOption, secondaryOption));
        dialog.pack();
        dialog.setMinimumSize(new Dimension(430, 220));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return selected[0];
    }

    private static JPanel content(
            JDialog dialog,
            int[] selected,
            String title,
            String message,
            Color accent,
            String primaryOption,
            String secondaryOption
    ) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(header(title, accent), BorderLayout.NORTH);
        root.add(body(message), BorderLayout.CENTER);
        root.add(footer(dialog, selected, accent, primaryOption, secondaryOption), BorderLayout.SOUTH);
        return root;
    }

    private static JPanel header(String title, Color accent) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(accent);
        header.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLabel.setForeground(Color.WHITE);
        header.add(titleLabel, BorderLayout.WEST);
        return header;
    }

    private static JComponent body(String message) {
        JTextArea text = new JTextArea(message == null || message.isBlank() ? "-" : message);
        text.setEditable(false);
        text.setFocusable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        text.setForeground(HomeViewHelper.TEXT_PRIMARY);
        text.setBackground(Color.WHITE);
        text.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JScrollPane scroll = new JScrollPane(text);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setPreferredSize(new Dimension(460, Math.min(260, preferredMessageHeight(text))));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scroll;
    }

    private static JPanel footer(
            JDialog dialog,
            int[] selected,
            Color accent,
            String primaryOption,
            String secondaryOption
    ) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setBackground(FOOTER);
        footer.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        if (secondaryOption != null) {
            JButton secondary = secondaryButton(secondaryOption);
            secondary.addActionListener(event -> {
                selected[0] = 1;
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
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(Math.max(92, text.length() * 9 + 34), 34));
        button.setBackground(accent);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(Math.max(82, text.length() * 9 + 30), 34));
        button.setBackground(Color.WHITE);
        button.setForeground(HomeViewHelper.TEXT_SECONDARY);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(HomeViewHelper.BORDER));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static int preferredMessageHeight(JTextArea text) {
        int lines = Math.max(3, text.getLineCount() + text.getText().length() / 72);
        return Math.min(260, 42 + lines * 18);
    }

    private static Window owner(Component parent) {
        if (parent instanceof Window window) {
            return window;
        }
        return parent == null ? null : SwingUtilities.getWindowAncestor(parent);
    }
}
