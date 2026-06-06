package com.kgm;

import com.kgm.config.DatabaseConnection;
import com.kgm.database.DatabaseInitializer;
import com.kgm.ui.DatabaseSetupView;
import com.kgm.ui.LoginView;
import com.kgm.ui.StartupLoadingView;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.util.concurrent.ExecutionException;

public final class StartupController {
    private static final int STARTUP_NOTICE_DELAY_MS = 850;

    private StartupController() {
    }

    public static void start() {
        installLookAndFeel();
        SwingUtilities.invokeLater(StartupController::startDatabaseCheck);
    }

    private static void startDatabaseCheck() {
        StartupLoadingView loadingView = new StartupLoadingView();
        Timer loadingTimer = new Timer(STARTUP_NOTICE_DELAY_MS, event -> loadingView.setVisible(true));
        loadingTimer.setRepeats(false);
        loadingTimer.start();

        new SwingWorker<RuntimeException, Void>() {
            @Override
            protected RuntimeException doInBackground() {
                return initializeDatabaseSafely();
            }

            @Override
            protected void done() {
                DatabaseConnection.setConnectionFailureListener(DatabaseSetupView::showConnectionFailure);
                loadingTimer.stop();
                RuntimeException startupFailure = startupResult();
                loadingView.finish();
                if (startupFailure == null) {
                    showLoginWindow();
                } else {
                    showDatabaseSetupWindow(startupFailure);
                }
            }

            private RuntimeException startupResult() {
                try {
                    return get();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return new IllegalStateException("Database startup was interrupted.", exception);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    if (cause instanceof RuntimeException runtimeException) {
                        return runtimeException;
                    }
                    return new IllegalStateException("Database startup failed.", cause);
                }
            }
        }.execute();
    }

    public static void showLoginWindow() {
        new LoginView().setVisible(true);
        System.out.println("Guest App started");
    }

    public static void initializeDatabase() {
        DatabaseInitializer.init();
    }

    public static void reconnectDatabase() {
        DatabaseInitializer.reconnect();
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
        DatabaseSetupView.showStartupFailure(startupFailure);
        System.out.println("Guest App opened database setup guide");
    }

    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ignored) {
        }
    }
}
