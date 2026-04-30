package org.example.util;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Consumer;

/**
 * Calendrier interactif avec affichage horizontal des jours par semaine
 */
public class CalendarPicker extends VBox {
    private YearMonth currentMonth;
    private final ObjectProperty<LocalDate> selectedDate = new SimpleObjectProperty<>();
    private final Consumer<LocalDate> onDateSelected;
    private final Map<LocalDate, Integer> dateAvailability;
    private GridPane calendarGrid;
    private Label monthYearLabel;

    public CalendarPicker(Consumer<LocalDate> onDateSelected) {
        this.onDateSelected = onDateSelected;
        this.currentMonth = YearMonth.now();
        this.dateAvailability = new HashMap<>();
        this.setStyle("-fx-padding: 15; -fx-spacing: 15; -fx-background-color: white;");
        this.setPrefSize(1400, 550);
        initializeUI();
    }

    private void initializeUI() {
        HBox header = createHeader();
        this.getChildren().add(header);

        VBox calendarContainer = new VBox(20);
        calendarContainer.setPadding(new Insets(20));

        Label titleLabel = new Label("Vérifiez les disponibilités");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        titleLabel.setStyle("-fx-text-fill: #1976D2;");

        calendarGrid = new GridPane();
        calendarGrid.setStyle("-fx-hgap: 18; -fx-vgap: 20;");
        calendarGrid.setAlignment(Pos.CENTER_LEFT);
        calendarGrid.setPadding(new Insets(20));

        refreshCalendar();
        
        calendarContainer.getChildren().addAll(titleLabel, calendarGrid);
        this.getChildren().add(calendarContainer);

        HBox legend = createLegend();
        this.getChildren().add(legend);
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(10));
        header.setStyle("-fx-border-color: #D1E3FF; -fx-border-radius: 8;");

        monthYearLabel = new Label();
        monthYearLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        monthYearLabel.setStyle("-fx-text-fill: #1c4f96;");

