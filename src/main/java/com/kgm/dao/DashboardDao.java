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
        int totalSeats = totalSeats();
        int occupiedSeats = occupiedSeats();
        int vacantSeats = Math.max(0, totalSeats - occupiedSeats);
        int occupancyPercent = totalSeats == 0 ? 0 : (int) Math.round((occupiedSeats * 100.0) / totalSeats);
        double averageStayHours = averageStayHours();
        String peakArrival = peakArrivalHour();

        return new DashboardStats(
                totalSeats,
                vacantSeats,
                occupiedSeats,
                occupancyPercent,
                averageStayHours,
                peakArrival
        );
    }

    public OccupancyChartData loadOccupancyChart() throws SQLException {
        String sql = """
                SELECT
                    a.name,
                    a.capacity,
                    COUNT(g.id) AS occupied
                FROM accommodations a
                LEFT JOIN guests g
                    ON g.accommodation_id = a.id
                   AND g.arrival_at <= NOW()
                   AND g.departure_at >= NOW()
                WHERE a.active = TRUE
                GROUP BY a.id, a.name, a.capacity
                ORDER BY a.name
                LIMIT 8
                """;

        List<String> labels = new ArrayList<>();
        List<Integer> capacity = new ArrayList<>();
        List<Integer> occupied = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                labels.add(shortLabel(resultSet.getString("name")));
                capacity.add(resultSet.getInt("capacity"));
                occupied.add(resultSet.getInt("occupied"));
            }
        }
        return new OccupancyChartData(toStringArray(labels), toIntArray(capacity), toIntArray(occupied));
    }

    public DepartmentChartData loadDepartmentChart() throws SQLException {
        String sql = """
                SELECT requested_department, COUNT(*) AS guests
                FROM guests
                GROUP BY requested_department
                ORDER BY guests DESC, requested_department
                LIMIT 5
                """;

        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                labels.add(shortLabel(resultSet.getString("requested_department")));
                values.add(resultSet.getInt("guests"));
            }
        }
        return new DepartmentChartData(toStringArray(labels), toIntArray(values));
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
        return text.length() <= 10 ? text : text.substring(0, 9) + ".";
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
            int occupancyPercent,
            double averageStayHours,
            String peakArrival
    ) {
    }

    public record OccupancyChartData(String[] labels, int[] capacity, int[] occupied) {
    }

    public record DepartmentChartData(String[] labels, int[] guests) {
    }
}
