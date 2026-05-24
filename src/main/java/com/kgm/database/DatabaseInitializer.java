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
    private static final String SPECIAL_ACCOMMODATION_NAME = "Rear Wing";
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
            ensureSchemaMigrations();
            ensurePerformanceIndexes();
            removeUnusedSeedData();
            ensureAccommodationRoomPrefixCheck();
            initialized = true;
            System.out.println(created
                    ? "DB is connected :: DB created :: " + connectionInfo()
                    : "DB is connected :: DB connected :: " + connectionInfo());
        } catch (SQLException | IOException exception) {
            System.err.println("DB connection error :: " + connectionInfo() + " :: " + exception.getMessage());
            throw new IllegalStateException("Database setup failed: " + exception.getMessage(), exception);
        }
    }

    public static synchronized void ensureAccommodationNameCanRepeatAcrossCategories() throws SQLException {
        ensureAccommodationCategoryNameUniqueKey();
        ensureAccommodationRoomPrefixCheck();
    }

    private static String connectionInfo() {
        return DatabaseConfig.username()
                + "@"
                + DatabaseConfig.host()
                + ":"
                + DatabaseConfig.port()
                + "/"
                + DatabaseConfig.databaseName();
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

    private static void ensureSchemaMigrations() throws SQLException {
        ensureColumn(
                "guests",
                "accommodation_category",
                "ALTER TABLE guests ADD COLUMN accommodation_category VARCHAR(120) NOT NULL DEFAULT '' AFTER departure_at"
        );
        ensureColumn(
                "guests",
                "company_name",
                "ALTER TABLE guests ADD COLUMN company_name VARCHAR(150) NOT NULL DEFAULT '' AFTER guest_category_id"
        );
        ensureColumn(
                "guests",
                "visit_type",
                "ALTER TABLE guests ADD COLUMN visit_type VARCHAR(40) NOT NULL DEFAULT 'Official Visit' AFTER company_name"
        );
        ensureAccommodationCategoryNameUniqueKey();
    }

    private static void ensurePerformanceIndexes() throws SQLException {
        ensureIndex("guest_categories", "idx_guest_categories_active_name", "active, name");
        ensureIndex("accommodation_categories", "idx_accommodation_categories_active_name", "active, name");
        ensureIndex("accommodations", "idx_accommodations_active_category_name", "active, category_id, name");
        ensureIndex("accommodations", "idx_accommodations_active_status_category_name", "active, status, category_id, name");
        ensureIndex("guests", "idx_guests_arrival_id", "arrival_at, id");
        ensureIndex("guests", "idx_guests_name_arrival", "guest_name, arrival_at");
        ensureIndex("guests", "idx_guests_cnic_arrival_departure", "cnic, arrival_at, departure_at");
        ensureIndex("guests", "idx_guests_accommodation_stay", "accommodation_id, arrival_at, departure_at");
        ensureIndex("guests", "idx_guests_department", "requested_department");
    }

    private static void ensureIndex(String tableName, String indexName, String columnsSql) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS index_count
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = ?
                  AND TABLE_NAME = ?
                  AND INDEX_NAME = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DatabaseConfig.databaseName());
            statement.setString(2, tableName);
            statement.setString(3, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && resultSet.getInt("index_count") > 0) {
                    return;
                }
            }

            try (Statement createStatement = connection.createStatement()) {
                createStatement.executeUpdate(
                        "CREATE INDEX " + quoteIdentifier(indexName)
                                + " ON " + quoteIdentifier(tableName)
                                + " (" + columnsSql + ")"
                );
            }
        }
    }

    private static void ensureColumn(String tableName, String columnName, String alterSql) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS column_count
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = ?
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DatabaseConfig.databaseName());
            statement.setString(2, tableName);
            statement.setString(3, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && resultSet.getInt("column_count") > 0) {
                    return;
                }
            }

            try (Statement alterStatement = connection.createStatement()) {
                alterStatement.executeUpdate(alterSql);
            }
        }
    }

    private static void ensureAccommodationCategoryNameUniqueKey() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            for (String indexName : accommodationNameOnlyUniqueIndexes(connection)) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE accommodations DROP INDEX " + quoteIdentifier(indexName));
                }
            }

            if (!hasAccommodationCategoryNameUniqueIndex(connection)) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("""
                            ALTER TABLE accommodations
                            ADD CONSTRAINT uq_accommodations_category_name UNIQUE (category_id, name)
                            """);
                }
            }
        }
    }

    private static List<String> accommodationNameOnlyUniqueIndexes(Connection connection) throws SQLException {
        String sql = """
                SELECT INDEX_NAME
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = ?
                  AND TABLE_NAME = 'accommodations'
                  AND NON_UNIQUE = 0
                  AND INDEX_NAME <> 'PRIMARY'
                GROUP BY INDEX_NAME
                HAVING COUNT(*) = 1
                   AND SUM(CASE WHEN COLUMN_NAME = 'name' THEN 1 ELSE 0 END) = 1
                """;
        List<String> indexes = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DatabaseConfig.databaseName());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    indexes.add(resultSet.getString("INDEX_NAME"));
                }
            }
        }
        return indexes;
    }

    private static boolean hasAccommodationCategoryNameUniqueIndex(Connection connection) throws SQLException {
        String sql = """
                SELECT INDEX_NAME
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = ?
                  AND TABLE_NAME = 'accommodations'
                  AND NON_UNIQUE = 0
                  AND INDEX_NAME <> 'PRIMARY'
                GROUP BY INDEX_NAME
                HAVING COUNT(*) = 2
                   AND SUM(CASE WHEN SEQ_IN_INDEX = 1 AND COLUMN_NAME = 'category_id' THEN 1 ELSE 0 END) = 1
                   AND SUM(CASE WHEN SEQ_IN_INDEX = 2 AND COLUMN_NAME = 'name' THEN 1 ELSE 0 END) = 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DatabaseConfig.databaseName());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private static void ensureAccommodationRoomPrefixCheck() throws SQLException {
        String constraintName = "chk_accommodations_room_name_prefix";
        try (Connection connection = DatabaseConnection.getConnection()) {
            boolean constraintExists = hasTableConstraint(connection, "accommodations", constraintName);
            if (constraintExists && accommodationRoomPrefixCheckAllowsSpecialName(connection, constraintName)) {
                return;
            }
            if (hasActiveAccommodationWithoutAllowedName(connection)) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                if (constraintExists) {
                    statement.executeUpdate("ALTER TABLE accommodations DROP CHECK " + quoteIdentifier(constraintName));
                }
                statement.executeUpdate("""
                        ALTER TABLE accommodations
                        ADD CONSTRAINT chk_accommodations_room_name_prefix
                        CHECK (active = FALSE OR name LIKE 'Room-%' OR name = 'Rear Wing')
                        """);
            }
        }
    }

    private static boolean hasTableConstraint(
            Connection connection,
            String tableName,
            String constraintName
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS constraint_count
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                WHERE CONSTRAINT_SCHEMA = ?
                  AND TABLE_NAME = ?
                  AND CONSTRAINT_NAME = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DatabaseConfig.databaseName());
            statement.setString(2, tableName);
            statement.setString(3, constraintName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt("constraint_count") > 0;
            }
        }
    }

    private static boolean accommodationRoomPrefixCheckAllowsSpecialName(
            Connection connection,
            String constraintName
    ) throws SQLException {
        String sql = """
                SELECT CHECK_CLAUSE
                FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS
                WHERE CONSTRAINT_SCHEMA = ?
                  AND CONSTRAINT_NAME = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DatabaseConfig.databaseName());
            statement.setString(2, constraintName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        && resultSet.getString("CHECK_CLAUSE") != null
                        && resultSet.getString("CHECK_CLAUSE").toLowerCase()
                        .contains(SPECIAL_ACCOMMODATION_NAME.toLowerCase());
            }
        }
    }

    private static boolean hasActiveAccommodationWithoutAllowedName(Connection connection) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS accommodation_count
                FROM accommodations
                WHERE active = TRUE
                  AND name NOT LIKE 'Room-%'
                  AND LOWER(TRIM(name)) <> LOWER(?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, SPECIAL_ACCOMMODATION_NAME);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt("accommodation_count") > 0;
            }
        }
    }

    private static void removeUnusedSeedData() throws SQLException {
        String seedAccommodationSql = """
                UPDATE accommodations a
                LEFT JOIN guests g ON g.accommodation_id = a.id
                SET a.active = FALSE
                WHERE g.id IS NULL
                  AND a.assigned_staff = 'Admin Office'
                  AND a.name IN ('Room I', 'Room II', 'Room III', 'Room IV', 'Room V', 'Room VI', 'Room VII')
                """;
        String seedCategorySql = """
                UPDATE accommodation_categories c
                LEFT JOIN accommodations a ON a.category_id = c.id AND a.active = TRUE
                SET c.active = FALSE
                WHERE a.id IS NULL
                  AND c.name IN ('Rooms', 'Suites', 'Guest House')
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(seedAccommodationSql);
            statement.executeUpdate(seedCategorySql);
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
