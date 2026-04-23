package org.example.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.event.EventService;
import org.example.reservation.ReservationService;
import org.example.auth.AuthService;

import java.time.LocalDate;

/**
 * Exemple d'utilisation du calendrier dans la réservation d'événements
 * Cette classe montre comment intégrer le calendrier avancé dans votre application
 */
public class AdvancedReservationDialog {
    private final EventService eventService;
    private final ReservationService reservationService;
    private final AuthService authService;
    private CalendarReservationController calendarController;
    private LocalDate selectedDate;

    public AdvancedReservationDialog(EventService eventService, ReservationService reservationService, AuthService authService) {
        this.eventService = eventService;
        this.reservationService = reservationService;
        this.authService = authService;
    }

    /**
     * Afficher le dialogue de réservation avancé
     */
    public void show(Stage owner) {
        Stage stage = new Stage();
        stage.setTitle("Ajouter une Réservation - Mode Avancé");
        stage.initOwner(owner);

        // Créer le contenu
        VBox content = createContent();

        Scene scene = new Scene(content, 550, 750);
        stage.setScene(scene);
        stage.show();
    }

    private VBox createContent() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f9fbff;");

        // En-tête
        VBox header = createHeader();
        root.getChildren().add(header);

        // Panneau du calendrier
        calendarController = new CalendarReservationController(eventService);
        VBox calendarPanel = calendarController.createDateSelectionPanel();
        root.getChildren().add(calendarPanel);

        // Formulaire de réservation
        VBox form = createReservationForm();
        root.getChildren().add(form);

        // Boutons d'action
        HBox buttons = createActionButtons();
        root.getChildren().add(buttons);

        return root;
    }

    private VBox createHeader() {
        VBox header = new VBox(5);
        header.setStyle("-fx-border-color: #d7e7ff; -fx-border-radius: 8; -fx-background-color: #f9fbff; " +
                "-fx-padding: 15; -fx-border-width: 1;");

        Label title = new Label("🎟️ Ajouter une Réservation - Calendrier Avancé");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: #1976D2;");

        Label subtitle = new Label("Utilisez le calendrier interactif pour sélectionner une date");
        subtitle.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private VBox createReservationForm() {
        VBox form = new VBox(12);
        form.setPadding(new Insets(15));
        form.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-background-color: white;");

        // Événement
        Label eventLabel = new Label("Événement");
        eventLabel.setStyle("-fx-font-weight: bold;");
        ComboBox<String> eventCombo = new ComboBox<>();
        eventCombo.setStyle("-fx-font-size: 12; -fx-padding: 8; -fx-min-height: 35;");

        // Détails de l'événement
        VBox eventDetails = createEventDetailsPanel();

        // Utilisateur
        Label userLabel = new Label("Utilisateur");
        userLabel.setStyle("-fx-font-weight: bold;");
        ComboBox<String> userCombo = new ComboBox<>();
        userCombo.setStyle("-fx-font-size: 12; -fx-padding: 8; -fx-min-height: 35;");

        // Quantité
        Label quantityLabel = new Label("Nombre de places");
        quantityLabel.setStyle("-fx-font-weight: bold;");
        Spinner<Integer> quantitySpinner = new Spinner<>(1, 10, 1);
        quantitySpinner.setStyle("-fx-font-size: 12;");

        // Notes
        VBox infoBox = createInfoBox();

        form.getChildren().addAll(
                eventLabel, eventCombo,
                eventDetails,
                userLabel, userCombo,
                quantityLabel, quantitySpinner,
                infoBox
        );

        return form;
    }

    private VBox createEventDetailsPanel() {
        VBox details = new VBox(10);
        details.setPadding(new Insets(12));
        details.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-color: #f5f5f5;");

        Label titleLabel = new Label("Détails de l'événement sélectionné");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");

        HBox infoBox = new HBox(20);
        infoBox.setPadding(new Insets(10));

        // Date/Heure
        VBox dateBox = new VBox(3);
        Label dateKeyLabel = new Label("Date/Heure");
        dateKeyLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #666;");
        Label dateValueLabel = new Label("À sélectionner");
        dateValueLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
        dateBox.getChildren().addAll(dateKeyLabel, dateValueLabel);

        // Lieu
        VBox locationBox = new VBox(3);
        Label locationKeyLabel = new Label("Lieu");
        locationKeyLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #666;");
        Label locationValueLabel = new Label("À sélectionner");
        locationValueLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
        locationBox.getChildren().addAll(locationKeyLabel, locationValueLabel);

        // Capacité
        VBox capacityBox = new VBox(3);
        Label capacityKeyLabel = new Label("Capacité");
        capacityKeyLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #666;");
        Label capacityValueLabel = new Label("0 places");
        capacityValueLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
        capacityBox.getChildren().addAll(capacityKeyLabel, capacityValueLabel);

        // Disponibles
        VBox availableBox = new VBox(3);
        Label availableKeyLabel = new Label("Disponibles");
        availableKeyLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #666;");
        Label availableValueLabel = new Label("0 places");
        availableValueLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12; -fx-text-fill: #4CAF50;");
        availableBox.getChildren().addAll(availableKeyLabel, availableValueLabel);

        infoBox.getChildren().addAll(dateBox, locationBox, capacityBox, availableBox);
        details.getChildren().addAll(titleLabel, infoBox);

        return details;
    }

    private VBox createInfoBox() {
        VBox infoBox = new VBox(5);
        infoBox.setPadding(new Insets(12));
        infoBox.setStyle("-fx-border-color: #E3F2FD; -fx-border-radius: 5; -fx-background-color: #F0F8FF;");

        Label titleLabel = new Label("ℹ️ Information importante");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11; -fx-text-fill: #1565C0;");

        Label msgLabel = new Label(
                "• Sélectionnez d'abord une date dans le calendrier\n" +
                "• Orange = Peu de places disponibles\n" +
                "• Cyan = Beaucoup de places disponibles\n" +
                "• Gris = Complet (non disponible)\n" +
                "• Une réservation ne peut être annulée que par l'administrateur"
        );
        msgLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #1565C0; -fx-wrap-text: true;");

        infoBox.getChildren().addAll(titleLabel, msgLabel);
        return infoBox;
    }

    private HBox createActionButtons() {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10));

        Button cancelButton = new Button("Annuler");
        cancelButton.setStyle("-fx-background-color: white; -fx-text-fill: #666; " +
                "-fx-border-color: #ccc; -fx-padding: 10 20; -fx-background-radius: 8;");
        cancelButton.setOnAction(e -> System.out.println("Réservation annulée"));

        Button confirmButton = new Button("✓ Confirmer la réservation");
        confirmButton.setStyle("-fx-background-color: linear-gradient(to right,#0f69ff,#38a4ff); " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; " +
                "-fx-background-radius: 8; -fx-font-size: 12;");
        confirmButton.setOnAction(e -> confirmReservation());

        buttonBox.getChildren().addAll(cancelButton, confirmButton);
        return buttonBox;
    }

    private void confirmReservation() {
        LocalDate selectedDate = calendarController.getSelectedDate();
        if (selectedDate == null) {
            showAlert("Erreur", "Veuillez sélectionner une date");
            return;
        }
        showAlert("Succès", "Réservation confirmée pour la date: " + selectedDate);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
