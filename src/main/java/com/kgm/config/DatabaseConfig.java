package com.kgm.config;

public final class DatabaseConfig {
    private static final String URL_OPTIONS = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private DatabaseConfig() {
    }

    public static String host() {
        return EnvironmentConfig.setting("kgm.db.host", "KGM_DB_HOST", "127.0.0.1");
    }

    public static String port() {
        return EnvironmentConfig.setting("kgm.db.port", "KGM_DB_PORT", "3306");
    }

    public static String databaseName() {
        return EnvironmentConfig.setting("kgm.db.name", "KGM_DB_NAME", "kgm_guest");
    }

    public static String username() {
        return EnvironmentConfig.setting("kgm.db.user", "KGM_DB_USER", "root");
    }

    public static String password() {
        return EnvironmentConfig.setting("kgm.db.password", "KGM_DB_PASSWORD", "");
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
}
