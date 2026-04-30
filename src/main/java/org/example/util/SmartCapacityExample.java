package org.example.util;

import org.example.event.Event;
import org.example.event.EventService;
import org.example.event.SmartCapacityManager;
import org.example.reservation.ReservationService;
import org.example.reservation.ReservationStatus;
import org.example.reservation.WaitlistService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Exemple d'intégration complète de la Smart Capacity
 * Démontre tous les cas d'usage du système
 */
public class SmartCapacityExample {

    public static void main(String[] args) {
        try {
            // Initialiser les services
            EventService eventService = new EventService();
            ReservationService reservationService = new ReservationService();
            SmartCapacityManager capacityManager = new SmartCapacityManager();
            WaitlistService waitlistService = new WaitlistService();

            System.out.println("=== Smart Capacity Management - Démo ===\n");

            // 1. Créer un événement avec surbooking
            System.out.println("1️⃣  CRÉATION D'UN ÉVÉNEMENT AVEC SURBOOKING");
            System.out.println("-".repeat(50));

            Event event = new Event();
            event.setTitre("Concert Live 2024");
            event.setDescription("Un magnifique concert en live");
            event.setDateEvent(LocalDateTime.now().plusDays(7));
            event.setLieu("Palais Omnisports");
            event.setCapacite(100);  // Capacité réelle: 100
            event.setOverbookingPercentage(10);  // Surbooking: +10%
            event.setCategorie("Musique");

            eventService.createTableIfNotExists();
            eventService.addEvent(event);

            System.out.println("✅ Événement créé: " + event.getTitre());
            System.out.println("   Capacité réelle: " + event.getCapacite());
            System.out.println("   Surbooking: " + event.getOverbookingPercentage() + "%");
            System.out.println("   Capacité max: " + 
                Math.ceil(event.getCapacite() * (1 + event.getOverbookingPercentage() / 100.0)));
            System.out.println();

            // 2. Réserver jusqu'à saturation
            System.out.println("2️⃣  RÉSERVATIONS PROGRESSIVES (100 places)");
            System.out.println("-".repeat(50));

            // Remplir les 100 places confirmées
            for (int i = 1; i <= 100; i++) {
                ReservationService.ReservationResult result = 
                    reservationService.reserveEvent(event, i);
                
                if (i <= 5 || i >= 96) {  // Afficher premiers et derniers
                    System.out.printf("Utilisateur %3d: %s%n", i, result.status.getDescription());
                }
                if (i == 95) System.out.println("   ... (90 autres) ...");
            }
            System.out.println();

            // 3. Remplir la liste d'attente
            System.out.println("3️⃣  LISTE D'ATTENTE (10 places)");
            System.out.println("-".repeat(50));

            for (int i = 101; i <= 110; i++) {
                ReservationService.ReservationResult result = 
                    reservationService.reserveEvent(event, 1000 + i);
                System.out.printf("Utilisateur %4d: %s (Position #%d)%n", 
                    1000 + i, result.status.getDescription(), result.waitlistPosition);
            }
            System.out.println();

            // 4. Afficher la capacité
            System.out.println("4️⃣  RÉSUMÉ DE CAPACITÉ");
            System.out.println("-".repeat(50));

            SmartCapacityManager.CapacitySummary summary = 
                capacityManager.getCapacitySummary(event.getId());

            System.out.println("Réservations confirmées: " + summary.confirmedReservations);
            System.out.println("Réservations en attente: " + summary.waitlistedReservations);
            System.out.println("Capacité réelle: " + summary.actualCapacity);
            System.out.println("Capacité max (surbooking): " + summary.maxCapacity);
            System.out.println("Surbooking appliqué: " + summary.overbookingPercentage + "%");
            System.out.println("Taux d'occupation: " + 
                String.format("%.1f%%", summary.getOccupancyRate()));
            System.out.println("Taux d'occupation (avec surbooking): " + 
                String.format("%.1f%%", summary.getOccupancyRateWithOverbooking()));
            System.out.println();

            // 5. Afficher la liste d'attente
            System.out.println("5️⃣  LISTE D'ATTENTE COMPLÈTE");
            System.out.println("-".repeat(50));

            List<WaitlistService.WaitlistEntry> waitlist = waitlistService.getWaitlist(event.getId());
            for (WaitlistService.WaitlistEntry entry : waitlist) {
                System.out.println(entry);
            }
            System.out.println();

            // 6. Simuler une annulation et promotion
            System.out.println("6️⃣  ANNULATION ET PROMOTION AUTOMATIQUE");
            System.out.println("-".repeat(50));

            // Annuler l'utilisateur 50 (confirmé)
            System.out.println("❌ Utilisateur 50 annule sa réservation...");
            reservationService.leaveEvent(event.getId(), 50);

            // Vérifier le premier en attente
            System.out.println("✅ Premier en attente (1101) devrait être promu");
            ReservationStatus newStatus = 
                reservationService.getReservationStatus(1101);  // Premier en attente
            System.out.println("   Nouveau statut: " + newStatus.getDescription());
            System.out.println();

            // 7. Afficher les statistiques
            System.out.println("7️⃣  STATISTIQUES PAR CATÉGORIE");
            System.out.println("-".repeat(50));

            Map<String, Map<String, Integer>> stats = 
                reservationService.getStatsByCategory();

            for (String categorie : stats.keySet()) {
                Map<String, Integer> data = stats.get(categorie);
                System.out.println(categorie + ":");
                System.out.println("  Confirmées: " + data.get("confirmées"));
                System.out.println("  En attente: " + data.get("en_attente"));
                System.out.println("  Capacité totale: " + data.get("capacité_totale"));
            }
            System.out.println();

            // 8. Statistiques détaillées par événement
            System.out.println("8️⃣  STATISTIQUES DÉTAILLÉES");
            System.out.println("-".repeat(50));

            List<Map<String, Object>> eventStats = 
                reservationService.getEventReservationStats();

            for (Map<String, Object> row : eventStats) {
                System.out.println(row.get("titre") + ":");
                System.out.println("  Confirmées: " + row.get("confirmées"));
                System.out.println("  En attente: " + row.get("en_attente"));
                System.out.println("  Capacité réelle: " + row.get("capacité_réelle"));
                System.out.println("  Capacité max: " + row.get("capacité_max"));
                System.out.println("  Surbooking: " + row.get("surbooking_%") + "%");
                System.out.println("  Taux d'occupation: " + row.get("taux_occupation") + "%");
            }
            System.out.println();

            // 9. Résumé final
            System.out.println("9️⃣  RÉSUMÉ FINAL");
            System.out.println("-".repeat(50));
            System.out.println("Total confirmés: " + reservationService.getTotalReservations());
            System.out.println("Total en attente: " + reservationService.getTotalWaitlisted());
            System.out.println("Événements avec réservations: " + 
                reservationService.getEventReservationStats().size());
            System.out.println();

            System.out.println("✅ Démo terminée avec succès!");

        } catch (SQLException e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Exemple d'utilisation simple pour une application
     */
    public static void exempleSimple() throws SQLException {
        ReservationService reservationService = new ReservationService();
        SmartCapacityManager capacityManager = new SmartCapacityManager();

        // Créer un événement
        Event event = new Event();
        event.setTitre("Atelier Java");
        event.setCapacite(30);
        event.setOverbookingPercentage(20);

        // Essayer de réserver
        try {
            ReservationService.ReservationResult result = 
                reservationService.reserveEvent(event, 42);

            if (result.status == ReservationStatus.CONFIRMED) {
                System.out.println("✅ Vous êtes confirmé!");
            } else if (result.status == ReservationStatus.WAITLISTED) {
                System.out.println("⏳ Vous êtes #" + result.waitlistPosition + 
                    " en liste d'attente");
            }
        } catch (SQLException e) {
            System.out.println("❌ Impossible de réserver: " + e.getMessage());
        }

        // Vérifier la capacité
        SmartCapacityManager.CapacitySummary summary = 
            capacityManager.getCapacitySummary(event.getId());
        System.out.println("Occupation: " + 
            String.format("%.1f%%", summary.getOccupancyRate()));
    }
}
