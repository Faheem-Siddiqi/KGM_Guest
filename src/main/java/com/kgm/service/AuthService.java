package com.kgm.service;
public class AuthService {

    public static boolean login(String username, String password) {
        // temporary login (replace later with Excel/DB)
        // ADMIN AUTH: admin / 1234
        return "admin".equals(username) && "1234".equals(password);
    }
}