        header.getChildren().add(monthYearLabel);
        return header;
    }

    private HBox createLegend() {
        HBox legend = new HBox(25);
        legend.setPadding(new Insets(15));
        legend.setAlignment(Pos.CENTER);

        VBox lowAvail = createLegendItem("■", "#FFB74D", "3 options");
        VBox highAvail = createLegendItem("■", "#4DD0E1", "5+ options");
        VBox full = createLegendItem("■", "#BDBDBD", "Complet");
        VBox available = createLegendItem("■", "#66BB6A", "Disponible");

        legend.getChildren().addAll(lowAvail, highAvail, full, available);
        return legend;
    }

    private VBox createLegendItem(String symbol, String color, String label) {
        VBox box = new VBox(3);
        box.setAlignment(Pos.CENTER);

        Label symbolLabel = new Label(symbol);
        symbolLabel.setStyle("-fx-font-size: 14; -fx-text-fill: " + color + ";");

        Label textLabel = new Label(label);
        textLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");

        box.getChildren().addAll(symbolLabel, textLabel);
        return box;
    }

    private void refreshCalendar() {
        calendarGrid.getChildren().clear();

        monthYearLabel.setText(String.format("%s %d", 
                currentMonth.getMonth().toString(), currentMonth.getYear()));

        LocalDate firstDay = currentMonth.atDay(1);
        LocalDate lastDay = currentMonth.atEndOfMonth();

        LocalDate currentDate = firstDay;
        int row = 0;

        while (!currentDate.isAfter(lastDay)) {
            for (int col = 0; col < 7; col++) {
                if (!currentDate.isAfter(lastDay)) {
                    VBox card = createDateCard(currentDate);
                    calendarGrid.add(card, col, row);
                    currentDate = currentDate.plusDays(1);
                } else {
                    break;
                }
            }
            row++;
        }
    }

    private VBox createDateCard(LocalDate date) {
        VBox card = new VBox(8);
        card.setPrefWidth(160);
        card.setPrefHeight(160);
        card.setSpacing(10);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER);

        int availability = dateAvailability.getOrDefault(date, 3);

        String[] dayNames = {"LUN.", "MAR.", "MER.", "JEU.", "VEN.", "SAM.", "DIM."};
        String dayName = date.equals(LocalDate.now()) ? "AUJOURD'HUI" : dayNames[date.getDayOfWeek().getValue() - 1];

        Label dayLabel = new Label(dayName);
        dayLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        dayLabel.setStyle("-fx-text-fill: inherit;");

        Label dateLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dateLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));
        dateLabel.setStyle("-fx-text-fill: inherit;");

        Label optionsLabel = new Label();
        optionsLabel.setFont(Font.font("Segoe UI", 11));
        optionsLabel.setStyle("-fx-text-fill: inherit;");

        String optionsText;
        if (availability == 0) {
            optionsText = "3 options";
        } else if (availability == 1) {
            optionsText = "5+ options";
        } else if (availability == 2) {
            optionsText = "Complet";
        } else {
            optionsText = "Disponible";
        }

        optionsLabel.setText(optionsText);

        String bgColor, textColor, borderColor;

        if (availability == 0) {
            bgColor = "linear-gradient(to bottom, #FFB74D, #FB8C00)";
            textColor = "white";
            borderColor = "#F57C00";
        } else if (availability == 1) {
            bgColor = "linear-gradient(to bottom, #4DD0E1, #00ACC1)";
            textColor = "white";
            borderColor = "#0097A7";
        } else if (availability == 2) {
            bgColor = "#BDBDBD";
            textColor = "rgba(255,255,255,0.95)";
            borderColor = "#757575";
        } else {
            bgColor = "linear-gradient(to bottom, #66BB6A, #43A047)";
            textColor = "white";
            borderColor = "#2E7D32";
        }

        card.setStyle("-fx-background-color: " + bgColor + "; " +
                "-fx-border-color: " + borderColor + "; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 8; " +
                "-fx-cursor: hand;");

        dayLabel.setStyle("-fx-text-fill: " + textColor + ";");
        dateLabel.setStyle("-fx-text-fill: " + textColor + ";");
        optionsLabel.setStyle("-fx-text-fill: " + textColor + ";");

        if (date.equals(LocalDate.now())) {
            card.setStyle(card.getStyle() + " -fx-border-width: 3; -fx-border-color: #FF6F00;");
        }

        if (availability == 2) {
            card.setDisable(true);
            card.setOpacity(0.7);
        }

        if (!card.isDisabled()) {
            card.setOnMouseEntered(e -> {
                card.setScaleX(1.1);
                card.setScaleY(1.1);
            });
            card.setOnMouseExited(e -> {
                card.setScaleX(1.0);
                card.setScaleY(1.0);
            });
        }

        card.setOnMouseClicked(e -> {
            if (!card.isDisabled()) {
                selectedDate.set(date);
                onDateSelected.accept(date);
            }
        });

        card.getChildren().addAll(dayLabel, dateLabel, optionsLabel);
        return card;
    }

    public void setDateAvailabilities(Map<LocalDate, Integer> availabilities) {
        this.dateAvailability.clear();
        this.dateAvailability.putAll(availabilities);
        refreshCalendar();
    }

    public void clearSelection() {
        selectedDate.set(null);
    }

    public LocalDate getSelectedDate() {
        return selectedDate.get();
    }

    public void goToToday() {
        currentMonth = YearMonth.now();
        selectedDate.set(LocalDate.now());
        refreshCalendar();
        onDateSelected.accept(LocalDate.now());
    }

    private void previousMonth() {
        currentMonth = currentMonth.minusMonths(1);
        refreshCalendar();
    }

    private void nextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        refreshCalendar();
    }

    public ObjectProperty<LocalDate> selectedDateProperty() {
        return selectedDate;
    }

    public YearMonth getCurrentMonth() {
        return currentMonth;
    }

    public void goToMonth(YearMonth month) {
        currentMonth = month;
        refreshCalendar();
    }

    public void setDateAvailability(LocalDate date, int availability) {
        dateAvailability.put(date, availability);
    }

    public int getDateAvailabilityLevel(LocalDate date) {
        return dateAvailability.getOrDefault(date, 3);
    }
}
