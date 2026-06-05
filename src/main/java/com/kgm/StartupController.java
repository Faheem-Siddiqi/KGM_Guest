package com.kgm;

import com.kgm.database.DatabaseInitializer;
import com.kgm.ui.DatabaseSetupView;
import com.kgm.ui.LoginView;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class StartupController {
    private StartupController() {
    }

    public static void start() {
        installLookAndFeel();
        RuntimeException startupFailure = initializeDatabaseSafely();
        SwingUtilities.invokeLater(() -> {
            if (startupFailure == null) {
                showLoginWindow();
            } else {
                showDatabaseSetupWindow(startupFailure);
            }
        });
    }

    public static void showLoginWindow() {
        new LoginView().setVisible(true);
        System.out.println("Guest App started");
    }

    public static void initializeDatabase() {
        DatabaseInitializer.init();
    }

    private static RuntimeException initializeDatabaseSafely() {
        try {
            initializeDatabase();
            return null;
        } catch (RuntimeException exception) {
            return exception;
        }
    }

    private static void showDatabaseSetupWindow(RuntimeException startupFailure) {
        new DatabaseSetupView(startupFailure).setVisible(true);
        System.out.println("Guest App opened database setup guide");
    }

    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ignored) {
        }
    }
}
