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
    private final java.util.Set<YearMonth> loadedMonths = new java.util.HashSet<>();
    private List<Event> allEventsCache = null;

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
        Button previousYearButton = createNavigationButton("«");
        previousYearButton.setOnAction(event -> {
            currentMonth = currentMonth.minusYears(1);
            ensureAvailabilityLoaded();
            refreshMonths();
        });

        Button previousButton = createNavigationButton("‹");
        previousButton.setOnAction(event -> {
            currentMonth = currentMonth.minusMonths(1);
            ensureAvailabilityLoaded();
            refreshMonths();
        });

        Button nextButton = createNavigationButton("›");
        nextButton.setOnAction(event -> {
            currentMonth = currentMonth.plusMonths(1);
            ensureAvailabilityLoaded();
            refreshMonths();
        });

        Button nextYearButton = createNavigationButton("»");
        nextYearButton.setOnAction(event -> {
            currentMonth = currentMonth.plusYears(1);
            ensureAvailabilityLoaded();
            refreshMonths();
        });

        Button todayButton = createNavigationButton("Aujourd'hui");
        todayButton.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        todayButton.setMinSize(90, 36);
        todayButton.setPrefSize(90, 36);
        todayButton.setStyle("-fx-background-color: #0f69ff; -fx-text-fill: white; "
                + "-fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: 700;");
        todayButton.setOnAction(event -> {
            currentMonth = YearMonth.now();
            ensureAvailabilityLoaded();
            refreshMonths();
        });

        firstMonthTitle = createMonthTitleLabel();
        secondMonthTitle = createMonthTitleLabel();

        HBox monthTitles = new HBox(48, firstMonthTitle, secondMonthTitle);
        monthTitles.setAlignment(Pos.CENTER);
        HBox.setHgrow(monthTitles, Priority.ALWAYS);

        HBox header = new HBox(8,
                previousYearButton, previousButton,
                monthTitles,
                todayButton,
                nextButton, nextYearButton);
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
                createLegendItem("#22c55e", "Disponible"),
                createLegendItem("#f59e0b", "Peu de places"),
                createLegendItem("#ef4444", "Complet"));
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
        int availability = availabilityMap.getOrDefault(date, -1);
        button.getProperties().put("calendarDate", date);
        button.getProperties().put("availability", availability);

        if (availability == -1) {
            // Pas d'événement — neutre, barre invisible
            availabilityBar.setOpacity(0);
            button.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: transparent;" +
                "-fx-border-radius: 12;");
            if (button.getGraphic() instanceof VBox vbox) {
                for (var node : vbox.getChildren()) {
                    if (node instanceof Label lbl) {
                        lbl.setStyle("-fx-text-fill: #243b6c; -fx-font-weight: 700; -fx-font-size: 15;");
                    }
                }
            }
            return;
        }

        if (availability == 2) {
            // 🔴 Complet
            availabilityBar.setOpacity(1);
            availabilityBar.setStyle("-fx-background-color: #ef4444; -fx-background-radius: 99;");
            button.setStyle(
                "-fx-background-color: #fff1f1;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #fecaca;" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;" +
                "-fx-cursor: default;");
            if (button.getGraphic() instanceof VBox vbox) {
                for (var node : vbox.getChildren()) {
                    if (node instanceof Label lbl) {
                        lbl.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: 700; -fx-font-size: 15;");
                    }
                }
            }
            button.setOnAction(null);
            button.getStyleClass().add("is-full");
            return;
        }

        if (availability == 0) {
            // 🟡 Peu de places
            availabilityBar.setOpacity(1);
            availabilityBar.setStyle("-fx-background-color: #f59e0b; -fx-background-radius: 99;");
            button.setStyle(
                "-fx-background-color: #fffbeb;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #fde68a;" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;");
            if (button.getGraphic() instanceof VBox vbox) {
                for (var node : vbox.getChildren()) {
                    if (node instanceof Label lbl) {
                        lbl.setStyle("-fx-text-fill: #92400e; -fx-font-weight: 700; -fx-font-size: 15;");
                    }
                }
            }
            button.getStyleClass().add("is-limited");
            return;
        }

        // 🟢 Disponible
        availabilityBar.setOpacity(1);
        availabilityBar.setStyle("-fx-background-color: #22c55e; -fx-background-radius: 99;");
        button.setStyle(
            "-fx-background-color: #f0fdf4;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #bbf7d0;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1;");
        if (button.getGraphic() instanceof VBox vbox) {
            for (var node : vbox.getChildren()) {
                if (node instanceof Label lbl) {
                    lbl.setStyle("-fx-text-fill: #166534; -fx-font-weight: 700; -fx-font-size: 15;");
                }
            }
        }
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
        loadedMonths.clear();
        allEventsCache = null;
        try {
            List<Event> allEvents = eventService.getAllEvents();
            allEventsCache = allEvents;

            // Compute for every date that has an event
            for (Event event : allEvents) {
                LocalDate d = event.getDateEvent().toLocalDate();
                loadedMonths.add(YearMonth.from(d));
            }
            // Also pre-load the currently visible months
            for (int offset = 0; offset <= 1; offset++) {
                loadedMonths.add(currentMonth.plusMonths(offset));
            }

            // Compute availability for all loaded months
            for (YearMonth month : loadedMonths) {
                for (LocalDate date = month.atDay(1); !date.isAfter(month.atEndOfMonth()); date = date.plusDays(1)) {
                    availabilityMap.put(date, computeAvailabilityForDate(allEvents, date));
                }
            }
        } catch (Exception exception) {
            System.err.println("Erreur lors du chargement des disponibilites: " + exception.getMessage());
        }
        refreshMonths();
    }

    /**
     * Ensures the currently visible months have availability data loaded.
     * Called when navigating to avoid blank months.
     */
    private void ensureAvailabilityLoaded() {
        if (!hasDataServices()) return;
        boolean needsLoad = false;
        for (int offset = 0; offset <= 1; offset++) {
            YearMonth m = currentMonth.plusMonths(offset);
            if (!loadedMonths.contains(m)) {
                needsLoad = true;
                break;
            }
        }
        if (needsLoad) {
            try {
                List<Event> events = allEventsCache != null ? allEventsCache : eventService.getAllEvents();
                if (allEventsCache == null) allEventsCache = events;
                for (int offset = 0; offset <= 1; offset++) {
                    YearMonth m = currentMonth.plusMonths(offset);
                    if (!loadedMonths.contains(m)) {
                        for (LocalDate d = m.atDay(1); !d.isAfter(m.atEndOfMonth()); d = d.plusDays(1)) {
                            availabilityMap.put(d, computeAvailabilityForDate(events, d));
                        }
                        loadedMonths.add(m);
                    }
                }
            } catch (Exception ex) {
                System.err.println("Erreur chargement disponibilités: " + ex.getMessage());
            }
        }
    }

    private int computeAvailabilityForDate(List<Event> events, LocalDate date) {
        boolean hasEvent = false;
        boolean allFull = true;
        boolean hasLimited = false;

        for (Event event : events) {
            if (!event.getDateEvent().toLocalDate().equals(date)) continue;

            int capacity = event.getCapacite();
            if (capacity <= 0) continue;

            hasEvent = true;
            int reserved = 0;
            try {
                reserved = reservationService.getConfirmedCountByEvent(event.getId());
            } catch (Exception ignored) {}

            int remaining = capacity - reserved;

            if (remaining <= 0) {
                // complet — ne pas changer allFull
            } else {
                allFull = false;
                double ratio = (double) remaining / capacity;
                // Orange si moins de 75% des places restantes (au moins 1 réservation)
                if (ratio < 0.75) hasLimited = true;
            }
        }

        if (!hasEvent) return -1;         // pas d'événement → neutre (gris)
        if (allFull)   return 2;          // complet → rouge
        if (hasLimited) return 0;         // peu de places → orange
        return 1;                         // disponible → vert
    }

    public void setDateAvailabilities(Map<LocalDate, Integer> availabilities) {
        availabilityMap.clear();
        loadedMonths.clear();
        allEventsCache = null;
        if (availabilities != null) {
            availabilityMap.putAll(availabilities);
        }
        refreshMonths();
    }

    /**
     * Force a full reload from the database — call this after reservations change.
     */
    public void reloadFromDatabase() {
        if (hasDataServices()) {
            loadAvailabilityData();
        }
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
