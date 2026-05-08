package com.kgm.util;
import com.kgm.ui.LoginView;
import com.kgm.ui.styling.DialogHelper;
import javax.swing.*;
import java.awt.*;

public final class SessionWatcher {
    private static Timer timer;
    private static boolean handlingTimeout;

    private SessionWatcher() {
    }

    public static void start() {
        runOnEdt(SessionWatcher::scheduleTimeout);
    }

    private static void scheduleTimeout() {
        stopTimer();
        handlingTimeout = false;

        if (!SessionManager.isValid()) {
            return;
        }

        int delay = (int) Math.min(Integer.MAX_VALUE, Math.max(1, SessionManager.getRemainingMillis()));
        timer = new Timer(delay, e -> handleTimeout());
        timer.setRepeats(false);
        timer.start();
    }

    public static void stop() {
        runOnEdt(() -> {
            stopTimer();
            handlingTimeout = false;
        });
    }

    public static void logoutToLogin() {
        runOnEdt(() -> {
            stopTimer();
            handlingTimeout = false;
            SessionManager.clear();
            closeAllWindows();
            new LoginView().setVisible(true);
        });
    }

    private static void handleTimeout() {
        if (handlingTimeout) {
            return;
        }
        if (SessionManager.isValid()) {
            scheduleTimeout();
            return;
        }

        handlingTimeout = true;
        stopTimer();
        SessionManager.clear();

        DialogHelper.warning(activeWindow(), "Session Expired", "Session expired. Please login again.");

        closeAllWindows();
        new LoginView().setVisible(true);
        handlingTimeout = false;
    }

    private static void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    public static void closeAllWindows() {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(SessionWatcher::disposeAllWindows);
            } catch (Exception exception) {
                throw new IllegalStateException("Unable to close application windows", exception);
            }
            return;
        }
        disposeAllWindows();
    }

    private static void disposeAllWindows() {
        for (Window window : Window.getWindows()) {
            if (window.isDisplayable()) {
                window.dispose();
            }
        }
    }

    private static Window activeWindow() {
        for (Window window : Window.getWindows()) {
            if (window.isActive() && window.isDisplayable()) {
                return window;
            }
        }
        for (Window window : Window.getWindows()) {
            if (window.isVisible() && window.isDisplayable()) {
                return window;
            }
        }
        return null;
    }

    private static void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }
}
