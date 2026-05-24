package com.kgm.dao;

import com.kgm.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DashboardDao {
    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final int TOP_DEPARTMENT_LIMIT = 5;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

    public DashboardStats loadStats() throws SQLException {
        String sql = """
                SELECT
                    (SELECT COALESCE(SUM(capacity), 0)
                     FROM accommodations a
                     WHERE a.active = TRUE) AS total_seats,
                    (SELECT COALESCE(SUM(LEAST(COALESCE(occupied.current_guests, 0), a.capacity)), 0)
                     FROM accommodations a
                     LEFT JOIN (
                         SELECT accommodation_id, COUNT(*) AS current_guests
                         FROM guests
                         WHERE arrival_at <= NOW()
                           AND departure_at >= NOW()
                         GROUP BY accommodation_id
                     ) occupied ON occupied.accommodation_id = a.id
                     WHERE a.active = TRUE) AS occupied_seats,
                    (SELECT COUNT(*)
                     FROM guests g
                     JOIN accommodations a ON a.id = g.accommodation_id
                     WHERE a.active = TRUE
                       AND g.arrival_at > NOW()) AS reserved_seats,
                    (SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, g.arrival_at, g.departure_at)), 0)
                     FROM guests g
                     JOIN accommodations a ON a.id = g.accommodation_id
                     WHERE a.active = TRUE
                       AND g.departure_at >= g.arrival_at
                       AND g.arrival_at >= month_bounds.month_start
                       AND g.arrival_at < month_bounds.next_month) AS monthly_average_minutes,
                    (SELECT COALESCE(SUM(TIMESTAMPDIFF(
                            MINUTE,
                            GREATEST(g.arrival_at, month_bounds.month_start),
                            LEAST(g.departure_at, month_bounds.next_month)
                     )), 0)
                     FROM guests g
                     JOIN accommodations a ON a.id = g.accommodation_id
                     WHERE a.active = TRUE
                       AND g.arrival_at < month_bounds.next_month
                       AND g.departure_at > month_bounds.month_start
                       AND g.departure_at > g.arrival_at) AS monthly_occupied_minutes,
                    TIMESTAMPDIFF(MINUTE, month_bounds.month_start, month_bounds.next_month)
                        AS monthly_minutes_per_seat,
                    (SELECT AVG(HOUR(g.arrival_at) * 60 + MINUTE(g.arrival_at))
                     FROM guests g
                     JOIN accommodations a ON a.id = g.accommodation_id
                     WHERE a.active = TRUE
                       AND g.arrival_at <= NOW()) AS average_arrival_minutes
                FROM (
                    SELECT
                        CAST(DATE_FORMAT(CURRENT_DATE(), '%Y-%m-01') AS DATETIME) AS month_start,
                        DATE_ADD(CAST(DATE_FORMAT(CURRENT_DATE(), '%Y-%m-01') AS DATETIME), INTERVAL 1 MONTH)
                            AS next_month
                ) month_bounds
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return dashboardStats(resultSet);
            }
        }
        return new DashboardStats(0, 0, 0, 0, 0, 0, "-");
    }

    public DashboardStats loadStatsForAccommodation(long accommodationId) throws SQLException {
        String sql = """
                SELECT
                    (SELECT COALESCE(SUM(capacity), 0)
                     FROM accommodations a
                     WHERE a.id = ?
                       AND a.active = TRUE) AS total_seats,
                    (SELECT COALESCE(SUM(LEAST(COALESCE(occupied.current_guests, 0), a.capacity)), 0)
                     FROM accommodations a
                     LEFT JOIN (
                         SELECT accommodation_id, COUNT(*) AS current_guests
                         FROM guests
                         WHERE accommodation_id = ?
                           AND arrival_at <= NOW()
                           AND departure_at >= NOW()
                         GROUP BY accommodation_id
                     ) occupied ON occupied.accommodation_id = a.id
                     WHERE a.id = ?
                       AND a.active = TRUE) AS occupied_seats,
                    (SELECT COUNT(*)
                     FROM guests g
                     JOIN accommodations a ON a.id = g.accommodation_id
                     WHERE g.accommodation_id = ?
                       AND a.active = TRUE
                       AND g.arrival_at > NOW()) AS reserved_seats,
                    (SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, g.arrival_at, g.departure_at)), 0)
                     FROM guests g
                     JOIN accommodations a ON a.id = g.accommodation_id
                     WHERE g.accommodation_id = ?
                       AND a.active = TRUE
                       AND g.departure_at >= g.arrival_at
                       AND g.arrival_at >= month_bounds.month_start
                       AND g.arrival_at < month_bounds.next_month) AS monthly_average_minutes,
                    (SELECT COALESCE(SUM(TIMESTAMPDIFF(
                            MINUTE,
                            GREATEST(g.arrival_at, month_bounds.month_start),
                            LEAST(g.departure_at, month_bounds.next_month)
                     )), 0)
                     FROM guests g
                     JOIN accommodations a ON a.id = g.accommodation_id
                     WHERE g.accommodation_id = ?
                       AND a.active = TRUE
                       AND g.arrival_at < month_bounds.next_month
                       AND g.departure_at > month_bounds.month_start
                       AND g.departure_at > g.arrival_at) AS monthly_occupied_minutes,
                    TIMESTAMPDIFF(MINUTE, month_bounds.month_start, month_bounds.next_month)
                        AS monthly_minutes_per_seat,
                    (SELECT AVG(HOUR(g.arrival_at) * 60 + MINUTE(g.arrival_at))
                     FROM guests g
                     JOIN accommodations a ON a.id = g.accommodation_id
                     WHERE g.accommodation_id = ?
                       AND a.active = TRUE
                       AND g.arrival_at <= NOW()) AS average_arrival_minutes
                FROM (
                    SELECT
                        CAST(DATE_FORMAT(CURRENT_DATE(), '%Y-%m-01') AS DATETIME) AS month_start,
                        DATE_ADD(CAST(DATE_FORMAT(CURRENT_DATE(), '%Y-%m-01') AS DATETIME), INTERVAL 1 MONTH)
                            AS next_month
                ) month_bounds
        """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 1; index <= 7; index++) {
                statement.setLong(index, accommodationId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return dashboardStats(resultSet);
                }
            }
        }
        return new DashboardStats(0, 0, 0, 0, 0, 0, "-");
    }

    private DashboardStats dashboardStats(ResultSet resultSet) throws SQLException {
        int totalSeats = resultSet.getInt("total_seats");
        int occupiedSeats = resultSet.getInt("occupied_seats");
        int reservedSeats = resultSet.getInt("reserved_seats");
        int vacantSeats = Math.max(0, totalSeats - occupiedSeats);
        double monthlyOccupiedMinutes = resultSet.getDouble("monthly_occupied_minutes");
        int monthlyMinutesPerSeat = resultSet.getInt("monthly_minutes_per_seat");
        int occupancyPercent = monthlyOccupancyPercent(
                monthlyOccupiedMinutes,
                totalSeats,
                monthlyMinutesPerSeat
        );
        double averageStayHours = resultSet.getDouble("monthly_average_minutes") / 60.0;
        double averageArrivalMinutes = resultSet.getDouble("average_arrival_minutes");
        String averageArrival = resultSet.wasNull() ? "-" : arrivalTimeText(averageArrivalMinutes);
        return new DashboardStats(
                totalSeats,
                vacantSeats,
                occupiedSeats,
                reservedSeats,
                occupancyPercent,
                averageStayHours,
                averageArrival
        );
    }

    private String arrivalTimeText(double averageArrivalMinutes) {
        int roundedMinutes = (int) Math.round(averageArrivalMinutes);
        int dayMinute = Math.floorMod(roundedMinutes, MINUTES_PER_DAY);
        return LocalTime.of(dayMinute / 60, dayMinute % 60).format(TIME_FORMAT);
    }

    private int monthlyOccupancyPercent(
            double monthlyOccupiedMinutes,
            int totalSeats,
            int monthlyMinutesPerSeat
    ) {
        if (totalSeats <= 0 || monthlyMinutesPerSeat <= 0) {
            return 0;
        }
        double monthlyAvailableMinutes = totalSeats * (double) monthlyMinutesPerSeat;
        int occupancyPercent = (int) Math.round((monthlyOccupiedMinutes * 100.0) / monthlyAvailableMinutes);
        return Math.max(0, Math.min(100, occupancyPercent));
    }

    public OccupancyChartData loadOccupancyChart() throws SQLException {
        return loadOccupancyChart("");
    }

    public OccupancyChartData loadOccupancyChart(String category) throws SQLException {
        String normalizedCategory = category == null ? "" : category.trim();
        String sql = normalizedCategory.isEmpty() ? """
                SELECT
                    c.name AS category_name,
                    a.name,
                    a.capacity,
                    COUNT(g.id) AS occupied
                FROM accommodations a
                JOIN accommodation_categories c ON c.id = a.category_id
                LEFT JOIN guests g
                    ON g.accommodation_id = a.id
                   AND g.arrival_at <= NOW()
                   AND g.departure_at >= NOW()
                WHERE a.active = TRUE
                GROUP BY a.id, c.name, a.name, a.capacity
                ORDER BY c.name, a.name
                """ : """
                SELECT
                    c.name AS category_name,
                    a.name,
                    a.capacity,
                    COUNT(g.id) AS occupied
                FROM accommodations a
                JOIN accommodation_categories c ON c.id = a.category_id
                LEFT JOIN guests g
                    ON g.accommodation_id = a.id
                   AND g.arrival_at <= NOW()
                   AND g.departure_at >= NOW()
                WHERE a.active = TRUE
                  AND c.name = ?
                GROUP BY a.id, c.name, a.name, a.capacity
                ORDER BY c.name, a.name
                """;

        List<String> labels = new ArrayList<>();
        List<Integer> capacity = new ArrayList<>();
        List<Integer> occupied = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (!normalizedCategory.isEmpty()) {
                statement.setString(1, normalizedCategory);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String room = resultSet.getString("name");
                String label = normalizedCategory.isEmpty()
                        ? shortLabel(resultSet.getString("category_name")) + "\n" + shortLabel(room)
                        : shortLabel(room);
                labels.add(label);
                capacity.add(resultSet.getInt("capacity"));
                occupied.add(resultSet.getInt("occupied"));
            }
            }
        }
        return new OccupancyChartData(toStringArray(labels), toIntArray(capacity), toIntArray(occupied));
    }

    public DepartmentChartData loadDepartmentChart() throws SQLException {
        String sql = """
                SELECT
                    COALESCE(NULLIF(TRIM(g.requested_department), ''), 'Unknown') AS department_name,
                    COUNT(g.id) AS guests
                FROM guests g
                GROUP BY COALESCE(NULLIF(TRIM(g.requested_department), ''), 'Unknown')
                ORDER BY guests DESC, department_name
                LIMIT ?
                """;

        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        int totalGuestRequests;
        try (Connection connection = DatabaseConnection.getConnection()) {
            totalGuestRequests = countGuestRequests(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, TOP_DEPARTMENT_LIMIT);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        labels.add(labelText(resultSet.getString("department_name")));
                        values.add(resultSet.getInt("guests"));
                    }
                }
            }
        }
        return new DepartmentChartData(toStringArray(labels), toIntArray(values), totalGuestRequests);
    }

    private int countGuestRequests(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) AS total_requests FROM guests";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt("total_requests") : 0;
        }
    }

    public String[] loadAccommodationCategories() throws SQLException {
        String sql = """
                SELECT name
                FROM accommodation_categories
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
        return toStringArray(categories);
    }
