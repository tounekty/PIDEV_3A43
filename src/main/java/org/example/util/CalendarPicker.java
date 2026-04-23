package org.example.util;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;

/**
 * Calendrier interactif personnalisé avec sélection de date et indicateurs de disponibilité
 */
public class CalendarPicker extends VBox {
    private YearMonth currentMonth;
    private final ObjectProperty<LocalDate> selectedDate = new SimpleObjectProperty<>();
    private final Consumer<LocalDate> onDateSelected;
    private final Map<LocalDate, Integer> dateAvailability; // 0 = peu d'options, 1 = bcp d'options, 2 = complet
    private GridPane calendarGrid;
    private Label monthYearLabel;
    private final Set<Button> dateButtons = new LinkedHashSet<>();

    public CalendarPicker(Consumer<LocalDate> onDateSelected) {
        this.onDateSelected = onDateSelected;
        this.currentMonth = YearMonth.now();
        this.dateAvailability = new HashMap<>();
        this.setStyle("-fx-padding: 15; -fx-spacing: 15; -fx-background-color: white; -fx-border-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        this.setPrefSize(550, 500);

        initializeUI();
    }

    private void initializeUI() {
        // En-tête avec navigation
        HBox header = createHeader();
        this.getChildren().add(header);

        // Grille du calendrier
        calendarGrid = new GridPane();
        calendarGrid.setStyle("-fx-hgap: 8; -fx-vgap: 8;");
        calendarGrid.setAlignment(Pos.CENTER);

        refreshCalendar();
        this.getChildren().add(calendarGrid);

        // Légende
        HBox legend = createLegend();
        this.getChildren().add(legend);
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(10));
        header.setStyle("-fx-border-color: #D1E3FF; -fx-border-radius: 8; -fx-background-color: linear-gradient(to right, #F9FBFF, #E8F4FF);");

        Button prevButton = new Button("◀");
        prevButton.setStyle("-fx-font-size: 16; -fx-padding: 8 15; -fx-background-color: white; " +
                "-fx-border-color: #cfe3ff; -fx-border-radius: 6; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");
        prevButton.setOnAction(e -> previousMonth());
        prevButton.setOnMouseEntered(e -> prevButton.setStyle(prevButton.getStyle() + "-fx-background-color: #f0f7ff;"));
        prevButton.setOnMouseExited(e -> prevButton.setStyle(prevButton.getStyle().replace("-fx-background-color: #f0f7ff;", "-fx-background-color: white;")));

        monthYearLabel = new Label();
        monthYearLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        monthYearLabel.setStyle("-fx-text-fill: #1c4f96; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 2, 0, 0, 1);");

        Button nextButton = new Button("▶");
        nextButton.setStyle("-fx-font-size: 16; -fx-padding: 8 15; -fx-background-color: white; " +
                "-fx-border-color: #cfe3ff; -fx-border-radius: 6; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");
        nextButton.setOnAction(e -> nextMonth());
        nextButton.setOnMouseEntered(e -> nextButton.setStyle(nextButton.getStyle() + "-fx-background-color: #f0f7ff;"));
        nextButton.setOnMouseExited(e -> nextButton.setStyle(nextButton.getStyle().replace("-fx-background-color: #f0f7ff;", "-fx-background-color: white;")));

        // Bouton pour revenir au mois actuel
        Button todayButton = new Button("Aujourd'hui");
        todayButton.setStyle("-fx-font-size: 12; -fx-padding: 6 12; -fx-background-color: #4CAF50; " +
                "-fx-text-fill: white; -fx-border-radius: 4; -fx-cursor: hand;");
        todayButton.setOnAction(e -> goToToday());

        HBox navigationBox = new HBox(10);
        navigationBox.setAlignment(Pos.CENTER);
        navigationBox.getChildren().addAll(prevButton, todayButton, nextButton);

        header.getChildren().addAll(navigationBox, new javafx.scene.layout.Region(), monthYearLabel);
        HBox.setHgrow(header.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);

        return header;
    }

