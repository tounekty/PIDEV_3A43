package org.example.ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.example.model.Journal;
import org.example.model.Mood;
import org.example.service.AlertService;
import org.example.service.AppService;
import org.example.service.EmailApiService;
import org.example.service.MoodAlert;
import org.example.service.ServiceException;
import org.example.service.ValidationException;
import org.example.ui.template.ThemeStyle;

import java.util.List;

/**
 * Admin dashboard for reviewing alerts, moods, journals, and admin comments.
 */
public class AdminDashboard {
    private final AppService appService = AppService.getInstance();
    private final AlertService alertService = new AlertService();
    private final Integer currentAdminUserId;
    private final Runnable onBackToLogin;

    private final ObservableList<MoodAlert> alertList = FXCollections.observableArrayList();
    private final ObservableList<Mood> moodItems = FXCollections.observableArrayList();
    private final ObservableList<Journal> journalItems = FXCollections.observableArrayList();

    private final TableView<Mood> moodTable = new TableView<>();
    private final TableView<Journal> journalTable = new TableView<>();
    private final TextArea moodCommentArea = new TextArea();
    private final TextArea journalCommentArea = new TextArea();
    private final Label moodDetailLabel = new Label("Select a mood to review it.");
    private final Label journalDetailLabel = new Label("Select a journal to review it.");
    private final Label summaryLabel = new Label("Loading dashboard...");

    public AdminDashboard() {
        this(null, null);
    }

    public AdminDashboard(Runnable onBackToLogin) {
        this(null, onBackToLogin);
    }

    public AdminDashboard(Integer currentAdminUserId, Runnable onBackToLogin) {
        this.currentAdminUserId = currentAdminUserId;
        this.onBackToLogin = onBackToLogin;
    }

