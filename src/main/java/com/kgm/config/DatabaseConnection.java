package com.kgm.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private static final String HOST = "localhost";
    private static final int PORT = 3306;
    private static final String DATABASE = "KGM_GUESTS";
    private static final String USERNAME = "FaheemSIDDIQI";
    private static final String PASSWORD = "FS@12345";

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
            + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private DatabaseConnection() {
    }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to connect to MySQL database: " + e.getMessage(), e);
        }
    }
}
