package com.kgm.config;

public final class DatabaseConfig {
    private static final String URL_OPTIONS = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private DatabaseConfig() {
    }

    public static String host() {
        return setting("kgm.db.host", "KGM_DB_HOST", "127.0.0.1");
    }

    public static String port() {
        return setting("kgm.db.port", "KGM_DB_PORT", "3306");
    }

    public static String databaseName() {
        return setting("kgm.db.name", "KGM_DB_NAME", "kgm_guest");
    }

    public static String username() {
        return setting("kgm.db.user", "KGM_DB_USER", "FaheemSIDDIQI");
    }

    public static String password() {
        return setting("kgm.db.password", "KGM_DB_PASSWORD", "FS@12345");
    }

    public static String serverUrl() {
        return "jdbc:mysql://" + host() + ":" + port() + "/" + URL_OPTIONS;
    }

    public static String databaseUrl() {
        return "jdbc:mysql://" + host() + ":" + port() + "/" + databaseName() + URL_OPTIONS;
    }

    public static String escapedDatabaseName() {
        return databaseName().replace("`", "``");
    }

    private static String setting(String propertyName, String envName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        return defaultValue;
    }
}
