package com.kgm.service;

import com.kgm.config.EnvironmentConfig;

public class AuthService {
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "1234";

    public static boolean login(String username, String password) {
        return configuredUsername().equals(username) && configuredPassword().equals(password);
    }

    private static String configuredUsername() {
        return EnvironmentConfig.setting("kgm.login.username", "KGM_LOGIN_USERNAME", DEFAULT_USERNAME);
    }

    private static String configuredPassword() {
        return EnvironmentConfig.setting("kgm.login.password", "KGM_LOGIN_PASSWORD", DEFAULT_PASSWORD);
    }
}
