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
import java.time.format.DateTimeFormatter;

/**
 * Exemple d'intégration du calendrier avancé sans FXML
 * Montre comment utiliser le système de calendrier dans une application JavaFX
 */
public class CalendarIntegrationExample extends Application {
    
    private AdvancedCalendarController advancedCalendarController;
    private Label selectedDateLabel;
    private Label eventCountLabel;
    private Label availabilityLabel;
    
    @Override
    public void start(Stage primaryStage) {
        // Initialiser les services
        EventService eventService = new EventService();
        ReservationService reservationService = new ReservationService();
        AuthService authService = new AuthService();
        
        // Créer l'interface
        VBox root = createInterface(eventService, reservationService);
        
        Scene scene = new Scene(root, 1000, 750);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        primaryStage.setTitle("Intégration du Calendrier Avancé - Exemple");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private VBox createInterface(EventService eventService, ReservationService reservationService) {
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: linear-gradient(to bottom, #f9fbff, #e8f4ff);");
        
        // En-tête
        VBox header = createHeader();
        mainContainer.getChildren().add(header);
        
        // Barre d'outils
        HBox toolbar = createToolbar();
        mainContainer.getChildren().add(toolbar);
        
        // Contenu principal
        HBox content = createContent(eventService, reservationService);
        mainContainer.getChildren().add(content);
        
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
        
        Label titleLabel = new Label("🗓️ Intégration du Calendrier Avancé");
        titleLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #1c4f96;");
        
        Label subtitleLabel = new Label("Exemple d'utilisation du système de calendrier interactif");
        subtitleLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #666;");
        
        header.getChildren().addAll(titleLabel, subtitleLabel);
        return header;
    }
    
    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setStyle("-fx-background-color: white; -fx-border-radius: 8; " +
                "-fx-border-color: #d7e7ff;");
        
        Button refreshButton = new Button("🔄 Actualiser");
        refreshButton.setStyle("-fx-font-size: 12; -fx-padding: 6 12; " +
                "-fx-background-color: #2196F3; -fx-text-fill: white; " +
                "-fx-border-radius: 4; -fx-cursor: hand;");
        refreshButton.setOnAction(e -> refreshCalendar());
        
        Button todayButton = new Button("📅 Aujourd'hui");
        todayButton.setStyle("-fx-font-size: 12; -fx-padding: 6 12; " +
                "-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                "-fx-border-radius: 4; -fx-cursor: hand;");
        todayButton.setOnAction(e -> goToToday());
        
        Button clearButton = new Button("✕ Effacer");
        clearButton.setStyle("-fx-font-size: 12; -fx-padding: 6 12; " +
                "-fx-background-color: #FF9800; -fx-text-fill: white; " +
                "-fx-border-radius: 4; -fx-cursor: hand;");
        clearButton.setOnAction(e -> clearSelection());
        
        // Espace flexible
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Label pour la date sélectionnée
        selectedDateLabel = new Label("Aucune date sélectionnée");
        selectedDateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14; " +
                "-fx-text-fill: #1976D2;");
        
        toolbar.getChildren().addAll(refreshButton, todayButton, clearButton, spacer, selectedDateLabel);
        return toolbar;
    }
    
    private HBox createContent(EventService eventService, ReservationService reservationService) {
        HBox content = new HBox(20);
        content.setPrefHeight(500);
        
        // Calendrier
        VBox calendarBox = new VBox(10);
        calendarBox.setStyle("-fx-background-color: white; -fx-border-radius: 12; " +
                "-fx-border-color: #d7e7ff; -fx-padding: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        
        // Créer le contrôleur de calendrier avancé
        advancedCalendarController = new AdvancedCalendarController(eventService, reservationService);
        advancedCalendarController.setOnDateSelected(this::onDateSelected);
        
        VBox calendarPanel = advancedCalendarController.createAdvancedCalendarPanel();
        calendarBox.getChildren().add(calendarPanel);
        
        // Panneau d'informations
        VBox infoPanel = createInfoPanel();
        
        content.getChildren().addAll(calendarBox, infoPanel);
        HBox.setHgrow(calendarBox, Priority.ALWAYS);
        
        return content;
    }
    
    private VBox createInfoPanel() {
        VBox infoPanel = new VBox(15);
        infoPanel.setPrefWidth(300);
        infoPanel.setStyle("-fx-background-color: white; -fx-border-radius: 12; " +
                "-fx-border-color: #d7e7ff; -fx-padding: 20;");
        
        Label infoTitle = new Label("📊 Informations de la date");
        infoTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16; " +
                "-fx-text-fill: #1c4f96;");
        
        Separator separator1 = new Separator();
        
        // Événements programmés
        VBox eventsBox = new VBox(10);
        Label eventsLabel = new Label("Événements programmés:");
        eventsLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");
        
        eventCountLabel = new Label("Chargement...");
        eventCountLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; " +
                "-fx-text-fill: #1976D2;");
        
        eventsBox.getChildren().addAll(eventsLabel, eventCountLabel);
        
        // Disponibilité
        VBox availabilityBox = new VBox(10);
        Label availabilityTitle = new Label("Disponibilité:");
        availabilityTitle.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");
        
        availabilityLabel = new Label("Chargement...");
        availabilityLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; " +
                "-fx-text-fill: #4CAF50;");
        
        availabilityBox.getChildren().addAll(availabilityTitle, availabilityLabel);
        
        Separator separator2 = new Separator();
        
        // Légende
        VBox legendBox = createLegendBox();
        
