package org.example.util;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.example.event.EventService;
import org.example.reservation.ReservationService;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Contrôleur pour le dialogue d'ajout de réservation avec calendrier intégré
 */
public class AddReservationController implements Initializable {
    
    @FXML
    private VBox calendarContainer;
    
    @FXML
    private ComboBox<String> eventComboBox;
    
    @FXML
    private Label eventDateLabel;
    
    @FXML
    private Label eventLocationLabel;
    
    @FXML
    private Label eventCapacityLabel;
    
    @FXML
    private Label eventAvailableLabel;
    
    @FXML
    private ComboBox<String> userComboBox;
    
    @FXML
    private Spinner<Integer> quantitySpinner;
    
    @FXML
    private Button cancelButton;
    
    @FXML
    private Button confirmButton;
    
    private AdvancedCalendarController advancedCalendarController;
    private EventService eventService;
    private ReservationService reservationService;
    private LocalDate selectedDate;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser les services
        eventService = new EventService();
        reservationService = new ReservationService();
        
        // Initialiser le calendrier
        initializeCalendar();
        
        // Initialiser les autres composants
        initializeComponents();
        
        // Configurer les boutons
        setupButtons();
    }
    
    private void initializeCalendar() {
        // Créer le contrôleur de calendrier avancé
        advancedCalendarController = new AdvancedCalendarController(
            eventService, reservationService
        );
        
        // Définir le callback pour la sélection de date
        advancedCalendarController.setOnDateSelected(this::onDateSelected);
        
        // Obtenir le panneau de calendrier et l'ajouter au conteneur
        VBox calendarPanel = advancedCalendarController.createAdvancedCalendarPanel();
        calendarContainer.getChildren().add(calendarPanel);
    }
    
    private void initializeComponents() {
        // Configurer le Spinner pour le nombre de places
        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        
        // Initialiser les ComboBox (pour l'instant vides)
        // En réalité, vous chargeriez les données depuis la base de données
        eventComboBox.getItems().addAll("Événement 1", "Événement 2", "Événement 3");
        userComboBox.getItems().addAll("Utilisateur 1", "Utilisateur 2", "Utilisateur 3");
        
        // Écouter les changements dans la sélection d'événement
        eventComboBox.setOnAction(e -> updateEventDetails());
    }
    
    private void setupButtons() {
        // Bouton Annuler
        cancelButton.setOnAction(e -> {
            // Fermer la fenêtre ou réinitialiser le formulaire
            System.out.println("Réservation annulée");
            clearForm();
        });
        
        // Bouton Confirmer
        confirmButton.setOnAction(e -> {
            if (validateForm()) {
                confirmReservation();
            }
        });
    }
    
    private void onDateSelected(LocalDate date) {
        this.selectedDate = date;
        
        if (date != null) {
            // Mettre à jour l'interface avec la date sélectionnée
            updateEventListForDate(date);
            
            // Formater et afficher la date
            String formattedDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            System.out.println("Date sélectionnée pour réservation: " + formattedDate);
        }
    }
    
    private void updateEventListForDate(LocalDate date) {
        // En réalité, vous chargeriez les événements pour cette date depuis la base de données
        // Pour l'exemple, on simule le chargement
        eventComboBox.getItems().clear();
        
        if (date != null) {
            // Simuler des événements pour cette date
            eventComboBox.getItems().addAll(
                "Conférence Tech - " + date.format(DateTimeFormatter.ofPattern("dd/MM")),
                "Atelier Développement - " + date.format(DateTimeFormatter.ofPattern("dd/MM")),
                "Réunion Équipe - " + date.format(DateTimeFormatter.ofPattern("dd/MM"))
            );
            
            // Sélectionner le premier événement par défaut
            if (!eventComboBox.getItems().isEmpty()) {
                eventComboBox.setValue(eventComboBox.getItems().get(0));
                updateEventDetails();
            }
        }
    }
    
    private void updateEventDetails() {
        String selectedEvent = eventComboBox.getValue();
        
        if (selectedEvent != null && selectedDate != null) {
            // Simuler des détails d'événement
            eventDateLabel.setText(selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " - 14:00");
            eventLocationLabel.setText("Salle " + (selectedEvent.hashCode() % 10 + 1));
            eventCapacityLabel.setText("50 places");
            eventAvailableLabel.setText("35 places");
            eventAvailableLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        } else {
            // Réinitialiser les labels
            eventDateLabel.setText("Non sélectionné");
            eventLocationLabel.setText("Non sélectionné");
            eventCapacityLabel.setText("0 places");
            eventAvailableLabel.setText("0 places");
            eventAvailableLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        }
    }
    
    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        
        // Vérifier la date
        if (selectedDate == null) {
            errors.append("• Veuillez sélectionner une date\n");
        }
        
        // Vérifier l'événement
        if (eventComboBox.getValue() == null || eventComboBox.getValue().isEmpty()) {
            errors.append("• Veuillez sélectionner un événement\n");
        }
        
        // Vérifier l'utilisateur
        if (userComboBox.getValue() == null || userComboBox.getValue().isEmpty()) {
            errors.append("• Veuillez sélectionner un utilisateur\n");
        }
        
        // Vérifier le nombre de places
        if (quantitySpinner.getValue() == null || quantitySpinner.getValue() <= 0) {
            errors.append("• Veuillez spécifier un nombre de places valide\n");
        }
        
        // Si des erreurs ont été trouvées
        if (errors.length() > 0) {
            showAlert("Erreur de validation", errors.toString(), Alert.AlertType.ERROR);
            return false;
        }
        
        return true;
    }
    
    private void confirmReservation() {
        // Récupérer les valeurs du formulaire
        String event = eventComboBox.getValue();
        String user = userComboBox.getValue();
        int quantity = quantitySpinner.getValue();
        String date = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        
        // En réalité, vous enregistreriez la réservation dans la base de données
        String message = String.format(
            "Réservation confirmée !\n\n" +
            "Détails :\n" +
            "• Événement : %s\n" +
            "• Date : %s\n" +
            "• Utilisateur : %s\n" +
            "• Nombre de places : %d\n\n" +
            "La réservation a été enregistrée avec succès.",
            event, date, user, quantity
        );
        
        showAlert("Réservation confirmée", message, Alert.AlertType.INFORMATION);
        clearForm();
    }
    
    private void clearForm() {
        // Réinitialiser le formulaire
        eventComboBox.setValue(null);
        userComboBox.setValue(null);
        quantitySpinner.getValueFactory().setValue(1);
        
        // Réinitialiser les labels
        eventDateLabel.setText("Non sélectionné");
        eventLocationLabel.setText("Non sélectionné");
        eventCapacityLabel.setText("0 places");
        eventAvailableLabel.setText("0 places");
        
        // Réinitialiser la date sélectionnée
        selectedDate = null;
        
        // Réinitialiser le calendrier si disponible
        if (advancedCalendarController != null && 
            advancedCalendarController.getCalendarPicker() != null) {
            advancedCalendarController.getCalendarPicker().clearSelection();
        }
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Méthodes publiques pour accéder aux données du formulaire
    
    public LocalDate getSelectedDate() {
        return selectedDate;
    }
    
    public String getSelectedEvent() {
        return eventComboBox.getValue();
    }
    
    public String getSelectedUser() {
        return userComboBox.getValue();
    }
    
    public int getQuantity() {
        return quantitySpinner.getValue();
    }
}