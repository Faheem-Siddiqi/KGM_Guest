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
                    (SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, arrival_at, departure_at)), 0)
                     FROM guests
                     WHERE departure_at >= arrival_at) AS average_minutes,
                    (SELECT HOUR(arrival_at)
                     FROM guests
                     GROUP BY HOUR(arrival_at)
                     ORDER BY COUNT(*) DESC, HOUR(arrival_at)
                     LIMIT 1) AS peak_arrival_hour
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                int totalSeats = resultSet.getInt("total_seats");
                int occupiedSeats = resultSet.getInt("occupied_seats");
                int reservedSeats = resultSet.getInt("reserved_seats");
                int vacantSeats = Math.max(0, totalSeats - occupiedSeats);
                int occupancyPercent = totalSeats == 0 ? 0 : (int) Math.round((occupiedSeats * 100.0) / totalSeats);
                double averageStayHours = resultSet.getDouble("average_minutes") / 60.0;
                Object peakHour = resultSet.getObject("peak_arrival_hour");
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
        }
        return new DashboardStats(0, 0, 0, 0, 0, 0, "-");
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

    private int totalSeats() throws SQLException {
        String sql = "SELECT COALESCE(SUM(capacity), 0) AS total_seats FROM accommodations WHERE active = TRUE";
        return intValue(sql, "total_seats");
    }

    private int occupiedSeats() throws SQLException {
        String sql = """
                SELECT COUNT(*) AS occupied_seats
                FROM guests
                WHERE arrival_at <= NOW()
                  AND departure_at >= NOW()
                """;
        return intValue(sql, "occupied_seats");
    }

    private double averageStayHours() throws SQLException {
        String sql = """
                SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, arrival_at, departure_at)), 0) AS average_minutes
                FROM guests
                WHERE departure_at >= arrival_at
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getDouble("average_minutes") / 60.0;
            }
        }
        return 0;
    }

    private String peakArrivalHour() throws SQLException {
        String sql = """
                SELECT HOUR(arrival_at) AS arrival_hour, COUNT(*) AS arrivals
                FROM guests
                GROUP BY HOUR(arrival_at)
                ORDER BY arrivals DESC, arrival_hour
                LIMIT 1
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return LocalTime.of(resultSet.getInt("arrival_hour"), 0).format(HOUR_FORMAT);
            }
        }
        return "-";
    }

    private int intValue(String sql, String columnName) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(columnName);
            }
        }
        return 0;
    }

    private String shortLabel(String value) {
        String text = value == null || value.isBlank() ? "-" : value.trim();
        return text.length() <= 14 ? text : text.substring(0, 13) + ".";
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
