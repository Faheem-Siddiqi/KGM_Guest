package com.kgm.util;
import com.kgm.ui.LoginView;
import javax.swing.*;
import java.awt.*;
public class SessionWatcher {
    private static Timer timer;
    private static boolean handled = false;
    public static void start() {
        if (timer != null && timer.isRunning())
            return;
        handled = false;
        timer = new Timer(1000, e -> {
            if (!SessionManager.isValid() && !handled) {
                handled = true;
                timer.stop();
                JOptionPane.showMessageDialog(null, "Session expired");
                SessionManager.clear();
                closeAllWindows();
                new LoginView().setVisible(true);
            }
        });
        timer.start();
    }
    public static void stop() {
        if (timer != null) {
            timer.stop();
        }
    }
    public static void closeAllWindows() {
        for (Window window : Window.getWindows()) {
            if (window.isDisplayable()) {
                window.dispose();
            }
        }
    }
}