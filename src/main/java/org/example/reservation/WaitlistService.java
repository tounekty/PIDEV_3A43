package org.example.reservation;

import org.example.config.DatabaseConnection;
import org.example.event.SmartCapacityManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de gestion de la liste d'attente
 * Gère automatiquement les transitions des utilisateurs en attente vers confirmés
 */
public class WaitlistService {
    private final SmartCapacityManager capacityManager = new SmartCapacityManager();

    /**
     * Ajoute une réservation à la liste d'attente
     */
    public int addToWaitlist(int eventId, int userId) throws SQLException {
        String sql = """
                INSERT INTO reservation_event(event_id, user_id, status, reserved_at)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, eventId);
            stmt.setInt(2, userId);
            stmt.setString(3, ReservationStatus.WAITLISTED.getValue());
            stmt.setTimestamp(4, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }
        return -1;
    }

    /**
     * Obtient le classement d'un utilisateur dans la liste d'attente
     */
    public int getWaitlistPosition(int eventId, int userId) throws SQLException {
        String sql = """
                SELECT COUNT(*) + 1 as position
                FROM reservation_event
                WHERE event_id = ? AND status = ? AND reserved_at < (
                    SELECT reserved_at FROM reservation_event
                    WHERE event_id = ? AND user_id = ? AND status = ?
                )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setString(2, ReservationStatus.WAITLISTED.getValue());
            stmt.setInt(3, eventId);
            stmt.setInt(4, userId);
            stmt.setString(5, ReservationStatus.WAITLISTED.getValue());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("position");
                }
            }
        }
        return -1;
    }

    /**
     * Obtient la liste des utilisateurs en attente (ordonnée par date)
     */
    public List<WaitlistEntry> getWaitlist(int eventId) throws SQLException {
        String sql = """
                SELECT r.id, r.user_id, r.reserved_at,
                       ROW_NUMBER() OVER (ORDER BY r.reserved_at) as position
                FROM reservation_event r
                WHERE r.event_id = ? AND r.status = ?
                ORDER BY r.reserved_at ASC
                """;

        List<WaitlistEntry> waitlist = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setString(2, ReservationStatus.WAITLISTED.getValue());

            try (ResultSet rs = stmt.executeQuery()) {
                int position = 1;
                while (rs.next()) {
                    waitlist.add(new WaitlistEntry(
                            rs.getInt("id"),
                            eventId,
                            rs.getInt("user_id"),
                            position++,
                            rs.getTimestamp("reserved_at").toLocalDateTime()
                    ));
                }
            }
        }

        return waitlist;
    }

    /**
     * Promeut automatiquement les premiers utilisateurs en attente
     * quand une place confirmée se libère
     */
    public int promoteFromWaitlist(int eventId) throws SQLException {
        SmartCapacityManager.CapacitySummary summary = capacityManager.getCapacitySummary(eventId);
        if (summary == null || !summary.confirmedSpaceAvailable) {
            return 0;
        }

        // Nombre de places à pourvoir
        int placesAvailable = summary.actualCapacity - summary.confirmedReservations;
        int promoted = 0;

        String selectSql = """
                SELECT id, user_id FROM reservation_event
                WHERE event_id = ? AND status = ?
                ORDER BY reserved_at ASC
                LIMIT ?
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            // Récupérer les utilisateurs à promouvoir
            List<Integer> toPromote = new ArrayList<>();
            try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
                selectStmt.setInt(1, eventId);
                selectStmt.setString(2, ReservationStatus.WAITLISTED.getValue());
                selectStmt.setInt(3, placesAvailable);

                try (ResultSet rs = selectStmt.executeQuery()) {
                    while (rs.next()) {
                        toPromote.add(rs.getInt("id"));
                    }
                }
            }

            // Promouvoir les utilisateurs sélectionnés
            String updateSql = """
                    UPDATE reservation_event
                    SET status = ?
                    WHERE id = ?
                    """;
            try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                for (Integer reservationId : toPromote) {
                    updateStmt.setString(1, ReservationStatus.CONFIRMED.getValue());
                    updateStmt.setInt(2, reservationId);
                    updateStmt.executeUpdate();
                    promoted++;
                }
            }
        }

        return promoted;
    }

    /**
     * Annule une réservation en attente et promeut le prochain
     */
    public boolean cancelWaitlistReservation(int eventId, int reservationId) throws SQLException {
        String cancelSql = """
                UPDATE reservation_event
                SET status = ?
                WHERE id = ? AND event_id = ?
                """;

        boolean cancelled;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(cancelSql)) {
            stmt.setString(1, ReservationStatus.CANCELLED.getValue());
            stmt.setInt(2, reservationId);
            stmt.setInt(3, eventId);
            cancelled = stmt.executeUpdate() > 0;
        }

        if (cancelled) {
            promoteFromWaitlist(eventId);
        }
        return cancelled;
    }

    /**
     * Convertit une réservation confirmée en attente (en cas de surbooking)
     */
    public boolean convertToWaitlist(int eventId, int reservationId) throws SQLException {
        String sql = """
                UPDATE reservation_event
                SET status = ?
                WHERE id = ? AND event_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ReservationStatus.WAITLISTED.getValue());
            stmt.setInt(2, reservationId);
            stmt.setInt(3, eventId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Classe interne pour représenter une entrée de liste d'attente
     */
    public static class WaitlistEntry {
        public final int id;
        public final int eventId;
        public final int userId;
        public final int position;
        public final LocalDateTime addedAt;

        public WaitlistEntry(int id, int eventId, int userId, int position, LocalDateTime addedAt) {
            this.id = id;
            this.eventId = eventId;
            this.userId = userId;
            this.position = position;
            this.addedAt = addedAt;
        }

        @Override
        public String toString() {
            return String.format("Attente #%d: Utilisateur %d (depuis %s)",
                    position, userId, addedAt);
        }
    }
}
