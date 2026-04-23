package org.example.util;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.example.event.Event;
import org.example.event.EventService;
import org.example.reservation.ReservationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Contrôleur de calendrier avancé avec fonctionnalités étendues
 * - Affichage des événements par date
 * - Statistiques de disponibilité
 * - Filtres par type d'événement
 * - Vue semaine/mois
 */
public class AdvancedCalendarController {
    private final EventService eventService;
    private final ReservationService reservationService;
    private CalendarPicker calendarPicker;
    private Label statsLabel;
    private Label selectedDateInfoLabel;
    private ComboBox<String> filterComboBox;
    private Consumer<LocalDate> onDateSelectedCallback;
    private Map<LocalDate, List<Event>> eventsByDate = new HashMap<>();

    public AdvancedCalendarController(EventService eventService, ReservationService reservationService) {
        this.eventService = eventService;
        this.reservationService = reservationService;
    }

    /**
     * Créer un panneau de calendrier complet avec fonctionnalités avancées
     */
    public VBox createAdvancedCalendarPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: #f9fbff; -fx-border-radius: 12; " +
                "-fx-border-color: #d7e7ff; -fx-border-width: 1;");

        // En-tête avec filtres
        HBox headerBox = createHeaderWithFilters();
        panel.getChildren().add(headerBox);

        // Calendrier principal
        calendarPicker = new CalendarPicker(this::onDateSelected);
        panel.getChildren().add(calendarPicker);

        // Panneau d'informations
        VBox infoPanel = createInfoPanel();
        panel.getChildren().add(infoPanel);

        // Charger les données initiales
        loadEventsData();

        return panel;
    }

    private HBox createHeaderWithFilters() {
        HBox header = new HBox(20);
        header.setPadding(new Insets(10));
        header.setStyle("-fx-background-color: linear-gradient(to right, #E8F4FF, #D1E3FF); " +
                "-fx-border-radius: 8; -fx-border-color: #cfe3ff;");

        // Titre
        Label titleLabel = new Label("📅 Calendrier Avancé des Événements");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        titleLabel.setStyle("-fx-text-fill: #1c4f96;");

        // Filtres
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Label filterLabel = new Label("Filtrer par:");
        filterLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");

        filterComboBox = new ComboBox<>();
        filterComboBox.getItems().addAll("Tous les événements", "Événements disponibles", 
                "Événements complets", "Événements à venir", "Événements passés");
        filterComboBox.setValue("Tous les événements");
        filterComboBox.setStyle("-fx-font-size: 12; -fx-pref-width: 180;");
        filterComboBox.setOnAction(e -> applyFilter());

        filterBox.getChildren().addAll(filterLabel, filterComboBox);

        // Espace flexible
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titleLabel, spacer, filterBox);
        return header;
    }

    private VBox createInfoPanel() {
        VBox infoPanel = new VBox(10);
        infoPanel.setPadding(new Insets(15));
        infoPanel.setStyle("-fx-background-color: white; -fx-border-radius: 8; " +
                "-fx-border-color: #e0e0e0; -fx-border-width: 1;");

        // Statistiques
        statsLabel = new Label();
        statsLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666; -fx-font-weight: bold;");

        // Informations sur la date sélectionnée
        selectedDateInfoLabel = new Label("Sélectionnez une date pour voir les détails");
        selectedDateInfoLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #1976D2; -fx-wrap-text: true;");

        // Boutons d'action
        HBox actionButtons = createActionButtons();

        infoPanel.getChildren().addAll(statsLabel, selectedDateInfoLabel, actionButtons);
        return infoPanel;
    }

    private HBox createActionButtons() {
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        Button todayButton = new Button("Aujourd'hui");
        todayButton.setStyle("-fx-font-size: 12; -fx-padding: 6 12; -fx-background-color: #4CAF50; " +
                "-fx-text-fill: white; -fx-border-radius: 4; -fx-cursor: hand;");
        todayButton.setOnAction(e -> calendarPicker.goToToday());

        Button clearButton = new Button("Effacer la sélection");
        clearButton.setStyle("-fx-font-size: 12; -fx-padding: 6 12; -fx-background-color: #FF9800; " +
                "-fx-text-fill: white; -fx-border-radius: 4; -fx-cursor: hand;");
        clearButton.setOnAction(e -> {
            calendarPicker.clearSelection();
            selectedDateInfoLabel.setText("Sélectionnez une date pour voir les détails");
        });

        Button refreshButton = new Button("Actualiser");
        refreshButton.setStyle("-fx-font-size: 12; -fx-padding: 6 12; -fx-background-color: #2196F3; " +
                "-fx-text-fill: white; -fx-border-radius: 4; -fx-cursor: hand;");
        refreshButton.setOnAction(e -> loadEventsData());

        buttonBox.getChildren().addAll(todayButton, clearButton, refreshButton);
        return buttonBox;
    }

    private void loadEventsData() {
        try {
            List<Event> events = eventService.getAllEvents();
            String selectedFilter = filterComboBox != null ? filterComboBox.getValue() : "Tous les événements";
            
            // Filtrer les événements basé sur le filtre sélectionné
            List<Event> filteredEvents = filterEventsByStatus(events, selectedFilter);
            
            eventsByDate.clear();
            Map<LocalDate, Integer> availabilityMap = new HashMap<>();

            int totalEvents = 0;
            int availableEvents = 0;
            int fullEvents = 0;

            for (Event event : filteredEvents) {
                LocalDate eventDate = event.getDateEvent().toLocalDate();
                
                // Grouper les événements par date
                eventsByDate.computeIfAbsent(eventDate, k -> new java.util.ArrayList<>()).add(event);
                
                int availableSpots = event.getCapacite() - reservationService.getReservationCountByEvent(event.getId());
                totalEvents++;

                // Déterminer la disponibilité
                int availability;
                if (availableSpots <= 0) {
                    availability = 2; // Complet
                    fullEvents++;
                } else if (availableSpots < event.getCapacite() * 0.25) {
                    availability = 0; // Peu d'options
                    availableEvents++;
                } else if (availableSpots < event.getCapacite()) {
                    availability = 1; // Beaucoup d'options
                    availableEvents++;
                } else {
                    availability = 3; // Disponible (100%)
                    availableEvents++;
                }

                // Utiliser le plus restrictif pour cette date (le PIRE état)
                if (availabilityMap.containsKey(eventDate)) {
                    availabilityMap.put(eventDate, Math.min(availabilityMap.get(eventDate), availability));
                } else {
                    availabilityMap.put(eventDate, availability);
                }
            }

            // Mettre à jour le calendrier
            calendarPicker.setDateAvailabilities(availabilityMap);

            // Mettre à jour les statistiques
            updateStats(totalEvents, availableEvents, fullEvents);

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des événements: " + e.getMessage());
            statsLabel.setText("❌ Erreur de chargement des données");
        }
    }

    private List<Event> filterEventsByStatus(List<Event> events, String filter) throws Exception {
        List<Event> result = new ArrayList<>();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        for (Event event : events) {
            boolean matches = false;
            
            switch (filter) {
                case "Événements disponibles" -> {
                    // Événements avec places disponibles
                    int availableSpots = event.getCapacite() - reservationService.getReservationCountByEvent(event.getId());
                    matches = availableSpots > 0;
                }
                case "Événements complets" -> {
                    // Événements SANS places disponibles (complets)
                    int availableSpots = event.getCapacite() - reservationService.getReservationCountByEvent(event.getId());
                    matches = availableSpots <= 0;
                }
                case "Événements à venir" -> {
                    // Événements futurs
                    matches = event.getDateEvent().isAfter(now);
                }
                case "Événements passés" -> {
                    // Événements passés
                    matches = event.getDateEvent().isBefore(now);
                }
                default -> matches = true; // "Tous les événements"
            }
            
            if (matches) {
                result.add(event);
            }
        }
        
        return result;
    }

    private void updateStats(int totalEvents, int availableEvents, int fullEvents) {
        String statsText = String.format(
                "📊 Statistiques: %d événements au total | %d avec places disponibles | %d complets",
                totalEvents, availableEvents, fullEvents
        );
        statsLabel.setText(statsText);
    }

    private void onDateSelected(LocalDate date) {
        // Mettre à jour les informations de la date sélectionnée
        updateDateInfo(date);
        
        // Appeler le callback si défini
        if (onDateSelectedCallback != null) {
            onDateSelectedCallback.accept(date);
        }
    }

    private void updateDateInfo(LocalDate date) {
        List<Event> events = eventsByDate.get(date);
        
        if (events == null || events.isEmpty()) {
            selectedDateInfoLabel.setText(
                    String.format("📅 %s: Aucun événement programmé", 
                            date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            );
            return;
        }

        StringBuilder info = new StringBuilder();
        info.append(String.format("📅 %s: %d événement(s)\n", 
                date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), events.size()));

        for (Event event : events) {
            int availableSpots;
            try {
                availableSpots = event.getCapacite() - reservationService.getReservationCountByEvent(event.getId());
            } catch (Exception e) {
                availableSpots = event.getCapacite(); // Par défaut, toutes les places sont disponibles
            }
            
            String status;
            if (availableSpots <= 0) {
                status = "🔴 COMPLET";
            } else if (availableSpots < 5) {
                status = "🟠 " + availableSpots + " place(s) restante(s)";
            } else {
                status = "🟢 " + availableSpots + " place(s) restante(s)";
            }
            
            info.append(String.format("• %s - %s (%s)\n", 
                    event.getTitre(), event.getLieu(), status));
        }

        selectedDateInfoLabel.setText(info.toString());
    }

    private void applyFilter() {
        String filter = filterComboBox.getValue();
        // Recharger les données avec le nouveau filtre appliqué
        loadEventsData();
    }

    /**
     * Définir un callback pour la sélection de date
     */
    public void setOnDateSelected(Consumer<LocalDate> callback) {
        this.onDateSelectedCallback = callback;
    }

    /**
     * Obtenir la date sélectionnée
     */
    public LocalDate getSelectedDate() {
        return calendarPicker.getSelectedDate();
    }

    /**
     * Obtenir le calendrier picker
     */
    public CalendarPicker getCalendarPicker() {
        return calendarPicker;
    }

    /**
     * Obtenir les événements pour une date spécifique
     */
    public List<Event> getEventsForDate(LocalDate date) {
        return eventsByDate.getOrDefault(date, java.util.Collections.emptyList());
    }
}