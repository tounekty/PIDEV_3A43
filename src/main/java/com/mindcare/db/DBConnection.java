package com.mindcare.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

public final class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/mindcare?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static final HikariDataSource DATA_SOURCE = createDataSource();

    static {
        // Initialize database schema on startup
        try {
            initializeSchema();
        } catch (SQLException e) {
            System.err.println("Warning: Failed to initialize database schema: " + e.getMessage());
        }
    }

    private DBConnection() {
    }

    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(120000);
        config.setConnectionTimeout(10000);
        config.setMaxLifetime(1800000);
        config.setPoolName("MindCarePool");
        return new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    public static void shutdown() {
        if (DATA_SOURCE != null && !DATA_SOURCE.isClosed()) {
            DATA_SOURCE.close();
        }
    }

    private static void initializeSchema() throws SQLException {
        try (Connection connection = DATA_SOURCE.getConnection()) {
            try (Statement st = connection.createStatement()) {
                // Drop old foreign keys referencing 'user'
                String[] tables = {"appointment", "patient_file"};
                String[] cols = {"idetudiant", "idpsy", "student_id"};
                for (String t : tables) {
                    for (String c : cols) {
                        try {
                            java.sql.ResultSet rs = st.executeQuery("SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA='mindcare' AND TABLE_NAME = '" + t + "' AND COLUMN_NAME = '" + c + "' AND REFERENCED_TABLE_NAME = 'user'");
                            while (rs.next()) {
                                String fk = rs.getString(1);
                                st.execute("ALTER TABLE " + t + " DROP FOREIGN KEY " + fk);
                            }
                        } catch (Exception e) {}
                    }
                }

                // Add new foreign keys referencing 'users'
                try { st.execute("ALTER TABLE appointment ADD CONSTRAINT fk_appt_student FOREIGN KEY (idetudiant) REFERENCES users(id) ON DELETE CASCADE"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE appointment ADD CONSTRAINT fk_appt_psy FOREIGN KEY (idpsy) REFERENCES users(id) ON DELETE SET NULL"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE patient_file ADD CONSTRAINT fk_pf_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE"); } catch (Exception e) {}
            }
        }
    }
}
