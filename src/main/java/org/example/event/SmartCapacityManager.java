package org.example.event;

import org.example.config.DatabaseConnection;
import org.example.reservation.ReservationStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire de capacité dynamique pour les événements
 * Gère le surbooking contrôlé et les transitions de statuts de réservation
 */
public class SmartCapacityManager {

    /**
     * Obtient le nombre de places confirmées pour un événement
     */
    public int getConfirmedCount(int eventId) throws SQLException {
        // Count rows that are CONFIRMED or have NULL/empty status (legacy rows)
        String sql = """
                SELECT COUNT(*) as count
                FROM reservation_event
                WHERE event_id = ?
                  AND (status = ? OR status IS NULL OR status = '')
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setString(2, ReservationStatus.CONFIRMED.getValue());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        }
        return 0;
    }

    /**
     * Obtient le nombre total de réservations (confirmées + en attente) pour un événement
     */
    public int getTotalReservationCount(int eventId) throws SQLException {
        // Count CONFIRMED + WAITLISTED + legacy NULL rows
        String sql = """
                SELECT COUNT(*) as count
                FROM reservation_event
                WHERE event_id = ?
                  AND (status IN (?, ?) OR status IS NULL OR status = '')
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setString(2, ReservationStatus.CONFIRMED.getValue());
            stmt.setString(3, ReservationStatus.WAITLISTED.getValue());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        }
        return 0;
    }

    /**
     * Obtient le nombre de places en attente
     */
    public int getWaitlistCount(int eventId) throws SQLException {
        String sql = """
                SELECT COUNT(*) as count
                FROM reservation_event
                WHERE event_id = ? AND status = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setString(2, ReservationStatus.WAITLISTED.getValue());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        }
        return 0;
    }

    /**
     * Calcule la capacité maximale permise (y compris surbooking)
     */
    public int getMaxCapacity(int eventId) throws SQLException {
        Event event = getEvent(eventId);
        if (event == null) {
            return 0;
        }
        
        double overbookingPercentage = event.getOverbookingPercentage();
        return (int) Math.ceil(event.getCapacite() * (1 + overbookingPercentage / 100.0));
    }

    /**
     * Obtient la capacité réelle de l'événement
     */
    public int getActualCapacity(int eventId) throws SQLException {
        Event event = getEvent(eventId);
        return event != null ? event.getCapacite() : 0;
    }

    /**
     * Obtient le pourcentage de surbooking
     */
    public double getOverbookingPercentage(int eventId) throws SQLException {
        Event event = getEvent(eventId);
        return event != null ? event.getOverbookingPercentage() : 0;
    }

    /**
     * Vérifie si une place confirmée est disponible
     */
    public boolean isConfirmedSpaceAvailable(int eventId) throws SQLException {
        int confirmedCount = getConfirmedCount(eventId);
        int capacity = getActualCapacity(eventId);
        return confirmedCount < capacity;
    }

    /**
     * Vérifie si une place avec surbooking est disponible
     */
    public boolean isSpotsAvailable(int eventId) throws SQLException {
        int totalReservations = getTotalReservationCount(eventId);
        int maxCapacity = getMaxCapacity(eventId);
        return totalReservations < maxCapacity;
    }

    /**
     * Calcule le taux d'occupation (0-100)
     */
    public double getOccupancyRate(int eventId) throws SQLException {
        int confirmedCount = getConfirmedCount(eventId);
        int capacity = getActualCapacity(eventId);
        if (capacity == 0) {
            return 0;
        }
        return (confirmedCount * 100.0) / capacity;
    }

    /**
     * Calcule le taux d'occupation avec surbooking
     */
    public double getOccupancyRateWithOverbooking(int eventId) throws SQLException {
        int totalReservations = getTotalReservationCount(eventId);
        int maxCapacity = getMaxCapacity(eventId);
        if (maxCapacity == 0) {
            return 0;
        }
        return (totalReservations * 100.0) / maxCapacity;
    }

    /**
     * Obtient un résumé de la capacité de l'événement
     */
    public CapacitySummary getCapacitySummary(int eventId) throws SQLException {
        Event event = getEvent(eventId);
        if (event == null) {
            return null;
        }

        int confirmedCount = getConfirmedCount(eventId);
        int waitlistCount = getWaitlistCount(eventId);
        int capacity = event.getCapacite();
        double overbookingPercentage = event.getOverbookingPercentage();
        int maxCapacity = (int) Math.ceil(capacity * (1 + overbookingPercentage / 100.0));

        return new CapacitySummary(
                confirmedCount,
                waitlistCount,
                capacity,
                maxCapacity,
                overbookingPercentage,
                confirmedCount < capacity,
                confirmedCount + waitlistCount < maxCapacity
        );
    }

    /**
     * Récupère les données d'un événement
     */
    private Event getEvent(int eventId) throws SQLException {
        String sql = """
                SELECT id, capacite, overbooking_percentage
                FROM event
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Event event = new Event();
                    event.setId(rs.getInt("id"));
                    event.setCapacite(rs.getInt("capacite"));
                    event.setOverbookingPercentage(rs.getDouble("overbooking_percentage"));
                    return event;
                }
            }
        }
        return null;
    }

    /**
     * Classe interne pour résumer les informations de capacité
     */
    public static class CapacitySummary {
        public final int confirmedReservations;
        public final int waitlistedReservations;
        public final int actualCapacity;
        public final int maxCapacity;
        public final double overbookingPercentage;
        public final boolean confirmedSpaceAvailable;
        public final boolean spotsAvailable;

        public CapacitySummary(int confirmed, int waitlisted, int actual, int max,
                              double overbooking, boolean confirmedAvailable, boolean spotsAvailable) {
            this.confirmedReservations = confirmed;
            this.waitlistedReservations = waitlisted;
            this.actualCapacity = actual;
            this.maxCapacity = max;
            this.overbookingPercentage = overbooking;
            this.confirmedSpaceAvailable = confirmedAvailable;
            this.spotsAvailable = spotsAvailable;
        }

        public double getOccupancyRate() {
            if (actualCapacity == 0) return 0;
            return (confirmedReservations * 100.0) / actualCapacity;
        }

        public double getOccupancyRateWithOverbooking() {
            if (maxCapacity == 0) return 0;
            return ((confirmedReservations + waitlistedReservations) * 100.0) / maxCapacity;
        }

        @Override
        public String toString() {
            return "CapacitySummary{" +
                    "confirmées=" + confirmedReservations +
                    ", en attente=" + waitlistedReservations +
                    ", capacité réelle=" + actualCapacity +
                    ", capacité max=" + maxCapacity +
                    ", surbooking=" + overbookingPercentage + "%" +
                    ", occupation=" + String.format("%.1f%%", getOccupancyRate()) +
                    "}";
        }
    }
}
