package com.kgm.ui.styling;

import javax.swing.JOptionPane;
import java.awt.Component;

public final class DialogHelper {
    private DialogHelper() {
    }

    public static void success(Component parent, String message) {
        show(parent, "Success", message, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void info(Component parent, String title, String message) {
        show(parent, title, message, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void warning(Component parent, String title, String message) {
        show(parent, title, message, JOptionPane.WARNING_MESSAGE);
    }

    public static void error(Component parent, String title, String message) {
        show(parent, title, message, JOptionPane.ERROR_MESSAGE);
    }

    private static void show(Component parent, String title, String message, int type) {
        JOptionPane.showMessageDialog(parent, message, title, type);
    }
}
