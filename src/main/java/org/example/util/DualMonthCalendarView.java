package org.example.util;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.example.event.Event;
import org.example.event.EventService;
import org.example.reservation.ReservationService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Calendrier moderne a deux mois avec indicateurs de disponibilite.
 */
public class DualMonthCalendarView extends VBox {
    private static final Locale FRENCH = Locale.FRENCH;
    private static final String[] DAY_HEADERS = {"lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim."};

    private final EventService eventService;
    private final ReservationService reservationService;
    private final Map<LocalDate, Integer> availabilityMap = new HashMap<>();
    private final Map<LocalDate, Button> activeDateButtons = new LinkedHashMap<>();

    private YearMonth currentMonth;
    private LocalDate selectedDate;
    private Consumer<LocalDate> onDateSelected;

    private HBox monthsWrapper;
    private Label firstMonthTitle;
    private Label secondMonthTitle;

    public DualMonthCalendarView(EventService eventService, ReservationService reservationService) {
        this(eventService, reservationService, null);
    }

    public DualMonthCalendarView(EventService eventService, ReservationService reservationService,
                                 Consumer<LocalDate> onDateSelected) {
        this.eventService = eventService;
        this.reservationService = reservationService;
        this.onDateSelected = onDateSelected;
        this.currentMonth = YearMonth.now();
        configureRoot();
        buildUi();
        if (hasDataServices()) {
            loadAvailabilityData();
        } else {
            refreshMonths();
        }
    }

    public DualMonthCalendarView(Consumer<LocalDate> onDateSelected) {
        this(null, null, onDateSelected);
    }

    private void configureRoot() {
        getStyleClass().add("dual-calendar-root");
        setSpacing(18);
        setPadding(new Insets(0));
        setFillWidth(true);
    }

    private void buildUi() {
        HBox navigationHeader = createNavigationHeader();

        monthsWrapper = new HBox(22);
        monthsWrapper.setAlignment(Pos.TOP_CENTER);

        HBox legend = createLegend();

        getChildren().setAll(navigationHeader, monthsWrapper, legend);
    }

