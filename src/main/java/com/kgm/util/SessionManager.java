package com.kgm.util;
import com.kgm.model.UserSession;
public class SessionManager {
    private static UserSession session;
    // SESSION TIME 30 MINS
    private static final long EXPIRY = 30 * 60 * 1000;
    // private static final long EXPIRY = 5 * 1000; // 5 seconds for testing
    public static void startSession(String user) {
        session = new UserSession();
        session.username = user;
        session.loginTime = System.currentTimeMillis();
    }
    public static boolean isValid() {
        return session != null &&
                (System.currentTimeMillis() - session.loginTime) < EXPIRY;
    }
    public static String getUser() {
        return session != null ? session.username : null;
    }
    public static void clear() {
        session = null;
    }
}