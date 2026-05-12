package com.mindcare.view.client;

import com.mindcare.utils.NavigationManager;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.example.db.SchemaInitializer;
import org.example.model.Journal;
import org.example.model.Mood;
import org.example.service.JournalAiAnalysisService;
import org.example.service.JournalService;
import org.example.service.MoodJournalPdfService;
import org.example.service.MoodService;
import org.example.service.MoodSupportEmailService;
import org.example.service.ServiceException;
import org.example.service.TranslationService;
import org.example.service.WellnessReportService;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MoodJournalView – combined tabbed page (Mood / Journal / Heatmap) styled
 * with the global orion-theme.css so it matches the rest of the application.
 */
public class MoodJournalView implements NavigationManager.Buildable {

    public enum InitialTab { MOOD, JOURNAL, HEATMAP }

    private static final String[] MOOD_TYPES = {
            "Calm", "Happy", "Excited", "Grateful", "Content",
            "Neutral", "Anxious", "Sad", "Stressed", "Angry", "Depressed"
    };
    private static final String[] STRESS_LABELS = {
            "1 - Very low", "2", "3", "4", "5 - Moderate",
            "6", "7", "8", "9", "10 - Very high"
    };
    private static final String[] ENERGY_LABELS = STRESS_LABELS;

    private static final String[] ZEN_QUOTES = {
            "Be present. The only moment that matters is this one.",
            "You are not your thoughts; you are the awareness behind them.",
            "Small steps every day lead to big changes one day.",
            "Breathe. You are doing better than you think.",
            "Progress, not perfection.",
            "What you focus on grows. Choose kindness — to others and to yourself.",
            "The wound is the place where the light enters you. — Rumi",
            "Do not anticipate trouble or worry about what may never happen. — Benjamin Franklin"
    };

    private final InitialTab initialTab;
    private final MoodService moodService = new MoodService();
    private final JournalService journalService = new JournalService();

    private final ObservableList<Mood> moods = FXCollections.observableArrayList();
    private final ObservableList<Journal> journals = FXCollections.observableArrayList();

    // Mood form
    private ComboBox<String> moodTypeCombo;
    private ComboBox<String> stressCombo;
    private ComboBox<String> energyCombo;
    private DatePicker moodDatePicker;
    private TextArea moodNoteArea;
    private TextField moodSearchField;
    private ComboBox<String> moodSortCombo;
    private TableView<Mood> moodTable;
    private Label moodStatusLabel;
    private Label moodMetaLabel;
    private VBox moodPromptsBox;
    private PieChart moodPieChart;
    private BarChart<String, Number> moodBarChart;

    // Journal form
    private TextField journalTitleField;
    private TextArea journalContentArea;
    private DatePicker journalDatePicker;
    private ComboBox<Mood> journalMoodCombo;
    private TextField journalSearchField;
    private ComboBox<String> journalSortCombo;
    private TableView<Journal> journalTable;
    private Label journalStatusLabel;

    // Heatmap
    private GridPane heatmapGrid;
    private Label heatmapMonthLabel;

    private TabPane tabPane;

    public MoodJournalView() {
        this(InitialTab.MOOD);
    }

    public MoodJournalView(InitialTab initialTab) {
        this.initialTab = initialTab == null ? InitialTab.MOOD : initialTab;
    }

    @Override
    public Node build() {
        ensureSchema();

        VBox page = new VBox(18);
        page.getStyleClass().addAll("module-page", "module-page-content");

        Label title = new Label("Mood Journal");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Track your emotional wellbeing — moods, journal entries, and a monthly heatmap.");
        subtitle.getStyleClass().add("page-subtitle");
        VBox header = new VBox(4, title, subtitle);

        tabPane = new TabPane();
        tabPane.getStyleClass().add("module-tabs");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab moodTab = new Tab("Mood", buildMoodTab());
        Tab journalTab = new Tab("Journal", buildJournalTab());
        Tab heatmapTab = new Tab("Heatmap", buildHeatmapTab());
        tabPane.getTabs().addAll(moodTab, journalTab, heatmapTab);

        switch (initialTab) {
            case JOURNAL -> tabPane.getSelectionModel().select(journalTab);
            case HEATMAP -> tabPane.getSelectionModel().select(heatmapTab);
            default -> tabPane.getSelectionModel().select(moodTab);
        }

        page.getChildren().addAll(header, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("module-page");
        scroll.setStyle("-fx-background-color: #eff6ff; -fx-background: #eff6ff;");

        reloadMoods();
        reloadJournals();
        rebuildHeatmap();

        return scroll;
    }

    // ==================================================================
    //  MOOD TAB
    // ==================================================================

    private Node buildMoodTab() {
        VBox container = new VBox(18);
        container.setPadding(new Insets(18, 4, 4, 4));

        container.getChildren().addAll(
                buildMoodEntryCard(),
                buildQuickActionsCard(true),
                buildFindEntriesCard(true),
                buildMoodHistoryCard(),
                buildMoodChartsCard()
        );

        return container;
    }

    private Node buildMoodEntryCard() {
        Label sectionTitle = new Label("Entry details");
        sectionTitle.getStyleClass().add("section-title");
        Label sectionSub = new Label("Fill in the fields below, then save or update your entry.");
        sectionSub.getStyleClass().add("section-subtitle");

        // Form controls
        moodTypeCombo = new ComboBox<>(FXCollections.observableArrayList(MOOD_TYPES));
        moodTypeCombo.setPromptText("Aucun mood");
        moodTypeCombo.getStyleClass().add("module-input");
        moodTypeCombo.setMaxWidth(Double.MAX_VALUE);
        moodTypeCombo.valueProperty().addListener((obs, o, n) -> updatePrompts(n));

        stressCombo = new ComboBox<>(FXCollections.observableArrayList(STRESS_LABELS));
        stressCombo.setPromptText("stress level");
        stressCombo.getStyleClass().add("module-input");
        stressCombo.setMaxWidth(Double.MAX_VALUE);

        energyCombo = new ComboBox<>(FXCollections.observableArrayList(ENERGY_LABELS));
        energyCombo.setPromptText("energy level");
        energyCombo.getStyleClass().add("module-input");
        energyCombo.setMaxWidth(Double.MAX_VALUE);

        moodDatePicker = new DatePicker(LocalDate.now());
        moodDatePicker.getStyleClass().add("module-input");
        moodDatePicker.setMaxWidth(Double.MAX_VALUE);
        moodDatePicker.setConverter(dateConverter());

        moodNoteArea = new TextArea();
        moodNoteArea.setPromptText("optional note");
        moodNoteArea.getStyleClass().add("module-textarea");
        moodNoteArea.setPrefRowCount(3);
        moodNoteArea.setWrapText(true);

        // Prompts
        moodPromptsBox = new VBox(8);
        moodPromptsBox.getStyleClass().add("prompt-card");
        updatePrompts(null);

        // Inner card containing the actual mood form (matches the white card in screenshots)
        VBox innerCard = new VBox(14);
        innerCard.getStyleClass().add("section-card");

        Label innerTitle = new Label("Mood entry");
        innerTitle.getStyleClass().add("section-title");
        Label innerSub = new Label("Fill in the fields below and save when you're ready.");
        innerSub.getStyleClass().add("section-subtitle");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(22);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(78);
        grid.getColumnConstraints().addAll(c1, c2);

        int r = 0;
        grid.add(fieldLabel("Mood:"), 0, r);
        grid.add(moodTypeCombo, 1, r++);

        Label promptsLabel = fieldLabel("Mood-Based Prompts:");
        grid.add(promptsLabel, 0, r);
        grid.add(moodPromptsBox, 1, r++);

        grid.add(fieldLabel("Stress Level:"), 0, r);
        grid.add(stressCombo, 1, r++);

        grid.add(fieldLabel("Energy Level:"), 0, r);
        grid.add(energyCombo, 1, r++);

        grid.add(fieldLabel("Mood Date:"), 0, r);
        grid.add(moodDatePicker, 1, r++);

        grid.add(fieldLabel("Note:"), 0, r);
        grid.add(moodNoteArea, 1, r++);

        innerCard.getChildren().addAll(innerTitle, innerSub, grid);

        VBox card = new VBox(12, sectionTitle, sectionSub, innerCard);
        card.getStyleClass().add("section-card");
        return card;
    }

    private void updatePrompts(String moodType) {
        if (moodPromptsBox == null) return;
        moodPromptsBox.getChildren().clear();
        String[] prompts = promptsFor(moodType);
        for (String prompt : prompts) {
            Button b = new Button(prompt);
            b.getStyleClass().add("prompt-button");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> appendToNote(prompt));
            moodPromptsBox.getChildren().add(b);
        }
    }

