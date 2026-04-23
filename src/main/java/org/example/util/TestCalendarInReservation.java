package org.example.util;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Application de test pour voir le calendrier dans le dialogue de réservation
 */
public class TestCalendarInReservation extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Charger le FXML du dialogue de réservation
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/add_reservation_dialog.fxml")
            );
            
            VBox root = loader.load();
            
            // Créer la scène
            Scene scene = new Scene(root, 600, 700);
            
            // Appliquer les styles CSS
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            
            // Configurer la fenêtre
            primaryStage.setTitle("Ajouter une Réservation - Calendrier Intégré");
            primaryStage.setScene(scene);
            primaryStage.show();
            
            System.out.println("Dialogue de réservation avec calendrier chargé avec succès !");
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du dialogue : " + e.getMessage());
            e.printStackTrace();
            
            // Fallback: Afficher un message d'erreur
            showErrorDialog(primaryStage, e);
        }
    }
    
    private void showErrorDialog(Stage primaryStage, Exception e) {
        // Créer une interface simple de secours
        VBox fallbackRoot = new VBox();
        fallbackRoot.setStyle("-fx-padding: 20; -fx-background-color: #f9fbff;");
        
        javafx.scene.control.Label errorLabel = new javafx.scene.control.Label(
            "Erreur de chargement du dialogue.\n" +
            "Message: " + e.getMessage() + "\n\n" +
            "Veuillez vérifier:\n" +
            "1. Que le fichier add_reservation_dialog.fxml existe\n" +
            "2. Que le contrôleur AddReservationController est compilé\n" +
            "3. Que les dépendances JavaFX sont correctes"
        );
        errorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12; -fx-wrap-text: true;");
        
        fallbackRoot.getChildren().add(errorLabel);
        
        Scene fallbackScene = new Scene(fallbackRoot, 500, 300);
        primaryStage.setScene(fallbackScene);
        primaryStage.setTitle("Erreur - Calendrier Intégré");
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}