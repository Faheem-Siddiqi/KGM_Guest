package com.kgm.dao;

import com.kgm.config.DatabaseConnection;
import com.kgm.model.Guest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GuestDao {
    private final AccommodationDao accommodationDao = new AccommodationDao();

    public Guest save(Guest guest) throws SQLException {
        if (hasOverlappingStayByCnic(guest.getCnic(), guest.getArrivalAt(), guest.getDepartureAt())) {
            throw new SQLException("This CNIC already has an overlapping guest stay. A guest cannot be assigned to two rooms, accommodations, or categories at the same time.");
        }

        long guestCategoryId = findOrCreateGuestCategory(guest.getGuestCategory());
        Long accommodationId = accommodationDao.findReadyIdByCategoryAndName(
                guest.getAccommodation(),
                guest.getRoomName()
        );
        if (accommodationId == null) {
            throw new SQLException("Selected room is not ready for assignment.");
        }

        String sql = """
                INSERT INTO guests (
                    guest_name,
                    cnic,
                    nationality,
                    guest_category_id,
                    address,
                    requested_by,
                    requested_department,
                    approved_by,
                    accommodated_by,
                    arrival_at,
                    departure_at,
                    accommodation_category,
                    accommodation_id,
                    room_name,
                    remarks,
                    review
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, guest.getGuestName());
                statement.setString(2, guest.getCnic());
                statement.setString(3, guest.getNationality());
                statement.setLong(4, guestCategoryId);
                statement.setString(5, guest.getAddress());
                statement.setString(6, guest.getRequestedBy());
                statement.setString(7, guest.getRequestedDepartment());
                statement.setString(8, guest.getApprovedBy());
                statement.setString(9, guest.getAccommodatedBy());
                statement.setTimestamp(10, timestamp(guest.getArrivalAt()));
                statement.setTimestamp(11, timestamp(guest.getDepartureAt()));
                statement.setString(12, guest.getAccommodation());
                statement.setLong(13, accommodationId);
                statement.setString(14, guest.getRoomName());
                statement.setString(15, guest.getRemarks());
                statement.setString(16, guest.getReview() == null ? guest.getRemarks() : guest.getReview());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        guest.setId(keys.getLong(1));
                    }
                }

                reserveAccommodation(connection, accommodationId);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
        return guest;
    }

    public List<Guest> findAll() throws SQLException {
        String sql = """
                SELECT
                    g.id,
                    g.guest_name,
                    g.cnic,
                    g.nationality,
                    gc.name AS guest_category,
                    g.address,
                    g.requested_by,
                    g.requested_department,
                    g.approved_by,
                    g.accommodated_by,
                    g.arrival_at,
                    g.departure_at,
                    g.accommodation_category,
                    g.room_name,
                    g.remarks,
                    g.review
                FROM guests g
                JOIN guest_categories gc ON gc.id = g.guest_category_id
                ORDER BY g.arrival_at DESC, g.id DESC
                """;
        List<Guest> guests = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                guests.add(mapGuest(resultSet));
            }
        }
        return guests;
    }

    public List<Guest> findByArrivalRange(Date startInclusive, Date endExclusive) throws SQLException {
        String sql = """
                SELECT
                    g.id,
                    g.guest_name,
                    g.cnic,
                    g.nationality,
                    gc.name AS guest_category,
                    g.address,
                    g.requested_by,
                    g.requested_department,
                    g.approved_by,
                    g.accommodated_by,
                    g.arrival_at,
                    g.departure_at,
                    g.accommodation_category,
                    g.room_name,
                    g.remarks,
                    g.review
                FROM guests g
                JOIN guest_categories gc ON gc.id = g.guest_category_id
                WHERE g.arrival_at >= ?
                  AND g.arrival_at < ?
                ORDER BY g.arrival_at DESC, g.id DESC
                """;
        List<Guest> guests = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, timestamp(startInclusive));
            statement.setTimestamp(2, timestamp(endExclusive));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    guests.add(mapGuest(resultSet));
                }
            }
        }
        return guests;
    }

    public boolean existsByNameOnArrivalDate(String guestName, Date arrivalAt) throws SQLException {
        String sql = """
                SELECT 1
                FROM guests
                WHERE LOWER(TRIM(guest_name)) = LOWER(TRIM(?))
                  AND DATE(arrival_at) = DATE(?)
                LIMIT 1
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guestName);
            statement.setTimestamp(2, timestamp(arrivalAt));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public List<String> findActiveGuestCategoryNames() throws SQLException {
        String sql = """
                SELECT name
                FROM guest_categories
                WHERE active = TRUE
                ORDER BY name
                """;
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

    public boolean hasOverlappingStayByCnic(String cnic, Date arrivalAt, Date departureAt) throws SQLException {
        String normalizedCnic = cnic == null ? "" : cnic.trim();
        if (normalizedCnic.isEmpty() || arrivalAt == null || departureAt == null) {
            return false;
        }

        String sql = """
                SELECT 1
                FROM guests
                WHERE TRIM(cnic) = ?
                  AND arrival_at < ?
                  AND departure_at > ?
                LIMIT 1
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedCnic);
            statement.setTimestamp(2, timestamp(departureAt));
            statement.setTimestamp(3, timestamp(arrivalAt));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public void updateDepartureAndRemarks(long guestId, Date departureAt, String remarks) throws SQLException {
        String sql = """
                UPDATE guests
                SET departure_at = ?, remarks = ?
                WHERE id = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, timestamp(departureAt));
            statement.setString(2, remarks);
            statement.setLong(3, guestId);
            statement.executeUpdate();
        }
    }

    private Guest mapGuest(ResultSet resultSet) throws SQLException {
        Guest guest = new Guest();
        guest.setId(resultSet.getLong("id"));
        guest.setGuestName(resultSet.getString("guest_name"));
        guest.setCnic(resultSet.getString("cnic"));
        guest.setNationality(resultSet.getString("nationality"));
        guest.setGuestCategory(resultSet.getString("guest_category"));
        guest.setAddress(resultSet.getString("address"));
        guest.setRequestedBy(resultSet.getString("requested_by"));
        guest.setRequestedDepartment(resultSet.getString("requested_department"));
        guest.setApprovedBy(resultSet.getString("approved_by"));
        guest.setAccommodatedBy(resultSet.getString("accommodated_by"));
        guest.setArrivalAt(resultSet.getTimestamp("arrival_at"));
        guest.setDepartureAt(resultSet.getTimestamp("departure_at"));
        guest.setAccommodation(resultSet.getString("accommodation_category"));
        guest.setRoomName(resultSet.getString("room_name"));
        guest.setRemarks(resultSet.getString("remarks"));
        guest.setReview(resultSet.getString("review"));
        return guest;
    }

    private long findOrCreateGuestCategory(String name) throws SQLException {
        String normalized = name == null ? "" : name.trim();
        Long existingId = findGuestCategoryId(normalized);
        if (existingId != null) {
            return existingId;
        }

        String sql = "INSERT INTO guest_categories (name, active) VALUES (?, TRUE)";
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
        throw new SQLException("Could not create guest category.");
    }

    private Long findGuestCategoryId(String name) throws SQLException {
        String sql = "SELECT id FROM guest_categories WHERE name = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong("id") : null;
            }
        }
    }

    private void reserveAccommodation(Connection connection, long accommodationId) throws SQLException {
        String sql = """
                UPDATE accommodations
                SET status = 'Reserved'
                WHERE id = ?
                  AND status = 'Ready for Assignment'
                  AND active = TRUE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accommodationId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Selected room is no longer ready for assignment.");
            }
        }
    }

    private Timestamp timestamp(java.util.Date date) {
        return new Timestamp(date.getTime());
    }
}
