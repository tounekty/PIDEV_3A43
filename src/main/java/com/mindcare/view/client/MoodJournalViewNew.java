package com.mindcare.view.client;

import com.mindcare.utils.NavigationManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.example.db.SchemaInitializer;
import org.example.model.Mood;
import org.example.model.Journal;
import org.example.service.MoodService;
import org.example.service.JournalService;
import org.example.service.MoodJournalPdfService;
import org.example.service.JournalAiAnalysisService;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MoodJournalViewNew implements NavigationManager.Buildable {

    private final MoodService moodService = new MoodService();
    private final JournalService journalService = new JournalService();
    private final ObservableList<Mood> moods = FXCollections.observableArrayList();
    private final ObservableList<Journal> journals = FXCollections.observableArrayList();

    // Tab pane
    private TabPane tabPane;
    
    // Mood Tab Components
    private TableView<Mood> moodTable;
    private ComboBox<String> moodTypeCombo;
    private DatePicker moodDatePicker;
    private Slider stressSlider;
    private Slider energySlider;
    private TextField sleepHoursField;
    private TextArea moodNoteArea;
    private Button saveMoodBtn;
    private Button clearMoodBtn;
    private Button exportPdfBtn;
    private Button adminPanelBtn;
    private Label moodStatusLabel;
    
    // Journal Tab Components
    private TableView<Journal> journalTable;
    private TextField journalTitleField;
    private TextArea journalContentArea;
    private DatePicker journalDatePicker;
    private ComboBox<Mood> journalMoodCombo;
    private Button saveJournalBtn;
    private Button clearJournalBtn;
    private Button analyzeJournalBtn;
    private Label journalStatusLabel;
    
    // Charts
    private PieChart moodPieChart;
    private BarChart<String, Number> moodBarChart;

    @Override
    public Node build() {
        ensureSchema();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #f5f5f5;");

        // Create tab pane
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Create tabs
        Tab moodTab = new Tab("Mood");
        Tab journalTab = new Tab("Journal");
        
        moodTab.setContent(buildMoodTab());
        journalTab.setContent(buildJournalTab());
        
        tabPane.getTabs().addAll(moodTab, journalTab);
        
        root.setCenter(tabPane);
        
        // Load data
        reloadMoods();
        reloadJournals();
        updateCharts();

        return root;
    }

    private Node buildMoodTab() {
        VBox mainContainer = new VBox(10);
        mainContainer.setPadding(new Insets(10));
        
        // Top section - Form
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        formGrid.setPadding(new Insets(10));
        formGrid.setStyle("-fx-background-color: white; -fx-background-radius: 5; -fx-border-color: #ddd;");
        
        // Mood Type
        formGrid.add(new Label("Mood Type:"), 0, 0);
        moodTypeCombo = new ComboBox<>();
        moodTypeCombo.getItems().addAll("Happy", "Sad", "Anxious", "Angry", "Excited", "Calm", "Stressed", "Neutral");
        moodTypeCombo.setPromptText("Select mood");
        formGrid.add(moodTypeCombo, 1, 0);
        
        // Date
        formGrid.add(new Label("Date:"), 0, 1);
        moodDatePicker = new DatePicker(LocalDate.now());
        formGrid.add(moodDatePicker, 1, 1);
        
        // Stress Slider
        formGrid.add(new Label("Stress Level:"), 0, 2);
        stressSlider = new Slider(1, 10, 5);
        stressSlider.setShowTickLabels(true);
        stressSlider.setShowTickMarks(true);
        stressSlider.setMajorTickUnit(1);
        stressSlider.setMinorTickCount(0);
        formGrid.add(stressSlider, 1, 2);
        
        // Energy Slider
        formGrid.add(new Label("Energy Level:"), 0, 3);
        energySlider = new Slider(1, 10, 5);
        energySlider.setShowTickLabels(true);
        energySlider.setShowTickMarks(true);
        energySlider.setMajorTickUnit(1);
        energySlider.setMinorTickCount(0);
        formGrid.add(energySlider, 1, 3);
        
        // Sleep Hours
        formGrid.add(new Label("Sleep Hours:"), 0, 4);
        sleepHoursField = new TextField("8");
        sleepHoursField.setPromptText("Hours of sleep");
        formGrid.add(sleepHoursField, 1, 4);
        
        // Note
        formGrid.add(new Label("Note:"), 0, 5);
        moodNoteArea = new TextArea();
        moodNoteArea.setPromptText("Add a note about your mood...");
        moodNoteArea.setPrefRowCount(3);
        formGrid.add(moodNoteArea, 1, 5);
        
        // Buttons
        HBox buttonBox = new HBox(10);
        saveMoodBtn = new Button("Save Mood");
        clearMoodBtn = new Button("Clear");
        saveMoodBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        clearMoodBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        buttonBox.getChildren().addAll(saveMoodBtn, clearMoodBtn);
        formGrid.add(buttonBox, 1, 6);
        
        saveMoodBtn.setOnAction(e -> saveMood());
        clearMoodBtn.setOnAction(e -> clearMoodForm());
        
        // Middle section - Table
        moodTable = new TableView<>();
        moodTable.setItems(moods);
        moodTable.setPrefHeight(200);
        
        TableColumn<Mood, String> moodCol = new TableColumn<>("Mood");
        moodCol.setCellValueFactory(new PropertyValueFactory<>("moodType"));
        
        TableColumn<Mood, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("moodDate"));
        dateCol.setCellFactory(col -> new TableCell<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(fmt));
            }
        });
        
        TableColumn<Mood, Integer> stressCol = new TableColumn<>("Stress");
        stressCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getStressLevel()));
        
        TableColumn<Mood, Integer> energyCol = new TableColumn<>("Energy");
        energyCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getEnergyLevel()));
        
        TableColumn<Mood, String> noteCol = new TableColumn<>("Note");
        noteCol.setCellValueFactory(new PropertyValueFactory<>("note"));
        noteCol.setPrefWidth(200);
        
        moodTable.getColumns().addAll(moodCol, dateCol, stressCol, energyCol, noteCol);
        
        // Bottom section - Charts and Actions
        HBox bottomBox = new HBox(10);
        
        // Charts
        VBox chartsBox = new VBox(10);
        chartsBox.setPadding(new Insets(10));
        chartsBox.setStyle("-fx-background-color: white; -fx-border-color: #ddd;");
        
        moodPieChart = new PieChart();
        moodPieChart.setTitle("Mood Distribution");
        
        moodBarChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        moodBarChart.setTitle("Mood Trends");
        moodBarChart.setPrefHeight(200);
        
        chartsBox.getChildren().addAll(moodPieChart, moodBarChart);
        
        // Actions
        VBox actionsBox = new VBox(10);
        actionsBox.setPadding(new Insets(10));
        actionsBox.setStyle("-fx-background-color: white; -fx-border-color: #ddd;");
        
        exportPdfBtn = new Button("Export PDF");
        adminPanelBtn = new Button("Admin Panel");
        moodStatusLabel = new Label("");
        
        exportPdfBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        adminPanelBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        
        exportPdfBtn.setOnAction(e -> exportMoodPdf());
        adminPanelBtn.setOnAction(e -> openAdminPanel());
        
        actionsBox.getChildren().addAll(exportPdfBtn, adminPanelBtn, moodStatusLabel);
        
        bottomBox.getChildren().addAll(chartsBox, actionsBox);
        
        mainContainer.getChildren().addAll(formGrid, moodTable, bottomBox);
        
        return mainContainer;
    }

    private Node buildJournalTab() {
        VBox mainContainer = new VBox(10);
        mainContainer.setPadding(new Insets(10));
        
        // Top section - Form
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        formGrid.setPadding(new Insets(10));
        formGrid.setStyle("-fx-background-color: white; -fx-background-radius: 5; -fx-border-color: #ddd;");
        
        // Title
        formGrid.add(new Label("Title:"), 0, 0);
        journalTitleField = new TextField();
        journalTitleField.setPromptText("Enter journal title");
        formGrid.add(journalTitleField, 1, 0);
        
        // Content
        formGrid.add(new Label("Content:"), 0, 1);
        journalContentArea = new TextArea();
        journalContentArea.setPromptText("Write your journal entry...");
        journalContentArea.setPrefRowCount(5);
        formGrid.add(journalContentArea, 1, 1);
        
        // Date
        formGrid.add(new Label("Date:"), 0, 2);
        journalDatePicker = new DatePicker(LocalDate.now());
        formGrid.add(journalDatePicker, 1, 2);
        
        // Associated Mood
        formGrid.add(new Label("Associated Mood:"), 0, 3);
        journalMoodCombo = new ComboBox<>(moods);
        journalMoodCombo.setPromptText("Select mood (optional)");
        formGrid.add(journalMoodCombo, 1, 3);
        
        // Buttons
        HBox buttonBox = new HBox(10);
        saveJournalBtn = new Button("Save Journal");
        clearJournalBtn = new Button("Clear");
        analyzeJournalBtn = new Button("AI Analyze");
        saveJournalBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        clearJournalBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        analyzeJournalBtn.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white;");
        buttonBox.getChildren().addAll(saveJournalBtn, clearJournalBtn, analyzeJournalBtn);
        formGrid.add(buttonBox, 1, 4);
        
        saveJournalBtn.setOnAction(e -> saveJournal());
        clearJournalBtn.setOnAction(e -> clearJournalForm());
        analyzeJournalBtn.setOnAction(e -> analyzeJournal());
        
        // Middle section - Table
        journalTable = new TableView<>();
        journalTable.setItems(journals);
        journalTable.setPrefHeight(200);
        
        TableColumn<Journal, String> jTitleCol = new TableColumn<>("Title");
        jTitleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        jTitleCol.setPrefWidth(150);
        
        TableColumn<Journal, String> jContentCol = new TableColumn<>("Content");
        jContentCol.setCellValueFactory(new PropertyValueFactory<>("content"));
        jContentCol.setPrefWidth(300);
        
        TableColumn<Journal, LocalDate> jDateCol = new TableColumn<>("Date");
        jDateCol.setCellValueFactory(new PropertyValueFactory<>("entryDate"));
        jDateCol.setCellFactory(col -> new TableCell<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(fmt));
            }
        });
        
        journalTable.getColumns().addAll(jTitleCol, jContentCol, jDateCol);
        
        // Bottom section - Status
        VBox statusBox = new VBox(10);
        statusBox.setPadding(new Insets(10));
        statusBox.setStyle("-fx-background-color: white; -fx-border-color: #ddd;");
        journalStatusLabel = new Label("");
        statusBox.getChildren().add(journalStatusLabel);
        
        mainContainer.getChildren().addAll(formGrid, journalTable, statusBox);
        
        return mainContainer;
    }

    private void ensureSchema() {
        try {
            SchemaInitializer.ensureSchema();
        } catch (SQLException e) {
            System.err.println("Schema initialization failed: " + e.getMessage());
        }
    }

    private void saveMood() {
        try {
            String moodType = moodTypeCombo.getValue();
            if (moodType == null) {
                moodStatusLabel.setText("Please select a mood type");
                return;
            }
            
            moodService.createMood(
                moodType,
                moodDatePicker.getValue(),
                moodNoteArea.getText(),
                (int) stressSlider.getValue(),
                (int) energySlider.getValue(),
                null, // sleep time
                null, // wake time
                Double.parseDouble(sleepHoursField.getText()),
                null, // admin comment
                null  // support email sent
            );
            
            moodStatusLabel.setText("Mood saved successfully!");
            clearMoodForm();
            reloadMoods();
            updateCharts();
        } catch (Exception e) {
            moodStatusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void clearMoodForm() {
        moodTypeCombo.setValue(null);
        moodDatePicker.setValue(LocalDate.now());
        stressSlider.setValue(5);
        energySlider.setValue(5);
        sleepHoursField.setText("8");
        moodNoteArea.clear();
        moodStatusLabel.setText("");
    }

    private void saveJournal() {
        try {
            String title = journalTitleField.getText();
            if (title == null || title.trim().isEmpty()) {
                journalStatusLabel.setText("Please enter a title");
                return;
            }
            
            journalService.createJournal(
                title,
                journalContentArea.getText(),
                journalDatePicker.getValue(),
                journalMoodCombo.getValue() != null ? journalMoodCombo.getValue().getId() : null,
                null // userId - can be set based on current user if needed
            );
            
            journalStatusLabel.setText("Journal saved successfully!");
            clearJournalForm();
            reloadJournals();
        } catch (Exception e) {
            journalStatusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void clearJournalForm() {
        journalTitleField.clear();
        journalContentArea.clear();
        journalDatePicker.setValue(LocalDate.now());
        journalMoodCombo.setValue(null);
        journalStatusLabel.setText("");
    }

    private void analyzeJournal() {
        Journal selected = journalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            journalStatusLabel.setText("Please select a journal entry to analyze");
            return;
        }
        
        try {
            JournalAiAnalysisService aiService = new JournalAiAnalysisService();
            String analysis = aiService.analyze(selected);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("AI Analysis");
            alert.setHeaderText("Journal Analysis");
            TextArea area = new TextArea(analysis);
            area.setEditable(false);
            area.setWrapText(true);
            area.setPrefRowCount(10);
            alert.getDialogPane().setContent(area);
            alert.showAndWait();
        } catch (Exception e) {
            journalStatusLabel.setText("AI Analysis failed: " + e.getMessage());
        }
    }

    private void exportMoodPdf() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export Moods to PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            chooser.setInitialFileName("moods.pdf");
            File file = chooser.showSaveDialog(null);
            
            if (file != null) {
                MoodJournalPdfService pdfService = new MoodJournalPdfService();
                Path out = pdfService.exportMoods(file.toPath(), moods);
                moodStatusLabel.setText("PDF exported to: " + out.getFileName());
            }
        } catch (Exception e) {
            moodStatusLabel.setText("Export failed: " + e.getMessage());
        }
    }

    private void openAdminPanel() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Admin Panel");
        alert.setHeaderText("Admin Functions");
        alert.setContentText("Admin panel features:\n• View all user moods\n• Send support emails\n• Generate reports\n• Manage users");
        alert.showAndWait();
    }

    private void reloadMoods() {
        try {
            List<Mood> allMoods = moodService.getAllMoods();
            moods.setAll(allMoods);
        } catch (Exception e) {
            System.err.println("Error loading moods: " + e.getMessage());
        }
    }

    private void reloadJournals() {
        try {
            List<Journal> allJournals = journalService.getAllJournals();
            journals.setAll(allJournals);
        } catch (Exception e) {
            System.err.println("Error loading journals: " + e.getMessage());
        }
    }

    private void updateCharts() {
        // Update pie chart
        Map<String, Integer> moodCounts = new HashMap<>();
        for (Mood mood : moods) {
            String type = mood.getMoodType();
            moodCounts.put(type, moodCounts.getOrDefault(type, 0) + 1);
        }
        
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }
        moodPieChart.setData(pieChartData);
        
        // Update bar chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        moodBarChart = new BarChart<>(xAxis, yAxis);
        moodBarChart.setTitle("Mood Trends");
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Mood Count");
        
        for (Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        
        moodBarChart.getData().add(series);
    }
}
