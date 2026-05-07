package com.kgm;
import com.kgm.ui.LoginView;
import javax.swing.SwingUtilities;
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginView().setVisible(true);
            System.out.println("App started");
        });
    }
}