    private HBox createLegend() {
        HBox legend = new HBox(20);
        legend.setPadding(new Insets(10));
        legend.setStyle("-fx-border-color: #D1E3FF; -fx-border-radius: 8; -fx-background-color: linear-gradient(to right, #F9FBFF, #F0F8FF);");
        legend.setAlignment(Pos.CENTER);

        // Peu d'options (orange)
        VBox lowAvail = createLegendItem("■", "#FF9800", "Peu d'options (1-25%)");
        
        // Beaucoup d'options (cyan)
        VBox highAvail = createLegendItem("■", "#00BCD4", "Beaucoup d'options (25-99%)");
        
        // Complet (gris)
        VBox full = createLegendItem("■", "#9E9E9E", "Complet (0%)");
        
        // Disponible (vert)
        VBox available = createLegendItem("■", "#4CAF50", "Disponible (100%)");

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
        dateButtons.clear();

        monthYearLabel.setText(String.format("%s %d", 
                currentMonth.getMonth().toString(), currentMonth.getYear()));

        // En-têtes des jours avec style amélioré
        String[] dayHeaders = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(dayHeaders[i]);
            dayLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            dayLabel.setStyle("-fx-text-fill: #555; -fx-padding: 10; -fx-background-color: #F0F7FF; " +
                    "-fx-border-radius: 4; -fx-alignment: center;");
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setPrefWidth(60);
            dayLabel.setPrefHeight(35);
            calendarGrid.add(dayLabel, i, 0);
        }

        // Jours du calendrier
        LocalDate firstDayOfMonth = currentMonth.atDay(1);
        LocalDate lastDayOfMonth = currentMonth.atEndOfMonth();

        int dayOfWeek = firstDayOfMonth.getDayOfWeek().getValue() - 1; // Lundi = 0
        int dayCounter = 1;
        int row = 1;