    private void appendToNote(String prompt) {
        if (moodNoteArea == null) return;
        String existing = moodNoteArea.getText();
        String prefix = (existing == null || existing.isBlank()) ? "" : (existing + "\n");
        moodNoteArea.setText(prefix + "• " + prompt + "\n");
        moodNoteArea.positionCaret(moodNoteArea.getText().length());
    }

    private String[] promptsFor(String moodType) {
        if (moodType == null) {
            return new String[] {
                    "What is the strongest feeling in your body right now?",
                    "What triggered this mood today?",
                    "What do you need next?"
            };
        }
        return switch (moodType.toLowerCase(Locale.ROOT)) {
            case "happy", "excited", "grateful", "content" -> new String[] {
                    "What contributed to this good feeling?",
                    "Who would you like to share this with?",
                    "How can you carry this energy into tomorrow?"
            };
            case "sad", "depressed" -> new String[] {
                    "What is weighing on you the most right now?",
                    "What would feel kind to do for yourself in 5 minutes?",
                    "Who could you reach out to today?"
            };
            case "anxious", "stressed" -> new String[] {
                    "Take 3 slow breaths — what changes in your body?",
                    "What is one thing you can control right now?",
                    "What worry is loudest, and is it likely or unlikely?"
            };
            case "angry" -> new String[] {
                    "What boundary feels crossed?",
                    "What would you say if you felt heard already?",
                    "What helps your body release tension?"
            };
            default -> new String[] {
                    "What is the strongest feeling in your body right now?",
                    "What triggered this mood today?",
                    "What do you need next?"
            };
        };
    }

    private Node buildQuickActionsCard(boolean forMood) {
        Label sectionTitle = new Label("Quick actions");
        sectionTitle.getStyleClass().add("section-title");
        Label sectionSub = new Label("Save a new entry, update the selected row, or clear the form.");
        sectionSub.getStyleClass().add("section-subtitle");

        Button createBtn = new Button(forMood ? "Create Mood" : "Create Journal");
        createBtn.getStyleClass().add("btn-success-solid");

        Button updateBtn = new Button("Update selected");
        updateBtn.getStyleClass().add("btn-primary-solid");

        Button deleteBtn = new Button("Delete selected");
        deleteBtn.getStyleClass().add("btn-danger-solid");

        HBox primaryRow = new HBox(10, createBtn, updateBtn, deleteBtn);
        primaryRow.setAlignment(Pos.CENTER_LEFT);

        Button clearBtn = new Button("Clear form");
        clearBtn.getStyleClass().add("btn-pill-outline");
        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("btn-pill-outline");

        HBox secondaryRow = new HBox(10);
        secondaryRow.setAlignment(Pos.CENTER_LEFT);
        secondaryRow.getChildren().addAll(clearBtn, refreshBtn);

        if (forMood) {
            Button overwhelmedBtn = new Button("I feel overwhelmed");
            overwhelmedBtn.getStyleClass().add("btn-pill-outline");
            Button preventiveBtn = new Button("Preventive Wellness Assistant");
            preventiveBtn.getStyleClass().add("btn-pill-outline");
            Button exportPdfBtn = new Button("Export PDF");
            exportPdfBtn.getStyleClass().add("btn-pill-outline");
            secondaryRow.getChildren().addAll(overwhelmedBtn, preventiveBtn, exportPdfBtn);

            createBtn.setOnAction(e -> onCreateMood());
            updateBtn.setOnAction(e -> onUpdateMood());
            deleteBtn.setOnAction(e -> onDeleteMood());
            clearBtn.setOnAction(e -> clearMoodForm());
            refreshBtn.setOnAction(e -> { reloadMoods(); rebuildHeatmap(); });
            overwhelmedBtn.setOnAction(e -> onIFeelOverwhelmed());
            preventiveBtn.setOnAction(e -> onPreventiveWellness());
            exportPdfBtn.setOnAction(e -> onExportMoodPdf());

            moodStatusLabel = new Label("");
            moodStatusLabel.getStyleClass().add("module-status-label");

            VBox card = new VBox(12, sectionTitle, sectionSub, primaryRow, secondaryRow, moodStatusLabel);
            card.getStyleClass().add("section-card");
            return card;
        } else {
            Button hfBtn = new Button("Hugging Face Analyse");
            hfBtn.getStyleClass().add("btn-pill-outline");
            Button zenBtn = new Button("Zen Quote");
            zenBtn.getStyleClass().add("btn-pill-outline");
            Button translateBtn = new Button("Translate");
            translateBtn.getStyleClass().add("btn-pill-outline");
            Button explainBtn = new Button("Explain Words");
            explainBtn.getStyleClass().add("btn-pill-outline");
            Button exportBtn = new Button("Export PDF");
            exportBtn.getStyleClass().add("btn-pill-outline");
            secondaryRow.getChildren().addAll(hfBtn, zenBtn, translateBtn, explainBtn, exportBtn);

            createBtn.setOnAction(e -> onCreateJournal());
            updateBtn.setOnAction(e -> onUpdateJournal());
            deleteBtn.setOnAction(e -> onDeleteJournal());
            clearBtn.setOnAction(e -> clearJournalForm());
            refreshBtn.setOnAction(e -> reloadJournals());
            hfBtn.setOnAction(e -> onAiAnalyseJournal());
            zenBtn.setOnAction(e -> onZenQuote());
            translateBtn.setOnAction(e -> onTranslateJournal());
            explainBtn.setOnAction(e -> onExplainWords());
            exportBtn.setOnAction(e -> onExportJournalPdf());

            journalStatusLabel = new Label("");
            journalStatusLabel.getStyleClass().add("module-status-label");

            VBox card = new VBox(12, sectionTitle, sectionSub, primaryRow, secondaryRow, journalStatusLabel);
            card.getStyleClass().add("section-card");
            return card;
        }
    }

