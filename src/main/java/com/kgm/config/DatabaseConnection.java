package com.kgm.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;
import java.util.function.Consumer;

public final class DatabaseConnection {
    private static volatile Consumer<Throwable> connectionFailureListener;

    private DatabaseConnection() {
    }

    public static void setConnectionFailureListener(Consumer<Throwable> listener) {
        connectionFailureListener = listener;
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
                    if (isConnectionFailure(exception)) {
                        notifyConnectionFailure(exception);
                        throw new DatabaseConnectionFailure(exception);
                    }
                    throw exception;
                }
                if (firstException == null) {
                    firstException = exception;
                }
            }
        }

        if (firstException != null && isConnectionFailure(firstException)) {
            notifyConnectionFailure(firstException);
            throw new DatabaseConnectionFailure(firstException);
        }

        throw firstException;
    }

    public static boolean isConnectionFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof DatabaseConnectionFailure) {
                return true;
            }
            if (current instanceof SQLException exception) {
                if (exception.getErrorCode() == 1045) {
                    return true;
                }
                String state = exception.getSQLState();
                if (state != null && state.startsWith("08")) {
                    return true;
                }
                String message = exception.getMessage();
                if (message != null) {
                    String lower = message.toLowerCase(Locale.ROOT);
                    if (lower.contains("communications link failure")
                            || lower.contains("driver has not received any packets")
                            || lower.contains("connection refused")
                            || lower.contains("connection timed out")
                            || lower.contains("connect timed out")
                            || lower.contains("socket")
                            || lower.contains("unknown host")
                            || lower.contains("no operations allowed after connection closed")) {
                        return true;
                    }
                }
            }
            current = current.getCause();
        }
        return false;
    }

    public static String userFriendlyConnectionMessage() {
        return DatabaseConnectionFailure.USER_MESSAGE;
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

    private static void notifyConnectionFailure(SQLException exception) {
        Consumer<Throwable> listener = connectionFailureListener;
        if (listener != null) {
            try {
                listener.accept(exception);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static void loadDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("MySQL JDBC driver not found.", exception);
        }
    }
}