        for (int i = 0; i < 42; i++) {
            int col = i % 7;
            
            if (i >= dayOfWeek && dayCounter <= lastDayOfMonth.getDayOfMonth()) {
                LocalDate date = firstDayOfMonth.plusDays(dayCounter - 1);
                Button dateButton = createDateButton(date, dayCounter);
                calendarGrid.add(dateButton, col, row);
                dayCounter++;
            } else {
                // Jours des mois précédents/suivants
                Label emptyLabel = new Label("");
                emptyLabel.setPrefSize(60, 60);
                emptyLabel.setStyle("-fx-background-color: transparent;");
                calendarGrid.add(emptyLabel, col, row);
            }
            
            if (col == 6) row++;
        }
    }

    private Button createDateButton(LocalDate date, int dayNumber) {
        Button dateButton = new Button(String.valueOf(dayNumber));
        dateButton.setPrefSize(60, 60);
        dateButton.setStyle("-fx-padding: 0; -fx-font-size: 14; -fx-font-weight: bold;");
        
        // Vérifier si c'est aujourd'hui
        boolean isToday = date.equals(LocalDate.now());
        
        // Déterminer la disponibilité
        int availability = dateAvailability.getOrDefault(date, 3); // Par défaut: disponible (100%)
        
        // Appliquer le style basé sur la disponibilité
        String baseStyle = "-fx-border-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1); ";
        
        if (availability == 0) {
            // Peu d'options (Orange) - 1-25%
            dateButton.setStyle(dateButton.getStyle() + baseStyle + 
                    "-fx-background-color: linear-gradient(to bottom, #FF9800, #F57C00); " +
                    "-fx-text-fill: white;");
        } else if (availability == 1) {
            // Beaucoup d'options (Cyan) - 25-99%
            dateButton.setStyle(dateButton.getStyle() + baseStyle + 
                    "-fx-background-color: linear-gradient(to bottom, #00BCD4, #0097A7); " +
                    "-fx-text-fill: white;");
        } else if (availability == 2) {
            // Complet (Gris) - 0%
            dateButton.setStyle(dateButton.getStyle() + 
                    "-fx-background-color: #9E9E9E; -fx-text-fill: white; " +
                    "-fx-border-radius: 8; -fx-cursor: not-allowed; -fx-opacity: 0.7;");
            dateButton.setDisable(true);
        } else {
            // Disponible (Vert) - 100%
            dateButton.setStyle(dateButton.getStyle() + baseStyle + 
                    "-fx-background-color: linear-gradient(to bottom, #4CAF50, #388E3C); " +
                    "-fx-text-fill: white;");
        }
        
        // Style spécial pour aujourd'hui
        if (isToday) {
            dateButton.setStyle(dateButton.getStyle() + 
                    "-fx-border-width: 2; -fx-border-color: #FF5722;");
        }

        // Effet de survol pour les boutons actifs
        if (!dateButton.isDisabled()) {
            final String originalStyle = dateButton.getStyle();
            dateButton.setOnMouseEntered(e -> {
                dateButton.setStyle(originalStyle + " -fx-scale-x: 1.05; -fx-scale-y: 1.05;");
            });
            dateButton.setOnMouseExited(e -> {
                dateButton.setStyle(originalStyle.replace(" -fx-scale-x: 1.05; -fx-scale-y: 1.05;", ""));
            });
        }

        // Sélection
        dateButton.setOnAction(e -> {
            selectedDate.set(date);
            onDateSelected.accept(date);
            updateSelectedDateStyle();
        });

        dateButtons.add(dateButton);
        return dateButton;
    }

    private void updateSelectedDateStyle() {
        for (Button btn : dateButtons) {
            // Supprimer le style de sélection précédent
            String style = btn.getStyle();
            style = style.replace("-fx-border-width: 3;", "")
                         .replace("-fx-border-color: #1976D2;", "")
                         .replace("-fx-effect: dropshadow(gaussian, rgba(25,118,210,0.3), 5, 0, 0, 2);", "");
            btn.setStyle(style);
        }

        if (selectedDate.get() != null) {
            // Trouver et mettre à jour le bouton sélectionné
            for (Button btn : dateButtons) {
                if (btn.getText().equals(String.valueOf(selectedDate.get().getDayOfMonth())) && 
                    selectedDate.get().getMonth() == currentMonth.getMonth()) {
                    btn.setStyle(btn.getStyle() + 
                            "-fx-border-width: 3; -fx-border-color: #1976D2; " +
                            "-fx-effect: dropshadow(gaussian, rgba(25,118,210,0.3), 5, 0, 0, 2);");
                    break;
                }
            }
        }
    }

    private void previousMonth() {
        currentMonth = currentMonth.minusMonths(1);
        refreshCalendar();
        updateSelectedDateStyle();
    }

    private void nextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        refreshCalendar();
        updateSelectedDateStyle();
    }

    public void goToToday() {
        currentMonth = YearMonth.now();
        selectedDate.set(LocalDate.now());
        refreshCalendar();
        updateSelectedDateStyle();
        onDateSelected.accept(LocalDate.now());
    }

    /**
     * Définir la disponibilité pour une date
     * @param date La date
     * @param availability 0 = peu d'options (orange), 1 = beaucoup d'options (cyan), 2 = complet (gris)
     */
    public void setDateAvailability(LocalDate date, int availability) {
        dateAvailability.put(date, availability);
    }

    /**
     * Définir les disponibilités pour plusieurs dates
     */
    public void setDateAvailabilities(Map<LocalDate, Integer> availabilities) {
        dateAvailability.clear();
        dateAvailability.putAll(availabilities);
        refreshCalendar();
        updateSelectedDateStyle();
    }

    /**
     * Obtenir la date sélectionnée
     */
    public LocalDate getSelectedDate() {
        return selectedDate.get();
    }

    /**
     * Définir la date sélectionnée
     */
    public void setSelectedDate(LocalDate date) {
        if (date != null) {
            currentMonth = YearMonth.from(date);
            refreshCalendar();
            selectedDate.set(date);
            updateSelectedDateStyle();
        }
    }

    /**
     * Obtenir la propriété de date sélectionnée
     */
    public ObjectProperty<LocalDate> selectedDateProperty() {
        return selectedDate;
    }

    /**
     * Effacer la sélection
     */
    public void clearSelection() {
        selectedDate.set(null);
        updateSelectedDateStyle();
    }

    /**
     * Obtenir le mois actuel affiché
     */
    public YearMonth getCurrentMonth() {
        return currentMonth;
    }

    /**
     * Aller à un mois spécifique
     */
    public void goToMonth(YearMonth month) {
        currentMonth = month;
        refreshCalendar();
        updateSelectedDateStyle();
    }

    /**
     * Aller à une date spécifique
     */
    public void goToDate(LocalDate date) {
        currentMonth = YearMonth.from(date);
        selectedDate.set(date);
        refreshCalendar();
        updateSelectedDateStyle();
        onDateSelected.accept(date);
    }

    /**
     * Obtenir toutes les dates avec disponibilité
     */
    public Map<LocalDate, Integer> getDateAvailabilities() {
        return new HashMap<>(dateAvailability);
    }

    /**
     * Vérifier si une date est disponible
     */
    public boolean isDateAvailable(LocalDate date) {
        Integer availability = dateAvailability.get(date);
        return availability != null && availability < 2; // 0 ou 1 = disponible
    }

    /**
     * Obtenir le niveau de disponibilité pour une date
     * @return 0=peu, 1=beaucoup, 2=complet, 3=disponible (100%)
     */
    public int getDateAvailabilityLevel(LocalDate date) {
        return dateAvailability.getOrDefault(date, 3);
    }
}
