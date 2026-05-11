package org.example.util;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalTime;

/**
 * Sélecteur d'heure compact et moderne — une ligne horizontale avec +/- ronds
 */
public class TimePickerSpinner extends VBox {

    private int hour;
    private int minute;

    private final Label hourLabel;
    private final Label minuteLabel;

    // Bouton rond petit
    private static final String BTN =
            "-fx-background-color: #e8f0fe;" +
            "-fx-text-fill: #1c4f96;" +
            "-fx-font-weight: 900;" +
            "-fx-font-size: 13px;" +
            "-fx-background-radius: 50%;" +
            "-fx-min-width: 26px; -fx-max-width: 26px;" +
            "-fx-min-height: 26px; -fx-max-height: 26px;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 0;";

    private static final String BTN_HOVER =
            "-fx-background-color: #b8d0ff;" +
            "-fx-text-fill: #0f3a8a;" +
            "-fx-font-weight: 900;" +
            "-fx-font-size: 13px;" +
            "-fx-background-radius: 50%;" +
            "-fx-min-width: 26px; -fx-max-width: 26px;" +
            "-fx-min-height: 26px; -fx-max-height: 26px;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 0;";

    // Affichage du chiffre
    private static final String NUM =
            "-fx-background-color: white;" +
            "-fx-border-color: #d7e7ff;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-font-size: 20px;" +
            "-fx-font-weight: 800;" +
            "-fx-text-fill: #10233f;" +
            "-fx-alignment: center;" +
            "-fx-pref-width: 52px;" +
            "-fx-pref-height: 44px;" +
            "-fx-effect: dropshadow(gaussian, rgba(46,94,166,0.08), 6, 0, 0, 2);";

    public TimePickerSpinner() {
        this(LocalTime.of(10, 0));
    }

    public TimePickerSpinner(LocalTime initialTime) {
        this.hour   = initialTime.getHour();
        this.minute = initialTime.getMinute();

        setSpacing(0);
        setAlignment(Pos.CENTER_LEFT);

        hourLabel   = new Label(String.format("%02d", hour));
        minuteLabel = new Label(String.format("%02d", minute));
        hourLabel.setStyle(NUM);
        minuteLabel.setStyle(NUM);

        Label colon = new Label(":");
        colon.setStyle(
            "-fx-font-size: 24px;" +
            "-fx-font-weight: 900;" +
            "-fx-text-fill: #1c4f96;" +
            "-fx-padding: 0 4 6 4;"
        );

        // Colonnes heure / minute avec boutons +/-
        VBox hourCol   = buildColumn(hourLabel,
                () -> { hour   = (hour   + 1) % 24; refresh(); },
                () -> { hour   = (hour   + 23) % 24; refresh(); });

        VBox minuteCol = buildColumn(minuteLabel,
                () -> { minute = (minute + 5) % 60; refresh(); },
                () -> { minute = (minute + 55) % 60; refresh(); });

        HBox row = new HBox(8, hourCol, colon, minuteCol);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
            "-fx-background-color: #f9fbff;" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: #d7e7ff;" +
            "-fx-border-radius: 14;" +
            "-fx-padding: 10 16 10 16;"
        );

        getChildren().add(row);
    }

    private VBox buildColumn(Label display, Runnable onUp, Runnable onDown) {
        Button up   = makeBtn("▲", onUp);
        Button down = makeBtn("▼", onDown);
        VBox col = new VBox(4, up, display, down);
        col.setAlignment(Pos.CENTER);
        return col;
    }

    private Button makeBtn(String text, Runnable action) {
        Button b = new Button(text);
        b.setStyle(BTN);
        b.setOnAction(e -> action.run());
        b.setOnMouseEntered(e -> b.setStyle(BTN_HOVER));
        b.setOnMouseExited(e -> b.setStyle(BTN));
        return b;
    }

    private void refresh() {
        hourLabel.setText(String.format("%02d", hour));
        minuteLabel.setText(String.format("%02d", minute));
    }

    public String getTimeAsString() {
        return String.format("%02d:%02d", hour, minute);
    }

    public LocalTime getTimeAsLocalTime() {
        return LocalTime.of(hour, minute);
    }

    public void setTime(LocalTime time) {
        this.hour   = time.getHour();
        this.minute = time.getMinute();
        refresh();
    }

    public void reset() {
        setTime(LocalTime.now());
    }
}
