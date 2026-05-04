package com.mindcare.legacy.admin;

import com.mindcare.components.BadgeLabel;
import com.mindcare.model.Appointment;
import com.mindcare.model.PatientFile;
import com.mindcare.model.User;
import com.mindcare.services.AppointmentService;
import com.mindcare.utils.NavigationManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GestionReservationsLegacyContent implements NavigationManager.Buildable {

    private final AppointmentService appointmentService = new AppointmentService();
    private final ObservableList<Appointment> appointments = FXCollections.observableArrayList();

    @Override
    public Node build() {
        loadAppointments();
        return buildContent();
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);
        content.getStyleClass().add("module-content");
        content.setPadding(new Insets(28, 32, 28, 32));

        Label title = new Label("Gestion rendez-vous");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Admin: creer, modifier, supprimer les rendez-vous et consulter le dossier etudiant lie");
        sub.getStyleClass().add("page-subtitle");

        Button addBtn = new Button("+ Nouveau rendez-vous");
        addBtn.getStyleClass().addAll("btn", "btn-primary");
        addBtn.setOnAction(e -> openEditor(null));

        Button statsBtn = new Button("Statistique");
        statsBtn.getStyleClass().addAll("btn", "btn-secondary");
        statsBtn.setOnAction(e -> NavigationManager.getInstance()
            .navigateContent("Statistiques rendez-vous", new GestionReservationsStatsLegacyContent()));

        HBox headerRow = new HBox();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        HBox headerActions = new HBox(8, statsBtn, addBtn);
        headerActions.setAlignment(Pos.CENTER_RIGHT);
        headerRow.getChildren().addAll(new VBox(6, title, sub), spacer, headerActions);

        content.getChildren().addAll(headerRow, buildTable());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox buildTable() {
        TextField searchField = new TextField();
        searchField.setPromptText("Recherche: etudiant, psychologue, statut, lieu...");
        searchField.getStyleClass().add("module-search-field");

        ComboBox<String> sortBox = new ComboBox<>();
        sortBox.getItems().addAll(
            "Date (plus recente)",
            "Date (plus ancienne)",
            "Etudiant (A-Z)",
            "Psychologue (A-Z)",
            "Statut (A-Z)"
        );
        sortBox.setValue("Date (plus recente)");
        sortBox.getStyleClass().add("module-sort-box");

        HBox filtersRow = new HBox(10, searchField, sortBox);
        filtersRow.getStyleClass().add("module-filters-row");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox cardsContainer = new VBox(10);
        cardsContainer.getStyleClass().add("appointment-cards-container");
        ScrollPane cardsScroll = new ScrollPane(cardsContainer);
        cardsScroll.setFitToWidth(true);
        cardsScroll.setPrefHeight(480);
        cardsScroll.getStyleClass().add("scroll-pane");

        FilteredList<Appointment> filtered = new FilteredList<>(appointments, a -> true);
        SortedList<Appointment> sorted = new SortedList<>(filtered);
        sorted.setComparator((a, b) -> compareByOption(a, b, sortBox.getValue()));

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase();
            filtered.setPredicate(appointment -> {
                if (query.isEmpty()) {
                    return true;
                }
                return contains(appointment.getLocation(), query)
                    || contains(appointment.getStatus(), query)
                    || contains(appointment.getStudentName(), query)
                    || contains(appointment.getPsyName(), query)
                    || contains(appointment.getDossierReference(), query)
                    || contains(appointment.getDateTimeDisplay(), query);
            });
        });

        sortBox.valueProperty().addListener((obs, oldValue, newValue) ->
            sorted.setComparator((a, b) -> compareByOption(a, b, newValue))
        );

        Runnable refreshCards = () -> {
            cardsContainer.getChildren().clear();
            for (Appointment appointment : sorted) {
                cardsContainer.getChildren().add(buildAdminAppointmentCard(appointment));
            }
            if (cardsContainer.getChildren().isEmpty()) {
                Label empty = new Label("Aucun rendez-vous trouve.");
                empty.getStyleClass().add("label-muted");
                cardsContainer.getChildren().add(empty);
            }
        };

        sorted.addListener((ListChangeListener<Appointment>) change -> refreshCards.run());
        refreshCards.run();

        VBox card = new VBox(10, filtersRow, cardsScroll);
        card.getStyleClass().addAll("card", "appointments-board");
        card.setPadding(new Insets(10));
        return card;
    }

    private VBox buildAdminAppointmentCard(Appointment current) {
        Label dateLabel = new Label("Date/Heure: " + emptySafe(current.getDateTimeDisplay()));
        Label locationLabel = new Label("Lieu: " + emptySafe(current.getLocation()));
        Label studentLabel = new Label("Etudiant: " + displayUser(current.getStudentName(), current.getStudentId()));
        Label dossierLabel = new Label("Dossier Etudiant: " + current.getDossierReference());
        Label psyLabel = new Label("Psychologue: " + displayUser(current.getPsyName(), current.getPsyId()));

        BadgeLabel statusBadge = BadgeLabel.forStatus(emptySafe(current.getStatus()).toUpperCase());
        HBox statusRow = new HBox(8, new Label("Statut:"), statusBadge);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Button viewDossierBtn = new Button("Voir Dossier");
        viewDossierBtn.getStyleClass().addAll("btn", "btn-info", "btn-sm");
        viewDossierBtn.setOnAction(e -> viewPatientFile(current));

        Button detailsBtn = new Button("Détails");
        detailsBtn.getStyleClass().addAll("btn", "btn-primary", "btn-sm");
        detailsBtn.setOnAction(e -> showAppointmentDetails(current));

        Button editBtn = new Button("Modifier");
        editBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        editBtn.setOnAction(e -> openEditor(current));

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.getStyleClass().addAll("btn", "btn-danger", "btn-sm");
        deleteBtn.setOnAction(e -> deleteAppointment(current));

        HBox topActions = new HBox(6, viewDossierBtn, detailsBtn);
        HBox bottomActions = new HBox(6, editBtn, deleteBtn);

        VBox card = new VBox(8, dateLabel, locationLabel, studentLabel, dossierLabel, psyLabel, statusRow, topActions, bottomActions);
        card.getStyleClass().addAll("card", "appointment-card");
        card.setPadding(new Insets(12));
        return card;
    }

    private void loadAppointments() {
        appointments.setAll(appointmentService.findAll());
    }

    private void showStatisticsDialog() {
        loadAppointments();
        List<Appointment> data = new ArrayList<>(appointments);

        int total = data.size();
        int pending = countByStatus(data, "pending");
        int accepted = countByStatus(data, "accepted");
        int rejected = countByStatus(data, "rejected");
        int cancelled = countByStatus(data, "cancelled");
        int completed = countByStatus(data, "completed");
        String acceptanceRate = formatRate(accepted, total);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Statistiques des rendez-vous");
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/mindcare/styles/orion-theme.css").toExternalForm()
        );
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label globalTitle = new Label("Global KPI cards");
        globalTitle.getStyleClass().add("card-title");

        GridPane kpiGrid = new GridPane();
        kpiGrid.setHgap(10);
        kpiGrid.setVgap(10);
        kpiGrid.add(buildKpiCard("Total appointments", String.valueOf(total)), 0, 0);
        kpiGrid.add(buildKpiCard("Pending", String.valueOf(pending)), 1, 0);
        kpiGrid.add(buildKpiCard("Accepted", String.valueOf(accepted)), 2, 0);
        kpiGrid.add(buildKpiCard("Rejected", String.valueOf(rejected)), 0, 1);
        kpiGrid.add(buildKpiCard("Cancelled", String.valueOf(cancelled)), 1, 1);
        kpiGrid.add(buildKpiCard("Completed", String.valueOf(completed)), 2, 1);
        kpiGrid.add(buildKpiCard("Acceptance rate", acceptanceRate), 0, 2);

        Label trendTitle = new Label("Time trend (weekly/monthly)");
        trendTitle.getStyleClass().add("card-title");

        TableView<TrendRow> weeklyTable = new TableView<>();
        weeklyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        weeklyTable.setFixedCellSize(36);
        weeklyTable.setPrefHeight(220);

        TableColumn<TrendRow, String> wPeriodCol = new TableColumn<>("Jour");
        wPeriodCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPeriod()));
        TableColumn<TrendRow, String> wAppointmentsCol = new TableColumn<>("Appointments created per day");
        wAppointmentsCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getTotal())));
        TableColumn<TrendRow, String> wCompletedCol = new TableColumn<>("Completed per day");
        wCompletedCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getCompleted())));
        TableColumn<TrendRow, String> wCancelledCol = new TableColumn<>("Cancellation trend");
        wCancelledCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getCancelled())));
        weeklyTable.getColumns().addAll(wPeriodCol, wAppointmentsCol, wCompletedCol, wCancelledCol);
        weeklyTable.setItems(FXCollections.observableArrayList(buildWeeklyTrendRows(data)));

        TableView<TrendRow> monthlyTable = new TableView<>();
        monthlyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        monthlyTable.setFixedCellSize(36);
        monthlyTable.setPrefHeight(220);

        TableColumn<TrendRow, String> mPeriodCol = new TableColumn<>("Mois");
        mPeriodCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPeriod()));
        TableColumn<TrendRow, String> mAppointmentsCol = new TableColumn<>("Appointments");
        mAppointmentsCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getTotal())));
        TableColumn<TrendRow, String> mCompletedCol = new TableColumn<>("Completed");
        mCompletedCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getCompleted())));
        TableColumn<TrendRow, String> mCancelledCol = new TableColumn<>("Cancelled");
        mCancelledCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getCancelled())));
        monthlyTable.getColumns().addAll(mPeriodCol, mAppointmentsCol, mCompletedCol, mCancelledCol);
        monthlyTable.setItems(FXCollections.observableArrayList(buildMonthlyTrendRows(data)));

        Label perfTitle = new Label("Psychologue performance table");
        perfTitle.getStyleClass().add("card-title");

        TableView<PsychologuePerformanceRow> perfTable = new TableView<>();
        perfTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        perfTable.setFixedCellSize(36);
        perfTable.setPrefHeight(280);

        TableColumn<PsychologuePerformanceRow, String> pNameCol = new TableColumn<>("Psychologue");
        pNameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPsychologue()));
        TableColumn<PsychologuePerformanceRow, String> pTotalCol = new TableColumn<>("Appointments per psychologue");
        pTotalCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getTotal())));
        TableColumn<PsychologuePerformanceRow, String> pAccCol = new TableColumn<>("Acceptance rate by psychologue");
        pAccCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getAcceptanceRate()));
        TableColumn<PsychologuePerformanceRow, String> pCancelCol = new TableColumn<>("Cancellation rate by psychologue");
        pCancelCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCancellationRate()));
        perfTable.getColumns().addAll(pNameCol, pTotalCol, pAccCol, pCancelCol);
        perfTable.setItems(FXCollections.observableArrayList(buildPsychologuePerformanceRows(data)));

        VBox content = new VBox(
            12,
            globalTitle,
            kpiGrid,
            trendTitle,
            new Label("Weekly"),
            weeklyTable,
            new Label("Monthly"),
            monthlyTable,
            perfTitle,
            perfTable
        );
        content.setPadding(new Insets(10));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(700);
        dialog.getDialogPane().setContent(scroll);
        dialog.showAndWait();
    }

    private VBox buildKpiCard(String title, String value) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("label-muted");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("card-title");
        VBox card = new VBox(4, titleLabel, valueLabel);
        card.setPadding(new Insets(10));
        card.getStyleClass().add("card");
        return card;
    }

    private int countByStatus(List<Appointment> data, String status) {
        int count = 0;
        for (Appointment appointment : data) {
            if (status.equals(normalizeStatus(appointment.getStatus()))) {
                count++;
            }
        }
        return count;
    }

    private String formatRate(int numerator, int denominator) {
        if (denominator <= 0) {
            return "0.00%";
        }
        return String.format("%.2f%%", (numerator * 100.0) / denominator);
    }

    private List<TrendRow> buildWeeklyTrendRows(List<Appointment> data) {
        Map<LocalDate, TrendCounter> counters = new TreeMap<>();
        LocalDate start = LocalDate.now().minusDays(6);
        for (int i = 0; i < 7; i++) {
            counters.put(start.plusDays(i), new TrendCounter());
        }

        for (Appointment appointment : data) {
            if (appointment.getDateTime() == null) {
                continue;
            }
            LocalDate date = appointment.getDateTime().toLocalDate();
            TrendCounter counter = counters.get(date);
            if (counter == null) {
                continue;
            }
            counter.total++;
            String status = normalizeStatus(appointment.getStatus());
            if ("completed".equals(status)) {
                counter.completed++;
            }
            if ("cancelled".equals(status)) {
                counter.cancelled++;
            }
        }

        List<TrendRow> rows = new ArrayList<>();
        for (Map.Entry<LocalDate, TrendCounter> entry : counters.entrySet()) {
            rows.add(new TrendRow(entry.getKey().toString(), entry.getValue().total, entry.getValue().completed, entry.getValue().cancelled));
        }
        return rows;
    }

    private List<TrendRow> buildMonthlyTrendRows(List<Appointment> data) {
        Map<YearMonth, TrendCounter> counters = new LinkedHashMap<>();
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            counters.put(now.minusMonths(i), new TrendCounter());
        }

        for (Appointment appointment : data) {
            if (appointment.getDateTime() == null) {
                continue;
            }
            YearMonth month = YearMonth.from(appointment.getDateTime());
            TrendCounter counter = counters.get(month);
            if (counter == null) {
                continue;
            }
            counter.total++;
            String status = normalizeStatus(appointment.getStatus());
            if ("completed".equals(status)) {
                counter.completed++;
            }
            if ("cancelled".equals(status)) {
                counter.cancelled++;
            }
        }

        List<TrendRow> rows = new ArrayList<>();
        for (Map.Entry<YearMonth, TrendCounter> entry : counters.entrySet()) {
            rows.add(new TrendRow(entry.getKey().toString(), entry.getValue().total, entry.getValue().completed, entry.getValue().cancelled));
        }
        return rows;
    }

    private List<PsychologuePerformanceRow> buildPsychologuePerformanceRows(List<Appointment> data) {
        Map<String, PsychologueCounter> counters = new TreeMap<>();
        for (Appointment appointment : data) {
            String name = displayUser(appointment.getPsyName(), appointment.getPsyId());
            PsychologueCounter counter = counters.computeIfAbsent(name, ignored -> new PsychologueCounter());
            counter.total++;
            String status = normalizeStatus(appointment.getStatus());
            if ("accepted".equals(status)) {
                counter.accepted++;
            }
            if ("cancelled".equals(status)) {
                counter.cancelled++;
            }
        }

        List<PsychologuePerformanceRow> rows = new ArrayList<>();
        for (Map.Entry<String, PsychologueCounter> entry : counters.entrySet()) {
            PsychologueCounter counter = entry.getValue();
            rows.add(new PsychologuePerformanceRow(
                entry.getKey(),
                counter.total,
                formatRate(counter.accepted, counter.total),
                formatRate(counter.cancelled, counter.total)
            ));
        }
        return rows;
    }

    private String normalizeStatus(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static final class TrendCounter {
        private int total;
        private int completed;
        private int cancelled;
    }

    private static final class PsychologueCounter {
        private int total;
        private int accepted;
        private int cancelled;
    }

    private static final class TrendRow {
        private final String period;
        private final int total;
        private final int completed;
        private final int cancelled;

        private TrendRow(String period, int total, int completed, int cancelled) {
            this.period = period;
            this.total = total;
            this.completed = completed;
            this.cancelled = cancelled;
        }

        public String getPeriod() {
            return period;
        }

        public int getTotal() {
            return total;
        }

        public int getCompleted() {
            return completed;
        }

        public int getCancelled() {
            return cancelled;
        }
    }

    private static final class PsychologuePerformanceRow {
        private final String psychologue;
        private final int total;
        private final String acceptanceRate;
        private final String cancellationRate;

        private PsychologuePerformanceRow(String psychologue, int total, String acceptanceRate, String cancellationRate) {
            this.psychologue = psychologue;
            this.total = total;
            this.acceptanceRate = acceptanceRate;
            this.cancellationRate = cancellationRate;
        }

        public String getPsychologue() {
            return psychologue;
        }

        public int getTotal() {
            return total;
        }

        public String getAcceptanceRate() {
            return acceptanceRate;
        }

        public String getCancellationRate() {
            return cancellationRate;
        }
    }

    private void openEditor(Appointment existing) {
        Dialog<Appointment> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Nouveau rendez-vous" : "Modifier rendez-vous");
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/mindcare/styles/orion-theme.css").toExternalForm()
        );
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Load students and psychologists
        ObservableList<User> students = FXCollections.observableArrayList(appointmentService.getStudents());
        ObservableList<User> psychologists = FXCollections.observableArrayList(appointmentService.getPsychologists());

        DatePicker datePicker = new DatePicker(existing != null && existing.getDateTime() != null
            ? existing.getDateTime().toLocalDate()
            : LocalDate.now());

        ComboBox<String> timeBox = new ComboBox<>();
        String preferredTime = existing != null && existing.getDateTime() != null
            ? existing.getDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
            : "09:00";

        ComboBox<String> locationBox = new ComboBox<>();
        locationBox.getItems().addAll("online", "in office");
        String existingLocation = existing == null ? "in office" : emptySafe(existing.getLocation()).toLowerCase();
        locationBox.setValue("online".equals(existingLocation) ? "online" : "in office");

        // ComboBox for students
        ComboBox<User> studentCombo = new ComboBox<>(students);
        studentCombo.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User user) {
                return user == null ? "" : user.getFirstName() + " " + user.getLastName() + " (ID: " + user.getId() + ")";
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });
        studentCombo.setCellFactory(param -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? "" : user.getFirstName() + " " + user.getLastName() + " (ID: " + user.getId() + ")");
            }
        });

        if (existing != null && existing.getStudentId() != null) {
            students.stream()
                .filter(u -> u.getId() == existing.getStudentId())
                .findFirst()
                .ifPresent(studentCombo::setValue);
        }

        // ComboBox for psychologists
        ComboBox<User> psyCombo = new ComboBox<>(psychologists);
        psyCombo.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User user) {
                return user == null ? "" : user.getFirstName() + " " + user.getLastName() + " (ID: " + user.getId() + ")";
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });
        psyCombo.setCellFactory(param -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? "" : user.getFirstName() + " " + user.getLastName() + " (ID: " + user.getId() + ")");
            }
        });

        if (existing != null && existing.getPsyId() != null) {
            psychologists.stream()
                .filter(u -> u.getId() == existing.getPsyId())
                .findFirst()
                .ifPresent(psyCombo::setValue);
        } else if (!psychologists.isEmpty()) {
            psyCombo.setValue(psychologists.get(0));
        }

        refreshAvailableSlots(
            timeBox,
            psyCombo.getValue() == null ? null : psyCombo.getValue().getId(),
            datePicker.getValue(),
            existing == null ? null : existing.getId(),
            preferredTime
        );

        datePicker.valueProperty().addListener((obs, oldDate, newDate) ->
            refreshAvailableSlots(
                timeBox,
                psyCombo.getValue() == null ? null : psyCombo.getValue().getId(),
                newDate,
                existing == null ? null : existing.getId(),
                timeBox.getValue()
            )
        );

        psyCombo.valueProperty().addListener((obs, oldUser, newUser) ->
            refreshAvailableSlots(
                timeBox,
                newUser == null ? null : newUser.getId(),
                datePicker.getValue(),
                existing == null ? null : existing.getId(),
                timeBox.getValue()
            )
        );

        TextArea descriptionArea = new TextArea(existing == null ? "" : emptySafe(existing.getDescription()));
        descriptionArea.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Date"), 0, 0);
        grid.add(datePicker, 1, 0);
        grid.add(new Label("Heure"), 0, 1);
        grid.add(timeBox, 1, 1);
        grid.add(new Label("Lieu"), 0, 2);
        grid.add(locationBox, 1, 2);
        grid.add(new Label("Étudiant *"), 0, 3);
        grid.add(studentCombo, 1, 3);
        grid.add(new Label("Psychologue *"), 0, 4);
        grid.add(psyCombo, 1, 4);
        grid.add(new Label("Description"), 0, 5);
        grid.add(descriptionArea, 1, 5);

        Label inlineError = new Label();
        inlineError.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12; -fx-font-weight: bold;");
        inlineError.setWrapText(true);
        grid.add(inlineError, 1, 6);

        dialog.getDialogPane().setContent(grid);

        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            String validationError = validateAppointmentInput(
                datePicker.getValue(),
                timeBox.getValue(),
                studentCombo.getValue(),
                psyCombo.getValue(),
                descriptionArea.getText()
            );

            if (validationError != null) {
                inlineError.setText(validationError);
                event.consume();
            } else {
                inlineError.setText("");
            }
        });

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }

            LocalDate date = datePicker.getValue();
            LocalTime time = parseTime(timeBox.getValue());
            Appointment target = existing == null ? new Appointment() : existing;
            LocalDateTime selectedDateTime = LocalDateTime.of(date, time);

            target.setDateTime(selectedDateTime);
            target.setLocation(locationBox.getValue());
            target.setStatus("pending");  // Always set to pending for new appointments
            String description = descriptionArea.getText() == null ? null : descriptionArea.getText().trim();
            target.setDescription(description == null || description.isEmpty() ? null : description);
            target.setStudentId(studentCombo.getValue().getId());
            target.setPsyId(psyCombo.getValue().getId());
            target.setPatientFileId(null);  // Will be auto-resolved by service
            return target;
        });

        try {
            Optional<Appointment> result = dialog.showAndWait();
            if (result.isPresent()) {
                if (existing == null) {
                    appointmentService.create(result.get());
                } else {
                    appointmentService.update(result.get());
                }
                loadAppointments();
            }
        } catch (Exception exception) {
            showError("Operation failed", rootMessage(exception));
        }
    }

    private void deleteAppointment(Appointment appointment) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer le rendez-vous #" + appointment.getId() + " ?",
            ButtonType.YES, ButtonType.NO);
        confirm.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/mindcare/styles/orion-theme.css").toExternalForm()
        );
        confirm.setHeaderText("Confirmation");
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.YES) {
            return;
        }

        try {
            appointmentService.deleteById(appointment.getId());
            loadAppointments();
        } catch (Exception exception) {
            showError("Delete failed", rootMessage(exception));
        }
    }

    private void viewPatientFile(Appointment appointment) {
        try {
            PatientFile patientFile = null;
            Integer studentId = appointment.getStudentId();
            if (studentId != null && studentId > 0) {
                patientFile = appointmentService.getPatientFileByStudentId(studentId);
            }

            String studentName = appointment.getStudentName() != null ? appointment.getStudentName() : "Étudiant";
            
            DossierMedicalLegacyContent.setPatientFile(patientFile, studentName, studentId);
            NavigationManager.getInstance()
                .navigateContent("Dossier Médical - " + studentName, new DossierMedicalLegacyContent());
        } catch (Exception exception) {
            showError("Erreur", rootMessage(exception));
        }
    }

    private int compareByOption(Appointment a, Appointment b, String option) {
        String selected = option == null ? "Date (plus recente)" : option;
        switch (selected) {
            case "Date (plus ancienne)":
                return Comparator.nullsLast(LocalDateTime::compareTo)
                    .compare(a.getDateTime(), b.getDateTime());
            case "Etudiant (A-Z)":
                return safeCompare(a.getStudentName(), b.getStudentName());
            case "Psychologue (A-Z)":
                return safeCompare(a.getPsyName(), b.getPsyName());
            case "Statut (A-Z)":
                return safeCompare(a.getStatus(), b.getStatus());
            case "Date (plus recente)":
            default:
                return Comparator.nullsLast(LocalDateTime::compareTo)
                    .compare(b.getDateTime(), a.getDateTime());
        }
    }

    private String validateAppointmentInput(
        LocalDate date,
        String time,
        User student,
        User psychologist,
        String description
    ) {
        if (date == null) {
            return "La date est obligatoire.";
        }
        if (student == null) {
            return "L'etudiant est obligatoire.";
        }
        if (psychologist == null) {
            return "Le psychologue est obligatoire.";
        }
        if (time == null || time.isBlank()) {
            return "L'heure est obligatoire.";
        }

        LocalTime parsedTime;
        try {
            parsedTime = parseTime(time == null ? "" : time.trim());
        } catch (Exception ex) {
            return "Heure invalide. Format attendu: HH:mm.";
        }

        LocalDateTime selectedDateTime = LocalDateTime.of(date, parsedTime);
        if (selectedDateTime.isBefore(LocalDateTime.now().plusHours(1))) {
            return "Le rendez-vous doit etre au moins 1 heure apres l'heure actuelle.";
        }

        String trimmedDescription = description == null ? "" : description.trim();
        if (!trimmedDescription.isEmpty() && trimmedDescription.length() < 6) {
            return "La description doit contenir au moins 6 caracteres si elle est renseignee.";
        }

        return null;
    }

    private int safeCompare(String left, String right) {
        String l = left == null ? "" : left.toLowerCase();
        String r = right == null ? "" : right.toLowerCase();
        return l.compareTo(r);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private List<String> buildTimeSlots() {
        List<String> slots = new java.util.ArrayList<>();
        LocalTime time = LocalTime.of(9, 0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        while (!time.isAfter(LocalTime.of(17, 0))) {
            slots.add(time.format(formatter));
            time = time.plusMinutes(15);
        }
        return slots;
    }

    private void refreshAvailableSlots(ComboBox<String> timeBox, Integer psyId, LocalDate selectedDate, Integer excludedAppointmentId, String preferredTime) {
        timeBox.getItems().clear();
        if (psyId == null || selectedDate == null) {
            return;
        }

        List<String> allSlots = buildTimeSlots();
        List<String> occupiedStartTimes = appointmentService.getOccupiedSlotsForPsychologist(psyId, selectedDate, excludedAppointmentId);
        LocalDateTime minAllowed = LocalDateTime.now().plusHours(1);

        List<LocalTime> occupiedTimes = new java.util.ArrayList<>();
        for (String startTime : occupiedStartTimes) {
            try {
                occupiedTimes.add(LocalTime.parse(startTime));
            } catch (Exception ignored) {
            }
        }

        for (String slot : allSlots) {
            LocalTime slotTime = LocalTime.parse(slot);

            boolean isBlocked = false;
            for (LocalTime occupiedStart : occupiedTimes) {
                LocalTime occupiedEnd = occupiedStart.plusHours(1);
                if (!slotTime.isBefore(occupiedStart) && slotTime.isBefore(occupiedEnd)) {
                    isBlocked = true;
                    break;
                }
            }

            if (isBlocked) {
                continue;
            }

            LocalDateTime slotDateTime = LocalDateTime.of(selectedDate, slotTime);
            if (slotDateTime.isBefore(minAllowed)) {
                continue;
            }

            timeBox.getItems().add(slot);
        }

        if (preferredTime != null && timeBox.getItems().contains(preferredTime)) {
            timeBox.setValue(preferredTime);
        } else if (!timeBox.getItems().isEmpty()) {
            timeBox.setValue(timeBox.getItems().get(0));
        } else {
            timeBox.setPromptText("Aucun creneau disponible");
        }
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalTime.of(9, 0);
        }
        return LocalTime.parse(value.length() == 5 ? value : value.substring(0, 5));
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/mindcare/styles/orion-theme.css").toExternalForm()
        );
        alert.setHeaderText(title);
        alert.setContentText(message == null ? "Unexpected error" : message);
        alert.showAndWait();
    }

    private String displayUser(String name, Integer id) {
        String safeName = emptySafe(name);
        if (safeName.isBlank()) {
            return id == null ? "N/A" : "ID " + id;
        }
        return id == null ? safeName : safeName + " (ID " + id + ")";
    }

    private String emptySafe(String value) {
        return value == null ? "" : value;
    }

    private void showAppointmentDetails(Appointment appointment) {
        try {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Détails du rendez-vous");
            dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/mindcare/styles/orion-theme.css").toExternalForm()
            );
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            String dateTimeStr = appointment.getDateTime() != null
                ? appointment.getDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "N/A";
            String location = emptySafe(appointment.getLocation());
            String studentName = emptySafe(appointment.getStudentName());
            String psychologistName = emptySafe(appointment.getPsyName());
            String status = emptySafe(appointment.getStatus());
            String description = emptySafe(appointment.getDescription());

            Label dateLabel = new Label("Date et Heure:");
            dateLabel.getStyleClass().add("label-bold");
            Label dateValue = new Label(dateTimeStr);

            Label locationLabel = new Label("Lieu:");
            locationLabel.getStyleClass().add("label-bold");
            Label locationValue = new Label(location);

            Label studentLabel = new Label("Étudiant:");
            studentLabel.getStyleClass().add("label-bold");
            Label studentValue = new Label(studentName);

            Label psychoLabel = new Label("Psychologue:");
            psychoLabel.getStyleClass().add("label-bold");
            Label psychoValue = new Label(psychologistName);

            Label statusLabel = new Label("Statut:");
            statusLabel.getStyleClass().add("label-bold");
            Label statusValue = new Label(status);

            Label descriptionLabel = new Label("Description:");
            descriptionLabel.getStyleClass().add("label-bold");
            TextArea descriptionArea = new TextArea(description);
            descriptionArea.setWrapText(true);
            descriptionArea.setEditable(false);
            descriptionArea.setPrefRowCount(4);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(15));
            grid.add(dateLabel, 0, 0);
            grid.add(dateValue, 1, 0);
            grid.add(locationLabel, 0, 1);
            grid.add(locationValue, 1, 1);
            grid.add(studentLabel, 0, 2);
            grid.add(studentValue, 1, 2);
            grid.add(psychoLabel, 0, 3);
            grid.add(psychoValue, 1, 3);
            grid.add(statusLabel, 0, 4);
            grid.add(statusValue, 1, 4);
            grid.add(descriptionLabel, 0, 5);
            grid.add(descriptionArea, 0, 6, 2, 1);
            GridPane.setHgrow(descriptionArea, Priority.ALWAYS);

            dialog.getDialogPane().setContent(grid);
            dialog.showAndWait();
        } catch (Exception exception) {
            showError("Erreur", rootMessage(exception));
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}
