package org.example.reservation;

/**
 * Enum représentant les statuts possibles d'une réservation
 * dans le système de gestion dynamique de capacité
 */
public enum ReservationStatus {
    CONFIRMED("CONFIRMED", "Réservation confirmée"),
    WAITLISTED("WAITLISTED", "En attente d'une place"),
    CANCELLED("CANCELLED", "Réservation annulée");

    private final String value;
    private final String description;

    ReservationStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Convertit une chaîne en enum ReservationStatus
     */
    public static ReservationStatus fromString(String status) {
        if (status == null || status.isBlank()) {
            return CONFIRMED; // Défaut
        }
        for (ReservationStatus rs : ReservationStatus.values()) {
            if (rs.value.equalsIgnoreCase(status.trim())) {
                return rs;
            }
        }
        return CONFIRMED; // Défaut si invalide
    }

    @Override
    public String toString() {
        return value;
    }
}
