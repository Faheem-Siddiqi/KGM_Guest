package com.kgm.ui.styling;

import com.kgm.ui.dialog.UniversalDialog;

import java.awt.Component;

public final class DialogHelper {
    private DialogHelper() {
    }

    public static void success(Component parent, String message) {
        UniversalDialog.message(parent, UniversalDialog.Type.SUCCESS, "Success", message);
    }

    public static int successOption(
            Component parent,
            String title,
            String message,
            String primaryOption,
            String secondaryOption
    ) {
        return UniversalDialog.option(
                parent,
                UniversalDialog.Type.SUCCESS,
                title,
                message,
                primaryOption,
                secondaryOption
        );
    }

    public static void info(Component parent, String title, String message) {
        UniversalDialog.message(parent, UniversalDialog.Type.INFO, title, message);
    }

    public static void warning(Component parent, String title, String message) {
        UniversalDialog.message(parent, UniversalDialog.Type.WARNING, title, message);
    }

    public static void warningSections(Component parent, String title, String... sections) {
        UniversalDialog.message(
                parent,
                UniversalDialog.Type.WARNING,
                title,
                String.join(UniversalDialog.SECTION_SEPARATOR, sections)
        );
    }

    public static void error(Component parent, String title, String message) {
        UniversalDialog.message(parent, UniversalDialog.Type.ERROR, title, message);
    }

    public static void errorSections(Component parent, String title, String... sections) {
        UniversalDialog.message(
                parent,
                UniversalDialog.Type.ERROR,
                title,
                String.join(UniversalDialog.SECTION_SEPARATOR, sections)
        );
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
