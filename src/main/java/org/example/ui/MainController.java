package org.example.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.model.Journal;
import org.example.model.Mood;
import org.example.repository.JournalRepository;
import org.example.repository.MoodRepository;
import org.example.service.AppService;
import org.example.service.FormValidator;
import org.example.service.HuggingFaceJournalAnalysisService;
import org.example.service.LibreTranslateService;
import org.example.service.PreventiveWellnessAssistantService;
import org.example.service.ServiceException;
import org.example.service.ValidationException;
import org.example.service.WiktionaryEmotionService;
import org.example.service.ZenQuotesService;
import org.example.ui.template.CRUDTabTemplate;
import org.example.ui.template.FormTemplate;
import org.example.ui.template.StyledMainLayoutTemplate;
import org.example.ui.template.ThemeStyle;
import org.example.ui.theme.ThemeManager;
import org.example.ui.emoji.MoodEmojiManager;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MainController {
    private final boolean adminUser;
    private final String currentUsername;
    private final Runnable onBackToLogin;

    // Mood fields
    private ComboBox<String> moodTypeField;
    private ComboBox<Integer> moodStressLevelField;
    private ComboBox<Integer> moodEnergyLevelField;
    private DatePicker moodDatePicker;
    private TextArea moodNoteArea;
    private TableView<Mood> moodTable;
    private TableColumn<Mood, String> moodTypeColumn;
    private TableColumn<Mood, String> moodDateColumn;
    private TableColumn<Mood, String> moodStressColumn;
    private TableColumn<Mood, String> moodEnergyColumn;
    private TableColumn<Mood, String> moodNoteColumn;
    private TextField moodSearchField;
    private ComboBox<String> moodSortCombo;
    private Label moodStatsLabel;

    // Journal fields
    private TextField journalTitleField;
    private TextArea journalContentArea;
    private DatePicker journalDatePicker;
    private ComboBox<MoodOption> journalMoodCombo;
    private TableView<Journal> journalTable;
    private TableColumn<Journal, Integer> journalIdColumn;
    private TableColumn<Journal, String> journalTitleColumn;
    private TableColumn<Journal, String> journalContentColumn;
    private TableColumn<Journal, String> journalDateColumn;
    private TableColumn<Journal, String> journalMoodIdColumn;
    private TextField journalSearchField;
    private ComboBox<String> journalSortCombo;
    private Label journalStatsLabel;
    private VBox journalMoodPromptsBox;

    // Services
    private final AppService appService = AppService.getInstance();
    
    // Repositories (kept for compatibility)
    private final MoodRepository moodRepository = new MoodRepository();
    private final JournalRepository journalRepository = new JournalRepository();

    // Observable lists
    private final ObservableList<Mood> allMoods = FXCollections.observableArrayList();
    private final ObservableList<Mood> moodItems = FXCollections.observableArrayList();
    private final ObservableList<Journal> allJournals = FXCollections.observableArrayList();
    private final ObservableList<Journal> journalItems = FXCollections.observableArrayList();
    private final ObservableList<MoodOption> journalMoodOptions = FXCollections.observableArrayList();

    // Charts for Mood display
    private javafx.scene.layout.HBox chartsBox;
    private javafx.scene.chart.PieChart moodPieChart;
    private javafx.scene.chart.BarChart<String, Number> moodBarChart;
    private VBox moodHeatmapBox;
    private javafx.scene.Scene appScene;
    private final Integer currentUserId;
    
    public MainController() {
        this("admin", true, null);
    }

    public MainController(String currentUsername, boolean adminUser) {
        this(currentUsername, adminUser, null);
    }

    public MainController(String currentUsername, boolean adminUser, Runnable onBackToLogin) {
        this(null, currentUsername, adminUser, onBackToLogin);
    }

    public MainController(Integer currentUserId, String currentUsername, boolean adminUser, Runnable onBackToLogin) {
        this.currentUserId = currentUserId;
        this.currentUsername = currentUsername;
        this.adminUser = adminUser;
        this.onBackToLogin = onBackToLogin;
    }

    public Parent createView() {
        // Build Mood Tab with charts
        javafx.scene.control.Tab moodTabWithCharts = buildMoodTabComplete();
        
        // Build Journal Tab
        CRUDTabTemplate<Journal> journalTabTemplate = buildJournalTab();
        
        // Build Heatmap Tab
        javafx.scene.control.Tab heatmapTab = buildHeatmapTab();

        // Use Styled Main Layout Template with admin button
        StyledMainLayoutTemplate mainLayout = new StyledMainLayoutTemplate(
                "MindCare",
                adminUser
                        ? "Track your moods, capture your thoughts, and notice your patterns."
                        : "Bienvenue " + capitalize(currentUsername) + ". Track your moods, capture your thoughts, and notice your patterns.",
                adminUser ? "Admin dashboard" : "Retour login",
                adminUser ? this::handleAdminPanel : onBackToLogin,
                adminUser ? null : "Export PDF",
                adminUser ? null : this::handleExportPdf,
                moodTabWithCharts,
                journalTabTemplate.build(),
                heatmapTab
        );

        Parent root = mainLayout.build();
        
        // Apply CSS styling
        root.getStylesheets().add(ThemeStyle.getCssDataUri());

        initialize();
        
        // Store scene for theme switching
        if (root.getScene() != null) {
            currentScene = root.getScene();
        }
        
        return root;
    }
    
    private javafx.scene.Scene currentScene;

    private CRUDTabTemplate<Mood> buildMoodTab() {
        // Initialize form fields
        moodTypeField = new ComboBox<>();
        moodTypeField.setItems(FXCollections.observableArrayList(
                "Happy", "Sad", "Angry", "Calm", "Excited", 
                "Anxious", "Stressed", "Depressed", "Grateful", "Content"
        ));
        moodTypeField.setPrefWidth(300);
        moodStressLevelField = createLevelComboBox("stress level");
        moodEnergyLevelField = createLevelComboBox("energy level");
        moodDatePicker = new DatePicker();
        moodDatePicker.setValue(LocalDate.now());
        moodNoteArea = new TextArea();
        moodNoteArea.setPromptText("optional note");
        moodNoteArea.setPrefRowCount(3);
        configureDatePicker(moodDatePicker);
        configureTextLimit(moodNoteArea, FormValidator.MOOD_NOTE_MAX_LENGTH);

        // Create form
        FormTemplate moodForm = new FormTemplate()
                .setTitle("Mood entry")
                .addField("Mood Type:", moodTypeField)
                .addField("Stress Level:", moodStressLevelField)
                .addField("Energy Level:", moodEnergyLevelField)
                .addField("Mood Date:", moodDatePicker)
                .addField("Note:", moodNoteArea)
                .setPadding(new Insets(0));

        // Initialize table
        moodTable = new TableView<>();
        moodTypeColumn = new TableColumn<>("Mood Type");
        moodTypeColumn.setPrefWidth(170);
        moodDateColumn = new TableColumn<>("Mood Date");
        moodDateColumn.setPrefWidth(150);
        moodStressColumn = new TableColumn<>("Stress");
        moodStressColumn.setPrefWidth(100);
        moodEnergyColumn = new TableColumn<>("Energy");
        moodEnergyColumn.setPrefWidth(100);
        moodNoteColumn = new TableColumn<>("Note");
        moodNoteColumn.setPrefWidth(360);
        moodTable.getColumns().addAll(moodTypeColumn, moodDateColumn, moodStressColumn, moodEnergyColumn, moodNoteColumn);

        // Initialize filter fields
        moodSearchField = new TextField();
        moodSearchField.setPrefWidth(220);
        moodSortCombo = new ComboBox<>();
        moodStatsLabel = new Label("No entries yet");

        // Build the CRUD template
        CRUDTabTemplate<Mood> moodTab = new CRUDTabTemplate<>("Mood", moodForm, moodTable)
                .setSearchPlaceholder("type ou note...")
                .setSortOptions(List.of("Date desc", "Date asc", "Type A-Z", "Type Z-A"))
                .withActions(
                        this::handleCreateMood,
                        this::handleUpdateMood,
                        this::handleDeleteMood,
                        this::handleClearMoodForm,
                        this::handleRefreshMoods
                )
                .withFilters(
                        this::handleApplyMoodFilters,
                        this::handleResetMoodFilters
                )
                .withSecondaryAction("I feel overwhelmed", this::handleEmergencyMode)
                .withSecondaryAction("Preventive Wellness Assistant", this::handlePreventiveWellnessAssistant);

        // Store references for filter operations
        moodSearchField = moodTab.getSearchField();
        moodSortCombo = moodTab.getSortCombo();
        moodStatsLabel = moodTab.getStatsLabel();

        return moodTab;
    }

    private javafx.scene.control.Tab buildMoodTabComplete() {
        // First build the basic CRUD tab
        CRUDTabTemplate<Mood> moodTab = buildMoodTab();
        javafx.scene.control.Tab baseTab = moodTab.build();

        javafx.scene.control.ScrollPane scrollPane = (javafx.scene.control.ScrollPane) baseTab.getContent();
        VBox content = (VBox) scrollPane.getContent();
        
        // Add charts if there is data
        try {
            moodPieChart = createMoodPieChart();
            moodBarChart = createMoodBarChart();
            
            // Create charts section
            Label chartsTitle = new Label("Mood insights");
            chartsTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #1C4F96; -fx-padding: 6 4 0 4;");
            
            chartsBox = new HBox(20);
            chartsBox.setPadding(new Insets(4, 0, 0, 0));
            chartsBox.setStyle("-fx-background-color: transparent;");
            chartsBox.getChildren().addAll(moodPieChart, moodBarChart);
            HBox.setHgrow(moodPieChart, javafx.scene.layout.Priority.ALWAYS);
            HBox.setHgrow(moodBarChart, javafx.scene.layout.Priority.ALWAYS);
            
            // Add charts to content
            content.getChildren().addAll(chartsTitle, chartsBox);

            // Heatmap moved to separate tab
        } catch (Exception e) {
            System.out.println("Charts error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return baseTab;
    }

    private javafx.scene.chart.PieChart createMoodPieChart() {
        java.util.Map<String, Long> moodDistribution = allMoods.stream()
                .collect(Collectors.groupingBy(
                        m -> capitalize(m.getMoodType()),
                        Collectors.counting()
                ));

        javafx.collections.ObservableList<javafx.scene.chart.PieChart.Data> pieData =
                FXCollections.observableArrayList();
        for (java.util.Map.Entry<String, Long> entry : moodDistribution.entrySet()) {
            javafx.scene.chart.PieChart.Data data = new javafx.scene.chart.PieChart.Data(entry.getKey(), entry.getValue());
            pieData.add(data);
        }

        javafx.scene.chart.PieChart chart = new javafx.scene.chart.PieChart(pieData);
        chart.setTitle("Mood distribution");
        chart.setLegendVisible(true);
        chart.setLabelsVisible(true);
        chart.setPrefSize(360, 300);
        chart.setMinHeight(300);
        chart.setStyle("-fx-font-size: 11px;");
        return chart;
    }

    private javafx.scene.chart.BarChart<String, Number> createMoodBarChart() {
        java.util.Map<String, Long> moodCounts = allMoods.stream()
                .collect(Collectors.groupingBy(
                        m -> capitalize(m.getMoodType()),
                        Collectors.counting()
                ));

        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        xAxis.setLabel("Mood");
        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        yAxis.setLabel("Count");

        javafx.scene.chart.BarChart<String, Number> barChart = 
                new javafx.scene.chart.BarChart<>(xAxis, yAxis);
        barChart.setTitle("Mood frequency");
        barChart.setLegendVisible(false);
        barChart.setPrefSize(360, 300);
        barChart.setMinHeight(300);
        barChart.setStyle("-fx-font-size: 11px;");

        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName("Moods");

        for (java.util.Map.Entry<String, Long> entry : moodCounts.entrySet()) {
            series.getData().add(new javafx.scene.chart.XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        barChart.getData().add(series);
        return barChart;
    }

    private VBox createMoodHeatmapCalendar() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(22));
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-background-radius: 26;" +
                        "-fx-border-color: #CFE3FF;" +
                        "-fx-border-radius: 26;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(34,49,63,0.07), 20, 0.14, 0, 6);"
        );

        Label title = new Label("Emotional Heatmap Calendar");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #1C4F96;");

        Label subtitle = new Label("This month colored by emotional intensity. Dark red means difficult day, green means good day.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #6F87A6;");

        GridPane grid = buildHeatmapGrid(YearMonth.now());
        HBox legend = createHeatmapLegend();

        card.getChildren().addAll(title, subtitle, grid, legend);
        return card;
    }

    private GridPane buildHeatmapGrid(YearMonth month) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(8, 0, 2, 0));

        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int col = 0; col < days.length; col++) {
            Label day = new Label(days[col]);
            day.setMinWidth(74);
            day.setAlignment(Pos.CENTER);
            day.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #6F87A6;");
            grid.add(day, col, 0);
        }

        LocalDate firstDay = month.atDay(1);
        int firstColumn = firstDay.getDayOfWeek().getValue() - 1;
        int length = month.lengthOfMonth();
        for (int day = 1; day <= length; day++) {
            LocalDate date = month.atDay(day);
            int index = firstColumn + day - 1;
            int row = index / 7 + 1;
            int col = index % 7;
            grid.add(createHeatmapCell(date), col, row);
        }
        return grid;
    }

    private StackPane createHeatmapCell(LocalDate date) {
        List<Mood> moodsForDay = allMoods.stream()
                .filter(mood -> date.equals(mood.getMoodDate()))
                .toList();
        double score = moodsForDay.stream()
                .mapToDouble(this::moodHeatScore)
                .average()
                .orElse(0.0);
        String color = heatmapColor(score, moodsForDay.isEmpty());

        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: " + heatmapTextColor(score, moodsForDay.isEmpty()) + ";");

        // Add emoji for the primary mood
        String emojiText = "";
        if (!moodsForDay.isEmpty()) {
            String primaryMoodType = moodsForDay.get(0).getMoodType();
            emojiText = MoodEmojiManager.getEmojiString(primaryMoodType);
        }
        Label emojiLabel = new Label(emojiText);
        emojiLabel.setStyle("-fx-font-size: 20px;");
        StackPane.setAlignment(emojiLabel, Pos.CENTER);

        Label countLabel = new Label(moodsForDay.isEmpty() ? "" : String.valueOf(moodsForDay.size()));
        countLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: 900; -fx-text-fill: rgba(255,255,255,0.86);");
        StackPane.setAlignment(countLabel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(countLabel, new Insets(0, 7, 5, 0));

        StackPane cell = new StackPane(dayLabel, emojiLabel, countLabel);
        cell.setMinSize(74, 54);
        cell.setPrefSize(74, 54);
        cell.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: " + (date.equals(LocalDate.now()) ? "#163D7A" : "rgba(255,255,255,0.86)") + ";" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: " + (date.equals(LocalDate.now()) ? "2" : "1") + ";" +
                        "-fx-cursor: hand;"
        );
        
        // Make calendar interactive - double-click to edit moods for that day
        final LocalDate cellDate = date;
        cell.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !moodsForDay.isEmpty()) {
                // Double-click: open mood editor for first mood of the day
                editMoodFromHeatmap(moodsForDay.get(0));
            }
        });
        
        Tooltip.install(cell, new Tooltip(heatmapTooltip(date, moodsForDay, score)));
        return cell;
    }
    
    private void editMoodFromHeatmap(Mood mood) {
        moodTypeField.setValue(mood.getMoodType());
        moodStressLevelField.setValue(mood.getStressLevel());
        moodEnergyLevelField.setValue(mood.getEnergyLevel());
        moodDatePicker.setValue(mood.getMoodDate());
        moodNoteArea.setText(mood.getNote() == null ? "" : mood.getNote());
        moodTable.getSelectionModel().select(mood);
    }

    private HBox createHeatmapLegend() {
        HBox legend = new HBox(10);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.getChildren().addAll(
                createLegendItem("#8B1E2D", "Bad day"),
                createLegendItem("#D96B5F", "Difficult"),
                createLegendItem("#F2D6A2", "Mixed"),
                createLegendItem("#8CCB93", "Good"),
                createLegendItem("#247A55", "Very good"),
                createLegendItem("#EDF2F7", "No data")
        );
        return legend;
    }

    private HBox createLegendItem(String color, String text) {
        Region swatch = new Region();
        swatch.setMinSize(18, 18);
        swatch.setPrefSize(18, 18);
        swatch.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 6;");
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #6F87A6;");
        HBox item = new HBox(5, swatch, label);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private double moodHeatScore(Mood mood) {
        String type = mood.getMoodType() == null ? "" : mood.getMoodType().toLowerCase(Locale.ROOT);
        double base;
        if (containsMoodTerm(type, "happy", "joy", "great", "excellent", "good", "grateful", "excited", "content", "calm")) {
            base = 0.75;
        } else if (containsMoodTerm(type, "sad", "angry", "depressed", "anxious", "stressed", "bad", "overwhelmed")) {
            base = -0.78;
        } else {
            base = 0.0;
        }

        if (mood.getStressLevel() != null) {
            base -= (mood.getStressLevel() - 5.0) / 8.0;
        }
        if (mood.getEnergyLevel() != null) {
            base += (mood.getEnergyLevel() - 5.0) / 10.0;
        }
        return Math.max(-1.0, Math.min(1.0, base));
    }

    private boolean containsMoodTerm(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String heatmapColor(double score, boolean empty) {
        if (empty) return "#EDF2F7";
        if (score <= -0.70) return "#8B1E2D";
        if (score <= -0.30) return "#D96B5F";
        if (score < 0.30) return "#F2D6A2";
        if (score < 0.70) return "#8CCB93";
        return "#247A55";
    }

    private String heatmapTextColor(double score, boolean empty) {
        if (empty) return "#9AAEC8";
        return Math.abs(score) >= 0.30 ? "#FFFFFF" : "#7A4F14";
    }

    private String heatmapTooltip(LocalDate date, List<Mood> moodsForDay, double score) {
        if (moodsForDay.isEmpty()) {
            return date + "\nNo mood entries";
        }
        String moods = moodsForDay.stream()
                .map(mood -> mood.getMoodType() + " (stress " + formatLevel(mood.getStressLevel()) + ", energy " + formatLevel(mood.getEnergyLevel()) + ")")
                .collect(Collectors.joining("\n"));
        return date + "\nScore: " + String.format(Locale.US, "%.2f", score) + "\n" + moods;
    }

    private CRUDTabTemplate<Journal> buildJournalTab() {
        // Initialize form fields
        journalTitleField = new TextField();
        journalTitleField.setPromptText("journal title");
        journalContentArea = new TextArea();
        journalContentArea.setPromptText("write your entry...");
        journalContentArea.setPrefRowCount(4);
        journalDatePicker = new DatePicker();
        journalDatePicker.setValue(LocalDate.now());
        journalMoodCombo = new ComboBox<>();
        journalMoodCombo.setPromptText("select mood (optional)");
        journalMoodCombo.setItems(journalMoodOptions);
        journalMoodPromptsBox = new VBox(8);
        journalMoodPromptsBox.setStyle(
                "-fx-background-color: #F4FAF6;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #D6EBDD;" +
                        "-fx-border-radius: 16;" +
                        "-fx-padding: 12 14 12 14;" +
                        "-fx-font-size: 13px;"
        );
        Label promptsHeader = new Label("Select a mood to get writing prompts tailored to how you feel.");
        promptsHeader.setStyle("-fx-text-fill: #35506F; -fx-font-weight: bold;");
        journalMoodPromptsBox.getChildren().add(promptsHeader);
        ScrollPane promptsScrollPane = new ScrollPane(journalMoodPromptsBox);
        promptsScrollPane.setFitToWidth(true);
        promptsScrollPane.setMaxHeight(150);
        configureDatePicker(journalDatePicker);
        configureTextLimit(journalTitleField, FormValidator.JOURNAL_TITLE_MAX_LENGTH);
        configureTextLimit(journalContentArea, FormValidator.JOURNAL_CONTENT_MAX_LENGTH);

        // Create form
        FormTemplate journalForm = new FormTemplate()
                .setTitle("Journal entry")
                .addField("Title:", journalTitleField)
                .addField("Content:", journalContentArea)
                .addField("Entry Date:", journalDatePicker)
                .addField("Mood:", journalMoodCombo)
                .addField("Mood-Based Prompts:", promptsScrollPane)
                .setPadding(new Insets(0));

        // Initialize table
        journalTable = new TableView<>();
        journalIdColumn = new TableColumn<>("ID");
        journalIdColumn.setPrefWidth(60);
        journalTitleColumn = new TableColumn<>("Title");
        journalTitleColumn.setPrefWidth(170);
        journalContentColumn = new TableColumn<>("Content");
        journalContentColumn.setPrefWidth(300);
        journalDateColumn = new TableColumn<>("Entry Date");
        journalDateColumn.setPrefWidth(130);
        journalMoodIdColumn = new TableColumn<>("Mood");
        journalMoodIdColumn.setPrefWidth(100);
        journalTable.getColumns().addAll(journalIdColumn, journalTitleColumn, journalContentColumn, journalDateColumn, journalMoodIdColumn);

        // Initialize filter fields
        journalSearchField = new TextField();
        journalSearchField.setPrefWidth(220);
        journalSortCombo = new ComboBox<>();
        journalStatsLabel = new Label("No entries yet");

        // Build tab with template
        CRUDTabTemplate<Journal> journalTab = new CRUDTabTemplate<>("Journal", journalForm, journalTable)
                .setSearchPlaceholder("title ou content...")
                .setSortOptions(List.of("Date desc", "Date asc", "Title A-Z", "Title Z-A", "ID asc", "ID desc"))
                .withActions(
                        this::handleCreateJournal,
                        this::handleUpdateJournal,
                        this::handleDeleteJournal,
                        this::handleClearJournalForm,
                        this::handleRefreshJournals
                )
                .withFilters(
                        this::handleApplyJournalFilters,
                        this::handleResetJournalFilters
                )
                .withSecondaryAction("Hugging Face Analyse", this::handleHuggingFaceJournalAnalysis)
                .withSecondaryAction("Zen Quote", this::handleZenQuote)
                .withSecondaryAction("Translate", this::handleTranslateJournal)
                .withSecondaryAction("Explain Words", this::handleExplainJournalWords);

        // Store references for filter operations
        journalSearchField = journalTab.getSearchField();
        journalSortCombo = journalTab.getSortCombo();
        journalStatsLabel = journalTab.getStatsLabel();

        return journalTab;
    }

    private javafx.scene.control.Tab buildHeatmapTab() {
        javafx.scene.control.Tab tab = new javafx.scene.control.Tab();
        tab.setText("Heatmap");
        tab.setClosable(false);
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        
        VBox content = new VBox(12);
        content.setPadding(new Insets(14));
        content.setStyle("-fx-background-color: transparent;");
        
        moodHeatmapBox = createMoodHeatmapCalendar();
        content.getChildren().add(moodHeatmapBox);
        
        scrollPane.setContent(content);
        tab.setContent(scrollPane);
        
        return tab;
    }

    private void initialize() {
        configureMoodTypeEditor();
        configureMoodTable();
        configureJournalTable();
        loadMoods();
        loadJournals();

        moodTable.getSelectionModel().selectedItemProperty().addListener((obs, oldMood, selectedMood) -> {
            if (selectedMood != null) {
                moodTypeField.setValue(selectedMood.getMoodType());
                moodStressLevelField.setValue(selectedMood.getStressLevel());
                moodEnergyLevelField.setValue(selectedMood.getEnergyLevel());
                moodDatePicker.setValue(selectedMood.getMoodDate());
                moodNoteArea.setText(selectedMood.getNote() == null ? "" : selectedMood.getNote());
            }
        });

        journalTable.getSelectionModel().selectedItemProperty().addListener((obs, oldJournal, selectedJournal) -> {
            if (selectedJournal != null) {
                journalTitleField.setText(selectedJournal.getTitle());
                journalContentArea.setText(selectedJournal.getContent());
                journalDatePicker.setValue(selectedJournal.getEntryDate());
                selectJournalMood(selectedJournal.getMoodId());
            }
        });

        journalMoodCombo.valueProperty().addListener((obs, oldMood, selectedMood) ->
                updateJournalMoodPrompts(selectedMood));
        updateJournalMoodPrompts(journalMoodCombo.getValue());
    }

    private void handleCreateMood() {
        try {
            // Clear previous error styles
            clearFieldErrors(moodTypeField, moodStressLevelField, moodEnergyLevelField, moodDatePicker, moodNoteArea);

            // Validate inputs
            FormValidator.validateMoodForm(
                    moodTypeField.getValue(),
                    moodDatePicker.getValue(),
                    moodNoteArea.getText(),
                    moodStressLevelField.getValue(),
                    moodEnergyLevelField.getValue()
            );

            Mood mood = appService.createMood(
                    moodTypeField.getValue(),
                    moodDatePicker.getValue(),
                    blankToNull(moodNoteArea.getText()),
                    moodStressLevelField.getValue(),
                    moodEnergyLevelField.getValue(),
                    currentUserId
            );
            handleClearMoodForm();
            loadMoods();
            showSuccess("Mood Created", "Mood successfully added!");
        } catch (ValidationException e) {
            showValidationError("Validation Error", e.getMessage());
            applyErrorStyle(moodTypeField, moodStressLevelField, moodEnergyLevelField, moodDatePicker, moodNoteArea);
        } catch (ServiceException e) {
            showError("Create Mood Failed", e.getMessage());
        } catch (Exception e) {
            showError("Create Mood Failed", e.getMessage());
        }
    }

    private void handleUpdateMood() {
        Mood selected = moodTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Update Mood", "Select a mood from the table first.");
            return;
        }
        try {
            // Clear previous error styles
            clearFieldErrors(moodTypeField, moodStressLevelField, moodEnergyLevelField, moodDatePicker, moodNoteArea);

            // Validate inputs
            FormValidator.validateMoodForm(
                    moodTypeField.getValue(),
                    moodDatePicker.getValue(),
                    moodNoteArea.getText(),
                    moodStressLevelField.getValue(),
                    moodEnergyLevelField.getValue()
            );

            appService.updateMood(
                    selected.getId(),
                    moodTypeField.getValue(),
                    moodDatePicker.getValue(),
                    blankToNull(moodNoteArea.getText()),
                    moodStressLevelField.getValue(),
                    moodEnergyLevelField.getValue()
            );
            handleClearMoodForm();
            loadMoods();
            showSuccess("Mood Updated", "Mood successfully updated!");
        } catch (ValidationException e) {
            showValidationError("Validation Error", e.getMessage());
            applyErrorStyle(moodTypeField, moodStressLevelField, moodEnergyLevelField, moodDatePicker, moodNoteArea);
        } catch (ServiceException e) {
            showError("Update Mood Failed", e.getMessage());
        } catch (Exception e) {
            showError("Update Mood Failed", e.getMessage());
        }
    }

    private void handleDeleteMood() {
        Mood selected = moodTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Delete Mood", "Select a mood from the table first.");
            return;
        }
        try {
            appService.deleteMood(selected.getId());
            handleClearMoodForm();
            loadMoods();
            loadJournals();
        } catch (ServiceException e) {
            showError("Delete Mood Failed", e.getMessage());
        } catch (Exception e) {
            showError("Delete Mood Failed", e.getMessage());
        }
    }

    private void handleRefreshMoods() {
        loadMoods();
    }

    private void handleApplyMoodFilters() {
        applyMoodFilters();
    }

    private void handleResetMoodFilters() {
        moodSearchField.clear();
        moodSortCombo.getSelectionModel().select("Date desc");
        applyMoodFilters();
    }

    private void handleClearMoodForm() {
        moodTypeField.setValue(null);
        if (moodTypeField.getEditor() != null) {
            moodTypeField.getEditor().clear();
        }
        moodStressLevelField.setValue(null);
        moodEnergyLevelField.setValue(null);
        moodDatePicker.setValue(LocalDate.now());
        moodNoteArea.clear();
        moodTable.getSelectionModel().clearSelection();
    }

    private void handleCreateJournal() {
        try {
            // Clear previous error styles
            clearFieldErrors(journalTitleField, journalContentArea, journalDatePicker, journalMoodCombo);

            // Validate inputs
            String selectedMoodId = getSelectedMoodIdAsText();
            FormValidator.validateJournalForm(
                    journalTitleField.getText(),
                    journalContentArea.getText(),
                    journalDatePicker.getValue(),
                    selectedMoodId
            );

            Journal journal = appService.createJournal(
                    journalTitleField.getText(),
                    journalContentArea.getText(),
                    journalDatePicker.getValue(),
                    parseMoodId(selectedMoodId)
            );
            handleClearJournalForm();
            loadJournals();
            showSuccess("Journal Created", "Journal entry successfully added!");
        } catch (ValidationException e) {
            showValidationError("Validation Error", e.getMessage());
            applyErrorStyle(journalTitleField, journalContentArea, journalDatePicker, journalMoodCombo);
        } catch (ServiceException e) {
            showError("Create Journal Failed", e.getMessage());
        } catch (Exception e) {
            showError("Create Journal Failed", e.getMessage());
        }
    }

    private void handleUpdateJournal() {
        Journal selected = journalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Update Journal", "Select a journal from the table first.");
            return;
        }
        try {
            // Clear previous error styles
            clearFieldErrors(journalTitleField, journalContentArea, journalDatePicker, journalMoodCombo);

            // Validate inputs
            String selectedMoodId = getSelectedMoodIdAsText();
            FormValidator.validateJournalForm(
                    journalTitleField.getText(),
                    journalContentArea.getText(),
                    journalDatePicker.getValue(),
                    selectedMoodId
            );

            appService.updateJournal(
                    selected.getId(),
                    journalTitleField.getText(),
                    journalContentArea.getText(),
                    journalDatePicker.getValue(),
                    parseMoodId(selectedMoodId)
            );
            handleClearJournalForm();
            loadJournals();
            showSuccess("Journal Updated", "Journal entry successfully updated!");
        } catch (ValidationException e) {
            showValidationError("Validation Error", e.getMessage());
            applyErrorStyle(journalTitleField, journalContentArea, journalDatePicker, journalMoodCombo);
        } catch (ServiceException e) {
            showError("Update Journal Failed", e.getMessage());
        } catch (Exception e) {
            showError("Update Journal Failed", e.getMessage());
        }
    }

    private void handleDeleteJournal() {
        Journal selected = journalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Delete Journal", "Select a journal from the table first.");
            return;
        }
        try {
            appService.deleteJournal(selected.getId());
            handleClearJournalForm();
            loadJournals();
        } catch (ServiceException e) {
            showError("Delete Journal Failed", e.getMessage());
        } catch (Exception e) {
            showError("Delete Journal Failed", e.getMessage());
        }
    }

    private void handleRefreshJournals() {
        loadJournals();
    }

    private void handleHuggingFaceJournalAnalysis() {
        Journal selected = journalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Hugging Face Analyse", "Select a journal entry from the table first.");
            return;
        }
        try {
            HuggingFaceJournalAnalysisService.JournalAiAnalysis analysis = appService.analyzeJournalWithHuggingFace(selected);
            showHuggingFaceJournalAnalysis(selected, analysis);
        } catch (ServiceException e) {
            showError("Hugging Face Analyse Failed", e.getMessage());
        } catch (Exception e) {
            showError("Hugging Face Analyse Failed", e.getMessage());
        }
    }

    private void handlePreventiveWellnessAssistant() {
        try {
            PreventiveWellnessAssistantService.WellnessReport report = appService.generatePreventiveWellnessReport();
            showPreventiveWellnessReport(report);
        } catch (ServiceException e) {
            showError("Preventive Wellness Assistant Failed", e.getMessage());
        } catch (Exception e) {
            showError("Preventive Wellness Assistant Failed", e.getMessage());
        }
    }

    private void handleEmergencyMode() {
        Label statusLabel = showEmergencyModeScreen();
        try {
            appService.createMood(
                    "Stressed",
                    LocalDate.now(),
                    "Emergency Mode used: user reported feeling overwhelmed.",
                    10,
                    null,
                    currentUserId
            );
            loadMoods();
            statusLabel.setText("High-stress event logged. If email API is configured, admin has been alerted.");
        } catch (ServiceException e) {
            statusLabel.setText("Calming screen opened, but the event could not be logged: " + e.getMessage());
        } catch (Exception e) {
            statusLabel.setText("Calming screen opened, but the event could not be logged.");
        }
    }

    private void handleZenQuote() {
        Journal selected = journalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Zen Quote", "Select a journal entry from the table first.");
            return;
        }
        try {
            ZenQuotesService.Quote quote = appService.getZenQuoteForJournal(selected);
            showZenQuote(selected, quote);
        } catch (ServiceException e) {
            showError("Zen Quote Failed", e.getMessage());
        } catch (Exception e) {
            showError("Zen Quote Failed", e.getMessage());
        }
    }

    private void handleTranslateJournal() {
        Journal selected = journalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Translate Journal", "Select a journal entry from the table first.");
            return;
        }
        try {
            showJournalTranslations(selected, appService.translateJournal(selected));
        } catch (ServiceException e) {
            showError("Translate Journal Failed", e.getMessage());
        }
    }

    private void handleExplainJournalWords() {
        Journal selected = journalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Explain Words", "Select a journal entry from the table first.");
            return;
        }
        try {
            showJournalWordDefinitions(selected, appService.explainJournalEmotionWords(selected));
        } catch (ServiceException e) {
            showError("Explain Words Failed", e.getMessage());
        }
    }

    private void handleExportPdf() {
        try {
            appService.savePdfExportToFile(null);
            showSuccess("PDF Export", "PDF backup generated successfully in the project folder.");
        } catch (ServiceException e) {
            showError("PDF Export Failed", e.getMessage());
        }
    }

    private void handleApplyJournalFilters() {
        applyJournalFilters();
    }

    private void handleResetJournalFilters() {
        journalSearchField.clear();
        journalSortCombo.getSelectionModel().select("Date desc");
        applyJournalFilters();
    }

    private void handleClearJournalForm() {
        journalTitleField.clear();
        journalContentArea.clear();
        journalDatePicker.setValue(LocalDate.now());
        selectJournalMood(null);
        updateJournalMoodPrompts(journalMoodCombo.getValue());
        journalTable.getSelectionModel().clearSelection();
    }

    private void configureMoodTable() {
        moodTypeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                MoodEmojiManager.getEmojiString(data.getValue().getMoodType()) + " " + data.getValue().getMoodType()
        ));
        moodDateColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getMoodDate())));
        moodStressColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatLevel(data.getValue().getStressLevel())));
        moodEnergyColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatLevel(data.getValue().getEnergyLevel())));
        moodNoteColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(valueOrEmpty(data.getValue().getNote())));
        moodTable.setItems(moodItems);
    }

    private void configureJournalTable() {
        journalIdColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        journalTitleColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getTitle()));
        journalContentColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getContent()));
        journalDateColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getEntryDate())));
        journalMoodIdColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(resolveMoodLabel(data.getValue().getMoodId())));
        journalTable.setItems(journalItems);
    }

    private void loadMoods() {
        try {
            List<Mood> moods = appService.getAllMoods();
            allMoods.setAll(moods);
            refreshJournalMoodOptions();
            applyMoodFilters();
            refreshMoodCharts();
            if (journalTable != null) {
                journalTable.refresh();
            }
        } catch (ServiceException e) {
            showError("Load Moods Failed", e.getMessage());
        }
    }

    private void refreshMoodCharts() {
        if (moodPieChart != null && moodBarChart != null) {
            try {
                javafx.scene.chart.PieChart updatedPie = createMoodPieChart();
                javafx.scene.chart.BarChart<String, Number> updatedBar = createMoodBarChart();
                
                moodPieChart.getData().setAll(updatedPie.getData());
                moodBarChart.getData().setAll(updatedBar.getData());
            } catch (Exception e) {
                System.out.println("Error refreshing charts: " + e.getMessage());
            }
        }
        if (moodHeatmapBox != null) {
            moodHeatmapBox.getChildren().setAll(createMoodHeatmapCalendar().getChildren());
        }
    }

    private void loadJournals() {
        try {
            List<Journal> journals = appService.getAllJournals();
            allJournals.setAll(journals);
            applyJournalFilters();
        } catch (ServiceException e) {
            showError("Load Journals Failed", e.getMessage());
        }
    }

    private void applyMoodFilters() {
        String search = safeLower(moodSearchField.getText());
        Predicate<Mood> searchPredicate = mood -> search.isBlank()
                || safeLower(mood.getMoodType()).contains(search)
                || safeLower(mood.getNote()).contains(search);

        String moodSort = moodSortCombo.getValue() == null ? "Date desc" : moodSortCombo.getValue();
        Comparator<Mood> comparator = switch (moodSort) {
            case "Date asc" -> Comparator.comparing(Mood::getMoodDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "Type A-Z" -> Comparator.comparing(m -> safeLower(m.getMoodType()));
            case "Type Z-A" -> Comparator.comparing((Mood m) -> safeLower(m.getMoodType())).reversed();
            default ->
                    Comparator.comparing(Mood::getMoodDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        };

        List<Mood> filteredSorted = allMoods.stream()
                .filter(searchPredicate)
                .sorted(comparator)
                .toList();
        moodItems.setAll(filteredSorted);
        updateMoodStats(filteredSorted);
    }

    private void applyJournalFilters() {
        String search = safeLower(journalSearchField.getText());
        Predicate<Journal> searchPredicate = journal -> search.isBlank()
                || safeLower(journal.getTitle()).contains(search)
                || safeLower(journal.getContent()).contains(search);

        String journalSort = journalSortCombo.getValue() == null ? "Date desc" : journalSortCombo.getValue();
        Comparator<Journal> comparator = switch (journalSort) {
            case "Date asc" -> Comparator.comparing(Journal::getEntryDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "Title A-Z" -> Comparator.comparing(j -> safeLower(j.getTitle()));
            case "Title Z-A" -> Comparator.comparing((Journal j) -> safeLower(j.getTitle())).reversed();
            case "ID asc" -> Comparator.comparingInt(Journal::getId);
            case "ID desc" -> Comparator.comparingInt(Journal::getId).reversed();
            default ->
                    Comparator.comparing(Journal::getEntryDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        };

        List<Journal> filteredSorted = allJournals.stream()
                .filter(searchPredicate)
                .sorted(comparator)
                .toList();
        journalItems.setAll(filteredSorted);
        updateJournalStats(filteredSorted);
    }

    private void updateMoodStats(List<Mood> source) {
        int total = source.size();
        if (total == 0) {
            formatStatsLabel(moodStatsLabel, "0 entries", "", "");
            return;
        }
        Map<String, Long> byType = source.stream()
                .collect(Collectors.groupingBy(m -> safeLower(m.getMoodType()), Collectors.counting()));

        Map.Entry<String, Long> top = byType.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        String topType = top == null ? "-" : capitalize(top.getKey());
        long topCount = top == null ? 0 : top.getValue();
        int uniqueTypes = byType.size();
        String avgStress = formatAverageLevel(source.stream()
                .map(Mood::getStressLevel)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(Double.NaN));
        String avgEnergy = formatAverageLevel(source.stream()
                .map(Mood::getEnergyLevel)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(Double.NaN));
        String stats = String.format("%d total | %d types | Top: %s (%d) | Avg stress: %s | Avg energy: %s",
                total, uniqueTypes, topType, topCount, avgStress, avgEnergy);
        formatStatsLabel(moodStatsLabel, stats, "#1C4F96", "-fx-font-weight: 700;");
    }

    private void updateJournalStats(List<Journal> source) {
        int total = source.size();
        if (total == 0) {
            formatStatsLabel(journalStatsLabel, "0 entries", "", "");
            return;
        }
        long linkedMood = source.stream().filter(j -> j.getMoodId() != null).count();
        long thisMonth = source.stream()
                .filter(j -> j.getEntryDate() != null
                        && j.getEntryDate().getYear() == LocalDate.now().getYear()
                        && j.getEntryDate().getMonth() == LocalDate.now().getMonth())
                .count();
        
        String stats = String.format("%d total | %d linked to mood | %d this month", total, linkedMood, thisMonth);
        formatStatsLabel(journalStatsLabel, stats, "#1C4F96", "-fx-font-weight: 700;");
    }

    private void formatStatsLabel(Label label, String text, String color, String style) {
        label.setText(text);
        String baseStyle = "-fx-font-size: 12px; -fx-padding: 8 14; -fx-background-color: #F9FBFF; -fx-background-radius: 999; -fx-border-color: #CFE3FF; -fx-border-radius: 999; -fx-border-width: 1;";
        if (!color.isEmpty()) {
            baseStyle += " -fx-text-fill: " + color + ";";
        } else {
            baseStyle += " -fx-text-fill: #6F87A6;";
        }
        if (!style.isEmpty()) {
            baseStyle += " " + style;
        }
        label.setStyle(baseStyle);
    }

    public void setScene(javafx.scene.Scene scene) {
        this.appScene = scene;
    }

    public void switchToLightTheme() {
        if (appScene != null) {
            ThemeManager.setTheme(ThemeManager.ThemeType.LIGHT, appScene);
        }
    }

    public void switchToDarkTheme() {
        if (appScene != null) {
            ThemeManager.setTheme(ThemeManager.ThemeType.DARK, appScene);
        }
    }

    public void switchToCustomTheme() {
        if (appScene != null) {
            ThemeManager.setTheme(ThemeManager.ThemeType.CUSTOM, appScene);
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private void refreshJournalMoodOptions() {
        if (journalMoodCombo == null) {
            return;
        }
        Integer selectedMoodId = getSelectedMoodId();
        List<MoodOption> options = new ArrayList<>();
        options.add(MoodOption.none());
        allMoods.stream()
                .sorted(Comparator.comparing(Mood::getMoodDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .forEach(mood -> options.add(new MoodOption(
                        mood.getId(),
                        capitalize(mood.getMoodType()) + " (" + mood.getMoodDate() + ")"
                )));
        journalMoodOptions.setAll(options);
        selectJournalMood(selectedMoodId);
    }

    private void selectJournalMood(Integer moodId) {
        if (journalMoodCombo == null) {
            return;
        }
        if (moodId == null) {
            MoodOption empty = journalMoodOptions.isEmpty() ? MoodOption.none() : journalMoodOptions.get(0);
            journalMoodCombo.setValue(empty);
            return;
        }
        MoodOption option = journalMoodOptions.stream()
                .filter(item -> item.id() != null && item.id().equals(moodId))
                .findFirst().orElseGet(() -> journalMoodOptions.isEmpty() ? MoodOption.none() : journalMoodOptions.get(0));
        journalMoodCombo.setValue(option);
    }

    private Integer getSelectedMoodId() {
        MoodOption selected = journalMoodCombo == null ? null : journalMoodCombo.getValue();
        return selected == null ? null : selected.id();
    }

    private String getSelectedMoodIdAsText() {
        Integer moodId = getSelectedMoodId();
        return moodId == null ? "" : String.valueOf(moodId);
    }

    private String resolveMoodLabel(Integer moodId) {
        if (moodId == null) {
            return "";
        }
        return allMoods.stream()
                .filter(mood -> mood.getId() == moodId)
                .map(mood -> capitalize(mood.getMoodType()))
                .findFirst()
                .orElse("Inconnu");
    }

    private void updateJournalMoodPrompts(MoodOption selectedMood) {
        if (journalMoodPromptsBox == null || journalContentArea == null) {
            return;
        }

        String moodType = resolveMoodType(selectedMood == null ? null : selectedMood.id());
        List<String> prompts = promptsForMood(moodType);
        
        // Clear previous prompts
        journalMoodPromptsBox.getChildren().clear();
        
        // Add header
        Label promptsHeader = new Label("Select a mood to get writing prompts tailored to how you feel.");
        promptsHeader.setStyle("-fx-text-fill: #35506F; -fx-font-weight: bold;");
        journalMoodPromptsBox.getChildren().add(promptsHeader);
        
        if (prompts.isEmpty()) {
            journalContentArea.setPromptText("write your entry...");
            return;
        }

        String moodLabel = moodType == null || moodType.isBlank() ? "this mood" : capitalize(moodType);
        Label moodPromptTitle = new Label("Prompts for " + moodLabel + ":");
        moodPromptTitle.setStyle("-fx-text-fill: #35506F; -fx-font-weight: bold; -fx-font-size: 12px;");
        journalMoodPromptsBox.getChildren().add(moodPromptTitle);
        
        // Create clickable buttons for each prompt
        for (String prompt : prompts) {
            Button promptButton = new Button(prompt);
            promptButton.setStyle(
                    "-fx-background-color: #E8F5ED;" +
                    "-fx-text-fill: #1B5E20;" +
                    "-fx-border-color: #4CAF50;" +
                    "-fx-border-radius: 8;" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 8 12 8 12;" +
                    "-fx-font-size: 12px;" +
                    "-fx-cursor: hand;"
            );
            promptButton.setWrapText(true);
            promptButton.setMaxWidth(Double.MAX_VALUE);
            promptButton.setOnAction(e -> insertPromptToJournal(prompt));
            journalMoodPromptsBox.getChildren().add(promptButton);
        }
    }
    
    private void insertPromptToJournal(String prompt) {
        String currentText = journalContentArea.getText();
        if (currentText == null || currentText.isBlank()) {
            journalContentArea.setText(prompt + "\n\n");
        } else {
            journalContentArea.appendText("\n" + prompt + "\n\n");
        }
        journalContentArea.positionCaret(journalContentArea.getLength());
    }

    private String resolveMoodType(Integer moodId) {
        if (moodId == null) {
            return "";
        }
        return allMoods.stream()
                .filter(mood -> mood.getId() == moodId)
                .map(Mood::getMoodType)
                .findFirst()
                .orElse("");
    }

    private List<String> promptsForMood(String moodType) {
        String mood = moodType == null ? "" : moodType.toLowerCase(Locale.ROOT);
        if (mood.contains("sad") || mood.contains("depress")) {
            return List.of(
                    "What hurt you today?",
                    "What comfort do you need right now?",
                    "What is one gentle thing you can do for yourself tonight?"
            );
        }
        if (mood.contains("happy") || mood.contains("excited") || mood.contains("content") || mood.contains("grateful")) {
            return List.of(
                    "What made today special?",
                    "How can you repeat this feeling?",
                    "Who or what helped create this good moment?"
            );
        }
        if (mood.contains("stress") || mood.contains("anxious") || mood.contains("anxiety")) {
            return List.of(
                    "What is causing the most pressure?",
                    "What can wait until tomorrow?",
                    "What is one small next step you can take now?"
            );
        }
        if (mood.contains("angry") || mood.contains("anger")) {
            return List.of(
                    "What boundary feels crossed?",
                    "What do you wish the other person understood?",
                    "What response would protect you without making things worse?"
            );
        }
        if (mood.contains("calm")) {
            return List.of(
                    "What helped you feel calm today?",
                    "How can you protect this calm feeling?",
                    "What would you like to remember from this moment?"
            );
        }
        return List.of(
                "What is the strongest feeling in your body right now?",
                "What triggered this mood today?",
                "What do you need next?"
        );
    }

    private Integer parseMoodId(String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Mood ID must be a number or empty.");
        }
    }

    private String required(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private LocalDate requiredDate(LocalDate date, String message) {
        if (date == null) {
            throw new IllegalArgumentException(message);
        }
        return date;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private ComboBox<Integer> createLevelComboBox(String promptText) {
        ComboBox<Integer> comboBox = new ComboBox<>();
        comboBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        comboBox.setPromptText(promptText);
        comboBox.setPrefWidth(300);
        return comboBox;
    }

    private String formatLevel(Integer level) {
        return level == null ? "" : String.valueOf(level);
    }

    private String formatAverageLevel(double value) {
        return Double.isNaN(value) ? "n/a" : String.format(Locale.US, "%.1f", value);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showValidationError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText("Please correct the following errors:");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showPreventiveWellnessReport(PreventiveWellnessAssistantService.WellnessReport report) {
        Stage stage = new Stage();
        stage.setTitle("Preventive Wellness Assistant");
        stage.initOwner(getStageFromTab(moodTable));

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #F5F8FC;");

        VBox page = new VBox(22);
        page.setPadding(new Insets(28, 32, 32, 32));
        page.getChildren().addAll(
                createWellnessHero(report, stage),
                createWellnessMetrics(report),
                createWellnessSection("Early warning signs", report.warningSigns(), "Signals detected from recent mood, stress, energy, and journal patterns."),
                createWellnessSection("Preventive recommendations", report.recommendations(), "Practical actions to reduce overload before it becomes more serious.")
        );

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        root.setCenter(scrollPane);

        Scene scene = new Scene(root, 1040, 760);
        scene.getStylesheets().add(ThemeStyle.getCssDataUri());
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(640);
        stage.show();
    }

    private void showHuggingFaceJournalAnalysis(Journal journal, HuggingFaceJournalAnalysisService.JournalAiAnalysis analysis) {
        Stage stage = new Stage();
        stage.setTitle("Hugging Face Journal Analysis");
        stage.initOwner(getStageFromTab(journalTable));

        BorderPane root = new BorderPane();
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #F6FAFF, #EEF5F8 58%, #F7FBF8 100%);"
        );

        VBox page = new VBox(24);
        page.setPadding(new Insets(28, 32, 32, 32));
        page.getChildren().addAll(
                createHuggingFaceHero(journal, analysis, stage),
                createHuggingFaceMetrics(analysis),
                createHuggingFaceTextCard("Summary", analysis.summary(), "Short reading of the journal content."),
                createHuggingFaceTextCard("Interpretation", analysis.interpretation(), "What the detected sentiment and emotion may suggest."),
                createHuggingFaceRecommendations(analysis.recommendations())
        );

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        root.setCenter(scrollPane);
        root.setBottom(createHuggingFaceFooter(analysis));

        Scene scene = new Scene(root, 1040, 760);
        scene.getStylesheets().add(ThemeStyle.getCssDataUri());
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(640);
        stage.show();
    }

    private void showZenQuote(Journal journal, ZenQuotesService.Quote quote) {
        Stage stage = new Stage();
        stage.setTitle("Zen Quote");
        stage.initOwner(getStageFromTab(journalTable));

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #F6FAFF, #EEF7F1);");

        VBox card = new VBox(18);
        card.setPadding(new Insets(34));
        card.setMaxWidth(720);
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-background-radius: 30;" +
                        "-fx-border-color: #D6EBDD;" +
                        "-fx-border-radius: 30;" +
                        "-fx-border-width: 1.2;" +
                        "-fx-effect: dropshadow(gaussian, rgba(25,45,75,0.14), 28, 0.18, 0, 10);"
        );

        Label eyebrow = new Label("ZenQuotes API - matched to selected journal");
        eyebrow.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: #287455;");

        Label contextLabel = new Label("Based on: " + safeQuoteText(journal == null ? null : journal.getTitle()));
        contextLabel.setWrapText(true);
        contextLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #627894;");

        TextArea quoteLabel = new TextArea("\"" + safeQuoteText(quote == null ? null : quote.quote()) + "\"");
        quoteLabel.setEditable(false);
        quoteLabel.setFocusTraversable(false);
        quoteLabel.setWrapText(true);
        quoteLabel.setPrefRowCount(4);
        quoteLabel.setMinHeight(142);
        quoteLabel.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-control-inner-background: #FFFFFF;" +
                        "-fx-background-insets: 0;" +
                        "-fx-border-color: transparent;" +
                        "-fx-text-fill: #143A63;" +
                        "-fx-font-size: 26px;" +
                        "-fx-font-weight: 900;"
        );

        Label authorLabel = new Label("- " + safeQuoteText(quote == null ? null : quote.author()));
        authorLabel.setWrapText(true);
        authorLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #536A86;");

        Label attribution = new Label("Inspirational quotes provided by ZenQuotes API. Source: "
                + safeQuoteText(quote == null ? null : quote.source()));
        attribution.setWrapText(true);
        attribution.setStyle(
                "-fx-background-color: #F4FAF6;" +
                        "-fx-background-radius: 16;" +
                        "-fx-padding: 11 14 11 14;" +
                        "-fx-font-size: 12px;" +
                        "-fx-text-fill: #627894;"
        );

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button anotherButton = new Button("Another related quote");
        anotherButton.setOnAction(event -> {
            try {
                ZenQuotesService.Quote nextQuote = appService.getZenQuoteForJournal(journal);
                quoteLabel.setText("\"" + safeQuoteText(nextQuote.quote()) + "\"");
                quoteLabel.positionCaret(0);
                authorLabel.setText("- " + safeQuoteText(nextQuote.author()));
                attribution.setText("Inspirational quotes provided by ZenQuotes API. Source: "
                        + safeQuoteText(nextQuote.source()));
            } catch (ServiceException e) {
                showError("Zen Quote Failed", e.getMessage());
            }
        });
        Button closeButton = createCloseButton(stage);
        actions.getChildren().addAll(anotherButton, closeButton);

        card.getChildren().addAll(eyebrow, contextLabel, quoteLabel, authorLabel, attribution, actions);
        root.setCenter(card);
        BorderPane.setMargin(card, new Insets(30));

        Scene scene = new Scene(root, 860, 540);
        scene.getStylesheets().add(ThemeStyle.getCssDataUri());
        stage.setScene(scene);
        stage.setMinWidth(720);
        stage.setMinHeight(460);
        stage.show();
    }

    private String safeQuoteText(String value) {
        return value == null || value.isBlank() ? "No quote available" : value.trim();
    }

    private void showJournalTranslations(Journal journal, List<LibreTranslateService.TranslationResult> translations) {
        Stage stage = new Stage();
        stage.setTitle("Journal Translation");
        stage.initOwner(getStageFromTab(journalTable));

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #F6FAFF, #F1F7FF);");

        VBox page = new VBox(16);
        page.setPadding(new Insets(28));
        Label title = new Label("LibreTranslate API");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: #1C4F96;");
        Label context = new Label("Journal: " + safeQuoteText(journal == null ? null : journal.getTitle()));
        context.setWrapText(true);
        context.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #627894;");
        page.getChildren().addAll(title, context);

        for (LibreTranslateService.TranslationResult translation : translations) {
            page.getChildren().add(createResultTextCard(
                    translation.language() + " (" + translation.languageCode() + ")",
                    translation.text(),
                    translation.source()
            ));
        }

        HBox actions = new HBox(createCloseButton(stage));
        actions.setAlignment(Pos.CENTER_RIGHT);
        page.getChildren().add(actions);

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        root.setCenter(scrollPane);

        Scene scene = new Scene(root, 900, 650);
        scene.getStylesheets().add(ThemeStyle.getCssDataUri());
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(520);
        stage.show();
    }

    private void showJournalWordDefinitions(Journal journal, List<WiktionaryEmotionService.EmotionWordDefinition> definitions) {
        Stage stage = new Stage();
        stage.setTitle("Emotion Word Dictionary");
        stage.initOwner(getStageFromTab(journalTable));

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #F7FBF8, #F1F7FF);");

        VBox page = new VBox(16);
        page.setPadding(new Insets(28));
        Label title = new Label("Wiktionary emotion words");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: #1C4F96;");
        Label context = new Label("Detected from: " + safeQuoteText(journal == null ? null : journal.getTitle()));
        context.setWrapText(true);
        context.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #627894;");
        page.getChildren().addAll(title, context);

        for (WiktionaryEmotionService.EmotionWordDefinition definition : definitions) {
            String text = safeQuoteText(definition.definition()) + "\n\nAdvice: " + safeQuoteText(definition.advice());
            page.getChildren().add(createResultTextCard(definition.word(), text, definition.source()));
        }

        HBox actions = new HBox(createCloseButton(stage));
        actions.setAlignment(Pos.CENTER_RIGHT);
        page.getChildren().add(actions);

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        root.setCenter(scrollPane);

        Scene scene = new Scene(root, 900, 650);
        scene.getStylesheets().add(ThemeStyle.getCssDataUri());
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(520);
        stage.show();
    }

    private VBox createResultTextCard(String title, String body, String source) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: #DDE7F3;" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1.1;"
        );
        Label titleLabel = new Label(safeQuoteText(title));
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #1C4F96;");

        TextArea bodyArea = new TextArea(body == null ? "" : body);
        bodyArea.setEditable(false);
        bodyArea.setWrapText(true);
        bodyArea.setPrefRowCount(5);
        bodyArea.setStyle("-fx-control-inner-background: #F8FBFF; -fx-font-size: 13px; -fx-text-fill: #213B5B;");

        Label sourceLabel = new Label("Source: " + safeQuoteText(source));
        sourceLabel.setWrapText(true);
        sourceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6F87A6;");
        card.getChildren().addAll(titleLabel, bodyArea, sourceLabel);
        return card;
    }

    private Label showEmergencyModeScreen() {
        Stage stage = new Stage();
        stage.setTitle("Emergency Mode");
        stage.initOwner(getStageFromTab(moodTable));

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #EAF7F1, #F7FBFF 56%, #FDF8EF);");

        Circle bubbleOne = createDriftingBubble(145, "rgba(58,143,109,0.14)", -360, -210, 54, 28, 13);
        Circle bubbleTwo = createDriftingBubble(105, "rgba(36,94,189,0.11)", 350, -130, -42, 38, 16);
        Circle bubbleThree = createDriftingBubble(78, "rgba(232,184,109,0.16)", -250, 230, 34, -30, 18);
        Circle bubbleFour = createDriftingBubble(54, "rgba(58,143,109,0.13)", 390, 220, -26, -34, 14);

        VBox card = new VBox(22);
        card.setPadding(new Insets(38));
        card.setMaxWidth(760);
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-background-radius: 34;" +
                        "-fx-border-color: #D6EBDD;" +
                        "-fx-border-radius: 34;" +
                        "-fx-border-width: 1.2;" +
                        "-fx-effect: dropshadow(gaussian, rgba(25,45,75,0.12), 32, 0.18, 0, 10);"
        );

        Label eyebrow = new Label("Emergency Mode");
        eyebrow.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: #287455;");

        Label title = new Label("You are not alone. Let's make the screen simpler.");
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 31px; -fx-font-weight: 900; -fx-text-fill: #143A63; -fx-line-spacing: 4;");

        Label body = new Label("For the next minute, the only task is to breathe and reduce pressure. No forms, no tables, no decisions.");
        body.setWrapText(true);
        body.setStyle("-fx-font-size: 15px; -fx-text-fill: #536A86; -fx-line-spacing: 5;");

        StackPane breathingOrb = createBreathingOrb();
        Label breathingLabel = new Label("Breathe in");
        breathingLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: 900; -fx-text-fill: #287455;");
        Label breathingSubLabel = new Label("Let the circle guide your pace.");
        breathingSubLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #6F87A6;");
        VBox breathingCopy = new VBox(4, breathingLabel, breathingSubLabel);
        breathingCopy.setAlignment(Pos.CENTER_LEFT);

        HBox breathingRow = new HBox(18, breathingOrb, breathingCopy);
        breathingRow.setAlignment(Pos.CENTER_LEFT);
        breathingRow.setStyle(
                "-fx-background-color: linear-gradient(to right, #F4FAF6, #F9FBFF);" +
                        "-fx-background-radius: 24;" +
                        "-fx-border-color: #D6EBDD;" +
                        "-fx-border-radius: 24;" +
                        "-fx-padding: 16 18 16 18;"
        );
        startBreathingTextCycle(breathingLabel, breathingSubLabel);

        VBox steps = new VBox(12);
        steps.getChildren().addAll(
                createEmergencyStep("1", "Breathe in slowly for 4 seconds."),
                createEmergencyStep("2", "Hold gently for 2 seconds."),
                createEmergencyStep("3", "Breathe out for 6 seconds."),
                createEmergencyStep("4", "Name 3 things you can see around you.")
        );

        Label status = new Label("Logging high-stress event...");
        status.setWrapText(true);
        status.setStyle(
                "-fx-background-color: #F4FAF6;" +
                        "-fx-background-radius: 16;" +
                        "-fx-padding: 12 14 12 14;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: 800;" +
                        "-fx-text-fill: #287455;"
        );

        Label safetyNote = new Label("If you might hurt yourself or someone else, contact emergency services or a trusted person now.");
        safetyNote.setWrapText(true);
        safetyNote.setStyle(
                "-fx-background-color: #FFF4F4;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #FFD5D8;" +
                        "-fx-border-radius: 16;" +
                        "-fx-padding: 12 14 12 14;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: 800;" +
                        "-fx-text-fill: #A92F38;"
        );

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button closeButton = new Button("I feel a little safer");
        closeButton.getStyleClass().add("success");
        closeButton.setOnAction(event -> stage.close());
        actions.getChildren().add(closeButton);

        card.getChildren().addAll(eyebrow, title, body, breathingRow, steps, status, safetyNote, actions);
        root.getChildren().addAll(bubbleOne, bubbleTwo, bubbleThree, bubbleFour, card);
        StackPane.setMargin(card, new Insets(34));

        Scene scene = new Scene(root, 900, 660);
        scene.getStylesheets().add(ThemeStyle.getCssDataUri());
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(560);
        stage.show();
        return status;
    }

    private HBox createEmergencyStep(String number, String text) {
        Label marker = new Label(number);
        marker.setMinSize(34, 34);
        marker.setAlignment(Pos.CENTER);
        marker.setStyle(
                "-fx-background-color: #EAF7F1;" +
                        "-fx-background-radius: 999;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: 900;" +
                        "-fx-text-fill: #287455;"
        );

        Label copy = new Label(text);
        copy.setWrapText(true);
        copy.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #143A63;");
        HBox.setHgrow(copy, Priority.ALWAYS);

        HBox row = new HBox(12, marker, copy);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
                "-fx-background-color: #F9FBFF;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #DDE7F3;" +
                        "-fx-border-radius: 18;" +
                        "-fx-padding: 12 14 12 14;"
        );
        return row;
    }

    private StackPane createBreathingOrb() {
        Circle outer = new Circle(42);
        outer.setStyle("-fx-fill: rgba(58,143,109,0.16);");

        Circle middle = new Circle(31);
        middle.setStyle("-fx-fill: rgba(58,143,109,0.24);");

        Circle inner = new Circle(19);
        inner.setStyle("-fx-fill: #3A8F6D;");

        StackPane orb = new StackPane(outer, middle, inner);
        orb.setMinSize(96, 96);
        orb.setPrefSize(96, 96);

        ScaleTransition breathe = new ScaleTransition(Duration.seconds(4), orb);
        breathe.setFromX(0.86);
        breathe.setFromY(0.86);
        breathe.setToX(1.18);
        breathe.setToY(1.18);
        breathe.setAutoReverse(true);
        breathe.setCycleCount(Animation.INDEFINITE);
        breathe.play();

        return orb;
    }

    private Circle createDriftingBubble(double radius, String color, double x, double y, double moveX, double moveY, double seconds) {
        Circle bubble = new Circle(radius);
        bubble.setMouseTransparent(true);
        bubble.setStyle("-fx-fill: " + color + ";");
        bubble.setTranslateX(x);
        bubble.setTranslateY(y);

        TranslateTransition drift = new TranslateTransition(Duration.seconds(seconds), bubble);
        drift.setFromX(x);
        drift.setFromY(y);
        drift.setToX(x + moveX);
        drift.setToY(y + moveY);
        drift.setAutoReverse(true);
        drift.setCycleCount(Animation.INDEFINITE);
        drift.play();

        return bubble;
    }

    private void startBreathingTextCycle(Label label, Label subLabel) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, event -> {
                    label.setText("Breathe in");
                    subLabel.setText("Let the circle expand. Count 1, 2, 3, 4.");
                }),
                new KeyFrame(Duration.seconds(4), event -> {
                    label.setText("Hold gently");
                    subLabel.setText("No forcing. Just a small pause.");
                }),
                new KeyFrame(Duration.seconds(6), event -> {
                    label.setText("Breathe out");
                    subLabel.setText("Let your shoulders drop. Count 1 to 6.");
                }),
                new KeyFrame(Duration.seconds(12), event -> {
                    label.setText("Notice the room");
                    subLabel.setText("Name one color, one sound, and one surface.");
                })
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private VBox createHuggingFaceHero(Journal journal, HuggingFaceJournalAnalysisService.JournalAiAnalysis analysis, Stage stage) {
        VBox hero = new VBox(16);
        hero.setPadding(new Insets(30, 34, 30, 34));
        hero.setStyle(
                "-fx-background-color: linear-gradient(to right, #143A63, #1F6D74);" +
                        "-fx-background-radius: 30;" +
                        "-fx-border-color: rgba(255,255,255,0.58);" +
                        "-fx-border-radius: 30;" +
                        "-fx-border-width: 1.2;" +
                        "-fx-effect: dropshadow(gaussian, rgba(17,43,78,0.28), 34, 0.22, 0, 12);"
        );

        HBox top = new HBox(16);
        top.setAlignment(Pos.TOP_LEFT);

        VBox titleBlock = new VBox(8);
        Label eyebrow = new Label("Hugging Face Inference API");
        eyebrow.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: rgba(255,255,255,0.82);");

        Label title = new Label(journal == null ? "Journal analysis" : journal.getTitle());
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 34px; -fx-font-weight: 900; -fx-text-fill: #FFFFFF;");

        Label summary = new Label("AI-assisted reading for sentiment, dominant emotion, summary, and practical follow-up.");
        summary.setWrapText(true);
        summary.setMaxWidth(760);
        summary.setStyle("-fx-font-size: 15px; -fx-text-fill: rgba(255,255,255,0.84); -fx-line-spacing: 5;");
        titleBlock.getChildren().addAll(eyebrow, title, summary);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox actions = new VBox(10);
        actions.setAlignment(Pos.TOP_RIGHT);
        actions.getChildren().addAll(createSourceBadge(analysis.source()), createCloseButton(stage));

        top.getChildren().addAll(titleBlock, spacer, actions);

        HBox insightStrip = new HBox(12);
        insightStrip.setAlignment(Pos.CENTER_LEFT);
        insightStrip.setStyle(
                "-fx-background-color: rgba(255,255,255,0.14);" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: rgba(255,255,255,0.20);" +
                        "-fx-border-radius: 18;" +
                        "-fx-padding: 12 14 12 14;"
        );
        Label insight = new Label("Detected pattern: " + analysis.sentiment() + " tone, with " + analysis.emotion() + " as the strongest emotional signal.");
        insight.setWrapText(true);
        insight.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #FFFFFF;");
        insightStrip.getChildren().add(insight);

        hero.getChildren().addAll(top, insightStrip);
        return hero;
    }

    private FlowPane createHuggingFaceMetrics(HuggingFaceJournalAnalysisService.JournalAiAnalysis analysis) {
        FlowPane metrics = new FlowPane();
        metrics.setHgap(16);
        metrics.setVgap(16);
        metrics.getChildren().addAll(
                createHuggingFaceMetricCard("Sentiment", analysis.sentiment(), "S", "#EAF2FF", "#1C4F96"),
                createHuggingFaceMetricCard("Confidence", formatPercent(analysis.sentimentScore()), "%", "#EDF8F3", "#287455"),
                createHuggingFaceMetricCard("Emotion", analysis.emotion(), "E", "#FFF3E4", "#A85D13"),
                createHuggingFaceMetricCard("Emotion score", formatPercent(analysis.emotionScore()), "%", "#F4EEFF", "#6947A8")
        );
        return metrics;
    }

    private VBox createHuggingFaceMetricCard(String title, String value, String icon, String background, String accent) {
        VBox card = new VBox(12);
        card.setPrefWidth(230);
        card.setMinHeight(126);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #FFFFFF, " + background + ");" +
                        "-fx-background-radius: 24;" +
                        "-fx-border-color: rgba(255,255,255,0.88);" +
                        "-fx-border-radius: 24;" +
                        "-fx-effect: dropshadow(gaussian, rgba(25,45,75,0.13), 22, 0.18, 0, 8);"
        );

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.setMinSize(38, 38);
        iconLabel.setAlignment(Pos.CENTER);
        iconLabel.setStyle(
                "-fx-background-color: rgba(255,255,255,0.88);" +
                        "-fx-background-radius: 14;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: 900;" +
                        "-fx-text-fill: " + accent + ";"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: #627894;");
        top.getChildren().addAll(iconLabel, titleLabel);

        Label valueLabel = new Label(value == null || value.isBlank() ? "Unknown" : value);
        valueLabel.setWrapText(true);
        valueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: " + accent + ";");

        card.getChildren().addAll(top, valueLabel);
        return card;
    }

    private VBox createHuggingFaceTextCard(String title, String text) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(22));
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-background-radius: 22;" +
                        "-fx-border-color: #DDE7F3;" +
                        "-fx-border-radius: 22;" +
                        "-fx-effect: dropshadow(gaussian, rgba(25,45,75,0.10), 20, 0.16, 0, 6);"
        );
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #1C3354;");
        Label body = new Label(text == null || text.isBlank() ? "Aucune donnée disponible." : text);
        body.setWrapText(true);
        body.setStyle("-fx-font-size: 14px; -fx-text-fill: #35506F; -fx-line-spacing: 4;");
        card.getChildren().addAll(titleLabel, body);
        return card;
    }

    private VBox createHuggingFaceTextCard(String title, String text, String subtitle) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(24));
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.92);" +
                        "-fx-background-radius: 26;" +
                        "-fx-border-color: rgba(221,231,243,0.95);" +
                        "-fx-border-radius: 26;" +
                        "-fx-effect: dropshadow(gaussian, rgba(25,45,75,0.11), 24, 0.18, 0, 8);"
        );
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: #112B4E;");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setWrapText(true);
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #6F87A6;");
        Label body = new Label(text == null || text.isBlank() ? "No data available." : text);
        body.setWrapText(true);
        body.setStyle("-fx-font-size: 15px; -fx-text-fill: #35506F; -fx-line-spacing: 5;");
        card.getChildren().addAll(titleLabel, subtitleLabel, new Separator(), body);
        return card;
    }

    private VBox createHuggingFaceRecommendations(List<String> items) {
        VBox card = new VBox(16);
        card.setPadding(new Insets(24));
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #FFFFFF, #F4FAF6);" +
                        "-fx-background-radius: 26;" +
                        "-fx-border-color: #D6EBDD;" +
                        "-fx-border-radius: 26;" +
                        "-fx-effect: dropshadow(gaussian, rgba(25,45,75,0.11), 24, 0.18, 0, 8);"
        );

        Label titleLabel = new Label("Recommendations");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: #112B4E;");

        Label subtitleLabel = new Label("Practical next steps based on the journal text analysis.");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #627894;");

        VBox list = new VBox(12);
        for (String item : items) {
            list.getChildren().add(createHuggingFaceBullet(item));
        }

        card.getChildren().addAll(titleLabel, subtitleLabel, new Separator(), list);
        return card;
    }

    private HBox createHuggingFaceBullet(String text) {
        Label marker = new Label("OK");
        marker.setMinSize(32, 32);
        marker.setAlignment(Pos.CENTER);
        marker.setStyle(
                "-fx-background-color: #EAF7F1;" +
                        "-fx-background-radius: 12;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: 900;" +
                        "-fx-text-fill: #287455;"
        );

        Label copy = new Label(text);
        copy.setWrapText(true);
        copy.setStyle("-fx-font-size: 14px; -fx-text-fill: #35506F; -fx-line-spacing: 4;");
        HBox.setHgrow(copy, Priority.ALWAYS);

        HBox row = new HBox(12, marker, copy);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private HBox createHuggingFaceFooter(HuggingFaceJournalAnalysisService.JournalAiAnalysis analysis) {
        HBox footer = new HBox(10);
        footer.setPadding(new Insets(0, 32, 24, 32));
        footer.setAlignment(Pos.CENTER_LEFT);
        Label info = new Label(analysis.source() == null ? "" : analysis.source());
        info.setWrapText(true);
        info.setStyle("-fx-font-size: 12px; -fx-text-fill: #6F87A6;");
        footer.getChildren().add(info);
        return footer;
    }

    private Label createSourceBadge(String source) {
        boolean fallback = source != null && source.toLowerCase(Locale.ROOT).contains("fallback");
        String color = fallback ? "#C87525" : "#2F8F68";
        String background = fallback ? "#FFF3E4" : "#EAF7F1";
        Label badge = new Label(fallback ? "Mode local" : "HF API");
        badge.setStyle(
                "-fx-background-color: " + background + ";" +
                        "-fx-background-radius: 999;" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-radius: 999;" +
                        "-fx-padding: 9 15 9 15;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: 900;" +
                        "-fx-text-fill: " + color + ";"
        );
        return badge;
    }

    private String formatPercent(double value) {
        return String.format(Locale.US, "%.0f%%", Math.max(0, Math.min(1, value)) * 100);
    }

    private VBox createWellnessHero(PreventiveWellnessAssistantService.WellnessReport report, Stage stage) {
        VBox hero = new VBox(16);
        hero.setPadding(new Insets(28, 32, 28, 32));
        hero.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-background-radius: 24;" +
                        "-fx-border-color: #DDE7F3;" +
                        "-fx-border-radius: 24;" +
                        "-fx-effect: dropshadow(gaussian, rgba(25,45,75,0.12), 24, 0.18, 0, 8);"
        );

        HBox top = new HBox(16);
        top.setAlignment(Pos.TOP_LEFT);

        VBox titleBlock = new VBox(8);
        Label eyebrow = new Label("Preventive Wellness Assistant");
        eyebrow.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #6F87A6;");

        Label title = new Label(report.headline());
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: 900; -fx-text-fill: #1C3354;");

        Label summary = new Label(report.summary());
        summary.setWrapText(true);
        summary.setMaxWidth(760);
        summary.setStyle("-fx-font-size: 14px; -fx-text-fill: #536A86; -fx-line-spacing: 4;");
        titleBlock.getChildren().addAll(eyebrow, title, summary);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox actions = new VBox(10);
        actions.setAlignment(Pos.TOP_RIGHT);
        actions.getChildren().addAll(createStatusBadge(report.status()), createCloseButton(stage));

        top.getChildren().addAll(titleBlock, spacer, actions);
        hero.getChildren().add(top);
        return hero;
    }

    private FlowPane createWellnessMetrics(PreventiveWellnessAssistantService.WellnessReport report) {
        FlowPane metrics = new FlowPane();
        metrics.setHgap(16);
        metrics.setVgap(16);
        metrics.getChildren().addAll(
                createWellnessMetricCard("Mood coverage", report.moodCoverage()),
                createWellnessMetricCard("Journal coverage", report.journalCoverage()),
                createWellnessMetricCard("Average stress", report.averageStress()),
                createWellnessMetricCard("Average energy", report.averageEnergy())
        );
        return metrics;
    }

    private VBox createWellnessMetricCard(String title, String value) {
        VBox card = new VBox(10);
        card.setPrefWidth(230);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #DDE7F3;" +
                        "-fx-border-radius: 18;" +
                        "-fx-effect: dropshadow(gaussian, rgba(25,45,75,0.09), 18, 0.16, 0, 5);"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #6F87A6;");

        Label valueLabel = new Label(value);
        valueLabel.setWrapText(true);
        valueLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #1C4F96;");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private VBox createWellnessSection(String title, List<String> items, String subtitle) {
        VBox card = new VBox(16);
        card.setPadding(new Insets(22));
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-background-radius: 22;" +
                        "-fx-border-color: #DDE7F3;" +
                        "-fx-border-radius: 22;" +
                        "-fx-effect: dropshadow(gaussian, rgba(25,45,75,0.10), 20, 0.16, 0, 6);"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #1C3354;");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setWrapText(true);
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6F87A6;");

        VBox list = new VBox(10);
        for (String item : items) {
            list.getChildren().add(createWellnessBullet(item));
        }

        card.getChildren().addAll(titleLabel, subtitleLabel, new Separator(), list);
        return card;
    }

    private HBox createWellnessBullet(String text) {
        Label marker = new Label("-");
        marker.setMinWidth(16);
        marker.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #1C4F96;");

        Label copy = new Label(text);
        copy.setWrapText(true);
        copy.setStyle("-fx-font-size: 14px; -fx-text-fill: #35506F; -fx-line-spacing: 4;");
        HBox.setHgrow(copy, Priority.ALWAYS);

        HBox row = new HBox(10, marker, copy);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private Label createStatusBadge(String status) {
        String color = switch (status) {
            case "RED" -> "#C63D48";
            case "ORANGE" -> "#C87525";
            default -> "#2F8F68";
        };
        String background = switch (status) {
            case "RED" -> "#FDECEF";
            case "ORANGE" -> "#FFF3E4";
            default -> "#EAF7F1";
        };
        String label = switch (status) {
            case "RED" -> "High risk";
            case "ORANGE" -> "Watch closely";
            default -> "Stable";
        };

        Label badge = new Label(label);
        badge.setStyle(
                "-fx-background-color: " + background + ";" +
                        "-fx-background-radius: 999;" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-radius: 999;" +
                        "-fx-padding: 9 15 9 15;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: 900;" +
                        "-fx-text-fill: " + color + ";"
        );
        return badge;
    }

    private Button createCloseButton(Stage stage) {
        Button close = new Button("Close");
        close.setOnAction(event -> stage.close());
        close.setStyle(
                "-fx-background-color: #F5F8FC;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #DDE7F3;" +
                        "-fx-border-radius: 14;" +
                        "-fx-text-fill: #1C4F96;" +
                        "-fx-font-weight: 800;"
        );
        return close;
    }

    private void applyErrorStyle(Control... controls) {
        String errorStyle = "-fx-border-color: #C63D48; -fx-border-width: 2; -fx-border-radius: 16; -fx-background-radius: 16;";
        for (Control control : controls) {
            if (control != null) {
                control.setStyle(errorStyle);
            }
        }
    }

    private void clearFieldErrors(Control... controls) {
        for (Control control : controls) {
            if (control != null) {
                control.setStyle("");
            }
        }
    }

    private void configureMoodTypeEditor() {
        moodTypeField.setEditable(true);
        if (moodTypeField.getEditor() != null) {
            configureTextLimit(moodTypeField.getEditor(), FormValidator.MOOD_TYPE_MAX_LENGTH);
        }
    }

    private void configureTextLimit(TextInputControl control, int maxLength) {
        control.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= maxLength ? change : null));
    }

    private void configureDatePicker(DatePicker datePicker) {
        datePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty || item == null || item.isAfter(LocalDate.now()));
            }
        });
    }

    private void handleAdminPanel() {
        // Create new stage for admin dashboard
        javafx.stage.Stage adminStage = new javafx.stage.Stage();
        adminStage.setTitle("Admin Dashboard - Mood Alerts");
        adminStage.setWidth(900);
        adminStage.setHeight(700);
        
        // Build and show admin dashboard
        AdminDashboard adminDashboard = new AdminDashboard();
        javafx.scene.Scene scene = new javafx.scene.Scene(adminDashboard.build());
        adminStage.setScene(scene);
        
        // Show as modal dialog
        adminStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
        adminStage.initOwner(this.getStageFromTab(moodTable));
        adminStage.show();
    }

    /**
     * Helper method to get the Stage from a Node.
     */
    private javafx.stage.Stage getStageFromTab(javafx.scene.Node node) {
        javafx.scene.Scene scene = node.getScene();
        if (scene != null) {
            return (javafx.stage.Stage) scene.getWindow();
        }
        return null;
    }

    private record MoodOption(Integer id, String label) {
        private static MoodOption none() {
            return new MoodOption(null, "Aucun mood");
        }

        @Override
        public String toString() {
            return label;
        }
    }

}

