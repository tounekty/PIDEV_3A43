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
            // Initialize mood and journal tables
            org.example.db.SchemaInitializer.ensureSchema();
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
                            java.sql.ResultSet rs = st.executeQuery("SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA='moodtracker' AND TABLE_NAME = '" + t + "' AND COLUMN_NAME = '" + c + "' AND REFERENCED_TABLE_NAME = 'user'");
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

                // Ensure appointment table has report fields used by AppointmentService
                try { st.execute("ALTER TABLE appointment ADD COLUMN report_name VARCHAR(255) NULL"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE appointment ADD COLUMN report_updated_at DATETIME NULL"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE appointment ADD COLUMN patient_file_id INT NULL"); } catch (Exception e) {}

                // Ensure event table has all required columns
                try { st.execute("ALTER TABLE event ADD COLUMN description TEXT NULL"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE event ADD COLUMN categorie VARCHAR(100) NULL"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE event ADD COLUMN image VARCHAR(255) NULL"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE event ADD COLUMN title VARCHAR(255) NOT NULL DEFAULT ''"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE event ADD COLUMN event_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE event ADD COLUMN location VARCHAR(255) NOT NULL DEFAULT ''"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE event ADD COLUMN id_user INT NULL DEFAULT NULL"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE event ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE event ADD COLUMN duration_minutes INT NOT NULL DEFAULT 60"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE event ADD COLUMN overbooking_percentage DOUBLE NOT NULL DEFAULT 10.0"); } catch (Exception e) {}

                // Ensure sujet_forum table has all required columns
                try { st.execute("ALTER TABLE sujet_forum ADD COLUMN is_pinned TINYINT(1) NOT NULL DEFAULT 0"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE sujet_forum ADD COLUMN is_anonymous TINYINT(1) NOT NULL DEFAULT 0"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE sujet_forum ADD COLUMN status VARCHAR(100)"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE sujet_forum ADD COLUMN category VARCHAR(100)"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE sujet_forum ADD COLUMN attachment_path VARCHAR(500)"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE sujet_forum ADD COLUMN attachment_mime_type VARCHAR(150)"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE sujet_forum ADD COLUMN attachment_size BIGINT"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE sujet_forum ADD COLUMN id_user INT"); } catch (Exception e) {}

                // Ensure message_forum table has all required columns
                try { st.execute("ALTER TABLE message_forum ADD COLUMN is_anonymous TINYINT(1) NOT NULL DEFAULT 0"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE message_forum ADD COLUMN id_user INT"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE message_forum ADD COLUMN id_sujet INT NOT NULL"); } catch (Exception e) {}

                // Ensure reservation_event table has all required columns
                try { st.execute("ALTER TABLE reservation_event ADD COLUMN event_id INT NOT NULL"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE reservation_event ADD COLUMN user_id INT NOT NULL"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE reservation_event ADD COLUMN reserved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE reservation_event ADD COLUMN statut VARCHAR(20)"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE reservation_event ADD COLUMN nom VARCHAR(100)"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE reservation_event ADD COLUMN prenom VARCHAR(100)"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE reservation_event ADD COLUMN telephone VARCHAR(30)"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE reservation_event ADD COLUMN commentaire LONGTEXT"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE reservation_event ADD COLUMN confirmation_token VARCHAR(64)"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE reservation_event ADD COLUMN sms_reminder_sent TINYINT(1) NOT NULL DEFAULT 0"); } catch (Exception e) {}

                // Ensure journal table has user_id column (added in later version)
                try { st.execute("ALTER TABLE journal ADD COLUMN user_id INT NULL"); } catch (Exception e) {}
                // Ensure mood table has admin fields
                try { st.execute("ALTER TABLE mood ADD COLUMN admin_comment TEXT NULL"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE mood ADD COLUMN support_email_sent TINYINT(1) NOT NULL DEFAULT 0"); } catch (Exception e) {}
                try { st.execute("ALTER TABLE mood ADD COLUMN case_status VARCHAR(60) NOT NULL DEFAULT 'New'"); } catch (Exception e) {}
            }
        }
    }
}
