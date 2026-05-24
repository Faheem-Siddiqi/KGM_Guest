package com.kgm.ui.panel;

import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class FooterPanel extends JPanel {
    private static final String FOOTER_TEXT = "Kohinoor Textile Mills Gujar Khan Ltd.";
    private static final Color PRIMARY = HomeViewHelper.PRIMARY;

    public FooterPanel() {
        this(null);
    }

    public FooterPanel(JComponent trailingActions) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(8, 20, 8, 20));

        JLabel label = new JLabel(FOOTER_TEXT, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setForeground(PRIMARY);
        add(label, BorderLayout.CENTER);

        if (trailingActions != null) {
            trailingActions.setOpaque(false);
            add(trailingActions, BorderLayout.EAST);

            JPanel balance = new JPanel();
            balance.setOpaque(false);
            balance.setPreferredSize(trailingActions.getPreferredSize());
            add(balance, BorderLayout.WEST);
        }
    }
}