// NAME TRUNCATION FOR CHART LABELS - MAX 16 CHARACTERS WITH ELLIPSIS IF TRUNCATED
    private String shortLabel(String value) {
        String text = labelText(value);
        return text.length() <= 16 ? text : text.substring(0, 15) + ".";
    }

    private String labelText(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String[] toStringArray(List<String> values) {
        return values.toArray(new String[0]);
    }

    private int[] toIntArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    public record DashboardStats(
            int totalSeats,
            int vacantSeats,
            int occupiedSeats,
            int reservedSeats,
            int occupancyPercent,
            double averageStayHours,
            String averageArrivalTime
    ) {
        public int upcomingGuests() {
            return reservedSeats;
        }
    }

    public record OccupancyChartData(String[] labels, int[] capacity, int[] occupied) {
    }

    public record DepartmentChartData(String[] labels, int[] guests, int totalGuestRequests) {
        public DepartmentChartData(String[] labels, int[] guests) {
            this(labels, guests, sum(guests));
        }

        private static int sum(int[] values) {
            int total = 0;
            if (values == null) {
                return total;
            }
            for (int value : values) {
                total += Math.max(0, value);
            }
            return total;
        }
    }

    /**
     * Category statistics for KPI dashboard display
     * Contains room-level and bed-level metrics per accommodation category
     */
    public record CategoryKpiStats(
            String categoryName,
            int totalRooms,
            int occupiedRooms,
            int vacantRooms,
            int totalBeds,
            int occupiedBeds,
            int vacantBeds
    ) {
    }

    /**
     * Loads KPI statistics per accommodation category
     * Excludes security block category if specified
     * @param excludeSecurityBlock if true, excludes "Security Block" category
     * @return List of CategoryKpiStats
     */
    public List<CategoryKpiStats> loadCategoryKpiStats(boolean excludeSecurityBlock) throws SQLException {
        String sql = """
                SELECT
                    c.name AS category_name,
                    COUNT(a.id) AS total_rooms,
                    COALESCE(SUM(CASE
                        WHEN occupied.current_guests > 0 THEN 1
                        ELSE 0
                    END), 0) AS occupied_rooms,
                    COUNT(a.id) - COALESCE(SUM(CASE
                        WHEN occupied.current_guests > 0 THEN 1
                        ELSE 0
                    END), 0) AS vacant_rooms,
                    COALESCE(SUM(a.capacity), 0) AS total_beds,
                    COALESCE(SUM(LEAST(COALESCE(occupied.current_guests, 0), a.capacity)), 0) AS occupied_beds,
                    COALESCE(SUM(a.capacity), 0) - COALESCE(SUM(LEAST(COALESCE(occupied.current_guests, 0), a.capacity)), 0) AS vacant_beds
                FROM accommodation_categories c
                LEFT JOIN accommodations a ON a.category_id = c.id AND a.active = TRUE
                LEFT JOIN (
                    SELECT accommodation_id, COUNT(*) AS current_guests
                    FROM guests
                    WHERE arrival_at <= NOW()
                      AND departure_at >= NOW()
                    GROUP BY accommodation_id
                ) occupied ON occupied.accommodation_id = a.id
                WHERE c.active = TRUE
                """ + (excludeSecurityBlock ? "AND LOWER(c.name) <> LOWER(?)" : "") + """
                GROUP BY c.id, c.name
                ORDER BY c.name
                """;

        List<CategoryKpiStats> stats = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (excludeSecurityBlock) {
                statement.setString(1, "Security Block");
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    stats.add(new CategoryKpiStats(
                            resultSet.getString("category_name"),
                            resultSet.getInt("total_rooms"),
                            resultSet.getInt("occupied_rooms"),
                            resultSet.getInt("vacant_rooms"),
                            resultSet.getInt("total_beds"),
                            resultSet.getInt("occupied_beds"),
                            resultSet.getInt("vacant_beds")
                    ));
                }
            }
        }
        return stats;
    }
}
