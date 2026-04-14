package com.mindcare.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * StatCard â€“ reusable KPI card for dashboards.
 * Shows an icon, value and label. Color variant controls the icon box background.
 */
public class StatCard extends VBox {

    public enum Color { GREEN, BLUE, ORANGE, RED, PURPLE }

    public StatCard(String label, String value, Feather icon, Color color) {
        getStyleClass().add("stat-card");
        setSpacing(16);
        setMinWidth(170);

        // Icon box
        FontIcon fi = FontIcon.of(icon, 22);
        fi.setStyle("-fx-icon-color: " + colorHex(color) + ";");

        Label iconBox = new Label();
        iconBox.setGraphic(fi);
        iconBox.getStyleClass().add("stat-card-icon-box");
        iconBox.getStyleClass().add(colorClass(color));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setMinSize(48, 48);
        iconBox.setMaxSize(48, 48);

        // Value
        Label valueLbl = new Label(value);
        valueLbl.getStyleClass().add("stat-card-value");

        // Label
        Label labelLbl = new Label(label);
        labelLbl.getStyleClass().add("stat-card-label");

        VBox textBox = new VBox(4, valueLbl, labelLbl);

        HBox row = new HBox(16, iconBox, textBox);
        row.setAlignment(Pos.CENTER_LEFT);

        getChildren().add(row);
        setPadding(new Insets(20));
    }

    private String colorHex(Color color) {
        return switch (color) {
            case GREEN  -> "#0D6EFD";
            case BLUE   -> "#3B82F6";
            case ORANGE -> "#F59E0B";
            case RED    -> "#EF4444";
            case PURPLE -> "#8B5CF6";
        };
    }

    private String colorClass(Color color) {
        return switch (color) {
            case GREEN  -> "stat-icon-green";
            case BLUE   -> "stat-icon-blue";
            case ORANGE -> "stat-icon-orange";
            case RED    -> "stat-icon-red";
            case PURPLE -> "stat-icon-purple";
        };
    }
}

