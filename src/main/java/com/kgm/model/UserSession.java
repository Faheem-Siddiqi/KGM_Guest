package com.kgm.model;

public final class UserSession {
    private final String username;
    private final long loginTime;
    private final long expiresAt;

    public UserSession(String username, long loginTime, long expiryMillis) {
        this.username = username;
        this.loginTime = loginTime;
        this.expiresAt = loginTime + expiryMillis;
    }

    public String getUsername() {
        return username;
    }

    public long getLoginTime() {
        return loginTime;
    }

    public boolean isValid(long now) {
        return now < expiresAt;
    }

    public long remainingMillis(long now) {
        return Math.max(0, expiresAt - now);
    }
}
