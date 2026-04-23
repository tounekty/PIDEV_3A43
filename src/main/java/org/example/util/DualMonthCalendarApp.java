package org.example.util;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.example.config.DatabaseConnection;
import org.example.event.EventService;
import org.example.reservation.ReservationService;

/**
 * Application de démonstration du calendrier à deux mois
 * Affiche un calendrier moderne avec système de disponibilité coloré
 */
public class DualMonthCalendarApp extends Application {
    private EventService eventService;
    private ReservationService reservationService;
    private Label selectedDateLabel;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialiser les services
            eventService = new EventService();
            reservationService = new ReservationService();

            // Créer la scène principale
            BorderPane root = new BorderPane();
            root.setStyle("-fx-background-color: #f5f7fa;");

            // En-tête
            VBox header = createHeader();
            root.setTop(header);

            // Contenu principal avec le calendrier
            VBox content = createContent();
            root.setCenter(content);

            // Pied de page
            HBox footer = createFooter();
            root.setBottom(footer);

            Scene scene = new Scene(root, 1100, 750);
            primaryStage.setScene(scene);
            primaryStage.setTitle("📅 Calendrier de Réservation - Vue à Deux Mois");
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(20));
        header.setStyle("-fx-background-color: linear-gradient(to right, #1976D2, #1565C0); " +
                "-fx-border-color: #0D47A1; -fx-border-width: 0 0 1 0;");

        Label title = new Label("📅 Système de Réservation - Calendrier Avancé");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setStyle("-fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 0, 1);");

        Label subtitle = new Label("Sélectionnez une date pour vérifier la disponibilité et réserver votre événement");
        subtitle.setFont(Font.font("Segoe UI", 13));
        subtitle.setStyle("-fx-text-fill: rgba(255,255,255,0.9);");

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private VBox createContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #f5f7fa;");

        // Créer le calendrier à deux mois
        DualMonthCalendarView calendarView = new DualMonthCalendarView(
                eventService,
                reservationService,
                selectedDate -> {
                    updateSelectedDateInfo(selectedDate);
                }
        );

        content.getChildren().add(calendarView);
        VBox.setVgrow(calendarView, Priority.ALWAYS);

        // Panneau d'informations sur la date sélectionnée
        VBox infoPanel = createInfoPanel();
        content.getChildren().add(infoPanel);

        return content;
    }

    private VBox createInfoPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: white; -fx-border-radius: 10; " +
                "-fx-border-color: #e0e0e0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 1);");

        Label infoTitle = new Label("ℹ️ Information sur la Date Sélectionnée");
        infoTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        infoTitle.setStyle("-fx-text-fill: #1976D2;");

        selectedDateLabel = new Label("Veuillez sélectionner une date sur le calendrier");
        selectedDateLabel.setFont(Font.font("Segoe UI", 12));
        selectedDateLabel.setStyle("-fx-text-fill: #666; -fx-wrap-text: true;");

        panel.getChildren().addAll(infoTitle, selectedDateLabel);
        return panel;
    }

    private HBox createFooter() {
        HBox footer = new HBox(10);
        footer.setPadding(new Insets(15, 20, 15, 20));
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button cancelButton = new Button("Annuler");
        cancelButton.setStyle("-fx-font-size: 12; -fx-padding: 10 25; -fx-background-color: #f44336; " +
                "-fx-text-fill: white; -fx-border-radius: 5; -fx-cursor: hand;");
        cancelButton.setOnAction(e -> System.exit(0));

        Button continueButton = new Button("Continuer →");
        continueButton.setStyle("-fx-font-size: 12; -fx-padding: 10 25; -fx-background-color: #4CAF50; " +
                "-fx-text-fill: white; -fx-border-radius: 5; -fx-cursor: hand; -fx-font-weight: bold;");
        continueButton.setOnAction(e -> System.out.println("Réservation en cours..."));

        footer.getChildren().addAll(spacer, cancelButton, continueButton);
        return footer;
    }

    private void updateSelectedDateInfo(java.time.LocalDate selectedDate) {
        if (selectedDate != null) {
            String dayOfWeek = selectedDate.getDayOfWeek().toString();
            String dayOfWeekFR = translateDayOfWeek(dayOfWeek);
            String formattedDate = String.format("📍 Date sélectionnée: %s %d %s %d",
                    dayOfWeekFR,
                    selectedDate.getDayOfMonth(),
                    translateMonth(selectedDate.getMonth().toString()),
                    selectedDate.getYear());

            try {
                // Récupérer les événements pour cette date
                java.util.List<org.example.event.Event> eventsOnDate = eventService.getAllEvents().stream()
                        .filter(e -> e.getDateEvent().toLocalDate().equals(selectedDate))
                        .collect(java.util.stream.Collectors.toList());

                if (eventsOnDate.isEmpty()) {
                    selectedDateLabel.setText(formattedDate + "\n\n❌ Aucun événement prévu pour cette date.");
                } else {
                    StringBuilder info = new StringBuilder(formattedDate + "\n\n✅ Événements disponibles:\n");
                    for (org.example.event.Event event : eventsOnDate) {
                        info.append(String.format("  • %s - %s\n", event.getTitle(), event.getLieu()));
                    }
                    selectedDateLabel.setText(info.toString());
                }
            } catch (Exception e) {
                selectedDateLabel.setText(formattedDate + "\n\n⚠️ Erreur lors du chargement des événements.");
            }
        }
    }

    private String translateDayOfWeek(String english) {
        return switch (english) {
            case "MONDAY" -> "Lundi";
            case "TUESDAY" -> "Mardi";
            case "WEDNESDAY" -> "Mercredi";
            case "THURSDAY" -> "Jeudi";
            case "FRIDAY" -> "Vendredi";
            case "SATURDAY" -> "Samedi";
            case "SUNDAY" -> "Dimanche";
            default -> english;
        };
    }

    private String translateMonth(String english) {
        return switch (english) {
            case "JANUARY" -> "Janvier";
            case "FEBRUARY" -> "Février";
            case "MARCH" -> "Mars";
            case "APRIL" -> "Avril";
            case "MAY" -> "Mai";
            case "JUNE" -> "Juin";
            case "JULY" -> "Juillet";
            case "AUGUST" -> "Août";
            case "SEPTEMBER" -> "Septembre";
            case "OCTOBER" -> "Octobre";
            case "NOVEMBER" -> "Novembre";
            case "DECEMBER" -> "Décembre";
            default -> english;
        };
    }

    public static void main(String[] args) {
        launch(args);
    }
}
