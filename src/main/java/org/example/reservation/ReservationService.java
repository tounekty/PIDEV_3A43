package org.example.reservation;

import org.example.config.DatabaseConnection;
import org.example.event.Event;
import org.example.event.SmartCapacityManager;
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

/**
 * Service de gestion des réservations avec support de la capacité dynamique
 * Gère les réservations confirmées et en liste d'attente
 */
public class ReservationService {
    private final SmartCapacityManager capacityManager = new SmartCapacityManager();
    private final WaitlistService waitlistService = new WaitlistService();

    public void initializeReservations() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS reservation_event (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    event_id INT NOT NULL,
                    user_id INT NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
                    reserved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    seat_category VARCHAR(50) DEFAULT 'STANDARD',
                    price DECIMAL(10, 2) DEFAULT 0.00
                )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            ensureColumnExists(statement, "event_id", "ALTER TABLE reservation_event ADD COLUMN event_id INT NOT NULL");
            ensureColumnExists(statement, "user_id", "ALTER TABLE reservation_event ADD COLUMN user_id INT NOT NULL");
            ensureColumnExists(statement, "status", "ALTER TABLE reservation_event ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED'");
            ensureColumnExists(statement, "reserved_at", "ALTER TABLE reservation_event ADD COLUMN reserved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
            ensureColumnExists(statement, "seat_category", "ALTER TABLE reservation_event ADD COLUMN seat_category VARCHAR(50) DEFAULT 'STANDARD'");
            ensureColumnExists(statement, "price", "ALTER TABLE reservation_event ADD COLUMN price DECIMAL(10, 2) DEFAULT 0.00");

            // Migrate legacy rows: set NULL or empty status to CONFIRMED
            statement.execute(
                "UPDATE reservation_event SET status = 'CONFIRMED' " +
                "WHERE status IS NULL OR status = '' OR status NOT IN ('CONFIRMED','WAITLISTED','CANCELLED')"
            );

            // Nettoyer les anciennes colonnes non utilisées
            cleanupOldColumns(statement);
        }
    }

    /**
     * Réserve un événement avec gestion automatique de la liste d'attente
     */
    public ReservationResult reserveEvent(Event event, int userId) throws SQLException {
        return reserveEventWithDetails(event, userId, null, null);
    }

    /**
     * Réserve un événement avec détails supplémentaires (catégorie de siège, prix)
     */
    public ReservationResult reserveEventWithDetails(Event event, int userId, String seatCategory, Double price) throws SQLException {
        // Validation des données
        try {
            ValidationUtil.validateReservation(event.getId(), userId);
        } catch (IllegalArgumentException e) {
            throw new SQLException("Erreur de validation : " + e.getMessage(), e);
        }

        // Vérifier si l'utilisateur a déjà une réservation active (confirmée ou en attente)
        if (hasActiveReservation(event.getId(), userId)) {
            throw new SQLException("Vous avez déjà réservé cet événement.");
        }

        SmartCapacityManager.CapacitySummary summary = capacityManager.getCapacitySummary(event.getId());
        if (summary == null) {
            throw new SQLException("Événement non trouvé");
        }

        // Déterminer le statut de la réservation
        if (summary.confirmedSpaceAvailable) {
            // Ajouter comme confirmé (gère aussi la réactivation d'une réservation annulée)
            return confirmReservationWithDetails(event.getId(), userId, seatCategory, price);
        } else if (summary.spotsAvailable) {
            // Ajouter à la liste d'attente
            return addToWaitlist(event.getId(), userId);
        } else {
            throw new SQLException("Cet événement est complet et la liste d'attente est saturée.");
        }
    }

    /**
     * Ajoute une réservation confirmée avec détails supplémentaires
     */
    private ReservationResult confirmReservationWithDetails(int eventId, int userId, String seatCategory, Double price) throws SQLException {
        // Utiliser INSERT ... ON DUPLICATE KEY UPDATE pour gérer le cas
        // où une réservation annulée existe déjà (contrainte UNIQUE event_id+user_id)
        String sql = """
                INSERT INTO reservation_event(event_id, user_id, status, reserved_at, seat_category, price)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    status      = VALUES(status),
                    reserved_at = VALUES(reserved_at),
                    seat_category = VALUES(seat_category),
                    price       = VALUES(price)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, eventId);
            stmt.setInt(2, userId);
            stmt.setString(3, ReservationStatus.CONFIRMED.getValue());
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(5, seatCategory != null ? seatCategory : "STANDARD");
            stmt.setDouble(6, price != null ? price : 0.0);
            stmt.executeUpdate();

            // Récupérer l'ID (INSERT ou UPDATE)
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new ReservationResult(generatedKeys.getInt(1), ReservationStatus.CONFIRMED, null);
                }
            }
            // Si ON DUPLICATE KEY UPDATE, récupérer l'ID existant
            String selectSql = "SELECT id FROM reservation_event WHERE event_id = ? AND user_id = ?";
            try (PreparedStatement sel = connection.prepareStatement(selectSql)) {
                sel.setInt(1, eventId);
                sel.setInt(2, userId);
                try (ResultSet rs = sel.executeQuery()) {
                    if (rs.next()) {
                        return new ReservationResult(rs.getInt("id"), ReservationStatus.CONFIRMED, null);
                    }
                }
            }
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            throw new SQLException("Vous avez déjà une réservation pour cet événement.", e);
        }
        return null;
    }

    /**
     * Ajoute une réservation à la liste d'attente
     */
    private ReservationResult addToWaitlist(int eventId, int userId) throws SQLException {
        int reservationId = waitlistService.addToWaitlist(eventId, userId);
        int position = waitlistService.getWaitlistPosition(eventId, userId);
        return new ReservationResult(reservationId, ReservationStatus.WAITLISTED, position);
    }

    /**
     * Annule une réservation et promeut automatiquement depuis la liste d'attente
     */
    public boolean cancelReservation(int eventId, int reservationId, int userId) throws SQLException {
        ReservationStatus status = getReservationStatus(reservationId);
        
        if (status == ReservationStatus.WAITLISTED) {
            return waitlistService.cancelWaitlistReservation(eventId, reservationId);
        } else if (status == ReservationStatus.CONFIRMED) {
            return cancelConfirmedReservation(eventId, reservationId);
        }
        return false;
    }

    /**
     * Annule une réservation confirmée et promeut depuis la liste d'attente
     */
    private boolean cancelConfirmedReservation(int eventId, int reservationId) throws SQLException {
        String sql = """
                UPDATE reservation_event
                SET status = ?
                WHERE id = ? AND event_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ReservationStatus.CANCELLED.getValue());
            stmt.setInt(2, reservationId);
            stmt.setInt(3, eventId);
            boolean cancelled = stmt.executeUpdate() > 0;
            
            if (cancelled) {
                // Promouvoir depuis la liste d'attente
                waitlistService.promoteFromWaitlist(eventId);
            }
            return cancelled;
        }
    }

    /**
     * Quitter un événement (alias pour annuler une réservation active)
     */
    public boolean leaveEvent(int eventId, int userId) throws SQLException {
        // Trouver la réservation active (confirmée ou en attente)
        String sql = """
                SELECT id FROM reservation_event 
                WHERE event_id = ? AND user_id = ? AND status IN (?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setInt(2, userId);
            stmt.setString(3, ReservationStatus.CONFIRMED.getValue());
            stmt.setString(4, ReservationStatus.WAITLISTED.getValue());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int reservationId = rs.getInt("id");
                    return cancelReservation(eventId, reservationId, userId);
                }
            }
        }
        return false;
    }

    /**
     * Vérifie si l'utilisateur a une réservation active
     */
    public boolean hasActiveReservation(int eventId, int userId) throws SQLException {
        String sql = """
                SELECT id FROM reservation_event 
                WHERE event_id = ? AND user_id = ? AND status IN (?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setInt(2, userId);
            stmt.setString(3, ReservationStatus.CONFIRMED.getValue());
            stmt.setString(4, ReservationStatus.WAITLISTED.getValue());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Vérifie si une entrée existe dans reservation_event (tous statuts confondus).
     * Utilisé pour éviter les doublons sur la contrainte UNIQUE.
     */
    private boolean hasAnyReservationEntry(int eventId, int userId) throws SQLException {
        String sql = "SELECT id FROM reservation_event WHERE event_id = ? AND user_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Obtient le statut d'une réservation
     */
    public ReservationStatus getReservationStatus(int reservationId) throws SQLException {
        String sql = "SELECT status FROM reservation_event WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, reservationId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return ReservationStatus.fromString(rs.getString("status"));
                }
            }
        }
        return null;
    }

    /**
     * Récupère tous les détails d'une réservation incluant seat_category et price
     */
    public Map<String, Object> getReservationDetails(int reservationId) throws SQLException {
        String sql = """
                SELECT id, event_id, user_id, status, reserved_at, seat_category, price
                FROM reservation_event WHERE id = ?
                """;
        
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, reservationId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> details = new java.util.HashMap<>();
                    details.put("id", rs.getInt("id"));
                    details.put("eventId", rs.getInt("event_id"));
                    details.put("userId", rs.getInt("user_id"));
                    details.put("status", rs.getString("status"));
                    details.put("reservedAt", rs.getTimestamp("reserved_at"));
                    details.put("seatCategory", rs.getString("seat_category"));
                    details.put("price", rs.getDouble("price"));
                    return details;
                }
            }
        }
        return null;
    }

    /**
     * Obtient le nombre total de réservations (confirmées + en attente) pour un événement
     */
    public int getReservationCountByEvent(int eventId) throws SQLException {
        return capacityManager.getTotalReservationCount(eventId);
    }

    /**
     * Obtient le nombre de réservations confirmées pour un événement
     */
    public int getConfirmedCountByEvent(int eventId) throws SQLException {
        return capacityManager.getConfirmedCount(eventId);
    }

    /**
     * Obtient les IDs des événements réservés par un utilisateur (confirmés seulement)
     */
    public Set<Integer> getReservedEventIdsByUser(int userId) throws SQLException {
        String sql = """
                SELECT DISTINCT event_id FROM reservation_event 
                WHERE user_id = ? AND status = ?
                """;
        Set<Integer> reservedIds = new HashSet<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, ReservationStatus.CONFIRMED.getValue());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservedIds.add(rs.getInt("event_id"));
                }
            }
        }

        return reservedIds;
    }

    /**
     * Récupère tous les enregistrements de réservation
     */
    public List<ReservationRecord> getAllReservations() throws SQLException {
        String sql = """
                SELECT r.id, r.event_id, r.user_id, r.status, r.reserved_at,
                       e.titre AS event_title,
                       u.username
                FROM reservation_event r
                LEFT JOIN event e ON e.id = r.event_id
                LEFT JOIN users u ON u.id = r.user_id
                WHERE r.status IN (?, ?)
                ORDER BY r.reserved_at DESC
                """;

        List<ReservationRecord> reservations = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ReservationStatus.CONFIRMED.getValue());
            stmt.setString(2, ReservationStatus.WAITLISTED.getValue());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp reservedAt = rs.getTimestamp("reserved_at");
                    reservations.add(new ReservationRecord(
                            rs.getInt("id"),
                            rs.getInt("event_id"),
                            rs.getInt("user_id"),
                            rs.getString("event_title"),
                            rs.getString("username"),
                            reservedAt == null ? LocalDateTime.now() : reservedAt.toLocalDateTime(),
                            rs.getString("status")
                    ));
                }
            }
        }

        return reservations;
    }

    /**
     * Récupère les participants confirmés d'un événement
     */
    public List<ReservationRecord> getParticipantsByEvent(int eventId) throws SQLException {
        String sql = """
                SELECT r.id, r.event_id, r.user_id, r.status, r.reserved_at,
                       e.titre AS event_title,
                       u.username
                FROM reservation_event r
                LEFT JOIN event e ON e.id = r.event_id
                LEFT JOIN users u ON u.id = r.user_id
                WHERE r.event_id = ? AND r.status = ?
                ORDER BY r.reserved_at ASC
                """;

        List<ReservationRecord> participants = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setString(2, ReservationStatus.CONFIRMED.getValue());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp reservedAt = rs.getTimestamp("reserved_at");
                    participants.add(new ReservationRecord(
                            rs.getInt("id"),
                            rs.getInt("event_id"),
                            rs.getInt("user_id"),
                            rs.getString("event_title"),
                            rs.getString("username"),
                            reservedAt == null ? LocalDateTime.now() : reservedAt.toLocalDateTime(),
                            rs.getString("status")
                    ));
                }
            }
        }

        return participants;
    }

    /**
     * Récupère la liste d'attente d'un événement
     */
    public List<WaitlistService.WaitlistEntry> getWaitlistByEvent(int eventId) throws SQLException {
        return waitlistService.getWaitlist(eventId);
    }

    /**
     * Récupère les réservations groupées par événement
     */
    public Map<Integer, List<ReservationRecord>> getReservationsGroupedByEvent() throws SQLException {
        Map<Integer, List<ReservationRecord>> grouped = new LinkedHashMap<>();
        for (ReservationRecord reservation : getAllReservations()) {
            grouped.computeIfAbsent(reservation.getEventId(), ignored -> new ArrayList<>()).add(reservation);
        }
        return grouped;
    }

    /**
     * Supprime toutes les réservations d'un événement
     */
    public void deleteReservationsForEvent(int eventId) throws SQLException {
        String sql = "DELETE FROM reservation_event WHERE event_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.executeUpdate();
        }
    }

    /**
     * Obtient les statistiques par catégorie
     */
    public Map<String, Map<String, Integer>> getStatsByCategory() throws SQLException {
        String sql = """
                SELECT e.categorie,
                       COUNT(CASE WHEN r.status = 'CONFIRMED' THEN 1 END) AS confirmed,
                       COUNT(CASE WHEN r.status = 'WAITLISTED' THEN 1 END) AS waitlisted,
                       SUM(e.capacite) AS total_capacity
                FROM event e
                LEFT JOIN reservation_event r ON e.id = r.event_id
                GROUP BY e.categorie
                ORDER BY confirmed DESC
                """;
        Map<String, Map<String, Integer>> stats = new LinkedHashMap<>();

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                String categorie = resultSet.getString("categorie");
                categorie = categorie == null || categorie.isBlank() ? "Non catégorisé" : categorie;
                Map<String, Integer> data = new LinkedHashMap<>();
                data.put("confirmées", resultSet.getInt("confirmed"));
                data.put("en_attente", resultSet.getInt("waitlisted"));
                data.put("capacité_totale", resultSet.getInt("total_capacity"));
                stats.put(categorie, data);
            }
        }

        return stats;
    }

    /**
     * Obtient le nombre de réservations par catégorie
     */
    public Map<String, Integer> getReservationCountByCategory() throws SQLException {
        String sql = """
                        SELECT e.categorie, COUNT(r.id) as count FROM event e
                        LEFT JOIN reservation_event r ON e.id = r.event_id AND r.status = 'CONFIRMED'
                        GROUP BY e.categorie ORDER BY count DESC
                        """;
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                String categorie = resultSet.getString("categorie");
                categorie = categorie == null || categorie.isBlank() ? "Non catégorisé" : categorie;
                counts.put(categorie, resultSet.getInt("count"));
            }
        }
        return counts;
    }

    /**
     * Obtient le nombre d'événements par catégorie
     */
    public Map<String, Integer> getEventCountByCategory() throws SQLException {
        String sql = "SELECT categorie, COUNT(*) as count FROM event GROUP BY categorie ORDER BY count DESC";
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                String categorie = resultSet.getString("categorie");
                categorie = categorie == null || categorie.isBlank() ? "Non catégorisé" : categorie;
                counts.put(categorie, resultSet.getInt("count"));
            }
        }
        return counts;
    }

    /**
     * Obtient le nombre de réservations par événement
     */
    public Map<Integer, Integer> getReservationCountsByEvent() throws SQLException {
        String sql = "SELECT event_id, COUNT(*) as count FROM reservation_event WHERE status = 'CONFIRMED' GROUP BY event_id";
        Map<Integer, Integer> counts = new HashMap<>();
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                counts.put(resultSet.getInt("event_id"), resultSet.getInt("count"));
            }
        }
        return counts;
    }

    /**
     * Obtient les statistiques détaillées des réservations par événement
     */
    public List<Map<String, Object>> getEventReservationStats() throws SQLException {
        String sql = """
                SELECT e.id,
                       e.titre,
                       e.categorie,
                       e.capacite,
                       e.overbooking_percentage,
                       COUNT(CASE WHEN r.status = 'CONFIRMED' THEN 1 END) AS confirmed_count,
                       COUNT(CASE WHEN r.status = 'WAITLISTED' THEN 1 END) AS waitlist_count
                FROM event e
                LEFT JOIN reservation_event r ON e.id = r.event_id
                GROUP BY e.id, e.titre, e.categorie, e.capacite, e.overbooking_percentage
                ORDER BY e.titre
                """;
        List<Map<String, Object>> stats = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                int capacity = resultSet.getInt("capacite");
                double overbooking = resultSet.getDouble("overbooking_percentage");
                int confirmed = resultSet.getInt("confirmed_count");
                int waitlisted = resultSet.getInt("waitlist_count");
                int maxCapacity = (int) Math.ceil(capacity * (1 + overbooking / 100.0));

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", resultSet.getInt("id"));
                row.put("titre", resultSet.getString("titre"));
                row.put("categorie", resultSet.getString("categorie"));
                row.put("capacité_réelle", capacity);
                row.put("capacité_max", maxCapacity);
                row.put("surbooking_%", overbooking);
                row.put("confirmées", confirmed);
                row.put("en_attente", waitlisted);
                row.put("taux_occupation", capacity > 0 ? (int) (100.0 * confirmed / capacity) : 0);
                stats.add(row);
            }
        }

        return stats;
    }

    /**
     * Obtient le nombre total de réservations confirmées
     */
    public int getTotalReservations() throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM reservation_event WHERE status = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ReservationStatus.CONFIRMED.getValue());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }

        return 0;
    }

    /**
     * Obtient le nombre total d'entrées en liste d'attente
     */
    public int getTotalWaitlisted() throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM reservation_event WHERE status = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ReservationStatus.WAITLISTED.getValue());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }

        return 0;
    }

    // Méthodes utilitaires privées
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

    private void cleanupOldColumns(Statement statement) throws SQLException {
        String[] oldColumns = {"statut", "nom", "prenom", "telephone", "id_event", "id_user", "date_reservation"};
        for (String column : oldColumns) {
            try {
                if (columnExists(statement, column)) {
                    statement.execute("ALTER TABLE reservation_event MODIFY COLUMN " + column + " VARCHAR(255) NULL");
                }
            } catch (SQLException e) {
                // Colonne déjà supprimée ou ignorée
            }
        }
    }

    /**
     * Classe interne pour représenter le résultat d'une réservation
     */
    public static class ReservationResult {
        public final int reservationId;
        public final ReservationStatus status;
        public final Integer waitlistPosition; // null si confirmé

        public ReservationResult(int reservationId, ReservationStatus status, Integer waitlistPosition) {
            this.reservationId = reservationId;
            this.status = status;
            this.waitlistPosition = waitlistPosition;
        }

        @Override
        public String toString() {
            if (status == ReservationStatus.CONFIRMED) {
                return "Réservation confirmée (ID: " + reservationId + ")";
            } else {
                return "Ajouté à la liste d'attente (Position: " + waitlistPosition + ")";
            }
        }
    }
}
