package org.example.util;

import org.example.event.Event;
import org.example.event.EventService;
import org.example.event.SmartCapacityManager;
import org.example.reservation.ReservationService;
import org.example.reservation.ReservationStatus;
import org.example.reservation.WaitlistService;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur pour la Smart Capacity
 * Gère les interactions utilisateur avec la capacité dynamique
 */
public class SmartCapacityController {

    private final EventService eventService = new EventService();
    private final ReservationService reservationService = new ReservationService();
    private final SmartCapacityManager capacityManager = new SmartCapacityManager();
    private final WaitlistService waitlistService = new WaitlistService();

    // ============================================
    // RÉSERVATION
    // ============================================

    /**
     * Traite une demande de réservation utilisateur
     */
    public ReservationResponse handleReservationRequest(int eventId, int userId) {
        try {
            Event event = eventService.getEventById(eventId);
            if (event == null) {
                return new ReservationResponse(false, "Événement non trouvé");
            }

            ReservationService.ReservationResult result = 
                reservationService.reserveEvent(event, userId);

            if (result.status == ReservationStatus.CONFIRMED) {
                return new ReservationResponse(true, 
                    "✅ Réservation confirmée! Vous recevrez un email de confirmation.");
            } else if (result.status == ReservationStatus.WAITLISTED) {
                return new ReservationResponse(true,
                    "⏳ Vous êtes inscrit en liste d'attente à la position #" + 
                    result.waitlistPosition + ". Nous vous notifierons si une place se libère.");
            }
        } catch (SQLException e) {
            return new ReservationResponse(false, 
                "❌ Erreur: " + e.getMessage());
        }

        return new ReservationResponse(false, "Erreur inconnue");
    }

    /**
     * Traite une demande d'annulation
     */
    public ReservationResponse handleCancellation(int eventId, int userId) {
        try {
            if (reservationService.leaveEvent(eventId, userId)) {
                return new ReservationResponse(true, 
                    "✅ Votre réservation a été annulée.");
            } else {
                return new ReservationResponse(false, 
                    "❌ Aucune réservation trouvée pour cet événement.");
            }
        } catch (SQLException e) {
            return new ReservationResponse(false, 
                "❌ Erreur lors de l'annulation: " + e.getMessage());
        }
    }

    // ============================================
    // INFORMATIONS CAPACITÉ
    // ============================================

