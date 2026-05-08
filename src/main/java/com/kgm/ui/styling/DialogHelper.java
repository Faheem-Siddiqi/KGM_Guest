package com.kgm.ui.styling;

import com.kgm.ui.dialog.UniversalDialog;

import java.awt.Component;

public final class DialogHelper {
    private DialogHelper() {
    }

    public static void success(Component parent, String message) {
        UniversalDialog.message(parent, UniversalDialog.Type.SUCCESS, "Success", message);
    }

    public static void info(Component parent, String title, String message) {
        UniversalDialog.message(parent, UniversalDialog.Type.INFO, title, message);
    }

    public static void warning(Component parent, String title, String message) {
        UniversalDialog.message(parent, UniversalDialog.Type.WARNING, title, message);
    }

    public static void error(Component parent, String title, String message) {
        UniversalDialog.message(parent, UniversalDialog.Type.ERROR, title, message);
    }

    public static int option(
            Component parent,
            String title,
            String message,
            String primaryOption,
            String secondaryOption
    ) {
        return UniversalDialog.option(
                parent,
                UniversalDialog.Type.INFO,
                title,
                message,
                primaryOption,
                secondaryOption
        );
    }
}
