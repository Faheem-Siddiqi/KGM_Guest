package com.kgm.dao;

import com.kgm.config.DatabaseConnection;
import com.kgm.ui.panel.AccommodationRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AccommodationDao {
    private final AccommodationCategoryDao categoryDao = new AccommodationCategoryDao();

    public List<AccommodationRecord> findAll() throws SQLException {
        String sql = """
                SELECT a.id, a.name, a.capacity, a.status, a.assigned_staff, c.name AS category_name
                FROM accommodations a
                JOIN accommodation_categories c ON c.id = a.category_id
                WHERE a.active = TRUE
                ORDER BY c.name, a.name
                """;
        List<AccommodationRecord> accommodations = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                long id = resultSet.getLong("id");
                accommodations.add(new AccommodationRecord(
                        id,
                        resultSet.getString("name"),
                        resultSet.getString("category_name"),
                        resultSet.getInt("capacity"),
                        resultSet.getString("status"),
                        resultSet.getString("assigned_staff"),
                        findAmenities(connection, id)
                ));
            }
        }
        return accommodations;
    }

    public AccommodationRecord save(AccommodationRecord accommodation) throws SQLException {
        long categoryId = categoryDao.findOrCreate(accommodation.getCategory());
        String sql = """
                INSERT INTO accommodations (category_id, name, capacity, status, assigned_staff, active)
                VALUES (?, ?, ?, ?, ?, TRUE)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, categoryId);
            statement.setString(2, accommodation.getName());
            statement.setInt(3, accommodation.getCapacity());
            statement.setString(4, accommodation.getStatus());
            statement.setString(5, accommodation.getAssignedStaff());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    accommodation.setId(keys.getLong(1));
                }
            }
        }
        saveAmenities(accommodation.getId(), accommodation.getAmenities());
        return accommodation;
    }

    public AccommodationRecord update(AccommodationRecord accommodation) throws SQLException {
        if (accommodation.getId() <= 0) {
            return save(accommodation);
        }

        long categoryId = categoryDao.findOrCreate(accommodation.getCategory());
        String sql = """
                UPDATE accommodations
                SET category_id = ?, name = ?, capacity = ?, status = ?, assigned_staff = ?, active = TRUE
                WHERE id = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, categoryId);
            statement.setString(2, accommodation.getName());
            statement.setInt(3, accommodation.getCapacity());
            statement.setString(4, accommodation.getStatus());
            statement.setString(5, accommodation.getAssignedStaff());
            statement.setLong(6, accommodation.getId());
            statement.executeUpdate();
        }
        saveAmenities(accommodation.getId(), accommodation.getAmenities());
        return accommodation;
    }

    private List<String> findAmenities(Connection connection, long accommodationId) throws SQLException {
        String sql = "SELECT amenity FROM accommodation_amenities WHERE accommodation_id = ? ORDER BY amenity";
        List<String> amenities = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accommodationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    amenities.add(resultSet.getString("amenity"));
                }
            }
        }
        return amenities;
    }

    private void saveAmenities(long accommodationId, List<String> amenities) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                deleteAmenities(connection, accommodationId);
                insertAmenities(connection, accommodationId, amenities);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void deleteAmenities(Connection connection, long accommodationId) throws SQLException {
        String sql = "DELETE FROM accommodation_amenities WHERE accommodation_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accommodationId);
            statement.executeUpdate();
        }
    }

    private void insertAmenities(Connection connection, long accommodationId, List<String> amenities) throws SQLException {
        String sql = "INSERT IGNORE INTO accommodation_amenities (accommodation_id, amenity) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String amenity : amenities) {
                String value = amenity == null ? "" : amenity.trim();
                if (value.isEmpty()) {
                    continue;
                }
                statement.setLong(1, accommodationId);
                statement.setString(2, value);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
