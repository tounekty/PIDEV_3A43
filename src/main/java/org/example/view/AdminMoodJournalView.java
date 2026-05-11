package org.example.view;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.model.Journal;
import org.example.model.Mood;
import org.example.service.JournalService;
import org.example.service.MoodService;
import org.example.service.ServiceException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin panel for reviewing and managing student mood entries and journal posts.
 */
public class AdminMoodJournalView {

    private static final String NAV_ACTIVE =
            "-fx-background-color: white; -fx-text-fill: #1C4F96; -fx-font-weight: 900;" +
            "-fx-font-size: 13px; -fx-padding: 10 22; -fx-background-radius: 20;" +
            "-fx-cursor: hand; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.13),8,0,0,2);";
    private static final String NAV_NORMAL =
            "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.82); -fx-font-weight: 600;" +
            "-fx-font-size: 13px; -fx-padding: 10 22; -fx-background-radius: 20; -fx-cursor: hand;";

    private static final List<String> CASE_STATUSES = List.of(
            "New", "In review", "Support email sent",
            "Appointment proposed", "Escalated to psychologist", "Solved"
    );

    private final MoodService moodService = new MoodService();
    private final JournalService journalService = new JournalService();

    private final ObservableList<Mood> moodItems = FXCollections.observableArrayList();
    private final ObservableList<Journal> journalItems = FXCollections.observableArrayList();
    private final ObservableList<Mood> alertItems = FXCollections.observableArrayList();

    private final TableView<Mood> moodTable = new TableView<>();
    private final TableView<Journal> journalTable = new TableView<>();
    private final TableView<Mood> alertTable = new TableView<>();

    private final TextArea moodCommentArea = new TextArea();
    private final TextArea journalCommentArea = new TextArea();
    private final ComboBox<String> moodStatusCombo = new ComboBox<>();
    private final Label moodDetailLabel = new Label("Select a mood entry to review it.");
    private final Label journalDetailLabel = new Label("Select a journal entry to review it.");
    private final Label summaryLabel = new Label("Loading...");
    private final Label openCasesLabel = new Label("0");
    private final Label criticalCasesLabel = new Label("0");
    private final Label solvedCasesLabel = new Label("0");
    private final Label totalMoodsLabel = new Label("0");

    // Nav buttons
    private Button activePage;
    private final StackPane contentArea = new StackPane();

    public Node build() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #EEF2FF;");
        root.setTop(createHeader());
        root.setCenter(createBody());

