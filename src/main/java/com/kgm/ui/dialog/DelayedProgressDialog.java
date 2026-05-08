package com.kgm.ui.dialog;

import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;

public class DelayedProgressDialog extends JDialog {
    private static final int DEFAULT_DELAY_MS = 900;

    private DelayedProgressDialog(Window owner, String title, String message) {
        super(owner, titleText(title), ModalityType.MODELESS);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setContentPane(content(title, message));
        setSize(460, 176);
        setLocationRelativeTo(owner);
    }

    public static Handle showAfter(Component parent, String title, String message) {
        return showAfter(parent, title, message, DEFAULT_DELAY_MS);
    }

    public static Handle showAfter(Component parent, String title, String message, int delayMs) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        return new Handle(new DelayedProgressDialog(owner, title, message), Math.max(0, delayMs));
    }

    private JPanel content(String title, String message) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HomeViewHelper.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JLabel titleLabel = new JLabel(titleText(title));
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        header.add(titleLabel, BorderLayout.WEST);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        JLabel state = new JLabel(messageText(message));
        state.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        state.setForeground(HomeViewHelper.TEXT_SECONDARY);
        state.setAlignmentX(Component.LEFT_ALIGNMENT);

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);
        progress.setPreferredSize(new Dimension(400, 16));

        body.add(state);
        body.add(Box.createVerticalStrut(14));
        body.add(progress);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        return root;
    }

    private static String titleText(String title) {
        return title == null || title.isBlank() ? "Working" : title.trim();
    }

    private static String messageText(String message) {
        return message == null || message.isBlank()
                ? "This is taking longer than usual. Please wait..."
                : message.trim();
    }

    public static final class Handle {
        private final DelayedProgressDialog dialog;
        private final Timer timer;
        private boolean finished;

        private Handle(DelayedProgressDialog dialog, int delayMs) {
            this.dialog = dialog;
            this.timer = new Timer(delayMs, event -> showDialog());
            this.timer.setRepeats(false);
            this.timer.start();
        }

        public void done() {
            if (SwingUtilities.isEventDispatchThread()) {
                finish();
            } else {
                SwingUtilities.invokeLater(this::finish);
            }
        }

        private void showDialog() {
            if (!finished) {
                dialog.setVisible(true);
            }
        }

        private void finish() {
            finished = true;
            timer.stop();
            dialog.dispose();
        }
    }
}
