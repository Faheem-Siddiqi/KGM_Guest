package com.kgm.database;

import com.kgm.config.DatabaseConfig;
import com.kgm.config.DatabaseConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseInitializer {
    private static final String SCHEMA_RESOURCE = "/db/schema.sql";
    private static boolean initialized;

    private DatabaseInitializer() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }

        try {
            boolean created = ensureDatabase();
            applySchema();
            initialized = true;
            System.out.println(created ? "DB created" : "DB connected");
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Database setup failed: " + exception.getMessage(), exception);
        }
    }

    private static boolean ensureDatabase() throws SQLException {
        try (Connection connection = DatabaseConnection.getServerConnection()) {
            boolean exists = databaseExists(connection);
            if (!exists) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(
                            "CREATE DATABASE `" + DatabaseConfig.escapedDatabaseName()
                                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
                    );
                }
            }
            return !exists;
        }
    }

    private static boolean databaseExists(Connection connection) throws SQLException {
        String sql = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DatabaseConfig.databaseName());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static void applySchema() throws SQLException, IOException {
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : schemaStatements()) {
                statement.execute(sql);
            }
        }
    }

    private static List<String> schemaStatements() throws IOException {
        String schema = readSchema();
        String[] parts = schema.split(";");
        List<String> statements = new ArrayList<>();
        for (String part : parts) {
            String sql = part.trim();
            if (!sql.isEmpty()) {
                statements.add(sql);
            }
        }
        return statements;
    }

    private static String readSchema() throws IOException {
        InputStream stream = DatabaseInitializer.class.getResourceAsStream(SCHEMA_RESOURCE);
        if (stream == null) {
            throw new IOException("Schema resource not found: " + SCHEMA_RESOURCE);
        }

        StringBuilder schema = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                schema.append(line).append('\n');
            }
        }
        return schema.toString();
    }
}
