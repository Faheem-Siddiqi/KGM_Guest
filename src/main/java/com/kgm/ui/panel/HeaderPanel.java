package com.kgm.ui.panel;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.util.SessionWatcher;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
public class HeaderPanel extends JPanel {
    private static final Color BG = Color.WHITE;
    private static final Color PRIMARY_TEXT = new Color(38, 45, 56);
    private static final Color SECONDARY_TEXT = new Color(110, 110, 110);
    private static final Color LINK_COLOR = new Color(0, 120, 215);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final int HORIZONTAL_INSET = 20;

    public HeaderPanel(String title) {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(12, HORIZONTAL_INSET, 12, HORIZONTAL_INSET));
        add(createLeft(title), BorderLayout.WEST);
        add(createRight(), BorderLayout.EAST);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(BORDER);
        int y = getHeight() - 1;
        g.drawLine(HORIZONTAL_INSET, y, getWidth() - HORIZONTAL_INSET, y);
    }

    // ================= LEFT =================
    private JPanel createLeft(String title) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        panel.setOpaque(false);
        JLabel logo = new JLabel(scaleIcon("images/Header.jpg", 60, 50));
        JLabel company = new JLabel("Kohinoor Textile Mill Ltd. Gujar Khan");
        company.setFont(new Font("Segoe UI", Font.BOLD, 18));
        company.setForeground(PRIMARY_TEXT);
        JLabel screen = new JLabel(title);
        screen.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        screen.setForeground(SECONDARY_TEXT);
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        text.add(company);
        text.add(Box.createVerticalStrut(2));
        text.add(screen);
        panel.add(logo);
        panel.add(text);
        return panel;
    }
    // ================= RIGHT =================
    private JPanel createRight() {
        JPanel container = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        container.setOpaque(false);
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);
        right.add(infoRow("Phone", "0092-051-54955328"));
        right.add(infoRow("Export", "0092-051-5473085"));
        right.add(Box.createVerticalStrut(6));
        right.add(logoutRow());
        container.add(right);
        return container;
    }
    // ================= INFO ROW =================
    private JPanel infoRow(String label, String value) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        p.setOpaque(false);
        JLabel l1 = new JLabel(label + ":");
        l1.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l1.setForeground(SECONDARY_TEXT);
        JLabel l2 = new JLabel(value);
        l2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l2.setForeground(PRIMARY_TEXT);
        p.add(l1);
        p.add(l2);
        return p;
    }
    // ================= LOGOUT =================
    private JPanel logoutRow() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        p.setOpaque(false);
        JLabel logout = new JLabel("Logout");
        logout.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        logout.setForeground(LINK_COLOR);
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                logout();
            }
        });
        p.add(logout);
        return p;
    }
    // ================= IMAGE HELPER =================
    private ImageIcon scaleIcon(String path, int w, int h) {
        ImageIcon icon = new ImageIcon(path);
        Image img = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }
    // ================= LOGOUT LOGIC =================
    private void logout() {
        try {
            SessionWatcher.logoutToLogin();
        } catch (Exception e) {
            DialogHelper.error(SwingUtilities.getWindowAncestor(this), "Error", "Failure - Try Again");
        }
    }
}
