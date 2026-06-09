package com.kgm.ui.component;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

public final class PendingButtonState {
    private final JButton button;
    private final String idleText;
    private final boolean idleEnabled;

    private PendingButtonState(JButton button, String pendingText) {
        this.button = button;
        this.idleText = button.getText();
        this.idleEnabled = button.isEnabled();
        button.setText(pendingText);
        button.setEnabled(false);
    }

    public static PendingButtonState start(JButton button, String pendingText) {
        return new PendingButtonState(button, pendingText);
    }

    public void restore() {
        Runnable restoreButton = () -> {
            button.setText(idleText);
            button.setEnabled(idleEnabled);
        };
        if (SwingUtilities.isEventDispatchThread()) {
            restoreButton.run();
        } else {
            SwingUtilities.invokeLater(restoreButton);
        }
    }
}
