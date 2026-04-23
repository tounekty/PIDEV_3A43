package org.example.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.Map;

/**
 * Dialogue moderne pour afficher le calendrier a deux mois et selectionner une date.
 */
public class CalendarDialog extends Dialog<LocalDate> {
    private final DualMonthCalendarView calendarView;
    private LocalDate selectedDate;

    public CalendarDialog() {
        setTitle("Selection de date");

        calendarView = new DualMonthCalendarView(date -> selectedDate = date);
        calendarView.setPrefWidth(760);

        VBox content = new VBox(18, createHeader(), calendarView);
        content.setPadding(new Insets(22));
        content.getStyleClass().add("calendar-dialog-root");

        getDialogPane().setContent(content);
        getDialogPane().getStyleClass().add("calendar-dialog-pane");
        var stylesheet = getClass().getResource("/calendar-styles.css");
        if (stylesheet != null) {
            getDialogPane().getStylesheets().add(stylesheet.toExternalForm());
        }
        getDialogPane().setPrefWidth(820);
        getDialogPane().setPrefHeight(610);
        getDialogPane().getButtonTypes().addAll(
                new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );

        styleDialogButtons();
        setResultConverter(buttonType -> buttonType != null
                && buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE ? selectedDate : null);
    }

    private HBox createHeader() {
        Label title = new Label("Selectionnez une date");
        title.getStyleClass().add("calendar-dialog-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeButton = new Button("x");
        closeButton.getStyleClass().add("calendar-dialog-close");
        closeButton.setOnAction(event -> {
            selectedDate = null;
            close();
        });

        HBox header = new HBox(12, title, spacer, closeButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("calendar-dialog-header");
        return header;
    }

    private void styleDialogButtons() {
        Button confirmButton = (Button) getDialogPane().lookupButton(
                getDialogPane().getButtonTypes().stream()
                        .filter(type -> type.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                        .findFirst()
                        .orElseThrow()
        );
        confirmButton.setDisable(true);
        confirmButton.getStyleClass().add("calendar-dialog-confirm");

        Button cancelButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("calendar-dialog-cancel");

        calendarView.setOnDateSelected(date -> {
            selectedDate = date;
            confirmButton.setDisable(date == null);
        });
    }

    public void setDateAvailability(LocalDate date, int availability) {
        calendarView.setDateAvailability(date, availability);
    }

    public void setDateAvailabilities(Map<LocalDate, Integer> availabilities) {
        calendarView.setDateAvailabilities(availabilities);
    }

    public void setDefaultDate(LocalDate date) {
        selectedDate = date;
        calendarView.setSelectedDate(date);
    }
}
