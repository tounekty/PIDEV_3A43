package org.example.util;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.event.EventService;
import org.example.reservation.ReservationService;
import org.example.auth.AuthService;

import java.time.LocalDate;

/**
 * Application de démonstration du calendrier avancé
 * Montre comment utiliser le système de calendrier interactif
 */
public class CalendarExampleApp extends Application {
    
    private AdvancedCalendarController advancedCalendarController;
    private EventService eventService;
    private ReservationService reservationService;
    private AuthService authService;

    @Override
    public void start(Stage primaryStage) {
        // Initialiser les services (pour la démonstration)
        eventService = new EventService();
        reservationService = new ReservationService();
        authService = new AuthService();

        // Créer l'interface principale
        VBox root = createMainInterface();
        
        Scene scene = new Scene(root, 900, 700);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        primaryStage.setTitle("Système de Calendrier Avancé - Démonstration");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createMainInterface() {
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: linear-gradient(to bottom, #f9fbff, #e8f4ff);");

        // En-tête
        VBox header = createHeader();
        mainContainer.getChildren().add(header);

        // Contenu principal avec onglets
        TabPane tabPane = createTabPane();
        mainContainer.getChildren().add(tabPane);

        // Pied de page
        VBox footer = createFooter();
        mainContainer.getChildren().add(footer);

        return mainContainer;
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(15));
        header.setStyle("-fx-background-color: white; -fx-border-radius: 12; " +
                "-fx-border-color: #d7e7ff; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label titleLabel = new Label("🗓️ Système de Calendrier Interactif Avancé");
        titleLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #1c4f96;");

        Label subtitleLabel = new Label("Gestion d'événements et réservations avec calendrier visuel");
        subtitleLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #666;");

        header.getChildren().addAll(titleLabel, subtitleLabel);
        return header;
    }

    private TabPane createTabPane() {
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: transparent; -fx-tab-min-width: 120;");

        // Onglet 1: Calendrier avancé
        Tab advancedCalendarTab = new Tab("📅 Calendrier Avancé");
        advancedCalendarTab.setClosable(false);
        advancedCalendarTab.setContent(createAdvancedCalendarTab());
        
        // Onglet 2: Réservation rapide
        Tab quickReservationTab = new Tab("🎟️ Réservation Rapide");
        quickReservationTab.setClosable(false);
        quickReservationTab.setContent(createQuickReservationTab());
        
        // Onglet 3: Statistiques
        Tab statsTab = new Tab("📊 Statistiques");
        statsTab.setClosable(false);
        statsTab.setContent(createStatsTab());

        tabPane.getTabs().addAll(advancedCalendarTab, quickReservationTab, statsTab);
        return tabPane;
    }

    private VBox createAdvancedCalendarTab() {
        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(20));
        tabContent.setStyle("-fx-background-color: white; -fx-border-radius: 8;");

        // Créer le contrôleur de calendrier avancé
        advancedCalendarController = new AdvancedCalendarController(eventService, reservationService);
        
        // Définir un callback pour la sélection de date
        advancedCalendarController.setOnDateSelected(date -> {
            System.out.println("Date sélectionnée dans le calendrier avancé: " + date);
            showEventDetailsForDate(date);
        });

        // Ajouter le panneau de calendrier
        VBox calendarPanel = advancedCalendarController.createAdvancedCalendarPanel();
        tabContent.getChildren().add(calendarPanel);

        return tabContent;
    }

    private VBox createQuickReservationTab() {
        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(20));
        tabContent.setStyle("-fx-background-color: white; -fx-border-radius: 8;");

        Label titleLabel = new Label("🎯 Réservation Rapide avec Calendrier");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1c4f96;");

        // Créer un calendrier simple pour la réservation rapide
        CalendarPicker quickCalendar = new CalendarPicker(date -> {
            System.out.println("Date sélectionnée pour réservation rapide: " + date);
            showQuickReservationDialog(date);
        });

        // Configurer quelques disponibilités de démonstration
        quickCalendar.setDateAvailability(LocalDate.now().plusDays(1), 0); // Peu d'options
        quickCalendar.setDateAvailability(LocalDate.now().plusDays(2), 1); // Beaucoup d'options
        quickCalendar.setDateAvailability(LocalDate.now().plusDays(3), 2); // Complet
        quickCalendar.setDateAvailability(LocalDate.now().plusDays(4), 3); // Disponible

        // Instructions
        VBox instructions = new VBox(10);
        instructions.setPadding(new Insets(15));
        instructions.setStyle("-fx-background-color: #F0F8FF; -fx-border-radius: 8; -fx-border-color: #E3F2FD;");
        
        Label instructionsTitle = new Label("Instructions:");
        instructionsTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #1565C0;");
        
        TextArea instructionsText = new TextArea(
                "1. Sélectionnez une date dans le calendrier\n" +
                "2. Les couleurs indiquent la disponibilité:\n" +
                "   • 🟠 Orange: Peu de places disponibles (1-25%)\n" +
                "   • 🔵 Cyan: Beaucoup de places (25-99%)\n" +
                "   • ⚫ Gris: Complet (0%)\n" +
                "   • 🟢 Vert: Disponible (100%)\n" +
                "3. Cliquez sur une date disponible pour réserver"
        );
        instructionsText.setEditable(false);
        instructionsText.setWrapText(true);
        instructionsText.setStyle("-fx-background-color: transparent; -fx-border: none; -fx-font-size: 12;");
        instructionsText.setPrefHeight(120);

        instructions.getChildren().addAll(instructionsTitle, instructionsText);

        tabContent.getChildren().addAll(titleLabel, quickCalendar, instructions);
        return tabContent;
    }

