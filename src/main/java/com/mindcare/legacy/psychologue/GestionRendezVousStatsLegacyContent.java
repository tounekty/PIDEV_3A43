package com.mindcare.legacy.psychologue;

import com.mindcare.model.Appointment;
import com.mindcare.model.User;
import com.mindcare.services.AppointmentService;
import com.mindcare.utils.NavigationManager;
import com.mindcare.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GestionRendezVousStatsLegacyContent implements NavigationManager.Buildable {

    private static final int WORK_MINUTES_PER_DAY = 8 * 60; // 09:00-17:00

    private final AppointmentService appointmentService = new AppointmentService();

    @Override
    public Node build() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        int psychologueId = currentUser == null ? -1 : currentUser.getId();

        List<Appointment> all = psychologueId <= 0
            ? new ArrayList<>()
            : new ArrayList<>(appointmentService.findForPsychologist(psychologueId));

        int total = all.size();
        int pendingNow = countByStatus(all, "pending");
        int acceptedThisWeek = countAcceptedThisWeek(all);
        int cancelledThisMonth = countCancelledThisMonth(all);

        List<WeeklyLoadRow> weeklyLoad = buildWeeklyLoadRows(all);
        List<StatusDistributionRow> statusDistribution = buildStatusDistributionRows(all);

        VBox root = new VBox(14);
        root.setPadding(new Insets(12));

        Label title = new Label("Statistiques rendez-vous - Psychologue");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Vue personnelle: KPI, charge hebdomadaire (Mon-Sun), et distribution des statuts.");
        subtitle.getStyleClass().add("page-subtitle");

        Button backBtn = new Button("Retour gestion rendez-vous");
        backBtn.getStyleClass().addAll("btn", "btn-outline", "btn-sm");
        backBtn.setOnAction(e -> NavigationManager.getInstance().navigateContent("Gestion rendez-vous", new GestionRendezVousLegacyContent()));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(new VBox(4, title, subtitle), spacer, backBtn);

        Label kpiTitle = new Label("Personal KPI cards");
        kpiTitle.getStyleClass().add("card-title");

        GridPane kpiGrid = new GridPane();
        kpiGrid.setHgap(10);
        kpiGrid.setVgap(10);
        kpiGrid.add(buildKpiCard("My total appointments", String.valueOf(total), "#1E293B", "#FFFFFF"), 0, 0);
        kpiGrid.add(buildKpiCard("Pending now", String.valueOf(pendingNow), "#F59E0B", "#111827"), 1, 0);
        kpiGrid.add(buildKpiCard("Accepted this week", String.valueOf(acceptedThisWeek), "#10B981", "#FFFFFF"), 2, 0);
        kpiGrid.add(buildKpiCard("Cancelled this month", String.valueOf(cancelledThisMonth), "#FB7185", "#111827"), 0, 1);

        Label loadTitle = new Label("My weekly calendar load");
        loadTitle.getStyleClass().add("card-title");

        TableView<WeeklyLoadRow> weeklyLoadTable = new TableView<>();
        configureStatsTable(weeklyLoadTable, weeklyLoad.size());

        TableColumn<WeeklyLoadRow, String> dayCol = new TableColumn<>("Day (Mon-Sun)");
        dayCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDayLabel()));
        TableColumn<WeeklyLoadRow, String> appointmentsCol = new TableColumn<>("Appointments per day");
        appointmentsCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getAppointments())));
        TableColumn<WeeklyLoadRow, String> occupancyCol = new TableColumn<>("Occupancy rate in work hours (09:00-17:00)");
        occupancyCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOccupancyRate()));
        weeklyLoadTable.getColumns().addAll(dayCol, appointmentsCol, occupancyCol);
        weeklyLoadTable.setItems(FXCollections.observableArrayList(weeklyLoad));

        Label distTitle = new Label("Status distribution");
        distTitle.getStyleClass().add("card-title");

        TableView<StatusDistributionRow> distributionTable = new TableView<>();
        configureStatsTable(distributionTable, Math.max(5, statusDistribution.size()));

        TableColumn<StatusDistributionRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus()));
        TableColumn<StatusDistributionRow, String> countCol = new TableColumn<>("Count");
        countCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getCount())));
        TableColumn<StatusDistributionRow, String> rateCol = new TableColumn<>("Rate");
        rateCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getRate()));
        distributionTable.getColumns().addAll(statusCol, countCol, rateCol);
        distributionTable.setItems(FXCollections.observableArrayList(statusDistribution));

        VBox content = new VBox(12,
            header,
            kpiTitle,
            kpiGrid,
            loadTitle,
            wrapSectionCard(weeklyLoadTable),
            distTitle,
            wrapSectionCard(distributionTable)
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox buildKpiCard(String label, String value, String bgColor, String textColor) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-text-fill: " + textColor + "; -fx-opacity: 0.92;");

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 22px; -fx-font-weight: 700;");

        VBox card = new VBox(4, labelNode, valueNode);
        card.setPadding(new Insets(12));
        card.setMinWidth(220);
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12; -fx-border-radius: 12;");
        return card;
    }

    private <T> void configureStatsTable(TableView<T> table, int rowCount) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(42);
        table.setStyle(
            "-fx-font-size: 13px;" +
                "-fx-background-color: #FFFFFF;" +
                "-fx-control-inner-background: #FFFFFF;" +
                "-fx-table-cell-border-color: #E2E8F0;" +
                "-fx-text-background-color: #0F172A;"
        );

        int visibleRows = Math.max(4, rowCount);
        table.setPrefHeight((visibleRows + 1.35) * table.getFixedCellSize());
        table.setPlaceholder(new Label("Aucune donnee disponible"));
    }

    private VBox wrapSectionCard(Node contentNode) {
        VBox card = new VBox(contentNode);
        card.setPadding(new Insets(10));
        card.setStyle(
            "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #E2E8F0;" +
                "-fx-border-radius: 12;"
        );
        return card;
    }

    private int countByStatus(List<Appointment> appointments, String status) {
        int count = 0;
        for (Appointment appointment : appointments) {
            if (status.equals(normalizeStatus(appointment.getStatus()))) {
                count++;
            }
        }
        return count;
    }

    private int countAcceptedThisWeek(List<Appointment> appointments) {
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        int count = 0;

        for (Appointment appointment : appointments) {
            LocalDate date = toDate(appointment);
            if (date == null) {
                continue;
            }
            if (!"accepted".equals(normalizeStatus(appointment.getStatus()))) {
                continue;
            }
            if (!date.isBefore(monday) && !date.isAfter(sunday)) {
                count++;
            }
        }
        return count;
    }

    private int countCancelledThisMonth(List<Appointment> appointments) {
        YearMonth currentMonth = YearMonth.now();
        int count = 0;

        for (Appointment appointment : appointments) {
            LocalDate date = toDate(appointment);
            if (date == null) {
                continue;
            }
            if (!"cancelled".equals(normalizeStatus(appointment.getStatus()))) {
                continue;
            }
            if (YearMonth.from(date).equals(currentMonth)) {
                count++;
            }
        }
        return count;
    }

    private List<WeeklyLoadRow> buildWeeklyLoadRows(List<Appointment> appointments) {
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEE dd/MM");
        List<WeeklyLoadRow> rows = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            int dayAppointments = 0;
            for (Appointment appointment : appointments) {
                LocalDate apptDate = toDate(appointment);
                if (apptDate == null || !apptDate.equals(day)) {
                    continue;
                }
                String status = normalizeStatus(appointment.getStatus());
                if ("cancelled".equals(status) || "rejected".equals(status)) {
                    continue;
                }
                dayAppointments++;
            }

            double occupancy = (dayAppointments * 60.0 * 100.0) / WORK_MINUTES_PER_DAY;
            String occupancyLabel = String.format("%.2f%%", occupancy);
            rows.add(new WeeklyLoadRow(dayFormatter.format(day), dayAppointments, occupancyLabel));
        }

        return rows;
    }

    private List<StatusDistributionRow> buildStatusDistributionRows(List<Appointment> appointments) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("pending", 0);
        counts.put("accepted", 0);
        counts.put("rejected", 0);
        counts.put("cancelled", 0);
        counts.put("completed", 0);

        for (Appointment appointment : appointments) {
            String status = normalizeStatus(appointment.getStatus());
            if (!counts.containsKey(status)) {
                counts.put(status, 0);
            }
            counts.put(status, counts.get(status) + 1);
        }

        int total = appointments.size();
        List<StatusDistributionRow> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            rows.add(new StatusDistributionRow(entry.getKey().toUpperCase(), entry.getValue(), formatRate(entry.getValue(), total)));
        }
        return rows;
    }

    private String formatRate(int numerator, int denominator) {
        if (denominator <= 0) {
            return "0.00%";
        }
        return String.format("%.2f%%", (numerator * 100.0) / denominator);
    }

    private LocalDate toDate(Appointment appointment) {
        LocalDateTime dt = appointment == null ? null : appointment.getDateTime();
        return dt == null ? null : dt.toLocalDate();
    }

    private String normalizeStatus(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static final class WeeklyLoadRow {
        private final String dayLabel;
        private final int appointments;
        private final String occupancyRate;

        private WeeklyLoadRow(String dayLabel, int appointments, String occupancyRate) {
            this.dayLabel = dayLabel;
            this.appointments = appointments;
            this.occupancyRate = occupancyRate;
        }

        public String getDayLabel() {
            return dayLabel;
        }

        public int getAppointments() {
            return appointments;
        }

        public String getOccupancyRate() {
            return occupancyRate;
        }
    }

    private static final class StatusDistributionRow {
        private final String status;
        private final int count;
        private final String rate;

        private StatusDistributionRow(String status, int count, String rate) {
            this.status = status;
            this.count = count;
            this.rate = rate;
        }

        public String getStatus() {
            return status;
        }

        public int getCount() {
            return count;
        }

        public String getRate() {
            return rate;
        }
    }
}
