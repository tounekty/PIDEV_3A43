package com.mindcare.view.client;

import com.mindcare.utils.NavigationManager;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.sql.SQLException;

public class StatsView implements NavigationManager.Buildable {

    private final org.example.service.MoodService moodService = new org.example.service.MoodService();
    private final org.example.service.EventService eventService = new org.example.service.EventService();
    private final org.example.service.ForumService forumService = new org.example.service.ForumService();
    private final org.example.service.ResourceService resourceService = new org.example.service.ResourceService();

    @Override
    public Node build() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(18));

        Label title = new Label("Statistics Dashboard");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 700;");

        root.getChildren().add(title);

        root.getChildren().add(buildCountStats("Mood Entries", () -> {
            try {
                return String.valueOf(moodService.getAllMoods().size());
            } catch (Exception e) {
                return "0";
            }
        }));

        root.getChildren().add(buildCountStats("Events", () -> {
            try {
                return String.valueOf(eventService.getAllEvents().size());
            } catch (Exception e) {
                return "0";
            }
        }));

        root.getChildren().add(buildCountStats("Forum Subjects", () -> {
            try {
                return String.valueOf(forumService.getAllSubjects().size());
            } catch (Exception e) {
                return "0";
            }
        }));

        root.getChildren().add(buildCountStats("Resources", () -> {
            try {
                return String.valueOf(resourceService.getAllResources().size());
            } catch (Exception e) {
                return "0";
            }
        }));

        return root;
    }

    private Node buildCountStats(String label, java.util.function.Supplier<String> countSupplier) {
        VBox box = new VBox(10);
        Label titleLabel = new Label(label);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600;");

        Label countLabel = new Label("Total: " + countSupplier.get());
        countLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: #1c4f96;");

        box.getChildren().addAll(titleLabel, countLabel);
        return box;
    }
}
