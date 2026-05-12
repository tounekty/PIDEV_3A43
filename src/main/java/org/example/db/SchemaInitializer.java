package org.example.db;

import com.mindcare.db.DBConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class SchemaInitializer {
    private SchemaInitializer() {
    }

    public static void ensureSchema() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            ensureUserTable(conn);
            ensureMoodTable(conn);
            ensureJournalTable(conn);
        }
    }

    private static void ensureUserTable(Connection conn) throws SQLException {
        String tableName = tableExists(conn, "users") ? "users" : tableExists(conn, "user") ? "user" : null;
        if (tableName == null) {
            execute(conn, """
                    CREATE TABLE IF NOT EXISTS `users` (
                      id INT(11) NOT NULL AUTO_INCREMENT,
                      first_name VARCHAR(255) NOT NULL,
                      last_name VARCHAR(255) NOT NULL,
                      email VARCHAR(180) NOT NULL,
                      role VARCHAR(50) NOT NULL,
                      password VARCHAR(255) NOT NULL,
                      banned_until DATETIME DEFAULT NULL,
                      reset_code VARCHAR(6) DEFAULT NULL,
                      reset_code_expires_at DATETIME DEFAULT NULL,
                      email_verified TINYINT(1) NOT NULL DEFAULT 0,
                      is_verified TINYINT(1) NOT NULL DEFAULT 0,
                      verification_token VARCHAR(255) DEFAULT NULL,
                      activation_token VARCHAR(255) DEFAULT NULL,
                      activation_token_expires_at DATETIME DEFAULT NULL,
                      reset_password_token VARCHAR(255) DEFAULT NULL,
                      reset_password_token_expires_at DATETIME DEFAULT NULL,
                      face_id_enabled TINYINT(1) NOT NULL DEFAULT 0,
                      face_id_subject VARCHAR(255) DEFAULT NULL,
                      created_by_id INT DEFAULT NULL,
                      updated_by_id INT DEFAULT NULL,
                      created_at DATETIME DEFAULT NULL,
                      updated_at DATETIME DEFAULT NULL,
                      PRIMARY KEY (id),
                      UNIQUE KEY UNIQ_8D93D649E7927C74 (email)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
                    """);
            tableName = "users";
        }

        ensureUserColumn(conn, tableName, "email_verified", "TINYINT(1) NOT NULL DEFAULT 0");
        ensureUserColumn(conn, tableName, "is_verified", "TINYINT(1) NOT NULL DEFAULT 0");
        ensureUserColumn(conn, tableName, "activation_token", "VARCHAR(255) DEFAULT NULL");
        ensureUserColumn(conn, tableName, "activation_token_expires_at", "DATETIME DEFAULT NULL");
        ensureUserColumn(conn, tableName, "reset_password_token", "VARCHAR(255) DEFAULT NULL");
        ensureUserColumn(conn, tableName, "reset_password_token_expires_at", "DATETIME DEFAULT NULL");
        ensureUserColumn(conn, tableName, "reset_code", "VARCHAR(6) DEFAULT NULL");
        ensureUserColumn(conn, tableName, "reset_code_expires_at", "DATETIME DEFAULT NULL");
        ensureUserColumn(conn, tableName, "verification_token", "VARCHAR(255) DEFAULT NULL");
        ensureUserColumn(conn, tableName, "face_id_enabled", "TINYINT(1) NOT NULL DEFAULT 0");
        ensureUserColumn(conn, tableName, "face_id_subject", "VARCHAR(255) DEFAULT NULL");
        ensureUserColumn(conn, tableName, "created_by_id", "INT DEFAULT NULL");
        ensureUserColumn(conn, tableName, "updated_by_id", "INT DEFAULT NULL");
        ensureUserColumn(conn, tableName, "updated_at", "DATETIME DEFAULT NULL");
    }

    private static void ensureUserColumn(Connection conn, String tableName, String columnName, String definition) throws SQLException {
        if (!columnExists(conn, tableName, columnName)) {
            execute(conn, String.format("ALTER TABLE `%s` ADD COLUMN %s %s", tableName, columnName, definition));
        }
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(conn.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static void ensureMoodTable(Connection conn) throws SQLException {
        execute(conn, """
                CREATE TABLE IF NOT EXISTS mood (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  humeur VARCHAR(50) NOT NULL,
                  intensite INT NOT NULL,
                  datemood DATE NOT NULL,
                  id_user INT NOT NULL,
                  ai_analysis LONGTEXT DEFAULT NULL,
                  pdf_path VARCHAR(255) DEFAULT NULL,
                  mood_type VARCHAR(50) NOT NULL DEFAULT 'neutral',
                  mood_date DATE NOT NULL DEFAULT '2000-01-01',
                  note TEXT DEFAULT NULL,
                  admin_comment TEXT DEFAULT NULL,
                  sleep_hours DOUBLE DEFAULT NULL,
                  sleep_time VARCHAR(5) DEFAULT NULL,
                  wake_time VARCHAR(5) DEFAULT NULL,
                  stress_level INT DEFAULT NULL,
                  energy_level INT DEFAULT NULL,
                  support_email_sent TINYINT(1) NOT NULL DEFAULT 0,
                  case_status VARCHAR(40) NOT NULL DEFAULT 'New',
                  KEY IDX_339AEF66B3CA4B (id_user)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
                """);

        if (!columnExists(conn, "mood", "humeur")) {
            execute(conn, "ALTER TABLE mood ADD COLUMN humeur VARCHAR(50) NOT NULL DEFAULT 'neutral'");
        }
        if (!columnExists(conn, "mood", "intensite")) {
            execute(conn, "ALTER TABLE mood ADD COLUMN intensite INT NOT NULL DEFAULT 5");
        }
        if (!columnExists(conn, "mood", "datemood")) {
            execute(conn, "ALTER TABLE mood ADD COLUMN datemood DATE NOT NULL DEFAULT '2000-01-01'");
        }
        if (!columnExists(conn, "mood", "id_user")) {
            execute(conn, "ALTER TABLE mood ADD COLUMN id_user INT NULL");
        }
        if (!columnExists(conn, "mood", "ai_analysis")) {
            execute(conn, "ALTER TABLE mood ADD COLUMN ai_analysis LONGTEXT DEFAULT NULL");
        }
        if (!columnExists(conn, "mood", "pdf_path")) {
            execute(conn, "ALTER TABLE mood ADD COLUMN pdf_path VARCHAR(255) DEFAULT NULL");
        }
        if (!columnExists(conn, "mood", "mood_type")) {
            execute(conn, "ALTER TABLE mood ADD COLUMN mood_type VARCHAR(50) NOT NULL DEFAULT 'neutral'");
        }
        if (!columnExists(conn, "mood", "mood_date")) {
            execute(conn, "ALTER TABLE mood ADD COLUMN mood_date DATE NOT NULL DEFAULT '2000-01-01'");
        }
        if (!columnExists(conn, "mood", "note")) {
            execute(conn, "ALTER TABLE mood ADD COLUMN note TEXT");
        }
        if (!columnExists(conn, "mood", "stress_level")) {
            execute(conn, "ALTER TABLE mood ADD COLUMN stress_level INT NULL");
        }
        if (!columnExists(conn, "mood", "energy_level")) {
            execute(conn, "ALTER TABLE mood ADD COLUMN energy_level INT NULL");
        }
        if (!columnExists(conn, "mood", "sleep_time")) {
            execute(conn, "ALTER TABLE mood ADD COLUMN sleep_time VARCHAR(5) NULL");
        }
        if (!columnExists(conn, "mood", "wake_time")) {
            execute(conn, "ALTER TABLE mood ADD COLUMN wake_time VARCHAR(5) NULL");
        }
        if (!columnExists(conn, "mood", "sleep_hours")) {
            execute(conn, "ALTER TABLE mood ADD COLUMN sleep_hours DOUBLE NULL");
        }
        if (!columnExists(conn, "mood", "admin_comment")) {
            execute(conn, "ALTER TABLE mood ADD COLUMN admin_comment TEXT");
        }
        if (!columnExists(conn, "mood", "support_email_sent")) {
            execute(conn, "ALTER TABLE mood ADD COLUMN support_email_sent BOOLEAN NOT NULL DEFAULT FALSE");
        }
        if (!columnExists(conn, "mood", "case_status")) {
            execute(conn, "ALTER TABLE mood ADD COLUMN case_status VARCHAR(40) NOT NULL DEFAULT 'New'");
        }
    }

    private static void ensureJournalTable(Connection conn) throws SQLException {
        execute(conn, """
                CREATE TABLE IF NOT EXISTS journal (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  title VARCHAR(255) NOT NULL,
                  content TEXT NOT NULL,
                  entry_date DATE NOT NULL,
                  mood_id INT NULL
                )
                """);

        if (!columnExists(conn, "journal", "title")) {
            execute(conn, "ALTER TABLE journal ADD COLUMN title VARCHAR(255) NOT NULL DEFAULT 'Untitled'");
        }
        if (!columnExists(conn, "journal", "content")) {
            execute(conn, "ALTER TABLE journal ADD COLUMN content TEXT");
        }
        if (!columnExists(conn, "journal", "entry_date")) {
            execute(conn, "ALTER TABLE journal ADD COLUMN entry_date DATE NOT NULL DEFAULT '2000-01-01'");
        }
        if (!columnExists(conn, "journal", "mood_id")) {
            execute(conn, "ALTER TABLE journal ADD COLUMN mood_id INT NULL");
        }
        if (!columnExists(conn, "journal", "admin_comment")) {
            execute(conn, "ALTER TABLE journal ADD COLUMN admin_comment TEXT");
        }

        if (!foreignKeyExists(conn, "journal", "fk_journal_mood")) {
            execute(conn, """
                    ALTER TABLE journal
                    ADD CONSTRAINT fk_journal_mood
                    FOREIGN KEY (mood_id) REFERENCES mood(id)
                    ON DELETE SET NULL ON UPDATE CASCADE
                    """);
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tableName, columnName)) {
            return rs.next();
        }
    }

    private static boolean foreignKeyExists(Connection conn, String tableName, String keyName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getImportedKeys(conn.getCatalog(), null, tableName)) {
            while (rs.next()) {
                String fkName = rs.getString("FK_NAME");
                if (keyName.equalsIgnoreCase(fkName)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static void execute(Connection conn, String sql) throws SQLException {
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
        }
    }
}
