package com.mindcare.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class MyDatabase {

    private static final String URL = "jdbc:mysql://localhost:8889/moodtracker?useSSL=false&serverTimezone=UTC&zeroDateTimeBehavior=convertToNull";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    private static MyDatabase instance;

    private MyDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
            // Driver auto-loading works on modern JDBC; keep fallback silent.
        }
    }

    public static synchronized MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