        configureMoodTable();
        configureJournalTable();
        configureAlertTable();
        loadAll();
        return root;
    }

    // ── Header with embedded nav bar ──────────────────────────────────────────

    private VBox createHeader() {
        VBox header = new VBox(14);
        header.setPadding(new Insets(22, 28, 0, 28));
        header.setStyle("-fx-background-color: linear-gradient(to right, #163D7A, #245EBD);");

        // Title row
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox copy = new VBox(4);
        Label title = new Label("Mood & Journal Admin");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: white;");
        Label subtitle = new Label("Review student entries, set case status, and leave admin comments.");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.78);");
        summaryLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: rgba(255,255,255,0.70);");
        copy.getChildren().addAll(title, subtitle, summaryLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("↻  Refresh");
        refreshBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white;" +
                "-fx-font-size: 12px; -fx-padding: 8 16; -fx-background-radius: 14; -fx-cursor: hand;" +
                "-fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 14; -fx-border-width: 1;"
        );
        refreshBtn.setOnAction(e -> loadAll());
        titleRow.getChildren().addAll(copy, spacer, refreshBtn);

        // Nav bar
        HBox navBar = new HBox(4);
        navBar.setPadding(new Insets(0, 0, 0, 0));
        navBar.setAlignment(Pos.CENTER_LEFT);

        Button dashBtn  = navBtn("📊  Dashboard",  this::showDashboard);
        Button alertBtn = navBtn("⚠️  Alerts",      this::showAlerts);
        Button moodBtn  = navBtn("😊  Moods",        this::showMoods);
        Button jrnlBtn  = navBtn("📔  Journals",     this::showJournals);

        navBar.getChildren().addAll(dashBtn, alertBtn, moodBtn, jrnlBtn);

        // Pre-select dashboard
        activePage = dashBtn;
        dashBtn.setStyle(NAV_ACTIVE);

        header.getChildren().addAll(titleRow, navBar);
        return header;
    }

    private Button navBtn(String label, Runnable action) {
        Button btn = new Button(label);
        btn.setStyle(NAV_NORMAL);
        btn.setOnAction(e -> {
            if (activePage != null) activePage.setStyle(NAV_NORMAL);
            btn.setStyle(NAV_ACTIVE);
            activePage = btn;
            action.run();
        });
        return btn;
    }

    // ── Body ──────────────────────────────────────────────────────────────────

    private Node createBody() {
        contentArea.setPadding(new Insets(20));
        contentArea.setStyle("-fx-background-color: #EEF2FF;");
        showDashboard();
        return contentArea;
    }

    private void setContent(Node node) {
        contentArea.getChildren().setAll(node);
    }

    // ── Pages ─────────────────────────────────────────────────────────────────

    private void showDashboard() { setContent(buildDashboard()); }
    private void showAlerts()    { setContent(buildAlerts()); }
    private void showMoods()     { setContent(buildMoods()); }
    private void showJournals()  { setContent(buildJournals()); }

    // ── Dashboard page ────────────────────────────────────────────────────────

    private Node buildDashboard() {
        VBox page = new VBox(18);
        page.setAlignment(Pos.TOP_LEFT);

        Label heading = sectionTitle("📊 Case Overview");

        HBox metrics = new HBox(14);
        metrics.getChildren().addAll(
                metricCard("Total Moods", totalMoodsLabel, "#245EBD", "📝"),
                metricCard("Open Cases",  openCasesLabel,  "#1C4F96", "📂"),
                metricCard("Critical",    criticalCasesLabel, "#C0392B", "🚨"),
                metricCard("Solved",      solvedCasesLabel, "#27AE60", "✅")
        );

        Label alertHeading = new Label("⚠️  High-risk entries");
        alertHeading.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #C0392B;");

        alertTable.setPrefHeight(340);
        VBox.setVgrow(alertTable, Priority.ALWAYS);

        page.getChildren().addAll(heading, metrics, alertHeading, alertTable);
        return wrapScroll(page);
    }

    private VBox metricCard(String label, Label valueLabel, String color, String icon) {
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 22px;");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #7A8FAD;");
        valueLabel.setStyle("-fx-font-size: 30px; -fx-font-weight: 900; -fx-text-fill: " + color + ";");

        VBox card = new VBox(4, iconLbl, lbl, valueLabel);
        card.setPadding(new Insets(16, 18, 16, 18));
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 18;" +
                "-fx-border-color: #D8E4F7; -fx-border-radius: 18; -fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian,rgba(28,79,150,0.07),10,0,0,3);"
        );
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    // ── Alerts page ──────────────────────────────────────────────────────────

    private Node buildAlerts() {
        VBox page = new VBox(12);
        page.setAlignment(Pos.TOP_LEFT);

        Label heading = sectionTitle("⚠️ High-Risk Alerts");

        Label hint = new Label("Entries where stress level ≥ 8 or mood type indicates a negative/critical state.");
        hint.setWrapText(true);
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #7A8FAD;");

        alertTable.setPrefHeight(600);
        VBox.setVgrow(alertTable, Priority.ALWAYS);

        page.getChildren().addAll(heading, hint, alertTable);
        return wrapScroll(page);
    }

    // ── Moods page ───────────────────────────────────────────────────────────

    private Node buildMoods() {
        VBox page = new VBox(14);
        page.setAlignment(Pos.TOP_LEFT);

        Label heading = sectionTitle("😊 Mood Review");

        moodDetailLabel.setWrapText(true);
        moodDetailLabel.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: #4A6FA5; -fx-background-color: #EEF4FF;" +
                "-fx-background-radius: 10; -fx-padding: 10 14;"
        );

        moodCommentArea.setPromptText("Write an admin comment for the selected mood entry...");
        moodCommentArea.setPrefRowCount(4);
        moodCommentArea.setWrapText(true);
        moodCommentArea.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().length() <= 1000 ? c : null));
        moodCommentArea.setStyle("-fx-font-size: 13px; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #C5D5EF;");

        moodStatusCombo.setItems(FXCollections.observableArrayList(CASE_STATUSES));
        moodStatusCombo.setPromptText("Set case status...");
        moodStatusCombo.setMaxWidth(Double.MAX_VALUE);

        Button saveCommentBtn = primaryBtn("💾  Save Comment");
        saveCommentBtn.setOnAction(e -> saveMoodComment());

        Button saveStatusBtn  = successBtn("🔄  Update Status");
        saveStatusBtn.setOnAction(e -> updateMoodCaseStatus());

        HBox workflowBar = new HBox(10, moodStatusCombo, saveStatusBtn);
        workflowBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(moodStatusCombo, Priority.ALWAYS);

        moodTable.setPrefHeight(340);
        VBox.setVgrow(moodTable, Priority.ALWAYS);

        page.getChildren().addAll(heading, moodTable, moodDetailLabel, workflowBar, moodCommentArea, saveCommentBtn);
        return wrapScroll(page);
    }

    // ── Journals page ────────────────────────────────────────────────────────

    private Node buildJournals() {
        VBox page = new VBox(14);
        page.setAlignment(Pos.TOP_LEFT);

        Label heading = sectionTitle("📔 Journal Review");

        journalDetailLabel.setWrapText(true);
        journalDetailLabel.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: #4A6FA5; -fx-background-color: #EEF4FF;" +
                "-fx-background-radius: 10; -fx-padding: 10 14;"
        );

        journalCommentArea.setPromptText("Write an admin comment for the selected journal entry...");
        journalCommentArea.setPrefRowCount(4);
        journalCommentArea.setWrapText(true);
        journalCommentArea.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().length() <= 1000 ? c : null));
        journalCommentArea.setStyle("-fx-font-size: 13px; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #C5D5EF;");

        Button saveBtn = primaryBtn("💾  Save Journal Comment");
        saveBtn.setOnAction(e -> saveJournalComment());

        journalTable.setPrefHeight(380);
        VBox.setVgrow(journalTable, Priority.ALWAYS);

        page.getChildren().addAll(heading, journalTable, journalDetailLabel, journalCommentArea, saveBtn);
        return wrapScroll(page);
    }

    // ── Table configuration ──────────────────────────────────────────────────

    private void configureMoodTable() {
        TableColumn<Mood, String> studentCol = col("Student", 170, m -> val(m.getStudentName()));
        TableColumn<Mood, String> typeCol    = col("Mood",    120, m -> val(m.getMoodType()));
        TableColumn<Mood, String> dateCol    = col("Date",    105, m -> String.valueOf(m.getMoodDate()));
        TableColumn<Mood, String> stressCol  = col("Stress",  68,  m -> lvl(m.getStressLevel()));
        TableColumn<Mood, String> energyCol  = col("Energy",  68,  m -> lvl(m.getEnergyLevel()));
        TableColumn<Mood, String> statusCol  = col("Status",  170, m -> val(m.getCaseStatus()));
        TableColumn<Mood, String> commentCol = col("Admin Comment", 200, m -> val(m.getAdminComment()));

        moodTable.getColumns().setAll(studentCol, typeCol, dateCol, stressCol, energyCol, statusCol, commentCol);
        moodTable.setItems(moodItems);
        moodTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        styleTable(moodTable);

        moodTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null) {
                moodDetailLabel.setText("Select a mood entry to review it.");
                moodCommentArea.clear();
                moodStatusCombo.setValue(null);
            } else {
                moodDetailLabel.setText(
                        "Mood #" + sel.getId() + "  ·  " + sel.getMoodType() + "  ·  " + sel.getMoodDate()
                        + "\nStudent: " + val(sel.getStudentName()) + "   Email: " + val(sel.getStudentEmail())
                        + "   Stress: " + lvl(sel.getStressLevel()) + "   Energy: " + lvl(sel.getEnergyLevel())
                        + "   Case: " + val(sel.getCaseStatus())
                        + (sel.getNote() != null && !sel.getNote().isBlank() ? "\nNote: " + sel.getNote() : "")
                );
                moodCommentArea.setText(val(sel.getAdminComment()));
                moodStatusCombo.setValue(sel.getCaseStatus());
            }
        });
    }

    private void configureJournalTable() {
        // No ID column - per user request
        TableColumn<Journal, String> titleCol   = col("Title",         160, j -> val(j.getTitle()));
        TableColumn<Journal, String> dateCol    = col("Date",          105, j -> String.valueOf(j.getEntryDate()));
        TableColumn<Journal, String> contentCol = col("Content",       320, j -> val(j.getContent()));
        TableColumn<Journal, String> commentCol = col("Admin Comment", 250, j -> val(j.getAdminComment()));

        journalTable.getColumns().setAll(titleCol, dateCol, contentCol, commentCol);
        journalTable.setItems(journalItems);
        journalTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        styleTable(journalTable);

        journalTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null) {
                journalDetailLabel.setText("Select a journal entry to review it.");
                journalCommentArea.clear();
            } else {
                journalDetailLabel.setText(
                        "\"" + sel.getTitle() + "\"  ·  " + sel.getEntryDate()
                        + (sel.getContent() != null && !sel.getContent().isBlank()
                                ? "\n" + sel.getContent().substring(0, Math.min(sel.getContent().length(), 200))
                                    + (sel.getContent().length() > 200 ? "…" : "")
                                : "")
                );
                journalCommentArea.setText(val(sel.getAdminComment()));
            }
        });
    }

    private void configureAlertTable() {
        TableColumn<Mood, String> studentCol = col("Student", 160, m -> val(m.getStudentName()));
        TableColumn<Mood, String> typeCol    = col("Mood",    120, m -> val(m.getMoodType()));
        TableColumn<Mood, String> dateCol    = col("Date",    105, m -> String.valueOf(m.getMoodDate()));
        TableColumn<Mood, String> stressCol  = col("Stress",  68,  m -> lvl(m.getStressLevel()));
        TableColumn<Mood, String> noteCol    = col("Note",    240, m -> val(m.getNote()));
        TableColumn<Mood, String> statusCol  = col("Status",  160, m -> val(m.getCaseStatus()));

        alertTable.getColumns().setAll(studentCol, typeCol, dateCol, stressCol, noteCol, statusCol);
        alertTable.setItems(alertItems);
        alertTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        styleTable(alertTable);
    }

    private <T> TableColumn<T, String> col(String title, double width,
            java.util.function.Function<T, String> extractor) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setCellValueFactory(data -> new ReadOnlyStringWrapper(extractor.apply(data.getValue())));
        c.setPrefWidth(width);
        return c;
    }

    private <T> void styleTable(TableView<T> table) {
        table.setStyle(
                "-fx-background-color: white; -fx-background-radius: 14;" +
                "-fx-border-color: #D8E4F7; -fx-border-radius: 14; -fx-border-width: 1;"
        );
        table.setFixedCellSize(42);
    }

    // ── Data loading ─────────────────────────────────────────────────────────

    private void loadAll() {
        List<Mood> moods = List.of();
        List<Journal> journals = List.of();

        try {
            moods = moodService.getAllMoods();
        } catch (ServiceException e) {
            summaryLabel.setText("Mood load error: " + e.getMessage());
        }

        try {
            journals = journalService.getAllJournals();
        } catch (SQLException e) {
            summaryLabel.setText("Journal load error: " + e.getMessage());
        }

        moodItems.setAll(moods);
        journalItems.setAll(journals);

        List<Mood> alerts = detectAlerts(moods);
        alertItems.setAll(alerts);

        long total   = moods.size();
        long open    = moods.stream().filter(m -> !"Solved".equals(m.getCaseStatus())).count();
        long critical = moods.stream().filter(this::isCritical).count();
        long solved  = moods.stream().filter(m -> "Solved".equals(m.getCaseStatus())).count();

        totalMoodsLabel.setText(String.valueOf(total));
        openCasesLabel.setText(String.valueOf(open));
        criticalCasesLabel.setText(String.valueOf(critical));
        solvedCasesLabel.setText(String.valueOf(solved));
        summaryLabel.setText("Alerts: " + alerts.size() + "  ·  Moods: " + total + "  ·  Journals: " + journals.size());
    }

    private List<Mood> detectAlerts(List<Mood> moods) {
        List<Mood> out = new ArrayList<>();
        for (Mood m : moods) if (isCritical(m)) out.add(m);
        return out;
    }

    private boolean isCritical(Mood m) {
        String t = m.getMoodType() == null ? "" : m.getMoodType().toLowerCase();
        boolean neg = t.contains("depressed") || t.contains("sad") || t.contains("stressed")
                   || t.contains("anxious")   || t.contains("angry") || t.contains("crisis");
        return neg || (m.getStressLevel() != null && m.getStressLevel() >= 8);
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    private void saveMoodComment() {
        Mood sel = moodTable.getSelectionModel().getSelectedItem();
        if (sel == null) { moodDetailLabel.setText("Please select a mood entry first."); return; }
        try {
            moodService.updateMood(sel.getId(), sel.getMoodType(), sel.getMoodDate(), sel.getNote(),
                    sel.getStressLevel(), sel.getEnergyLevel(), sel.getSleepTime(), sel.getWakeTime(),
                    sel.getSleepHours(), blankToNull(moodCommentArea.getText()), sel.isSupportEmailSent(), sel.getCaseStatus());
            int id = sel.getId();
            loadAll();
            reselectMood(id);
            moodDetailLabel.setText("✅ Admin comment saved.");
        } catch (ServiceException e) {
            moodDetailLabel.setText("Error: " + e.getMessage());
        }
    }

    private void updateMoodCaseStatus() {
        Mood sel = moodTable.getSelectionModel().getSelectedItem();
        if (sel == null) { moodDetailLabel.setText("Please select a mood entry first."); return; }
        String status = moodStatusCombo.getValue();
        if (status == null || status.isBlank()) { moodDetailLabel.setText("Choose a case status first."); return; }
        try {
            int id = sel.getId();
            moodService.updateCaseStatus(id, status);
            loadAll();
            reselectMood(id);
            moodDetailLabel.setText("✅ Status updated to: " + status);
        } catch (ServiceException e) {
            moodDetailLabel.setText("Error: " + e.getMessage());
        }
    }

    private void saveJournalComment() {
        Journal sel = journalTable.getSelectionModel().getSelectedItem();
        if (sel == null) { journalDetailLabel.setText("Please select a journal entry first."); return; }
        try {
            int id = sel.getId();
            journalService.addAdminComment(id, blankToNull(journalCommentArea.getText()));
            loadAll();
            reselectJournal(id);
            journalDetailLabel.setText("✅ Admin comment saved.");
        } catch (SQLException e) {
            journalDetailLabel.setText("Error: " + e.getMessage());
        }
    }

    private void reselectMood(int id) {
        moodItems.stream().filter(m -> m.getId() == id).findFirst()
                .ifPresent(moodTable.getSelectionModel()::select);
    }

    private void reselectJournal(int id) {
        journalItems.stream().filter(j -> j.getId() == id).findFirst()
                .ifPresent(journalTable.getSelectionModel()::select);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Node wrapScroll(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        StackPane.setAlignment(scroll, Pos.TOP_LEFT);
        return scroll;
    }

    private Label sectionTitle(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 17px; -fx-font-weight: 900; -fx-text-fill: #1C4F96;");
        return lbl;
    }

    private Button primaryBtn(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: #245EBD; -fx-text-fill: white; -fx-font-weight: 700;" +
                "-fx-font-size: 13px; -fx-padding: 10 22; -fx-background-radius: 12; -fx-cursor: hand;"
        );
        return btn;
    }

    private Button successBtn(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-weight: 700;" +
                "-fx-font-size: 13px; -fx-padding: 10 22; -fx-background-radius: 12; -fx-cursor: hand;"
        );
        return btn;
    }

    private String val(String s) { return s == null ? "" : s; }
    private String lvl(Integer l) { return l == null ? "—" : String.valueOf(l); }
    private String blankToNull(String s) { return (s == null || s.trim().isEmpty()) ? null : s.trim(); }
}
