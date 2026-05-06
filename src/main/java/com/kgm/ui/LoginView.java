package com.kgm.ui;
import com.kgm.service.AuthService;
import com.kgm.util.SessionManager;
import com.kgm.util.SessionWatcher;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
public class LoginView extends JFrame {
    // Custom Rounded Border
    static class RoundedBorder extends AbstractBorder {
        private int radius;
        RoundedBorder(int radius) {
            this.radius = radius;
        }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.BLACK);
            g2.draw(new RoundRectangle2D.Float(x, y, width - 1, height - 1, radius, radius));
            g2.dispose();
        }
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius / 2, radius / 2, radius / 2, radius / 2);
        }
        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
    public LoginView() {
        setTitle("Login");
        setSize(420, 360);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        // ================= HEADER =================
        ImageIcon logoIcon = new ImageIcon("images/Logo.jpg");
        Image logoImage = logoIcon.getImage().getScaledInstance(75, 60, Image.SCALE_SMOOTH);
        JLabel logo = new JLabel(new ImageIcon(logoImage));
        JLabel companyName = new JLabel("Koohinoor Textile Mills");
        companyName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JLabel appTitle = new JLabel("Ex Employee Records");
        appTitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(Color.WHITE);
        titlePanel.add(companyName);
        titlePanel.add(appTitle);
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(200, 200, 200)),
                new EmptyBorder(10, 15, 0, 15)));
        header.setBackground(Color.WHITE);
        header.add(logo, BorderLayout.WEST);
        header.add(titlePanel, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);
        // ================= FORM =================
        JPanel form = new JPanel();
        form.setLayout(new GridLayout(5, 1, 10, 10));
        form.setBorder(new EmptyBorder(20, 30, 20, 30));
        form.setBackground(Color.WHITE);
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        styleField(userField);
        styleField(passField);
        JButton loginBtn = new JButton("LOGIN");
        styleButton(loginBtn);
        form.add(new JLabel("Username"));
        form.add(userField);
        form.add(new JLabel("Password"));
        form.add(passField);
        form.add(loginBtn);
        add(form, BorderLayout.CENTER);
        getRootPane().setDefaultButton(loginBtn);
        // ================= ACTION =================
        loginBtn.addActionListener(e -> {
            String user = userField.getText();
            String pass = new String(passField.getPassword());
            if (AuthService.login(user, pass)) {
                SessionManager.startSession(user);
                SessionWatcher.start();
                SessionWatcher.closeAllWindows();
                new HomeView();
//   new AddGuest().setVisible(true);
//   new AccommodationManagement().setVisible(true);

          

            } else {
                JOptionPane.showMessageDialog(this, "Invalid Login");
            }
        });
    }
    // ================= STYLING =================
    private void styleField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        field.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 200, 200)),
                new EmptyBorder(2, 10, 2, 10)));
    }
    private void styleButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setBackground(new Color(52, 152, 219));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBorder(new EmptyBorder(10, 0, 10, 0));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
}
