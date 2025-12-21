package org.minifootball.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = System.getenv("JDBC_URL");
    private static final String USER = System.getenv("USERNAME");
    private static final String PASSWORD = System.getenv("PASSWORD");

    public static Connection getDBConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}