    /**
     * Obtient les infos de capacité pour l'interface utilisateur
     */
    public CapacityDisplayInfo getCapacityInfo(int eventId) {
        try {
            SmartCapacityManager.CapacitySummary summary = 
                capacityManager.getCapacitySummary(eventId);

            if (summary == null) {
                return null;
            }

            return new CapacityDisplayInfo(
                summary.actualCapacity,
                summary.maxCapacity,
                summary.confirmedReservations,
                summary.waitlistedReservations,
                summary.overbookingPercentage,
                summary.confirmedSpaceAvailable,
                summary.spotsAvailable,
                summary.getOccupancyRate()
            );
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Vérifie si des places confirmées sont disponibles
     */
    public boolean hasConfirmedSpaces(int eventId) {
        try {
            return capacityManager.isConfirmedSpaceAvailable(eventId);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Vérifie s'il y a des places (confirmées ou en attente)
     */
    public boolean hasAnySpaces(int eventId) {
        try {
            return capacityManager.isSpotsAvailable(eventId);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Obtient la position en liste d'attente de l'utilisateur
     */
    public Integer getUserWaitlistPosition(int eventId, int userId) {
        try {
            ReservationStatus status = 
                reservationService.getReservationStatus(
                    getReservationIdForUser(eventId, userId));

            if (status == ReservationStatus.WAITLISTED) {
                return waitlistService.getWaitlistPosition(eventId, userId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ============================================
    // ADMIN / STATISTIQUES
    // ============================================

    /**
     * Obtient un résumé complet pour l'admin
     */
    public EventAdminView getEventAdminView(int eventId) {
        try {
            Event event = eventService.getEventById(eventId);
            SmartCapacityManager.CapacitySummary summary = 
                capacityManager.getCapacitySummary(eventId);
            List<WaitlistService.WaitlistEntry> waitlist = 
                waitlistService.getWaitlist(eventId);

            return new EventAdminView(event, summary, waitlist);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Obtient les statistiques détaillées pour le dashboard
     */
    public List<Map<String, Object>> getDetailedStats() {
        try {
            return reservationService.getEventReservationStats();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Obtient les statistiques par catégorie
     */
    public Map<String, Map<String, Integer>> getStatsByCategory() {
        try {
            return reservationService.getStatsByCategory();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Exporte les résultats pour un événement
     */
    public EventExportData exportEventData(int eventId) {
        try {
            Event event = eventService.getEventById(eventId);
            List<Map<String, Object>> stats = getDetailedStats();
            List<WaitlistService.WaitlistEntry> waitlist = 
                waitlistService.getWaitlist(eventId);

            return new EventExportData(event, stats, waitlist);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ============================================
    // CLASSES INTERNES
    // ============================================

    /**
     * Réponse pour une opération de réservation
     */
    public static class ReservationResponse {
        public final boolean success;
        public final String message;

        public ReservationResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        @Override
        public String toString() {
            return (success ? "✅ " : "❌ ") + message;
        }
    }

    /**
     * Infos de capacité pour l'affichage
     */
    public static class CapacityDisplayInfo {
        public final int actualCapacity;
        public final int maxCapacity;
        public final int confirmedCount;
        public final int waitlistedCount;
        public final double overbookingPercentage;
        public final boolean confirmedSpaceAvailable;
        public final boolean totalSpaceAvailable;
        public final double occupancyRate;

        public CapacityDisplayInfo(int actualCapacity, int maxCapacity,
                                  int confirmedCount, int waitlistedCount,
                                  double overbookingPercentage,
                                  boolean confirmedSpaceAvailable,
                                  boolean totalSpaceAvailable,
                                  double occupancyRate) {
            this.actualCapacity = actualCapacity;
            this.maxCapacity = maxCapacity;
            this.confirmedCount = confirmedCount;
            this.waitlistedCount = waitlistedCount;
            this.overbookingPercentage = overbookingPercentage;
            this.confirmedSpaceAvailable = confirmedSpaceAvailable;
            this.totalSpaceAvailable = totalSpaceAvailable;
            this.occupancyRate = occupancyRate;
        }

        public String getStatusText() {
            if (!totalSpaceAvailable) {
                return "❌ Complet (file d'attente saturée)";
            }
            if (!confirmedSpaceAvailable) {
                return "⏳ Places confirmées pleines (liste d'attente active)";
            }
            return String.format("✅ %d places disponibles", 
                actualCapacity - confirmedCount);
        }

        public String getOccupancyText() {
            return String.format("%.1f%% occupation", occupancyRate);
        }

        @Override
        public String toString() {
            return String.format(
                "Capacité: %d/%d | Confirmées: %d | En attente: %d | Surbooking: %.0f%% | %s",
                confirmedCount, actualCapacity, confirmedCount, 
                waitlistedCount, overbookingPercentage, getStatusText()
            );
        }
    }

    /**
     * Vue admin pour un événement
     */
    public static class EventAdminView {
        public final Event event;
        public final SmartCapacityManager.CapacitySummary capacity;
        public final List<WaitlistService.WaitlistEntry> waitlist;

        public EventAdminView(Event event, 
                            SmartCapacityManager.CapacitySummary capacity,
                            List<WaitlistService.WaitlistEntry> waitlist) {
            this.event = event;
            this.capacity = capacity;
            this.waitlist = waitlist;
        }

        public String getTitle() {
            return event.getTitre();
        }

        public String getSummary() {
            return String.format(
                "%s - %d confirmées, %d en attente (capacité: %d + %d surbooking)",
                event.getTitre(),
                capacity.confirmedReservations,
                capacity.waitlistedReservations,
                capacity.actualCapacity,
                capacity.maxCapacity - capacity.actualCapacity
            );
        }
    }

    /**
     * Données d'export pour un événement
     */
    public static class EventExportData {
        public final Event event;
        public final List<Map<String, Object>> stats;
        public final List<WaitlistService.WaitlistEntry> waitlist;

        public EventExportData(Event event, 
                              List<Map<String, Object>> stats,
                              List<WaitlistService.WaitlistEntry> waitlist) {
            this.event = event;
            this.stats = stats;
            this.waitlist = waitlist;
        }
    }

    // ============================================
    // UTILITAIRES PRIVÉS
    // ============================================

    /**
     * Obtient l'ID de réservation pour un utilisateur et un événement
     */
    private int getReservationIdForUser(int eventId, int userId) throws SQLException {
        // À implémenter selon la structure du code
        // Pour l'instant, retourne -1 par défaut
        return -1;
    }
}
