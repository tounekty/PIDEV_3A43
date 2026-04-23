package org.example;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import org.example.auth.AppUser;
import org.example.auth.AuthService;
import org.example.event.EventEngagementService;
import org.example.event.EventReview;
import org.example.event.Event;
import org.example.event.EventService;
import org.example.reservation.ReservationRecord;
import org.example.reservation.ReservationService;
import org.example.util.CalendarPicker;
import org.example.util.DualMonthCalendarView;
import org.example.util.CalendarReservationController;
import org.example.util.TimePickerSpinner;
import org.example.util.ImageManager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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

    private final EventService eventService = new EventService();
    private final AuthService authService = new AuthService();
    private final EventEngagementService eventEngagementService = new EventEngagementService();
    private final ReservationService reservationService = new ReservationService();
    private final Map<Integer, Integer> reservationCounts = new HashMap<>();
    private final Map<Integer, Integer> likeCounts = new HashMap<>();
    private final Map<Integer, Integer> reviewCounts = new HashMap<>();
    private final Map<Integer, Double> averageRatings = new HashMap<>();
    private final Set<Integer> reservedEventIds = new HashSet<>();
    private final Set<Integer> likedEventIds = new HashSet<>();

    private BorderPane root;
    private StackPane pageContainer;
    private VBox loginPage;
    private VBox formPage;
    private VBox eventsPage;
    private VBox reservationsPage;
    private VBox header;
    private Stage primaryStage;
    private AppUser currentUser;
    private Event editingEvent;

    private TextField titleField;
    private TextArea descriptionArea;
    private TextField locationField;
    private DatePicker datePicker;
    private TimePickerSpinner timePicker;
    private TextField capacityField;
    private ComboBox<String> categoryField;
    private TextField imageIdField;
    private TextField imagePathField;
    private Label imageThumbnailLabel;
    private TextField searchField;
    private ComboBox<String> sortField;
    private TextField usernameField;
    private PasswordField passwordField;
    private Label userBadge;
    private Label formTitle;
    private Label eventsTitle;
    private Label eventsSubtitle;
    private Button saveButton;
    private Button cancelEditButton;
    private Button addHeaderButton;
    private Button eventsHeaderButton;
    private Button reservationsHeaderButton;
    private Button logoutButton;
    private ListView<Event> eventListView;
    private TilePane eventGrid;
    private HBox availabilityDatesRow;
    private Label resultsCountLabel;
    private Label availabilitySummaryLabel;
    private CalendarPicker eventCalendar;
    private Label calendarSelectedDateLabel;
    private ListView<ReservationRecord> reservationListView;
    private ListView<Event> calendarListView;
    private ComboBox<String> calendarModeField;
    private DatePicker calendarDatePicker;
    private Label calendarSubtitle;

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
        formPage = buildFormPage();
        eventsPage = buildEventsPageTiqets();
        reservationsPage = buildReservationsPage();
        pageContainer.getChildren().setAll(loginPage);
        root.setCenter(pageContainer);
        Scene scene = new Scene(root, 1200, 760);
        // Charger le CSS global
        java.net.URL cssUrl = getClass().getResource("/styles.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        stage.setScene(scene);
        stage.setTitle("MindCare Events");
        stage.show();
        initializeDatabase();
        showLoginPage();
    }

    private VBox buildHeader() {
        Label t1 = new Label("Event Studio 2026");
        t1.setStyle("-fx-text-fill:#2b7cff; -fx-font-size:12px; -fx-font-weight:800;");
        Label t2 = new Label("Gestion des evenements");
        t2.setStyle("-fx-text-fill:#10233f; -fx-font-size:30px; -fx-font-weight:800;");
        Label t3 = new Label("Admin ajoute et suit les reservations. Etudiant parcourt les cartes et reserve.");
        t3.setStyle("-fx-text-fill:#58708f; -fx-font-size:14px;");
        addHeaderButton = button("Ajouter", PRIMARY, e -> showPage(formPage));
        eventsHeaderButton = button("Evenements", SECONDARY, e -> { loadEvents(); showPage(eventsPage); });
        reservationsHeaderButton = button("Reservations", SECONDARY, e -> { loadReservations(); showPage(reservationsPage); });
        logoutButton = button("Deconnexion", SECONDARY, e -> logout());
        userBadge = new Label();
        userBadge.setStyle("-fx-text-fill:#0f69ff; -fx-background-color:rgba(15,105,255,0.10); -fx-background-radius:999; -fx-padding:8 12 8 12; -fx-font-size:12px; -fx-font-weight:800;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(12, spacer, userBadge, addHeaderButton, eventsHeaderButton, reservationsHeaderButton, logoutButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox box = new VBox(18, new VBox(6, t1, t2, t3), actions);
        box.setPadding(new Insets(0, 0, 18, 0));
        return box;
    }

    private VBox buildLoginPage() {
        usernameField = input("Nom d'utilisateur");
        passwordField = new PasswordField(); passwordField.setPromptText("Mot de passe"); passwordField.setStyle(INPUT);
        GridPane form = formGrid();
        addRow(form, 0, "Utilisateur", usernameField);
        addRow(form, 1, "Mot de passe", passwordField);
        Button loginBtn = button("Se connecter", PRIMARY, e -> attemptLogin());
        VBox card = card(new Label("Connexion"), new Label("Admin et etudiant utilisent chacun leur acces."), form, loginBtn,
                small("Comptes demo: admin / admin123   |   etudiant / etud123"));
        ((Label) card.getChildren().get(0)).setStyle("-fx-text-fill:#10233f; -fx-font-size:26px; -fx-font-weight:800;");
        ((Label) card.getChildren().get(1)).setStyle("-fx-text-fill:#637a97; -fx-font-size:13px;");
        card.setMaxWidth(500);
        VBox page = new VBox(card); page.setAlignment(Pos.CENTER); VBox.setVgrow(page, Priority.ALWAYS);
        return page;
    }

    private VBox buildFormPage() {
        formTitle = new Label("Nouvel evenement"); formTitle.setStyle("-fx-text-fill:#10233f; -fx-font-size:24px; -fx-font-weight:800;");
        titleField = input("Ex: Morning Yoga Session");
        descriptionArea = new TextArea(); descriptionArea.setPromptText("Description"); descriptionArea.setStyle(INPUT); descriptionArea.setPrefRowCount(4);
        locationField = input("Ex: Tunis, Lac 2");
        datePicker = new DatePicker(LocalDate.now().plusDays(1));
        datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.setStyle(
            "-fx-background-color: #f9fbff;" +
            "-fx-border-color: #d7e7ff;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: 600;" +
            "-fx-text-fill: #10233f;" +
            "-fx-padding: 8 12 8 12;"
        );
        // Wrapper pour aligner le DatePicker
        HBox dateWrapper = new HBox(datePicker);
        dateWrapper.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(datePicker, Priority.ALWAYS);
        dateWrapper.setStyle(
            "-fx-background-color: #f9fbff;" +
            "-fx-border-color: #d7e7ff;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 2 4;"
        );

        // TimePicker au lieu de TextField
        timePicker = new TimePickerSpinner(LocalTime.of(10, 0));
        
        capacityField = input("20");
        categoryField = new ComboBox<>(FXCollections.observableArrayList("yoga", "wellness", "sport", "meditation"));
        categoryField.setValue("yoga"); categoryField.setMaxWidth(Double.MAX_VALUE); categoryField.setStyle(INPUT);
        
        // Image fields — on affiche seulement le nom du fichier, pas le chemin
        imageIdField = input("Auto-générée"); imageIdField.setEditable(false);
        imageIdField.setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;");

        // imagePathField reste en mémoire pour stocker le chemin mais n'est PAS affiché
        imagePathField = new TextField(); imagePathField.setEditable(false);
        imagePathField.setVisible(false); imagePathField.setManaged(false);

        imageThumbnailLabel = new Label("Aucune image sélectionnée");
        imageThumbnailLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 12px; -fx-padding: 4 0 0 0;");
        imageThumbnailLabel.setWrapText(true);

        Button browseBtn = button("📁 Parcourir", SECONDARY, e -> chooseImage());
        HBox imageBox = new HBox(10, browseBtn, imageThumbnailLabel);
        imageBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(imageThumbnailLabel, Priority.ALWAYS);

        VBox imagePanel = new VBox(8);
        imagePanel.setStyle("-fx-border-color: #d7e7ff; -fx-border-radius: 10; -fx-background-color: #f9fbff; -fx-background-radius: 10; -fx-padding: 12;");
        imagePanel.getChildren().addAll(imageBox);
        
        GridPane form = formGrid();
        addRow(form, 0, "Titre", titleField); 
        addRow(form, 1, "Description", descriptionArea); 
        addRow(form, 2, "Lieu", locationField);
        addRow(form, 3, "Date", dateWrapper); 
        addRow(form, 4, "Heure", timePicker); 
        addRow(form, 5, "Capacite", capacityField);
        addRow(form, 6, "Categorie", categoryField); 
        addRow(form, 7, "Image", imagePanel);
        
        saveButton = button("Ajouter l'evenement", PRIMARY, e -> saveEvent());
        cancelEditButton = button("Annuler", SECONDARY, e -> resetForm()); 
        cancelEditButton.setVisible(false); cancelEditButton.setManaged(false);
        
        VBox content = card(formTitle, small("Creation et modification reservees a l'admin."), form, new HBox(12, saveButton, cancelEditButton, button("Voir le catalogue", SECONDARY, e -> { loadEvents(); showPage(eventsPage); })));
        return scrollPage(content);
    }

    private VBox buildEventsPage() {
        eventsTitle = new Label("Catalogue des evenements"); eventsTitle.setStyle("-fx-text-fill:#10233f; -fx-font-size:24px; -fx-font-weight:800;");
        eventsSubtitle = small("L'etudiant consulte les evenements avec photo et peut reserver.");
        eventListView = new ListView<>(); eventListView.setCellFactory(v -> new EventCell()); eventListView.setStyle("-fx-background-color:transparent; -fx-control-inner-background:transparent; -fx-padding:6;");
        VBox.setVgrow(eventListView, Priority.ALWAYS);

        searchField = input("Recherche par titre, lieu, categorie...");
        searchField.textProperty().addListener((obs, oldText, newText) -> loadEvents());
        searchField.setMaxWidth(320);

        sortField = new ComboBox<>(FXCollections.observableArrayList("Par défaut", "Date", "Capacite", "Categorie", "Lieu"));
        sortField.setValue("Par défaut");
        sortField.setOnAction(e -> loadEvents());
        sortField.setStyle(INPUT);
        sortField.setMaxWidth(180);

        Button clearButton = button("Effacer", SECONDARY, e -> {
            searchField.clear(); sortField.setValue("Par défaut"); loadEvents();
        });
        HBox controls = new HBox(12, searchField, sortField, clearButton, button("Actualiser", SECONDARY, e -> loadEvents()));
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox content = card(eventsTitle, eventsSubtitle, controls, eventListView);
        return scrollPage(content);
    }

    private VBox buildEventsPageTiqets() {
        eventsTitle = new Label("Evenements et reservations");
        eventsTitle.setStyle("-fx-text-fill:#0f2942; -fx-font-size:28px; -fx-font-weight:800;");
        eventsSubtitle = small("Consultez les evenements disponibles, verifiez les places restantes et reservez directement.");

        searchField = input("Rechercher un evenement, un lieu ou une categorie");
        searchField.textProperty().addListener((obs, oldText, newText) -> loadEvents());
        HBox.setHgrow(searchField, Priority.ALWAYS);

        sortField = new ComboBox<>(FXCollections.observableArrayList("Par defaut", "Date", "Capacite", "Categorie", "Lieu"));
        sortField.setValue("Par defaut");
        sortField.setOnAction(e -> loadEvents());
        sortField.setStyle(INPUT);
        sortField.setPrefWidth(170);

        Button clearButton = button("Effacer", SECONDARY, e -> {
            searchField.clear();
            sortField.setValue("Par defaut");
            loadEvents();
        });
        Button refreshButton = button("Actualiser", SECONDARY, e -> loadEvents());
        HBox controls = new HBox(12, searchField, sortField, clearButton, refreshButton);
        controls.setAlignment(Pos.CENTER_LEFT);

        Label topBrand = new Label("Espace Etudiant");
        topBrand.setStyle("-fx-text-fill:#12b7c8; -fx-font-size:30px; -fx-font-weight:900;");
        Label topLink = new Label("Evenements a venir");
        topLink.setStyle("-fx-text-fill:#415a78; -fx-font-size:12px; -fx-font-weight:700;");
        Label topHelp = new Label("Catalogue connecte a la base");
        topHelp.setStyle("-fx-text-fill:#415a78; -fx-font-size:12px;");
        Region navSpacer = new Region();
        HBox.setHgrow(navSpacer, Priority.ALWAYS);
        HBox topBar = new HBox(18, topBrand, navSpacer, topLink, topHelp);
        topBar.setAlignment(Pos.CENTER_LEFT);

        StackPane heroVisual = createImagePane(null, 1060, 280, "Evenements et activites etudiantes", "-fx-background-color: linear-gradient(to right, #2f699e, #19324d);");
        heroVisual.setStyle("-fx-background-radius:26; -fx-border-radius:26;");

        Label heroEyebrow = new Label("Plateforme etudiante");
        heroEyebrow.setStyle("-fx-text-fill:rgba(255,255,255,0.82); -fx-font-size:12px; -fx-font-weight:700;");
        Label heroTitle = new Label("Reservez vos evenements");
        heroTitle.setStyle("-fx-text-fill:white; -fx-font-size:34px; -fx-font-weight:900;");
        Label heroText = new Label("Chaque carte affiche les vraies informations stockees en base : date, lieu, categorie, capacite et disponibilite.");
        heroText.setWrapText(true);
        heroText.setStyle("-fx-text-fill:rgba(255,255,255,0.92); -fx-font-size:13px; -fx-font-weight:600;");
        VBox heroCopy = new VBox(8, heroEyebrow, heroTitle, heroText);
        heroCopy.setMaxWidth(440);
        heroCopy.setAlignment(Pos.BOTTOM_LEFT);
        heroCopy.setPadding(new Insets(0, 0, 22, 22));
        StackPane.setAlignment(heroCopy, Pos.BOTTOM_LEFT);
        heroVisual.getChildren().add(heroCopy);

        VBox heroCard = new VBox(16, topBar, controls, heroVisual);
        heroCard.setPadding(new Insets(24));
        heroCard.setStyle("-fx-background-color: rgba(255,255,255,0.97); -fx-background-radius:30; -fx-border-radius:30; -fx-border-color: rgba(18,183,200,0.15); -fx-effect: dropshadow(gaussian, rgba(10,31,68,0.10), 20, 0.18, 0, 8);");

        Label noticeLabel = new Label("Les informations affichees sont chargees depuis la base de donnees et mises a jour selon les reservations.");
        noticeLabel.setWrapText(true);
        noticeLabel.setStyle("-fx-text-fill:#20425f; -fx-font-size:13px; -fx-font-weight:700;");
        HBox noticeBar = new HBox(noticeLabel);
        noticeBar.setAlignment(Pos.CENTER_LEFT);
        noticeBar.setPadding(new Insets(14, 18, 14, 18));
        noticeBar.setStyle("-fx-background-color:white; -fx-background-radius:20; -fx-border-radius:20; -fx-border-color:#dfeaf4;");

        Label availabilityTitle = new Label("Verifiez la disponibilite");
        availabilityTitle.setStyle("-fx-text-fill:#0f2942; -fx-font-size:18px; -fx-font-weight:800;");
        availabilitySummaryLabel = new Label("Choisissez un jour pour voir les evenements ouverts.");
        availabilitySummaryLabel.setStyle("-fx-text-fill:#67809d; -fx-font-size:12px;");
        availabilityDatesRow = new HBox(10);
        availabilityDatesRow.setAlignment(Pos.CENTER_LEFT);

        HBox filterRow = new HBox(10, filterChip("Date reelle"), filterChip("Lieu reel"), filterChip("Capacite restante"), filterChip("Reservation etudiante"));
        filterRow.setAlignment(Pos.CENTER_LEFT);

        resultsCountLabel = new Label("0 evenements");
        resultsCountLabel.setStyle("-fx-text-fill:#20425f; -fx-font-size:12px; -fx-font-weight:700;");
        Label sortHint = new Label("Trie par : base de donnees");
        sortHint.setStyle("-fx-text-fill:#67809d; -fx-font-size:12px;");
        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);
        HBox metaRow = new HBox(10, resultsCountLabel, metaSpacer, sortHint);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        eventGrid = new TilePane();
        eventGrid.setHgap(18);
        eventGrid.setVgap(18);
        eventGrid.setPrefColumns(3);
        eventGrid.setTileAlignment(Pos.TOP_LEFT);
        eventGrid.setPrefTileWidth(320);
        eventGrid.setStyle("-fx-padding: 8 0 12 0;");

        VBox content = new VBox(18, heroCard, noticeBar, availabilityTitle, availabilitySummaryLabel, availabilityDatesRow, filterRow, metaRow, eventGrid);
        content.setPadding(new Insets(8));
        return scrollPage(content);
    }

    private VBox buildCalendarPageReal() {
        Label calendarTitle = new Label("Calendrier des evenements");
        calendarTitle.setStyle("-fx-text-fill:#10233f; -fx-font-size:24px; -fx-font-weight:800;");
        calendarSubtitle = small("Selectionnez une date contenant de vrais evenements de la base de donnees.");

        // Utiliser le DualMonthCalendarView pour afficher deux mois côte à côte
        DualMonthCalendarView dualCalendar = new DualMonthCalendarView(eventService, reservationService, this::loadCalendarForDate);
        updateDualCalendarAvailabilities(dualCalendar);

        VBox calendarContainer = new VBox(15);
        calendarContainer.setPadding(new Insets(15));
        calendarContainer.setStyle("-fx-border-color: #d7e7ff; -fx-border-radius: 8; -fx-background-color: white;");
        calendarContainer.getChildren().add(dualCalendar);

        calendarListView = new ListView<>();
        calendarListView.setCellFactory(v -> new CalendarCell());
        calendarListView.setStyle("-fx-background-color:transparent; -fx-control-inner-background:transparent; -fx-padding:6;");
        VBox.setVgrow(calendarListView, Priority.ALWAYS);

        Label eventsTitle = new Label("Evenements pour la date selectionnee");
        eventsTitle.setStyle("-fx-text-fill:#10233f; -fx-font-size:14px; -fx-font-weight:700;");
        calendarSelectedDateLabel = new Label("Choisissez une date du calendrier.");
        calendarSelectedDateLabel.setStyle("-fx-text-fill:#67809d; -fx-font-size:12px;");

        VBox eventsContainer = new VBox(12);
        eventsContainer.setPadding(new Insets(15));
        eventsContainer.setStyle("-fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-background-color: #F5F5F5;");
        eventsContainer.getChildren().addAll(eventsTitle, calendarSelectedDateLabel, calendarListView);
        VBox.setVgrow(eventsContainer, Priority.ALWAYS);

        Button actualizeButton = button("Actualiser", SECONDARY, e -> {
            updateDualCalendarAvailabilities(dualCalendar);
        });

        HBox controls = new HBox(12, actualizeButton);
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox content = card(calendarTitle, calendarSubtitle, controls, calendarContainer, eventsContainer);
        return scrollPage(content);
    }

    private VBox buildCalendarPage() {
        Label calendarTitle = new Label("📅 Calendrier des événements");
        calendarTitle.setStyle("-fx-text-fill:#10233f; -fx-font-size:24px; -fx-font-weight:800;");
        calendarSubtitle = small("Sélectionnez une date pour voir les événements disponibles. 🟠 Orange = Peu de places | 🔵 Cyan = Beaucoup | ⚫ Gris = Complet");

        // Créer le calendrier interactif
        CalendarPicker calendar = new CalendarPicker(selectedDate -> {
            System.out.println("Date sélectionnée: " + selectedDate);
            loadCalendarForDate(selectedDate);
        });

        // Charger les disponibilités
        updateCalendarAvailabilities(calendar);

        // Conteneur pour le calendrier
        VBox calendarContainer = new VBox(15);
        calendarContainer.setPadding(new Insets(15));
        calendarContainer.setStyle("-fx-border-color: #d7e7ff; -fx-border-radius: 8; -fx-background-color: white;");
        calendarContainer.getChildren().add(calendar);

        // Liste des événements pour la date sélectionnée
        calendarListView = new ListView<>();
        calendarListView.setCellFactory(v -> new CalendarCell());
        calendarListView.setStyle("-fx-background-color:transparent; -fx-control-inner-background:transparent; -fx-padding:6;");
        VBox.setVgrow(calendarListView, Priority.ALWAYS);

        Label eventsTitle = new Label("Événements pour la date sélectionnée");
        eventsTitle.setStyle("-fx-text-fill:#10233f; -fx-font-size:14px; -fx-font-weight:700;");

        VBox eventsContainer = new VBox(12);
        eventsContainer.setPadding(new Insets(15));
        eventsContainer.setStyle("-fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-background-color: #F5F5F5;");
        eventsContainer.getChildren().addAll(eventsTitle, calendarListView);
        VBox.setVgrow(eventsContainer, Priority.ALWAYS);

        Button actualizeButton = button("Actualiser", SECONDARY, e -> {
            updateCalendarAvailabilities(calendar);
            LocalDate selectedDate = calendar.getSelectedDate();
            if (selectedDate != null) {
                loadCalendarForDate(selectedDate);
            }
        });

        HBox controls = new HBox(12, actualizeButton);
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox content = card(calendarTitle, calendarSubtitle, controls, calendarContainer, eventsContainer);
        return scrollPage(content);
    }

    /**
     * Charger les disponibilités pour le calendrier
     */
    private void updateCalendarAvailabilities(CalendarPicker calendar) {
        try {
            List<Event> allEvents = eventService.getAllEvents();
            Map<LocalDate, Integer> availabilityMap = new HashMap<>();
            LocalDate firstAvailableDate = null;

            for (Event event : allEvents) {
                LocalDate eventDate = event.getDateEvent().toLocalDate();
                if (firstAvailableDate == null || eventDate.isBefore(firstAvailableDate)) {
                    firstAvailableDate = eventDate;
                }
                int reserved = reservationService.getReservationCountByEvent(event.getId());
                int available = event.getCapacite() - reserved;

                // Déterminer le code couleur
                int availability;
                if (available <= 0) {
                    availability = 2; // Complet (gris)
                } else if (available < event.getCapacite() * 0.25) {
                    availability = 0; // Peu d'options (orange)
                } else {
                    availability = 1; // Beaucoup d'options (cyan)
                }

                // Garder le plus restrictif pour cette date (le PIRE état)
                if (availabilityMap.containsKey(eventDate)) {
                    int current = availabilityMap.get(eventDate);
                    // Priorité: 2 (Complet) > 0 (Peu d'options) > 1 (Beaucoup) > 3 (Disponible)
                    if (current == 2 || availability == 2) {
                        availabilityMap.put(eventDate, 2);
                    } else if (current == 0 || availability == 0) {
                        availabilityMap.put(eventDate, 0);
                    } else {
                        availabilityMap.put(eventDate, Math.min(current, availability));
                    }
                } else {
                    availabilityMap.put(eventDate, availability);
                }
            }

            calendar.setDateAvailabilities(availabilityMap);
            if (calendar.getSelectedDate() == null) {
                LocalDate defaultDate = firstAvailableDate != null ? firstAvailableDate : LocalDate.now();
                calendar.setSelectedDate(defaultDate);
                loadCalendarForDate(defaultDate);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement disponibilités: " + e.getMessage());
        }
    }

    private void updateDualCalendarAvailabilities(DualMonthCalendarView dualCalendar) {
        try {
            List<Event> allEvents = eventService.getAllEvents();
            Map<LocalDate, Integer> availabilityMap = new HashMap<>();

            for (Event event : allEvents) {
                LocalDate eventDate = event.getDateEvent().toLocalDate();
                int reserved = reservationService.getReservationCountByEvent(event.getId());
                int available = event.getCapacite() - reserved;

                // Déterminer le code couleur
                int availability;
                if (available <= 0) {
                    availability = 2; // Complet (gris)
                } else if (available < event.getCapacite() * 0.25) {
                    availability = 0; // Peu d'options (orange)
                } else {
                    availability = 1; // Beaucoup d'options (cyan)
                }

                // Garder le plus restrictif pour cette date (le PIRE état)
                if (availabilityMap.containsKey(eventDate)) {
                    int current = availabilityMap.get(eventDate);
                    // Priorité: 2 (Complet) > 0 (Peu d'options) > 1 (Beaucoup) > 3 (Disponible)
                    if (current == 2 || availability == 2) {
                        availabilityMap.put(eventDate, 2);
                    } else if (current == 0 || availability == 0) {
                        availabilityMap.put(eventDate, 0);
                    } else {
                        availabilityMap.put(eventDate, Math.min(current, availability));
                    }
                } else {
                    availabilityMap.put(eventDate, availability);
                }
            }

            dualCalendar.setDateAvailabilities(availabilityMap);
        } catch (Exception e) {
            System.err.println("Erreur chargement disponibilités dual calendar: " + e.getMessage());
        }
    }

    /**
     * Charger les événements pour une date spécifique
     */
    private void loadCalendarForDate(LocalDate selectedDate) {
        if (calendarListView == null) {
            return;
        }
        try {
            LocalDateTime from = selectedDate.atStartOfDay();
            LocalDateTime to = selectedDate.plusDays(1).atStartOfDay();

            List<Event> events = eventService.getEventsInRange(from, to);
            calendarListView.getItems().setAll(events);
            if (calendarSelectedDateLabel != null) {
                calendarSelectedDateLabel.setText(
                        events.isEmpty()
                                ? "Aucun evenement le " + selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                : events.size() + (events.size() > 1 ? " evenements le " : " evenement le ") + selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                );
            }
        } catch (SQLException e) {
            calendarListView.getItems().clear();
            showError("Chargement impossible", e.getMessage());
        }
    }

    private VBox buildStatsPageGlobal() throws SQLException {
        VBox statsBox = new VBox(24);
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
            Label resTitle = title("Réservations par catégorie", 16);
            resBox.getChildren().add(resTitle);

            javafx.scene.chart.PieChart resPieChart = new javafx.scene.chart.PieChart();
            resPieChart.setTitle("Réservations");
            resPieChart.setLegendSide(javafx.geometry.Side.RIGHT);
            resPieChart.setPrefSize(400, 300);

            int totalRes = reservationsByCategory.values().stream().mapToInt(Integer::intValue).sum();
            for (Map.Entry<String, Integer> entry : reservationsByCategory.entrySet()) {
                String cat = entry.getKey().isEmpty() ? "Non catégorisé" : entry.getKey();
                int count = entry.getValue();
                double percentage = totalRes > 0 ? (100.0 * count / totalRes) : 0;
                resPieChart.getData().add(new javafx.scene.chart.PieChart.Data(cat + " (" + count + ")", percentage));
            }
            resBox.getChildren().add(resPieChart);
            chartsRow.getChildren().add(resBox);
        }

        if (!eventsByCategory.isEmpty()) {
            VBox evtBox = new VBox(12);
            Label evtTitle = title("Événements par catégorie", 16);
            evtBox.getChildren().add(evtTitle);

            javafx.scene.chart.PieChart evtPieChart = new javafx.scene.chart.PieChart();
            evtPieChart.setTitle("Événements");
            evtPieChart.setLegendSide(javafx.geometry.Side.RIGHT);
            evtPieChart.setPrefSize(400, 300);

            int totalEvt = eventsByCategory.values().stream().mapToInt(Integer::intValue).sum();
            for (Map.Entry<String, Integer> entry : eventsByCategory.entrySet()) {
                String cat = entry.getKey().isEmpty() ? "Non catégorisé" : entry.getKey();
                int count = entry.getValue();
                double percentage = totalEvt > 0 ? (100.0 * count / totalEvt) : 0;
                evtPieChart.getData().add(new javafx.scene.chart.PieChart.Data(cat + " (" + count + ")", percentage));
            }
            evtBox.getChildren().add(evtPieChart);
            chartsRow.getChildren().add(evtBox);
        }

        statsBox.getChildren().add(chartsRow);

        VBox content = card(statsBox);
        return scrollPage(content);
    }

    private VBox buildReservationsPage() {
        reservationListView = new ListView<>(); reservationListView.setCellFactory(v -> new ReservationCell()); reservationListView.setStyle("-fx-background-color:transparent; -fx-control-inner-background:transparent; -fx-padding:6;");
        VBox.setVgrow(reservationListView, Priority.ALWAYS);

        VBox statsSection = buildReservationStats();

        VBox content = card(title("Reservations des etudiants", 24), small("Admin voit ici qui reserve chaque evenement."), button("Actualiser", SECONDARY, e -> loadReservations()), reservationListView, statsSection);
        return scrollPage(content);
    }

    private VBox buildReservationStats() {
        VBox statsBox = new VBox(20);
        statsBox.setPadding(new Insets(24));
        statsBox.setStyle("-fx-background-color: rgba(15,105,255,0.08); -fx-border-color: rgba(15,105,255,0.2); -fx-border-radius: 12; -fx-background-radius: 12;");
        
        Label statsTitle = title("Réservations par catégorie", 16);
        statsBox.getChildren().add(statsTitle);
        
        try {
            Map<String, Integer> reservationsByCategory = reservationService.getReservationCountByCategory();
            
            if (!reservationsByCategory.isEmpty()) {
                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                for (Map.Entry<String, Integer> entry : reservationsByCategory.entrySet()) {
                    String cat = entry.getKey().isEmpty() ? "Non catégorisé" : entry.getKey();
                    int count = entry.getValue();
                    if (count > 0) {
                        pieData.add(new PieChart.Data(cat, count));
                    }
                }
                
                if (!pieData.isEmpty()) {
                    PieChart pieChart = new PieChart(pieData);
                    pieChart.setLegendSide(Side.RIGHT);
                    pieChart.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                    pieChart.setPrefSize(500, 300);
                    pieChart.setAnimated(false);
                    pieChart.setLabelsVisible(true);
                    
                    int totalRes = reservationsByCategory.values().stream().mapToInt(Integer::intValue).sum();
                    
                    VBox legendBox = new VBox(10);
                    legendBox.setStyle("-fx-padding: 16;");
                    
                    for (Map.Entry<String, Integer> entry : reservationsByCategory.entrySet()) {
                        String cat = entry.getKey().isEmpty() ? "Non catégorisé" : entry.getKey();
                        int count = entry.getValue();
                        double percentage = totalRes > 0 ? (100.0 * count / totalRes) : 0;
                        
                        Label catLabel = new Label(String.format("%s: %d (%.1f%%)", cat, count, percentage));
                        catLabel.setStyle("-fx-text-fill: #1c4f96; -fx-font-size: 13px; -fx-font-weight: 700;");
                        legendBox.getChildren().add(catLabel);
                    }
                    
                    Label totalLabel = new Label("Total: " + totalRes + " réservations");
                    totalLabel.setStyle("-fx-text-fill: #0f69ff; -fx-font-size: 14px; -fx-font-weight: 700; -fx-padding: 12 0 0 0;");
                    legendBox.getChildren().add(totalLabel);
                    
                    HBox chartContainer = new HBox(20, pieChart, legendBox);
                    chartContainer.setAlignment(Pos.CENTER_LEFT);
                    statsBox.getChildren().add(chartContainer);
                } else {
                    Label noData = new Label("Aucune réservation pour le moment");
                    noData.setStyle("-fx-text-fill: #637a97; -fx-font-size: 13px;");
                    statsBox.getChildren().add(noData);
                }
            } else {
                Label noData = new Label("Aucune réservation pour le moment");
                noData.setStyle("-fx-text-fill: #637a97; -fx-font-size: 13px;");
                statsBox.getChildren().add(noData);
            }
        } catch (SQLException e) {
            Label error = new Label("Erreur chargement statistique: " + e.getMessage());
            error.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12px;");
            statsBox.getChildren().add(error);
        }
        
        return statsBox;
    }

    private void initializeDatabase() {
        try {
            authService.initializeUsers();
            eventService.createTableIfNotExists();
            reservationService.initializeReservations();
            eventEngagementService.initializeTables();
        } catch (SQLException e) {
            showError("Initialisation BDD impossible", e.getMessage());
        }
    }

    private void attemptLogin() {
        if (usernameField.getText().isBlank() || passwordField.getText().isBlank()) { showWarning("Utilisateur et mot de passe sont obligatoires."); return; }
        try {
            AppUser user = authService.login(usernameField.getText().trim(), passwordField.getText().trim());
            if (user == null) { showError("Connexion refusee", "Identifiants invalides."); return; }
            currentUser = user;
            applyRole();
            resetForm();
            loadEvents();
            loadCalendar();
            loadReservations();
            showPage(currentUser.isAdmin() ? formPage : eventsPage);
        } catch (SQLException e) { showError("Connexion impossible", e.getMessage()); }
    }

    private void applyRole() {
        boolean admin = currentUser != null && currentUser.isAdmin();
        userBadge.setText(currentUser.getUsername() + " | " + currentUser.getRole());
        userBadge.setVisible(true); userBadge.setManaged(true);
        logoutButton.setVisible(true); logoutButton.setManaged(true);
        eventsHeaderButton.setVisible(true); eventsHeaderButton.setManaged(true);
        addHeaderButton.setVisible(admin); addHeaderButton.setManaged(admin);
        reservationsHeaderButton.setVisible(admin); reservationsHeaderButton.setManaged(admin);
        eventsTitle.setText(admin ? "Back office des evenements" : "Catalogue des evenements");
        eventsSubtitle.setText(admin ? "Admin peut modifier, supprimer et suivre les reservations." : "L'etudiant consulte les evenements avec photo et peut reserver.");
    }

    private void showLoginPage() {
        for (Node n : List.of(userBadge, addHeaderButton, eventsHeaderButton, reservationsHeaderButton, logoutButton)) { n.setVisible(false); n.setManaged(false); }
        showPage(loginPage);
    }

    private void logout() {
        currentUser = null; editingEvent = null; reservedEventIds.clear(); reservationCounts.clear(); likedEventIds.clear(); likeCounts.clear(); reviewCounts.clear(); averageRatings.clear();
        usernameField.clear(); passwordField.clear(); resetForm(); showLoginPage();
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
        if (titleField.getText().isBlank() || descriptionArea.getText().isBlank() || locationField.getText().isBlank() || datePicker.getValue() == null) { 
            showWarning("Tous les champs obligatoires doivent etre remplis."); 
            return null; 
        }
        
        int cap;
        try { 
            cap = Integer.parseInt(capacityField.getText().trim()); 
        } catch (Exception e) { 
            showWarning("La capacite doit etre un entier."); 
            return null; 
        }
        if (cap <= 0) { 
            showWarning("La capacite doit etre superieure a zero."); 
            return null; 
        }
        
        // Obtenir l'heure depuis le TimePicker
        LocalTime time = timePicker.getTimeAsLocalTime();
        LocalDateTime eventDateTime = LocalDateTime.of(datePicker.getValue(), time);
        
        // Obtenir le chemin de l'image (stocké dans imagePathField)
        String imagePath = imagePathField.getText().isBlank() ? "" : imagePathField.getText();
        
        return new Event(titleField.getText().trim(), descriptionArea.getText().trim(), eventDateTime, 
                locationField.getText().trim(), cap, categoryField.getValue(), imagePath, null);
    }

    private void loadEvents() {
        try {
            reservationCounts.clear(); reservationCounts.putAll(reservationService.getReservationCountsByEvent());
            likeCounts.clear(); likeCounts.putAll(eventEngagementService.getLikeCountsByEvent());
            reviewCounts.clear(); reviewCounts.putAll(eventEngagementService.getReviewCountsByEvent());
            averageRatings.clear(); averageRatings.putAll(eventEngagementService.getAverageRatingsByEvent());
            reservedEventIds.clear();
            likedEventIds.clear();
            if (currentUser != null && !currentUser.isAdmin()) {
                reservedEventIds.addAll(reservationService.getReservedEventIdsByUser(currentUser.getId()));
                likedEventIds.addAll(eventEngagementService.getLikedEventIdsByUser(currentUser.getId()));
            }
            String query = searchField == null ? null : searchField.getText().trim();
            String sortBy = sortField == null ? null : sortField.getValue();
            List<Event> events = eventService.getEvents(query, sortBy);
            if (eventGrid != null) {
                eventGrid.getChildren().setAll(events.stream().map(this::buildEventCard).toList());
            }
            updateAvailabilityStrip(events);
        } catch (SQLException e) {
            if (eventGrid != null) {
                eventGrid.getChildren().clear();
            }
            showError("Chargement impossible", e.getMessage());
        }
    }
    
    private void updateEventGrid(List<Event> events) {
        try {
            reservationCounts.clear(); reservationCounts.putAll(reservationService.getReservationCountsByEvent());
            likeCounts.clear(); likeCounts.putAll(eventEngagementService.getLikeCountsByEvent());
            reviewCounts.clear(); reviewCounts.putAll(eventEngagementService.getReviewCountsByEvent());
            averageRatings.clear(); averageRatings.putAll(eventEngagementService.getAverageRatingsByEvent());
            reservedEventIds.clear();
            likedEventIds.clear();
            if (currentUser != null && !currentUser.isAdmin()) {
                reservedEventIds.addAll(reservationService.getReservedEventIdsByUser(currentUser.getId()));
                likedEventIds.addAll(eventEngagementService.getLikedEventIdsByUser(currentUser.getId()));
            }
            if (eventGrid != null) {
                eventGrid.getChildren().setAll(events.stream().map(this::buildEventCard).toList());
            }
            if (resultsCountLabel != null) {
                resultsCountLabel.setText(events.size() + (events.size() > 1 ? " evenements" : " evenement"));
            }
        } catch (SQLException e) {
            if (eventGrid != null) {
                eventGrid.getChildren().clear();
            }
            showError("Erreur", e.getMessage());
        }
    }

    private void loadCalendar() {
        // La nouvelle page calendrier se met à jour automatiquement
        // Cette méthode est conservée pour compatibilité mais n'a plus besoin de faire grand-chose
        if (eventCalendar != null) {
            updateCalendarAvailabilities(eventCalendar);
        } else if (calendarListView != null) {
            // Charger les événements d'aujourd'hui par défaut
            loadCalendarForDate(LocalDate.now());
        }
    }

    private void loadReservations() {
        try { reservationListView.getItems().setAll(reservationService.getAllReservations()); }
        catch (SQLException e) { reservationListView.getItems().clear(); showError("Chargement des reservations impossible", e.getMessage()); }
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
                loadCalendar();
                loadReservations(); 
            }
            catch (SQLException e) { showError("Reservation impossible", e.getMessage()); }
        }
    }

    private void edit(Event event) {
        if (currentUser == null || !currentUser.isAdmin()) { showError("Acces refuse", "Seul l'admin peut modifier."); return; }
        editingEvent = event; 
        formTitle.setText("Modifier l'evenement #" + event.getId()); 
        saveButton.setText("Enregistrer");
        cancelEditButton.setVisible(true); 
        cancelEditButton.setManaged(true);
        titleField.setText(event.getTitre()); 
        descriptionArea.setText(event.getDescription()); 
        locationField.setText(event.getLieu());
        datePicker.setValue(event.getDateEvent().toLocalDate()); 
        timePicker.setTime(event.getDateEvent().toLocalTime());
        capacityField.setText(String.valueOf(event.getCapacite())); 
        categoryField.setValue(event.getCategorie() == null || event.getCategorie().isBlank() ? "yoga" : event.getCategorie());
        
        // Restaurer les infos de l'image
        String imagePath = event.getImage() == null ? "" : event.getImage();
        if (!imagePath.isBlank()) {
            imagePathField.setText(imagePath);
            updateImagePreview(imagePath);
        } else {
            imageIdField.setText("");
            imagePathField.setText("");
            imageThumbnailLabel.setText("Aucune image sélectionnée");
        }
        
        showPage(formPage);
    }

    private void delete(Event event) {
        if (currentUser == null || !currentUser.isAdmin()) { showError("Acces refuse", "Seul l'admin peut supprimer."); return; }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION); alert.setHeaderText("Supprimer l'evenement #" + event.getId()); alert.setContentText("Confirmer la suppression de " + event.getTitre() + " ?");
        Optional<ButtonType> ok = alert.showAndWait();
        if (ok.isPresent() && ok.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            try { reservationService.deleteReservationsForEvent(event.getId()); eventEngagementService.deleteEngagementForEvent(event.getId()); eventService.deleteEvent(event.getId()); loadEvents(); loadReservations(); }
            catch (SQLException e) { showError("Suppression impossible", e.getMessage()); }
        }
    }

    private void resetForm() {
        editingEvent = null; 
        formTitle.setText("Nouvel evenement"); 
        saveButton.setText("Ajouter l'evenement");
        cancelEditButton.setVisible(false); 
        cancelEditButton.setManaged(false);
        titleField.clear(); 
        descriptionArea.clear(); 
        locationField.clear(); 
        datePicker.setValue(LocalDate.now().plusDays(1));
        timePicker.setTime(LocalTime.of(10, 0));
        capacityField.setText("20"); 
        categoryField.setValue("yoga"); 
        imageIdField.setText("");
        imagePathField.setText("");
        imageThumbnailLabel.setText("Aucune image sélectionnée");
    }

    private void chooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image pour l'événement");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif"));
        
        File file = chooser.showOpenDialog(primaryStage);
        if (file != null) {
            try {
                // Uploader l'image avec ImageManager
                ImageManager.ImageInfo imageInfo = ImageManager.uploadImage(file);
                
                // Mettre à jour les champs
                imageIdField.setText(imageInfo.id);
                imagePathField.setText(imageInfo.path);
                
                // Afficher un aperçu
                updateImagePreview(imageInfo.path);
                
            } catch (IOException e) {
                showError("Erreur upload image", e.getMessage());
            }
        }
    }
    
    /**
     * Mettre à jour l'aperçu de l'image
     */
    private void updateImagePreview(String imagePath) {
        if (imagePath != null && !imagePath.isBlank() && ImageManager.imageExists(imagePath)) {
            String fileName = new File(imagePath).getName();
            imageThumbnailLabel.setText("✓ Image chargée: " + fileName);
            imageThumbnailLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px; -fx-padding: 10; -fx-font-weight: bold;");
        } else {
            imageThumbnailLabel.setText("Aucune image sélectionnée");
            imageThumbnailLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 12px; -fx-padding: 10;");
        }
    }

    private void showPage(VBox page) { pageContainer.getChildren().setAll(page); }

    private GridPane formGrid() { GridPane g = new GridPane(); g.setHgap(14); g.setVgap(14); return g; }
    private void addRow(GridPane g, int row, String label, Node field) { Label l = new Label(label); l.setStyle("-fx-text-fill:#29496f; -fx-font-size:13px; -fx-font-weight:700;"); g.add(l, 0, row); g.add(field, 1, row); GridPane.setHgrow(field, Priority.ALWAYS); }
    private TextField input(String prompt) { TextField f = new TextField(); f.setPromptText(prompt); f.setStyle(INPUT); return f; }
    private Label small(String text) { Label l = new Label(text); l.setStyle("-fx-text-fill:#637a97; -fx-font-size:13px;"); return l; }
    private Label title(String text, int size) { Label l = new Label(text); l.setStyle("-fx-text-fill:#10233f; -fx-font-size:" + size + "px; -fx-font-weight:800;"); return l; }
    private Button button(String text, String style, javafx.event.EventHandler<javafx.event.ActionEvent> handler) { Button b = new Button(text); b.setStyle(style); b.setOnAction(handler); return b; }
    private VBox card(Node... nodes) { VBox b = new VBox(16, nodes); b.setPadding(new Insets(28)); b.setStyle(CARD); return b; }
    private VBox scrollPage(VBox content) { VBox page = new VBox(content); page.setPadding(new Insets(8)); ScrollPane s = new ScrollPane(page); s.setFitToWidth(true); s.setPannable(true); s.setStyle("-fx-background-color:transparent; -fx-background:transparent;"); s.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); VBox wrapper = new VBox(s); VBox.setVgrow(s, Priority.ALWAYS); return wrapper; }
    private void showWarning(String m) { alert(Alert.AlertType.WARNING, "Validation", m); }
    private void showError(String t, String m) { alert(Alert.AlertType.ERROR, t, m); }
    private void showInfo(String t, String m) { alert(Alert.AlertType.INFORMATION, t, m); }
    private void alert(Alert.AlertType type, String title, String message) { Alert a = new Alert(type); a.setHeaderText(title); a.setContentText(message); a.showAndWait(); }

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

    private StackPane createImagePane(String path, double width, double height, String fallbackText, String fallbackStyle) {
        StackPane box = new StackPane();
        box.setMinSize(width, height);
        box.setPrefSize(width, height);
        box.setMaxWidth(width);
        box.setStyle(fallbackStyle + " -fx-background-radius:22;");
        if (path != null && !path.isBlank()) {
            File file = new File(path);
            if (!file.isAbsolute()) {
                file = new File(System.getProperty("user.dir"), path);
            }
            if (file.exists()) {
                ImageView imageView = new ImageView(new Image(file.toURI().toString(), width, height, false, true));
                imageView.setFitWidth(width);
                imageView.setFitHeight(height);
                imageView.setPreserveRatio(false);
                box.getChildren().add(imageView);
                return box;
            }
        }
        Label placeholder = new Label(fallbackText);
        placeholder.setStyle("-fx-text-fill:white; -fx-font-size:16px; -fx-font-weight:800;");
        box.getChildren().add(placeholder);
        return box;
    }

    private Label filterChip(String text) {
        Label chip = new Label(text);
        chip.setStyle("-fx-background-color:white; -fx-background-radius:999; -fx-border-radius:999; -fx-border-color:#d9e7f0; -fx-padding:8 14 8 14; -fx-text-fill:#35546e; -fx-font-size:12px; -fx-font-weight:700;");
        return chip;
    }

    private Node buildEventCard(Event event) {
        int reserved = reservationCounts.getOrDefault(event.getId(), 0);
        int remaining = Math.max(event.getCapacite() - reserved, 0);
        int totalLikes = likeCounts.getOrDefault(event.getId(), 0);
        int totalReviews = reviewCounts.getOrDefault(event.getId(), 0);
        double avgRating = averageRatings.getOrDefault(event.getId(), 0.0);
        boolean alreadyReserved = reservedEventIds.contains(event.getId());
        boolean alreadyLiked = likedEventIds.contains(event.getId());
        boolean full = reserved >= event.getCapacite();
        boolean admin = currentUser != null && currentUser.isAdmin();

        StackPane imagePane = createImagePane(
                event.getImage(),
                320,
                190,
                event.getTitre(),
                "-fx-background-color: linear-gradient(to bottom right,#44b9d0,#1b5f8e);"
        );
        imagePane.setCursor(javafx.scene.Cursor.HAND);
        imagePane.setOnMouseClicked(e -> showEventDetails(event));

        Label categoryTag = new Label((event.getCategorie() == null || event.getCategorie().isBlank() ? "Experience" : event.getCategorie()).toUpperCase());
        categoryTag.setStyle("-fx-background-color: rgba(255,255,255,0.92); -fx-background-radius:999; -fx-padding:6 10 6 10; -fx-text-fill:#1a4b67; -fx-font-size:10px; -fx-font-weight:900;");
        StackPane.setAlignment(categoryTag, Pos.TOP_LEFT);
        StackPane.setMargin(categoryTag, new Insets(12, 0, 0, 12));
        imagePane.getChildren().add(categoryTag);

        String dateText = event.getDateEvent().format(DateTimeFormatter.ofPattern("dd MMM")) + " | " + event.getDateEvent().format(DateTimeFormatter.ofPattern("HH:mm"));
        Label venue = new Label(event.getLieu());
        venue.setStyle("-fx-text-fill:#6a7f94; -fx-font-size:11px; -fx-font-weight:700;");
        Label title = new Label(event.getTitre());
        title.setWrapText(true);
        title.setStyle("-fx-text-fill:#10233f; -fx-font-size:17px; -fx-font-weight:900;");
        Label description = new Label(shortDescription(event.getDescription()));
        description.setWrapText(true);
        description.setStyle("-fx-text-fill:#536b84; -fx-font-size:12px;");
        Label details = new Label(dateText + "   |   " + remaining + " places restantes");
        details.setStyle("-fx-text-fill:#1c658f; -fx-font-size:11px; -fx-font-weight:700;");
        Label status = new Label(full ? "Complet" : (alreadyReserved ? "Reservation confirmee" : "Disponible"));
        status.setStyle("-fx-text-fill:" + (full ? "#b74949" : "#0e8f6a") + "; -fx-font-size:11px; -fx-font-weight:800;");
        Label reservationInfo = new Label(reserved + " reserve(s) sur " + event.getCapacite() + " place(s)");
        reservationInfo.setStyle("-fx-text-fill:#5d7388; -fx-font-size:11px; -fx-font-weight:700;");
        Label engagementInfo = new Label(totalLikes + " like(s)   |   " + totalReviews + " avis   |   " + formatRating(avgRating));
        engagementInfo.setStyle("-fx-text-fill:#5d7388; -fx-font-size:11px; -fx-font-weight:700;");
        Label dbInfo = new Label("Categorie : " + (event.getCategorie() == null || event.getCategorie().isBlank() ? "-" : event.getCategorie()));
        dbInfo.setStyle("-fx-text-fill:#7b8da0; -fx-font-size:10px;");

        VBox body = new VBox(8, venue, title, description, details, status, reservationInfo, engagementInfo, dbInfo);
        body.setPadding(new Insets(14, 16, 16, 16));

        VBox card = new VBox(imagePane, body);
        card.setPrefWidth(320);
        card.setMaxWidth(320);
        card.setStyle("-fx-background-color:white; -fx-background-radius:22; -fx-border-radius:22; -fx-border-color:#dfe8f2; -fx-effect: dropshadow(gaussian, rgba(13,43,74,0.08), 14, 0.18, 0, 6);");

        if (admin) {
            HBox adminRow = new HBox(8, button("Modifier", SECONDARY, e -> edit(event)), button("Supprimer", DANGER, e -> delete(event)));
            adminRow.setPadding(new Insets(0, 16, 16, 16));
            card.getChildren().add(adminRow);
        } else {
            Button likeButton = button(alreadyLiked ? "Retirer like" : "Like", alreadyLiked ? SECONDARY : PRIMARY, e -> toggleLike(event));
            Button reserveButton = button(alreadyReserved ? "Deja reserve" : (full ? "Complet" : "Reserver"), alreadyReserved || full ? SECONDARY : PRIMARY, e -> reserve(event));
            reserveButton.setDisable(alreadyReserved || full);
            Button reviewButton = button("Avis", SECONDARY, e -> {
                showEventDetails(event);
            });
            HBox actionRow = new HBox(8, likeButton, reviewButton);
            actionRow.setPadding(new Insets(0, 16, 8, 16));
            card.getChildren().add(actionRow);
            reserveButton.setMaxWidth(Double.MAX_VALUE);
            VBox.setMargin(reserveButton, new Insets(0, 16, 16, 16));
            card.getChildren().add(reserveButton);
        }

        return card;
    }

    private void showEventDetails(Event event) {
        int reserved = reservationCounts.getOrDefault(event.getId(), 0);
        int remaining = Math.max(event.getCapacite() - reserved, 0);
        int totalLikes = likeCounts.getOrDefault(event.getId(), 0);
        int totalReviews = reviewCounts.getOrDefault(event.getId(), 0);
        double avgRating = averageRatings.getOrDefault(event.getId(), 0.0);
        boolean alreadyReserved = reservedEventIds.contains(event.getId());
        boolean alreadyLiked = likedEventIds.contains(event.getId());
        boolean full = reserved >= event.getCapacite();
        boolean admin = currentUser != null && currentUser.isAdmin();
        EventReview myReview = null;

        if (!admin && currentUser != null) {
            try {
                myReview = eventEngagementService.getReviewByEventAndUser(event.getId(), currentUser.getId());
            } catch (SQLException e) {
                showError("Avis impossible", e.getMessage());
            }
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Details de l'evenement");
        dialog.setHeaderText(null);
        if (primaryStage != null) {
            dialog.initOwner(primaryStage);
        }
        dialog.getDialogPane().setStyle("-fx-background-color:#fffaf5;");

        StackPane imagePane = createImagePane(
                event.getImage(),
                500,
                240,
                event.getTitre(),
                "-fx-background-color: linear-gradient(to bottom right,#d7c2ab,#8e745e);"
        );
        imagePane.setStyle("-fx-background-radius:8; -fx-border-radius:8;");

        HBox infoStrip = new HBox(12,
                detailFact("🎭", event.getCategorie() == null || event.getCategorie().isBlank() ? "Evenement" : capitalize(event.getCategorie())),
                detailFact("⏱", formatDuration(event.getDurationMinutes())),
                detailFact("📅", event.getDateEvent().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH))),
                detailFact("🕘", event.getDateEvent().format(DateTimeFormatter.ofPattern("HH:mm")))
        );
        infoStrip.setAlignment(Pos.CENTER);
        infoStrip.setPadding(new Insets(4, 0, 12, 0));

        Separator separator = new Separator();

        Label descTitle = new Label("Description");
        descTitle.setStyle("-fx-text-fill:#10233f; -fx-font-size:16px; -fx-font-weight:900;");
        Label descLabel = new Label(event.getDescription() == null || event.getDescription().isBlank() ? "Aucune description disponible." : event.getDescription());
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill:#3f5060; -fx-font-size:13px; -fx-line-spacing: 2px;");

        Label titleLabel = new Label(event.getTitre());
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-text-fill:#2d2b2a; -fx-font-size:23px; -fx-font-weight:900;");

        Label subMeta = new Label(
                event.getLieu() + "   |   "
                        + "Reservations " + reserved + "/" + event.getCapacite()
                        + "   |   "
                        + "Places restantes " + remaining
        );
        subMeta.setWrapText(true);
        subMeta.setStyle("-fx-text-fill:#7b6758; -fx-font-size:14px; -fx-font-weight:700;");

        Label socialLabel = new Label(
                "Statut : " + (full ? "Complet" : (alreadyReserved ? "Reservation deja effectuee" : "Disponible"))
                        + "   |   Likes : " + totalLikes
                        + "   |   Avis : " + totalReviews
                        + "   |   Note moyenne : " + formatRating(avgRating)
        );
        socialLabel.setWrapText(true);
        socialLabel.setStyle("-fx-text-fill:" + (full ? "#a54848" : "#7b6758") + "; -fx-font-size:13px; -fx-font-weight:800;");

        VBox myReviewBox = new VBox(12);
        ComboBox<Integer> inlineRatingBox = null;
        TextArea inlineCommentArea = null;
        if (!admin) {
            Label myReviewTitle = new Label("Ma note et mon avis");
            myReviewTitle.setStyle("-fx-text-fill:#10233f; -fx-font-size:17px; -fx-font-weight:900;");
            Label myReviewInfo = new Label(
                    myReview == null
                            ? "Vous n'avez pas encore donne de note pour cet evenement."
                            : "Votre note actuelle : " + myReview.getRating() + "/5"
            );
            myReviewInfo.setStyle("-fx-text-fill:#4f6478; -fx-font-size:13px; -fx-font-weight:700;");
            Label ratingLabel = new Label("Choisissez votre note");
            ratingLabel.setStyle("-fx-text-fill:#7b6758; -fx-font-size:13px; -fx-font-weight:800;");
            inlineRatingBox = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5));
            inlineRatingBox.setValue(myReview == null ? 5 : myReview.getRating());
            inlineRatingBox.setMaxWidth(Double.MAX_VALUE);
            inlineRatingBox.setStyle(INPUT);

            Label commentLabel = new Label("Ecrivez votre avis");
            commentLabel.setStyle("-fx-text-fill:#7b6758; -fx-font-size:13px; -fx-font-weight:800;");
            inlineCommentArea = new TextArea(myReview == null ? "" : myReview.getComment());
            inlineCommentArea.setPromptText("Ecrivez ici ce que vous avez pense de cet evenement...");
            inlineCommentArea.setWrapText(true);
            inlineCommentArea.setPrefRowCount(5);
            inlineCommentArea.setStyle("-fx-control-inner-background:#ffffff; -fx-background-color:#ffffff; -fx-border-color:#d9c8b8; -fx-border-radius:14; -fx-background-radius:14; -fx-padding:12;");

            Label helper = new Label("Enregistrez votre note et votre avis directement depuis cette fiche.");
            helper.setStyle("-fx-text-fill:#8d7868; -fx-font-size:12px;");

            myReviewBox.getChildren().addAll(myReviewTitle, myReviewInfo, ratingLabel, inlineRatingBox, commentLabel, inlineCommentArea, helper);
            myReviewBox.setPadding(new Insets(14));
            myReviewBox.setStyle("-fx-background-color:#fff7ef; -fx-background-radius:20; -fx-border-radius:20; -fx-border-color:#ead8c7;");
        }

        Label reviewTitle = new Label("Avis des etudiants");
        reviewTitle.setStyle("-fx-text-fill:#10233f; -fx-font-size:16px; -fx-font-weight:900;");
        VBox reviewsBox = buildReviewsBox(event);
        ScrollPane reviewsScroll = new ScrollPane(reviewsBox);
        reviewsScroll.setFitToWidth(true);
        reviewsScroll.setPrefViewportHeight(140);
        reviewsScroll.setStyle("-fx-background-color:transparent; -fx-background:transparent;");

        VBox content = admin
                ? new VBox(14, infoStrip, separator, titleLabel, subMeta, imagePane, socialLabel, descTitle, descLabel, reviewTitle, reviewsScroll)
                : new VBox(14, infoStrip, separator, titleLabel, subMeta, imagePane, socialLabel, descTitle, descLabel, myReviewBox, reviewTitle, reviewsScroll);
        content.setPadding(new Insets(10));
        content.setPrefWidth(540);
        content.setStyle("-fx-background-color:#fffdfb;");
        ScrollPane contentScroll = new ScrollPane(content);
        contentScroll.setFitToWidth(true);
        contentScroll.setPrefViewportWidth(560);
        contentScroll.setPrefViewportHeight(650);
        contentScroll.setStyle("-fx-background-color:transparent; -fx-background:#fffdfb;");

        dialog.getDialogPane().setContent(contentScroll);
        dialog.getDialogPane().setPrefWidth(600);
        dialog.getDialogPane().setMaxWidth(600);
        dialog.getDialogPane().setMinWidth(600);
        ButtonType closeType = ButtonType.CLOSE;
        dialog.getDialogPane().getButtonTypes().add(closeType);

        ButtonType likeType = null;
        ButtonType saveReviewType = null;
        ButtonType reserveType = null;

        if (!admin) {
            likeType = new ButtonType(alreadyLiked ? "Retirer like" : "Like", ButtonBar.ButtonData.LEFT);
            dialog.getDialogPane().getButtonTypes().add(0, likeType);
            saveReviewType = new ButtonType("Enregistrer ma note et mon avis", ButtonBar.ButtonData.OTHER);
            dialog.getDialogPane().getButtonTypes().add(1, saveReviewType);
        }

        if (!admin && !full && !alreadyReserved) {
            reserveType = new ButtonType("Reserver", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().add(0, reserveType);
        }

        if (saveReviewType != null && inlineCommentArea != null) {
            Node saveButtonNode = dialog.getDialogPane().lookupButton(saveReviewType);
            saveButtonNode.setDisable(inlineCommentArea.getText().trim().isEmpty());
            TextArea finalInlineCommentArea = inlineCommentArea;
            inlineCommentArea.textProperty().addListener((obs, oldValue, newValue) ->
                    saveButtonNode.setDisable(newValue.trim().isEmpty()));
            saveButtonNode.setStyle(PRIMARY);
        }

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }
        if (reserveType != null && result.get() == reserveType) {
            reserve(event);
            loadEvents();
            showEventDetails(event);
            return;
        }
        if (likeType != null && result.get() == likeType) {
            toggleLike(event);
            showEventDetails(event);
            return;
        }
        if (saveReviewType != null && result.get() == saveReviewType && inlineRatingBox != null && inlineCommentArea != null) {
            try {
                eventEngagementService.addOrUpdateReview(event.getId(), currentUser.getId(), inlineRatingBox.getValue(), inlineCommentArea.getText());
                loadEvents();
                showInfo("Avis enregistre", "Votre note et votre avis ont ete enregistres.");
                showEventDetails(event);
            } catch (SQLException e) {
                showError("Avis impossible", e.getMessage());
            }
        }
    }

    private VBox buildReviewsBox(Event event) {
        VBox reviewsBox = new VBox(10);
        reviewsBox.setPadding(new Insets(4));
        try {
            List<EventReview> reviews = eventEngagementService.getReviewsByEvent(event.getId());
            if (reviews.isEmpty()) {
                Label empty = new Label("Aucun avis pour le moment.");
                empty.setStyle("-fx-text-fill:#6a7f94; -fx-font-size:12px;");
                reviewsBox.getChildren().add(empty);
                return reviewsBox;
            }

            for (EventReview review : reviews) {
                Label author = new Label((review.getUsername() == null ? "Utilisateur" : review.getUsername()) + " - " + review.getRating() + "/5");
                author.setStyle("-fx-text-fill:#10233f; -fx-font-size:12px; -fx-font-weight:800;");
                Label date = new Label(review.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                date.setStyle("-fx-text-fill:#6a7f94; -fx-font-size:11px;");
                Label comment = new Label(review.getComment());
                comment.setWrapText(true);
                comment.setStyle("-fx-text-fill:#4f6478; -fx-font-size:12px;");
                VBox item = new VBox(4, author, date, comment);
                item.setPadding(new Insets(10));
                item.setStyle("-fx-background-color:#f8fbff; -fx-background-radius:16; -fx-border-radius:16; -fx-border-color:#dfe8f2;");
                reviewsBox.getChildren().add(item);
            }
        } catch (SQLException e) {
            Label error = new Label("Impossible de charger les avis : " + e.getMessage());
            error.setStyle("-fx-text-fill:#b74949; -fx-font-size:12px;");
            reviewsBox.getChildren().add(error);
        }
        return reviewsBox;
    }

    private void promptReview(Event event) {
        if (currentUser == null || currentUser.isAdmin()) {
            showError("Acces refuse", "Seul un etudiant peut laisser un avis.");
            return;
        }

        try {
            EventReview existingReview = eventEngagementService.getReviewByEventAndUser(event.getId(), currentUser.getId());
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Laisser un avis");
            dialog.setHeaderText("Avis pour " + event.getTitre());
            if (primaryStage != null) {
                dialog.initOwner(primaryStage);
            }

            ComboBox<Integer> ratingBox = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5));
            ratingBox.setValue(existingReview == null ? 5 : existingReview.getRating());
            ratingBox.setStyle(INPUT);
            ratingBox.setMaxWidth(Double.MAX_VALUE);

            TextArea commentArea = new TextArea(existingReview == null ? "" : existingReview.getComment());
            commentArea.setPromptText("Ecrivez votre avis ici...");
            commentArea.setPrefRowCount(7);
            commentArea.setPrefWidth(420);
            commentArea.setWrapText(true);
            commentArea.setStyle(INPUT);

            Label ratingLabel = new Label("Donnez une note");
            ratingLabel.setStyle("-fx-text-fill:#10233f; -fx-font-size:13px; -fx-font-weight:800;");
            Label commentLabel = new Label("Votre commentaire");
            commentLabel.setStyle("-fx-text-fill:#10233f; -fx-font-size:13px; -fx-font-weight:800;");
            Label helperLabel = new Label("Partagez votre experience sur cet evenement.");
            helperLabel.setStyle("-fx-text-fill:#67809d; -fx-font-size:12px;");

            VBox content = new VBox(12,
                    helperLabel,
                    ratingLabel,
                    ratingBox,
                    commentLabel,
                    commentArea
            );
            content.setPadding(new Insets(10));
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.getDialogPane().setMinWidth(480);

            Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
            okButton.setDisable(commentArea.getText().trim().isEmpty());
            commentArea.textProperty().addListener((obs, oldValue, newValue) -> okButton.setDisable(newValue.trim().isEmpty()));

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                eventEngagementService.addOrUpdateReview(event.getId(), currentUser.getId(), ratingBox.getValue(), commentArea.getText());
                loadEvents();
                showInfo("Avis enregistre", "Votre avis a ete enregistre.");
            }
        } catch (SQLException e) {
            showError("Avis impossible", e.getMessage());
        }
    }

    private void toggleLike(Event event) {
        if (currentUser == null || currentUser.isAdmin()) {
            showError("Acces refuse", "Seul un etudiant peut liker un evenement.");
            return;
        }
        try {
            boolean liked = eventEngagementService.toggleLike(event.getId(), currentUser.getId());
            loadEvents();
            showInfo("Like mis a jour", liked ? "Evenement ajoute a vos likes." : "Like retire.");
        } catch (SQLException e) {
            showError("Like impossible", e.getMessage());
        }
    }

    private void updateAvailabilityStrip(List<Event> events) {
        if (resultsCountLabel != null) {
            resultsCountLabel.setText(events.size() + (events.size() > 1 ? " evenements" : " evenement"));
        }
        if (availabilityDatesRow == null || availabilitySummaryLabel == null) {
            return;
        }

        availabilityDatesRow.getChildren().clear();
        if (events.isEmpty()) {
            availabilitySummaryLabel.setText("Aucune date disponible pour cette recherche.");
            availabilityDatesRow.getChildren().add(filterChip("Aucun evenement"));
            return;
        }

        Map<LocalDate, Integer> grouped = new LinkedHashMap<>();
        events.stream()
                .sorted(Comparator.comparing(Event::getDateEvent))
                .forEach(event -> {
                    LocalDate date = event.getDateEvent().toLocalDate();
                    try {
                        int available = event.getCapacite() - reservationService.getReservationCountByEvent(event.getId());
                        grouped.merge(date, Math.max(0, available), Integer::sum);
                    } catch (SQLException e) {
                        grouped.merge(date, event.getCapacite(), Integer::sum);
                    }
                });

        availabilitySummaryLabel.setText("Dates a venir - cliquez sur Plus de dates pour voir le calendrier complet.");
        grouped.entrySet().stream().limit(8).forEach(entry -> {
            LocalDate date = entry.getKey();
            int optionsCount = entry.getValue();
            
            VBox chip = new VBox(2);
            chip.setAlignment(Pos.CENTER);
            chip.setPadding(new Insets(10, 14, 10, 14));
            chip.setStyle("-fx-background-color:white; -fx-background-radius:18; -fx-border-radius:18; -fx-border-color:#dce8f1; -fx-cursor: hand;");

            String dayName = date.format(DateTimeFormatter.ofPattern("EEE")).toUpperCase();
            Label dayLabel = new Label(dayName);
            dayLabel.setStyle("-fx-text-fill:#7b91a6; -fx-font-size:10px; -fx-font-weight:800;");
            
            Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
            dayNum.setStyle("-fx-text-fill:#0f2942; -fx-font-size:20px; -fx-font-weight:900;");
            
            String optionsText = optionsCount + (optionsCount > 1 ? " options" : " option");
            Label optionsLabel = new Label(optionsText);
            optionsLabel.setStyle("-fx-text-fill:#12b7c8; -fx-font-size:10px; -fx-font-weight:800;");

            chip.getChildren().addAll(dayLabel, dayNum, optionsLabel);
            chip.setOnMouseClicked(e -> filterEventsByDate(date));
            availabilityDatesRow.getChildren().add(chip);
        });
        
        // Bouton "Plus de dates" avec icône calendrier
        VBox plusButton = new VBox();
        plusButton.setAlignment(Pos.CENTER);
        plusButton.setPadding(new Insets(10, 10, 10, 10));
        plusButton.setStyle("-fx-background-color:white; -fx-background-radius:18; -fx-border-radius:18; -fx-border-color:#dce8f1; -fx-cursor: hand;");
        
        Label calendarIcon = new Label("📅");
        calendarIcon.setStyle("-fx-font-size:28px;");
        Label moreText = new Label("Plus de dates");
        moreText.setStyle("-fx-text-fill:#12b7c8; -fx-font-size:10px; -fx-font-weight:800;");
        
        plusButton.getChildren().addAll(calendarIcon, moreText);
        plusButton.setOnMouseClicked(e -> openFullCalendarWindow());
        availabilityDatesRow.getChildren().add(plusButton);
    }
    
    private void filterEventsByDate(LocalDate date) {
        // Filtrer les événements par date sélectionnée
        try {
            List<Event> allEvents = eventService.getAllEvents();
            List<Event> filtered = allEvents.stream()
                    .filter(event -> event.getDateEvent().toLocalDate().equals(date))
                    .collect(java.util.stream.Collectors.toList());
            
            updateEventGrid(filtered);
            availabilitySummaryLabel.setText("Événements du " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les événements");
        }
    }
    
    private void openFullCalendarWindow() {
        Stage calendarStage = new Stage();
        calendarStage.setTitle("Calendrier des événements");
        calendarStage.setWidth(900);
        calendarStage.setHeight(700);
        
        VBox content = createFullCalendarView();
        Scene scene = new Scene(content);
        calendarStage.setScene(scene);
        calendarStage.initOwner(primaryStage);
        calendarStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
        calendarStage.show();
    }
    
    private VBox createFullCalendarView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f9fbff;");
        
        Label title = new Label("📅 Calendrier complet des événements");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #0f2942;");
        
        try {
            List<Event> allEvents = eventService.getAllEvents();
            
            // Créer le calendrier
            CalendarPicker calendar = new CalendarPicker(date -> {
                filterEventsByDate(date);
            });
            
            // Grouper par date et compter les options
            Map<LocalDate, Integer> availabilityMap = new LinkedHashMap<>();
            for (Event event : allEvents) {
                LocalDate date = event.getDateEvent().toLocalDate();
                int available = event.getCapacite() - reservationService.getReservationCountByEvent(event.getId());
                availabilityMap.merge(date, Math.max(0, available), Integer::max);
            }
            
            calendar.setDateAvailabilities(availabilityMap);
            
            // Afficher la liste des dates avec options
            VBox datesPanel = new VBox(10);
            datesPanel.setPadding(new Insets(15));
            datesPanel.setStyle("-fx-border-color: #dce8f1; -fx-border-radius: 10; -fx-background-color: white;");
            
            Label datesTitle = new Label("Dates disponibles:");
            datesTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0f2942;");
            datesPanel.getChildren().add(datesTitle);
            
            FlowPane datesRow = new FlowPane(10, 10);
            datesRow.setAlignment(Pos.CENTER_LEFT);
            datesRow.setPrefWrapLength(800);
            
            availabilityMap.entrySet().stream().limit(15).forEach(entry -> {
                LocalDate date = entry.getKey();
                int options = entry.getValue();
                
                VBox dateChip = new VBox(2);
                dateChip.setAlignment(Pos.CENTER);
                dateChip.setPadding(new Insets(8, 12, 8, 12));
                dateChip.setStyle("-fx-background-color: #e8f4ff; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #12b7c8;");
                
                String dayName = date.format(DateTimeFormatter.ofPattern("EEE")).toUpperCase();
                Label dayLabel = new Label(dayName);
                dayLabel.setStyle("-fx-text-fill: #7b91a6; -fx-font-size: 9px; -fx-font-weight: 800;");
                
                Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
                dayNum.setStyle("-fx-text-fill: #0f2942; -fx-font-size: 16px; -fx-font-weight: 900;");
                
                Label optionsLabel = new Label(options + " options");
                optionsLabel.setStyle("-fx-text-fill: #12b7c8; -fx-font-size: 9px; -fx-font-weight: 800;");
                
                dateChip.getChildren().addAll(dayLabel, dayNum, optionsLabel);
                dateChip.setOnMouseClicked(e -> filterEventsByDate(date));
                
                datesRow.getChildren().add(dateChip);
            });
            
            ScrollPane scrollPane = new ScrollPane(datesRow);
            scrollPane.setFitToHeight(true);
            scrollPane.setStyle("-fx-control-inner-background: white;");
            datesPanel.getChildren().add(scrollPane);
            
            root.getChildren().addAll(title, calendar, datesPanel);
            
        } catch (SQLException e) {
            Label error = new Label("Erreur: " + e.getMessage());
            error.setStyle("-fx-text-fill: red;");
            root.getChildren().add(error);
        }
        
        return root;
    }

    private String shortDescription(String text) {
        if (text == null || text.isBlank()) {
            return "Evenement disponible avec informations synchronisees depuis la base de donnees.";
        }
        String compact = text.trim().replaceAll("\\s+", " ");
        return compact.length() <= 108 ? compact : compact.substring(0, 105) + "...";
    }

    private VBox detailFact(String icon, String value) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-text-fill:#25384a; -fx-font-size:22px;");
        Label valueLabel = new Label(value);
        valueLabel.setWrapText(true);
        valueLabel.setStyle("-fx-text-fill:#3f5060; -fx-font-size:12px; -fx-font-weight:700;");
        VBox box = new VBox(6, iconLabel, valueLabel);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(95);
        return box;
    }

    private String formatDuration(int minutes) {
        int safeMinutes = minutes <= 0 ? 60 : minutes;
        if (safeMinutes % 60 == 0) {
            int hours = safeMinutes / 60;
            return hours + (hours > 1 ? " heures" : " heure");
        }
        if (safeMinutes > 60) {
            int hours = safeMinutes / 60;
            int extraMinutes = safeMinutes % 60;
            return hours + "h " + extraMinutes + "min";
        }
        return safeMinutes + " min";
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "-";
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }

    private String formatRating(double rating) {
        if (rating <= 0) {
            return "Pas encore note";
        }
        return String.format(Locale.US, "%.1f/5", rating);
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

    private class CalendarCell extends ListCell<Event> {
        @Override protected void updateItem(Event event, boolean empty) {
            super.updateItem(event, empty);
            if (empty || event == null) { setGraphic(null); setText(null); return; }
            Label title = new Label(event.getTitre());
            title.setStyle("-fx-text-fill:#10233f; -fx-font-size:17px; -fx-font-weight:800;");
            Label timing = small(event.getDateEvent().format(EVENT_FMT) + "  |  Duree: " + event.getDurationMinutes() + " min");
            Label place = small("Lieu: " + event.getLieu() + "  |  Categorie: " + (event.getCategorie() == null ? "-" : event.getCategorie()));
            Label state = small("Statut: " + (event.getStatus() == null ? "ACTIVE" : event.getStatus()));
            state.setStyle("-fx-text-fill:#0f69ff; -fx-font-size:12px; -fx-font-weight:700;");
            VBox box = new VBox(8, title, timing, place, state);
            box.setPadding(new Insets(16));
            box.setStyle("-fx-background-color: linear-gradient(to right, rgba(255,255,255,0.98), rgba(244,249,255,0.95)); -fx-background-radius:18; -fx-border-radius:18; -fx-border-color: rgba(138,182,238,0.24);");
            setGraphic(box);
        }
    }

    private class ReservationCell extends ListCell<ReservationRecord> {
        @Override protected void updateItem(ReservationRecord r, boolean empty) {
            super.updateItem(r, empty);
            if (empty || r == null) { setGraphic(null); setText(null); return; }
            VBox box = new VBox(8,
                    title(r.getEventTitle() == null ? "Evenement supprime" : r.getEventTitle(), 16),
                    new Label("Etudiant: " + (r.getUsername() == null ? "inconnu" : r.getUsername())),
                    small("Reserve le " + r.getReservedAt().format(RES_FMT)));
            ((Label) box.getChildren().get(1)).setStyle("-fx-text-fill:#2a5fa3; -fx-font-size:13px; -fx-font-weight:700;");
            box.setPadding(new Insets(18));
            box.setStyle("-fx-background-color: linear-gradient(to right, rgba(255,255,255,0.98), rgba(244,249,255,0.95)); -fx-background-radius:22; -fx-border-radius:22; -fx-border-color: rgba(138,182,238,0.24);");
            setGraphic(box);
        }
    }

    public static void main(String[] args) { launch(args); }
}
