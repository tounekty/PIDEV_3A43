package org.example.util;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.example.event.Event;
import org.example.event.SmartCapacityManager;
import org.example.reservation.ReservationStatus;

/**
 * Composants JavaFX pour la Smart Capacity
 * Affichage dynamique des infos de capacité
 */
public class SmartCapacityUI {

    private final SmartCapacityController controller = new SmartCapacityController();

    // ============================================
    // AFFICHAGE CAPACITÉ
    // ============================================

    /**
     * Crée un panel de capacité pour un événement
     */
    public VBox createCapacityPanel(int eventId) {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-padding: 15;");
        panel.setPrefWidth(300);

        SmartCapacityController.CapacityDisplayInfo info = 
            controller.getCapacityInfo(eventId);

        if (info == null) {
            panel.getChildren().add(new Label("Événement non trouvé"));
            return panel;
        }

        // Titre
        Label titleLabel = new Label("Disponibilité");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        panel.getChildren().add(titleLabel);

        // Statut
        Label statusLabel = new Label(info.getStatusText());
        statusLabel.setFont(new Font(12));
        setStatusColor(statusLabel, info);
        panel.getChildren().add(statusLabel);

        // Barre de progression
        ProgressBar progressBar = new ProgressBar(info.occupancyRate / 100.0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setStyle(getProgressBarStyle(info.occupancyRate));
        panel.getChildren().add(progressBar);

        // Détails
        HBox detailsBox = createDetailsBox(info);
        panel.getChildren().add(detailsBox);

        return panel;
    }

    /**
     * Crée un dialog de réservation avec feedback
     */
    public Dialog<Boolean> createReservationDialog(int eventId, int userId, Event event) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Réserver: " + event.getTitre());

        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20;");

        // Info événement
        Label eventLabel = new Label(event.getTitre());
        eventLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        content.getChildren().add(eventLabel);

        // Info capacité
        SmartCapacityController.CapacityDisplayInfo info = 
            controller.getCapacityInfo(eventId);
        
        if (info != null) {
            Label capacityLabel = new Label(
                String.format("Capacité: %d/%d | En attente: %d",
                    info.confirmedCount, info.actualCapacity, info.waitlistedCount));
            capacityLabel.setStyle("-fx-text-fill: #666;");
            content.getChildren().add(capacityLabel);

            // Barre
            ProgressBar bar = new ProgressBar(info.occupancyRate / 100.0);
            bar.setPrefWidth(400);
            content.getChildren().add(bar);
        }

        // Boutons
        Button confirmButton = new Button("Réserver");
        Button cancelButton = new Button("Annuler");

        confirmButton.setOnAction(e -> {
            dialog.setResult(true);
            dialog.close();
        });

        cancelButton.setOnAction(e -> {
            dialog.setResult(false);
            dialog.close();
        });

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(cancelButton, confirmButton);
        content.getChildren().add(buttonBox);

        dialog.getDialogPane().setContent(content);
        return dialog;
    }

    /**
     * Affiche le résultat d'une réservation
     */
    public void showReservationResult(SmartCapacityController.ReservationResponse result) {
        Alert alert = new Alert(
            result.success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR
        );
        alert.setTitle(result.success ? "Succès" : "Erreur");
        alert.setHeaderText(null);
        alert.setContentText(result.message);
        alert.showAndWait();
    }

    /**
     * Affiche le statut en liste d'attente
     */
    public void showWaitlistStatus(int eventId, int userId) {
        Integer position = controller.getUserWaitlistPosition(eventId, userId);
        
        if (position != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Statut Liste d'Attente");
            alert.setHeaderText("Vous êtes #" + position + " en attente");
            alert.setContentText(
                "Vous serez automatiquement confirmé dès qu'une place se libère.\n" +
                "Nous vous enverrons une notification par email."
            );
            alert.showAndWait();
        }
    }

    // ============================================
    // ADMIN VIEW
    // ============================================

    /**
     * Crée un panel admin pour gérer les événements
     */
    public VBox createAdminPanel(int eventId) {
        VBox panel = new VBox(15);
        panel.setStyle("-fx-padding: 20;");

        SmartCapacityController.EventAdminView view = 
            controller.getEventAdminView(eventId);

        if (view == null) {
            panel.getChildren().add(new Label("Événement non trouvé"));
            return panel;
        }

        // Titre
        Label titleLabel = new Label(view.getTitle());
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        panel.getChildren().add(titleLabel);

        // Résumé
        Label summaryLabel = new Label(view.getSummary());
        summaryLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");
        panel.getChildren().add(summaryLabel);

        // Statistiques
        VBox statsBox = createStatsBox(view.capacity);
        panel.getChildren().add(statsBox);

        // Liste d'attente
        if (!view.waitlist.isEmpty()) {
            VBox waitlistBox = createWaitlistBox(view.waitlist);
            panel.getChildren().add(waitlistBox);
        }

        return panel;
    }

