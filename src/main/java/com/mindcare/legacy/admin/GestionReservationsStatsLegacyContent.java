package com.mindcare.legacy.admin;

import com.mindcare.model.Appointment;
import com.mindcare.services.AppointmentService;
import com.mindcare.utils.NavigationManager;
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

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GestionReservationsStatsLegacyContent implements NavigationManager.Buildable {

    private final AppointmentService appointmentService = new AppointmentService();

    @Override
    public Node build() {
        List<Appointment> data = new ArrayList<>(appointmentService.findAll());

        VBox root = new VBox(14);
        root.setPadding(new Insets(12));

        Label title = new Label("Statistiques rendez-vous");
        title.getStyleClass().add("page-title");

        Label sub = new Label("Vue admin: KPI globaux, tendances hebdomadaires/mensuelles, performance des psychologues.");
        sub.getStyleClass().add("page-subtitle");

        Button backBtn = new Button("Retour gestion rendez-vous");
        backBtn.getStyleClass().addAll("btn", "btn-outline", "btn-sm");
        backBtn.setOnAction(e -> NavigationManager.getInstance().navigateContent("Gestion rendez-vous", new GestionReservationsLegacyContent()));

        HBox header = new HBox(10);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(new VBox(4, title, sub), spacer, backBtn);

        int total = data.size();
        int pending = countByStatus(data, "pending");
        int accepted = countByStatus(data, "accepted");
        int rejected = countByStatus(data, "rejected");
        int cancelled = countByStatus(data, "cancelled");
        int completed = countByStatus(data, "completed");

        Label kpiTitle = new Label("Global KPI cards");
        kpiTitle.getStyleClass().add("card-title");

        GridPane kpiGrid = new GridPane();
        kpiGrid.setHgap(10);
        kpiGrid.setVgap(10);
        kpiGrid.add(buildKpiCard("Total appointments", String.valueOf(total), "#0F172A", "#FFFFFF"), 0, 0);
        kpiGrid.add(buildKpiCard("Pending", String.valueOf(pending), "#F59E0B", "#111827"), 1, 0);
        kpiGrid.add(buildKpiCard("Accepted", String.valueOf(accepted), "#10B981", "#FFFFFF"), 2, 0);
        kpiGrid.add(buildKpiCard("Rejected", String.valueOf(rejected), "#EF4444", "#FFFFFF"), 0, 1);
        kpiGrid.add(buildKpiCard("Cancelled", String.valueOf(cancelled), "#FB7185", "#111827"), 1, 1);
        kpiGrid.add(buildKpiCard("Completed", String.valueOf(completed), "#2563EB", "#FFFFFF"), 2, 1);
        kpiGrid.add(buildKpiCard("Acceptance rate", formatRate(accepted, total), "#14B8A6", "#042F2E"), 0, 2);

        Label trendTitle = new Label("Time trend (weekly/monthly)");
        trendTitle.getStyleClass().add("card-title");

        List<TrendRow> weeklyRows = buildWeeklyTrendRows(data);
        List<TrendRow> monthlyRows = buildMonthlyTrendRows(data);
        List<PsychologuePerformanceRow> perfRows = buildPsychologuePerformanceRows(data);

        TableView<TrendRow> weeklyTable = new TableView<>();
        configureStatsTable(weeklyTable, weeklyRows.size());

        TableColumn<TrendRow, String> wDayCol = new TableColumn<>("Jour");
        wDayCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPeriod()));
        TableColumn<TrendRow, String> wCreatedCol = new TableColumn<>("Appointments created per day");
        wCreatedCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getTotal())));
        TableColumn<TrendRow, String> wCompletedCol = new TableColumn<>("Completed per day");
        wCompletedCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getCompleted())));
        TableColumn<TrendRow, String> wCancelCol = new TableColumn<>("Cancellation trend");
        wCancelCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getCancelled())));
        weeklyTable.getColumns().addAll(wDayCol, wCreatedCol, wCompletedCol, wCancelCol);
        weeklyTable.setItems(FXCollections.observableArrayList(weeklyRows));

        TableView<TrendRow> monthlyTable = new TableView<>();
        configureStatsTable(monthlyTable, monthlyRows.size());

        TableColumn<TrendRow, String> mMonthCol = new TableColumn<>("Mois");
        mMonthCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPeriod()));
        TableColumn<TrendRow, String> mCreatedCol = new TableColumn<>("Appointments");
        mCreatedCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getTotal())));
        TableColumn<TrendRow, String> mCompletedCol = new TableColumn<>("Completed");
        mCompletedCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getCompleted())));
        TableColumn<TrendRow, String> mCancelCol = new TableColumn<>("Cancelled");
        mCancelCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getCancelled())));
        monthlyTable.getColumns().addAll(mMonthCol, mCreatedCol, mCompletedCol, mCancelCol);
        monthlyTable.setItems(FXCollections.observableArrayList(monthlyRows));

        Label perfTitle = new Label("Psychologue performance table");
        perfTitle.getStyleClass().add("card-title");

        TableView<PsychologuePerformanceRow> perfTable = new TableView<>();
        configureStatsTable(perfTable, Math.max(6, perfRows.size()));

        TableColumn<PsychologuePerformanceRow, String> pNameCol = new TableColumn<>("Psychologue");
        pNameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPsychologue()));
        TableColumn<PsychologuePerformanceRow, String> pTotalCol = new TableColumn<>("Appointments per psychologue");
        pTotalCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getTotal())));
        TableColumn<PsychologuePerformanceRow, String> pAcceptanceCol = new TableColumn<>("Acceptance rate by psychologue");
        pAcceptanceCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getAcceptanceRate()));
        TableColumn<PsychologuePerformanceRow, String> pCancelRateCol = new TableColumn<>("Cancellation rate by psychologue");
        pCancelRateCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCancellationRate()));
        perfTable.getColumns().addAll(pNameCol, pTotalCol, pAcceptanceCol, pCancelRateCol);
        perfTable.setItems(FXCollections.observableArrayList(perfRows));

        VBox weeklyCard = wrapSectionCard(new Label("Weekly"), weeklyTable);
        VBox monthlyCard = wrapSectionCard(new Label("Monthly"), monthlyTable);
        VBox perfCard = wrapSectionCard(null, perfTable);

        VBox content = new VBox(
            12,
            header,
            kpiTitle,
            kpiGrid,
            trendTitle,
            weeklyCard,
            monthlyCard,
            perfTitle,
            perfCard
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox buildKpiCard(String label, String value, String bgColor, String textColor) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-text-fill: " + textColor + "; -fx-opacity: 0.9;");

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 22px; -fx-font-weight: 700;");

        VBox card = new VBox(4, labelNode, valueNode);
        card.setPadding(new Insets(12));
        card.setMinWidth(210);
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
        table.setRowFactory(tv -> {
            return new javafx.scene.control.TableRow<>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    setStyle(empty
                        ? ""
                        : "-fx-cell-size: 42px; -fx-font-size: 13px; -fx-text-fill: #0F172A;");
                }
            };
        });
    }

    private VBox wrapSectionCard(Label sectionLabel, Node contentNode) {
        VBox card = new VBox(8);
        if (sectionLabel != null) {
            sectionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");
            card.getChildren().add(sectionLabel);
        }
        card.getChildren().add(contentNode);
        card.setPadding(new Insets(10));
        card.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #E2E8F0;" +
            "-fx-border-radius: 12;"
        );
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

    private String displayUser(String name, Integer id) {
        String safeName = name == null ? "" : name.trim();
        if (safeName.isBlank()) {
            return id == null ? "N/A" : "ID " + id;
        }
        return id == null ? safeName : safeName + " (ID " + id + ")";
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
}
