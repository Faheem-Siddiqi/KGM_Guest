package com.kgm.dao;

import com.kgm.config.DatabaseConnection;
import com.kgm.database.DatabaseInitializer;
import com.kgm.ui.panel.AccommodationRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AccommodationDao {
    private static final String ROOM_PREFIX = "Room-";

    private final AccommodationCategoryDao categoryDao = new AccommodationCategoryDao();

    public List<AccommodationRecord> findAll() throws SQLException {
        String sql = """
                SELECT
                    a.id,
                    a.name,
                    a.capacity,
                    GREATEST(a.capacity - COALESCE(occupied.current_guests, 0), 0) AS available_seats,
                    a.status,
                    a.assigned_staff,
                    c.name AS category_name,
                    GROUP_CONCAT(aa.amenity ORDER BY aa.amenity SEPARATOR '\\n') AS amenities
                FROM accommodations a
                JOIN accommodation_categories c ON c.id = a.category_id
                LEFT JOIN (
                    SELECT accommodation_id, COUNT(*) AS current_guests
                    FROM guests
                    WHERE arrival_at <= NOW()
                      AND departure_at >= NOW()
                    GROUP BY accommodation_id
                ) occupied ON occupied.accommodation_id = a.id
                LEFT JOIN accommodation_amenities aa ON aa.accommodation_id = a.id
                WHERE a.active = TRUE
                GROUP BY a.id, a.name, a.capacity, a.status, a.assigned_staff, c.name, occupied.current_guests
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
                        resultSet.getInt("available_seats"),
                        resultSet.getString("status"),
                        resultSet.getString("assigned_staff"),
                        amenitiesFrom(resultSet.getString("amenities"))
                ));
            }
        }
        return accommodations;
    }

    public List<String> findActiveNames() throws SQLException {
        String sql = """
                SELECT name
                FROM accommodations
                WHERE active = TRUE
                ORDER BY name
                """;
        List<String> names = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                names.add(resultSet.getString("name"));
            }
        }
        return names;
    }

    public List<String> findActiveNamesByCategory(String category) throws SQLException {
        String sql = """
                SELECT a.name
                FROM accommodations a
                JOIN accommodation_categories c ON c.id = a.category_id
                WHERE a.active = TRUE AND c.name = ?
                ORDER BY a.name
                """;
        List<String> names = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    names.add(resultSet.getString("name"));
                }
            }
        }
        return names;
    }

    public List<String> findReadyNamesByCategory(String category) throws SQLException {
        String sql = """
                SELECT a.name
                FROM accommodations a
                JOIN accommodation_categories c ON c.id = a.category_id
                WHERE a.active = TRUE
                  AND a.status = 'Ready for Assignment'
                  AND c.name = ?
                ORDER BY a.name
                """;
        List<String> names = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    names.add(resultSet.getString("name"));
                }
            }
        }
        return names;
    }

    public Long findIdByName(String name) throws SQLException {
        String sql = "SELECT id FROM accommodations WHERE name = ? AND active = TRUE";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong("id") : null;
            }
        }
    }

    public Long findIdByCategoryAndName(String category, String name) throws SQLException {
        String sql = """
                SELECT a.id
                FROM accommodations a
                JOIN accommodation_categories c ON c.id = a.category_id
                WHERE c.name = ? AND a.name = ? AND a.active = TRUE
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category);
            statement.setString(2, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong("id") : null;
            }
        }
    }

    public Long findReadyIdByCategoryAndName(String category, String name) throws SQLException {
        String sql = """
                SELECT a.id
                FROM accommodations a
                JOIN accommodation_categories c ON c.id = a.category_id
                WHERE c.name = ?
                  AND a.name = ?
                  AND a.status = 'Ready for Assignment'
                  AND a.active = TRUE
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category);
            statement.setString(2, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong("id") : null;
            }
        }
    }

    public AccommodationRecord save(AccommodationRecord accommodation) throws SQLException {
        accommodation.setName(requiredRoomName(accommodation.getName()));
        DatabaseInitializer.ensureAccommodationNameCanRepeatAcrossCategories();
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
        refreshAvailableSeats(accommodation);
        return accommodation;
    }

    public AccommodationRecord update(AccommodationRecord accommodation) throws SQLException {
        if (accommodation.getId() <= 0) {
            return save(accommodation);
        }

        accommodation.setName(requiredRoomName(accommodation.getName()));
        DatabaseInitializer.ensureAccommodationNameCanRepeatAcrossCategories();
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
        refreshAvailableSeats(accommodation);
        return accommodation;
    }

    private void refreshAvailableSeats(AccommodationRecord accommodation) throws SQLException {
        if (accommodation.getId() <= 0) {
            accommodation.setAvailableSeats(Math.max(accommodation.getCapacity(), 0));
            return;
        }

        String sql = """
                SELECT GREATEST(a.capacity - COUNT(g.id), 0) AS available_seats
                FROM accommodations a
                LEFT JOIN guests g
                    ON g.accommodation_id = a.id
                   AND g.arrival_at <= NOW()
                   AND g.departure_at >= NOW()
                WHERE a.id = ?
                GROUP BY a.id, a.capacity
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accommodation.getId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    accommodation.setAvailableSeats(resultSet.getInt("available_seats"));
                    return;
                }
            }
        }
        accommodation.setAvailableSeats(Math.max(accommodation.getCapacity(), 0));
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

    private List<String> amenitiesFrom(String value) {
        List<String> amenities = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return amenities;
        }
        for (String amenity : value.split("\\R")) {
            String text = amenity.trim();
            if (!text.isEmpty()) {
                amenities.add(text);
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

    private String requiredRoomName(String value) throws SQLException {
        String name = roomNameValue(value);
        if (ROOM_PREFIX.equals(name)) {
            throw new SQLException("Accommodation name must start with Room- and include a value.");
        }
        return name;
    }

    private String roomNameValue(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return ROOM_PREFIX;
        }
        if (text.regionMatches(true, 0, ROOM_PREFIX, 0, ROOM_PREFIX.length())) {
            return ROOM_PREFIX + text.substring(ROOM_PREFIX.length()).trim();
        }
        if (text.equalsIgnoreCase("Room")) {
            return ROOM_PREFIX;
        }
        if (text.toLowerCase().startsWith("room ")) {
            return ROOM_PREFIX + text.substring("room ".length()).trim();
        }
        return ROOM_PREFIX + text;
    }
}