    private HBox createNavigationHeader() {
        Button previousButton = createNavigationButton("<");
        previousButton.setOnAction(event -> {
            currentMonth = currentMonth.minusMonths(1);
            refreshMonths();
        });

        Button nextButton = createNavigationButton(">");
        nextButton.setOnAction(event -> {
            currentMonth = currentMonth.plusMonths(1);
            refreshMonths();
        });

        firstMonthTitle = createMonthTitleLabel();
        secondMonthTitle = createMonthTitleLabel();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox monthTitles = new HBox(48, firstMonthTitle, secondMonthTitle);
        monthTitles.setAlignment(Pos.CENTER);

        HBox header = new HBox(16, previousButton, monthTitles, spacer, nextButton);
        header.getStyleClass().add("dual-calendar-header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Button createNavigationButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("dual-calendar-nav");
        button.setFont(Font.font("Segoe UI Symbol", FontWeight.NORMAL, 24));
        button.setMinSize(42, 42);
        button.setPrefSize(42, 42);
        return button;
    }

    private Label createMonthTitleLabel() {
        Label label = new Label();
        label.getStyleClass().add("dual-calendar-month-title");
        label.setMinWidth(180);
        label.setAlignment(Pos.CENTER);
        return label;
    }

    private HBox createLegend() {
        HBox legend = new HBox(28,
                createLegendItem("#f4bc42", "Peu d'options disponibles"),
                createLegendItem("#55d1b4", "La plupart des options sont disponibles"));
        legend.getStyleClass().add("dual-calendar-legend");
        legend.setAlignment(Pos.CENTER_LEFT);
        return legend;
    }

    private HBox createLegendItem(String color, String labelText) {
        Region bar = new Region();
        bar.setPrefSize(34, 4);
        bar.setMinSize(34, 4);
        bar.setMaxSize(34, 4);
        bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 99;");

        Label label = new Label(labelText);
        label.getStyleClass().add("dual-calendar-legend-label");

        HBox item = new HBox(10, bar, label);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private void refreshMonths() {
        activeDateButtons.clear();
        YearMonth secondMonth = currentMonth.plusMonths(1);

        firstMonthTitle.setText(formatMonth(currentMonth));
        secondMonthTitle.setText(formatMonth(secondMonth));

        monthsWrapper.getChildren().setAll(
                createMonthPanel(currentMonth),
                createMonthPanel(secondMonth)
        );
        updateSelectedDateStyle();
    }

    private VBox createMonthPanel(YearMonth yearMonth) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("dual-calendar-grid");
        grid.setAlignment(Pos.TOP_CENTER);

        for (int col = 0; col < DAY_HEADERS.length; col++) {
            Label dayHeader = new Label(DAY_HEADERS[col]);
            dayHeader.getStyleClass().add("dual-calendar-day-header");
            dayHeader.setMaxWidth(Double.MAX_VALUE);
            GridPane.setFillWidth(dayHeader, true);
            GridPane.setHalignment(dayHeader, HPos.CENTER);
            grid.add(dayHeader, col, 0);
        }

        LocalDate firstVisibleDate = yearMonth.atDay(1)
                .minusDays(yearMonth.atDay(1).getDayOfWeek().getValue() - 1L);

        for (int index = 0; index < 42; index++) {
            LocalDate date = firstVisibleDate.plusDays(index);
            int row = (index / 7) + 1;
            int col = index % 7;
            grid.add(createDateCell(yearMonth, date), col, row);
        }

        VBox monthPanel = new VBox(14, grid);
        monthPanel.getStyleClass().add("dual-calendar-month-panel");
        HBox.setHgrow(monthPanel, Priority.ALWAYS);
        return monthPanel;
    }

    private Node createDateCell(YearMonth visibleMonth, LocalDate date) {
        boolean currentMonthDate = YearMonth.from(date).equals(visibleMonth);

        Button button = new Button();
        button.getStyleClass().add("dual-calendar-date");
        button.setMinSize(52, 58);
        button.setPrefSize(52, 58);
        button.setMaxWidth(Double.MAX_VALUE);

        Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
        dayNumber.getStyleClass().add("dual-calendar-date-number");

        Region availabilityBar = new Region();
        availabilityBar.setMinHeight(4);
        availabilityBar.setPrefHeight(4);
        availabilityBar.setMaxWidth(Double.MAX_VALUE);
        availabilityBar.getStyleClass().add("dual-calendar-availability-bar");

        VBox content = new VBox(9, dayNumber, availabilityBar);
        content.setAlignment(Pos.CENTER);
        button.setGraphic(content);

        if (!currentMonthDate) {
            button.getStyleClass().add("is-outside-month");
            dayNumber.getStyleClass().add("is-outside-month");
            availabilityBar.setOpacity(0);
            button.setDisable(true);
        } else {
            applyAvailabilityStyle(button, availabilityBar, date);
            button.setOnAction(event -> handleDateSelection(date));
            activeDateButtons.put(date, button);
        }

        if (date.equals(LocalDate.now())) {
            button.getStyleClass().add("is-today");
        }

        return button;
    }

    private void applyAvailabilityStyle(Button button, Region availabilityBar, LocalDate date) {
        int availability = availabilityMap.getOrDefault(date, 1);
        button.getProperties().put("calendarDate", date);
        button.getProperties().put("availability", availability);

        if (availability == 2) {
            availabilityBar.setStyle("-fx-background-color: #d0d7e5; -fx-background-radius: 99;");
            button.getStyleClass().add("is-full");
            button.setDisable(true);
            return;
        }

        if (availability == 0) {
            availabilityBar.setStyle("-fx-background-color: #f4bc42; -fx-background-radius: 99;");
            button.getStyleClass().add("is-limited");
            return;
        }

        availabilityBar.setStyle("-fx-background-color: #55d1b4; -fx-background-radius: 99;");
        button.getStyleClass().add("is-available");
    }

    private void handleDateSelection(LocalDate date) {
        selectedDate = date;
        updateSelectedDateStyle();
        if (onDateSelected != null) {
            onDateSelected.accept(date);
        }
    }

    private void updateSelectedDateStyle() {
        for (Map.Entry<LocalDate, Button> entry : activeDateButtons.entrySet()) {
            Button button = entry.getValue();
            button.getStyleClass().remove("is-selected");
            if (entry.getKey().equals(selectedDate)) {
                button.getStyleClass().add("is-selected");
            }
        }
    }

    private String formatMonth(YearMonth yearMonth) {
        String month = yearMonth.getMonth().getDisplayName(TextStyle.FULL, FRENCH);
        return month.substring(0, 1).toUpperCase(FRENCH) + month.substring(1) + " " + yearMonth.getYear();
    }

    private boolean hasDataServices() {
        return eventService != null && reservationService != null;
    }

    private void loadAvailabilityData() {
        availabilityMap.clear();
        try {
            List<Event> allEvents = eventService.getAllEvents();
            for (int monthOffset = -1; monthOffset <= 2; monthOffset++) {
                YearMonth month = currentMonth.plusMonths(monthOffset);
                for (LocalDate date = month.atDay(1); !date.isAfter(month.atEndOfMonth()); date = date.plusDays(1)) {
                    availabilityMap.put(date, computeAvailabilityForDate(allEvents, date));
                }
            }
        } catch (Exception exception) {
            System.err.println("Erreur lors du chargement des disponibilites: " + exception.getMessage());
        }
        refreshMonths();
    }

    private int computeAvailabilityForDate(List<Event> events, LocalDate date) {
        int totalCapacity = 0;
        int totalReserved = 0;
        boolean hasEvent = false;

        for (Event event : events) {
            if (!event.getDateEvent().toLocalDate().equals(date)) {
                continue;
            }
            hasEvent = true;
            totalCapacity += event.getCapacite();
            try {
                totalReserved += reservationService.getReservationCountByEvent(event.getId());
            } catch (Exception ignored) {
                // On garde une valeur conservative si le comptage echoue.
            }
        }

        if (!hasEvent || totalCapacity <= 0) {
            return 1;
        }

        int remaining = totalCapacity - totalReserved;
        if (remaining <= 0) {
            return 2;
        }
        double ratio = (double) remaining / totalCapacity;
        return ratio < 0.25 ? 0 : 1;
    }

    public void setDateAvailabilities(Map<LocalDate, Integer> availabilities) {
        availabilityMap.clear();
        if (availabilities != null) {
            availabilityMap.putAll(availabilities);
        }
        refreshMonths();
    }

    public void setDateAvailability(LocalDate date, int availability) {
        availabilityMap.put(date, availability);
        refreshMonths();
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
        if (selectedDate != null) {
            this.currentMonth = YearMonth.from(selectedDate);
        }
        refreshMonths();
    }

    public void clearSelection() {
        selectedDate = null;
        updateSelectedDateStyle();
    }

    public void goToToday() {
        currentMonth = YearMonth.now();
        refreshMonths();
    }

    public void setOnDateSelected(Consumer<LocalDate> callback) {
        this.onDateSelected = callback;
    }
}
