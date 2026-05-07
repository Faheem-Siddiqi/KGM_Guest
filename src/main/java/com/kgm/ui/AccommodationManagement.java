package com.kgm.ui;

import com.kgm.database.DatabaseInitializer;
import com.kgm.ui.panel.AccommodationManagementPanel;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.HeaderPanel;

import javax.swing.*;
import java.awt.*;

public class AccommodationManagement extends JFrame {

    public AccommodationManagement() {
        DatabaseInitializer.init();
        setTitle("Accommodation Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(new HeaderPanel("Accommodation Management"), BorderLayout.NORTH);
        root.add(new AccommodationManagementPanel(this::goBack), BorderLayout.CENTER);
        root.add(new FooterPanel(), BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void goBack() {
        new HomeView();
        dispose();
    }

    public static void main(String[] args) {
        DatabaseInitializer.init();
        SwingUtilities.invokeLater(() -> new AccommodationManagement().setVisible(true));
    }
}