    private Node buildFindEntriesCard(boolean forMood) {
        Label sectionTitle = new Label("Find entries quickly");
        sectionTitle.getStyleClass().add("section-title");

        Label searchLabel = fieldLabel("Search");
        Label sortLabel = fieldLabel("Sort");

        if (forMood) {
            moodSearchField = new TextField();
            moodSearchField.setPromptText("type or note...");
            moodSearchField.getStyleClass().add("module-input");
            HBox.setHgrow(moodSearchField, Priority.ALWAYS);

            moodSortCombo = new ComboBox<>(FXCollections.observableArrayList(
                    "Date desc", "Date asc", "Stress high → low", "Stress low → high",
                    "Energy high → low", "Energy low → high"
            ));
            moodSortCombo.setValue("Date desc");
            moodSortCombo.getStyleClass().add("module-input");
            moodSortCombo.setPrefWidth(180);

            VBox left = new VBox(6, searchLabel, moodSearchField);
            HBox.setHgrow(left, Priority.ALWAYS);
            VBox right = new VBox(6, sortLabel, moodSortCombo);

            HBox row = new HBox(16, left, right);
            row.setAlignment(Pos.BOTTOM_LEFT);

            VBox card = new VBox(12, sectionTitle, row);
            card.getStyleClass().add("section-card");
            return card;
        } else {
            journalSearchField = new TextField();
            journalSearchField.setPromptText("title or content...");
            journalSearchField.getStyleClass().add("module-input");
            HBox.setHgrow(journalSearchField, Priority.ALWAYS);

            journalSortCombo = new ComboBox<>(FXCollections.observableArrayList(
                    "Date desc", "Date asc", "Title A → Z", "Title Z → A"
            ));
            journalSortCombo.setValue("Date desc");
            journalSortCombo.getStyleClass().add("module-input");
            journalSortCombo.setPrefWidth(180);

            VBox left = new VBox(6, searchLabel, journalSearchField);
            HBox.setHgrow(left, Priority.ALWAYS);
            VBox right = new VBox(6, sortLabel, journalSortCombo);

            HBox row = new HBox(16, left, right);
            row.setAlignment(Pos.BOTTOM_LEFT);

            VBox card = new VBox(12, sectionTitle, row);
            card.getStyleClass().add("section-card");
            return card;
        }
    }

    private Node buildMoodHistoryCard() {
        Label sectionTitle = new Label("Mood history");
        sectionTitle.getStyleClass().add("section-title");
        Label sectionSub = new Label("Select a row to edit existing information.");
        sectionSub.getStyleClass().add("section-subtitle");

        moodMetaLabel = new Label("0 total");
        moodMetaLabel.getStyleClass().add("section-meta");

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        VBox titleCol = new VBox(2, sectionTitle, sectionSub);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        titleRow.getChildren().addAll(titleCol, spacer, moodMetaLabel);

        moodTable = buildMoodTable();
        VBox.setVgrow(moodTable, Priority.ALWAYS);

        VBox card = new VBox(12, titleRow, moodTable);
        card.getStyleClass().add("section-card");
        return card;
    }

    private TableView<Mood> buildMoodTable() {
        TableView<Mood> tv = new TableView<>();
        tv.getStyleClass().add("module-table");
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tv.setPrefHeight(280);

        TableColumn<Mood, String> typeCol = new TableColumn<>("Mood Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("moodType"));
        typeCol.setPrefWidth(120);

        TableColumn<Mood, LocalDate> dateCol = new TableColumn<>("Mood Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("moodDate"));
        dateCol.setPrefWidth(110);
        dateCol.setCellFactory(c -> new TableCell<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(fmt));
            }
        });