    private VBox createStatsTab() {
        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(20));
        tabContent.setStyle("-fx-background-color: white; -fx-border-radius: 8;");

        Label titleLabel = new Label("📈 Statistiques et Analyses");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1c4f96;");

        // Statistiques de démonstration
        VBox statsContainer = new VBox(10);
        statsContainer.setPadding(new Insets(15));
        statsContainer.setStyle("-fx-background-color: #F5F5F5; -fx-border-radius: 8; -fx-border-color: #E0E0E0;");

        addStatItem(statsContainer, "Événements totaux ce mois-ci", "24");
        addStatItem(statsContainer, "Taux d'occupation moyen", "78%");
        addStatItem(statsContainer, "Événements complets", "6");
        addStatItem(statsContainer, "Événements avec places disponibles", "18");
        addStatItem(statsContainer, "Jours les plus populaires", "Vendredi, Samedi");
        addStatItem(statsContainer, "Prochain événement", "Demain à 14:00");

        // Graphique de démonstration (simulé)
        VBox chartDemo = new VBox(10);
        chartDemo.setPadding(new Insets(15));
        chartDemo.setStyle("-fx-background-color: #FFF3E0; -fx-border-radius: 8; -fx-border-color: #FFE0B2;");
        
        Label chartLabel = new Label("📊 Répartition des disponibilités");
        chartLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #E65100;");
        
        ProgressBar availabilityBar = new ProgressBar(0.78);
        availabilityBar.setPrefWidth(300);
        availabilityBar.setStyle("-fx-accent: #4CAF50;");
        
        Label barLabel = new Label("78% des événements ont des places disponibles");
        barLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");

        chartDemo.getChildren().addAll(chartLabel, availabilityBar, barLabel);

        tabContent.getChildren().addAll(titleLabel, statsContainer, chartDemo);
        return tabContent;
    }

    private void addStatItem(VBox container, String label, String value) {
        HBox statItem = new HBox(10);
        statItem.setPadding(new Insets(5));
        
        Label statLabel = new Label(label + ":");
        statLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666; -fx-pref-width: 200;");
        
        Label statValue = new Label(value);
        statValue.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        statItem.getChildren().addAll(statLabel, statValue);
        container.getChildren().add(statItem);
    }

    private void showEventDetailsForDate(LocalDate date) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails des événements");
        alert.setHeaderText("Événements pour le " + date);
        alert.setContentText("Fonctionnalité de détail des événements à implémenter.\n" +
                "Date sélectionnée: " + date);
        alert.showAndWait();
    }

    private void showQuickReservationDialog(LocalDate date) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Réservation rapide");
        alert.setHeaderText("Confirmer la réservation");
        alert.setContentText("Voulez-vous réserver pour le " + date + " ?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Succès");
                successAlert.setHeaderText(null);
                successAlert.setContentText("Réservation confirmée pour le " + date);
                successAlert.showAndWait();
            }
        });
    }

    private VBox createFooter() {
        VBox footer = new VBox(5);
        footer.setPadding(new Insets(10));
        footer.setAlignment(Pos.CENTER);
        footer.setStyle("-fx-background-color: #1c4f96; -fx-border-radius: 8;");

        Label footerLabel = new Label("Système de Calendrier Avancé v2.0 • Gestion d'événements interactif");
        footerLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11;");

        Label copyrightLabel = new Label("© 2024 Application de Gestion d'Événements");
        copyrightLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 10;");

        footer.getChildren().addAll(footerLabel, copyrightLabel);
        return footer;
    }

    public static void main(String[] args) {
        launch(args);
    }
}