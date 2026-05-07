package com.kgm.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private DatabaseConnection() {
    }

    public static Connection getServerConnection() throws SQLException {
        loadDriver();
        return getConnection(DatabaseConfig.serverUrl());
    }

    public static Connection getConnection() throws SQLException {
        loadDriver();
        return getConnection(DatabaseConfig.databaseUrl());
    }

    private static Connection getConnection(String url) throws SQLException {
        SQLException firstException = null;
        for (String password : candidatePasswords()) {
            try {
                return DriverManager.getConnection(url, DatabaseConfig.username(), password);
            } catch (SQLException exception) {
                if (!isAccessDenied(exception)) {
                    throw exception;
                }
                if (firstException == null) {
                    firstException = exception;
                }
            }
        }

        throw firstException;
    }

    private static String[] candidatePasswords() {
        String password = DatabaseConfig.password();
        if (password.endsWith("`")) {
            return new String[]{password, password.substring(0, password.length() - 1)};
        }
        return new String[]{password, password + "`"};
    }

    private static boolean isAccessDenied(SQLException exception) {
        return exception.getErrorCode() == 1045;
    }

    private static void loadDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("MySQL JDBC driver not found.", exception);
        }
    }
}
