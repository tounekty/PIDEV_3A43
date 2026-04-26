package org.example.view;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.model.User;
import org.example.model.Event;
import org.example.model.ForumMessage;
import org.example.model.ForumSubject;
import org.example.model.ReservationRecord;
import org.example.controller.AuthController;
import org.example.controller.EventController;
import org.example.controller.ForumMessageController;
import org.example.controller.ForumController;
import org.example.controller.ReservationController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main extends Application {
    private static final DateTimeFormatter EVENT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter RES_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String ROOT = "-fx-background-color: linear-gradient(to bottom right,#ffffff,#edf6ff);";
    private static final String CARD = "-fx-background-color: rgba(255,255,255,0.96); -fx-background-radius: 24; -fx-border-radius: 24; -fx-border-color: rgba(120,169,230,0.20); -fx-effect: dropshadow(gaussian, rgba(46,94,166,0.12), 24, 0.18, 0, 8);";
    private static final String PRIMARY = "-fx-background-color: linear-gradient(to right,#0f69ff,#38a4ff); -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 14; -fx-padding: 12 18 12 18;";
    private static final String SECONDARY = "-fx-background-color: white; -fx-text-fill: #1c4f96; -fx-font-weight: 700; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #cfe3ff; -fx-padding: 11 16 11 16;";
    private static final String DANGER = "-fx-background-color: #fff4f4; -fx-text-fill: #c63d48; -fx-font-weight: 700; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #ffd5d8; -fx-padding: 11 16 11 16;";
    private static final String INPUT = "-fx-background-color: #f9fbff; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #d7e7ff; -fx-padding: 12 14 12 14;";
    private static final String REACTION_NEUTRAL = "-fx-background-color: white; -fx-text-fill: #2b5f9e; -fx-font-weight: 700; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #cfe3ff; -fx-padding: 8 12 8 12;";
    private static final String REACTION_LIKE_ACTIVE = "-fx-background-color: linear-gradient(to right,#0f69ff,#38a4ff); -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: transparent; -fx-padding: 8 12 8 12;";
    private static final String REACTION_DISLIKE_ACTIVE = "-fx-background-color: #ff6b7a; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: transparent; -fx-padding: 8 12 8 12;";
    private static final int MAX_MESSAGE_THREAD_LEVEL = 3;
    private static final int POPULAR_SUBJECT_SCORE_THRESHOLD = 10;

    private final EventController eventService = new EventController();
    private final AuthController authService = new AuthController();
    private final ReservationController reservationService = new ReservationController();
    private final ForumController forumService = new ForumController();
    private final ForumMessageController forumMessageService = new ForumMessageController();
    private final Map<Integer, Integer> reservationCounts = new HashMap<>();
    private final Map<Integer, ForumMessage> messageIndexById = new HashMap<>();
    private final Set<Integer> reservedEventIds = new HashSet<>();
    private VBox homePage;

    private BorderPane root;
    private StackPane pageContainer;
    private VBox loginPage;
    private VBox registerPage;
    private VBox formPage;
    private VBox eventsPage;
    private VBox reservationsPage;
    private VBox forumPage;
    private VBox subjectFormPage;
    private VBox messagesPage;
    private VBox statsPage;
    private VBox header;
    private Stage primaryStage;
    private User currentUser;
    private Event editingEvent;

    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField locationField;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;
    @FXML private TextField capacityField;
    @FXML private ComboBox<String> categoryField;
    @FXML private TextField imageField;
    @FXML private Label eventImageMeta;
    @FXML private ImageView eventImagePreview;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortField;
    @FXML private TextField loginUsernameField;
    @FXML private PasswordField loginPasswordField;
    @FXML private Label loginErrorLabel;
    @FXML private TextField registerUsernameField;
    @FXML private PasswordField registerPasswordField;
    @FXML private PasswordField registerConfirmPasswordField;
    @FXML private Label registerErrorLabel;
    private Label userBadge;
    @FXML private Label formTitle;
    @FXML private Label eventsTitle;
    @FXML private Label eventsSubtitle;
    @FXML private Label homeEventsCount;
    @FXML private Label homeForumCount;
    @FXML private Label homeReservationsCount;
    @FXML private Label homeEventsIcon;
    @FXML private Label homeForumIcon;
    @FXML private Label homeReservationsIcon;
    @FXML private Button saveButton;
    @FXML private Button cancelEditButton;
    private Button addHeaderButton;
    private Button eventsHeaderButton;
    private Button reservationsHeaderButton;
    private Button forumHeaderButton;
    private Button statsHeaderButton;
    private Button logoutButton;
    @FXML private ListView<Event> eventListView;
    @FXML private ListView<ReservationRecord> reservationListView;
    @FXML private ListView<ForumSubject> forumListView;
    @FXML private ListView<ForumMessage> messageListView;
    @FXML private TextField forumSearchField;
    @FXML private ComboBox<String> forumSortField;
    @FXML private Label forumTitle;
    @FXML private Label forumErrorLabel;
    @FXML private Label messagesTitle;
    @FXML private Label messagesErrorLabel;
    @FXML private Label subjectFormTitle;
    @FXML private Label subjectErrorLabel;
    @FXML private VBox statsBox;

    private ForumSubject editingSubject;
    private ForumSubject currentSubject;
    private ForumMessage replyingToMessage;

    @FXML private TextField subjectTitleField;
    @FXML private TextArea subjectDescriptionArea;
    @FXML private ToggleGroup subjectStatusGroup;
    @FXML private ToggleButton subjectStatusOpenButton;
    @FXML private ToggleButton subjectStatusClosedButton;
    @FXML private ToggleButton subjectStatusArchivedButton;
    @FXML private TextField subjectImageUrlField;
    @FXML private Label subjectImageMeta;
    @FXML private ImageView subjectImagePreview;
    @FXML private CheckBox subjectPinnedCheck;
    @FXML private CheckBox subjectAnonymousCheck;
    @FXML private Button subjectSaveButton;
    @FXML private Button subjectCancelButton;
    @FXML private TextArea messageContentArea;
    @FXML private CheckBox messageAnonymousCheck;
    @FXML private TextField messageAttachmentPathField;
    @FXML private Label messageAttachmentMeta;
    @FXML private Label messageReplyInfoLabel;
    @FXML private Button messageCancelReplyButton;
    private String messageAttachmentMimeType;
    private Long messageAttachmentSize;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        root = new BorderPane();
        root.setStyle(ROOT);
        root.setPadding(new Insets(22));
        header = buildHeader();
        root.setTop(header);
        pageContainer = new StackPane();
        loginPage = buildLoginPage();
        registerPage = buildRegisterPage();
        homePage = buildHomePage();  // Add this line
        formPage = buildFormPage();
        eventsPage = buildEventsPage();
        reservationsPage = buildReservationsPage();
        forumPage = buildForumPage();
        subjectFormPage = buildSubjectFormPage();
        messagesPage = buildMessagesPage();
        try {
            statsPage = buildStatsPageGlobal();
        } catch (SQLException e) {
            statsPage = new VBox(new Label("Erreur chargement stats"));
        }
        pageContainer.getChildren().setAll(loginPage);
        root.setCenter(pageContainer);
        stage.setScene(new Scene(root, 1200, 760));
        stage.setTitle("MindCare");
        stage.show();
        initializeDatabase();
        showLoginPage();
    }

    private VBox buildHeader() {
        addHeaderButton = button("Ajouter", PRIMARY, e -> showPage(formPage));
        eventsHeaderButton = button("Evenements", SECONDARY, e -> { loadEvents(); showPage(eventsPage); });
        reservationsHeaderButton = button("Reservations", SECONDARY, e -> { loadReservations(); showPage(reservationsPage); });
        forumHeaderButton = button("Forum", SECONDARY, e -> { loadForumSubjects(); showPage(forumPage); });
        statsHeaderButton = button("Statistiques", SECONDARY, e -> { try { statsPage = buildStatsPageGlobal(); showPage(statsPage); } catch (SQLException ex) { showError("Erreur", ex.getMessage()); } });
        logoutButton = button("Deconnexion", SECONDARY, e -> logout());
        userBadge = new Label();
        userBadge.setStyle("-fx-text-fill:#0f69ff; -fx-background-color:rgba(15,105,255,0.10); -fx-background-radius:999; -fx-padding:8 12 8 12; -fx-font-size:12px; -fx-font-weight:800;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(12, spacer, userBadge, addHeaderButton, eventsHeaderButton, reservationsHeaderButton, forumHeaderButton, statsHeaderButton, logoutButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox box = new VBox(12, actions);
        box.setPadding(new Insets(0, 0, 8, 0));
        return box;
    }

    private VBox buildLoginPage() {
        return loadPage("LoginView.fxml");
    }

    private VBox buildRegisterPage() {
        return loadPage("RegisterView.fxml");
    }

    private VBox loadPage(String resource) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(resource), "Missing resource: " + resource));
            loader.setController(this);
            return loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load " + resource, e);
        }
    }

    private VBox buildFormPage() {
        VBox page = loadPage("FormView.fxml");
        initFormPage();
        return page;
    }

    private void initFormPage() {
        if (categoryField != null && categoryField.getItems().isEmpty()) {
            categoryField.setItems(FXCollections.observableArrayList("yoga", "wellness", "sport", "meditation"));
        }
        if (categoryField != null) {
            categoryField.setValue("yoga");
            categoryField.setMaxWidth(Double.MAX_VALUE);
        }
        if (datePicker != null && datePicker.getValue() == null) {
            datePicker.setValue(LocalDate.now().plusDays(1));
        }
    }

    private VBox buildEventsPage() {
        VBox page = loadPage("EventsView.fxml");
        initEventsPage();
        return page;
    }

    private void initEventsPage() {
        if (eventListView != null) {
            eventListView.setCellFactory(v -> new EventCell());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldText, newText) -> loadEvents());
            searchField.setMaxWidth(320);
        }
        if (sortField != null) {
            if (sortField.getItems().isEmpty()) {
                sortField.setItems(FXCollections.observableArrayList("Par defaut", "Date", "Capacite", "Categorie", "Lieu"));
            }
            sortField.setValue("Par defaut");
            sortField.setOnAction(e -> loadEvents());
            sortField.setMaxWidth(180);
        }
    }

    private VBox buildStatsPageGlobal() throws SQLException {
        VBox page = loadPage("StatsView.fxml");
        if (statsBox != null) {
            statsBox.getChildren().clear();
            populateStatsBox(statsBox);
        }
        return page;
    }

    private void populateStatsBox(VBox statsBox) throws SQLException {
        statsBox.setPadding(new Insets(28));
        statsBox.setStyle("-fx-background-color:transparent;");

        Label title = title("Statistiques globales", 24);
        statsBox.getChildren().add(title);

        Map<String, Integer> reservationsByCategory = reservationService.getReservationCountByCategory();
        Map<String, Integer> eventsByCategory = reservationService.getEventCountByCategory();

        HBox chartsRow = new HBox(40);
        chartsRow.setAlignment(Pos.TOP_CENTER);

        if (!reservationsByCategory.isEmpty()) {
            VBox resBox = new VBox(12);
            Label resTitle = title("Reservations par categorie", 16);
            resBox.getChildren().add(resTitle);

            javafx.scene.chart.PieChart resPieChart = new javafx.scene.chart.PieChart();
            resPieChart.setTitle("Reservations");
            resPieChart.setLegendSide(javafx.geometry.Side.RIGHT);
            resPieChart.setPrefSize(400, 300);

            int totalRes = reservationsByCategory.values().stream().mapToInt(Integer::intValue).sum();
            for (Map.Entry<String, Integer> entry : reservationsByCategory.entrySet()) {
                String cat = entry.getKey().isEmpty() ? "Non categorise" : entry.getKey();
                int count = entry.getValue();
                double percentage = totalRes > 0 ? (100.0 * count / totalRes) : 0;
                resPieChart.getData().add(new javafx.scene.chart.PieChart.Data(cat + " (" + count + ")", percentage));
            }
            resBox.getChildren().add(resPieChart);
            chartsRow.getChildren().add(resBox);
        }

        if (!eventsByCategory.isEmpty()) {
            VBox evtBox = new VBox(12);
            Label evtTitle = title("Evenements par categorie", 16);
            evtBox.getChildren().add(evtTitle);

            javafx.scene.chart.PieChart evtPieChart = new javafx.scene.chart.PieChart();
            evtPieChart.setTitle("Evenements");
            evtPieChart.setLegendSide(javafx.geometry.Side.RIGHT);
            evtPieChart.setPrefSize(400, 300);

            int totalEvt = eventsByCategory.values().stream().mapToInt(Integer::intValue).sum();
            for (Map.Entry<String, Integer> entry : eventsByCategory.entrySet()) {
                String cat = entry.getKey().isEmpty() ? "Non categorise" : entry.getKey();
                int count = entry.getValue();
                double percentage = totalEvt > 0 ? (100.0 * count / totalEvt) : 0;
                evtPieChart.getData().add(new javafx.scene.chart.PieChart.Data(cat + " (" + count + ")", percentage));
            }
            evtBox.getChildren().add(evtPieChart);
            chartsRow.getChildren().add(evtBox);
        }

        statsBox.getChildren().add(chartsRow);
    }

    private VBox buildReservationsPage() {
        VBox page = loadPage("ReservationsView.fxml");
        initReservationsPage();
        return page;
    }

    private void initReservationsPage() {
        if (reservationListView != null) {
            reservationListView.setCellFactory(v -> new ReservationCell());
        }
    }

    private VBox buildForumPage() {
        VBox page = loadPage("ForumView.fxml");
        initForumPage();
        return page;
    }

    private void initForumPage() {
        clearInlineError(forumErrorLabel);
        if (forumListView != null) {
            forumListView.setCellFactory(v -> new ForumSubjectCell());
        }
        if (forumSearchField != null) {
            forumSearchField.textProperty().addListener((obs, oldText, newText) -> loadForumSubjects());
            forumSearchField.setMaxWidth(320);
        }
        if (forumSortField != null) {
            if (forumSortField.getItems().isEmpty()) {
                forumSortField.setItems(FXCollections.observableArrayList("Par defaut", "Date", "Pinned", "Categorie", "Statut"));
            }
            forumSortField.setValue("Par defaut");
            forumSortField.setOnAction(e -> loadForumSubjects());
            forumSortField.setMaxWidth(180);
        }
    }

    private VBox buildSubjectFormPage() {
        VBox page = loadPage("SubjectFormView.fxml");
        initSubjectFormPage();
        return page;
    }

    private void initSubjectFormPage() {
        if (subjectImageUrlField != null) {
            subjectImageUrlField.setEditable(false);
        }
        initSubjectStatusButtons();
    }

    private void initSubjectStatusButtons() {
        if (subjectStatusOpenButton == null || subjectStatusClosedButton == null || subjectStatusArchivedButton == null) {
            return;
        }
        if (subjectStatusGroup == null) {
            subjectStatusGroup = new ToggleGroup();
        }
        subjectStatusOpenButton.setToggleGroup(subjectStatusGroup);
        subjectStatusClosedButton.setToggleGroup(subjectStatusGroup);
        subjectStatusArchivedButton.setToggleGroup(subjectStatusGroup);
        if (subjectStatusGroup.getSelectedToggle() == null) {
            subjectStatusOpenButton.setSelected(true);
        }
        subjectStatusGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> applyStatusButtonStyles());
        applyStatusButtonStyles();
    }

    private void applyStatusButtonStyles() {
        if (subjectStatusOpenButton == null || subjectStatusClosedButton == null || subjectStatusArchivedButton == null) {
            return;
        }
        styleToggleButton(subjectStatusOpenButton);
        styleToggleButton(subjectStatusClosedButton);
        styleToggleButton(subjectStatusArchivedButton);
    }

    private void styleToggleButton(ToggleButton button) {
        if (button == null) return;
        if (button.isSelected()) {
            button.setStyle(PRIMARY);
        } else {
            button.setStyle(SECONDARY);
        }
    }

    private VBox buildMessagesPage() {
        VBox page = loadPage("MessagesView.fxml");
        initMessagesPage();
        return page;
    }

    private void initMessagesPage() {
        clearInlineError(messagesErrorLabel);
        if (messageListView != null) {
            messageListView.setCellFactory(v -> new ForumMessageCell());
        }
        if (messageAttachmentPathField != null) {
            messageAttachmentPathField.setEditable(false);
        }
        clearMessageReplyContext();
    }

    private void initializeDatabase() {
        try {
            authService.initializeUsers();
            System.out.println("✓ Users initialized");
            eventService.createTableIfNotExists();
            System.out.println("✓ Events table created");
            reservationService.initializeReservations();
            System.out.println("✓ Reservations table created");
            forumService.createTableIfNotExists();
            System.out.println("✓ Forum table created");
            forumMessageService.createTableIfNotExists();
            System.out.println("✓ Forum messages table created");
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
            showError("Initialisation BDD impossible", e.getMessage());
        }
    }
    private VBox buildHomePage() {
        VBox page = loadPage("HomeView.fxml");
        initHomePage();
        return page;
    }

    private void initHomePage() {
        if (homeEventsIcon != null) homeEventsIcon.setText("📅");
        if (homeForumIcon != null) homeForumIcon.setText("💬");
        if (homeReservationsIcon != null) homeReservationsIcon.setText("✅");
        if (homeEventsCount != null) homeEventsCount.setText("0");
        if (homeForumCount != null) homeForumCount.setText("0");
        if (homeReservationsCount != null) homeReservationsCount.setText("0");
    }

    private void updateHomePageStats() {
        if (homeEventsCount != null && homeForumCount != null && homeReservationsCount != null) {
            try {
                int eventCount = eventService.getAllEvents().size();
                homeEventsCount.setText(String.valueOf(eventCount));

                int forumSubjectCount = forumService.getAllSubjects().size();
                homeForumCount.setText(String.valueOf(forumSubjectCount));

                int reservationCount = reservationService.getTotalReservations();
                homeReservationsCount.setText(String.valueOf(reservationCount));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleLogin() {
        attemptLogin();
    }

    @FXML
    private void handleShowRegister() {
        showRegisterPage();
    }

    @FXML
    private void handleRegister() {
        attemptRegister();
    }

    @FXML
    private void handleShowLogin() {
        showLoginPage();
    }

    @FXML
    private void handleChooseImage() {
        chooseImage();
    }

    @FXML
    private void handleClearImage() {
        clearImage();
    }

    @FXML
    private void handleSaveEvent() {
        saveEvent();
    }

    @FXML
    private void handleCancelEdit() {
        resetForm();
    }

    @FXML
    private void handleViewCatalog() {
        loadEvents();
        showPage(eventsPage);
    }

    @FXML
    private void handleClearEventsSearch() {
        if (searchField != null) searchField.clear();
        if (sortField != null) sortField.setValue("Par defaut");
        loadEvents();
    }

    @FXML
    private void handleRefreshEvents() {
        loadEvents();
    }

    @FXML
    private void handleRefreshReservations() {
        loadReservations();
    }

    @FXML
    private void handleNewSubject() {
        showSubjectForm(null);
        showPage(subjectFormPage);
    }

    @FXML
    private void handleClearForumSearch() {
        if (forumSearchField != null) forumSearchField.clear();
        if (forumSortField != null) forumSortField.setValue("Par defaut");
        loadForumSubjects();
    }

    @FXML
    private void handleRefreshForum() {
        loadForumSubjects();
    }

    @FXML
    private void handleSubjectImageBrowse() {
        chooseSubjectImage();
    }

    @FXML
    private void handleSubjectImageClear() {
        clearSubjectImage();
    }

    @FXML
    private void handleSubjectSave() {
        saveSubject();
    }

    @FXML
    private void handleSubjectCancel() {
        resetSubjectForm();
        showPage(forumPage);
    }

    @FXML
    private void handleMessageAttachmentBrowse() {
        chooseMessageAttachment();
    }

    @FXML
    private void handleMessageAttachmentClear() {
        clearMessageAttachment();
    }

    @FXML
    private void handleMessageSend() {
        saveMessage();
    }

    @FXML
    private void handleMessageBack() {
        loadForumSubjects();
        showPage(forumPage);
    }

    @FXML
    private void handleMessageReplyCancel() {
        clearMessageReplyContext();
    }

    @FXML
    private void handleGoEvents() {
        loadEvents();
        showPage(eventsPage);
    }

    @FXML
    private void handleGoForum() {
        loadForumSubjects();
        showPage(forumPage);
    }

    private void attemptLogin() {
        String username = loginUsernameField != null && loginUsernameField.getText() != null
                ? loginUsernameField.getText().trim()
                : "";
        String password = loginPasswordField != null && loginPasswordField.getText() != null
                ? loginPasswordField.getText().trim()
                : "";

        // Clear previous error
        if (loginErrorLabel != null) {
            loginErrorLabel.setVisible(false);
            loginErrorLabel.setManaged(false);
        }

        if (username.isBlank() || password.isBlank()) {
            if (loginErrorLabel != null) {
                loginErrorLabel.setText("❌ Tous les champs sont obligatoires.");
                loginErrorLabel.setVisible(true);
                loginErrorLabel.setManaged(true);
            } else {
                showWarning("Tous les champs sont obligatoires.");
            }
            return;
        }

        try {
            User user = authService.login(username, password);
            if (user == null) {
                if (loginErrorLabel != null) {
                    loginErrorLabel.setText("❌ Nom d'utilisateur ou mot de passe incorrect.");
                    loginErrorLabel.setVisible(true);
                    loginErrorLabel.setManaged(true);
                } else {
                    showError("Connexion refusee", "Identifiants invalides.");
                }
                return;
            }

            currentUser = user;
            applyRole();
            resetForm();
            loadEvents();
            loadReservations();
            loadForumSubjects();
            updateHomePageStats();  // Update stats on home page

            // Clear error on success
            if (loginErrorLabel != null) {
                loginErrorLabel.setVisible(false);
                loginErrorLabel.setManaged(false);
            }

            // Redirect to HOME PAGE instead of formPage or eventsPage
            showPage(homePage);

        } catch (SQLException e) {
            String errorMsg = e.getMessage();
            if (loginErrorLabel != null) {
                if (errorMsg.contains("requis")) {
                    loginErrorLabel.setText("❌ Tous les champs sont obligatoires.");
                } else if (errorMsg.contains("incorrect")) {
                    loginErrorLabel.setText("❌ Nom d'utilisateur ou mot de passe incorrect.");
                } else if (errorMsg.contains("banni")) {
                    loginErrorLabel.setText("❌ Votre compte est banni. Contactez l'administrateur.");
                } else {
                    loginErrorLabel.setText("❌ Erreur: " + errorMsg);
                }
                loginErrorLabel.setVisible(true);
                loginErrorLabel.setManaged(true);
            } else {
                showError("Connexion impossible", errorMsg);
            }
        }
    }
    private void attemptRegister() {
        String username = registerUsernameField != null ? registerUsernameField.getText().trim() : "";
        String password = registerPasswordField != null ? registerPasswordField.getText() : "";
        String confirm = registerConfirmPasswordField != null ? registerConfirmPasswordField.getText() : "";

        if (registerErrorLabel != null) {
            registerErrorLabel.setVisible(false);
            registerErrorLabel.setManaged(false);
        }

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            setRegisterError("❌ Tous les champs sont obligatoires.");
            return;
        }

        if (password.length() < 6) {
            setRegisterError("❌ Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }

        if (!password.equals(confirm)) {
            setRegisterError("❌ Les mots de passe ne correspondent pas.");
            return;
        }

        try {
            authService.register(username, password, "ETUDIANT");
            showInfo("Inscription réussie", "Votre compte a été créé avec succès.");
            showLoginPage();
        } catch (SQLException ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("existe déjà")) {
                setRegisterError("❌ Ce nom d'utilisateur existe déjà.");
            } else {
                setRegisterError("❌ Erreur: " + msg);
            }
        }
    }

    private void setRegisterError(String message) {
        if (registerErrorLabel == null) return;
        registerErrorLabel.setText(message);
        registerErrorLabel.setVisible(true);
        registerErrorLabel.setManaged(true);
    }

    private void applyRole() {
        boolean admin = currentUser != null && currentUser.isAdmin();
        userBadge.setText(currentUser.getUsername() + " | " + currentUser.getRole());
        userBadge.setVisible(true); userBadge.setManaged(true);
        logoutButton.setVisible(true); logoutButton.setManaged(true);
        eventsHeaderButton.setVisible(true); eventsHeaderButton.setManaged(true);
        addHeaderButton.setVisible(admin); addHeaderButton.setManaged(admin);
        reservationsHeaderButton.setVisible(admin); reservationsHeaderButton.setManaged(admin);
        forumHeaderButton.setVisible(true); forumHeaderButton.setManaged(true);
        statsHeaderButton.setVisible(admin); statsHeaderButton.setManaged(admin);
        eventsTitle.setText(admin ? "Back office des evenements" : "Catalogue des evenements");
        eventsSubtitle.setText(admin ? "Admin peut modifier, supprimer et suivre les reservations." : "L'etudiant consulte les evenements avec photo et peut reserver.");
    }

    private void showLoginPage() {
        if (loginUsernameField != null) loginUsernameField.clear();
        if (loginPasswordField != null) loginPasswordField.clear();
        if (loginErrorLabel != null) {
            loginErrorLabel.setVisible(false);
            loginErrorLabel.setManaged(false);
        }
        if (registerErrorLabel != null) {
            registerErrorLabel.setVisible(false);
            registerErrorLabel.setManaged(false);
        }
        for (Node n : List.of(userBadge, addHeaderButton, eventsHeaderButton, reservationsHeaderButton, forumHeaderButton, statsHeaderButton, logoutButton)) {
            if (n != null) {
                n.setVisible(false);
                n.setManaged(false);
            }
        }
        showPage(loginPage);
    }

    private void showRegisterPage() {
        if (registerErrorLabel != null) {
            registerErrorLabel.setVisible(false);
            registerErrorLabel.setManaged(false);
        }
        if (registerUsernameField != null) registerUsernameField.clear();
        if (registerPasswordField != null) registerPasswordField.clear();
        if (registerConfirmPasswordField != null) registerConfirmPasswordField.clear();
        showPage(registerPage);
    }

    private void logout() {
        // Clear current user
        currentUser = null;

        // Clear editing references
        editingEvent = null;
        editingSubject = null;
        currentSubject = null;

        // Clear data collections
        reservedEventIds.clear();
        reservationCounts.clear();

        // Clear form fields
        if (loginUsernameField != null) loginUsernameField.clear();
        if (loginPasswordField != null) loginPasswordField.clear();
        if (registerUsernameField != null) registerUsernameField.clear();
        if (registerPasswordField != null) registerPasswordField.clear();
        if (registerConfirmPasswordField != null) registerConfirmPasswordField.clear();

        // Clear error labels
        if (loginErrorLabel != null) {
            loginErrorLabel.setVisible(false);
            loginErrorLabel.setManaged(false);
        }
        if (registerErrorLabel != null) {
            registerErrorLabel.setVisible(false);
            registerErrorLabel.setManaged(false);
        }

        // Hide all header buttons
        for (Node n : List.of(userBadge, addHeaderButton, eventsHeaderButton,
                reservationsHeaderButton, forumHeaderButton,
                statsHeaderButton, logoutButton)) {
            if (n != null) {
                n.setVisible(false);
                n.setManaged(false);
            }
        }

        // Clear the page container and show login page
        if (pageContainer != null) {
            pageContainer.getChildren().clear();
            pageContainer.getChildren().setAll(loginPage);
        }

        // Force refresh of login page
        showLoginPage();
    }

    private void saveEvent() {
        if (currentUser == null || !currentUser.isAdmin()) { showError("Acces refuse", "Seul l'admin peut ajouter ou modifier."); return; }
        Event event = buildEventFromForm(); if (event == null) return;
        try {
            if (editingEvent == null) { eventService.addEvent(event); showInfo("Ajout reussi", "L'evenement a ete ajoute."); }
            else { event.setId(editingEvent.getId()); eventService.updateEvent(event); showInfo("Modification reussie", "L'evenement a ete mis a jour."); }
            resetForm(); loadEvents(); showPage(eventsPage);
        } catch (SQLException e) { showError("Enregistrement impossible", e.getMessage()); }
    }

    private Event buildEventFromForm() {
        if (titleField.getText().isBlank() || descriptionArea.getText().isBlank() || locationField.getText().isBlank() || datePicker.getValue() == null) { showWarning("Tous les champs obligatoires doivent etre remplis."); return null; }
        LocalTime time; int cap;
        try { time = LocalTime.parse(timeField.getText().trim()); } catch (Exception e) { showWarning("L'heure doit etre au format HH:mm."); return null; }
        try { cap = Integer.parseInt(capacityField.getText().trim()); } catch (Exception e) { showWarning("La capacite doit etre un entier."); return null; }
        if (cap <= 0) { showWarning("La capacite doit etre superieure a zero."); return null; }
        return new Event(titleField.getText().trim(), descriptionArea.getText().trim(), LocalDateTime.of(datePicker.getValue(), time), locationField.getText().trim(), cap, categoryField.getValue(), imageField.getText().trim(), null);
    }

    private void loadEvents() {
        try {
            reservationCounts.clear(); reservationCounts.putAll(reservationService.getReservationCountsByEvent());
            reservedEventIds.clear();
            if (currentUser != null && !currentUser.isAdmin()) reservedEventIds.addAll(reservationService.getReservedEventIdsByUser(currentUser.getId()));
            String query = searchField == null ? null : searchField.getText().trim();
            String sortBy = sortField == null ? null : sortField.getValue();
            eventListView.getItems().setAll(eventService.getEvents(query, sortBy));
        } catch (SQLException e) { eventListView.getItems().clear(); showError("Chargement impossible", e.getMessage()); }
    }

    private void loadReservations() {
        try { reservationListView.getItems().setAll(reservationService.getAllReservations()); }
        catch (SQLException e) { reservationListView.getItems().clear(); showError("Chargement des reservations impossible", e.getMessage()); }
    }

    private void loadForumSubjects() {
        try {
            String query = forumSearchField == null ? null : forumSearchField.getText().trim();
            String sortBy = forumSortField == null ? null : forumSortField.getValue();
            Integer userId = currentUser == null ? null : currentUser.getId();
            forumListView.getItems().setAll(forumService.getSubjects(query, sortBy, userId));
            clearInlineError(forumErrorLabel);
        } catch (SQLException e) {
            forumListView.getItems().clear();
            setInlineError(forumErrorLabel, "Chargement du forum impossible: " + e.getMessage());
        }
    }

    private void loadMessages() {
        if (currentSubject == null) {
            messageIndexById.clear();
            messageListView.getItems().clear();
            clearInlineError(messagesErrorLabel);
            return;
        }
        try {
            Integer userId = currentUser == null ? null : currentUser.getId();
            List<ForumMessage> rawMessages = forumMessageService.getMessagesBySubject(currentSubject.getId(), userId);
            messageIndexById.clear();
            for (ForumMessage message : rawMessages) {
                if (message.getId() > 0) {
                    messageIndexById.put(message.getId(), message);
                }
            }
            messageListView.getItems().setAll(buildThreadedMessages(rawMessages));
            clearInlineError(messagesErrorLabel);
        } catch (SQLException e) {
            messageIndexById.clear();
            messageListView.getItems().clear();
            setInlineError(messagesErrorLabel, "Chargement des commentaires impossible: " + e.getMessage());
        }
    }

    private List<ForumMessage> buildThreadedMessages(List<ForumMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        Comparator<ForumMessage> byDateThenId = Comparator
                .comparing((ForumMessage m) -> m.getDateMessage() == null ? LocalDateTime.MIN : m.getDateMessage())
                .thenComparingInt(ForumMessage::getId);

        Set<Integer> knownIds = new HashSet<>();
        for (ForumMessage message : messages) {
            if (message.getId() > 0) {
                knownIds.add(message.getId());
            }
            message.setThreadLevel(1);
        }

        Map<Integer, List<ForumMessage>> childrenByParent = new HashMap<>();
        List<ForumMessage> roots = new ArrayList<>();
        for (ForumMessage message : messages) {
            Integer parentId = message.getParentMessageId();
            if (parentId == null || parentId <= 0 || !knownIds.contains(parentId)) {
                roots.add(message);
            } else {
                childrenByParent.computeIfAbsent(parentId, key -> new ArrayList<>()).add(message);
            }
        }

        roots.sort(byDateThenId);
        for (List<ForumMessage> children : childrenByParent.values()) {
            children.sort(byDateThenId);
        }

        List<ForumMessage> flattened = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        for (ForumMessage root : roots) {
            appendThreadedMessage(root, 1, childrenByParent, flattened, visited);
        }

        // Fallback to keep all rows visible even if malformed parent chains exist.
        for (ForumMessage message : messages) {
            if (!visited.contains(message.getId())) {
                appendThreadedMessage(message, 1, childrenByParent, flattened, visited);
            }
        }

        return flattened;
    }

    private void appendThreadedMessage(ForumMessage message,
                                       int level,
                                       Map<Integer, List<ForumMessage>> childrenByParent,
                                       List<ForumMessage> flattened,
                                       Set<Integer> visited) {
        if (message == null || message.getId() <= 0 || !visited.add(message.getId())) {
            return;
        }

        message.setThreadLevel(level);
        flattened.add(message);

        List<ForumMessage> children = childrenByParent.get(message.getId());
        if (children == null || children.isEmpty()) {
            return;
        }

        for (ForumMessage child : children) {
            appendThreadedMessage(child, level + 1, childrenByParent, flattened, visited);
        }
    }

    private void showSubjectForm(ForumSubject subject) {
        clearInlineError(subjectErrorLabel);
        editingSubject = subject;
        boolean editing = subject != null;
        subjectSaveButton.setText(editing ? "Enregistrer" : "Publier");
        if (subjectFormTitle != null) {
            subjectFormTitle.setText(editing ? "Modifier sujet #" + subject.getId() : "Nouveau sujet");
        }
        subjectCancelButton.setVisible(true);
        subjectCancelButton.setManaged(true);

        if (!editing) {
            resetSubjectForm();
            return;
        }

        subjectTitleField.setText(subject.getTitre());
        subjectDescriptionArea.setText(subject.getDescription() == null ? "" : subject.getDescription());
        subjectImageUrlField.setText(subject.getImageUrl() == null ? "" : subject.getImageUrl());
        updateImagePreview(subjectImageUrlField.getText(), subjectImagePreview, subjectImageMeta);
        subjectPinnedCheck.setSelected(subject.isPinned());
        subjectAnonymousCheck.setSelected(subject.isAnonymous());
        selectStatusButton(subject.getStatus());

        applySubjectPermissions();
    }

    private void resetSubjectForm() {
        clearInlineError(subjectErrorLabel);
        if (subjectFormTitle != null) {
            subjectFormTitle.setText("Nouveau sujet");
        }
        subjectTitleField.clear();
        subjectDescriptionArea.clear();
        selectStatusButton(null);
        subjectImageUrlField.clear();
        clearSubjectImage();
        subjectPinnedCheck.setSelected(false);
        subjectAnonymousCheck.setSelected(false);
        applySubjectPermissions();
    }

    private void applySubjectPermissions() {
        boolean admin = currentUser != null && currentUser.isAdmin();
        subjectPinnedCheck.setDisable(!admin);
    }

    private void saveSubject() {
        if (currentUser == null) { setInlineError(subjectErrorLabel, "Connexion requise."); return; }
        String title = subjectTitleField.getText() == null ? "" : subjectTitleField.getText().trim();
        String description = subjectDescriptionArea.getText() == null ? "" : subjectDescriptionArea.getText().trim();
        if (title.isBlank()) { setInlineError(subjectErrorLabel, "Le titre du sujet est obligatoire."); return; }
        if (title.length() < 4) { setInlineError(subjectErrorLabel, "Le titre doit contenir au moins 4 caracteres."); return; }
        if (description.isBlank()) { setInlineError(subjectErrorLabel, "La description est obligatoire."); return; }
        if (description.length() < 11) { setInlineError(subjectErrorLabel, "La description doit contenir au moins 11 caracteres."); return; }
        String status = getSelectedStatusValue();
        if (status == null) { setInlineError(subjectErrorLabel, "Veuillez choisir un statut (ouvert, ferme ou archive)."); return; }

        if (editingSubject != null && !canEditSubject(editingSubject)) { setInlineError(subjectErrorLabel, "Vous ne pouvez pas modifier ce sujet."); return; }
        if (editingSubject == null) {
            try {
                for (ForumSubject existing : forumService.getAllSubjects()) {
                    String existingTitle = existing.getTitre() == null ? "" : existing.getTitre().trim();
                    String existingDesc = existing.getDescription() == null ? "" : existing.getDescription().trim();
                    if (existingTitle.equalsIgnoreCase(title) && existingDesc.equalsIgnoreCase(description)) {
                        setInlineError(subjectErrorLabel, "Un sujet identique existe deja.");
                        return;
                    }
                }
            } catch (SQLException e) {
                setInlineError(subjectErrorLabel, "Verification impossible: " + e.getMessage());
                return;
            }
        }

        ForumSubject subject = new ForumSubject();
        subject.setTitre(title);
        subject.setDescription(description);
        subject.setDateCreation(editingSubject == null ? LocalDateTime.now() : editingSubject.getDateCreation());
        subject.setImageUrl(subjectImageUrlField.getText().trim());
        subject.setPinned(subjectPinnedCheck.isSelected());
        subject.setAnonymous(subjectAnonymousCheck.isSelected());
        subject.setStatus(status);
        subject.setCategory(null);
        subject.setIdUser(currentUser.getId());

        try {
            if (editingSubject == null) {
                forumService.addSubject(subject);
                showInfo("Sujet publie", "Le sujet a ete ajoute.");
            } else {
                subject.setId(editingSubject.getId());
                forumService.updateSubject(subject);
                showInfo("Sujet modifie", "Le sujet a ete mis a jour.");
            }
            clearInlineError(subjectErrorLabel);
            resetSubjectForm();
            loadForumSubjects();
            showPage(forumPage);
        } catch (SQLException e) {
            setInlineError(subjectErrorLabel, "Enregistrement impossible: " + e.getMessage());
        }
    }

    private void selectStatusButton(String status) {
        String value = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        if (subjectStatusOpenButton == null || subjectStatusClosedButton == null || subjectStatusArchivedButton == null) {
            return;
        }
        if (value.equals("ferme")) {
            subjectStatusClosedButton.setSelected(true);
        } else if (value.equals("archive")) {
            subjectStatusArchivedButton.setSelected(true);
        } else {
            subjectStatusOpenButton.setSelected(true);
        }
        applyStatusButtonStyles();
    }

    private String getSelectedStatusValue() {
        if (subjectStatusOpenButton != null && subjectStatusOpenButton.isSelected()) return "ouvert";
        if (subjectStatusClosedButton != null && subjectStatusClosedButton.isSelected()) return "ferme";
        if (subjectStatusArchivedButton != null && subjectStatusArchivedButton.isSelected()) return "archive";
        return null;
    }

    private void deleteSubject(ForumSubject subject) {
        if (!canEditSubject(subject)) { setInlineError(forumErrorLabel, "Vous ne pouvez pas supprimer ce sujet."); return; }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Supprimer le sujet");
        alert.setContentText("Confirmer la suppression de " + subject.getTitre() + " ?");
        Optional<ButtonType> ok = alert.showAndWait();
        if (ok.isPresent() && ok.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            try {
                forumMessageService.deleteMessagesForSubject(subject.getId());
                forumService.deleteSubject(subject.getId());
                loadForumSubjects();
                showInfo("Suppression reussie", "Sujet supprime.");
            } catch (SQLException e) {
                setInlineError(forumErrorLabel, "Suppression impossible: " + e.getMessage());
            }
        }
    }

    private void openSubject(ForumSubject subject) {
        currentSubject = subject;
        messagesTitle.setText("Commentaires - " + subject.getTitre());
        resetMessageForm();
        loadMessages();
        clearInlineError(messagesErrorLabel);
        showPage(messagesPage);
    }

    private void saveMessage() {
        if (currentUser == null) { setInlineError(messagesErrorLabel, "Connexion requise."); return; }
        if (currentSubject == null) { setInlineError(messagesErrorLabel, "Veuillez selectionner un sujet."); return; }
        if (messageContentArea.getText().isBlank()) { setInlineError(messagesErrorLabel, "Le commentaire est obligatoire."); return; }
        if (replyingToMessage != null && replyingToMessage.getThreadLevel() >= MAX_MESSAGE_THREAD_LEVEL) {
            setInlineError(messagesErrorLabel, "Le niveau maximum de reponse (3) est atteint.");
            return;
        }

        ForumMessage message = new ForumMessage();
        message.setContenu(messageContentArea.getText().trim());
        message.setDateMessage(LocalDateTime.now());
        message.setAnonymous(messageAnonymousCheck.isSelected());
        message.setAttachmentPath(messageAttachmentPathField.getText().trim());
        message.setAttachmentMimeType(messageAttachmentMimeType);
        message.setAttachmentSize(messageAttachmentSize);
        message.setIdSujet(currentSubject.getId());
        message.setIdUser(currentUser.getId());
        message.setParentMessageId(replyingToMessage == null ? null : replyingToMessage.getId());

        try {
            forumMessageService.addMessage(message);
            resetMessageForm();
            loadMessages();
            clearInlineError(messagesErrorLabel);
        } catch (SQLException e) {
            setInlineError(messagesErrorLabel, "Commentaire impossible: " + e.getMessage());
        }
    }

    private void deleteMessage(ForumMessage message) {
        if (!canDeleteMessage(message)) { setInlineError(messagesErrorLabel, "Vous ne pouvez pas supprimer ce commentaire."); return; }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Supprimer le commentaire #" + message.getId());
        alert.setContentText("Confirmer la suppression du commentaire ?");
        Optional<ButtonType> ok = alert.showAndWait();
        if (ok.isPresent() && ok.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            try {
                forumMessageService.deleteMessage(message.getId());
                if (replyingToMessage != null && Objects.equals(replyingToMessage.getId(), message.getId())) {
                    clearMessageReplyContext();
                }
                loadMessages();
            } catch (SQLException e) {
                setInlineError(messagesErrorLabel, "Suppression impossible: " + e.getMessage());
            }
        }
    }

    private void resetMessageForm() {
        messageContentArea.clear();
        messageAnonymousCheck.setSelected(false);
        clearMessageAttachment();
        clearMessageReplyContext();
    }

    private boolean canEditSubject(ForumSubject subject) {
        if (currentUser == null) return false;
        return currentUser.isAdmin() || (subject.getIdUser() != null && subject.getIdUser() == currentUser.getId());
    }

    private boolean canDeleteMessage(ForumMessage message) {
        if (currentUser == null) return false;
        return currentUser.isAdmin() || message.getIdUser() == currentUser.getId();
    }

    private void reactToSubject(ForumSubject subject, boolean like) {
        if (subject == null) {
            return;
        }
        if (currentUser == null) {
            setInlineError(forumErrorLabel, "Connexion requise pour liker/disliker.");
            return;
        }
        try {
            forumService.reactToSubject(subject.getId(), currentUser.getId(), like);
            loadForumSubjects();
            clearInlineError(forumErrorLabel);
        } catch (SQLException e) {
            setInlineError(forumErrorLabel, "Reaction impossible: " + e.getMessage());
        }
    }

    private void reactToMessage(ForumMessage message, boolean like) {
        if (message == null) {
            return;
        }
        if (currentUser == null) {
            setInlineError(messagesErrorLabel, "Connexion requise pour liker/disliker.");
            return;
        }
        try {
            forumMessageService.reactToMessage(message.getId(), currentUser.getId(), like);
            loadMessages();
            clearInlineError(messagesErrorLabel);
        } catch (SQLException e) {
            setInlineError(messagesErrorLabel, "Reaction impossible: " + e.getMessage());
        }
    }

    private String reactionButtonStyle(Boolean userReactionLike, boolean buttonLike) {
        if (userReactionLike != null && userReactionLike == buttonLike) {
            return buttonLike ? REACTION_LIKE_ACTIVE : REACTION_DISLIKE_ACTIVE;
        }
        return REACTION_NEUTRAL;
    }

    private String displayMessageAuthor(ForumMessage message) {
        if (message == null) {
            return "Utilisateur";
        }
        if (message.isAnonymous()) {
            return "Anonyme";
        }
        if (message.getUsername() == null || message.getUsername().isBlank()) {
            return "Utilisateur #" + message.getIdUser();
        }
        return message.getUsername();
    }

    private ForumMessage findMessageById(Integer messageId) {
        if (messageId == null || messageId <= 0) {
            return null;
        }
        return messageIndexById.get(messageId);
    }

    private boolean canReplyToMessage(ForumMessage message) {
        return currentUser != null && message != null && message.getThreadLevel() < MAX_MESSAGE_THREAD_LEVEL;
    }

    private void startReplyToMessage(ForumMessage message) {
        if (message == null) {
            return;
        }
        if (!canReplyToMessage(message)) {
            setInlineError(messagesErrorLabel, "Vous ne pouvez pas repondre a ce niveau.");
            return;
        }

        replyingToMessage = message;
        String author = message.isAnonymous() ? "Anonyme" : (message.getUsername() == null ? "Utilisateur #" + message.getIdUser() : message.getUsername());
        String preview = message.getContenu() == null ? "" : message.getContenu().trim();
        if (preview.length() > 40) {
            preview = preview.substring(0, 40) + "...";
        }
        if (messageReplyInfoLabel != null) {
            messageReplyInfoLabel.setText("Reponse a " + author + " : " + preview);
            messageReplyInfoLabel.setVisible(true);
            messageReplyInfoLabel.setManaged(true);
        }
        if (messageCancelReplyButton != null) {
            messageCancelReplyButton.setVisible(true);
            messageCancelReplyButton.setManaged(true);
        }
        messageContentArea.requestFocus();
    }

    private void clearMessageReplyContext() {
        replyingToMessage = null;
        if (messageReplyInfoLabel != null) {
            messageReplyInfoLabel.setText("");
            messageReplyInfoLabel.setVisible(false);
            messageReplyInfoLabel.setManaged(false);
        }
        if (messageCancelReplyButton != null) {
            messageCancelReplyButton.setVisible(false);
            messageCancelReplyButton.setManaged(false);
        }
    }

    private void chooseMessageAttachment() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un fichier");
        File file = chooser.showOpenDialog(primaryStage);
        if (file != null) {
            messageAttachmentPathField.setText(file.getPath());
            messageAttachmentMimeType = detectMimeType(file);
            messageAttachmentSize = file.length();
            messageAttachmentMeta.setText(attachmentMeta(messageAttachmentMimeType, messageAttachmentSize));
        }
    }

    private void clearMessageAttachment() {
        messageAttachmentPathField.clear();
        messageAttachmentMimeType = null;
        messageAttachmentSize = null;
        messageAttachmentMeta.setText("Aucun fichier");
    }

    private String detectMimeType(File file) {
        try {
            String detected = Files.probeContentType(file.toPath());
            return detected == null ? "application/octet-stream" : detected;
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }

    private String attachmentMeta(String mimeType, Long size) {
        if (size == null) {
            return "Aucun fichier";
        }
        return (mimeType == null ? "application/octet-stream" : mimeType) + " | " + formatSize(size);
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " o";
        double kb = size / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f Ko", kb);
        double mb = kb / 1024.0;
        return String.format(Locale.US, "%.1f Mo", mb);
    }

    private VBox buildStatsPanel() throws SQLException {
        VBox statsBox = new VBox(20);
        statsBox.setPadding(new Insets(16));
        statsBox.setStyle("-fx-background-color:transparent;");

        int totalRes = reservationService.getTotalReservations();
        Label totalLabel = title("Total reservations: " + totalRes, 18);
        statsBox.getChildren().add(totalLabel);

        Map<String, Map<String, Integer>> categoryStats = reservationService.getStatsByCategory();
        if (!categoryStats.isEmpty()) {
            Label catTitle = title("Reservations par categorie", 16);
            statsBox.getChildren().add(catTitle);

            ObservableList<PieChart.Data> categoryData = FXCollections.observableArrayList();
            for (Map.Entry<String, Map<String, Integer>> entry : categoryStats.entrySet()) {
                int res = entry.getValue().get("reservations");
                if (res > 0) {
                    categoryData.add(new PieChart.Data(entry.getKey() + " (" + res + ")", res));
                }
            }

            PieChart categoryChart = new PieChart(categoryData);
            categoryChart.setLegendSide(Side.RIGHT);
            categoryChart.setStyle("-fx-font-size:12px;");
            categoryChart.setPrefSize(500, 300);
            categoryChart.setAnimated(false);
            categoryChart.setLabelsVisible(true);
            statsBox.getChildren().add(categoryChart);

            VBox legendBox = new VBox(8);
            legendBox.setStyle("-fx-border-color: rgba(207,227,255,0.5); -fx-border-radius:12; -fx-padding:12; -fx-background-color:rgba(244,249,255,0.5);");
            for (Map.Entry<String, Map<String, Integer>> entry : categoryStats.entrySet()) {
                int res = entry.getValue().get("reservations");
                int cap = entry.getValue().get("capacity");
                int taux = cap > 0 ? (int)(100.0 * res / cap) : 0;
                Label stat = new Label(entry.getKey() + ": " + res + " / " + cap + " places (" + taux + "%)");
                stat.setStyle("-fx-text-fill:#2a5fa3; -fx-font-size:12px; -fx-font-weight:700;");
                legendBox.getChildren().add(stat);
            }
            statsBox.getChildren().add(legendBox);
        }

        List<Map<String, Object>> eventStats = reservationService.getEventReservationStats();
        if (!eventStats.isEmpty()) {
            Label eventTitle = title("Remplissage par evenement", 16);
            statsBox.getChildren().add(eventTitle);
            VBox eventBox = new VBox(10);
            eventBox.setStyle("-fx-background-color:transparent;");

            for (Map<String, Object> row : eventStats) {
                String titre = (String) row.get("titre");
                int cap = (int) row.get("capacite");
                int res = (int) row.get("reservations");
                int taux = (int) row.get("taux");
                String categorie = (String) row.get("categorie");

                VBox eventCard = new VBox(8);
                eventCard.setStyle("-fx-border-color: rgba(207,227,255,0.5); -fx-border-radius:12; -fx-padding:12; -fx-background-color:rgba(244,249,255,0.5);");

                Label eventName = new Label(titre);
                eventName.setStyle("-fx-text-fill:#10233f; -fx-font-size:13px; -fx-font-weight:700;");
                eventCard.getChildren().add(eventName);

                ProgressBar progressBar = new ProgressBar((double) res / cap);
                progressBar.setPrefWidth(Double.MAX_VALUE);
                progressBar.setStyle("-fx-control-inner-background: linear-gradient(to right, #0f69ff, #38a4ff); -fx-padding:8;");
                eventCard.getChildren().add(progressBar);

                Label stats = new Label(res + " / " + cap + " (" + taux + "%) | " + (categorie == null ? "Non catégorisé" : categorie));
                stats.setStyle("-fx-text-fill:#415a78; -fx-font-size:11px;");
                eventCard.getChildren().add(stats);

                eventBox.getChildren().add(eventCard);
            }
            statsBox.getChildren().add(eventBox);
        }

        ScrollPane scroll = new ScrollPane(statsBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent; -fx-background:transparent;");
        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapper;
    }

    private void reserve(Event event) {
        if (currentUser == null || currentUser.isAdmin()) { showError("Acces refuse", "Seul un etudiant peut reserver."); return; }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Confirmer la reservation");
        dialog.setHeaderText("Reservation pour " + event.getTitre());

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(14);
        form.setPadding(new Insets(16));

        TextField nomField = new TextField();
        nomField.setPromptText("Nom");
        nomField.setStyle(INPUT);

        TextField prenomField = new TextField();
        prenomField.setPromptText("Prenom");
        prenomField.setStyle(INPUT);

        TextField telephoneField = new TextField();
        telephoneField.setPromptText("Telephone");
        telephoneField.setStyle(INPUT);

        TextField mailField = new TextField();
        mailField.setPromptText("Email");
        mailField.setStyle(INPUT);

        Label nomLabel = new Label("Nom");
        nomLabel.setStyle("-fx-text-fill:#29496f; -fx-font-size:13px; -fx-font-weight:700;");
        Label prenomLabel = new Label("Prenom");
        prenomLabel.setStyle("-fx-text-fill:#29496f; -fx-font-size:13px; -fx-font-weight:700;");
        Label telLabel = new Label("Telephone");
        telLabel.setStyle("-fx-text-fill:#29496f; -fx-font-size:13px; -fx-font-weight:700;");
        Label mailLabel = new Label("Email");
        mailLabel.setStyle("-fx-text-fill:#29496f; -fx-font-size:13px; -fx-font-weight:700;");

        form.add(nomLabel, 0, 0);
        form.add(nomField, 1, 0);
        form.add(prenomLabel, 0, 1);
        form.add(prenomField, 1, 1);
        form.add(telLabel, 0, 2);
        form.add(telephoneField, 1, 2);
        form.add(mailLabel, 0, 3);
        form.add(mailField, 1, 3);

        GridPane.setHgrow(nomField, Priority.ALWAYS);
        GridPane.setHgrow(prenomField, Priority.ALWAYS);
        GridPane.setHgrow(telephoneField, Priority.ALWAYS);
        GridPane.setHgrow(mailField, Priority.ALWAYS);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String nom = nomField.getText().trim();
            String prenom = prenomField.getText().trim();
            String telephone = telephoneField.getText().trim();
            String mail = mailField.getText().trim();

            if (nom.isBlank() || prenom.isBlank() || telephone.isBlank() || mail.isBlank()) {
                showWarning("Tous les champs sont obligatoires.");
                return;
            }

            try {
                reservationService.reserveEvent(event, currentUser.getId());
                showInfo("Reservation confirmee", "Reservation de " + prenom + " " + nom + " pour " + event.getTitre() + " validee.");
                loadEvents();
                loadReservations();
            }
            catch (SQLException e) { showError("Reservation impossible", e.getMessage()); }
        }
    }

    private void edit(Event event) {
        if (currentUser == null || !currentUser.isAdmin()) { showError("Acces refuse", "Seul l'admin peut modifier."); return; }
        editingEvent = event; formTitle.setText("Modifier l'evenement #" + event.getId()); saveButton.setText("Enregistrer");
        cancelEditButton.setVisible(true); cancelEditButton.setManaged(true);
        titleField.setText(event.getTitre()); descriptionArea.setText(event.getDescription()); locationField.setText(event.getLieu());
        datePicker.setValue(event.getDateEvent().toLocalDate()); timeField.setText(event.getDateEvent().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        capacityField.setText(String.valueOf(event.getCapacite())); categoryField.setValue(event.getCategorie() == null || event.getCategorie().isBlank() ? "yoga" : event.getCategorie());
        imageField.setText(event.getImage() == null ? "" : event.getImage());
        updateImagePreview(imageField.getText(), eventImagePreview, eventImageMeta);
        showPage(formPage);
    }

    private void delete(Event event) {
        if (currentUser == null || !currentUser.isAdmin()) { showError("Acces refuse", "Seul l'admin peut supprimer."); return; }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION); alert.setHeaderText("Supprimer l'evenement #" + event.getId()); alert.setContentText("Confirmer la suppression de " + event.getTitre() + " ?");
        Optional<ButtonType> ok = alert.showAndWait();
        if (ok.isPresent() && ok.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            try { reservationService.deleteReservationsForEvent(event.getId()); eventService.deleteEvent(event.getId()); loadEvents(); loadReservations(); }
            catch (SQLException e) { showError("Suppression impossible", e.getMessage()); }
        }
    }

    private void resetForm() {
        editingEvent = null; formTitle.setText("Nouvel evenement"); saveButton.setText("Ajouter l'evenement");
        cancelEditButton.setVisible(false); cancelEditButton.setManaged(false);
        titleField.clear(); descriptionArea.clear(); locationField.clear(); datePicker.setValue(LocalDate.now().plusDays(1));
        timeField.setText("10:00"); capacityField.setText("20"); categoryField.setValue("yoga"); imageField.clear();
        clearImage();
    }

    private void chooseImage() {
        FileChooser chooser = new FileChooser(); chooser.setTitle("Choisir une image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png"));
        File file = chooser.showOpenDialog(primaryStage);
        if (file != null) {
            imageField.setText(file.getPath());
            updateImagePreview(imageField.getText(), eventImagePreview, eventImageMeta);
        }
    }

    private void clearImage() {
        imageField.clear();
        updateImagePreview("", eventImagePreview, eventImageMeta);
    }

    private void chooseSubjectImage() {
        FileChooser chooser = new FileChooser(); chooser.setTitle("Choisir une image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png"));
        File file = chooser.showOpenDialog(primaryStage);
        if (file != null) {
            subjectImageUrlField.setText(file.getPath());
            updateImagePreview(subjectImageUrlField.getText(), subjectImagePreview, subjectImageMeta);
        }
    }

    private void clearSubjectImage() {
        subjectImageUrlField.clear();
        updateImagePreview("", subjectImagePreview, subjectImageMeta);
    }

    private void updateImagePreview(String path, ImageView preview, Label meta) {
        if (preview == null || meta == null) return;
        if (path == null || path.isBlank()) {
            preview.setImage(null);
            preview.setVisible(false);
            preview.setManaged(false);
            meta.setText("Aucune image");
            return;
        }

        File file = new File(path);
        if (!file.isAbsolute()) {
            file = new File(System.getProperty("user.dir"), path);
        }

        if (!file.exists()) {
            preview.setImage(null);
            preview.setVisible(false);
            preview.setManaged(false);
            meta.setText("Image introuvable");
            return;
        }

        preview.setImage(new Image(file.toURI().toString(), 260, 160, true, true));
        preview.setVisible(true);
        preview.setManaged(true);
        meta.setText(file.getName());
    }

    private void showPage(VBox page) { pageContainer.getChildren().setAll(page); }

    private Label small(String text) { Label l = new Label(text); l.setStyle("-fx-text-fill:#637a97; -fx-font-size:13px;"); return l; }
    private Label title(String text, int size) { Label l = new Label(text); l.setStyle("-fx-text-fill:#10233f; -fx-font-size:" + size + "px; -fx-font-weight:800;"); return l; }
    private Button button(String text, String style, javafx.event.EventHandler<javafx.event.ActionEvent> handler) { Button b = new Button(text); b.setStyle(style); b.setOnAction(handler); return b; }
    private void showWarning(String m) { alert(Alert.AlertType.WARNING, "Validation", m); }
    private void showError(String t, String m) { alert(Alert.AlertType.ERROR, t, m); }
    private void showInfo(String t, String m) { alert(Alert.AlertType.INFORMATION, t, m); }
    private void alert(Alert.AlertType type, String title, String message) { Alert a = new Alert(type); a.setHeaderText(title); a.setContentText(message); a.showAndWait(); }

    private void setInlineError(Label label, String message) {
        if (label == null) return;
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearInlineError(Label label) {
        if (label == null) return;
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    private Node imageNode(String path) {
        if (path != null && !path.isBlank()) {
            File file = new File(path); if (!file.isAbsolute()) file = new File(System.getProperty("user.dir"), path);
            if (file.exists()) {
                ImageView v = new ImageView(new Image(file.toURI().toString(), 180, 120, true, true));
                v.setFitWidth(180); v.setFitHeight(120); v.setPreserveRatio(false);
                StackPane box = new StackPane(v); box.setMinSize(180, 120); box.setMaxSize(180, 120); return box;
            }
        }
        Label l = new Label("Photo indisponible"); l.setStyle("-fx-text-fill:white; -fx-font-weight:700;");
        StackPane box = new StackPane(l); box.setMinSize(180, 120); box.setMaxSize(180, 120); box.setStyle("-fx-background-color: linear-gradient(to bottom right,#1b74ff,#73c4ff); -fx-background-radius:18;");
        return box;
    }

    private class EventCell extends ListCell<Event> {
        @Override protected void updateItem(Event event, boolean empty) {
            super.updateItem(event, empty);
            if (empty || event == null) { setGraphic(null); setText(null); return; }
            int reserved = reservationCounts.getOrDefault(event.getId(), 0);
            int remaining = Math.max(event.getCapacite() - reserved, 0);
            boolean alreadyReserved = reservedEventIds.contains(event.getId());
            boolean full = reserved >= event.getCapacite();
            Label title = new Label(event.getTitre()); title.setStyle("-fx-text-fill:#10233f; -fx-font-size:18px; -fx-font-weight:800;");
            Label meta = small(event.getDateEvent().format(EVENT_FMT) + "  |  " + event.getLieu());
            Label desc = new Label(event.getDescription()); desc.setWrapText(true); desc.setStyle("-fx-text-fill:#415a78; -fx-font-size:13px;");
            String status = event.getDateEvent().isBefore(LocalDateTime.now()) ? "Terminé" : (full ? "Complet" : "Ouvert");
            Label stats = new Label("Categorie: " + event.getCategorie() + "   |   " + reserved + "/" + event.getCapacite() + " reserves   |   " + remaining + " places restantes");
            stats.setStyle("-fx-text-fill:#2a5fa3; -fx-font-size:12px; -fx-font-weight:700;");
            Label statusLabel = small("Statut: " + status);
            statusLabel.setStyle("-fx-text-fill:#0f69ff; -fx-font-size:12px; -fx-font-weight:700;");
            VBox text = new VBox(10, title, meta, desc, stats, statusLabel);
            Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
            VBox actions = new VBox(10); actions.setAlignment(Pos.CENTER_RIGHT);
            if (currentUser != null && currentUser.isAdmin()) {
                actions.getChildren().addAll(button("Modifier", SECONDARY, e -> edit(event)), button("Supprimer", DANGER, e -> delete(event)), button("Voir reservations", SECONDARY, e -> { loadReservations(); showPage(reservationsPage); }));
            } else {
                Button reserve = button(alreadyReserved ? "Deja reserve" : (full ? "Complet" : "Reserver"), alreadyReserved || full ? SECONDARY : PRIMARY, e -> reserve(event));
                reserve.setDisable(alreadyReserved || full);
                actions.getChildren().addAll(reserve, small(alreadyReserved ? "Votre reservation est enregistree." : "Reservation ouverte."));
            }
            HBox row = new HBox(18, imageNode(event.getImage()), text, spacer, actions);
            row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(18));
            row.setStyle("-fx-background-color: linear-gradient(to right, rgba(255,255,255,0.98), rgba(244,249,255,0.95)); -fx-background-radius:22; -fx-border-radius:22; -fx-border-color: rgba(138,182,238,0.24);");
            setGraphic(row);
        }
    }

    private class ReservationCell extends ListCell<ReservationRecord> {
        @Override protected void updateItem(ReservationRecord r, boolean empty) {
            super.updateItem(r, empty);
            if (empty || r == null) { setGraphic(null); setText(null); return; }
            VBox box = new VBox(8,
                    title(r.getEventTitle() == null ? "Evenement supprime" : r.getEventTitle(), 16),
                    new Label("Etudiant: " + (r.getUsername() == null ? "inconnu" : r.getUsername())),
                    small("Reserve le " + r.getReservedAt().format(RES_FMT) + "  |  Event #" + r.getEventId()));
            ((Label) box.getChildren().get(1)).setStyle("-fx-text-fill:#2a5fa3; -fx-font-size:13px; -fx-font-weight:700;");
            box.setPadding(new Insets(18));
            box.setStyle("-fx-background-color: linear-gradient(to right, rgba(255,255,255,0.98), rgba(244,249,255,0.95)); -fx-background-radius:22; -fx-border-radius:22; -fx-border-color: rgba(138,182,238,0.24);");
            setGraphic(box);
        }
    }

    private class ForumSubjectCell extends ListCell<ForumSubject> {
        @Override protected void updateItem(ForumSubject subject, boolean empty) {
            super.updateItem(subject, empty);
            if (empty || subject == null) { setGraphic(null); setText(null); return; }

            String author = subject.isAnonymous() ? "Anonyme" : (subject.getUsername() == null ? "Utilisateur #" + subject.getIdUser() : subject.getUsername());
            String dateText = subject.getDateCreation() == null ? "" : subject.getDateCreation().format(EVENT_FMT);
            String pinned = subject.isPinned() ? "Epingle" : "Normal";
            int successScore = subject.getLikeCount() + subject.getMessageCount() - subject.getDislikeCount();

            Label title = new Label(subject.getTitre()); title.setStyle("-fx-text-fill:#10233f; -fx-font-size:18px; -fx-font-weight:800;");
            Label successBadge = new Label("Succes");
            successBadge.setStyle("-fx-text-fill:#0f69ff; -fx-background-color:rgba(15,105,255,0.12); -fx-background-radius:999; -fx-padding:3 10 3 10; -fx-font-size:11px; -fx-font-weight:800;");
            successBadge.setVisible(successScore >= POPULAR_SUBJECT_SCORE_THRESHOLD);
            successBadge.setManaged(successScore >= POPULAR_SUBJECT_SCORE_THRESHOLD);
            HBox titleRow = new HBox(8, title, successBadge);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            Label meta = small("Auteur: " + author + "  |  " + dateText + "  |  " + pinned);
            Label desc = new Label(subject.getDescription() == null ? "" : subject.getDescription());
            desc.setWrapText(true); desc.setStyle("-fx-text-fill:#415a78; -fx-font-size:13px;");
            Label tags = new Label("Categorie: " + safeText(subject.getCategory()) + "   |   Statut: " + safeText(subject.getStatus()));
            tags.setStyle("-fx-text-fill:#2a5fa3; -fx-font-size:12px; -fx-font-weight:700;");

            Button like = button("Like (" + subject.getLikeCount() + ")", reactionButtonStyle(subject.getUserReactionLike(), true), e -> reactToSubject(subject, true));
            Button dislike = button("Dislike (" + subject.getDislikeCount() + ")", reactionButtonStyle(subject.getUserReactionLike(), false), e -> reactToSubject(subject, false));
            HBox reactions = new HBox(8, like, dislike);
            reactions.setAlignment(Pos.CENTER_LEFT);
            Label reactionHint = small("Cliquez une deuxieme fois pour retirer votre reaction.");
            reactionHint.setStyle("-fx-text-fill:#6b819d; -fx-font-size:11px;");

            VBox text = new VBox(10, titleRow, meta, desc, tags, reactions, reactionHint);
            Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

            VBox actions = new VBox(10); actions.setAlignment(Pos.CENTER_RIGHT);
            actions.getChildren().add(button("Ouvrir", SECONDARY, e -> openSubject(subject)));
            if (canEditSubject(subject)) {
                actions.getChildren().addAll(
                        button("Modifier", SECONDARY, e -> { showSubjectForm(subject); showPage(subjectFormPage); }),
                        button("Supprimer", DANGER, e -> deleteSubject(subject))
                );
            }

            HBox row = new HBox(18, imageNode(subject.getImageUrl()), text, spacer, actions);
            row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(18));
            row.setStyle("-fx-background-color: linear-gradient(to right, rgba(255,255,255,0.98), rgba(244,249,255,0.95)); -fx-background-radius:22; -fx-border-radius:22; -fx-border-color: rgba(138,182,238,0.24);");
            setGraphic(row);
        }
    }

    private class ForumMessageCell extends ListCell<ForumMessage> {
        @Override protected void updateItem(ForumMessage message, boolean empty) {
            super.updateItem(message, empty);
            if (empty || message == null) { setGraphic(null); setText(null); return; }

            String author = displayMessageAuthor(message);
            String dateText = message.getDateMessage() == null ? "" : message.getDateMessage().format(RES_FMT);
            int level = Math.max(1, Math.min(MAX_MESSAGE_THREAD_LEVEL, message.getThreadLevel()));
            String levelText = level == 1 ? "Commentaire" : "Reponse N" + level;

            Label header = title(author, 14);
            Label levelBadge = new Label(levelText);
            levelBadge.setStyle(level == 1
                    ? "-fx-text-fill:#1d4f92; -fx-background-color:rgba(29,79,146,0.10); -fx-background-radius:999; -fx-padding:3 10 3 10; -fx-font-size:11px; -fx-font-weight:700;"
                    : "-fx-text-fill:#2a6dc0; -fx-background-color:rgba(15,105,255,0.12); -fx-background-radius:999; -fx-padding:3 10 3 10; -fx-font-size:11px; -fx-font-weight:700;");
            HBox headerRow = new HBox(8, header, levelBadge);
            headerRow.setAlignment(Pos.CENTER_LEFT);

            Label meta = small(dateText);
            Label content = new Label(message.getContenu());
            content.setWrapText(true); content.setStyle("-fx-text-fill:#415a78; -fx-font-size:13px;");

            VBox box = new VBox(8, headerRow, meta);
            box.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(box, Priority.ALWAYS);

            if (level > 1) {
                ForumMessage parent = findMessageById(message.getParentMessageId());
                String parentAuthor = parent == null
                        ? (message.getParentMessageId() == null ? "Commentaire parent" : "Commentaire #" + message.getParentMessageId())
                        : displayMessageAuthor(parent);
                Label relation = new Label("Reponse a " + parentAuthor);
                relation.setStyle("-fx-text-fill:#2f6db3; -fx-font-size:12px; -fx-font-weight:700;");
                box.getChildren().add(relation);
            }

            box.getChildren().add(content);
            if (message.getAttachmentPath() != null && !message.getAttachmentPath().isBlank()) {
                Label att = small("Fichier: " + message.getAttachmentPath());
                box.getChildren().add(att);
            }

            Button like = button("Like (" + message.getLikeCount() + ")", reactionButtonStyle(message.getUserReactionLike(), true), e -> reactToMessage(message, true));
            Button dislike = button("Dislike (" + message.getDislikeCount() + ")", reactionButtonStyle(message.getUserReactionLike(), false), e -> reactToMessage(message, false));
            HBox reactions = new HBox(8, like, dislike);
            reactions.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(reactions);

            Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
            Button reply = button("Repondre", SECONDARY, e -> startReplyToMessage(message));
            reply.setDisable(!canReplyToMessage(message));
            Button del = button("Supprimer", DANGER, e -> deleteMessage(message));
            del.setDisable(!canDeleteMessage(message));
            VBox actions = new VBox(8, reply, del);
            actions.setAlignment(Pos.TOP_RIGHT);

            HBox card = new HBox(12, box, spacer, actions);
            card.setAlignment(Pos.TOP_LEFT);
            card.setPadding(new Insets(14));
            card.setStyle(level == 1
                    ? "-fx-background-color: linear-gradient(to right, rgba(255,255,255,0.99), rgba(245,250,255,0.98)); -fx-background-radius:18; -fx-border-radius:18; -fx-border-width:1; -fx-border-color: rgba(138,182,238,0.28);"
                    : "-fx-background-color: rgba(248,252,255,0.98); -fx-background-radius:16; -fx-border-radius:16; -fx-border-width:1 1 1 4; -fx-border-color: rgba(138,182,238,0.25) rgba(138,182,238,0.25) rgba(138,182,238,0.25) #5b95e9;");

            Region indent = new Region();
            double indentWidth = (level - 1) * 34;
            indent.setMinWidth(indentWidth);
            indent.setPrefWidth(indentWidth);

            HBox row = new HBox(0);
            row.setAlignment(Pos.TOP_LEFT);
            if (level > 1) {
                row.getChildren().add(indent);
            }
            row.getChildren().add(card);
            HBox.setHgrow(card, Priority.ALWAYS);

            VBox wrapper = new VBox(row);
            wrapper.setPadding(new Insets(0, 0, 8, 0));
            setGraphic(wrapper);
        }
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "Non defini" : value;
    }

    public static void main(String[] args) { launch(args); }
}
