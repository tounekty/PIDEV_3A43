package org.example.event;

import org.example.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReminderService {
    public void initializeReminderTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS event_reminder (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    event_id INT NOT NULL,
                    minutes_before INT NOT NULL DEFAULT 30,
                    enabled BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uq_event_reminder_user_event (user_id, event_id)
                )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            ensureColumnExists(statement, "minutes_before",
                    "ALTER TABLE event_reminder ADD COLUMN minutes_before INT NOT NULL DEFAULT 30");
            ensureColumnExists(statement, "enabled",
                    "ALTER TABLE event_reminder ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE");
            ensureColumnExists(statement, "created_at",
                    "ALTER TABLE event_reminder ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
        }
    }

    public void upsertReminder(int userId, int eventId, int minutesBefore, boolean enabled) throws SQLException {
        int safeMinutes = Math.max(1, Math.min(minutesBefore, 10080));
        String sql = """
                INSERT INTO event_reminder(user_id, event_id, minutes_before, enabled)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE minutes_before = VALUES(minutes_before), enabled = VALUES(enabled)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, eventId);
            preparedStatement.setInt(3, safeMinutes);
            preparedStatement.setBoolean(4, enabled);
            preparedStatement.executeUpdate();
        }
    }

    public List<Map<String, Object>> getUserUpcomingReminders(int userId, int withinMinutes) throws SQLException {
        int safeWithin = Math.max(1, Math.min(withinMinutes, 10080));
        String sql = """
                SELECT e.id AS event_id,
                       e.titre,
                       e.date_event,
                       e.lieu,
                       er.minutes_before,
                       TIMESTAMPDIFF(MINUTE, NOW(), e.date_event) AS minutes_until_start
                FROM event_reminder er
                INNER JOIN event e ON e.id = er.event_id
                LEFT JOIN reservation_event r ON r.event_id = e.id
                WHERE er.user_id = ?
                  AND er.enabled = TRUE
                  AND e.status <> 'CANCELLED'
                  AND (r.user_id = ? OR e.id_user = ?)
                  AND e.date_event > NOW()
                  AND TIMESTAMPDIFF(MINUTE, NOW(), e.date_event) BETWEEN er.minutes_before AND (er.minutes_before + ?)
                ORDER BY e.date_event ASC
                """;

        List<Map<String, Object>> reminders = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, userId);
            preparedStatement.setInt(3, userId);
            preparedStatement.setInt(4, safeWithin);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    Map<String, Object> reminder = new LinkedHashMap<>();
                    reminder.put("eventId", resultSet.getInt("event_id"));
                    reminder.put("titre", resultSet.getString("titre"));
                    reminder.put("dateEvent", resultSet.getTimestamp("date_event").toLocalDateTime().toString());
                    reminder.put("lieu", resultSet.getString("lieu"));
                    reminder.put("minutesBefore", resultSet.getInt("minutes_before"));
                    reminder.put("minutesUntilStart", resultSet.getInt("minutes_until_start"));
                    reminders.add(reminder);
                }
            }
        }

        return reminders;
    }

    public List<Map<String, Object>> getConfiguredRemindersByUser(int userId) throws SQLException {
        String sql = """
                SELECT er.event_id,
                       er.minutes_before,
                       er.enabled,
                       e.titre,
                       e.date_event
                FROM event_reminder er
                INNER JOIN event e ON e.id = er.event_id
                WHERE er.user_id = ?
                ORDER BY e.date_event ASC
                """;
        List<Map<String, Object>> reminders = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, userId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("eventId", resultSet.getInt("event_id"));
                    row.put("titre", resultSet.getString("titre"));
                    row.put("dateEvent", resultSet.getTimestamp("date_event").toLocalDateTime().toString());
                    row.put("minutesBefore", resultSet.getInt("minutes_before"));
                    row.put("enabled", resultSet.getBoolean("enabled"));
                    reminders.add(row);
                }
            }
        }
        return reminders;
    }

    public void createDefaultReminderIfMissing(int userId, int eventId) throws SQLException {
        String sql = """
                INSERT IGNORE INTO event_reminder(user_id, event_id, minutes_before, enabled)
                VALUES (?, ?, 30, TRUE)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, eventId);
            preparedStatement.executeUpdate();
        }
    }

    private void ensureColumnExists(Statement statement, String columnName, String alterSql) throws SQLException {
        try (ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM event_reminder LIKE '" + columnName + "'")) {
            if (!columns.next()) {
                statement.execute(alterSql);
            }
        }
    }

    /**
     * Récupère tous les rappels d'un utilisateur
     */
    public List<Map<String, Object>> getUserReminders(int userId) throws SQLException {
        return getConfiguredRemindersByUser(userId);
    }

    /**
     * Envoie tous les rappels en attente
     */
    public void sendAllReminders() throws SQLException {
        String sql = """
                SELECT er.user_id,
                       er.event_id,
                       e.titre,
                       e.date_event,
                       TIMESTAMPDIFF(MINUTE, NOW(), e.date_event) AS minutes_until_start
                FROM event_reminder er
                INNER JOIN event e ON e.id = er.event_id
                WHERE er.enabled = TRUE
                  AND e.status <> 'CANCELLED'
                  AND e.date_event > NOW()
                  AND TIMESTAMPDIFF(MINUTE, NOW(), e.date_event) BETWEEN er.minutes_before - 1 AND er.minutes_before + 1
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                int userId = resultSet.getInt("user_id");
                String titre = resultSet.getString("titre");
                LocalDateTime dateEvent = resultSet.getTimestamp("date_event").toLocalDateTime();
                
                // Notification (en production, ce serait un système de notifications réel)
                System.out.println("📢 REMINDER: User " + userId + " - Event '" + titre + "' starts at " + dateEvent);
            }
        }
    }
}
