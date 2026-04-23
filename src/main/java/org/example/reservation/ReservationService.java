package org.example.reservation;

import org.example.config.DatabaseConnection;
import org.example.event.Event;
import org.example.util.ValidationUtil;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReservationService {
    public void initializeReservations() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS reservation_event (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    event_id INT NOT NULL,
                    user_id INT NOT NULL,
                    reserved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            ensureColumnExists(statement, "event_id", "ALTER TABLE reservation_event ADD COLUMN event_id INT NOT NULL");
            ensureColumnExists(statement, "user_id", "ALTER TABLE reservation_event ADD COLUMN user_id INT NOT NULL");
            ensureColumnExists(statement, "reserved_at", "ALTER TABLE reservation_event ADD COLUMN reserved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");

            if (columnExists(statement, "statut")) {
                statement.execute("ALTER TABLE reservation_event MODIFY COLUMN statut VARCHAR(20) NULL DEFAULT NULL");
            }
            if (columnExists(statement, "nom")) {
                statement.execute("ALTER TABLE reservation_event MODIFY COLUMN nom VARCHAR(100) NULL DEFAULT NULL");
            }
            if (columnExists(statement, "prenom")) {
                statement.execute("ALTER TABLE reservation_event MODIFY COLUMN prenom VARCHAR(100) NULL DEFAULT NULL");
            }
            if (columnExists(statement, "telephone")) {
                statement.execute("ALTER TABLE reservation_event MODIFY COLUMN telephone VARCHAR(30) NULL DEFAULT NULL");
            }
            if (columnExists(statement, "id_event")) {
                statement.execute("ALTER TABLE reservation_event MODIFY COLUMN id_event INT NULL DEFAULT NULL");
            }
            if (columnExists(statement, "id_user")) {
                statement.execute("ALTER TABLE reservation_event MODIFY COLUMN id_user INT NULL DEFAULT NULL");
            }

            if (columnExists(statement, "date_reservation")) {
                if (!columnExists(statement, "reserved_at")) {
                    statement.execute("ALTER TABLE reservation_event ADD COLUMN reserved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
                }
                statement.execute("UPDATE reservation_event SET reserved_at = date_reservation WHERE reserved_at IS NULL");
                statement.execute("ALTER TABLE reservation_event MODIFY COLUMN date_reservation DATETIME NULL DEFAULT CURRENT_TIMESTAMP");
            }
        }
    }

    public void reserveEvent(Event event, int userId) throws SQLException {
        // Validation des données
        try {
            ValidationUtil.validateReservation(event.getId(), userId);
        } catch (IllegalArgumentException e) {
            throw new SQLException("Erreur de validation : " + e.getMessage(), e);
        }

        // Vérifier si l'utilisateur a déjà une réservation
        if (hasReservation(event.getId(), userId)) {
            throw new SQLException("Vous avez déjà réservé cet événement.");
        }

        // Vérifier la capacité
        int currentReservations = getReservationCountByEvent(event.getId());
        if (currentReservations >= event.getCapacite()) {
            throw new SQLException("La capacité maximale de cet événement est atteinte.");
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            String timestampColumn = getTimestampColumn(connection);
            String sql = "INSERT INTO reservation_event(event_id, user_id, " + timestampColumn + ") VALUES (?, ?, ?)";

            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, event.getId());
                preparedStatement.setInt(2, userId);
                preparedStatement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                preparedStatement.executeUpdate();
            }
        }
    }

    public boolean hasReservation(int eventId, int userId) throws SQLException {
        String sql = "SELECT id FROM reservation_event WHERE event_id = ? AND user_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, eventId);
            preparedStatement.setInt(2, userId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public Set<Integer> getReservedEventIdsByUser(int userId) throws SQLException {
        String sql = "SELECT event_id FROM reservation_event WHERE user_id = ?";
        Set<Integer> reservedIds = new HashSet<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, userId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    reservedIds.add(resultSet.getInt("event_id"));
                }
            }
        }

        return reservedIds;
    }

    public boolean leaveEvent(int eventId, int userId) throws SQLException {
        String sql = "DELETE FROM reservation_event WHERE event_id = ? AND user_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, eventId);
            preparedStatement.setInt(2, userId);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    public Map<Integer, Integer> getReservationCountsByEvent() throws SQLException {
        String sql = """
                SELECT event_id, COUNT(*) AS total
                FROM reservation_event
                GROUP BY event_id
                """;
        Map<Integer, Integer> counts = new HashMap<>();

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                counts.put(resultSet.getInt("event_id"), resultSet.getInt("total"));
            }
        }

        return counts;
    }

    public int getReservationCountByEvent(int eventId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM reservation_event WHERE event_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, eventId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("total");
                }
            }
        }

        return 0;
    }

    public List<ReservationRecord> getAllReservations() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            boolean hasReservedAt = hasColumn(connection, "reserved_at");
            boolean hasDateReservation = hasColumn(connection, "date_reservation");
            String timestampExpression;

            if (hasReservedAt && hasDateReservation) {
                timestampExpression = "COALESCE(r.reserved_at, r.date_reservation) AS reserved_at";
            } else if (hasReservedAt) {
                timestampExpression = "r.reserved_at AS reserved_at";
            } else if (hasDateReservation) {
                timestampExpression = "r.date_reservation AS reserved_at";
            } else {
                timestampExpression = "CURRENT_TIMESTAMP AS reserved_at";
            }

            String sql = String.format("""
                    SELECT r.id,
                           r.event_id,
                           r.user_id,
                           e.titre AS event_title,
                           u.username,
                           %s
                    FROM reservation_event r
                    LEFT JOIN event e ON e.id = r.event_id
                    LEFT JOIN app_user u ON u.id = r.user_id
                    ORDER BY reserved_at DESC
                    """, timestampExpression);

            java.util.ArrayList<ReservationRecord> reservations = new java.util.ArrayList<>();

            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {
                while (resultSet.next()) {
                    Timestamp reservedAt = resultSet.getTimestamp("reserved_at");
                    reservations.add(new ReservationRecord(
                            resultSet.getInt("id"),
                            resultSet.getInt("event_id"),
                            resultSet.getInt("user_id"),
                            resultSet.getString("event_title"),
                            resultSet.getString("username"),
                            reservedAt == null ? LocalDateTime.now() : reservedAt.toLocalDateTime()
                    ));
                }
            }

            return reservations;
        }
    }

    public List<ReservationRecord> getParticipantsByEvent(int eventId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            boolean hasReservedAt = hasColumn(connection, "reserved_at");
            boolean hasDateReservation = hasColumn(connection, "date_reservation");
            String timestampExpression;

            if (hasReservedAt && hasDateReservation) {
                timestampExpression = "COALESCE(r.reserved_at, r.date_reservation) AS reserved_at";
            } else if (hasReservedAt) {
                timestampExpression = "r.reserved_at AS reserved_at";
            } else if (hasDateReservation) {
                timestampExpression = "r.date_reservation AS reserved_at";
            } else {
                timestampExpression = "CURRENT_TIMESTAMP AS reserved_at";
            }

            String sql = String.format("""
                    SELECT r.id,
                           r.event_id,
                           r.user_id,
                           e.titre AS event_title,
                           u.username,
                           %s
                    FROM reservation_event r
                    LEFT JOIN event e ON e.id = r.event_id
                    LEFT JOIN app_user u ON u.id = r.user_id
                    WHERE r.event_id = ?
                    ORDER BY reserved_at DESC
                    """, timestampExpression);

            List<ReservationRecord> reservations = new ArrayList<>();
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, eventId);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        Timestamp reservedAt = resultSet.getTimestamp("reserved_at");
                        reservations.add(new ReservationRecord(
                                resultSet.getInt("id"),
                                resultSet.getInt("event_id"),
                                resultSet.getInt("user_id"),
                                resultSet.getString("event_title"),
                                resultSet.getString("username"),
                                reservedAt == null ? LocalDateTime.now() : reservedAt.toLocalDateTime()
                        ));
                    }
                }
            }
            return reservations;
        }
    }

    public Map<Integer, java.util.List<ReservationRecord>> getReservationsGroupedByEvent() throws SQLException {
        Map<Integer, java.util.List<ReservationRecord>> grouped = new LinkedHashMap<>();
        for (ReservationRecord reservation : getAllReservations()) {
            grouped.computeIfAbsent(reservation.getEventId(), ignored -> new java.util.ArrayList<>()).add(reservation);
        }
        return grouped;
    }

    public void deleteReservationsForEvent(int eventId) throws SQLException {
        String sql = "DELETE FROM reservation_event WHERE event_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, eventId);
            preparedStatement.executeUpdate();
        }
    }

    private void ensureColumnExists(Statement statement, String columnName, String alterSql) throws SQLException {
        try (ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM reservation_event LIKE '" + columnName + "'")) {
            if (!columns.next()) {
                statement.execute(alterSql);
            }
        }
    }

    private boolean columnExists(Statement statement, String columnName) throws SQLException {
        try (ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM reservation_event LIKE '" + columnName + "'")) {
            return columns.next();
        }
    }

    private String getTimestampColumn(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            if (columnExists(statement, "reserved_at")) {
                return "reserved_at";
            }
            if (columnExists(statement, "date_reservation")) {
                return "date_reservation";
            }
        }
        return "reserved_at";
    }

    private boolean hasColumn(Connection connection, String columnName) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            return columnExists(statement, columnName);
        }
    }

    public Map<String, Map<String, Integer>> getStatsByCategory() throws SQLException {
        String sql = """
                SELECT e.categorie,
                       COUNT(r.id) AS total_reservations,
                       SUM(e.capacite) AS total_capacity
                FROM event e
                LEFT JOIN reservation_event r ON e.id = r.event_id
                GROUP BY e.categorie
                ORDER BY total_reservations DESC
                """;
        Map<String, Map<String, Integer>> stats = new LinkedHashMap<>();

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                String categorie = resultSet.getString("categorie");
                int reservations = resultSet.getInt("total_reservations");
                int capacity = resultSet.getInt("total_capacity");
                categorie = categorie == null || categorie.isBlank() ? "Non catégorisé" : categorie;
                Map<String, Integer> data = new LinkedHashMap<>();
                data.put("reservations", reservations);
                data.put("capacity", capacity);
                stats.put(categorie, data);
            }
        }

        return stats;
    }

    public List<Map<String, Object>> getEventReservationStats() throws SQLException {
        String sql = """
                SELECT e.id,
                       e.titre,
                       e.categorie,
                       e.capacite,
                       COUNT(r.id) AS reservations
                FROM event e
                LEFT JOIN reservation_event r ON e.id = r.event_id
                GROUP BY e.id, e.titre, e.categorie, e.capacite
                ORDER BY e.titre
                """;
        List<Map<String, Object>> stats = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", resultSet.getInt("id"));
                row.put("titre", resultSet.getString("titre"));
                row.put("categorie", resultSet.getString("categorie"));
                row.put("capacite", resultSet.getInt("capacite"));
                row.put("reservations", resultSet.getInt("reservations"));
                row.put("taux", resultSet.getInt("capacite") > 0 
                    ? (int) (100.0 * resultSet.getInt("reservations") / resultSet.getInt("capacite")) 
                    : 0);
                stats.add(row);
            }
        }

        return stats;
    }

    public int getTotalReservations() throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM reservation_event";

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                return resultSet.getInt("total");
            }
        }

        return 0;
    }

    public Map<String, Integer> getReservationCountByCategory() throws SQLException {
        String sql = """
                SELECT COALESCE(e.categorie, '') AS categorie,
                       COUNT(r.id) AS total
                FROM event e
                LEFT JOIN reservation_event r ON e.id = r.event_id
                GROUP BY e.categorie
                ORDER BY total DESC
                """;
        Map<String, Integer> stats = new LinkedHashMap<>();

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                String categorie = resultSet.getString("categorie");
                int count = resultSet.getInt("total");
                stats.put(categorie, count);
            }
        }

        return stats;
    }

    public Map<String, Integer> getEventCountByCategory() throws SQLException {
        String sql = """
                SELECT COALESCE(categorie, '') AS categorie,
                       COUNT(*) AS total
                FROM event
                GROUP BY categorie
                ORDER BY total DESC
                """;
        Map<String, Integer> stats = new LinkedHashMap<>();

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                String categorie = resultSet.getString("categorie");
                int count = resultSet.getInt("total");
                stats.put(categorie, count);
            }
        }

        return stats;
    }
}