    /**
     * Crée un widget de synthèse capacité
     */
    public HBox createCompactCapacityWidget(int eventId) {
        HBox widget = new HBox(15);
        widget.setAlignment(Pos.CENTER_LEFT);
        widget.setStyle(
            "-fx-border-color: #e0e0e0; -fx-border-radius: 3; " +
            "-fx-padding: 10; -fx-background-color: #f9f9f9;"
        );

        SmartCapacityController.CapacityDisplayInfo info = 
            controller.getCapacityInfo(eventId);

        if (info == null) {
            return widget;
        }

        // Icône statut
        Label statusIcon = new Label(
            info.confirmedSpaceAvailable ? "✅" : 
            info.totalSpaceAvailable ? "⏳" : "❌"
        );
        statusIcon.setFont(new Font(16));
        widget.getChildren().add(statusIcon);

        // Texte
        Label textLabel = new Label(
            String.format("%d/%d | %d en attente",
                info.confirmedCount, info.actualCapacity, info.waitlistedCount)
        );
        textLabel.setFont(new Font(11));
        widget.getChildren().add(textLabel);

        // Barre
        ProgressBar bar = new ProgressBar(info.occupancyRate / 100.0);
        bar.setPrefWidth(150);
        bar.setStyle(getProgressBarStyle(info.occupancyRate));
        widget.getChildren().add(bar);

        return widget;
    }

    // ============================================
    // MÉTHODES PRIVÉES
    // ============================================

    private HBox createDetailsBox(SmartCapacityController.CapacityDisplayInfo info) {
        HBox box = new HBox(20);
        box.setStyle("-fx-padding: 10; -fx-background-color: #f0f0f0; -fx-border-radius: 3;");

        // Confirmées
        VBox confirmBox = new VBox(3);
        Label confirmLabel = new Label("Confirmées");
        confirmLabel.setFont(new Font(10));
        confirmLabel.setStyle("-fx-text-fill: #666;");
        Label confirmValue = new Label(String.valueOf(info.confirmedCount));
        confirmValue.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        confirmValue.setStyle("-fx-text-fill: #2ecc71;");
        confirmBox.getChildren().addAll(confirmLabel, confirmValue);

        // En attente
        VBox waitlistBox = new VBox(3);
        Label waitlistLabel = new Label("En attente");
        waitlistLabel.setFont(new Font(10));
        waitlistLabel.setStyle("-fx-text-fill: #666;");
        Label waitlistValue = new Label(String.valueOf(info.waitlistedCount));
        waitlistValue.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        waitlistValue.setStyle("-fx-text-fill: #f39c12;");
        waitlistBox.getChildren().addAll(waitlistLabel, waitlistValue);

        // Taux
        VBox rateBox = new VBox(3);
        Label rateLabel = new Label("Taux");
        rateLabel.setFont(new Font(10));
        rateLabel.setStyle("-fx-text-fill: #666;");
        Label rateValue = new Label(String.format("%.0f%%", info.occupancyRate));
        rateValue.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        rateValue.setStyle("-fx-text-fill: #3498db;");
        rateBox.getChildren().addAll(rateLabel, rateValue);

        box.getChildren().addAll(confirmBox, waitlistBox, rateBox);
        return box;
    }

    private VBox createStatsBox(SmartCapacityManager.CapacitySummary summary) {
        VBox statsBox = new VBox(8);
        statsBox.setStyle("-fx-border-color: #e0e0e0; -fx-padding: 10; -fx-border-radius: 3;");

        addStatLine(statsBox, "Confirmées:", summary.confirmedReservations + " / " + summary.actualCapacity);
        addStatLine(statsBox, "En attente:", String.valueOf(summary.waitlistedReservations));
        addStatLine(statsBox, "Capacité max:", String.valueOf(summary.maxCapacity));
        addStatLine(statsBox, "Surbooking:", String.format("%.0f%%", summary.overbookingPercentage));
        addStatLine(statsBox, "Occupation:", String.format("%.1f%%", summary.getOccupancyRate()));

        return statsBox;
    }

    private VBox createWaitlistBox(java.util.List<org.example.reservation.WaitlistService.WaitlistEntry> waitlist) {
        VBox waitlistBox = new VBox(5);
        waitlistBox.setStyle("-fx-border-color: #ffc107; -fx-padding: 10; -fx-border-radius: 3;");

        Label titleLabel = new Label("Liste d'attente (" + waitlist.size() + ")");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        titleLabel.setStyle("-fx-text-fill: #f39c12;");
        waitlistBox.getChildren().add(titleLabel);

        for (org.example.reservation.WaitlistService.WaitlistEntry entry : waitlist) {
            Label entryLabel = new Label(entry.toString());
            entryLabel.setFont(new Font(10));
            waitlistBox.getChildren().add(entryLabel);
        }

        return waitlistBox;
    }

    private void setStatusColor(Label label, SmartCapacityController.CapacityDisplayInfo info) {
        if (!info.totalSpaceAvailable) {
            label.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        } else if (!info.confirmedSpaceAvailable) {
            label.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
        } else {
            label.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
        }
    }

    private String getProgressBarStyle(double occupancyRate) {
        if (occupancyRate >= 95) {
            return "-fx-accent: #e74c3c;";  // Rouge
        } else if (occupancyRate >= 75) {
            return "-fx-accent: #f39c12;";  // Orange
        } else if (occupancyRate >= 50) {
            return "-fx-accent: #3498db;";  // Bleu
        } else {
            return "-fx-accent: #2ecc71;";  // Vert
        }
    }

    private void addStatLine(VBox box, String label, String value) {
        HBox line = new HBox(10);
        Label labelControl = new Label(label);
        labelControl.setStyle("-fx-text-fill: #666;");
        labelControl.setPrefWidth(100);
        Label valueControl = new Label(value);
        valueControl.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        line.getChildren().addAll(labelControl, valueControl);
        box.getChildren().add(line);
    }
}