    public Parent build() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + toHex(ThemeStyle.BACKGROUND_COLOR) + ";");
        root.setTop(createHeader());
        root.setCenter(createContent());
        root.setBottom(createFooter());

        configureMoodTable();
        configureJournalTable();
        loadAll();
        return root;
    }

    private VBox createHeader() {
        VBox header = new VBox(8);
        header.setPadding(new Insets(24, 30, 24, 30));
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, #163D7A, #245EBD);" +
                "-fx-background-radius: 0 0 24 24;"
        );

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox copy = new VBox(8);
        Label titleLabel = new Label("Admin dashboard");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: white;");

        Label subtitleLabel = new Label("Review alerts, browse moods and journals, and leave admin comments.");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.82);");

        summaryLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: rgba(255,255,255,0.88);");
        copy.getChildren().addAll(titleLabel, subtitleLabel, summaryLabel);
        topRow.getChildren().add(copy);

        if (onBackToLogin != null) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button backButton = new Button("Retour login");
            backButton.setStyle(
                    "-fx-padding: 12 20 12 20;" +
                    "-fx-font-size: 13px;" +
                    "-fx-font-weight: 700;" +
                    "-fx-background-color: rgba(255,255,255,0.12);" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 16;" +
                    "-fx-border-radius: 16;" +
                    "-fx-border-color: rgba(255,255,255,0.22);" +
                    "-fx-border-width: 1.1;"
            );
            backButton.setOnAction(e -> onBackToLogin.run());
            topRow.getChildren().addAll(spacer, backButton);
        }

        header.getChildren().add(topRow);
        return header;
    }

    private Parent createContent() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                new Tab("Alerts", createAlertsView()),
                new Tab("Moods", createMoodManagementView()),
                new Tab("Journals", createJournalManagementView())
        );

        VBox wrapper = new VBox(tabs);
        wrapper.setPadding(new Insets(18, 20, 20, 20));
        VBox.setVgrow(tabs, Priority.ALWAYS);
        return wrapper;
    }

    private Parent createAlertsView() {
        VBox content = new VBox(14);
        content.setPadding(new Insets(18));

        Label title = new Label("Active alerts");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #1C4F96;");

        ListView<MoodAlert> alertsView = new ListView<>(alertList);
        alertsView.setPrefHeight(420);
        alertsView.setCellFactory(param -> new AlertListCell());
        VBox.setVgrow(alertsView, Priority.ALWAYS);

        content.getChildren().addAll(title, alertsView);
        return wrapCard(content);
    }

    private Parent createMoodManagementView() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(18));

        Label title = new Label("Mood review");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #1C4F96;");

        moodDetailLabel.setWrapText(true);
        moodDetailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6F87A6;");

        moodCommentArea.setPromptText("Write an admin comment for this mood...");
        moodCommentArea.setPrefRowCount(5);
        moodCommentArea.setWrapText(true);
        moodCommentArea.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= org.example.service.FormValidator.ADMIN_COMMENT_MAX_LENGTH ? change : null));

        moodTable.setPrefHeight(360);
        moodTable.setMinHeight(240);

        Button saveBtn = new Button("Save mood comment");
        saveBtn.getStyleClass().add("success");
        saveBtn.setOnAction(e -> saveMoodComment());

        Button sendSupportEmailBtn = new Button("Send support email to student");
        sendSupportEmailBtn.setOnAction(e -> sendSupportEmailToStudent());

        VBox.setVgrow(moodTable, Priority.ALWAYS);
        content.getChildren().addAll(title, moodTable, moodDetailLabel, moodCommentArea, saveBtn, sendSupportEmailBtn);
        return wrapScrollableCard(content);
    }

    private Parent createJournalManagementView() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(18));

        Label title = new Label("Journal review");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #1C4F96;");

        journalDetailLabel.setWrapText(true);
        journalDetailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6F87A6;");

        journalCommentArea.setPromptText("Write an admin comment for this journal...");
        journalCommentArea.setPrefRowCount(5);
        journalCommentArea.setWrapText(true);
        journalCommentArea.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= org.example.service.FormValidator.ADMIN_COMMENT_MAX_LENGTH ? change : null));

        Button saveBtn = new Button("Save journal comment");
        saveBtn.getStyleClass().add("success");
        saveBtn.setOnAction(e -> saveJournalComment());

        VBox.setVgrow(journalTable, Priority.ALWAYS);
        content.getChildren().addAll(title, journalTable, journalDetailLabel, journalCommentArea, saveBtn);
        return wrapCard(content);
    }

    private HBox createFooter() {
        HBox footer = new HBox(10);
        footer.setPadding(new Insets(14, 20, 18, 20));
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button refreshBtn = new Button("Refresh dashboard");
        refreshBtn.setOnAction(e -> loadAll());

        footer.getChildren().add(refreshBtn);
        return footer;
    }

    private VBox wrapCard(Parent content) {
        VBox card = new VBox(content);
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 24;" +
                "-fx-border-color: " + toHex(ThemeStyle.BORDER_COLOR) + ";" +
                "-fx-border-radius: 24;" +
                "-fx-border-width: 1;"
        );
        VBox.setVgrow(content, Priority.ALWAYS);
        return card;
    }

    private VBox wrapScrollableCard(Parent content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox card = new VBox(scrollPane);
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 24;" +
                "-fx-border-color: " + toHex(ThemeStyle.BORDER_COLOR) + ";" +
                "-fx-border-radius: 24;" +
                "-fx-border-width: 1;"
        );
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return card;
    }

    private void configureMoodTable() {
        TableColumn<Mood, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        idCol.setPrefWidth(70);

        TableColumn<Mood, String> typeCol = new TableColumn<>("Mood");
        typeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getMoodType()));
        typeCol.setPrefWidth(160);

        TableColumn<Mood, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getMoodDate())));
        dateCol.setPrefWidth(140);

        TableColumn<Mood, String> studentCol = new TableColumn<>("Etudiant");
        studentCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(valueOrEmpty(data.getValue().getStudentName())));
        studentCol.setPrefWidth(220);

        TableColumn<Mood, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(valueOrEmpty(data.getValue().getStudentEmail())));
        emailCol.setPrefWidth(280);

        TableColumn<Mood, String> stressCol = new TableColumn<>("Stress");
        stressCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatLevel(data.getValue().getStressLevel())));
        stressCol.setPrefWidth(90);

        TableColumn<Mood, String> energyCol = new TableColumn<>("Energy");
        energyCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatLevel(data.getValue().getEnergyLevel())));
        energyCol.setPrefWidth(90);

        TableColumn<Mood, String> noteCol = new TableColumn<>("Note");
        noteCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(valueOrEmpty(data.getValue().getNote())));
        noteCol.setPrefWidth(360);

        TableColumn<Mood, String> commentCol = new TableColumn<>("Admin Comment");
        commentCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(valueOrEmpty(data.getValue().getAdminComment())));
        commentCol.setPrefWidth(340);

        TableColumn<Mood, String> statusCol = new TableColumn<>("Case Status");
        statusCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().isSupportEmailSent() ? "Solved" : "Not solved yet"));
        statusCol.setPrefWidth(170);

        moodTable.getColumns().setAll(idCol, typeCol, dateCol, studentCol, emailCol, stressCol, energyCol, noteCol, commentCol, statusCol);
        moodTable.setItems(moodItems);
        moodTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        moodTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected == null) {
                moodDetailLabel.setText("Select a mood to review it.");
                moodCommentArea.clear();
                return;
            }
            moodDetailLabel.setText("Mood #" + selected.getId() + " | " + selected.getMoodType() + " | " + selected.getMoodDate()
                    + " | Etudiant: " + valueOrEmpty(selected.getStudentName())
                    + " | Email: " + valueOrEmpty(selected.getStudentEmail())
                    + " | Stress: " + formatLevel(selected.getStressLevel())
                    + " | Energy: " + formatLevel(selected.getEnergyLevel())
                    + " | Case: " + (selected.isSupportEmailSent() ? "Solved" : "Not solved yet")
                    + "\nUser note: " + valueOrEmpty(selected.getNote()));
            moodCommentArea.setText(valueOrEmpty(selected.getAdminComment()));
        });
    }

    private void configureJournalTable() {
        TableColumn<Journal, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        idCol.setPrefWidth(70);

        TableColumn<Journal, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getTitle()));
        titleCol.setPrefWidth(180);

        TableColumn<Journal, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getEntryDate())));
        dateCol.setPrefWidth(130);

        TableColumn<Journal, String> moodIdCol = new TableColumn<>("Mood ID");
        moodIdCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getMoodId() == null ? "" : String.valueOf(data.getValue().getMoodId())));
        moodIdCol.setPrefWidth(100);

        TableColumn<Journal, String> contentCol = new TableColumn<>("Content");
        contentCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(valueOrEmpty(data.getValue().getContent())));
        contentCol.setPrefWidth(320);

        TableColumn<Journal, String> commentCol = new TableColumn<>("Admin Comment");
        commentCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(valueOrEmpty(data.getValue().getAdminComment())));
        commentCol.setPrefWidth(280);

        journalTable.getColumns().setAll(idCol, titleCol, dateCol, moodIdCol, contentCol, commentCol);
        journalTable.setItems(journalItems);
        journalTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        journalTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected == null) {
                journalDetailLabel.setText("Select a journal to review it.");
                journalCommentArea.clear();
                return;
            }
            journalDetailLabel.setText("Journal #" + selected.getId() + " | " + selected.getTitle() + " | " + selected.getEntryDate()
                    + "\nContent: " + valueOrEmpty(selected.getContent()));
            journalCommentArea.setText(valueOrEmpty(selected.getAdminComment()));
        });
    }

    private void loadAll() {
        try {
            List<Mood> moods = appService.getAllMoods();
            List<Journal> journals = appService.getAllJournals();
            List<MoodAlert> alerts = alertService.detectAlerts(moods);

            moodItems.setAll(moods);
            journalItems.setAll(journals);
            alertList.setAll(alerts);

            summaryLabel.setText("Alerts: " + alerts.size() + " | Moods: " + moods.size() + " | Journals: " + journals.size());
        } catch (ServiceException e) {
            summaryLabel.setText("Failed to load dashboard data");
        }
    }

    private void saveMoodComment() {
        Mood selected = moodTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            org.example.service.FormValidator.validateAdminComment(moodCommentArea.getText());
            appService.saveMoodAdminComment(selected.getId(), blankToNull(moodCommentArea.getText()));
            loadAll();
            reselectMood(selected.getId());
        } catch (ValidationException e) {
            moodDetailLabel.setText(e.getMessage());
        } catch (ServiceException e) {
            moodDetailLabel.setText("Could not save mood comment: " + e.getMessage());
        }
    }

    private void sendSupportEmailToStudent() {
        Mood selected = moodTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            moodDetailLabel.setText("Select a mood before sending an email.");
            return;
        }
        try {
            EmailApiService.EmailResult result = appService.sendSupportEmailToStudent(
                    selected,
                    moodCommentArea.getText(),
                    currentAdminUserId
            );
            loadAll();
            reselectMood(selected.getId());
            moodDetailLabel.setText(result.message());
            if (result.sent()) {
                showSuccessAlert("Email sent", result.message());
            }
        } catch (ServiceException e) {
            moodDetailLabel.setText("Could not send support email: " + e.getMessage());
        }
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void saveJournalComment() {
        Journal selected = journalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            org.example.service.FormValidator.validateAdminComment(journalCommentArea.getText());
            appService.saveJournalAdminComment(selected.getId(), blankToNull(journalCommentArea.getText()));
            loadAll();
            reselectJournal(selected.getId());
        } catch (ValidationException e) {
            journalDetailLabel.setText(e.getMessage());
        } catch (ServiceException e) {
            journalDetailLabel.setText("Could not save journal comment: " + e.getMessage());
        }
    }

    private void reselectMood(int id) {
        moodItems.stream()
                .filter(mood -> mood.getId() == id)
                .findFirst()
                .ifPresent(mood -> moodTable.getSelectionModel().select(mood));
    }

    private void reselectJournal(int id) {
        journalItems.stream()
                .filter(journal -> journal.getId() == id)
                .findFirst()
                .ifPresent(journal -> journalTable.getSelectionModel().select(journal));
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

    private String formatLevel(Integer level) {
        return level == null ? "-" : String.valueOf(level);
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private static class AlertListCell extends javafx.scene.control.ListCell<MoodAlert> {
        @Override
        protected void updateItem(MoodAlert alert, boolean empty) {
            super.updateItem(alert, empty);

            if (empty || alert == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            VBox cell = new VBox(5);
            cell.setPadding(new Insets(12));
            cell.setStyle("-fx-border-color: " + getSeverityColor(alert.getSeverity()) +
                    "; -fx-border-width: 0 0 0 4; -fx-background-color: white;");

            Label titleLabel = new Label(alert.getTitle());
            titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " +
                    getSeverityColor(alert.getSeverity()) + ";");

            Label messageLabel = new Label(alert.getMessage());
            messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #1C4F96;");
            messageLabel.setWrapText(true);

            HBox footer = new HBox(16);
            Label severity = new Label("Severity: " + alert.getSeverityLabel());
            severity.setStyle("-fx-font-size: 11px; -fx-text-fill: #6F87A6;");

            Label date = new Label("Detected: " + alert.getDetectedDate());
            date.setStyle("-fx-font-size: 11px; -fx-text-fill: #6F87A6;");
            footer.getChildren().addAll(severity, date);

            cell.getChildren().addAll(titleLabel, messageLabel, footer);
            setGraphic(cell);
        }

        private static String getSeverityColor(int severity) {
            return switch (severity) {
                case 1 -> "#3A8F6D";
                case 2 -> "#E8B86D";
                case 3 -> "#C46A5A";
                case 4, 5 -> "#8B0000";
                default -> "#1C4F96";
            };
        }
    }
}
