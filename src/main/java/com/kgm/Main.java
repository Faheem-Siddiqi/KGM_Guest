package com.kgm;
import com.kgm.database.DatabaseInitializer;
import com.kgm.ui.LoginView;
import javax.swing.SwingUtilities;
public class Main {
    public static void main(String[] args) {
        DatabaseInitializer.init();
        SwingUtilities.invokeLater(() -> {
            new LoginView().setVisible(true);
            System.out.println("Guest App started");
        });
    }
}
