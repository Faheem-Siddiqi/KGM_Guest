package com.kgm.config;

import java.sql.SQLException;

public final class DatabaseConnectionFailure extends SQLException {
    public static final String TITLE = "Server Connection Failed";
    public static final String USER_MESSAGE = """
            The application could not connect to the database server.

            Please make sure that:

            - Server PC 516 is turned on.
            - The server and this computer are connected to the correct LAN.
            - MySQL is running on the server.
            - The network connection is working properly.
            """;

    public DatabaseConnectionFailure(SQLException cause) {
        super(TITLE + "\n\n" + USER_MESSAGE, cause.getSQLState(), cause.getErrorCode(), cause);
    }
}
