package org.example.util;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.event.EventService;
import org.example.reservation.ReservationService;

/**
 * Application pour voir le calendrier SANS modifier le FXML existant
 * Charge le FXML original et ajoute le calendrier dynamiquement
 */
public class ViewCalendarOnly extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Créer un conteneur principal
            VBox mainContainer = new VBox(15);
            mainContainer.setStyle("-fx-padding: 20; -fx-background-color: #f9fbff;");
            
            // Titre
            Label titleLabel = new Label("🗓️ Visualisation du Calendrier Interactif");
            titleLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #1c4f96;");
            
            Label subtitleLabel = new Label("Calendrier ajouté dynamiquement sans modifier le FXML existant");
            subtitleLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #666;");
            
            // Créer le calendrier avancé
            AdvancedCalendarController calendarController = new AdvancedCalendarController(
                new EventService(), new ReservationService()
            );
            
            VBox calendarPanel = calendarController.createAdvancedCalendarPanel();
            
            // Charger le FXML original (sans modifications)
            Label originalLabel = new Label("FXML original chargé ci-dessous :");
            originalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #1c4f96;");
            
            // Essayer de charger le FXML original
            VBox originalFxmlContent;
            try {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/add_reservation_dialog.fxml")
                );
                originalFxmlContent = loader.load();
            } catch (Exception e) {
                originalFxmlContent = new VBox();
                Label errorLabel = new Label("Impossible de charger le FXML original : " + e.getMessage());
                errorLabel.setStyle("-fx-text-fill: #d32f2f;");
                originalFxmlContent.getChildren().add(errorLabel);
            }
            
            // Ajouter tout au conteneur principal
            mainContainer.getChildren().addAll(
                titleLabel,
                subtitleLabel,
                calendarPanel,
                originalLabel,
                originalFxmlContent
            );
            
            // Créer la scène
            Scene scene = new Scene(mainContainer, 800, 1000);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            
            // Configurer la fenêtre
            primaryStage.setTitle("Visualisation du Calendrier - Sans Modifications");
            primaryStage.setScene(scene);
            primaryStage.show();
            
            System.out.println("Calendrier affiché sans modifications du FXML !");
            
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
            e.printStackTrace();
            
            // Fallback simple
            VBox fallback = new VBox();
            fallback.setStyle("-fx-padding: 20; -fx-background-color: #f9fbff;");
            
            Label errorLabel = new Label("Erreur : " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #d32f2f;");
            
            fallback.getChildren().add(errorLabel);
            
            Scene fallbackScene = new Scene(fallback, 400, 200);
            primaryStage.setScene(fallbackScene);
            primaryStage.setTitle("Erreur");
            primaryStage.show();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}