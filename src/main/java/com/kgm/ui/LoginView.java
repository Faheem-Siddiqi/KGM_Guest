package com.kgm.ui;

import com.kgm.service.AuthService;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.LoginViewHelper;
import com.kgm.util.SessionManager;
import com.kgm.util.SessionWatcher;

import javax.swing.*;
import java.awt.*;

public class LoginView extends JFrame {

    public LoginView() {
        LoginViewHelper.applyFrame(this);

        JPanel root = LoginViewHelper.createRootPanel();
        add(root, BorderLayout.CENTER);

        root.add(LoginViewHelper.createImagePanel());
        root.add(createLoginPanel());

        setLocationRelativeTo(null);
    }

    private JPanel createLoginPanel() {
        JPanel outer = LoginViewHelper.createOuterPanel();
        JPanel form = LoginViewHelper.createFormPanel();

        JLabel eyebrow = LoginViewHelper.createEyebrowLabel("KGM Guest Management 1.0.2");
        JLabel welcome = LoginViewHelper.createWelcomeLabel("Welcome Back");
        JLabel subtitle = LoginViewHelper.createSubtitleLabel("Sign in to continue.");

        JTextField userField = LoginViewHelper.createTextField("Enter username");
        JPasswordField passField = LoginViewHelper.createPasswordField("Enter password");
        JButton loginBtn = LoginViewHelper.createPrimaryButton("Sign In");

        form.add(eyebrow);
        form.add(Box.createVerticalStrut(14));
        form.add(welcome);
        form.add(Box.createVerticalStrut(4));
        form.add(subtitle);
        form.add(Box.createVerticalStrut(30));
        form.add(LoginViewHelper.createFieldBlock("Username", userField));
        form.add(Box.createVerticalStrut(14));
        form.add(LoginViewHelper.createFieldBlock("Password", passField));
        form.add(Box.createVerticalStrut(20));
        form.add(loginBtn);

        outer.add(form, new GridBagConstraints());
        getRootPane().setDefaultButton(loginBtn);

        loginBtn.addActionListener(e -> {
            String user = userField.getText();
            String pass = new String(passField.getPassword());
            if (AuthService.login(user, pass)) {
                loginBtn.setEnabled(false);
                SessionManager.startSession(user);
                SessionWatcher.start();
                SessionWatcher.closeAllWindows();
                new HomeView();
            } else {
                DialogHelper.error(this, "Login Failed", "Invalid username or password.");
            }
        });

        return outer;
    }
}
