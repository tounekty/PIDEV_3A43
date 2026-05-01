package org.example.repository.impl;

import org.example.config.DatabaseConnection;
import org.example.model.ReservationRecord;
import org.example.repository.ReservationRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReservationRepositoryImpl implements ReservationRepository {

    @Override
    public void createTableIfNotExists() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS reservation_event (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    date_reservation DATETIME DEFAULT CURRENT_TIMESTAMP,
                    statut VARCHAR(20),
                    nom VARCHAR(100),
                    prenom VARCHAR(100),
                    telephone VARCHAR(30),
                    commentaire LONGTEXT,
                    confirmation_token VARCHAR(64),
                    sms_reminder_sent TINYINT(1) NOT NULL DEFAULT 0,
                    id_event INT,
                    id_user INT,
                    event_id INT NOT NULL,
                    user_id INT NOT NULL,
                    reserved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    @Override
    public void save(int eventId, int userId) throws SQLException {
        String sql = """
                INSERT INTO reservation_event (event_id, user_id, id_event, id_user, reserved_at, date_reservation, sms_reminder_sent)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            statement.setInt(1, eventId);
            statement.setInt(2, userId);
            statement.setInt(3, eventId);
            statement.setInt(4, userId);
            statement.setTimestamp(5, now);
            statement.setTimestamp(6, now);
            statement.setInt(7, 0);
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteByEventId(int eventId) throws SQLException {
        String sql = "DELETE FROM reservation_event WHERE event_id = ? OR id_event = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            statement.setInt(2, eventId);
            statement.executeUpdate();
        }
    }

    @Override
    public boolean existsByEventAndUser(int eventId, int userId) throws SQLException {
        String sql = """
                SELECT 1 FROM reservation_event
                WHERE (event_id = ? OR id_event = ?) AND (user_id = ? OR id_user = ?)
                LIMIT 1
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            statement.setInt(2, eventId);
            statement.setInt(3, userId);
            statement.setInt(4, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    @Override
    public Set<Integer> findReservedEventsByUser(int userId) throws SQLException {
        String sql = """
                SELECT DISTINCT COALESCE(event_id, id_event) AS event_id
                FROM reservation_event
                WHERE user_id = ? OR id_user = ?
                """;

        Set<Integer> eventIds = new HashSet<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    eventIds.add(resultSet.getInt("event_id"));
                }
            }
        }
        return eventIds;
    }

    @Override
    public Map<Integer, Integer> findReservationCounts() throws SQLException {
        String sql = """
                SELECT COALESCE(event_id, id_event) AS event_id, COUNT(*) AS count
                FROM reservation_event
                GROUP BY COALESCE(event_id, id_event)
                """;

        Map<Integer, Integer> counts = new HashMap<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                counts.put(resultSet.getInt("event_id"), resultSet.getInt("count"));
            }
        }
        return counts;
    }

    @Override
    public int countByEventId(int eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservation_event WHERE event_id = ? OR id_event = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            statement.setInt(2, eventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    @Override
    public List<ReservationRecord> findAll() throws SQLException {
        String sql = """
                SELECT r.id,
                       COALESCE(r.event_id, r.id_event) AS event_id,
                       COALESCE(r.user_id, r.id_user) AS user_id,
                       e.titre AS event_title,
                       u.username AS username,
                       COALESCE(r.reserved_at, r.date_reservation) AS reserved_at
                FROM reservation_event r
                LEFT JOIN event e ON e.id = COALESCE(r.event_id, r.id_event)
                LEFT JOIN users u ON u.id = COALESCE(r.user_id, r.id_user)
                ORDER BY reserved_at DESC
                """;

        List<ReservationRecord> records = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                records.add(new ReservationRecord(
                        resultSet.getInt("id"),
                        resultSet.getInt("event_id"),
                        resultSet.getInt("user_id"),
                        resultSet.getString("event_title"),
                        resultSet.getString("username"),
                        toLocalDateTime(resultSet.getTimestamp("reserved_at"))
                ));
            }
        }
        return records;
    }

    @Override
    public int countTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservation_event";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    @Override
    public Map<String, Integer> countByCategory() throws SQLException {
        String sql = """
                SELECT COALESCE(e.categorie, '') AS categorie, COUNT(*) AS count
                FROM reservation_event r
                LEFT JOIN event e ON e.id = COALESCE(r.event_id, r.id_event)
                GROUP BY COALESCE(e.categorie, '')
                """;

        Map<String, Integer> stats = new HashMap<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                stats.put(resultSet.getString("categorie"), resultSet.getInt("count"));
            }
        }
        return stats;
    }

    @Override
    public Map<String, Integer> countEventsByCategory() throws SQLException {
        String sql = "SELECT COALESCE(categorie, '') AS categorie, COUNT(*) AS count FROM event GROUP BY COALESCE(categorie, '')";

        Map<String, Integer> stats = new HashMap<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                stats.put(resultSet.getString("categorie"), resultSet.getInt("count"));
            }
        }
        return stats;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
