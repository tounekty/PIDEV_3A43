package org.example.ui.template;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Card-style form builder used across CRUD tabs.
 */
public class FormTemplate {
    private final VBox container;
    private final GridPane gridPane;
    private final Map<String, javafx.scene.Node> fields = new LinkedHashMap<>();
    private final Label titleLabel;
    private final Label subtitleLabel;
    private int rowIndex = 0;
    private String sectionTitle;
    private boolean built;

    public FormTemplate() {
        this.container = new VBox(18);
        this.container.setPadding(new Insets(0));
        this.container.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 26;" +
                "-fx-border-color: " + ThemeStyle.toHex(ThemeStyle.BORDER_COLOR) + ";" +
                "-fx-border-radius: 26;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(34,49,63,0.08), 24, 0.15, 0, 8);"
        );

        VBox header = new VBox(6);
        header.setPadding(new Insets(24, 26, 0, 26));

        this.titleLabel = new Label("Entry details");
        this.titleLabel.setStyle("-fx-font-size: 21px; -fx-font-weight: 800; -fx-text-fill: #1C4F96;");

        this.subtitleLabel = new Label("Fill in the fields below and save when you're ready.");
        this.subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6F87A6;");
        this.subtitleLabel.setWrapText(true);

        header.getChildren().addAll(titleLabel, subtitleLabel);

        this.gridPane = new GridPane();
        this.gridPane.setHgap(18);
        this.gridPane.setVgap(18);
        this.gridPane.setPadding(new Insets(0, 26, 26, 26));

        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(120);
        labelCol.setPrefWidth(140);
        labelCol.setHalignment(javafx.geometry.HPos.LEFT);

        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        fieldCol.setMinWidth(280);

        this.gridPane.getColumnConstraints().addAll(labelCol, fieldCol);
        this.container.getChildren().addAll(header, gridPane);
    }

    public FormTemplate addField(String label, Control field) {
        Label labelControl = new Label(label);
        labelControl.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #1C4F96;");
        labelControl.setAlignment(Pos.CENTER_LEFT);

        styleField(field);
        field.setMaxWidth(Double.MAX_VALUE);

        this.gridPane.add(labelControl, 0, this.rowIndex);
        this.gridPane.add(field, 1, this.rowIndex);
        this.fields.put(label, field);
        this.rowIndex++;
        return this;
    }

    public FormTemplate addSpacer(double height) {
        Region spacer = new Region();
        spacer.setPrefHeight(height);
        this.gridPane.add(spacer, 0, this.rowIndex);
        GridPane.setColumnSpan(spacer, 2);
        this.rowIndex++;
        return this;
    }

    public FormTemplate addSection(String title) {
        Label sectionLabel = ThemeStyle.createFormSectionLabel(title);
        this.gridPane.add(sectionLabel, 0, this.rowIndex);
        GridPane.setColumnSpan(sectionLabel, 2);
        this.rowIndex++;

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: " + ThemeStyle.toHex(ThemeStyle.DIVIDER_COLOR) + ";");
        this.gridPane.add(divider, 0, this.rowIndex);
        GridPane.setColumnSpan(divider, 2);
        this.rowIndex++;
        return this;
    }

    public FormTemplate setPadding(Insets insets) {
        this.gridPane.setPadding(insets);
        return this;
    }

    public FormTemplate setTitle(String title) {
        this.sectionTitle = title;
        this.titleLabel.setText(title);
        return this;
    }

    public Parent build() {
        this.built = true;
        return this.container;
    }

    public GridPane buildGridPane() {
        return this.gridPane;
    }

    public Map<String, javafx.scene.Node> getFields() {
        return this.fields;
    }

    private void styleField(Control field) {
        if (field instanceof javafx.scene.control.TextArea area) {
            area.setWrapText(true);
            area.setPrefRowCount(Math.max(area.getPrefRowCount(), 4));
        }
    }
}