        TableColumn<Mood, Integer> stressCol = new TableColumn<>("Stress");
        stressCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getStressLevel()));
        stressCol.setPrefWidth(80);

        TableColumn<Mood, Integer> energyCol = new TableColumn<>("Energy");
        energyCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getEnergyLevel()));
        energyCol.setPrefWidth(80);

        TableColumn<Mood, String> noteCol = new TableColumn<>("Note");
        noteCol.setCellValueFactory(new PropertyValueFactory<>("note"));
        noteCol.setPrefWidth(360);

        tv.getColumns().addAll(typeCol, dateCol, stressCol, energyCol, noteCol);

        FilteredList<Mood> filtered = new FilteredList<>(moods, m -> true);
        SortedList<Mood> sorted = new SortedList<>(filtered);
        tv.setItems(sorted);

        Runnable refresh = () -> {
            String q = moodSearchField == null || moodSearchField.getText() == null
                    ? "" : moodSearchField.getText().trim().toLowerCase();
            filtered.setPredicate(m -> {
                if (q.isEmpty()) return true;
                String type = m.getMoodType() == null ? "" : m.getMoodType().toLowerCase();
                String note = m.getNote() == null ? "" : m.getNote().toLowerCase();
                return type.contains(q) || note.contains(q);
            });

            String sort = moodSortCombo == null ? "Date desc" : moodSortCombo.getValue();
            Comparator<Mood> cmp = switch (sort == null ? "Date desc" : sort) {
                case "Date asc" -> Comparator.comparing(Mood::getMoodDate, Comparator.nullsLast(Comparator.naturalOrder()));
                case "Stress high → low" -> Comparator.comparing(
                        m -> m.getStressLevel() == null ? Integer.MIN_VALUE : m.getStressLevel(),
                        Comparator.reverseOrder());
                case "Stress low → high" -> Comparator.comparing(
                        m -> m.getStressLevel() == null ? Integer.MAX_VALUE : m.getStressLevel());
                case "Energy high → low" -> Comparator.comparing(
                        m -> m.getEnergyLevel() == null ? Integer.MIN_VALUE : m.getEnergyLevel(),
                        Comparator.reverseOrder());
                case "Energy low → high" -> Comparator.comparing(
                        m -> m.getEnergyLevel() == null ? Integer.MAX_VALUE : m.getEnergyLevel());
                default -> Comparator.comparing(Mood::getMoodDate, Comparator.nullsLast(Comparator.reverseOrder()));
            };
            sorted.setComparator(cmp);
        };

        if (moodSearchField != null) moodSearchField.textProperty().addListener((o, a, b) -> refresh.run());
        if (moodSortCombo != null) moodSortCombo.valueProperty().addListener((o, a, b) -> refresh.run());
        moods.addListener((javafx.collections.ListChangeListener<Mood>) c -> refresh.run());
        refresh.run();

        tv.getSelectionModel().selectedItemProperty().addListener((o, a, selected) -> {
            if (selected != null) populateMoodForm(selected);
        });

        return tv;
    }

    private Node buildMoodChartsCard() {
        Label sectionTitle = new Label("Insights");
        sectionTitle.getStyleClass().add("section-title");

        VBox pieBox = new VBox(8);
        pieBox.setAlignment(Pos.CENTER);
        Label pieTitle = new Label("Mood distribution");
        pieTitle.getStyleClass().add("section-title");
        moodPieChart = new PieChart();
        moodPieChart.setLegendVisible(true);
        moodPieChart.setLabelsVisible(false);
        moodPieChart.setPrefHeight(260);
        pieBox.getChildren().addAll(pieTitle, moodPieChart);

        VBox barBox = new VBox(8);
        barBox.setAlignment(Pos.CENTER);
        Label barTitle = new Label("Mood frequency");
        barTitle.getStyleClass().add("section-title");
        moodBarChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        moodBarChart.setLegendVisible(false);
        moodBarChart.setPrefHeight(260);
        barBox.getChildren().addAll(barTitle, moodBarChart);

        HBox row = new HBox(18, pieBox, barBox);
        HBox.setHgrow(pieBox, Priority.ALWAYS);
        HBox.setHgrow(barBox, Priority.ALWAYS);
        pieBox.setMaxWidth(Double.MAX_VALUE);
        barBox.setMaxWidth(Double.MAX_VALUE);

        VBox card = new VBox(12, sectionTitle, row);
        card.getStyleClass().add("section-card");
        return card;
    }

    // ==================================================================
    //  JOURNAL TAB
    // ==================================================================

    private Node buildJournalTab() {
        VBox container = new VBox(18);
        container.setPadding(new Insets(18, 4, 4, 4));

        container.getChildren().addAll(
                buildJournalEntryCard(),
                buildQuickActionsCard(false),
                buildFindEntriesCard(false),
                buildJournalHistoryCard()
        );

        return container;
    }

    private Node buildJournalEntryCard() {
        Label sectionTitle = new Label("Entry details");
        sectionTitle.getStyleClass().add("section-title");
        Label sectionSub = new Label("Fill in the fields below, then save or update your entry.");
        sectionSub.getStyleClass().add("section-subtitle");

        journalTitleField = new TextField();
        journalTitleField.setPromptText("journal title");
        journalTitleField.getStyleClass().add("module-input");

        journalContentArea = new TextArea();
        journalContentArea.setPromptText("write your entry...");
        journalContentArea.getStyleClass().add("module-textarea");
        journalContentArea.setPrefRowCount(6);
        journalContentArea.setWrapText(true);

        journalDatePicker = new DatePicker(LocalDate.now());
        journalDatePicker.getStyleClass().add("module-input");
        journalDatePicker.setMaxWidth(Double.MAX_VALUE);
        journalDatePicker.setConverter(dateConverter());

        journalMoodCombo = new ComboBox<>(moods);
        journalMoodCombo.setPromptText("Optional mood");
        journalMoodCombo.getStyleClass().add("module-input");
        journalMoodCombo.setMaxWidth(Double.MAX_VALUE);
        journalMoodCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Mood m) {
                if (m == null) return "";
                return m.getMoodType() + " — " + (m.getMoodDate() == null ? "" : m.getMoodDate());
            }
            @Override public Mood fromString(String s) { return null; }
        });

        VBox innerCard = new VBox(14);
        innerCard.getStyleClass().add("section-card");

        Label innerTitle = new Label("Journal entry");
        innerTitle.getStyleClass().add("section-title");
        Label innerSub = new Label("Fill in the fields below and save when you're ready.");
        innerSub.getStyleClass().add("section-subtitle");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(22);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(78);
        grid.getColumnConstraints().addAll(c1, c2);

        int r = 0;
        grid.add(fieldLabel("Title:"), 0, r);
        grid.add(journalTitleField, 1, r++);

        grid.add(fieldLabel("Content:"), 0, r);
        grid.add(journalContentArea, 1, r++);

        grid.add(fieldLabel("Entry Date:"), 0, r);
        grid.add(journalDatePicker, 1, r++);

        grid.add(fieldLabel("Mood:"), 0, r);
        grid.add(journalMoodCombo, 1, r++);

        innerCard.getChildren().addAll(innerTitle, innerSub, grid);

        VBox card = new VBox(12, sectionTitle, sectionSub, innerCard);
        card.getStyleClass().add("section-card");
        return card;
    }

    private Node buildJournalHistoryCard() {
        Label sectionTitle = new Label("Journal history");
        sectionTitle.getStyleClass().add("section-title");
        Label sectionSub = new Label("Select a row to edit existing information.");
        sectionSub.getStyleClass().add("section-subtitle");

        journalTable = buildJournalTable();
        VBox.setVgrow(journalTable, Priority.ALWAYS);

        VBox card = new VBox(12, sectionTitle, sectionSub, journalTable);
        card.getStyleClass().add("section-card");
        return card;
    }

    private TableView<Journal> buildJournalTable() {
        TableView<Journal> tv = new TableView<>();
        tv.getStyleClass().add("module-table");
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tv.setPrefHeight(280);

        TableColumn<Journal, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(180);

        TableColumn<Journal, String> contentCol = new TableColumn<>("Content");
        contentCol.setCellValueFactory(new PropertyValueFactory<>("content"));
        contentCol.setPrefWidth(400);

        TableColumn<Journal, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("entryDate"));
        dateCol.setPrefWidth(120);
        dateCol.setCellFactory(c -> new TableCell<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(fmt));
            }
        });

        TableColumn<Journal, Integer> moodCol = new TableColumn<>("Mood ID");
        moodCol.setCellValueFactory(new PropertyValueFactory<>("moodId"));
        moodCol.setPrefWidth(80);

        tv.getColumns().addAll(titleCol, contentCol, dateCol, moodCol);

        FilteredList<Journal> filtered = new FilteredList<>(journals, j -> true);
        SortedList<Journal> sorted = new SortedList<>(filtered);
        tv.setItems(sorted);

        Runnable refresh = () -> {
            String q = journalSearchField == null || journalSearchField.getText() == null
                    ? "" : journalSearchField.getText().trim().toLowerCase();
            filtered.setPredicate(j -> {
                if (q.isEmpty()) return true;
                String t = j.getTitle() == null ? "" : j.getTitle().toLowerCase();
                String ct = j.getContent() == null ? "" : j.getContent().toLowerCase();
                return t.contains(q) || ct.contains(q);
            });

            String sort = journalSortCombo == null ? "Date desc" : journalSortCombo.getValue();
            Comparator<Journal> cmp = switch (sort == null ? "Date desc" : sort) {
                case "Date asc" -> Comparator.comparing(Journal::getEntryDate, Comparator.nullsLast(Comparator.naturalOrder()));
                case "Title A → Z" -> Comparator.comparing(j -> safeLower(j.getTitle()));
                case "Title Z → A" -> Comparator.comparing((Journal j) -> safeLower(j.getTitle())).reversed();
                default -> Comparator.comparing(Journal::getEntryDate, Comparator.nullsLast(Comparator.reverseOrder()));
            };
            sorted.setComparator(cmp);
        };

        if (journalSearchField != null) journalSearchField.textProperty().addListener((o, a, b) -> refresh.run());
        if (journalSortCombo != null) journalSortCombo.valueProperty().addListener((o, a, b) -> refresh.run());
        journals.addListener((javafx.collections.ListChangeListener<Journal>) c -> refresh.run());
        refresh.run();

        tv.getSelectionModel().selectedItemProperty().addListener((o, a, selected) -> {
            if (selected != null) populateJournalForm(selected);
        });

        return tv;
    }

    // ==================================================================
    //  HEATMAP TAB
    // ==================================================================

    private Node buildHeatmapTab() {
        VBox container = new VBox(18);
        container.setPadding(new Insets(18, 4, 4, 4));

        Label sectionTitle = new Label("Emotional Heatmap Calendar");
        sectionTitle.getStyleClass().add("section-title");

        heatmapMonthLabel = new Label();
        heatmapMonthLabel.getStyleClass().add("section-subtitle");
        heatmapMonthLabel.setText("This month colored by emotional intensity. Dark red means difficult day, green means good day.");

        heatmapGrid = new GridPane();
        heatmapGrid.getStyleClass().add("heatmap-grid");
        heatmapGrid.setHgap(10);
        heatmapGrid.setVgap(10);

        HBox legend = new HBox(10);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.getChildren().addAll(
                legendChip("Great", "heatmap-day-great"),
                legendChip("Good", "heatmap-day-good"),
                legendChip("Okay", "heatmap-day-okay"),
                legendChip("Rough", "heatmap-day-rough"),
                legendChip("Bad", "heatmap-day-bad")
        );

        VBox card = new VBox(14, sectionTitle, heatmapMonthLabel, heatmapGrid, legend);
        card.getStyleClass().add("section-card");

        container.getChildren().add(card);
        return container;
    }

    private Node legendChip(String text, String styleClass) {
        Label dot = new Label();
        dot.getStyleClass().addAll("heatmap-day", styleClass);
        dot.setMinSize(18, 18);
        dot.setPrefSize(18, 18);
        dot.setMaxSize(18, 18);
        Label name = new Label(text);
        name.getStyleClass().add("section-subtitle");
        HBox h = new HBox(6, dot, name);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private void rebuildHeatmap() {
        if (heatmapGrid == null) return;
        heatmapGrid.getChildren().clear();

        YearMonth ym = YearMonth.now();
        if (heatmapMonthLabel != null) {
            heatmapMonthLabel.setText("This month (" + ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                    + " " + ym.getYear() + ") colored by emotional intensity. Dark red means difficult day, green means good day.");
        }

        // Weekday headers (Mon-Sun)
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < days.length; i++) {
            Label d = new Label(days[i]);
            d.getStyleClass().add("heatmap-weekday");
            d.setMinWidth(64);
            d.setAlignment(Pos.CENTER);
            heatmapGrid.add(d, i, 0);
        }

        Map<LocalDate, List<Mood>> byDate = new HashMap<>();
        for (Mood m : moods) {
            if (m.getMoodDate() != null && m.getMoodDate().getYear() == ym.getYear()
                    && m.getMoodDate().getMonth() == ym.getMonth()) {
                byDate.computeIfAbsent(m.getMoodDate(), k -> new java.util.ArrayList<>()).add(m);
            }
        }

        int firstDayOffset = (ym.atDay(1).getDayOfWeek().getValue() + 6) % 7; // Monday = 0
        int totalDays = ym.lengthOfMonth();

        for (int day = 1; day <= totalDays; day++) {
            int row = (firstDayOffset + day - 1) / 7 + 1;
            int col = (firstDayOffset + day - 1) % 7;

            LocalDate date = ym.atDay(day);
            Label cell = new Label(String.valueOf(day));
            cell.getStyleClass().add("heatmap-day");
            String moodClass = colorClassFor(byDate.get(date));
            if (moodClass != null) cell.getStyleClass().add(moodClass);
            if (date.equals(LocalDate.now())) cell.getStyleClass().add("heatmap-day-today");
            heatmapGrid.add(cell, col, row);
        }
    }

    private String colorClassFor(List<Mood> entries) {
        if (entries == null || entries.isEmpty()) return null;
        double score = 0;
        int counted = 0;
        for (Mood m : entries) {
            Integer s = m.getStressLevel();
            Integer e = m.getEnergyLevel();
            String t = m.getMoodType() == null ? "" : m.getMoodType().toLowerCase(Locale.ROOT);
            int base = switch (t) {
                case "happy", "excited", "grateful", "content", "calm" -> 8;
                case "neutral" -> 5;
                case "anxious", "stressed", "angry" -> 3;
                case "sad", "depressed" -> 2;
                default -> 5;
            };
            int adjusted = base;
            if (s != null) adjusted -= Math.max(0, s - 5) / 2;
            if (e != null) adjusted += Math.max(0, e - 5) / 3;
            score += Math.max(0, Math.min(10, adjusted));
            counted++;
        }
        if (counted == 0) return null;
        double avg = score / counted;
        if (avg >= 8) return "heatmap-day-great";
        if (avg >= 6) return "heatmap-day-good";
        if (avg >= 4) return "heatmap-day-okay";
        if (avg >= 2) return "heatmap-day-rough";
        return "heatmap-day-bad";
    }

    // ==================================================================
    //  Mood actions
    // ==================================================================

    private void onCreateMood() {
        try {
            String type = moodTypeCombo.getValue();
            if (type == null || type.isBlank()) {
                setMoodStatus("Please select a mood type.");
                return;
            }
            Integer stress = parseLevel(stressCombo.getValue());
            Integer energy = parseLevel(energyCombo.getValue());
            Mood created = moodService.createMood(
                    type,
                    moodDatePicker.getValue() == null ? LocalDate.now() : moodDatePicker.getValue(),
                    safeTrim(moodNoteArea.getText()),
                    stress,
                    energy,
                    null, null, null, null
            );
            setMoodStatus("Mood entry #" + created.getId() + " created.");
            clearMoodForm();
            reloadMoods();
            rebuildHeatmap();
            updateCharts();
        } catch (ServiceException e) {
            setMoodStatus("Error: " + e.getMessage());
        }
    }

    private void onUpdateMood() {
        Mood selected = moodTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setMoodStatus("Select a mood entry first.");
            return;
        }
        try {
            String type = moodTypeCombo.getValue();
            if (type == null || type.isBlank()) {
                setMoodStatus("Please select a mood type.");
                return;
            }
            moodService.updateMood(
                    selected.getId(),
                    type,
                    moodDatePicker.getValue() == null ? LocalDate.now() : moodDatePicker.getValue(),
                    safeTrim(moodNoteArea.getText()),
                    parseLevel(stressCombo.getValue()),
                    parseLevel(energyCombo.getValue()),
                    null, null, null,
                    selected.getAdminComment(),
                    selected.isSupportEmailSent(),
                    selected.getCaseStatus()
            );
            setMoodStatus("Mood entry #" + selected.getId() + " updated.");
            reloadMoods();
            rebuildHeatmap();
            updateCharts();
        } catch (ServiceException e) {
            setMoodStatus("Error: " + e.getMessage());
        }
    }

    private void onDeleteMood() {
        Mood selected = moodTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setMoodStatus("Select a mood entry first.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete the selected mood entry?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Confirm delete");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    moodService.deleteMood(selected.getId());
                    setMoodStatus("Mood entry deleted.");
                    clearMoodForm();
                    reloadMoods();
                    rebuildHeatmap();
                    updateCharts();
        } catch (ServiceException e) {
                    setMoodStatus("Error: " + e.getMessage());
                }
            }
        });
    }

    private void onIFeelOverwhelmed() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Feeling overwhelmed");
        dialog.setHeaderText("We will send a quick support email.");
        dialog.setContentText("Your email address:");
        dialog.showAndWait().ifPresent(email -> {
            String trimmed = email == null ? "" : email.trim();
            if (trimmed.isEmpty()) {
                setMoodStatus("Email required.");
                return;
            }
            try {
                Mood mood = createOrGetTodayMood("Overwhelmed");
                MoodSupportEmailService mail = new MoodSupportEmailService();
                mail.sendSupportEmailToStudent(trimmed, mood,
                        "We noticed you're feeling overwhelmed. Here are some grounding tips and resources.");
                moodService.markSupportEmailSent(mood.getId());
                setMoodStatus("Support email sent to " + trimmed + ".");
                reloadMoods();
                rebuildHeatmap();
            } catch (Exception e) {
                setMoodStatus("Could not send support email: " + e.getMessage());
            }
        });
    }

    private Mood createOrGetTodayMood(String type) throws ServiceException {
        // Try to use the current selection if available; otherwise create a quick entry.
        Mood selected = moodTable == null ? null : moodTable.getSelectionModel().getSelectedItem();
        if (selected != null) return selected;
        return moodService.createMood(type, LocalDate.now(),
                "Created from 'I feel overwhelmed' quick action.",
                8, 3, null, null, null, null);
    }

    private void onPreventiveWellness() {
        try {
            WellnessReportService svc = new WellnessReportService();
            String report = svc.generateReport(
                    moodService.getAllMoods(),
                    journalService.getAllJournals(),
                    LocalDate.now());

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Preventive Wellness Assistant");
            alert.setHeaderText("Your weekly snapshot");
            TextArea ta = new TextArea(report);
            ta.setEditable(false);
            ta.setWrapText(true);
            ta.setPrefSize(620, 380);
            ta.getStyleClass().add("module-textarea");
            alert.getDialogPane().setContent(ta);
            alert.getDialogPane().getStylesheets().add(
                    getClass().getResource("/com/mindcare/styles/orion-theme.css").toExternalForm());
            alert.showAndWait();
            setMoodStatus("Wellness report generated.");
        } catch (Exception e) {
            setMoodStatus("Could not generate report: " + e.getMessage());
        }
    }

    private void onExportMoodPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export moods to PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName("moods.pdf");
        File file = chooser.showSaveDialog(null);
        if (file == null) return;
        try {
            Path out = new MoodJournalPdfService().exportMoods(file.toPath(), moods);
            setMoodStatus("Moods exported: " + out.toAbsolutePath());
        } catch (Exception e) {
            setMoodStatus("Export failed: " + e.getMessage());
        }
    }

    private void clearMoodForm() {
        moodTable.getSelectionModel().clearSelection();
        moodTypeCombo.setValue(null);
        stressCombo.setValue(null);
        energyCombo.setValue(null);
        moodDatePicker.setValue(LocalDate.now());
        moodNoteArea.clear();
    }

    private void populateMoodForm(Mood mood) {
        moodTypeCombo.setValue(matchKnown(mood.getMoodType(), MOOD_TYPES));
        stressCombo.setValue(levelLabel(mood.getStressLevel(), STRESS_LABELS));
        energyCombo.setValue(levelLabel(mood.getEnergyLevel(), ENERGY_LABELS));
        moodDatePicker.setValue(mood.getMoodDate() == null ? LocalDate.now() : mood.getMoodDate());
        moodNoteArea.setText(mood.getNote() == null ? "" : mood.getNote());
    }

    // ==================================================================
    //  Journal actions
    // ==================================================================

    private void onCreateJournal() {
        String title = safeTrim(journalTitleField.getText());
        if (title == null || title.isEmpty()) {
            setJournalStatus("Title is required.");
            return;
        }
        try {
            Mood selectedMood = journalMoodCombo.getValue();
            Integer moodId = selectedMood == null ? null : selectedMood.getId();
            Journal created = journalService.createJournal(
                    title,
                    safeTrim(journalContentArea.getText()),
                    journalDatePicker.getValue() == null ? LocalDate.now() : journalDatePicker.getValue(),
                    moodId,
                    null);
            setJournalStatus("Journal entry #" + created.getId() + " created.");
            clearJournalForm();
            reloadJournals();
        } catch (SQLException e) {
            setJournalStatus("Error: " + e.getMessage());
        }
    }

    private void onUpdateJournal() {
        Journal selected = journalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setJournalStatus("Select a journal entry first.");
            return;
        }
        try {
            Mood selectedMood = journalMoodCombo.getValue();
            Integer moodId = selectedMood == null ? null : selectedMood.getId();
            journalService.updateJournal(
                    selected.getId(),
                    safeTrim(journalTitleField.getText()),
                    safeTrim(journalContentArea.getText()),
                    journalDatePicker.getValue() == null ? LocalDate.now() : journalDatePicker.getValue(),
                    moodId);
            setJournalStatus("Journal entry #" + selected.getId() + " updated.");
            reloadJournals();
        } catch (SQLException e) {
            setJournalStatus("Error: " + e.getMessage());
        }
    }

    private void onDeleteJournal() {
        Journal selected = journalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setJournalStatus("Select a journal entry first.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete the selected journal entry?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Confirm delete");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    journalService.deleteJournal(selected.getId());
                    setJournalStatus("Journal entry deleted.");
                    clearJournalForm();
                    reloadJournals();
                } catch (SQLException e) {
                    setJournalStatus("Error: " + e.getMessage());
                }
            }
        });
    }

    private void onAiAnalyseJournal() {
        Journal selected = journalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setJournalStatus("Select a journal entry first.");
            return;
        }
        try {
            JournalAiAnalysisService svc = new JournalAiAnalysisService();
            String result = svc.analyze(selected);
            showInfoDialog("Hugging Face Analyse", "AI analysis result", result);
            setJournalStatus("Analysis complete.");
        } catch (Exception e) {
            setJournalStatus("AI analysis failed: " + e.getMessage());
        }
    }

    private void onZenQuote() {
        String quote = ZEN_QUOTES[(int) (Math.random() * ZEN_QUOTES.length)];
        showInfoDialog("Zen Quote", "A moment of calm", quote);
        setJournalStatus("Zen quote shown.");
    }

    private void onTranslateJournal() {
        Journal selected = journalTable.getSelectionModel().getSelectedItem();
        String text = selected != null ? selected.getContent() : journalContentArea.getText();
        if (text == null || text.isBlank()) {
            setJournalStatus("Nothing to translate. Pick an entry or write something first.");
            return;
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>("English",
                "English", "French", "Spanish", "German", "Italian", "Arabic");
        dialog.setTitle("Translate");
        dialog.setHeaderText("Translate journal text");
        dialog.setContentText("Target language:");
        dialog.showAndWait().ifPresent(target -> {
            String code = switch (target) {
                case "French" -> "fr";
                case "Spanish" -> "es";
                case "German" -> "de";
                case "Italian" -> "it";
                case "Arabic" -> "ar";
                default -> "en";
            };
            String translated = TranslationService.translate(text, "auto", code);
            showInfoDialog("Translation", "Translated to " + target, translated);
            setJournalStatus("Translation complete.");
        });
    }

    private void onExplainWords() {
        Journal selected = journalTable.getSelectionModel().getSelectedItem();
        String text = selected != null ? selected.getContent() : journalContentArea.getText();
        if (text == null || text.isBlank()) {
            setJournalStatus("Pick an entry or write something to extract key words.");
            return;
        }
        // Simple keyword extraction: longest distinct words, ignoring stop words.
        java.util.Set<String> stop = java.util.Set.of(
                "the", "and", "for", "but", "with", "this", "that", "have", "from",
                "your", "you're", "it's", "are", "was", "were", "you", "they", "their",
                "what", "when", "where", "which", "into", "about", "would", "could", "should",
                "very", "much", "more", "less");
        java.util.LinkedHashMap<String, Integer> freq = new java.util.LinkedHashMap<>();
        for (String raw : text.toLowerCase(Locale.ROOT).split("[^a-zA-Zàâçéèêëîïôûùüÿñæœ']+")) {
            if (raw.length() < 5 || stop.contains(raw)) continue;
            freq.merge(raw, 1, Integer::sum);
        }
        List<Map.Entry<String, Integer>> top = freq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(8)
                .toList();
        StringBuilder sb = new StringBuilder("Most prominent words in this entry:\n\n");
        if (top.isEmpty()) {
            sb.append("(no significant words found)");
        } else {
            for (Map.Entry<String, Integer> e : top) {
                sb.append("• ").append(e.getKey()).append("  (×").append(e.getValue()).append(")\n");
            }
            sb.append("\nReflect on what each of these words is doing for you right now.");
        }
        showInfoDialog("Explain Words", "Key words in your entry", sb.toString());
        setJournalStatus("Key words extracted.");
    }

    private void onExportJournalPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export journals to PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName("journals.pdf");
        File file = chooser.showSaveDialog(null);
        if (file == null) return;
        try {
            Path out = new MoodJournalPdfService().exportJournals(file.toPath(), journals);
            setJournalStatus("Journals exported: " + out.toAbsolutePath());
        } catch (Exception e) {
            setJournalStatus("Export failed: " + e.getMessage());
        }
    }

    private void clearJournalForm() {
        journalTable.getSelectionModel().clearSelection();
        journalTitleField.clear();
        journalContentArea.clear();
        journalDatePicker.setValue(LocalDate.now());
        journalMoodCombo.setValue(null);
    }

    private void populateJournalForm(Journal j) {
        journalTitleField.setText(j.getTitle() == null ? "" : j.getTitle());
        journalContentArea.setText(j.getContent() == null ? "" : j.getContent());
        journalDatePicker.setValue(j.getEntryDate() == null ? LocalDate.now() : j.getEntryDate());
        if (j.getMoodId() != null) {
            for (Mood m : moods) {
                if (m.getId() == j.getMoodId()) {
                    journalMoodCombo.setValue(m);
                break;
            }
            }
        } else {
            journalMoodCombo.setValue(null);
        }
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    private void reloadMoods() {
        try {
            List<Mood> all = moodService.getAllMoods();
            moods.setAll(all);
            updateMoodMeta(all);
            updateCharts();
        } catch (ServiceException e) {
            setMoodStatus("Failed to load moods: " + e.getMessage());
        }
    }

    private void reloadJournals() {
        try {
            List<Journal> all = journalService.getAllJournals();
            journals.setAll(all);
            setJournalStatus(all.size() + " journal entries loaded.");
        } catch (SQLException e) {
            setJournalStatus("Failed to load journals: " + e.getMessage());
        }
    }

    private void updateMoodMeta(List<Mood> all) {
        if (moodMetaLabel == null) return;
        if (all.isEmpty()) {
            moodMetaLabel.setText("0 total");
                return;
        }
        Map<String, Long> byType = new HashMap<>();
        double stressSum = 0, energySum = 0;
        int stressCount = 0, energyCount = 0;
        for (Mood m : all) {
            String t = m.getMoodType() == null ? "Unknown" : m.getMoodType();
            byType.merge(t, 1L, Long::sum);
            if (m.getStressLevel() != null) { stressSum += m.getStressLevel(); stressCount++; }
            if (m.getEnergyLevel() != null) { energySum += m.getEnergyLevel(); energyCount++; }
        }
        Map.Entry<String, Long> top = byType.entrySet().stream()
                .max(Map.Entry.comparingByValue()).orElse(null);
        StringBuilder sb = new StringBuilder();
        sb.append(all.size()).append(" total | ").append(byType.size()).append(" types");
        if (top != null) sb.append(" | Top: ").append(top.getKey()).append(" (").append(top.getValue()).append(")");
        if (stressCount > 0) sb.append(" | Avg stress: ").append(String.format(Locale.US, "%.1f", stressSum / stressCount));
        if (energyCount > 0) sb.append(" | Avg energy: ").append(String.format(Locale.US, "%.1f", energySum / energyCount));
        moodMetaLabel.setText(sb.toString());
    }

    private void updateCharts() {
        if (moodPieChart == null || moodBarChart == null) return;

        Map<String, Long> counts = new java.util.LinkedHashMap<>();
        for (Mood m : moods) {
            String t = m.getMoodType() == null || m.getMoodType().isBlank() ? "Unknown" : m.getMoodType();
            counts.merge(t, 1L, Long::sum);
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        counts.forEach((k, v) -> pieData.add(new PieChart.Data(k, v)));
        moodPieChart.setData(pieData);

        moodBarChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Frequency");
        counts.forEach((k, v) -> series.getData().add(new XYChart.Data<>(k, v)));
        moodBarChart.getData().add(series);
    }

    private void ensureSchema() {
        try {
            SchemaInitializer.ensureSchema();
        } catch (SQLException ignored) {
            // best-effort; UI still loads.
        }
    }

    private void setMoodStatus(String msg) {
        if (moodStatusLabel != null) moodStatusLabel.setText(msg == null ? "" : msg);
    }

    private void setJournalStatus(String msg) {
        if (journalStatusLabel != null) journalStatusLabel.setText(msg == null ? "" : msg);
    }

    private static String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static Integer parseLevel(String label) {
        if (label == null) return null;
        try {
            return Integer.parseInt(label.split("[^0-9]")[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private static String levelLabel(Integer value, String[] labels) {
        if (value == null || value < 1 || value > labels.length) return null;
        return labels[value - 1];
    }

    private static String matchKnown(String value, String[] options) {
        if (value == null) return null;
        for (String o : options) {
            if (o.equalsIgnoreCase(value)) return o;
        }
        return value;
    }

    private static Label fieldLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }

    private static StringConverter<LocalDate> dateConverter() {
        return new StringConverter<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            @Override public String toString(LocalDate d) { return d == null ? "" : d.format(fmt); }
            @Override public LocalDate fromString(String s) {
                if (s == null || s.isBlank()) return null;
                try { return LocalDate.parse(s.trim(), fmt); }
                catch (Exception e) { return null; }
            }
        };
    }

    private void showInfoDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        TextArea ta = new TextArea(content == null ? "" : content);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefSize(560, 320);
        ta.getStyleClass().add("module-textarea");
        alert.getDialogPane().setContent(ta);
        try {
            alert.getDialogPane().getStylesheets().add(
                    getClass().getResource("/com/mindcare/styles/orion-theme.css").toExternalForm());
        } catch (Exception ignored) { }
        alert.showAndWait();
    }
}
