package org.example.view;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.model.User;
import org.example.model.Event;
import org.example.model.ForumMessage;
import org.example.model.ForumRewriteSuggestion;
import org.example.model.ForumSubject;
import org.example.model.ReservationRecord;
import org.example.controller.AuthController;
import org.example.controller.EventController;
import org.example.controller.ForumMessageController;
import org.example.controller.ForumController;
import org.example.controller.ReservationController;
import org.example.controller.ResourceCatalogController;
import org.example.controller.ResourceListController;
import org.example.controller.UserController;
import org.example.service.ForumAiRewriteService;
import org.example.service.MentionNotificationResult;
import org.example.service.MentionNotificationService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.github.sarxos.webcam.Webcam;
import org.example.service.CompreFaceClient;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javafx.application.Platform;
import javafx.concurrent.Task;
import java.util.function.Consumer;
import javafx.embed.swing.SwingFXUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.util.Duration;
import javafx.scene.Cursor;
import java.util.concurrent.CompletionException;

public class Main extends Application {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
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
    private final UserController userController = new UserController();
    private final CompreFaceClient compreFaceClient = new CompreFaceClient();

    private final Map<Integer, Integer> reservationCounts = new HashMap<>();
    private final Set<Integer> reservedEventIds = new HashSet<>();
    private VBox homePage;
    private VBox adminDashboardStatsPage;
    private VBox adminDashboardPage;
    private VBox adminUsersListPage;
    private VBox adminUserFormPage;
    private final ForumAiRewriteService forumAiRewriteService = new ForumAiRewriteService();
    private final MentionNotificationService mentionNotificationService = new MentionNotificationService();
    private final Map<Integer, ForumMessage> messageIndexById = new HashMap<>();
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
    private User editingAdminUser;
    @FXML private StackPane adminContentPane;
    @FXML private Button adminDashboardNavButton;
    @FXML private Button adminUsersNavButton;
    @FXML private Button adminAddEventNavButton;
    @FXML private Button adminEventsNavButton;
    @FXML private Button adminReservationsNavButton;
    @FXML private Button adminForumNavButton;
    @FXML private Button adminResourcesNavButton;
    @FXML private Button adminStatsNavButton;
    private final ObservableList<User> adminUsersMasterList = FXCollections.observableArrayList();
    private FilteredList<User> adminUsersFilteredList;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> usersIdCol;
    @FXML private TableColumn<User, String> usersUsernameCol;
    @FXML private TableColumn<User, String> usersEmailCol;
    @FXML private TableColumn<User, String> usersRoleCol;
    @FXML private TableColumn<User, String> usersStatusCol;
    @FXML private TableColumn<User, Void> usersActionsCol;
    @FXML private Label adminUsersListErrorLabel;
    @FXML private Button adminUsersRefreshButton;
    @FXML private Button adminUsersAddButton;
    @FXML private TextField adminUsersSearchField;
    @FXML private Button adminUsersClearSearchButton;
    @FXML private Label adminUserFormTitle;
    @FXML private Label adminUserFormErrorLabel;
    @FXML private TextField adminUsernameField;
    @FXML private TextField adminEmailField;
    @FXML private TextField adminFirstNameField;
    @FXML private TextField adminLastNameField;
    @FXML private PasswordField adminPasswordField;
    @FXML private ComboBox<String> adminRoleField;
    @FXML private DatePicker adminBannedUntilField;
    @FXML private Button adminSaveUserButton;
    @FXML private Button adminResetUserButton;
    @FXML private Button adminBackToListButton;

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
    @FXML private TextField loginEmailField;
    @FXML private Label loginErrorLabel;
    @FXML private TextField registerFirstNameField;
    @FXML private TextField registerLastNameField;
    @FXML private Button confirmAccountButton;
    @FXML private Button forgotPasswordButton;
    private MenuButton accountMenuButton;
    @FXML private TextField loginUsernameField;
    @FXML private PasswordField loginPasswordField;
    @FXML private TextField registerUsernameField;
    @FXML private TextField registerEmailField;
    @FXML private PasswordField registerPasswordField;
    @FXML private PasswordField registerConfirmPasswordField;
    @FXML private Label registerErrorLabel;
    @FXML private Label userBadge;
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
    private Button resourcesHeaderButton;
    private Button statsHeaderButton;
    private MenuItem accountEditProfileItem;
    private MenuItem accountAdminDashboardItem;
    private MenuItem accountLogoutItem;
    @FXML private ListView<Event> eventListView;
    @FXML private ListView<ReservationRecord> reservationListView;
    @FXML private ListView<ForumSubject> forumListView;
    @FXML private ListView<ForumMessage> messageListView;
    @FXML private TextField forumSearchField;
    @FXML private ComboBox<String> forumSortField;
    @FXML private Label forumTitle;
    @FXML private Label forumErrorLabel;
    @FXML private Label forumSubtitle;
    @FXML private Label adminStatsTotalUsersValue;
    @FXML private Label adminStatsAdminsValue;
    @FXML private Label adminStatsPsychologuesValue;
    @FXML private Label adminStatsClientsValue;
    @FXML private Label adminStatsEtudiantsValue;
    @FXML private Label adminStatsVerifiedValue;
    @FXML private Label adminStatsFaceIdValue;
    @FXML private Label adminStatsBannedValue;
    @FXML private Button adminStatsRefreshButton;
    @FXML private Button adminStatsManageUsersButton;

    @FXML private PieChart adminStatsRoleChart;
    @FXML private LineChart<String, Number> adminStatsTrendChart;
    @FXML private Label adminStatsLastUpdatedLabel;

    @FXML private Button forumPrevButton;
    @FXML private Button forumNextButton;
    @FXML private Label forumPageLabel;
    @FXML private Label messagesTitle;
    @FXML private Label messagesErrorLabel;
    @FXML private Button messagePrevButton;
    @FXML private Button messageNextButton;
    @FXML private Label messagePageLabel;
    @FXML private Label subjectFormTitle;
    @FXML private Label subjectErrorLabel;
    @FXML private VBox statsBox;

    private ForumSubject editingSubject;
    private ForumSubject currentSubject;
    private ForumMessage replyingToMessage;

