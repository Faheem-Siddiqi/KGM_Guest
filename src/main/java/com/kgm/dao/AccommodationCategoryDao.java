package com.kgm.dao;

import com.kgm.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AccommodationCategoryDao {
    public List<String> findActiveNames() throws SQLException {
        String sql = "SELECT name FROM accommodation_categories WHERE active = TRUE ORDER BY name";
        List<String> categories = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                categories.add(resultSet.getString("name"));
            }
        }
        return categories;
    }

    public long findOrCreate(String name) throws SQLException {
        String normalized = normalize(name);
        Long existingId = findIdByName(normalized);
        if (existingId != null) {
            activate(existingId);
            return existingId;
        }

        String sql = "INSERT INTO accommodation_categories (name, active) VALUES (?, TRUE)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, normalized);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Could not create accommodation category.");
    }

    public void save(String name) throws SQLException {
        findOrCreate(name);
    }

    public void updateName(String oldName, String newName) throws SQLException {
        String sql = "UPDATE accommodation_categories SET name = ?, active = TRUE WHERE name = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalize(newName));
            statement.setString(2, normalize(oldName));
            if (statement.executeUpdate() == 0) {
                save(newName);
            }
        }
    }

    public void deleteByName(String name) throws SQLException {
        String sql = "UPDATE accommodation_categories SET active = FALSE WHERE name = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalize(name));
            statement.executeUpdate();
        }
    }

    private Long findIdByName(String name) throws SQLException {
        String sql = "SELECT id FROM accommodation_categories WHERE name = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong("id") : null;
            }
        }
    }

    private void activate(long id) throws SQLException {
        String sql = "UPDATE accommodation_categories SET active = TRUE WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
