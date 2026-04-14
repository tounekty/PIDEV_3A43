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
import javafx.fxml.FXMLLoader;
import org.example.auth.AppUser;
import org.example.auth.AuthService;
import org.example.controller.ResourceCatalogController;
import org.example.controller.ResourceListController;
import org.example.event.Event;
import org.example.event.EventService;
import org.example.reservation.ReservationRecord;
import org.example.reservation.ReservationService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

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
    private final ReservationService reservationService = new ReservationService();
    private final Map<Integer, Integer> reservationCounts = new HashMap<>();
    private final Set<Integer> reservedEventIds = new HashSet<>();

    private BorderPane root;
    private StackPane pageContainer;
    private VBox loginPage;
    private VBox formPage;
    private VBox eventsPage;
    private VBox reservationsPage;
    private VBox statsPage;
    private VBox header;
    private Stage primaryStage;
    private AppUser currentUser;
    private Event editingEvent;

    private TextField titleField;
    private TextArea descriptionArea;
    private TextField locationField;
    private DatePicker datePicker;
    private TextField timeField;
    private TextField capacityField;
    private ComboBox<String> categoryField;
    private TextField imageField;
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
    private Button statsHeaderButton;
    private Button resourcesHeaderButton;
    private Button logoutButton;
    private ListView<Event> eventListView;
    private ListView<ReservationRecord> reservationListView;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{3,30}$");
    private static final Pattern TIME_PATTERN = Pattern.compile("^(?:[01]\\d|2[0-3]):[0-5]\\d$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+ ]{8,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

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
        eventsPage = buildEventsPage();
        reservationsPage = buildReservationsPage();
        try {
            statsPage = buildStatsPageGlobal();
        } catch (SQLException e) {
            statsPage = new VBox(new Label("Erreur chargement stats"));
        }
        pageContainer.getChildren().setAll(loginPage);
        root.setCenter(pageContainer);
        stage.setScene(new Scene(root, 1200, 760));
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
        statsHeaderButton = button("Statistiques", SECONDARY, e -> { try { statsPage = buildStatsPageGlobal(); showPage(statsPage); } catch (SQLException ex) { showError("Erreur", ex.getMessage()); } });
        resourcesHeaderButton = button("📚 Ressources", SECONDARY, e -> openResourcesWindow());
        logoutButton = button("Deconnexion", SECONDARY, e -> logout());
        userBadge = new Label();
        userBadge.setStyle("-fx-text-fill:#0f69ff; -fx-background-color:rgba(15,105,255,0.10); -fx-background-radius:999; -fx-padding:8 12 8 12; -fx-font-size:12px; -fx-font-weight:800;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(12, spacer, userBadge, addHeaderButton, eventsHeaderButton, reservationsHeaderButton, statsHeaderButton, resourcesHeaderButton, logoutButton);
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
        datePicker = new DatePicker(LocalDate.now().plusDays(1)); datePicker.setStyle(INPUT);
        timeField = input("10:00");
        capacityField = input("20");
        categoryField = new ComboBox<>(FXCollections.observableArrayList("yoga", "wellness", "sport", "meditation"));
        categoryField.setValue("yoga"); categoryField.setMaxWidth(Double.MAX_VALUE); categoryField.setStyle(INPUT);
        imageField = input("Chemin image"); imageField.setEditable(false);
        capacityField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,4}") ? change : null));
        HBox imageBox = new HBox(10, imageField, button("Parcourir", SECONDARY, e -> chooseImage())); HBox.setHgrow(imageField, Priority.ALWAYS);
        GridPane form = formGrid();
        addRow(form, 0, "Titre", titleField); addRow(form, 1, "Description", descriptionArea); addRow(form, 2, "Lieu", locationField);
        addRow(form, 3, "Date", datePicker); addRow(form, 4, "Heure", timeField); addRow(form, 5, "Capacite", capacityField);
        addRow(form, 6, "Categorie", categoryField); addRow(form, 7, "Image", imageBox);
        saveButton = button("Ajouter l'evenement", PRIMARY, e -> saveEvent());
        cancelEditButton = button("Annuler", SECONDARY, e -> resetForm()); cancelEditButton.setVisible(false); cancelEditButton.setManaged(false);
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

        VBox content = card(title("Reservations des etudiants", 24), small("Admin voit ici qui reserve chaque evenement."), button("Actualiser", SECONDARY, e -> loadReservations()), reservationListView);
        return scrollPage(content);
    }

    private void initializeDatabase() {
        try {
            authService.initializeUsers();
            eventService.createTableIfNotExists();
            reservationService.initializeReservations();
        } catch (SQLException e) {
            showError("Initialisation BDD impossible", e.getMessage());
        }
    }

    private void attemptLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText().trim();
        if (username.isBlank() || password.isBlank()) { showWarning("Utilisateur et mot de passe sont obligatoires."); return; }
        if (!USERNAME_PATTERN.matcher(username).matches()) { showWarning("Utilisateur invalide (3-30 caracteres: lettres, chiffres, ., _, -)."); return; }
        if (password.length() < 4 || password.length() > 64) { showWarning("Mot de passe invalide (4 a 64 caracteres)."); return; }
        try {
            AppUser user = authService.login(username, password);
            if (user == null) { showError("Connexion refusee", "Identifiants invalides."); return; }
            currentUser = user;
            applyRole();
            resetForm();
            loadEvents();
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
        statsHeaderButton.setVisible(admin); statsHeaderButton.setManaged(admin);
        resourcesHeaderButton.setVisible(true); resourcesHeaderButton.setManaged(true);
        eventsTitle.setText(admin ? "Back office des evenements" : "Catalogue des evenements");
        eventsSubtitle.setText(admin ? "Admin peut modifier, supprimer et suivre les reservations." : "L'etudiant consulte les evenements avec photo et peut reserver.");
    }

    private void showLoginPage() {
        for (Node n : List.of(userBadge, addHeaderButton, eventsHeaderButton, reservationsHeaderButton, statsHeaderButton, resourcesHeaderButton, logoutButton)) { n.setVisible(false); n.setManaged(false); }
        showPage(loginPage);
    }

    private void logout() {
        currentUser = null; editingEvent = null; reservedEventIds.clear(); reservationCounts.clear();
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
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String description = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
        String location = locationField.getText() == null ? "" : locationField.getText().trim();
        String timeText = timeField.getText() == null ? "" : timeField.getText().trim();
        String capText = capacityField.getText() == null ? "" : capacityField.getText().trim();

        if (title.isBlank() || description.isBlank() || location.isBlank() || datePicker.getValue() == null) { showWarning("Tous les champs obligatoires doivent etre remplis."); return null; }
        if (title.length() < 3 || title.length() > 120) { showWarning("Le titre doit contenir entre 3 et 120 caracteres."); return null; }
        if (description.length() < 10) { showWarning("La description doit contenir au moins 10 caracteres."); return null; }
        if (location.length() < 2 || location.length() > 120) { showWarning("Le lieu doit contenir entre 2 et 120 caracteres."); return null; }
        if (!TIME_PATTERN.matcher(timeText).matches()) { showWarning("L'heure doit etre au format HH:mm."); return null; }

        LocalTime time; int cap;
        try { time = LocalTime.parse(timeText); } catch (Exception e) { showWarning("L'heure doit etre au format HH:mm."); return null; }
        try { cap = Integer.parseInt(capText); } catch (Exception e) { showWarning("La capacite doit etre un entier."); return null; }
        if (cap <= 0) { showWarning("La capacite doit etre superieure a zero."); return null; }
        return new Event(title, description, LocalDateTime.of(datePicker.getValue(), time), location, cap, categoryField.getValue(), imageField.getText().trim(), null);
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
            if (nom.length() < 2 || prenom.length() < 2) {
                showWarning("Nom et prenom doivent contenir au moins 2 caracteres.");
                return;
            }
            if (!PHONE_PATTERN.matcher(telephone).matches()) {
                showWarning("Telephone invalide (8-20 caracteres, chiffres, espaces, +).");
                return;
            }
            if (!EMAIL_PATTERN.matcher(mail).matches()) {
                showWarning("Email invalide.");
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
        imageField.setText(event.getImage() == null ? "" : event.getImage()); showPage(formPage);
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
    }

    private void chooseImage() {
        FileChooser chooser = new FileChooser(); chooser.setTitle("Choisir une image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png"));
        File file = chooser.showOpenDialog(primaryStage); if (file != null) imageField.setText(file.getPath());
    }

    private void showPage(VBox page) { pageContainer.getChildren().setAll(page); }

    private GridPane formGrid() { GridPane g = new GridPane(); g.setHgap(14); g.setVgap(14); return g; }
    private void addRow(GridPane g, int row, String label, Node field) { Label l = new Label(label); l.setStyle("-fx-text-fill:#29496f; -fx-font-size:13px; -fx-font-weight:700;"); g.add(l, 0, row); g.add(field, 1, row); GridPane.setHgrow(field, Priority.ALWAYS); }
    private TextField input(String prompt) { TextField f = new TextField(); f.setPromptText(prompt); f.setStyle(INPUT); return f; }
    private Label small(String text) { Label l = new Label(text); l.setStyle("-fx-text-fill:#637a97; -fx-font-size:13px;"); return l; }
    private Label title(String text, int size) { Label l = new Label(text); l.setStyle("-fx-text-fill:#10233f; -fx-font-size:" + size + "px; -fx-font-weight:800;"); return l; }
    private Button button(String text, String style, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button b = new Button(text);
        String normalStyle = style + " -fx-cursor: hand;";
        String hoverStyle = normalStyle + " -fx-effect: dropshadow(gaussian, rgba(30, 86, 170, 0.22), 12, 0.2, 0, 3); -fx-opacity: 0.96;";
        String pressedStyle = normalStyle + " -fx-effect: dropshadow(gaussian, rgba(30, 86, 170, 0.16), 8, 0.2, 0, 1); -fx-scale-x: 0.985; -fx-scale-y: 0.985;";

        b.setStyle(normalStyle);
        b.setOnMouseEntered(e -> {
            if (!b.isDisabled()) b.setStyle(hoverStyle);
        });
        b.setOnMouseExited(e -> b.setStyle(normalStyle));
        b.setOnMousePressed(e -> {
            if (!b.isDisabled()) b.setStyle(pressedStyle);
        });
        b.setOnMouseReleased(e -> {
            if (b.isHover() && !b.isDisabled()) b.setStyle(hoverStyle);
            else b.setStyle(normalStyle);
        });
        b.setOnAction(handler);
        return b;
    }
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

    private void openResourcesWindow() {
        try {
            boolean admin = currentUser != null && currentUser.isAdmin();
            String view = admin ? "/org/example/fxml/resource_list.fxml" : "/org/example/fxml/resource_catalog.fxml";

            URL url = Main.class.getResource(view);
            if (url == null) {
                url = Thread.currentThread().getContextClassLoader().getResource(view.startsWith("/") ? view.substring(1) : view);
            }
            if (url == null) {
                showError("Erreur", "Fichier FXML introuvable: " + view);
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Stage stage = new Stage();
            stage.setTitle(admin ? "Gestion des Ressources et Commentaires" : "Catalogue des Ressources");
            stage.setScene(new Scene(loader.load(), 900, 700));
            if (admin) {
                ResourceListController controller = loader.getController();
                controller.setAdminMode(true);
                if (currentUser != null) {
                    controller.setCurrentUserId(currentUser.getId());
                }
            } else {
                ResourceCatalogController controller = loader.getController();
                controller.setAdminMode(false);
                if (currentUser != null) {
                    controller.setCurrentUserId(currentUser.getId());
                }
            }
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            showError("Erreur", "Impossible d'ouvrir la fenêtre Ressources:\n" + root.getClass().getSimpleName() + ": " + root.getMessage());
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

    public static void main(String[] args) { launch(args); }
}
