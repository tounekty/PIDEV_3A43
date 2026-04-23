package org.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.util.CalendarPicker;
import org.example.util.CalendarDialog;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Exemple minimal d'utilisation du calendrier
 * Exécutez cette classe pour tester le calendrier avancé
 * 
 * Pour tester rapidement:
 * 1. Décommentez la classe
 * 2. Changez la classe main dans pom.xml de Main à CalendarExample
 * 3. Exécutez: mvn javafx:run
 */
public class CalendarExample extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("📅 Exemple du Calendrier Avancé");

        // Option 1: Tester le CalendarPicker directement
        testCalendarPicker(primaryStage);
        
        // Option 2: Tester le CalendarDialog
        // testCalendarDialog(primaryStage);
    }

    /**
     * Test 1: Afficher le CalendarPicker directement
     */
    private void testCalendarPicker(Stage primaryStage) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f9fbff;");

        // Créer le calendrier avec un callback
        CalendarPicker calendar = new CalendarPicker(selectedDate -> {
            System.out.println("✓ Date sélectionnée: " + selectedDate);
        });

        // Ajouter les disponibilités d'exemple
        Map<LocalDate, Integer> availabilities = new HashMap<>();
        
        // Ajouter quelques dates d'exemple
        for (int i = 0; i < 30; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            
            // Alternance entre les différentes disponibilités
            int availability;
            if (i % 5 == 0) {
                availability = 2; // Complet (gris)
            } else if (i % 3 == 0) {
                availability = 0; // Peu d'options (orange)
            } else {
                availability = 1; // Beaucoup d'options (cyan)
            }
            
            availabilities.put(date, availability);
        }

        // Appliquer les disponibilités
        calendar.setDateAvailabilities(availabilities);

        root.getChildren().add(calendar);

        Scene scene = new Scene(root, 550, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        System.out.println("=== CALENDRIER PICKER ===");
        System.out.println("🟠 Orange = Peu d'options");
        System.out.println("🔵 Cyan = Beaucoup d'options");
        System.out.println("⚫ Gris = Complet");
        System.out.println("Cliquez sur une date pour tester");
    }

    /**
     * Test 2: Afficher le CalendarDialog
     */
    private void testCalendarDialog(Stage primaryStage) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white; -fx-alignment: center;");

        javafx.scene.control.Button openButton = new javafx.scene.control.Button("Ouvrir le calendrier");
        openButton.setStyle("-fx-font-size: 14; -fx-padding: 10 20; -fx-background-color: #0f69ff; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        javafx.scene.control.Label resultLabel = new javafx.scene.control.Label("Aucune date sélectionnée");
        resultLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #1976D2;");

        openButton.setOnAction(e -> {
            CalendarDialog dialog = new CalendarDialog();

            // Ajouter les disponibilités
            Map<LocalDate, Integer> availabilities = new HashMap<>();
            for (int i = 0; i < 30; i++) {
                LocalDate date = LocalDate.now().plusDays(i);
                int availability = (i % 2 == 0) ? 1 : 0;
                availabilities.put(date, availability);
            }
            dialog.setDateAvailabilities(availabilities);

            // Afficher et récupérer le résultat
            var result = dialog.showAndWait();
            if (result.isPresent()) {
                LocalDate selectedDate = result.get();
                resultLabel.setText("✓ Date sélectionnée: " + selectedDate);
                System.out.println("Date sélectionnée du dialogue: " + selectedDate);
            } else {
                resultLabel.setText("Sélection annulée");
            }
        });

        root.getChildren().addAll(
                new javafx.scene.control.Label("Cliquez pour ouvrir le dialogue du calendrier:"),
                openButton,
                resultLabel
        );

        Scene scene = new Scene(root, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();

        System.out.println("=== CALENDRIER DIALOG ===");
        System.out.println("Cliquez sur le bouton pour ouvrir le dialogue");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
