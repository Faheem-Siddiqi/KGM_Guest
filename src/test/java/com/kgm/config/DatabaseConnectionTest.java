package com.kgm.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

class DatabaseConnectionTest {

    @Test
    void shouldConnectToMySqlDatabase() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            assertNotNull(connection);
            assertFalse(connection.isClosed());
            assertTrue(connection.isValid(5));
        } catch (SQLException | IllegalStateException e) {
            fail("Database connection failed: " + e.getMessage());
        }
    }
}
