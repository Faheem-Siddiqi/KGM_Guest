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
    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("h:00 a");

    public DashboardStats loadStats() throws SQLException {
        String sql = """
                SELECT
                    (SELECT COALESCE(SUM(capacity), 0)
                     FROM accommodations
                     WHERE active = TRUE) AS total_seats,
                    (SELECT COUNT(*)
                     FROM guests
                     WHERE arrival_at <= NOW()
                       AND departure_at >= NOW()) AS occupied_seats,
                    (SELECT COUNT(*)
                     FROM guests
                     WHERE arrival_at > NOW()) AS reserved_seats,
                    (SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, g.arrival_at, g.departure_at)), 0)
                     FROM guests g
                     WHERE g.departure_at >= g.arrival_at
                       AND g.arrival_at >= month_bounds.month_start
                       AND g.arrival_at < month_bounds.next_month) AS monthly_average_minutes,
                    (SELECT COALESCE(SUM(TIMESTAMPDIFF(
                            MINUTE,
                            GREATEST(g.arrival_at, month_bounds.month_start),
                            LEAST(g.departure_at, month_bounds.next_month)
                     )), 0)
                     FROM guests g
                     WHERE g.arrival_at < month_bounds.next_month
                       AND g.departure_at > month_bounds.month_start
                       AND g.departure_at > g.arrival_at) AS monthly_occupied_minutes,
                    TIMESTAMPDIFF(MINUTE, month_bounds.month_start, month_bounds.next_month)
                        AS monthly_minutes_per_seat,
                    (SELECT HOUR(g.arrival_at)
                     FROM guests g
                     WHERE g.arrival_at >= month_bounds.month_start
                       AND g.arrival_at < month_bounds.next_month
                     GROUP BY HOUR(g.arrival_at)
                     ORDER BY COUNT(*) DESC, HOUR(g.arrival_at)
                     LIMIT 1) AS monthly_peak_arrival_hour
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
                     FROM accommodations
                     WHERE id = ?
                       AND active = TRUE) AS total_seats,
                    (SELECT COUNT(*)
                     FROM guests
                     WHERE accommodation_id = ?
                       AND arrival_at <= NOW()
                       AND departure_at >= NOW()) AS occupied_seats,
                    (SELECT COUNT(*)
                     FROM guests
                     WHERE accommodation_id = ?
                       AND arrival_at > NOW()) AS reserved_seats,
                    (SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, g.arrival_at, g.departure_at)), 0)
                     FROM guests g
                     WHERE g.accommodation_id = ?
                       AND g.departure_at >= g.arrival_at
                       AND g.arrival_at >= month_bounds.month_start
                       AND g.arrival_at < month_bounds.next_month) AS monthly_average_minutes,
                    (SELECT COALESCE(SUM(TIMESTAMPDIFF(
                            MINUTE,
                            GREATEST(g.arrival_at, month_bounds.month_start),
                            LEAST(g.departure_at, month_bounds.next_month)
                     )), 0)
                     FROM guests g
                     WHERE g.accommodation_id = ?
                       AND g.arrival_at < month_bounds.next_month
                       AND g.departure_at > month_bounds.month_start
                       AND g.departure_at > g.arrival_at) AS monthly_occupied_minutes,
                    TIMESTAMPDIFF(MINUTE, month_bounds.month_start, month_bounds.next_month)
                        AS monthly_minutes_per_seat,
                    (SELECT HOUR(g.arrival_at)
                     FROM guests g
                     WHERE g.accommodation_id = ?
                       AND g.arrival_at >= month_bounds.month_start
                       AND g.arrival_at < month_bounds.next_month
                     GROUP BY HOUR(g.arrival_at)
                     ORDER BY COUNT(*) DESC, HOUR(g.arrival_at)
                     LIMIT 1) AS monthly_peak_arrival_hour
                FROM (
                    SELECT
                        CAST(DATE_FORMAT(CURRENT_DATE(), '%Y-%m-01') AS DATETIME) AS month_start,
                        DATE_ADD(CAST(DATE_FORMAT(CURRENT_DATE(), '%Y-%m-01') AS DATETIME), INTERVAL 1 MONTH)
                            AS next_month
                ) month_bounds
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 1; index <= 6; index++) {
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
        Object peakHour = resultSet.getObject("monthly_peak_arrival_hour");
        String peakArrival = peakHour instanceof Number number
                ? LocalTime.of(number.intValue(), 0).format(HOUR_FORMAT)
                : "-";
        return new DashboardStats(
                totalSeats,
                vacantSeats,
                occupiedSeats,
                reservedSeats,
                occupancyPercent,
                averageStayHours,
                peakArrival
        );
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
        return (int) Math.round((monthlyOccupiedMinutes * 100.0) / monthlyAvailableMinutes);
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
                    COALESCE(NULLIF(TRIM(requested_department), ''), 'Unknown') AS department_name,
                    COUNT(*) AS guests
                FROM guests
                GROUP BY department_name
                ORDER BY guests DESC, department_name
                LIMIT 5
                """;

        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                labels.add(shortLabel(resultSet.getString("department_name")));
                values.add(resultSet.getInt("guests"));
            }
        }
        return new DepartmentChartData(toStringArray(labels), toIntArray(values));
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
        String text = value == null || value.isBlank() ? "-" : value.trim();
        return text.length() <= 16 ? text : text.substring(0, 15) + ".";
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
            String peakArrival
    ) {
        public int upcomingGuests() {
            return reservedSeats;
        }
    }

    public record OccupancyChartData(String[] labels, int[] capacity, int[] occupied) {
    }

    public record DepartmentChartData(String[] labels, int[] guests) {
    }
}
