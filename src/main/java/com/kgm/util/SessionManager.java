package com.kgm.util;
import com.kgm.model.UserSession;

public final class SessionManager {
    private static UserSession session;
    private static final long EXPIRY_MILLIS = 30L * 60L * 1000L;

    private SessionManager() {
    }

    public static synchronized void startSession(String user) {
        session = new UserSession(user, System.currentTimeMillis(), EXPIRY_MILLIS);
    }

    public static synchronized boolean isValid() {
        return session != null && session.isValid(System.currentTimeMillis());
    }

    public static synchronized long getRemainingMillis() {
        return session == null ? 0 : session.remainingMillis(System.currentTimeMillis());
    }

    public static synchronized String getUser() {
        return session != null ? session.getUsername() : null;
    }

    public static synchronized void clear() {
        session = null;
    }
}
