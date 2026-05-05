package com.kgm.ui.panel;

import javax.swing.*;
import java.awt.*;

public class ExcelImportButton extends JButton {

    private final Color excelGreen = new Color(107, 190, 120);
    private final Color hoverGreen = new Color(85, 170, 105);

    public ExcelImportButton(Runnable onClick) {
        super("Import Excel");

        // ================= SAP STYLE =================
        setFont(new Font("Segoe UI", Font.PLAIN, 12));
        setFocusPainted(false);
        setForeground(Color.WHITE);
        setBackground(excelGreen);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 🔥 MATCH EXACT SIZE WITH "ADD RECORD"
        setPreferredSize(new Dimension(120, 32));

        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // ================= ACTION =================
        addActionListener(e -> {
            if (onClick != null) {
                onClick.run();
            }
        });

        // ================= HOVER EFFECT =================
        addMouseListener(new java.awt.event.MouseAdapter() {

            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                setBackground(hoverGreen);
                repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                setBackground(excelGreen);
                repaint();
            }
        });
    }

    // ================= SAP ROUND STYLE =================
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(getBackground());
 
        // rounded SAP-style button border radius
        // g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

        super.paintComponent(g);
        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        // No border (clean SAP Fiori look)
    }

    // ================= OPTIONAL WRAPPER (TOP RIGHT) =================
    public static JPanel wrapTopRight(JButton button) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setOpaque(false);
        panel.add(button);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        return panel;
    }
}