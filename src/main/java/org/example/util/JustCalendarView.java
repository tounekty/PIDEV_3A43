package org.example.util;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.event.EventService;
import org.example.reservation.ReservationService;

/**
 * Application qui montre JUSTE le calendrier, rien d'autre
 * Pas de modifications au FXML existant
 */
public class JustCalendarView extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        // Créer un conteneur simple
        VBox root = new VBox(20);
        root.setStyle("-fx-padding: 30; -fx-background-color: linear-gradient(to bottom, #f9fbff, #e8f4ff);");
        
        // Titre
        Label titleLabel = new Label("🗓️ Calendrier Interactif Avancé");
        titleLabel.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: #1c4f96; " +
                           "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        Label subtitleLabel = new Label("Système de visualisation des disponibilités avec codes couleur");
        subtitleLabel.setStyle("-fx-font-size: 16; -fx-text-fill: #666;");
        
        // Créer le calendrier
        AdvancedCalendarController calendarController = new AdvancedCalendarController(
            new EventService(), new ReservationService()
        );
        
        // Définir un callback simple pour la sélection
        calendarController.setOnDateSelected(date -> {
            System.out.println("Date sélectionnée dans le calendrier: " + date);
        });
        
        VBox calendarPanel = calendarController.createAdvancedCalendarPanel();
        
        // Légende
        VBox legendBox = createLegendBox();
        
        // Instructions
        VBox instructionsBox = createInstructionsBox();
        
        // Ajouter tout au conteneur
        root.getChildren().addAll(
            titleLabel,
            subtitleLabel,
            calendarPanel,
            legendBox,
            instructionsBox
        );
        
        // Créer la scène
        Scene scene = new Scene(root, 900, 850);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        // Configurer la fenêtre
        primaryStage.setTitle("Calendrier Interactif - Vue Seule");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        System.out.println("Calendrier affiché seul, sans modifications du FXML existant !");
    }
    
    private VBox createLegendBox() {
        VBox legendBox = new VBox(10);
        legendBox.setStyle("-fx-background-color: white; -fx-border-radius: 12; " +
                          "-fx-border-color: #d7e7ff; -fx-padding: 20; " +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        Label legendTitle = new Label("🎨 Légende des Codes Couleur");
        legendTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: #1c4f96;");
        
        VBox legendItems = new VBox(8);
        legendItems.setStyle("-fx-padding: 10 0 0 0;");
        
        addLegendItem(legendItems, "🟢", "VERT", "Disponible (100% des places)");
        addLegendItem(legendItems, "🔵", "CYAN", "Beaucoup d'options (25-99% des places)");
        addLegendItem(legendItems, "🟠", "ORANGE", "Peu d'options (1-25% des places)");
        addLegendItem(legendItems, "⚫", "GRIS", "Complet (0% des places)");
        
        legendBox.getChildren().addAll(legendTitle, legendItems);
        return legendBox;
    }
    
    private void addLegendItem(VBox container, String emoji, String colorName, String description) {
        VBox item = new VBox(3);
        
        Label colorLabel = new Label(emoji + " " + colorName + " : " + description);
        colorLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        
        Label explanationLabel = new Label("   → Indique le niveau de disponibilité pour cette date");
        explanationLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");
        
        item.getChildren().addAll(colorLabel, explanationLabel);
        container.getChildren().add(item);
    }
    
    private VBox createInstructionsBox() {
        VBox instructionsBox = new VBox(10);
        instructionsBox.setStyle("-fx-background-color: #FFF3E0; -fx-border-radius: 12; " +
                               "-fx-border-color: #FFE0B2; -fx-padding: 20;");
        
        Label instructionsTitle = new Label("💡 Comment utiliser le calendrier");
        instructionsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: #E65100;");
        
        VBox instructionsList = new VBox(8);
        instructionsList.setStyle("-fx-padding: 10 0 0 0;");
        
        addInstruction(instructionsList, "1", "Navigation", "Utilisez les boutons ◀ ▶ pour changer de mois");
        addInstruction(instructionsList, "2", "Aujourd'hui", "Cliquez sur le bouton 'Aujourd'hui' pour revenir au mois actuel");
        addInstruction(instructionsList, "3", "Sélection", "Cliquez sur une date colorée pour la sélectionner");
        addInstruction(instructionsList, "4", "Filtres", "Utilisez le menu déroulant pour filtrer les événements");
        addInstruction(instructionsList, "5", "Informations", "Les détails s'affichent automatiquement en bas");
        
        instructionsBox.getChildren().addAll(instructionsTitle, instructionsList);
        return instructionsBox;
    }
    
    private void addInstruction(VBox container, String number, String title, String description) {
        HBox instruction = new HBox(15);
        
        Label numberLabel = new Label(number);
        numberLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1976D2; " +
                           "-fx-min-width: 30; -fx-alignment: center;");
        
        VBox contentBox = new VBox(3);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666; -fx-wrap-text: true;");
        
        contentBox.getChildren().addAll(titleLabel, descLabel);
        instruction.getChildren().addAll(numberLabel, contentBox);
        container.getChildren().add(instruction);
    }
    

    
    public static void main(String[] args) {
        launch(args);
    }
}