package com.kgm.dao;

import com.kgm.config.DatabaseConnection;
import com.kgm.model.Guest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GuestDao {
    private static final String OVERLAPPING_STAY_MESSAGE = "This CNIC already has an overlapping guest stay. A guest cannot be assigned to two rooms, accommodations, or categories at the same time.";
    private static final String ROOM_OVERLAP_MESSAGE = "This room is already booked for overlapping dates. Please select a different room or choose different dates.";
    private static final String NO_SEATS_AVAILABLE_MESSAGE = "No seats available in this room for the selected dates. The room has reached its capacity for this period.";

    private final AccommodationDao accommodationDao = new AccommodationDao();

    public Guest save(Guest guest) throws SQLException {
        validateStayDates(guest.getArrivalAt(), guest.getDepartureAt());
        if (hasOverlappingStayByCnic(guest.getCnic(), guest.getArrivalAt(), guest.getDepartureAt())) {
            throw new SQLException(OVERLAPPING_STAY_MESSAGE);
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
            try {
                // Check for overlapping bookings in the same room
                if (hasOverlappingRoomBooking(connection, accommodationId, guest.getArrivalAt(), guest.getDepartureAt())) {
                    throw new SQLException(ROOM_OVERLAP_MESSAGE);
                }

                // Check seat availability for the requested dates
                if (!hasAvailableSeat(connection, accommodationId, guest.getArrivalAt(), guest.getDepartureAt())) {
                    throw new SQLException(NO_SEATS_AVAILABLE_MESSAGE);
                }

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
                }

                // Only mark room as Reserved for current/past bookings (guest has arrived or should have arrived)
                // For future bookings, keep the room as "Ready for Assignment" so other seats remain available
                handleRoomStatus(connection, accommodationId, guest.getArrivalAt());
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

    public Guest findById(long guestId) throws SQLException {
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
                WHERE g.id = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, guestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapGuest(resultSet);
                }
                throw new SQLException("Guest record was not found.");
            }
        }
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
        if (arrivalAt == null) {
            return false;
        }
        String sql = """
                SELECT 1
                FROM guests
                WHERE guest_name = ?
                  AND arrival_at >= ?
                  AND arrival_at < ?
                LIMIT 1
                """;
        Date startOfDay = startOfDay(arrivalAt);
        Date nextDay = nextDay(arrivalAt);
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guestName == null ? "" : guestName.trim());
            statement.setTimestamp(2, timestamp(startOfDay));
            statement.setTimestamp(3, timestamp(nextDay));
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
        String normalizedCnic = digitsOnly(cnic);
        if (normalizedCnic.isEmpty() || arrivalAt == null || departureAt == null) {
            return false;
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            return hasOverlappingStayByCnic(connection, normalizedCnic, arrivalAt, departureAt, null);
        }
    }

    private boolean hasOverlappingStayByCnic(
            Connection connection,
            String normalizedCnic,
            Date arrivalAt,
            Date departureAt,
            Long excludedGuestId
    ) throws SQLException {
        if (normalizedCnic == null || normalizedCnic.isBlank() || arrivalAt == null || departureAt == null) {
            return false;
        }

        if (hasOverlappingStayByExactCnic(connection, normalizedCnic, arrivalAt, departureAt, excludedGuestId)) {
            return true;
        }
        return hasOverlappingStayByNormalizedCnic(connection, normalizedCnic, arrivalAt, departureAt, excludedGuestId);
    }

    private boolean hasOverlappingStayByExactCnic(
            Connection connection,
            String normalizedCnic,
            Date arrivalAt,
            Date departureAt,
            Long excludedGuestId
    ) throws SQLException {
        String sql = """
                SELECT 1
                FROM guests
                WHERE cnic = ?
                  AND arrival_at < ?
                  AND departure_at > ?
                  %s
                LIMIT 1
                """.formatted(excludedGuestId == null ? "" : "AND id <> ?");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedCnic);
            statement.setTimestamp(2, timestamp(departureAt));
            statement.setTimestamp(3, timestamp(arrivalAt));
            if (excludedGuestId != null) {
                statement.setLong(4, excludedGuestId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean hasOverlappingStayByNormalizedCnic(
            Connection connection,
            String normalizedCnic,
            Date arrivalAt,
            Date departureAt,
            Long excludedGuestId
    ) throws SQLException {
        String sql = """
                SELECT 1
                FROM guests
                WHERE arrival_at < ?
                  AND departure_at > ?
                  AND REPLACE(REPLACE(TRIM(cnic), '-', ''), ' ', '') = ?
                  %s
                LIMIT 1
                """.formatted(excludedGuestId == null ? "" : "AND id <> ?");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, timestamp(departureAt));
            statement.setTimestamp(2, timestamp(arrivalAt));
            statement.setString(3, normalizedCnic);
            if (excludedGuestId != null) {
                statement.setLong(4, excludedGuestId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public void delete(long guestId) throws SQLException {
        String selectSql = """
                SELECT accommodation_id, arrival_at
                FROM guests
                WHERE id = ?
                """;
        String deleteSql = "DELETE FROM guests WHERE id = ?";
        String releaseRoomSql = """
                UPDATE accommodations
                SET status = 'Ready for Assignment'
                WHERE id = ?
                  AND status = 'Reserved'
                  AND NOT EXISTS (
                      SELECT 1 FROM guests g
                      WHERE g.accommodation_id = ?
                        AND g.arrival_at <= NOW()
                        AND g.departure_at >= NOW()
                        AND g.id <> ?
                  )
                """;
        
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // First, get the accommodation_id and arrival_at
                long accommodationId = -1;
                Date arrivalAt = null;
                try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
                    statement.setLong(1, guestId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Guest record was not found.");
                        }
                        accommodationId = resultSet.getLong("accommodation_id");
                        arrivalAt = resultSet.getTimestamp("arrival_at");
                    }
                }
                
                // Check if the guest is upcoming (arrival is in the future)
                if (arrivalAt != null && arrivalAt.after(new Date())) {
                    // Delete the guest record for future booking
                    try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
                        statement.setLong(1, guestId);
                        statement.executeUpdate();
                    }
                    
                    // For future bookings, room was never marked as Reserved, so no need to release
                    connection.commit();
                } else if (arrivalAt != null) {
                    // Current or past booking - delete and potentially release room
                    try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
                        statement.setLong(1, guestId);
                        statement.executeUpdate();
                    }
                    
                    // Release the room if it's Reserved and no other current guests remain
                    if (accommodationId > 0) {
                        try (PreparedStatement statement = connection.prepareStatement(releaseRoomSql)) {
                            statement.setLong(1, accommodationId);
                            statement.setLong(2, accommodationId);
                            statement.setLong(3, guestId);
                            statement.executeUpdate();
                        }
                    }
                    
                    connection.commit();
                } else {
                    throw new SQLException("Cannot cancel this guest booking.");
                }
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void updateDepartureAndRemarks(long guestId, Date departureAt, String remarks) throws SQLException {
        String selectSql = """
                SELECT cnic, arrival_at, accommodation_id
                FROM guests
                WHERE id = ?
                """;
        String updateSql = """
                UPDATE guests
                SET departure_at = ?, remarks = ?
                WHERE id = ?
                """;
        String releaseRoomSql = """
                UPDATE accommodations
                SET status = 'Ready for Assignment'
                WHERE id = ?
                  AND status = 'Reserved'
                  AND NOT EXISTS (
                      SELECT 1 FROM guests g
                      WHERE g.accommodation_id = ?
                        AND g.arrival_at <= NOW()
                        AND g.departure_at >= NOW()
                        AND g.id <> ?
                  )
                """;
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String cnic;
                Date arrivalAt;
                long accommodationId;
                try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
                    statement.setLong(1, guestId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Guest record was not found.");
                        }
                        cnic = resultSet.getString("cnic");
                        arrivalAt = resultSet.getTimestamp("arrival_at");
                        accommodationId = resultSet.getLong("accommodation_id");
                    }
                }

                validateStayDates(arrivalAt, departureAt);
                if (hasOverlappingStayByCnic(connection, digitsOnly(cnic), arrivalAt, departureAt, guestId)) {
                    throw new SQLException(OVERLAPPING_STAY_MESSAGE);
                }

                try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                    statement.setTimestamp(1, timestamp(departureAt));
                    statement.setString(2, remarks);
                    statement.setLong(3, guestId);
                    statement.executeUpdate();
                }

                // If the guest has now departed (departure date is in the past), release the room
                // if no other current guests remain in the same accommodation
                if (departureAt != null && !departureAt.after(new Date())) {
                    if (accommodationId > 0) {
                        try (PreparedStatement statement = connection.prepareStatement(releaseRoomSql)) {
                            statement.setLong(1, accommodationId);
                            statement.setLong(2, accommodationId);
                            statement.setLong(3, guestId);
                            statement.executeUpdate();
                        }
                    }
                }

                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
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

    /**
     * Checks if the same room already has a booking that overlaps with the given dates.
     * This prevents double-booking the same room for overlapping time periods.
     */
    private boolean hasOverlappingRoomBooking(
            Connection connection,
            long accommodationId,
            Date arrivalAt,
            Date departureAt
    ) throws SQLException {
        String sql = """
                SELECT 1
                FROM guests
                WHERE accommodation_id = ?
                  AND arrival_at < ?
                  AND departure_at > ?
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accommodationId);
            statement.setTimestamp(2, timestamp(departureAt));
            statement.setTimestamp(3, timestamp(arrivalAt));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    /**
     * Checks if there is at least one available seat in the room for the given date range.
     * A seat is available if the room capacity has not been reached by overlapping bookings.
     */
    private boolean hasAvailableSeat(
            Connection connection,
            long accommodationId,
            Date arrivalAt,
            Date departureAt
    ) throws SQLException {
        // Get room capacity
        String capacitySql = """
                SELECT capacity
                FROM accommodations
                WHERE id = ? AND active = TRUE
                """;
        int capacity;
        try (PreparedStatement statement = connection.prepareStatement(capacitySql)) {
            statement.setLong(1, accommodationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return false;
                }
                capacity = resultSet.getInt("capacity");
            }
        }

        // Count overlapping guests (bookings that overlap with the given date range)
        String countSql = """
                SELECT COUNT(*) AS guest_count
                FROM guests
                WHERE accommodation_id = ?
                  AND arrival_at < ?
                  AND departure_at > ?
                """;
        int currentGuests;
        try (PreparedStatement statement = connection.prepareStatement(countSql)) {
            statement.setLong(1, accommodationId);
            statement.setTimestamp(2, timestamp(departureAt));
            statement.setTimestamp(3, timestamp(arrivalAt));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    currentGuests = 0;
                } else {
                    currentGuests = resultSet.getInt("guest_count");
                }
            }
        }

        return currentGuests < capacity;
    }

    /**
     * Handles the room status based on whether the booking is for the future or current/past.
     * - For future bookings: keeps the room as "Ready for Assignment" so other seats remain bookable
     * - For current/past bookings (arrival date is today or earlier): marks room as "Reserved"
     */
    private void handleRoomStatus(Connection connection, long accommodationId, Date arrivalAt) throws SQLException {
        Date now = new Date();
        if (arrivalAt != null && arrivalAt.after(now)) {
            // Future booking - do NOT mark room as Reserved, keep it available for other seat bookings
            return;
        }

        // Current or past booking - mark room as Reserved
        reserveAccommodation(connection, accommodationId);
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

    private void validateStayDates(Date arrivalAt, Date departureAt) throws SQLException {
        if (arrivalAt == null || departureAt == null) {
            throw new SQLException("Arrival and departure dates are required.");
        }
        if (!departureAt.after(arrivalAt)) {
            throw new SQLException("Departure date must be after arrival date.");
        }
    }

    private String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private Date startOfDay(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date nextDay(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1);
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Timestamp timestamp(java.util.Date date) {
        return new Timestamp(date.getTime());
    }
}
