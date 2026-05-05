package com.kgm.config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class DatabaseConnection {
    private static final String URL = "jdbc:sqlite:kgm_employees.db";
    public static Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(URL);
            if (conn != null) {
                System.out.println("  => Database connected successfully!");
            }
            return conn;
        } catch (SQLException e) {
            System.out.println("  =>  Connection failed!");
            e.printStackTrace();
            return null;
        }
    }
}