        // Instructions
        VBox instructionsBox = createInstructionsBox();
        
        infoPanel.getChildren().addAll(
            infoTitle, separator1, eventsBox, availabilityBox, 
            separator2, legendBox, instructionsBox
        );
        
        return infoPanel;
    }
    
    private VBox createLegendBox() {
        VBox legendBox = new VBox(10);
        legendBox.setPadding(new Insets(15));
        legendBox.setStyle("-fx-background-color: #F0F8FF; -fx-border-radius: 8; " +
                "-fx-border-color: #E3F2FD;");
        
        Label legendTitle = new Label("🎨 Légende des couleurs");
        legendTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14; " +
                "-fx-text-fill: #1565C0;");
        
        VBox legendItems = new VBox(5);
        
        addLegendItem(legendItems, "🟢", "Disponible (100%)");
        addLegendItem(legendItems, "🔵", "Beaucoup d'options (25-99%)");
        addLegendItem(legendItems, "🟠", "Peu d'options (1-25%)");
        addLegendItem(legendItems, "⚫", "Complet (0%)");
        
        legendBox.getChildren().addAll(legendTitle, legendItems);
        return legendBox;
    }
    
    private void addLegendItem(VBox container, String symbol, String text) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        
        Label symbolLabel = new Label(symbol);
        symbolLabel.setStyle("-fx-font-size: 16;");
        
        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");
        
        item.getChildren().addAll(symbolLabel, textLabel);
        container.getChildren().add(item);
    }
    
    private VBox createInstructionsBox() {
        VBox instructionsBox = new VBox(10);
        instructionsBox.setPadding(new Insets(15));
        instructionsBox.setStyle("-fx-background-color: #FFF3E0; -fx-border-radius: 8; " +
                "-fx-border-color: #FFE0B2;");
        
        Label instructionsTitle = new Label("💡 Comment utiliser");
        instructionsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14; " +
                "-fx-text-fill: #E65100;");
        
        Label instructionsText = new Label(
                "1. Sélectionnez une date dans le calendrier\n" +
                "2. Les informations s'affichent automatiquement\n" +
                "3. Utilisez les boutons pour naviguer\n" +
                "4. Les couleurs indiquent la disponibilité en temps réel"
        );
        instructionsText.setStyle("-fx-font-size: 11; -fx-text-fill: #E65100; " +
                "-fx-wrap-text: true;");
        
        instructionsBox.getChildren().addAll(instructionsTitle, instructionsText);
        return instructionsBox;
    }
    
    private VBox createFooter() {
        VBox footer = new VBox(5);
        footer.setPadding(new Insets(10));
        footer.setAlignment(Pos.CENTER);
        footer.setStyle("-fx-background-color: #1c4f96; -fx-border-radius: 8;");
        
        Label footerLabel = new Label("Système de Calendrier Avancé v2.0");
        footerLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11;");
        
        Label copyrightLabel = new Label("Intégration JavaFX sans FXML");
        copyrightLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 10;");
        
        footer.getChildren().addAll(footerLabel, copyrightLabel);
        return footer;
    }
    
    private void onDateSelected(LocalDate date) {
        if (date != null) {
            // Mettre à jour le label de date
            String formattedDate = date.format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy"));
            selectedDateLabel.setText("📅 " + formattedDate);
            
            // Mettre à jour les informations
            updateDateInfo(date);
        }
    }
    
    private void updateDateInfo(LocalDate date) {
        if (advancedCalendarController == null) return;
        
        var events = advancedCalendarController.getEventsForDate(date);
        
        if (events == null || events.isEmpty()) {
            eventCountLabel.setText("Aucun événement programmé");
            availabilityLabel.setText("Disponibilité: 100%");
            availabilityLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            return;
        }
        
        int totalEvents = events.size();
        int availableEvents = 0;
        
        for (var event : events) {
            // Pour la démonstration, on suppose que tous les événements sont disponibles
            // En réalité, il faudrait vérifier avec ReservationService
            availableEvents++;
        }
        
        eventCountLabel.setText(String.format("%d événement(s) programmé(s)", totalEvents));
        
        if (availableEvents == totalEvents) {
            availabilityLabel.setText("Disponibilité: 100%");
            availabilityLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        } else if (availableEvents == 0) {
            availabilityLabel.setText("Disponibilité: 0%");
            availabilityLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-weight: bold;");
        } else {
            int percentage = (availableEvents * 100) / totalEvents;
            availabilityLabel.setText(String.format("Disponibilité: %d%%", percentage));
            
            if (percentage >= 75) {
                availabilityLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            } else if (percentage >= 25) {
                availabilityLabel.setStyle("-fx-text-fill: #00BCD4; -fx-font-weight: bold;");
            } else {
                availabilityLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
            }
        }
    }
    
    private void refreshCalendar() {
        // Pour l'instant, on ne fait rien
        // En réalité, on rechargerait les données
        System.out.println("Actualisation du calendrier...");
    }
    
    private void goToToday() {
        if (advancedCalendarController != null && 
            advancedCalendarController.getCalendarPicker() != null) {
            advancedCalendarController.getCalendarPicker().goToToday();
        }
    }
    
    private void clearSelection() {
        if (advancedCalendarController != null && 
            advancedCalendarController.getCalendarPicker() != null) {
            advancedCalendarController.getCalendarPicker().clearSelection();
            selectedDateLabel.setText("Aucune date sélectionnée");
            eventCountLabel.setText("Chargement...");
            availabilityLabel.setText("Chargement...");
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}