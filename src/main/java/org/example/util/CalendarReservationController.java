package org.example.util;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.event.Event;
import org.example.event.EventService;
import org.example.reservation.ReservationService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur pour intégrer le calendrier dans le dialogue de réservation
 */
public class CalendarReservationController {
    private final EventService eventService;
    private final ReservationService reservationService;
    private CalendarPicker calendarPicker;
    private Label selectedDateLabel;

    public CalendarReservationController(EventService eventService) {
        this.eventService = eventService;
        this.reservationService = new ReservationService();
    }

    public CalendarReservationController(EventService eventService, ReservationService reservationService) {
        this.eventService = eventService;
        this.reservationService = reservationService;
    }

    /**
     * Créer un panneau de sélection de date avec calendrier
     */
    public VBox createDateSelectionPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-background-color: #f9fbff; -fx-padding: 15;");

        // Titre
        Label titleLabel = new Label("📅 Sélectionnez une date");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #1976D2;");

        // Sous-titre
        Label subtitleLabel = new Label("Choisissez une date pour voir les événements disponibles");
        subtitleLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");

        // Bouton pour ouvrir le calendrier
        Button openCalendarButton = new Button("Ouvrir le calendrier");
        openCalendarButton.setStyle("-fx-background-color: linear-gradient(to right,#0f69ff,#38a4ff); " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20 10 20; " +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 12;");

        // Label pour afficher la date sélectionnée
        selectedDateLabel = new Label("Aucune date sélectionnée");
        selectedDateLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #1976D2; -fx-font-weight: bold;");

        openCalendarButton.setOnAction(e -> showCalendarDialog());

        // Conteneur pour le bouton et la date sélectionnée
        HBox buttonBox = new HBox(15);
        buttonBox.setStyle("-fx-alignment: center-left;");
        buttonBox.getChildren().addAll(openCalendarButton, new javafx.scene.control.Separator(), selectedDateLabel);

        panel.getChildren().addAll(titleLabel, subtitleLabel, buttonBox);
        return panel;
    }

    /**
     * Afficher le dialogue du calendrier
     */
    private void showCalendarDialog() {
        CalendarDialog dialog = new CalendarDialog();

        // Récupérer tous les événements pour déterminer la disponibilité
        try {
            List<Event> events = eventService.getAllEvents();
            Map<LocalDate, Integer> availabilityMap = new HashMap<>();

            // Analyser les événements pour chaque date
            for (Event event : events) {
                LocalDate eventDate = event.getDateEvent().toLocalDate();
                int availableSpots = event.getCapacite() - reservationService.getReservationCountByEvent(event.getId());

                // 0 = peu d'options (1-25% disponibles), 1 = beaucoup (25-99%), 2 = complet
                int availability;
                if (availableSpots <= 0) {
                    availability = 2; // Complet
                } else if (availableSpots < event.getCapacite() * 0.25) {
                    availability = 0; // Peu d'options
                } else {
                    availability = 1; // Beaucoup d'options
                }

                // Utiliser le plus restrictif pour cette date
                if (availabilityMap.containsKey(eventDate)) {
                    availabilityMap.put(eventDate, Math.max(availabilityMap.get(eventDate), availability));
                } else {
                    availabilityMap.put(eventDate, availability);
                }
            }

            dialog.setDateAvailabilities(availabilityMap);

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des événements: " + e.getMessage());
        }

        // Afficher le dialogue
        var result = dialog.showAndWait();
        if (result.isPresent()) {
            LocalDate selectedDate = result.get();
            selectedDateLabel.setText("Date sélectionnée: " + selectedDate.format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            // Déclencher une action personnalisée si nécessaire
            onDateSelected(selectedDate);
        }
    }

    /**
     * Appelé quand une date est sélectionnée
     * À surcharger ou utiliser via un callback
     */
    protected void onDateSelected(LocalDate date) {
        System.out.println("Date sélectionnée: " + date);
    }

    /**
     * Obtenir la date sélectionnée actuellement
     */
    public LocalDate getSelectedDate() {
        if (calendarPicker != null) {
            return calendarPicker.getSelectedDate();
        }
        return null;
    }
}