    // Pagination variables
    private static final int SUBJECTS_PER_PAGE = 4;
    private static final int MESSAGES_PER_PAGE = 6;
    private List<ForumSubject> allForumSubjects = new ArrayList<>();
    private int currentForumPage = 1;
    private List<ForumMessage> allForumMessages = new ArrayList<>();
    private int currentMessagesPage = 1;

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
    @FXML private Button subjectAiRewriteButton;
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
        homePage = buildHomePage();
        adminDashboardStatsPage = buildAdminDashboardStatsPage();
        adminDashboardPage = buildAdminDashboardPage();
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
        resourcesHeaderButton = button("Ressources", SECONDARY, e -> openResourcesInMainWindow());
        statsHeaderButton = button("Statistiques", SECONDARY, e -> { try { statsPage = buildStatsPageGlobal(); showPage(statsPage); } catch (SQLException ex) { showError("Erreur", ex.getMessage()); } });
        accountMenuButton = new MenuButton();
        accountMenuButton.setStyle(SECONDARY);
        Label accountIcon = new Label("👤");
        accountIcon.setStyle("-fx-font-size:13px;");
        accountMenuButton.setGraphic(accountIcon);
        accountEditProfileItem = new MenuItem("Edit profile");
        accountAdminDashboardItem = new MenuItem("Admin Dashboard");
        accountLogoutItem = new MenuItem("Deconnexion");
        accountEditProfileItem.setOnAction(e -> openEditProfileDialog());
        accountAdminDashboardItem.setOnAction(e -> showPage(adminDashboardStatsPage != null ? adminDashboardStatsPage : adminDashboardPage));
        accountLogoutItem.setOnAction(e -> logout());
        accountMenuButton.getItems().addAll(accountEditProfileItem, accountAdminDashboardItem, accountLogoutItem);
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(12, spacer, accountMenuButton);
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
        if (subjectDescriptionArea != null) {
            subjectDescriptionArea.setPromptText("Description, avec @nomUtilisateur pour notifier une personne");
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
        if (messageContentArea != null) {
            messageContentArea.setPromptText("Votre commentaire, avec @nomUtilisateur pour notifier une personne");
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

    private void handleEditAdminUser(User user) {
        if (user != null) {
            showAdminUserFormPageForEdit(user);
        }
    }

    private void handleBanAdminUser(User user) {
        if (user == null) {
            return;
        }

        BanDialogResult banResult = showBanUntilDialog(user);
        if (banResult == null) {
            return;
        }

        LocalDateTime bannedUntil = banResult.clearBan ? null : banResult.selectedDate.atStartOfDay();

        try {
            userController.updateUser(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    null,
                    user.getRole(),
                    bannedUntil
            );
            if (banResult.clearBan) {
                showInfo("User unbanned", user.getUsername() + " is now active again.");
            } else {
                showInfo("User banned", user.getUsername() + " has been banned until " + banResult.selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");
            }
            loadAdminUsers();
        } catch (SQLException e) {
            showError("Error", e.getMessage());
        }
    }

    private void handleDeleteAdminUser(User user) {
        if (user != null) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
            alert.setHeaderText("Delete user #" + user.getId());
            alert.setContentText("Confirm deletion of " + user.getUsername() + "?");
            java.util.Optional<javafx.scene.control.ButtonType> ok = alert.showAndWait();
            if (ok.isPresent() && ok.get().getButtonData() == javafx.scene.control.ButtonBar.ButtonData.OK_DONE) {
                try {
                    userController.deleteUser(user.getId());
                    showInfo("User deleted", "The user has been deleted.");
                    loadAdminUsers();
                } catch (SQLException e) {
                    showError("Error", e.getMessage());
                }
            }
        }
    }

    private BanDialogResult showBanUntilDialog(User user) {
        javafx.scene.control.Dialog<BanDialogResult> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Ban user");
        dialog.setHeaderText("Choose a ban expiry date or clear the ban for " + user.getUsername() + ".");

        javafx.scene.control.ButtonType banButtonType = new javafx.scene.control.ButtonType("Ban", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(banButtonType, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.control.DatePicker datePicker = new javafx.scene.control.DatePicker(
                user.getBannedUntil() != null && user.isBanned() ? user.getBannedUntil().toLocalDate() : LocalDate.now().plusDays(7)
        );
        datePicker.setEditable(false);
        datePicker.setMaxWidth(Double.MAX_VALUE);
        javafx.scene.control.CheckBox clearBanCheckBox = new javafx.scene.control.CheckBox("Set banned until to null and unban this user");
        datePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now().plusDays(1)));
            }
        });
        clearBanCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> datePicker.setDisable(isSelected));

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10,
                new javafx.scene.control.Label("Select a ban expiry date:"),
                datePicker,
                clearBanCheckBox,
                new javafx.scene.control.Label("If you check the box, the user will be unbanned and banned_until will become null.")
        );
        content.setPadding(new javafx.geometry.Insets(10, 0, 0, 0));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(360);
        dialog.setResultConverter(buttonType -> {
            if (buttonType != banButtonType) {
                return null;
            }
            if (clearBanCheckBox.isSelected()) {
                return new BanDialogResult(true, null);
            }
            return new BanDialogResult(false, datePicker.getValue());
        });

        java.util.Optional<BanDialogResult> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private static final class BanDialogResult {
        private final boolean clearBan;
        private final LocalDate selectedDate;

        private BanDialogResult(boolean clearBan, LocalDate selectedDate) {
            this.clearBan = clearBan;
            this.selectedDate = selectedDate;
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
    private void handleFaceLogin() {
        if (loginErrorLabel != null) {
            loginErrorLabel.setVisible(false);
            loginErrorLabel.setManaged(false);
        }

        showFaceCaptureDialog("Face ID Login", base64Image -> {
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        String subject = compreFaceClient.recognizeFace(base64Image);
                        if (subject != null && !subject.isEmpty()) {
                            User user = authService.loginByFaceId(subject);
                            Platform.runLater(() -> {
                                currentUser = user;
                                applyRole();
                                resetForm();
                                loadEvents();
                                loadReservations();
                                loadForumSubjects();
                                updateHomePageStats();
                                if (user.isAdmin()) {
                                    loadAdminUsers();
                                    resetAdminUserForm();
                                }
                                showLandingPageForUser(user);
                            });
                        } else {
                            throw new Exception("Visage non reconnu ou non enregistre.");
                        }
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            if (loginErrorLabel != null) {
                                loginErrorLabel.setText("❌ Face ID Erreur: " + e.getMessage());
                                loginErrorLabel.setVisible(true);
                                loginErrorLabel.setManaged(true);
                            } else {
                                showError("Face ID Echoue", e.getMessage());
                            }
                        });
                    }
                    return null;
                }
            };
            new Thread(task).start();
        });
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
    private void handleConfirmAccount() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Confirm Account");
        dialog.setHeaderText("Enter the confirmation code from your MailHog email.");
        dialog.setContentText("Code:");

        Optional<String> tokenInput = dialog.showAndWait();
        if (tokenInput.isEmpty() || tokenInput.get().isBlank()) {
            return;
        }

        try {
            authService.confirmAccount(extractTokenFromLinkOrToken(tokenInput.get()));
            showInfo("Account confirmed", "Your account is now active. You can sign in.");
        } catch (SQLException e) {
            showError("Confirmation failed", e.getMessage());
        }
    }

    @FXML
    private void handleForgotPassword() {
        TextInputDialog requestDialog = new TextInputDialog();
        requestDialog.setTitle("Forgot Password");
        requestDialog.setHeaderText("Enter the email address tied to your account.");
        requestDialog.setContentText("Email:");

        Optional<String> email = requestDialog.showAndWait();
        if (email.isEmpty() || email.get().isBlank()) {
            return;
        }

        try {
            authService.requestPasswordReset(email.get().trim());
            showInfo("Reset email sent", "Check MailHog for the password reset code.");
            showPasswordResetDialog();
        } catch (SQLException e) {
            showError("Password reset failed", e.getMessage());
        }
    }

    private void showPasswordResetDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reset Password");
        dialog.setHeaderText("Enter the reset code from the email and choose a new password.");

        ButtonType resetButton = new ButtonType("Reset", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(resetButton, ButtonType.CANCEL);

        TextField tokenField = new TextField();
        tokenField.setPromptText("Reset code");
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("New password");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm new password");

        VBox box = new VBox(10,
            new Label("Reset code"),
                tokenField,
                new Label("New password"),
                newPasswordField,
                new Label("Confirm password"),
                confirmPasswordField
        );
        box.setPadding(new Insets(10, 0, 0, 0));
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != resetButton) {
                return null;
            }
            return buttonType;
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != resetButton) {
            return;
        }

        String token = tokenField.getText() == null ? "" : tokenField.getText().trim();
        String newPassword = newPasswordField.getText() == null ? "" : newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        if (token.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            showWarning("All reset fields are required.");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            showWarning("Passwords do not match.");
            return;
        }

        try {
            authService.resetPassword(token, newPassword);
            showInfo("Password updated", "Your password has been changed successfully.");
        } catch (SQLException e) {
            showError("Password reset failed", e.getMessage());
        }
    }

    private String extractTokenFromLinkOrToken(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) {
            return "";
        }
        int tokenIndex = value.indexOf("token=");
        if (tokenIndex < 0) {
            return value;
        }
        String tokenValue = value.substring(tokenIndex + 6);
        int ampIndex = tokenValue.indexOf('&');
        if (ampIndex >= 0) {
            tokenValue = tokenValue.substring(0, ampIndex);
        }
        int hashIndex = tokenValue.indexOf('#');
        if (hashIndex >= 0) {
            tokenValue = tokenValue.substring(0, hashIndex);
        }
        return tokenValue.trim();
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
    private void handleSubjectAiRewrite() {
        rewriteSubjectWithAi();
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

    @FXML
    private void handleGoResources() {
        openResourcesInMainWindow();
    }

    private void attemptLogin() {
        String email = loginEmailField != null && loginEmailField.getText() != null
            ? loginEmailField.getText().trim()
                : "";
        String password = loginPasswordField != null && loginPasswordField.getText() != null
                ? loginPasswordField.getText().trim()
                : "";

        // Clear previous error
        if (loginErrorLabel != null) {
            loginErrorLabel.setVisible(false);
            loginErrorLabel.setManaged(false);
        }

        if (email.isBlank() || password.isBlank()) {
            showLoginError("❌ Tous les champs sont obligatoires.");
            return;
        }

        if (!isValidEmail(email)) {
            showLoginError("❌ Format email invalide.");
            return;
        }

        try {
            User user = authService.login(email, password);
            if (user == null) {
                showLoginError("❌ Email ou mot de passe incorrect.");
                return;
            }

            if (user.getBannedUntil() != null && user.getBannedUntil().isAfter(java.time.LocalDateTime.now())) {
                showLoginError("❌ Votre compte est banni jusqu'au " + user.getBannedUntil().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                return;
            }

            currentUser = user;
            applyRole();
            resetForm();
            loadEvents();
            loadReservations();
            loadForumSubjects();
            updateHomePageStats();
            if (isBackOfficeUser(user)) {
                loadAdminUsers();
                resetAdminUserForm();
            }

            if (loginErrorLabel != null) {
                loginErrorLabel.setVisible(false);
                loginErrorLabel.setManaged(false);
            }

            showLandingPageForUser(user);

        } catch (SQLException e) {
            String errorMsg = e.getMessage();
            if (loginErrorLabel != null) {
                if (errorMsg.contains("requis")) {
                    showLoginError("❌ Tous les champs sont obligatoires.");
                } else if (errorMsg.contains("incorrect")) {
                    showLoginError("❌ Email ou mot de passe incorrect.");
                } else if (errorMsg.contains("non activé") || errorMsg.contains("confirmer votre compte")) {
                    boolean confirmed = promptForValidationCode();
                    if (confirmed) {
                        attemptLogin();
                        return;
                    }
                    showLoginError("❌ Compte non activé. Entrez le code envoyé par email.");
                } else if (errorMsg.contains("banni")) {
                    showLoginError("❌ Votre compte est banni. Contactez l'administrateur.");
                } else {
                    showLoginError("❌ Erreur: " + errorMsg);
                }
            } else {
                showError("Connexion impossible", errorMsg);
            }
        }
    }

    private boolean promptForValidationCode() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Account validation");
        dialog.setHeaderText("Enter the verification code sent to your email.");
        dialog.setContentText("Code:");

        Optional<String> codeInput = dialog.showAndWait();
        if (codeInput.isEmpty() || codeInput.get().isBlank()) {
            return false;
        }

        try {
            authService.confirmAccount(extractTokenFromLinkOrToken(codeInput.get()));
            showInfo("Account confirmed", "Your account is now active. Please sign in.");
            return true;
        } catch (SQLException ex) {
            showError("Confirmation failed", ex.getMessage());
            return false;
        }
    }
    private void attemptRegister() {
        String email = registerEmailField != null ? registerEmailField.getText().trim() : "";
        String firstName = registerFirstNameField != null ? registerFirstNameField.getText().trim() : "";
        String lastName = registerLastNameField != null ? registerLastNameField.getText().trim() : "";
        String password = registerPasswordField != null ? registerPasswordField.getText() : "";
        String confirm = registerConfirmPasswordField != null ? registerConfirmPasswordField.getText() : "";

        if (registerErrorLabel != null) {
            registerErrorLabel.setVisible(false);
            registerErrorLabel.setManaged(false);
        }

        if (email.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showRegisterError("❌ Tous les champs sont obligatoires.");
            return;
        }

        if (!isValidEmail(email)) {
            showRegisterError("❌ Email invalide (ex: user@mail.com).");
            return;
        }

        if (password.length() < 6) {
            showRegisterError("❌ Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }

        if (!password.equals(confirm)) {
            showRegisterError("❌ Les mots de passe ne correspondent pas.");
            return;
        }

        try {
            authService.register(email, firstName, lastName, password, "ETUDIANT");
            showInfo("Inscription réussie", "Votre compte a été créé. Vérifiez MailHog pour confirmer votre compte avant de vous connecter.");
            showLoginPage();
        } catch (SQLException ex) {
            String msg = ex.getMessage();
            if (msg != null && (msg.contains("existe déjà") || msg.contains("existe deja"))) {
                showRegisterError("❌ Cette adresse email ou ce compte existe déjà.");
            } else {
                showRegisterError("❌ Erreur: " + msg);
            }
        }
    }

    private void showRegisterError(String message) {
        if (registerErrorLabel == null) return;
        registerErrorLabel.setText(message);
        registerErrorLabel.setVisible(true);
        registerErrorLabel.setManaged(true);
    }

    private void showLoginError(String message) {
        if (loginErrorLabel == null) return;
        loginErrorLabel.setText(message);
        loginErrorLabel.setVisible(true);
        loginErrorLabel.setManaged(true);
    }


    private void applyRole() {
        boolean backOffice = isBackOfficeUser(currentUser);
        refreshAccountMenuLabel();
        accountMenuButton.setVisible(true); accountMenuButton.setManaged(true);
        if (accountAdminDashboardItem != null) {
            accountAdminDashboardItem.setVisible(backOffice);
        }
        // Top-right management buttons are removed; left navigation handles management.
        eventsHeaderButton.setVisible(false); eventsHeaderButton.setManaged(false);
        addHeaderButton.setVisible(false); addHeaderButton.setManaged(false);
        reservationsHeaderButton.setVisible(false); reservationsHeaderButton.setManaged(false);
        forumHeaderButton.setVisible(false); forumHeaderButton.setManaged(false);
        resourcesHeaderButton.setVisible(false); resourcesHeaderButton.setManaged(false);
        statsHeaderButton.setVisible(false); statsHeaderButton.setManaged(false);
        eventsTitle.setText(backOffice ? "Back office des evenements" : "Catalogue des evenements");
        eventsSubtitle.setText(backOffice ? "Admin/Psy peut modifier, supprimer et suivre les reservations." : "L'etudiant consulte les evenements avec photo et peut reserver.");
    }

    private boolean isBackOfficeUser(User user) {
        if (user == null) {
            return false;
        }
        if (user.isAdmin()) {
            return true;
        }
        String role = user.getRole();
        return role != null && "PSYCHOLOGUE".equalsIgnoreCase(role.trim());
    }

    private void refreshAccountMenuLabel() {
        if (accountMenuButton == null || currentUser == null) {
            return;
        }
        accountMenuButton.setText(currentUser.getUsername());
    }

    private VBox getLandingPageForUser(User user) {
        if (isBackOfficeUser(user)) {
            return adminDashboardPage != null ? adminDashboardPage : eventsPage;
        }
        return ensureHomePageLoaded();
    }

    private VBox buildAdminDashboardStatsPage() {
        VBox page = loadPage("AdminDashboardStats.fxml");
        initAdminDashboardStatsPage();
        return page;
    }

    private void initAdminDashboardStatsPage() {
        if (adminStatsRefreshButton != null) {
            adminStatsRefreshButton.setOnAction(e -> updateAdminDashboardStats());
        }
        if (adminStatsManageUsersButton != null) {
            adminStatsManageUsersButton.setOnAction(e -> {
                setAdminContent(adminUsersListPage);
            });
        }
    }

    private void updateAdminDashboardStats() {
        try {
            List<User> users = userController.getAllUsers();
            long totalUsers = users.size();
            long admins = users.stream().filter(User::isAdmin).count();
            long psychologues = users.stream().filter(user -> "PSYCHOLOGUE".equalsIgnoreCase(user.getRole())).count();
            long clients = users.stream().filter(user -> "CLIENT".equalsIgnoreCase(user.getRole())).count();
            long etudiants = users.stream().filter(user -> "ETUDIANT".equalsIgnoreCase(user.getRole())).count();
            long banned = users.stream().filter(User::isBanned).count();

            setStatLabel(adminStatsTotalUsersValue, totalUsers);
            setStatLabel(adminStatsAdminsValue, admins);
            setStatLabel(adminStatsPsychologuesValue, psychologues);
            setStatLabel(adminStatsBannedValue, banned);

            // Update Pie Chart
            if (adminStatsRoleChart != null) {
                adminStatsRoleChart.getData().clear();
                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                if (admins > 0) pieData.add(new PieChart.Data("Admins", admins));
                if (psychologues > 0) pieData.add(new PieChart.Data("Psychologists", psychologues));
                if (clients > 0) pieData.add(new PieChart.Data("Clients", clients));
                if (etudiants > 0) pieData.add(new PieChart.Data("Students", etudiants));
                
                adminStatsRoleChart.setData(pieData);
                
                // Add interactive tooltips and hover effects
                double total = pieData.stream().mapToDouble(PieChart.Data::getPieValue).sum();
                for (PieChart.Data data : pieData) {
                    Node node = data.getNode();
                    if (node != null) {
                        setupPieDataNode(data, total);
                    } else {
                        // Node might not be created yet, add listener
                        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                            if (newNode != null) {
                                setupPieDataNode(data, total);
                            }
                        });
                    }
                }
            }

            // Update Trend Chart (Last 7 days)
            if (adminStatsTrendChart != null) {
                adminStatsTrendChart.getData().clear();
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("New Registrations");

                LocalDate today = LocalDate.now();
                Map<LocalDate, Long> regCounts = users.stream()
                        .filter(u -> u.getCreatedAt() != null)
                        .map(u -> u.getCreatedAt().toLocalDate())
                        .filter(date -> !date.isBefore(today.minusDays(6)))
                        .collect(java.util.stream.Collectors.groupingBy(d -> d, java.util.stream.Collectors.counting()));

                for (int i = 6; i >= 0; i--) {
                    LocalDate date = today.minusDays(i);
                    String label = date.format(DateTimeFormatter.ofPattern("dd MMM"));
                    series.getData().add(new XYChart.Data<>(label, regCounts.getOrDefault(date, 0L)));
                }
                adminStatsTrendChart.getData().add(series);
            }

            if (adminStatsLastUpdatedLabel != null) {
                adminStatsLastUpdatedLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }

        } catch (SQLException e) {
            showError("User statistics unavailable", e.getMessage());
        }
    }

    private void setupPieDataNode(PieChart.Data data, double total) {
        Node node = data.getNode();
        if (node == null) return;

        double percentage = (data.getPieValue() / total) * 100;
        String tooltipText = String.format("%s\n━━━━━━━━━━━━━━━━\nUsers: %.0f\nShare: %.1f%%", 
                data.getName().toUpperCase(), data.getPieValue(), percentage);
        
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setShowDelay(Duration.millis(50));
        tooltip.setShowDuration(Duration.seconds(10));
        tooltip.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: #1e293b; -fx-text-fill: white; -fx-padding: 12; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 4);");
        Tooltip.install(node, tooltip);
        
        node.setOnMouseEntered(e -> {
            node.setScaleX(1.06);
            node.setScaleY(1.06);
            node.setCursor(Cursor.HAND);
            node.toFront(); // Ensure the hovered slice is on top
        });
        
        node.setOnMouseExited(e -> {
            node.setScaleX(1.0);
            node.setScaleY(1.0);
            node.setCursor(Cursor.DEFAULT);
        });
    }

    private void setStatLabel(Label label, long value) {
        if (label != null) {
            label.setText(String.valueOf(value));
        }
    }

    private VBox ensureHomePageLoaded() {
        if (homePage == null) {
            homePage = buildHomePage();
        }
        return homePage;
    }

    private void showLandingPageForUser(User user) {
        try {
            VBox landingPage = getLandingPageForUser(user);
            if (landingPage == null) {
                throw new IllegalStateException("Landing page is not initialized.");
            }
            showPage(landingPage);
            if (isBackOfficeUser(user)) {
                updateAdminDashboardStats();
            }
        } catch (RuntimeException ex) {
            // Keep login usable even if one view fails to load.
            showError("Impossible de charger HomeView", ex.getMessage());
            loadEvents();
            showPage(eventsPage);
        }
    }

    private VBox buildAdminDashboardPage() {
        VBox page = loadPage("AdminDashboardView.fxml");
        adminDashboardStatsPage = adminDashboardStatsPage != null ? adminDashboardStatsPage : buildAdminDashboardStatsPage();
        adminUsersListPage = buildAdminUsersListPage();
        adminUserFormPage = buildAdminUserFormPage();
        
        if (adminDashboardNavButton != null) {
            adminDashboardNavButton.setOnAction(e -> {
                if (adminContentPane != null && adminDashboardStatsPage != null) {
                    adminContentPane.getChildren().setAll(adminDashboardStatsPage);
                    updateAdminDashboardStats();
                }
            });
        }
        if (adminUsersNavButton != null) {
            adminUsersNavButton.setOnAction(e -> showAdminUsersListPage());
        }
        if (adminAddEventNavButton != null) {
            adminAddEventNavButton.setOnAction(e -> showAdminContentPage(formPage, this::resetForm));
        }
        if (adminEventsNavButton != null) {
            adminEventsNavButton.setOnAction(e -> showAdminContentPage(eventsPage, this::loadEvents));
        }
        if (adminReservationsNavButton != null) {
            adminReservationsNavButton.setOnAction(e -> showAdminContentPage(reservationsPage, this::loadReservations));
        }
        if (adminForumNavButton != null) {
            adminForumNavButton.setOnAction(e -> showAdminContentPage(forumPage, this::loadForumSubjects));
        }
        if (adminResourcesNavButton != null) {
            adminResourcesNavButton.setOnAction(e -> showAdminResourcesPage());
        }
        if (adminStatsNavButton != null) {
            adminStatsNavButton.setOnAction(e -> {
                try {
                    statsPage = buildStatsPageGlobal();
                    showAdminContentPage(statsPage, null);
                } catch (SQLException ex) {
                    showError("Erreur", ex.getMessage());
                }
            });
        }
        
        if (adminContentPane != null) {
            setAdminContent(adminDashboardStatsPage != null ? adminDashboardStatsPage : adminUsersListPage);
            loadAdminUsers();
            updateAdminDashboardStats();
        }
        
        return page;
    }

    private void showAdminContentPage(VBox page, Runnable beforeShow) {
        if (page == null || adminContentPane == null) {
            return;
        }
        if (beforeShow != null) {
            beforeShow.run();
        }
        setAdminContent(page);
    }

    private void showAdminResourcesPage() {
        if (adminContentPane == null) {
            return;
        }
        Node resourcesNode = buildResourcesNodeForCurrentUser();
        if (resourcesNode == null) {
            return;
        }
        setAdminContent(resourcesNode);
    }

    private VBox buildAdminUsersListPage() {
        VBox page = loadPage("AdminUsersListView.fxml");
        initAdminUsersListPage();
        return page;
    }

    private VBox buildAdminUserFormPage() {
        VBox page = loadPage("AdminUserFormView.fxml");
        initAdminUserFormPage();
        return page;
    }

    private void initAdminUsersListPage() {
        if (usersTable == null) {
            return;
        }
        usersTable.setPlaceholder(new Label("No users found."));
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        usersTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && usersTable.getSelectionModel().getSelectedItem() != null) {
                handleEditSelectedAdminUser();
            }
        });
        
        if (adminUsersFilteredList == null) {
            adminUsersFilteredList = new FilteredList<>(adminUsersMasterList, user -> true);
        }
        usersTable.setItems(adminUsersFilteredList);

        if (usersIdCol != null) {
            usersIdCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        }
        if (usersUsernameCol != null) {
            usersUsernameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername() == null ? "" : data.getValue().getUsername()));
        }
        if (usersEmailCol != null) {
            usersEmailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail() == null ? "" : data.getValue().getEmail()));
        }
        if (usersRoleCol != null) {
            usersRoleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole() == null ? "" : data.getValue().getRole()));
        }
        if (usersStatusCol != null) {
            usersStatusCol.setCellValueFactory(data -> {
                User u = data.getValue();
                if (!u.isEmailVerified()) {
                    return new SimpleStringProperty("Pending verification");
                } else if (u.isBanned() && u.getBannedUntil() != null) {
                    java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    return new SimpleStringProperty("Banned (" + u.getBannedUntil().format(f) + ")");
                } else {
                    return new SimpleStringProperty("Active");
                }
            });
        }
        if (usersActionsCol != null) {
            usersActionsCol.setCellFactory(param -> new TableCell<>() {
                private final Button editBtn = new Button("Edit");
                private final Button banBtn = new Button("Ban");
                private final Button deleteBtn = new Button("Delete");
                private final HBox pane = new HBox(10, editBtn, banBtn, deleteBtn);

                {
                    pane.setAlignment(javafx.geometry.Pos.CENTER);
                    pane.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                    editBtn.setMinWidth(Region.USE_PREF_SIZE);
                    banBtn.setMinWidth(Region.USE_PREF_SIZE);
                    deleteBtn.setMinWidth(Region.USE_PREF_SIZE);
                    editBtn.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #2563eb; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 5px 10px; -fx-background-radius: 5px;");
                    banBtn.setStyle("-fx-background-color: #fffbeb; -fx-text-fill: #d97706; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 5px 10px; -fx-background-radius: 5px;");
                    deleteBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #dc2626; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 5px 10px; -fx-background-radius: 5px;");

                    editBtn.setOnAction(event -> {
                        User user = getTableView().getItems().get(getIndex());
                        handleEditAdminUser(user);
                    });
                    banBtn.setOnAction(event -> {
                        User user = getTableView().getItems().get(getIndex());
                        handleBanAdminUser(user);
                    });
                    deleteBtn.setOnAction(event -> {
                        User user = getTableView().getItems().get(getIndex());
                        handleDeleteAdminUser(user);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : pane);
                }
            });
        }
        if (adminUsersRefreshButton != null) {
            adminUsersRefreshButton.setOnAction(e -> handleRefreshAdminUsers());
        }
        if (adminUsersAddButton != null) {
            adminUsersAddButton.setOnAction(e -> handleOpenAdminUserForm());
        }
        if (adminUsersSearchField != null) {
            adminUsersSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyAdminUsersFilter());
        }
        if (adminUsersClearSearchButton != null) {
            adminUsersClearSearchButton.setOnAction(e -> {
                if (adminUsersSearchField != null) {
                    adminUsersSearchField.clear();
                }
                applyAdminUsersFilter();
            });
        }
    }

    private void initAdminUserFormPage() {
        if (adminRoleField != null) {
            adminRoleField.setItems(FXCollections.observableArrayList("ADMIN", "ETUDIANT", "CLIENT", "PSYCHOLOGUE"));
            adminRoleField.setValue("ETUDIANT");
        }
        if (adminSaveUserButton != null) {
            adminSaveUserButton.setOnAction(e -> handleSaveAdminUser());
        }
        if (adminResetUserButton != null) {
            adminResetUserButton.setOnAction(e -> handleResetAdminUserForm());
        }
        if (adminBackToListButton != null) {
            adminBackToListButton.setOnAction(e -> handleOpenAdminUsersList());
        }
    }

    private void loadAdminUsers() {
        if (usersTable == null) {
            return;
        }
        try {
            adminUsersMasterList.setAll(userController.getAllUsers());
            applyAdminUsersFilter();
            clearInlineError(adminUsersListErrorLabel);
        } catch (SQLException e) {
            adminUsersMasterList.clear();
            setInlineError(adminUsersListErrorLabel, "Unable to load users: " + e.getMessage());
        }
    }

    private void applyAdminUsersFilter() {
        if (adminUsersFilteredList == null) {
            return;
        }
        String keyword = adminUsersSearchField == null || adminUsersSearchField.getText() == null
                ? ""
                : adminUsersSearchField.getText().trim().toLowerCase(Locale.ROOT);

        adminUsersFilteredList.setPredicate(user -> {
            if (user == null) return false;
            if (keyword.isBlank()) return true;
            return safeContains(user.getUsername(), keyword)
                    || safeContains(user.getEmail(), keyword)
                    || safeContains(user.getFirstName(), keyword)
                    || safeContains(user.getLastName(), keyword)
                    || safeContains(user.getRole(), keyword)
                    || String.valueOf(user.getId()).contains(keyword);
        });
    }

    private boolean safeContains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private void fillAdminUserForm(User user) {
        editingAdminUser = user;
        adminUserFormTitle.setText("Edit User #" + user.getId());
        adminSaveUserButton.setText("Update User");
        adminUsernameField.setText(user.getUsername() == null ? "" : user.getUsername());
        adminEmailField.setText(user.getEmail() == null ? "" : user.getEmail());
        adminFirstNameField.setText(user.getFirstName() == null ? "" : user.getFirstName());
        adminLastNameField.setText(user.getLastName() == null ? "" : user.getLastName());
        adminPasswordField.clear();
        adminRoleField.setValue(user.getRole() == null || user.getRole().isBlank() ? "ETUDIANT" : user.getRole().trim().toUpperCase(Locale.ROOT));
        if (adminBannedUntilField != null) {
            adminBannedUntilField.setValue(user.getBannedUntil() != null && user.isBanned() ? user.getBannedUntil().toLocalDate() : null);
        }
        clearInlineError(adminUserFormErrorLabel);
    }

    private void resetAdminUserForm() {
        editingAdminUser = null;
        if (adminUserFormTitle != null) {
            adminUserFormTitle.setText("Create User");
        }
        if (adminSaveUserButton != null) {
            adminSaveUserButton.setText("Create User");
        }
        if (adminUsernameField != null) adminUsernameField.clear();
        if (adminEmailField != null) adminEmailField.clear();
        if (adminFirstNameField != null) adminFirstNameField.clear();
        if (adminLastNameField != null) adminLastNameField.clear();
        if (adminPasswordField != null) adminPasswordField.clear();
        if (adminRoleField != null) adminRoleField.setValue("ETUDIANT");
        if (adminBannedUntilField != null) adminBannedUntilField.setValue(null);
        clearInlineError(adminUserFormErrorLabel);
    }

    private void saveAdminUser() {
        if (currentUser == null || !currentUser.isAdmin()) {
            setInlineError(adminUserFormErrorLabel, "Admin access required.");
            return;
        }

        String username = adminUsernameField.getText();
        String email = adminEmailField.getText();
        String firstName = adminFirstNameField.getText();
        String lastName = adminLastNameField.getText();
        String password = adminPasswordField.getText();
        String role = adminRoleField.getValue();
        java.time.LocalDateTime bannedUntil = (adminBannedUntilField != null && adminBannedUntilField.getValue() != null)
                ? adminBannedUntilField.getValue().atStartOfDay() : null;

        try {
            if (editingAdminUser == null) {
                userController.createUser(username, email, firstName, lastName, password, role, bannedUntil);
                showInfo("User created", "The user has been added.");
            } else {
                if (currentUser.getId() == editingAdminUser.getId() && bannedUntil != null) {
                    setInlineError(adminUserFormErrorLabel, "You cannot ban your current admin account.");
                    return;
                }
                userController.updateUser(editingAdminUser.getId(), username, email, firstName, lastName, password, role, bannedUntil);
                showInfo("User updated", "User data has been saved.");
            }
            loadAdminUsers();
            resetAdminUserForm();
            showAdminUsersListPage();
        } catch (SQLException e) {
            setInlineError(adminUserFormErrorLabel, e.getMessage());
        }
    }

    private void deleteSelectedAdminUser() {
        if (currentUser == null || !currentUser.isAdmin()) {
            setInlineError(adminUsersListErrorLabel, "Admin access required.");
            return;
        }

        User selected = usersTable == null ? null : usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setInlineError(adminUsersListErrorLabel, "Select a user to delete.");
            return;
        }
        if (currentUser.getId() == selected.getId()) {
            setInlineError(adminUsersListErrorLabel, "You cannot delete your own admin account.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Delete user #" + selected.getId());
        alert.setContentText("Confirm deletion of " + selected.getUsername() + "?");
        Optional<ButtonType> ok = alert.showAndWait();
        if (ok.isEmpty() || ok.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return;
        }

        try {
            userController.deleteUser(selected.getId());
            showInfo("User deleted", "The user has been removed.");
            loadAdminUsers();
            clearInlineError(adminUsersListErrorLabel);
        } catch (SQLException e) {
            setInlineError(adminUsersListErrorLabel, e.getMessage());
        }
    }

    private void showAdminUsersListPage() {
        if (adminContentPane == null || adminUsersListPage == null) {
            return;
        }
        setAdminContent(adminUsersListPage);
        loadAdminUsers();
    }

    private void showAdminUserFormPageForCreate() {
        resetAdminUserForm();
        setAdminContent(adminUserFormPage);
    }

    private void showAdminUserFormPageForEdit(User user) {
        if (user == null) {
            return;
        }
        setAdminContent(adminUserFormPage);
        fillAdminUserForm(user);
    }

    @FXML
    private void handleOpenAdminUsersList() {
        showAdminUsersListPage();
    }

    @FXML
    private void handleOpenAdminUserForm() {
        showAdminUserFormPageForCreate();
    }

    @FXML
    private void handleEditSelectedAdminUser() {
        User selected = usersTable == null ? null : usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setInlineError(adminUsersListErrorLabel, "Select a user to edit.");
            return;
        }
        clearInlineError(adminUsersListErrorLabel);
        showAdminUserFormPageForEdit(selected);
    }

    @FXML
    private void handleRefreshAdminUsers() {
        loadAdminUsers();
    }

    @FXML
    private void handleDeleteSelectedAdminUser() {
        deleteSelectedAdminUser();
    }

    @FXML
    private void handleSaveAdminUser() {
        saveAdminUser();
    }

    @FXML
    private void handleResetAdminUserForm() {
        resetAdminUserForm();
    }

    private void openEditProfileDialog() {
        if (currentUser == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit profile");
        dialog.setHeaderText("Update your account information");

        TextField usernameField = new TextField(currentUser.getUsername() == null ? "" : currentUser.getUsername());
        TextField emailField = new TextField(currentUser.getEmail() == null ? "" : currentUser.getEmail());
        TextField firstNameField = new TextField(currentUser.getFirstName() == null ? "" : currentUser.getFirstName());
        TextField lastNameField = new TextField(currentUser.getLastName() == null ? "" : currentUser.getLastName());
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Leave empty to keep current password");

        usernameField.setStyle(INPUT);
        emailField.setStyle(INPUT);
        firstNameField.setStyle(INPUT);
        lastNameField.setStyle(INPUT);
        newPasswordField.setStyle(INPUT);

        GridPane form = formGrid();
        addRow(form, 0, "Username", usernameField);
        addRow(form, 1, "Email", emailField);
        addRow(form, 2, "First name", firstNameField);
        addRow(form, 3, "Last name", lastNameField);
        addRow(form, 4, "New password", newPasswordField);

        Button faceIdToggleBtn = new Button(currentUser.isFaceIdEnabled() ? "Desactiver Face ID" : "Activer Face ID");
        faceIdToggleBtn.setStyle(currentUser.isFaceIdEnabled() ? DANGER : PRIMARY);
        faceIdToggleBtn.setOnAction(e -> handleToggleFaceId(faceIdToggleBtn));
        addRow(form, 5, "Face ID", faceIdToggleBtn);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String firstName = firstNameField.getText() == null ? "" : firstNameField.getText().trim();
        String lastName = lastNameField.getText() == null ? "" : lastNameField.getText().trim();
        String newPassword = newPasswordField.getText();

        if (username.isBlank() || email.isBlank()) {
            showWarning("Username et email sont obligatoires.");
            return;
        }
        if (!isValidEmail(email)) {
            showWarning("Email invalide (ex: user@mail.com).");
            return;
        }

        try {
            userController.updateUser(currentUser.getId(), username, email, firstName, lastName,
                    newPassword, currentUser.getRole(), currentUser.getBannedUntil());

            currentUser.setUsername(username);
            currentUser.setEmail(email);
            currentUser.setFirstName(firstName);
            currentUser.setLastName(lastName);
            refreshAccountMenuLabel();

            showInfo("Profile updated", "Your profile has been updated successfully.");
            if (currentUser.isAdmin()) {
                loadAdminUsers();
            }
        } catch (SQLException e) {
            showError("Profile update failed", e.getMessage());
        }
    }

    private void showFaceCaptureDialog(String titleText, Consumer<String> onCapture) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(titleText);
        dialog.setHeaderText("Alignez votre visage et cliquez sur Capturer");

        ImageView imageView = new ImageView();
        imageView.setFitWidth(400);
        imageView.setFitHeight(300);
        imageView.setPreserveRatio(true);

        VBox content = new VBox(15, imageView);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 20px; -fx-background-color: #000000; -fx-border-radius: 12px; -fx-background-radius: 12px;");

        dialog.getDialogPane().setContent(content);
        ButtonType captureButtonType = new ButtonType("Capturer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(captureButtonType, ButtonType.CANCEL);

        AtomicBoolean isCapturing = new AtomicBoolean(true);
        Webcam webcam = Webcam.getDefault();
        
        final java.util.concurrent.atomic.AtomicReference<BufferedImage> lastImage = new java.util.concurrent.atomic.AtomicReference<>();

        if (webcam != null) {
            Task<Void> cameraTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        webcam.open();
                        while (isCapturing.get() && webcam.isOpen()) {
                            BufferedImage image = webcam.getImage();
                            if (image != null) {
                                lastImage.set(image);
                                javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(image, null);
                                Platform.runLater(() -> imageView.setImage(fxImage));
                            }
                            Thread.sleep(33);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        webcam.close();
                    }
                    return null;
                }
            };
            Thread t = new Thread(cameraTask);
            t.setDaemon(true);
            t.start();
        } else {
            showError("Webcam", "Aucune webcam détectée.");
            return;
        }

        Optional<ButtonType> result = dialog.showAndWait();
        isCapturing.set(false);

        if (result.isPresent() && result.get() == captureButtonType) {
            BufferedImage image = lastImage.get();
            if (image != null) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "jpg", baos);
                    String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
                    onCapture.accept(base64Image);
                } catch (Exception e) {
                    showError("Erreur", "Impossible de lire l'image.");
                }
            } else {
                showError("Erreur", "Aucune image capturée.");
            }
        }
    }

    private void handleToggleFaceId(Button btn) {
        boolean isEnabled = currentUser.isFaceIdEnabled();

        if (isEnabled) {
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        Platform.runLater(() -> btn.setText("Desactivation..."));
                        compreFaceClient.deleteSubject(currentUser.getUsername());
                        userController.updateFaceIdStatus(currentUser.getId(), false);
                        Platform.runLater(() -> {
                            currentUser.setFaceIdEnabled(false);
                            btn.setText("Activer Face ID");
                            btn.setStyle(PRIMARY);
                            showInfo("Face ID", "Face ID a ete desactive.");
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            showError("Erreur Face ID", e.getMessage());
                            btn.setText("Desactiver Face ID");
                        });
                    }
                    return null;
                }
            };
            new Thread(task).start();
        } else {
            showFaceCaptureDialog("Activer Face ID", base64Image -> {
                Task<Void> task = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        try {
                            Platform.runLater(() -> btn.setText("Enregistrement du visage..."));
                            compreFaceClient.enrollFace(base64Image, currentUser.getUsername());
                            userController.updateFaceIdStatus(currentUser.getId(), true);
                            Platform.runLater(() -> {
                                currentUser.setFaceIdEnabled(true);
                                btn.setText("Desactiver Face ID");
                                btn.setStyle(DANGER);
                                showInfo("Face ID", "Votre visage a ete enregistre avec succes.");
                            });
                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                showError("Erreur Face ID", e.getMessage());
                                btn.setText("Activer Face ID");
                            });
                        }
                        return null;
                    }
                };
                new Thread(task).start();
            });
        }
    }

    private void showLoginPage() {
        if (loginEmailField != null) loginEmailField.clear();
        if (loginPasswordField != null) loginPasswordField.clear();
        if (loginErrorLabel != null) {
            loginErrorLabel.setVisible(false);
            loginErrorLabel.setManaged(false);
        }
        if (registerErrorLabel != null) {
            registerErrorLabel.setVisible(false);
            registerErrorLabel.setManaged(false);
        }
        for (Node n : List.of(addHeaderButton, eventsHeaderButton, reservationsHeaderButton, forumHeaderButton, resourcesHeaderButton, statsHeaderButton, accountMenuButton)) {
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
        if (registerEmailField != null) registerEmailField.clear();
        if (registerFirstNameField != null) registerFirstNameField.clear();
        if (registerLastNameField != null) registerLastNameField.clear();
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
        editingAdminUser = null;

        // Clear data collections
        reservedEventIds.clear();
        reservationCounts.clear();

        // Clear form fields
        if (loginEmailField != null) loginEmailField.clear();
        if (loginPasswordField != null) loginPasswordField.clear();
        if (registerEmailField != null) registerEmailField.clear();
        if (registerFirstNameField != null) registerFirstNameField.clear();
        if (registerLastNameField != null) registerLastNameField.clear();
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
        for (Node n : List.of(addHeaderButton, eventsHeaderButton,
                reservationsHeaderButton, forumHeaderButton, resourcesHeaderButton,
                statsHeaderButton, accountMenuButton)) {
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
        resetAdminUserForm();
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
            
            // Get all subjects
            allForumSubjects = forumService.getSubjects(query, sortBy, userId);
            currentForumPage = 1;
            
            displayForumPage();
            clearInlineError(forumErrorLabel);
        } catch (SQLException e) {
            allForumSubjects.clear();
            forumListView.getItems().clear();
            setInlineError(forumErrorLabel, "Chargement du forum impossible: " + e.getMessage());
        }
    }

    private void displayForumPage() {
        int totalPages = (int) Math.ceil((double) allForumSubjects.size() / SUBJECTS_PER_PAGE);
        if (currentForumPage < 1) currentForumPage = 1;
        if (currentForumPage > totalPages && totalPages > 0) currentForumPage = totalPages;

        int startIdx = (currentForumPage - 1) * SUBJECTS_PER_PAGE;
        int endIdx = Math.min(startIdx + SUBJECTS_PER_PAGE, allForumSubjects.size());
        List<ForumSubject> pageSubjects = allForumSubjects.subList(startIdx, endIdx);

        forumListView.getItems().setAll(pageSubjects);
        
        if (forumPageLabel != null) {
            forumPageLabel.setText("Page " + currentForumPage + " / " + Math.max(1, totalPages));
        }
        if (forumPrevButton != null) {
            forumPrevButton.setDisable(currentForumPage <= 1);
        }
        if (forumNextButton != null) {
            forumNextButton.setDisable(currentForumPage >= totalPages || totalPages == 0);
        }
    }

    @FXML
    private void handleForumPrevPage() {
        currentForumPage--;
        displayForumPage();
    }

    @FXML
    private void handleForumNextPage() {
        currentForumPage++;
        displayForumPage();
    }

    private void loadMessages() {
        if (currentSubject == null) {
            messageIndexById.clear();
            messageListView.getItems().clear();
            allForumMessages.clear();
            currentMessagesPage = 1;
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
            allForumMessages = buildThreadedMessages(rawMessages);
            currentMessagesPage = 1;
            displayMessagesPage();
            clearInlineError(messagesErrorLabel);
        } catch (SQLException e) {
            messageIndexById.clear();
            messageListView.getItems().clear();
            allForumMessages.clear();
            setInlineError(messagesErrorLabel, "Chargement des commentaires impossible: " + e.getMessage());
        }
    }

    private void displayMessagesPage() {
        int totalPages = (int) Math.ceil((double) allForumMessages.size() / MESSAGES_PER_PAGE);
        if (currentMessagesPage < 1) currentMessagesPage = 1;
        if (currentMessagesPage > totalPages && totalPages > 0) currentMessagesPage = totalPages;

        int startIdx = (currentMessagesPage - 1) * MESSAGES_PER_PAGE;
        int endIdx = Math.min(startIdx + MESSAGES_PER_PAGE, allForumMessages.size());
        List<ForumMessage> pageMessages = allForumMessages.subList(startIdx, endIdx);

        messageListView.getItems().setAll(pageMessages);
        
        if (messagePageLabel != null) {
            messagePageLabel.setText("Page " + currentMessagesPage + " / " + Math.max(1, totalPages));
        }
        if (messagePrevButton != null) {
            messagePrevButton.setDisable(currentMessagesPage <= 1);
        }
        if (messageNextButton != null) {
            messageNextButton.setDisable(currentMessagesPage >= totalPages || totalPages == 0);
        }
    }

    @FXML
    private void handleMessagePrevPage() {
        currentMessagesPage--;
        displayMessagesPage();
    }

    @FXML
    private void handleMessageNextPage() {
        currentMessagesPage++;
        displayMessagesPage();
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
            subjectFormTitle.setText(editing ? "Modifier sujet" : "Nouveau sujet");
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
        if (subjectPinnedCheck != null) {
            subjectPinnedCheck.setDisable(false);
        }
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
                String mentionMessage = notifySubjectMentions(subject);
                showInfo("Sujet publie", "Le sujet a ete ajoute." + mentionMessage);
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

    private void rewriteSubjectWithAi() {
        clearInlineError(subjectErrorLabel);
        String title = subjectTitleField.getText() == null ? "" : subjectTitleField.getText().trim();
        String description = subjectDescriptionArea.getText() == null ? "" : subjectDescriptionArea.getText().trim();

        if (title.isBlank() && description.isBlank()) {
            setInlineError(subjectErrorLabel, "Ecrivez un titre ou une description avant la reformulation.");
            return;
        }
        if (!forumAiRewriteService.isConfigured()) {
            setInlineError(subjectErrorLabel, "Ollama n'est pas configure. Verifiez OLLAMA_BASE_URL puis relancez l'application.");
            return;
        }

        // quick model availability check to provide clearer errors (token vs model permissions)
        String modelCheck = forumAiRewriteService.validateModelAvailability();
        if (modelCheck != null) {
            setInlineError(subjectErrorLabel, "Reformulation IA impossible: " + modelCheck);
            return;
        }

        setSubjectAiRewriteRunning(true);
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return forumAiRewriteService.rewriteSubject(title, description);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .whenComplete((suggestion, error) -> Platform.runLater(() -> {
                    setSubjectAiRewriteRunning(false);
                    if (error != null) {
                        Throwable cause = unwrapAsyncError(error);
                        setInlineError(subjectErrorLabel, "Reformulation IA impossible: " + cause.getMessage());
                        return;
                    }
                    applySubjectRewriteSuggestion(suggestion);
                }));
    }

    private void setSubjectAiRewriteRunning(boolean running) {
        if (subjectAiRewriteButton != null) {
            subjectAiRewriteButton.setDisable(running);
            subjectAiRewriteButton.setText(running ? "Reformulation..." : "Reformuler avec IA");
        }
        if (subjectSaveButton != null) {
            subjectSaveButton.setDisable(running);
        }
    }

    private void applySubjectRewriteSuggestion(ForumRewriteSuggestion suggestion) {
        if (suggestion == null) {
            setInlineError(subjectErrorLabel, "Reponse IA vide.");
            return;
        }
        subjectTitleField.setText(suggestion.getTitle());
        subjectDescriptionArea.setText(suggestion.getDescription());
        clearInlineError(subjectErrorLabel);
        showInfo("Reformulation IA", "Le sujet a ete reformule. Relisez puis cliquez sur Publier.");
    }

    private Throwable unwrapAsyncError(Throwable error) {
        Throwable current = error;
        while ((current instanceof CompletionException || current instanceof RuntimeException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
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
            String mentionMessage = notifyMessageMentions(message);
            resetMessageForm();
            loadMessages();
            clearInlineError(messagesErrorLabel);
            if (!mentionMessage.isBlank()) {
                showInfo("Commentaire publie", "Le commentaire a ete ajoute." + mentionMessage);
            }
        } catch (SQLException e) {
            setInlineError(messagesErrorLabel, "Commentaire impossible: " + e.getMessage());
        }
    }

    private String notifySubjectMentions(ForumSubject subject) {
        try {
            // Debug: show extracted mentions
            Set<String> mentions = mentionNotificationService.extractMentionedUsernames(
                subject.getTitre(), subject.getDescription());
            if (!mentions.isEmpty()) {
                System.out.println("[DEBUG] Mentions detectees dans sujet: " + mentions);
            }
            return formatMentionNotification(mentionNotificationService.notifySubjectMentions(subject, currentUser));
        } catch (SQLException e) {
            return "\nMentions detectees, mais verification des utilisateurs impossible: " + e.getMessage();
        }
    }

    private String notifyMessageMentions(ForumMessage message) {
        try {
            // Debug: show extracted mentions
            Set<String> mentions = mentionNotificationService.extractMentionedUsernames(message.getContenu());
            if (!mentions.isEmpty()) {
                System.out.println("[DEBUG] Mentions detectees dans message: " + mentions);
            }
            return formatMentionNotification(mentionNotificationService.notifyMessageMentions(currentSubject, message, currentUser));
        } catch (SQLException e) {
            return "\nMentions detectees, mais verification des utilisateurs impossible: " + e.getMessage();
        }
    }

    private String formatMentionNotification(MentionNotificationResult result) {
        if (result == null || result.getMentionedUsers() == 0) {
            return "";
        }
        if (!result.isMailConfigured()) {
            return "\n✓ " + result.getMentionedUsers() + " mention(s) trouvee(s) et detectable(s).\nConfigurez SMTP_HOST et SMTP_FROM pour envoyer les e-mails de notification.";
        }
        if (result.getEmailsSent() == result.getMentionedUsers()) {
            return "\n✓ E-mail(s) envoye(s) a " + result.getEmailsSent() + " personne(s) mentionnee(s).";
        }
        String detail = result.getFirstError().isBlank() ? "" : "\nErreur: " + result.getFirstError();
        return "\n✓ Mentions detectees: " + result.getMentionedUsers() + 
               " | E-mails envoyes: " + result.getEmailsSent() + 
               ", echecs: " + result.getEmailsFailed() + "." + detail;
    }

    private void deleteMessage(ForumMessage message) {
        if (!canDeleteMessage(message)) { setInlineError(messagesErrorLabel, "Vous ne pouvez pas supprimer ce commentaire."); return; }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Supprimer le commentaire " );
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
            return "Utilisateur";
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
        String author = message.isAnonymous() ? "Anonyme" : (message.getUsername() == null ? "Utilisateur" : message.getUsername());
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

    @SuppressWarnings("unused")
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
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION); alert.setHeaderText("Supprimer l'evenement "); alert.setContentText("Confirmer la suppression de " + event.getTitre() + " ?");
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

    private void showPage(VBox page) {
        if (page == null) {
            return;
        }
        // Keep left admin navigation visible by swapping only center content.
        if (isBackOfficeUser(currentUser)
                && adminDashboardPage != null
                && adminContentPane != null
                && pageContainer != null
                && page != adminDashboardPage
                && pageContainer.getChildren().contains(adminDashboardPage)) {
            setAdminContent(page);
            return;
        }
        pageContainer.getChildren().setAll(page);
    }

    private void setAdminContent(Node content) {
        if (adminContentPane == null || content == null) {
            return;
        }
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        adminContentPane.getChildren().setAll(scrollPane);
    }

    private void openResourcesInMainWindow() {
        try {
            Node rootNode = buildResourcesNodeForCurrentUser();
            if (rootNode == null) {
                return;
            }

            VBox wrapper = new VBox(rootNode);
            VBox.setVgrow(rootNode, Priority.ALWAYS);
            showPage(wrapper);
        } catch (Exception e) {
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            showError("Erreur", "Impossible d'ouvrir la section Ressources: " + rootCause.getMessage());
        }
    }

    private Node buildResourcesNodeForCurrentUser() {
        try {
            boolean backOffice = isBackOfficeUser(currentUser);
            String view = backOffice ? "/org/example/fxml/resource_list.fxml" : "/org/example/fxml/resource_catalog.fxml";

            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(view), "Missing resource: " + view));
            Node rootNode = loader.load();

            if (backOffice) {
                ResourceListController controller = loader.getController();
                controller.setAdminMode(true);
                if (currentUser != null) {
                    controller.setCurrentUser(currentUser);
                }
            } else {
                ResourceCatalogController controller = loader.getController();
                controller.setAdminMode(false);
                if (currentUser != null) {
                    controller.setCurrentUser(currentUser);
                }
            }
            return rootNode;
        } catch (Exception e) {
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            showError("Erreur", "Impossible de charger la section Ressources: " + rootCause.getMessage());
            return null;
        }
    }

    private GridPane formGrid() { GridPane g = new GridPane(); g.setHgap(14); g.setVgap(14); return g; }
    private void addRow(GridPane g, int row, String label, Node field) { Label l = new Label(label); l.setStyle("-fx-text-fill:#29496f; -fx-font-size:13px; -fx-font-weight:700;"); g.add(l, 0, row); g.add(field, 1, row); GridPane.setHgrow(field, Priority.ALWAYS); }
    private Label small(String text) { Label l = new Label(text); l.setStyle("-fx-text-fill:#637a97; -fx-font-size:13px;"); return l; }
    private Label title(String text, int size) { Label l = new Label(text); l.setStyle("-fx-text-fill:#10233f; -fx-font-size:" + size + "px; -fx-font-weight:800;"); return l; }
    private Button button(String text, String style, javafx.event.EventHandler<javafx.event.ActionEvent> handler) { Button b = new Button(text); b.setStyle(style); b.setOnAction(handler); return b; }
    @SuppressWarnings("unused")
    private VBox card(Node... nodes) { VBox b = new VBox(16, nodes); b.setPadding(new Insets(28)); b.setStyle(CARD); return b; }
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

    private boolean isValidEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value.trim()).matches();
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
