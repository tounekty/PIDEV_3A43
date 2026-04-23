package org.example.util;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.VBox;
import org.example.event.Event;
import org.example.event.EventService;
import org.example.auth.AppUser;
import org.example.reservation.ReservationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Contrôleur pour l'interface de réservation avec calendrier à deux mois
 * Intègre le DualMonthCalendarView dans le flux de réservation
 */
public class DualMonthReservationController {
    private EventService eventService;
    private ReservationService reservationService;
    
    @FXML
    private VBox calendarContainer;
    @FXML
    private ComboBox<Event> eventComboBox;
    @FXML
    private Label eventDateLabel;
    @FXML
    private Label eventLocationLabel;
    @FXML
    private Label eventCapacityLabel;
    @FXML
    private Label eventAvailableLabel;
    @FXML
    private ComboBox<AppUser> userComboBox;
    @FXML
    private Spinner<Integer> quantitySpinner;
    
    private DualMonthCalendarView dualCalendarView;
    private LocalDate selectedDate;

    public void initialize() {
        this.eventService = new EventService();
        this.reservationService = new ReservationService();
        
        initializeCalendar();
        loadEvents();
        loadUsers();
    }

    private void initializeCalendar() {
        // Créer et ajouter le calendrier à deux mois
        dualCalendarView = new DualMonthCalendarView(eventService, reservationService, this::onDateSelected);
        calendarContainer.getChildren().add(dualCalendarView);
    }

    private void onDateSelected(LocalDate date) {
        this.selectedDate = date;
        
        // Charger les événements pour cette date
        try {
            List<Event> eventsOnDate = eventService.getAllEvents().stream()
                    .filter(e -> e.getDateEvent().toLocalDate().equals(date))
                    .collect(Collectors.toList());
            
            eventComboBox.getItems().clear();
            eventComboBox.getItems().addAll(eventsOnDate);
            
            if (!eventsOnDate.isEmpty()) {
                eventComboBox.setValue(eventsOnDate.get(0));
                updateEventDetails();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des événements: " + e.getMessage());
        }
    }

    private void loadEvents() {
        try {
            List<Event> allEvents = eventService.getAllEvents();
            eventComboBox.getItems().addAll(allEvents);
            
            if (!allEvents.isEmpty()) {
                eventComboBox.setValue(allEvents.get(0));
                updateEventDetails();
            }
            
            // Ajouter un listener pour les changements
            eventComboBox.setOnAction(e -> updateEventDetails());
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des événements: " + e.getMessage());
        }
    }

    private void updateEventDetails() {
        Event selectedEvent = eventComboBox.getValue();
        if (selectedEvent != null) {
            eventDateLabel.setText(String.format("%s", selectedEvent.getDateEvent()));
            eventLocationLabel.setText(selectedEvent.getLieu());
            eventCapacityLabel.setText(String.format("%d places", selectedEvent.getCapacite()));
            
            try {
                int reserved = reservationService.getReservationCountByEvent(selectedEvent.getId());
                int available = selectedEvent.getCapacite() - reserved;
                eventAvailableLabel.setText(String.format("%d places disponibles", available));
                
                if (available > 0) {
                    eventAvailableLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                } else {
                    eventAvailableLabel.setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;");
                }
            } catch (Exception e) {
                eventAvailableLabel.setText("Erreur");
            }
        }
    }

    private void loadUsers() {
        try {
            // Récupérer tous les utilisateurs (à adapter selon votre structure)
            // userComboBox.getItems().addAll(userService.getAllUsers());
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des utilisateurs: " + e.getMessage());
        }
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public Event getSelectedEvent() {
        return eventComboBox.getValue();
    }

    public AppUser getSelectedUser() {
        return userComboBox.getValue();
    }

    public int getQuantity() {
        return quantitySpinner.getValue();
    }

    @FXML
    private void handleReservation() {
        if (selectedDate == null || eventComboBox.getValue() == null || userComboBox.getValue() == null) {
            System.out.println("Veuillez sélectionner une date, un événement et un utilisateur");
            return;
        }

        try {
            Event event = eventComboBox.getValue();
            AppUser user = userComboBox.getValue();
            int quantity = quantitySpinner.getValue();

            // Vérifier la disponibilité
            int reserved = reservationService.getReservationCountByEvent(event.getId());
            if (reserved + quantity > event.getCapacite()) {
                System.out.println("Pas assez de places disponibles");
                return;
            }

            // Effectuer la réservation (à adapter selon votre logique)
            System.out.println(String.format("Réservation effectuée pour %s le %s", user.getUsername(), selectedDate));
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la réservation: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        System.out.println("Réservation annulée");
    }
}
