package org.example.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.example.model.Mood;
import org.example.service.AppService;
import org.example.service.ServiceException;
import org.example.ui.template.ThemeStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Statistics panel with charts showing mood distribution and trends.
 */
public class StatisticsPanel {
    private final AppService appService = AppService.getInstance();

    public Parent build() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: " + toHex(ThemeStyle.BACKGROUND_COLOR));

        // Title
        Label titleLabel = new Label("📊 Mood Statistics Dashboard");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + toHex(ThemeStyle.PRIMARY_COLOR));

        // Charts container
        HBox chartsBox = new HBox(20);
        chartsBox.setPadding(new Insets(10));

        try {
            List<Mood> moods = appService.getAllMoods();
            
            if (!moods.isEmpty()) {
                // Pie chart for mood distribution
                PieChart pieChart = createMoodDistributionChart(moods);
                pieChart.setPrefSize(400, 400);

                // Bar chart for mood counts
                BarChart<String, Number> barChart = createMoodCountChart(moods);
                barChart.setPrefSize(400, 400);

                chartsBox.getChildren().addAll(pieChart, barChart);
            } else {
                Label emptyLabel = new Label("No mood data available yet. Add some moods to see statistics!");
                emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #999;");
                chartsBox.getChildren().add(emptyLabel);
            }
        } catch (ServiceException e) {
            Label errorLabel = new Label("Error loading statistics: " + e.getMessage());
            errorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #E74C3C;");
            chartsBox.getChildren().add(errorLabel);
        }

        root.getChildren().addAll(titleLabel, chartsBox);
        return root;
    }

    private PieChart createMoodDistributionChart(List<Mood> moods) {
        Map<String, Long> moodDistribution = moods.stream()
                .collect(Collectors.groupingBy(
                        m -> capitalize(m.getMoodType()),
                        Collectors.counting()
                ));

        javafx.collections.ObservableList<PieChart.Data> pieData = 
                javafx.collections.FXCollections.observableArrayList();

        // Color palette for pie chart
        String[] colors = {
                "#2E86AB", "#A23B72", "#F18F01", "#06A77D",
                "#C73E1D", "#6A8CAF", "#D4AF37", "#8B4789"
        };
        int colorIndex = 0;

        for (Map.Entry<String, Long> entry : moodDistribution.entrySet()) {
            PieChart.Data data = new PieChart.Data(entry.getKey(), entry.getValue());
            pieData.add(data);
            
            // Apply color to pie slice
            final int idx = colorIndex % colors.length;
            data.pieValueProperty().addListener((obs, oldVal, newVal) -> 
                data.getNode().setStyle("-fx-pie-color: " + colors[idx])
            );
            colorIndex++;
        }

        PieChart chart = new PieChart(pieData);
        chart.setTitle("Mood Distribution");
        chart.setLegendVisible(true);
        chart.setStyle("-fx-font-size: 11px;");
        return chart;
    }

    private BarChart<String, Number> createMoodCountChart(List<Mood> moods) {
        Map<String, Long> moodCounts = moods.stream()
                .collect(Collectors.groupingBy(
                        m -> capitalize(m.getMoodType()),
                        Collectors.counting()
                ));

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Mood Type");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Count");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Mood Frequency");
        barChart.setStyle("-fx-font-size: 11px;");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Moods");

        for (Map.Entry<String, Long> entry : moodCounts.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        barChart.getData().add(series);
        barChart.setStyle("-fx-font-size: 11px;");
        return barChart;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
