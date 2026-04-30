package org.example;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import org.example.event.Event;
import org.example.event.EventService;
import org.example.event.SmartSchedulingService;
import org.example.reservation.ReservationRecord;
import org.example.reservation.ReservationService;
import org.example.reservation.ReservationStatus;
import org.example.util.ImageManager;
import org.example.util.MapService;
import org.example.util.MapView;
import org.example.util.QRCodeService;
import org.example.util.TimePickerSpinner;
import org.example.util.WeatherService;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Back-office admin: manage events, view reservations, stats.
 */
public class AdminApp {

    // ── Style constants ──────────────────────────────────────────────────────
    private static final String ROOT      = "-fx-background-color: linear-gradient(to bottom right,#ffffff,#edf6ff);";
    private static final String CARD      = "-fx-background-color: rgba(255,255,255,0.96); -fx-background-radius: 24; -fx-border-radius: 24; -fx-border-color: rgba(120,169,230,0.20); -fx-effect: dropshadow(gaussian, rgba(46,94,166,0.12), 24, 0.18, 0, 8);";
    private static final String PRIMARY   = "-fx-background-color: linear-gradient(to right,#0f69ff,#38a4ff); -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 14; -fx-padding: 12 18 12 18;";
    private static final String SECONDARY = "-fx-background-color: white; -fx-text-fill: #1c4f96; -fx-font-weight: 700; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #cfe3ff; -fx-padding: 11 16 11 16;";
    private static final String DANGER    = "-fx-background-color: #fff4f4; -fx-text-fill: #c63d48; -fx-font-weight: 700; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #ffd5d8; -fx-padding: 11 16 11 16;";
    private static final String INPUT     = "-fx-background-color: #f9fbff; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #d7e7ff; -fx-padding: 12 14 12 14;";
    private static final String SIDEBAR_ITEM        = "-fx-background-color: transparent; -fx-text-fill: #c8d8f0; -fx-font-size: 14px; -fx-font-weight: 700; -fx-padding: 14 20; -fx-alignment: CENTER_LEFT; -fx-background-radius: 10; -fx-cursor: hand;";
    private static final String SIDEBAR_ITEM_ACTIVE = "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 700; -fx-padding: 14 20; -fx-alignment: CENTER_LEFT; -fx-background-radius: 10; -fx-cursor: hand;";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Services ─────────────────────────────────────────────────────────────
    private final EventService eventService             = new EventService();
    private final ReservationService reservationService = new ReservationService();
    private final SmartSchedulingService schedulingService = new SmartSchedulingService();
    private final WeatherService weatherService         = new WeatherService();

    // ── State ─────────────────────────────────────────────────────────────────
    private final AppUser currentUser;
    private final Stage stage = new Stage();
    private StackPane contentArea;

    // Form fields (reused for add/edit)
    private TextField titleField;
    private TextArea  descriptionArea;
    private TextField locationField;
    private DatePicker datePicker;
    private TimePickerSpinner timePicker;
    private TextField capacityField;
    private ComboBox<String> categoryField;
    private TextField imagePathField;
    private Label     imageThumbnailLabel;
    private Label     formTitleLabel;
    private Button    saveButton;
    private Event     editingEvent;

    // Sidebar buttons (to toggle active style)
    private Button btnEvents;
    private Button btnAdd;
    private Button btnReservations;
    private Button btnAI;
    private Button btnMap;

    public AdminApp(AppUser user) {
        this.currentUser = user;
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle(ROOT);

        // Header
        root.setTop(buildHeader());

        // Sidebar
        root.setLeft(buildSidebar());

        // Content
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(24));
        root.setCenter(contentArea);

        Scene scene = new Scene(root, 1280, 800);
        stage.setScene(scene);
        stage.setTitle("MindCare Events — Back Office Admin");
        stage.show();

        // Default page
        showEventsPage();
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private HBox buildHeader() {
        Label title = new Label("Back Office Admin");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: 800;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label("👤 " + currentUser.getUsername());
        badge.setStyle("-fx-text-fill: white; -fx-background-color: rgba(255,255,255,0.18); -fx-background-radius: 999; -fx-padding: 6 14 6 14; -fx-font-weight: 700; -fx-font-size: 13px;");

        HBox header = new HBox(16, title, spacer, badge);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setStyle("-fx-background-color: linear-gradient(to right,#0f2942,#1a3f6f);");
        return header;
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────
    private VBox buildSidebar() {
        Label brand = new Label("MindCare");
        brand.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: 900; -fx-padding: 24 20 8 20;");

        btnEvents       = sidebarBtn("📋  Événements",   () -> showEventsPage());
        btnAdd          = sidebarBtn("➕  Ajouter",       () -> showFormPage(null));
        btnReservations = sidebarBtn("📌  Réservations",  () -> showReservationsPage());
        btnAI           = sidebarBtn("🤖  Analyse IA",    () -> showAIPage());
        btnMap          = sidebarBtn("🗺️  Carte",         () -> showMapPage());
        Button btnLogout = sidebarBtn("🚪  Déconnexion",  this::logout);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox sidebar = new VBox(4, brand, btnEvents, btnAdd, btnReservations, btnAI, btnMap, spacer, btnLogout);
        sidebar.setStyle("-fx-background-color: linear-gradient(to bottom, #0f2942, #1a3f6f); -fx-min-width: 210; -fx-max-width: 210;");
        return sidebar;
    }

    private Button sidebarBtn(String text, Runnable action) {
        Button b = new Button(text);
        b.setStyle(SIDEBAR_ITEM);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> action.run());
        b.setOnMouseEntered(ev -> { if (!b.getStyle().equals(SIDEBAR_ITEM_ACTIVE)) b.setStyle(SIDEBAR_ITEM_ACTIVE); });
        b.setOnMouseExited(ev  -> { if (!b.getStyle().equals(SIDEBAR_ITEM_ACTIVE)) b.setStyle(SIDEBAR_ITEM); });
        return b;
    }

    private void setActive(Button active) {
        for (Button b : new Button[]{btnEvents, btnAdd, btnReservations, btnAI, btnMap}) {
            if (b != null) b.setStyle(b == active ? SIDEBAR_ITEM_ACTIVE : SIDEBAR_ITEM);
        }
    }

    // ── Pages ─────────────────────────────────────────────────────────────────

    /** Events list page — card grid layout */
    private void showEventsPage() {
        setActive(btnEvents);

        Label pageTitle = new Label("Événements");
        pageTitle.setStyle("-fx-text-fill:#10233f; -fx-font-size:24px; -fx-font-weight:800;");

        TextField search = new TextField();
        search.setPromptText("Rechercher…");
        search.setStyle(INPUT);
        HBox.setHgrow(search, Priority.ALWAYS);

        Button refresh = btn("Actualiser", SECONDARY);
        Button addBtn  = btn("➕ Ajouter", PRIMARY);
        addBtn.setOnAction(e -> showFormPage(null));

        HBox toolbar = new HBox(12, search, refresh, addBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Card grid
        TilePane grid = new TilePane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPrefColumns(3);
        grid.setTileAlignment(Pos.TOP_LEFT);
        grid.setPrefTileWidth(300);
        grid.setPadding(new Insets(4, 0, 8, 0));

        Runnable[] ref = {null};
        ref[0] = () -> {
            try {
                String q = search.getText();
                List<Event> events = q.isBlank()
                        ? eventService.getAllEvents()
                        : eventService.searchEvents(q);
                grid.getChildren().clear();
                for (Event ev : events) {
                    grid.getChildren().add(buildAdminEventCard(ev, ref[0]));
                }
            } catch (SQLException ex) { showError(ex.getMessage()); }
        };

        search.textProperty().addListener((obs, o, n) -> ref[0].run());
        refresh.setOnAction(e -> ref[0].run());
        ref[0].run();

        ScrollPane scrollGrid = new ScrollPane(grid);
        scrollGrid.setFitToWidth(true);
        scrollGrid.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollGrid, Priority.ALWAYS);

        VBox page = new VBox(18, pageTitle, toolbar, scrollGrid);
        page.setPadding(new Insets(8));
        VBox.setVgrow(scrollGrid, Priority.ALWAYS);

        contentArea.getChildren().setAll(page);
    }

    private VBox buildAdminEventCard(Event ev, Runnable reload) {
        // Image
        StackPane imgPane = createImagePane(ev.getImage(), 300, 160, ev.getTitre());
        imgPane.setStyle(imgPane.getStyle() + " -fx-background-radius: 16 16 0 0;");

        // Category badge
        Label catBadge = new Label(ev.getCategorie() != null ? ev.getCategorie() : "général");
        catBadge.setStyle("-fx-background-color: rgba(15,105,255,0.12); -fx-text-fill:#0f69ff;"
                + "-fx-font-size:11px; -fx-font-weight:800; -fx-background-radius:8; -fx-padding:3 10;");

        // Title
        Label titleLbl = new Label(ev.getTitre());
        titleLbl.setStyle("-fx-font-size:16px; -fx-font-weight:900; -fx-text-fill:#0f2942;");
        titleLbl.setWrapText(true);

        // Description (truncated)
        String descText = ev.getDescription() != null && ev.getDescription().length() > 60
                ? ev.getDescription().substring(0, 60) + "…" : ev.getDescription();
        Label descLbl = new Label(descText != null ? descText : "");
        descLbl.setStyle("-fx-text-fill:#637a97; -fx-font-size:12px;");
        descLbl.setWrapText(true);

        // Date
        Label dateLbl = new Label("📅 " + (ev.getDateEvent() != null
                ? ev.getDateEvent().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : ""));
        dateLbl.setStyle("-fx-text-fill:#415a78; -fx-font-size:12px;");

        // Location
        Label lieuLbl = new Label("📍 " + ev.getLieu());
        lieuLbl.setStyle("-fx-text-fill:#415a78; -fx-font-size:12px;");

        // Reservations count
        int resCount = 0;
        try { resCount = reservationService.getReservationCountByEvent(ev.getId()); } catch (SQLException ignored) {}
        int remaining = ev.getCapacite() - resCount;
        Label capLbl = new Label(resCount + " / " + ev.getCapacite() + " réservations  —  "
                + (remaining <= 0 ? "Complet" : remaining + " place(s)"));
        capLbl.setStyle("-fx-text-fill:" + (remaining <= 0 ? "#c63d48" : "#1a7a4a")
                + "; -fx-font-size:11px; -fx-font-weight:700;");

        // Action buttons
        Button editBtn = new Button("✏️ Modifier");
        editBtn.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1565c0;"
                + "-fx-padding: 8 14; -fx-background-radius: 10; -fx-font-size: 12px; -fx-font-weight: 700; -fx-cursor: hand;");
        editBtn.setOnAction(e -> showFormPage(ev));

        Button ticketsBtn = new Button("🎟 Billets");
        ticketsBtn.setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32;"
                + "-fx-padding: 8 14; -fx-background-radius: 10; -fx-font-size: 12px; -fx-font-weight: 700; -fx-cursor: hand;");
        ticketsBtn.setOnAction(e -> showEventTicketsDialog(ev));

        Button deleteBtn = new Button("🗑 Supprimer");
        deleteBtn.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c63d48;"
                + "-fx-padding: 8 14; -fx-background-radius: 10; -fx-font-size: 12px; -fx-font-weight: 700; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Supprimer « " + ev.getTitre() + " » ?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    try {
                        reservationService.deleteReservationsForEvent(ev.getId());
                        eventService.deleteEvent(ev.getId());
                        reload.run();
                    } catch (SQLException ex) { showError(ex.getMessage()); }
                }
            });
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(8, editBtn, ticketsBtn, spacer, deleteBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(8, catBadge, titleLbl, descLbl, dateLbl, lieuLbl, capLbl, actions);
        body.setPadding(new Insets(14, 16, 16, 16));

        VBox card = new VBox(imgPane, body);
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16;"
                + "-fx-border-radius: 16; -fx-border-color: #e8f0fe;"
                + "-fx-effect: dropshadow(gaussian, rgba(46,94,166,0.10), 12, 0, 0, 4);");
        return card;
    }

    private <T> TableColumn<T, String> col(String name, double width) {
        TableColumn<T, String> c = new TableColumn<>(name);
        c.setPrefWidth(width);
        return c;
    }

    /**
     * Affiche une fenêtre avec les QR codes de tous les participants confirmés d'un événement.
     */
    private void showEventTicketsDialog(Event ev) {
        Stage dialog = new Stage();
        dialog.setTitle("🎟 Billets — " + ev.getTitre());
        dialog.initOwner(stage);

        Label title = new Label("Billets confirmés — " + ev.getTitre());
        title.setStyle("-fx-font-size:16px; -fx-font-weight:800; -fx-text-fill:#0f2942;");
        title.setWrapText(true);

        VBox listBox = new VBox(12);
        listBox.setPadding(new Insets(8));

        try {
            List<ReservationRecord> participants = reservationService.getParticipantsByEvent(ev.getId());
            if (participants.isEmpty()) {
                listBox.getChildren().add(new Label("Aucun participant confirmé."));
            } else {
                org.example.util.QRCodeService qrSvc = new org.example.util.QRCodeService();
                for (ReservationRecord r : participants) {
                    // Générer le QR localement via ZXing
                    String payload = (ev.getDateEvent() != null)
                            ? qrSvc.generateQRPayload(r.getId(), r.getUserId(), r.getEventId(), ev.getDateEvent())
                            : r.getId() + "|" + r.getUserId() + "|" + r.getEventId() + "|0|NOHASH";
                    String ticketUrl = qrSvc.generateTicketUrl(payload);
                    byte[] qrBytes = qrSvc.generateQRBytes(ticketUrl, 120);

                    ImageView qrImg = new ImageView();
                    qrImg.setFitWidth(80);
                    qrImg.setFitHeight(80);
                    qrImg.setPreserveRatio(true);

                    if (qrBytes != null) {
                        qrImg.setImage(new Image(new java.io.ByteArrayInputStream(qrBytes)));
                    } else {
                        // Fallback réseau
                        String url = qrSvc.generateQRImageUrl(ticketUrl, 120);
                        new Thread(() -> {
                            try {
                                Image img = new Image(url, true);
                                Platform.runLater(() -> qrImg.setImage(img));
                            } catch (Exception ignored) {}
                        }, "qr-admin").start();
                    }

                    Label nameLbl = new Label("👤 " + (r.getUsername() != null ? r.getUsername() : "User #" + r.getUserId()));
                    nameLbl.setStyle("-fx-font-weight:700; -fx-font-size:13px; -fx-text-fill:#0f2942;");
                    Label resLbl = new Label("Réservation #" + r.getId());
                    resLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#637a97;");

                    VBox info = new VBox(4, nameLbl, resLbl);
                    info.setAlignment(Pos.CENTER_LEFT);
                    HBox.setHgrow(info, Priority.ALWAYS);

                    HBox row = new HBox(14, qrImg, info);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(10, 14, 10, 14));
                    row.setStyle("-fx-background-color: white; -fx-background-radius:12; -fx-border-color:#edf2fb; -fx-border-radius:12;");
                    listBox.getChildren().add(row);
                }
            }
        } catch (SQLException ex) {
            listBox.getChildren().add(new Label("Erreur: " + ex.getMessage()));
        }

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle(SECONDARY);
        closeBtn.setOnAction(e -> dialog.close());

        VBox root = new VBox(14, title, scroll, closeBtn);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f5f8ff;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Scene scene = new Scene(root, 420, 520);
        dialog.setScene(scene);
        dialog.show();
    }

    private void deleteEvent(Event event, TableView<Event> table) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer « " + event.getTitre() + " » ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    reservationService.deleteReservationsForEvent(event.getId());
                    eventService.deleteEvent(event.getId());
                    table.getItems().remove(event);
                } catch (SQLException ex) { showError(ex.getMessage()); }
            }
        });
    }
    /** Add / Edit form page */
    private void showFormPage(Event toEdit) {
        setActive(btnAdd);
        editingEvent = toEdit;

        formTitleLabel = new Label(toEdit == null ? "Nouvel événement" : "Modifier l'événement");
        formTitleLabel.setStyle("-fx-text-fill:#10233f; -fx-font-size:22px; -fx-font-weight:800;");

        titleField = inputField("Ex: Morning Yoga Session");
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("Description");
        descriptionArea.setStyle(INPUT);
        descriptionArea.setPrefRowCount(4);

        locationField = inputField("Ex: Tunis, Lac 2");

        datePicker = new DatePicker(LocalDate.now().plusDays(1));
        datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.setStyle(INPUT);

        timePicker = new TimePickerSpinner(LocalTime.of(10, 0));

        capacityField = inputField("20");

        // Editable category ComboBox with "+ Nouvelle catégorie" option
        javafx.collections.ObservableList<String> catItems = FXCollections.observableArrayList(
                "yoga", "wellness", "sport", "meditation", "conference", "atelier",
                "── + Nouvelle catégorie ──");
        categoryField = new ComboBox<>(catItems);
        categoryField.setValue("yoga");
        categoryField.setMaxWidth(Double.MAX_VALUE);
        categoryField.setStyle(INPUT);
        categoryField.setEditable(true); // allow typing directly

        // When user selects "+ Nouvelle catégorie", prompt for input
        categoryField.setOnAction(e -> {
            String selected = categoryField.getValue();
            if (selected != null && selected.startsWith("──")) {
                javafx.scene.control.TextInputDialog dialog =
                        new javafx.scene.control.TextInputDialog();
                dialog.setTitle("Nouvelle catégorie");
                dialog.setHeaderText(null);
                dialog.setContentText("Nom de la nouvelle catégorie :");
                dialog.showAndWait().ifPresent(newCat -> {
                    String trimmed = newCat.trim().toLowerCase();
                    if (!trimmed.isBlank()) {
                        // Add before the separator item
                        catItems.add(catItems.size() - 1, trimmed);
                        categoryField.setValue(trimmed);
                    } else {
                        categoryField.setValue("yoga");
                    }
                });
            }
        });

        imagePathField = new TextField();
        imagePathField.setEditable(false);
        imageThumbnailLabel = new Label("Aucune image sélectionnée");
        imageThumbnailLabel.setStyle("-fx-text-fill:#999; -fx-font-size:12px;");

        Button browseBtn = new Button("📁 Parcourir");
        browseBtn.setStyle(SECONDARY);
        browseBtn.setOnAction(e -> chooseImage());

        HBox imageBox = new HBox(10, browseBtn, imageThumbnailLabel);
        imageBox.setAlignment(Pos.CENTER_LEFT);

        // ── Indoor / Outdoor toggle ──
        ToggleButton indoorBtn  = new ToggleButton("🏠 Indoor");
        ToggleButton outdoorBtn = new ToggleButton("🌳 Outdoor");
        ToggleGroup  venueGroup = new ToggleGroup();
        indoorBtn.setToggleGroup(venueGroup);
        outdoorBtn.setToggleGroup(venueGroup);
        indoorBtn.setSelected(true); // défaut indoor

        String toggleBase = "-fx-font-weight:700; -fx-font-size:13px; -fx-background-radius:10;"
                + "-fx-border-radius:10; -fx-padding:9 20; -fx-cursor:hand;";
        String toggleOff  = toggleBase + "-fx-background-color:#f3f4f6; -fx-text-fill:#555; -fx-border-color:#d1d5db;";
        String toggleOn   = toggleBase + "-fx-background-color:#2563eb; -fx-text-fill:white; -fx-border-color:#2563eb;";

        indoorBtn.setStyle(toggleOn);
        outdoorBtn.setStyle(toggleOff);

        indoorBtn.selectedProperty().addListener((obs, o, n) ->
                indoorBtn.setStyle(n ? toggleOn : toggleOff));
        outdoorBtn.selectedProperty().addListener((obs, o, n) ->
                outdoorBtn.setStyle(n ? toggleOn : toggleOff));

        HBox venueRow = new HBox(8, indoorBtn, outdoorBtn);
        venueRow.setAlignment(Pos.CENTER_LEFT);

        // ── Widget météo (mis à jour quand la date change) ──
        Label wEmoji  = new Label("🌡️");
        wEmoji.setStyle("-fx-font-size:24px;");
        Label wCondLbl = new Label("Sélectionnez une date");
        wCondLbl.setStyle("-fx-font-size:13px; -fx-font-weight:800; -fx-text-fill:#0f2942;");
        Label wTempLbl = new Label("");
        wTempLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#637a97;");
        Label wAdvice  = new Label("");
        wAdvice.setStyle("-fx-font-size:11px; -fx-text-fill:#415a78; -fx-font-style:italic;");
        wAdvice.setWrapText(true);

        VBox wTextBox = new VBox(2, wCondLbl, wTempLbl, wAdvice);
        wTextBox.setAlignment(Pos.CENTER_LEFT);
        HBox weatherBox = new HBox(12, wEmoji, wTextBox);
        weatherBox.setAlignment(Pos.CENTER_LEFT);
        weatherBox.setPadding(new Insets(12, 16, 12, 16));
        weatherBox.setStyle("-fx-background-color:#f0f9ff; -fx-background-radius:12;"
                + "-fx-border-color:#bae6fd; -fx-border-radius:12;");

        // Runnable pour charger la météo selon la date choisie
        Runnable loadWeather = () -> {
            LocalDate chosen = datePicker.getValue();
            if (chosen == null) return;
            wCondLbl.setText("Chargement…");
            wEmoji.setText("🌡️");
            wTempLbl.setText("");
            wAdvice.setText("");
            Thread t = new Thread(() -> {
                String cond  = weatherService.getWeatherCondition("Tunis", chosen);
                int    temp  = weatherService.getTemperature("Tunis", chosen);
                String emoji = WeatherService.toEmoji(cond);
                boolean isOutdoor = outdoorBtn.isSelected();
                String advice = buildFormWeatherAdvice(cond, isOutdoor);
                String bg = cond.equals("Clear") ? "#fffbeb"
                        : (cond.equals("Rain") || cond.equals("Drizzle") || cond.equals("Thunderstorm")) ? "#eff6ff"
                        : cond.equals("Snow") ? "#f0f9ff" : "#f9fafb";
                String border = cond.equals("Clear") ? "#fde68a"
                        : (cond.equals("Rain") || cond.equals("Drizzle") || cond.equals("Thunderstorm")) ? "#bfdbfe"
                        : cond.equals("Snow") ? "#bae6fd" : "#e5e7eb";
                Platform.runLater(() -> {
                    wEmoji.setText(emoji);
                    wCondLbl.setText(cond.equals("Unknown") ? "Météo indisponible"
                            : emoji + "  " + cond);
                    wTempLbl.setText(cond.equals("Unknown") ? "" : "🌡️ " + temp + "°C  —  Tunis");
                    wAdvice.setText(advice);
                    weatherBox.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:12;"
                            + "-fx-border-color:" + border + "; -fx-border-radius:12;");
                });
            }, "weather-form");
            t.setDaemon(true);
            t.start();
        };

        // Déclencher quand la date change
        datePicker.valueProperty().addListener((obs, o, n) -> loadWeather.run());
        // Mettre à jour le conseil quand indoor/outdoor change
        venueGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            String cond = wCondLbl.getText().contains("  ")
                    ? wCondLbl.getText().split("  ")[1] : "Unknown";
            wAdvice.setText(buildFormWeatherAdvice(cond, outdoorBtn.isSelected()));
        });

        // Pre-fill if editing
        if (toEdit != null) {
            titleField.setText(toEdit.getTitre());
            descriptionArea.setText(toEdit.getDescription());
            locationField.setText(toEdit.getLieu());
            if (toEdit.getDateEvent() != null) {
                datePicker.setValue(toEdit.getDateEvent().toLocalDate());
                timePicker.setTime(toEdit.getDateEvent().toLocalTime());
            }
            capacityField.setText(String.valueOf(toEdit.getCapacite()));
            if (toEdit.getCategorie() != null) {
                String cat = toEdit.getCategorie();
                if (cat.startsWith("[Outdoor] ")) {
                    outdoorBtn.setSelected(true);
                    categoryField.setValue(cat.substring("[Outdoor] ".length()));
                } else if (cat.startsWith("[Indoor] ")) {
                    indoorBtn.setSelected(true);
                    categoryField.setValue(cat.substring("[Indoor] ".length()));
                } else {
                    categoryField.setValue(cat);
                }
            }
            if (toEdit.getImage() != null) {
                imagePathField.setText(toEdit.getImage());
                imageThumbnailLabel.setText(new File(toEdit.getImage()).getName());
            }
            // Charger météo pour la date existante
            loadWeather.run();
        }

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(14);
        ColumnConstraints labelCol = new ColumnConstraints(120);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        fieldCol.setFillWidth(true);
        form.getColumnConstraints().addAll(labelCol, fieldCol);

        addFormRow(form, 0, "Titre",         titleField);
        addFormRow(form, 1, "Description",   descriptionArea);
        addFormRow(form, 2, "Lieu",          locationField);
        addFormRow(form, 3, "Date",          datePicker);
        addFormRow(form, 4, "Heure",         timePicker);
        addFormRow(form, 5, "Capacité",      capacityField);
        addFormRow(form, 6, "Type de lieu",  venueRow);
        addFormRow(form, 7, "Météo du jour", weatherBox);

        // Category row: ComboBox + delete button
        Button deleteCatBtn = new Button("🗑");
        deleteCatBtn.setStyle("-fx-background-color: #fff4f4; -fx-text-fill: #c63d48;"
                + "-fx-font-weight: 700; -fx-font-size: 14px;"
                + "-fx-background-radius: 10; -fx-border-radius: 10;"
                + "-fx-border-color: #ffd5d8; -fx-padding: 8 12; -fx-cursor: hand;");
        deleteCatBtn.setTooltip(new Tooltip("Supprimer cette catégorie de la liste"));
        deleteCatBtn.setOnAction(e -> {
            String current = categoryField.getValue();
            if (current == null || current.isBlank() || current.startsWith("──")) return;
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Supprimer la catégorie « " + current + " » de la liste ?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    catItems.remove(current);
                    String next = catItems.stream()
                            .filter(s -> !s.startsWith("──"))
                            .findFirst().orElse("");
                    categoryField.setValue(next);
                }
            });
        });

        HBox catRow = new HBox(8, categoryField, deleteCatBtn);
        catRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(categoryField, Priority.ALWAYS);
        addFormRow(form, 8, "Catégorie", catRow);
        addFormRow(form, 9, "Image",     imageBox);

        saveButton = new Button(toEdit == null ? "Enregistrer" : "Mettre à jour");
        saveButton.setStyle(PRIMARY);
        saveButton.setOnAction(e -> {
            // Préfixer la catégorie avec Indoor/Outdoor avant de sauvegarder
            String venue = outdoorBtn.isSelected() ? "[Outdoor] " : "[Indoor] ";
            String rawCat = categoryField.getValue();
            if (rawCat != null && !rawCat.startsWith("[")) {
                categoryField.setValue(venue + rawCat);
            }
            saveEvent();
            // Restaurer la valeur sans préfixe pour l'affichage
            if (rawCat != null) categoryField.setValue(rawCat);
        });

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle(SECONDARY);
        cancelBtn.setOnAction(e -> showEventsPage());

        HBox buttons = new HBox(12, saveButton, cancelBtn);

        VBox card = new VBox(18, formTitleLabel, form, buttons);
        card.setPadding(new Insets(28));
        card.setStyle(CARD);

        ScrollPane scroll = new ScrollPane(card);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        contentArea.getChildren().setAll(scroll);
    }

    private void addFormRow(GridPane grid, int row, String label, Node field) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill:#10233f; -fx-font-weight:700; -fx-font-size:13px;");
        grid.add(lbl, 0, row);
        grid.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private void chooseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            try {
                ImageManager.ImageInfo info = ImageManager.uploadImage(file);
                imagePathField.setText(info.path);
                imageThumbnailLabel.setText(info.fileName);
                imageThumbnailLabel.setStyle("-fx-text-fill:#0f69ff; -fx-font-size:12px; -fx-font-weight:700;");
            } catch (Exception ex) {
                showError("Erreur image : " + ex.getMessage());
            }
        }
    }

    private void saveEvent() {
        String titre = titleField.getText().trim();
        String desc  = descriptionArea.getText().trim();
        String lieu  = locationField.getText().trim();
        String capStr = capacityField.getText().trim();

        // ── Contrôle de saisie ────────────────────────────────────────────────
        if (titre.isBlank() || desc.isBlank() || lieu.isBlank() || capStr.isBlank()) {
            showWarning("Tous les champs obligatoires doivent être remplis.");
            return;
        }
        try { org.example.util.ValidationUtil.validateTitle(titre); }
        catch (IllegalArgumentException ex) { showWarning(ex.getMessage()); return; }

        try { org.example.util.ValidationUtil.validateDescription(desc); }
        catch (IllegalArgumentException ex) { showWarning(ex.getMessage()); return; }

        try { org.example.util.ValidationUtil.validateLocation(lieu); }
        catch (IllegalArgumentException ex) { showWarning(ex.getMessage()); return; }

        int cap;
        try { cap = Integer.parseInt(capStr); }
        catch (NumberFormatException ex) { showWarning("La capacité doit être un nombre entier."); return; }
        try { org.example.util.ValidationUtil.validateCapacity(cap); }
        catch (IllegalArgumentException ex) { showWarning(ex.getMessage()); return; }

        LocalDate date = datePicker.getValue();
        if (date == null) { showWarning("Veuillez sélectionner une date."); return; }

        LocalTime time = timePicker.getTimeAsLocalTime();
        LocalDateTime dateTime = date.atTime(time);
        if (dateTime.isBefore(LocalDateTime.now())) {
            showWarning("La date et l'heure de l'événement doivent être dans le futur."); return;
        }

        String categorie = categoryField.getValue();
        try { org.example.util.ValidationUtil.validateCategory(categorie); }
        catch (IllegalArgumentException ex) { showWarning(ex.getMessage()); return; }
        // ─────────────────────────────────────────────────────────────────────

        String imagePath = imagePathField.getText().isBlank() ? null : imagePathField.getText();

        try {
            if (editingEvent == null) {
                Event ev = new Event(titre, desc, dateTime, lieu, cap, categorie, imagePath, currentUser.getId());
                eventService.addEvent(ev);
                showInfo("Événement ajouté avec succès.");
            } else {
                editingEvent.setTitre(titre);
                editingEvent.setTitle(titre);
                editingEvent.setDescription(desc);
                editingEvent.setDateEvent(dateTime);
                editingEvent.setEventDate(dateTime);
                editingEvent.setLieu(lieu);
                editingEvent.setLocation(lieu);
                editingEvent.setCapacite(cap);
                editingEvent.setCategorie(categorie);
                editingEvent.setImage(imagePath);
                eventService.updateEvent(editingEvent);
                showInfo("Événement mis à jour.");
            }
            showEventsPage();
        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    /** Reservations page */
    private void showReservationsPage() {
        setActive(btnReservations);

        Label pageTitle = new Label("Réservations");
        pageTitle.setStyle("-fx-text-fill:#10233f; -fx-font-size:24px; -fx-font-weight:800;");

        ListView<ReservationRecord> list = new ListView<>();
        list.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
        VBox.setVgrow(list, Priority.ALWAYS);

        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(ReservationRecord r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setGraphic(null); return; }

                Label title = new Label(r.getEventTitle() != null ? r.getEventTitle() : "Événement #" + r.getEventId());
                title.setStyle("-fx-font-weight:800; -fx-font-size:14px; -fx-text-fill:#10233f;");

                Label user = new Label("👤 " + (r.getUsername() != null ? r.getUsername() : "Utilisateur #" + r.getUserId()));
                user.setStyle("-fx-text-fill:#637a97; -fx-font-size:12px;");

                Label date = new Label("📅 " + (r.getReservedAt() != null ? r.getReservedAt().format(FMT) : ""));
                date.setStyle("-fx-text-fill:#637a97; -fx-font-size:12px;");

                // Determine status from reservation id lookup
                String statusText = "CONFIRMED";
                String statusStyle = "-fx-background-color:#e6f9f0; -fx-text-fill:#1a7a4a; -fx-background-radius:8; -fx-padding:3 10; -fx-font-weight:700; -fx-font-size:11px;";
                Label statusBadge = new Label(statusText);
                statusBadge.setStyle(statusStyle);

                HBox meta = new HBox(14, user, date, statusBadge);
                meta.setAlignment(Pos.CENTER_LEFT);

                VBox cell = new VBox(4, title, meta);
                cell.setPadding(new Insets(10, 14, 10, 14));
                cell.setStyle("-fx-background-color: white; -fx-background-radius:12; -fx-border-color:#edf2fb; -fx-border-radius:12;");
                setGraphic(cell);
                setStyle("-fx-background-color: transparent; -fx-padding: 4 0;");
            }
        });

        Button refresh = btn("Actualiser", SECONDARY);
        Runnable load = () -> {
            try {
                list.setItems(FXCollections.observableArrayList(reservationService.getAllReservations()));
            } catch (SQLException ex) { showError(ex.getMessage()); }
        };
        refresh.setOnAction(e -> load.run());
        load.run();

        HBox toolbar = new HBox(12, refresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox page = new VBox(18, pageTitle, toolbar, list);
        page.setPadding(new Insets(24));
        page.setStyle(CARD);
        VBox.setVgrow(list, Priority.ALWAYS);

        contentArea.getChildren().setAll(page);
    }

    // ── AI Analysis Page ──────────────────────────────────────────────────────
    private void showAIPage() {
        setActive(btnAI);

        // ── Titre ─────────────────────────────────────────────────────────────
        Label pageTitle = new Label("🤖 Assistant IA — Planification");
        pageTitle.setStyle("-fx-text-fill:#0f2942; -fx-font-size:22px; -fx-font-weight:900;");
        Label pageSub = new Label("Choisissez une catégorie et cliquez Analyser pour obtenir les meilleurs créneaux.");
        pageSub.setStyle("-fx-text-fill:#637a97; -fx-font-size:12px;");

        // ── Météo (widget compact) ────────────────────────────────────────────
        Label weatherIcon = new Label("🌡️");
        weatherIcon.setStyle("-fx-font-size:22px;");
        Label weatherCond = new Label("Chargement…");
        weatherCond.setStyle("-fx-font-size:13px; -fx-font-weight:800; -fx-text-fill:#0f2942;");
        Label weatherImpact = new Label("");
        weatherImpact.setStyle("-fx-font-size:11px; -fx-text-fill:#415a78;");
        weatherImpact.setWrapText(true);
        VBox weatherInfo = new VBox(2, weatherCond, weatherImpact);
        HBox weatherWidget = new HBox(10, weatherIcon, weatherInfo);
        weatherWidget.setAlignment(Pos.CENTER_LEFT);
        weatherWidget.setPadding(new Insets(10, 16, 10, 16));
        weatherWidget.setStyle("-fx-background-color:#f0f9ff; -fx-background-radius:12; -fx-border-color:#bae6fd; -fx-border-radius:12;");

        String[] weatherRef = {"Unknown"};
        new Thread(() -> {
            String cond = weatherService.getWeatherCondition("Tunis", LocalDate.now());
            int tempC   = weatherService.getTemperature("Tunis", LocalDate.now());
            weatherRef[0] = cond;
            String emoji   = WeatherService.toEmoji(cond);
            String display = cond.equals("Unknown") ? "Météo indisponible" : emoji + " " + cond + " — " + tempC + "°C  (Tunis)";
            Platform.runLater(() -> {
                weatherIcon.setText(emoji);
                weatherCond.setText(display);
            });
        }, "weather-loader").start();

        // ── Test SMS (compact) ────────────────────────────────────────────────
        javafx.scene.control.TextField phoneField = new javafx.scene.control.TextField("+21692047431");
        phoneField.setStyle(INPUT); phoneField.setPrefWidth(150);
        Button testSmsBtn = btn("📱 Tester SMS", SECONDARY);
        Label smsStatus = new Label("");
        smsStatus.setStyle("-fx-font-size:11px; -fx-font-weight:700;");
        testSmsBtn.setOnAction(e -> {
            smsStatus.setText("Envoi…");
            String phone = phoneField.getText().trim();
            new Thread(() -> {
                org.example.util.SmsService svc = new org.example.util.SmsService();
                int resId = 1;
                try { var r = reservationService.getAllReservations(); if (!r.isEmpty()) resId = r.get(0).getId(); } catch (Exception ignored) {}
                final int fId = resId;
                boolean ok = svc.sendSMS(phone, "MindCare Events\nTest SMS\nReservation #" + fId);
                Platform.runLater(() -> {
                    smsStatus.setText(ok ? "✅ Envoyé !" : "❌ Échec");
                    smsStatus.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill:" + (ok ? "#15803d" : "#c63d48") + ";");
                });
            }, "sms-test").start();
        });
        HBox smsRow = new HBox(8, new Label("Test SMS :"), phoneField, testSmsBtn, smsStatus);
        smsRow.setAlignment(Pos.CENTER_LEFT);
        ((Label) smsRow.getChildren().get(0)).setStyle("-fx-font-weight:700; -fx-text-fill:#0f2942; -fx-font-size:12px;");

        // ── Sélecteur catégorie + bouton Analyser ─────────────────────────────
        ComboBox<String> catBox = new ComboBox<>(FXCollections.observableArrayList(
                "Toutes catégories", "yoga", "wellness", "sport", "meditation", "conference", "atelier"));
        catBox.setValue("Toutes catégories");
        catBox.setStyle(INPUT); catBox.setEditable(true); catBox.setPrefWidth(200);
        Button analyzeBtn = btn("🔍 Analyser", PRIMARY);
        HBox catRow = new HBox(12, new Label("Catégorie :"), catBox, analyzeBtn);
        catRow.setAlignment(Pos.CENTER_LEFT);
        ((Label) catRow.getChildren().get(0)).setStyle("-fx-font-weight:700; -fx-text-fill:#0f2942;");

        // ── Zone résultats ────────────────────────────────────────────────────
        VBox resultBox = new VBox(14);

        Runnable runAnalysis = () -> {
            resultBox.getChildren().clear();
            String cat = "Toutes catégories".equals(catBox.getValue()) ? null : catBox.getValue();

            // Mise à jour impact météo
            weatherImpact.setText(buildWeatherImpactText(weatherRef[0], cat));

            // ── Résumé statistiques (1 ligne par stat) ──
            String summary = schedulingService.getAnalysisSummary(cat);
            Label summaryLbl = new Label(summary);
            summaryLbl.setStyle("-fx-text-fill:#415a78; -fx-font-size:12px;");
            summaryLbl.setWrapText(true);
            VBox summaryCard = new VBox(6,
                new Label("📊 Statistiques"),
                summaryLbl
            );
            ((Label) summaryCard.getChildren().get(0)).setStyle("-fx-font-weight:800; -fx-font-size:13px; -fx-text-fill:#0f2942;");
            summaryCard.setPadding(new Insets(14));
            summaryCard.setStyle("-fx-background-color:#f8fbff; -fx-background-radius:12; -fx-border-color:#d7e7ff; -fx-border-radius:12;");

            // ── Top 5 créneaux ──
            List<SmartSchedulingService.SuggestedSlot> slots = schedulingService.suggestBestSlots(cat, 60);
            Label slotsTitle = new Label("🏆 Top 5 créneaux recommandés");
            slotsTitle.setStyle("-fx-font-weight:900; -fx-font-size:14px; -fx-text-fill:#0f2942;");

            VBox slotsBox = new VBox(10);
            if (slots.isEmpty()) {
                Label noData = new Label("Pas encore assez de données. Créez des événements pour enrichir l'analyse.");
                noData.setStyle("-fx-text-fill:#9ab0cc; -fx-font-size:12px; -fx-font-style:italic;");
                slotsBox.getChildren().add(noData);
            } else {
                int rank = 1;
                for (SmartSchedulingService.SuggestedSlot slot : slots) {
                    slotsBox.getChildren().add(buildAISlotCard(slot, rank++, cat, weatherRef[0]));
                }
            }

            resultBox.getChildren().addAll(summaryCard, slotsTitle, slotsBox, buildHowItWorksBox());
        };

        analyzeBtn.setOnAction(e -> runAnalysis.run());
        runAnalysis.run();

        VBox content = new VBox(14, pageTitle, pageSub, weatherWidget, smsRow, new Separator(), catRow, new Separator(), resultBox);
        content.setPadding(new Insets(12));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox page = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        contentArea.getChildren().setAll(page);
    }

    private VBox buildAISlotCard(SmartSchedulingService.SuggestedSlot slot, int rank, String cat, String weather) {
        // Probabilité de succès (blend v1 + v2)
        double prob = slot.successProb;
        if (slot.prediction != null) prob = slot.prediction.getSuccessProbability() * 100;
        prob = Math.min(99, Math.max(1, prob));

        // Couleurs selon probabilité
        String probColor = prob >= 75 ? "#15803d" : prob >= 50 ? "#b45309" : "#c63d48";
        String probBg    = prob >= 75 ? "#dcfce7" : prob >= 50 ? "#fef9c3" : "#fee2e2";
        String badge     = prob >= 75 ? "🟢 Excellent" : prob >= 50 ? "🟡 Moyen" : "🔴 Faible";

        // ── Ligne 1 : rang + date + badge ──────────────────────────────────
        Label rankLbl = new Label("#" + rank);
        rankLbl.setStyle("-fx-text-fill:#0f69ff; -fx-font-weight:900; -fx-font-size:15px; -fx-min-width:28;");

        Label dateLbl = new Label(slot.label);
        dateLbl.setStyle("-fx-font-weight:800; -fx-font-size:13px; -fx-text-fill:#0f2942;");
        HBox.setHgrow(dateLbl, Priority.ALWAYS);

        Label badgeLbl = new Label(badge);
        badgeLbl.setStyle("-fx-background-color:" + probBg + "; -fx-text-fill:" + probColor
                + "; -fx-font-weight:800; -fx-font-size:11px; -fx-background-radius:8; -fx-padding:3 10;");

        HBox topRow = new HBox(10, rankLbl, dateLbl, badgeLbl);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // ── Ligne 2 : probabilité + participants ───────────────────────────
        Label probLbl = new Label(String.format("🎯 Succès : %.0f%%", prob));
        probLbl.setStyle("-fx-text-fill:" + probColor + "; -fx-font-size:13px; -fx-font-weight:800;");

        String partText = slot.prediction != null
                ? String.format("  ·  👥 ~%.0f participants", slot.prediction.getExpectedParticipants()) : "";
        Label partLbl = new Label(partText);
        partLbl.setStyle("-fx-text-fill:#415a78; -fx-font-size:12px;");

        HBox statsRow = new HBox(0, probLbl, partLbl);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        // ── Barre de progression visuelle ─────────────────────────────────
        javafx.scene.layout.Region barBg = new javafx.scene.layout.Region();
        barBg.setPrefHeight(6); barBg.setPrefWidth(Double.MAX_VALUE);
        barBg.setStyle("-fx-background-color:#e8f0fe; -fx-background-radius:99;");
        HBox.setHgrow(barBg, Priority.ALWAYS);

        javafx.scene.layout.Region barFill = new javafx.scene.layout.Region();
        barFill.setPrefHeight(6);
        barFill.setMaxWidth(prob / 100.0 * 400);
        barFill.setStyle("-fx-background-color:" + probColor + "; -fx-background-radius:99;");

        javafx.scene.layout.StackPane progressBar = new javafx.scene.layout.StackPane(barBg, barFill);
        javafx.scene.layout.StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        // ── Météo (1 ligne compacte) ───────────────────────────────────────
        String weatherLine = buildWeatherImpactText(weather, cat);
        Label weatherLbl = new Label(weatherLine);
        weatherLbl.setStyle("-fx-text-fill:#415a78; -fx-font-size:11px;");
        weatherLbl.setWrapText(true);

        // ── Explication IA (points clés seulement) ────────────────────────
        String expl = slot.explanation != null ? slot.explanation : "";
        // Garder max 3 lignes pour rester lisible
        String[] lines = expl.split("\n");
        StringBuilder shortExpl = new StringBuilder();
        int count = 0;
        for (String line : lines) {
            if (!line.isBlank() && count < 3) { shortExpl.append(line.trim()).append("\n"); count++; }
        }
        Label explLbl = new Label(shortExpl.toString().trim());
        explLbl.setStyle("-fx-text-fill:#637a97; -fx-font-size:11px;");
        explLbl.setWrapText(true);

        // ── Bouton utiliser ────────────────────────────────────────────────
        Button useBtn = new Button("✅ Utiliser ce créneau");
        useBtn.setStyle("-fx-background-color:#0f69ff; -fx-text-fill:white; -fx-font-weight:700;"
                + "-fx-font-size:12px; -fx-background-radius:10; -fx-padding:8 16; -fx-cursor:hand;");
        useBtn.setOnAction(e -> {
            showFormPage(null);
            Platform.runLater(() -> {
                if (datePicker != null)  datePicker.setValue(slot.dateTime.toLocalDate());
                if (timePicker != null)  timePicker.setTime(slot.dateTime.toLocalTime());
                if (cat != null && categoryField != null) categoryField.setValue(cat);
            });
        });

        // ── Assemblage ────────────────────────────────────────────────────
        VBox card = new VBox(8, topRow, statsRow, progressBar, weatherLbl, explLbl, useBtn);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle("-fx-background-color:white; -fx-background-radius:14;"
                + "-fx-border-color:#e8f0fe; -fx-border-radius:14;"
                + "-fx-effect:dropshadow(gaussian,rgba(46,94,166,0.08),8,0,0,3);");
        return card;
    }

    /** Génère le texte d'impact météo selon la condition et la catégorie. */
    private String buildWeatherImpactText(String weather, String category) {
        if (weather == null || weather.equalsIgnoreCase("Unknown")) return "Données météo indisponibles";
        boolean indoor  = WeatherService.isIndoor(category);
        boolean outdoor = WeatherService.isOutdoor(category);
        return switch (weather) {
            case "Rain", "Drizzle" -> indoor
                    ? "🌧️ Pluie → favorable pour événement indoor ✔"
                    : outdoor ? "🌧️ Pluie → défavorable pour événement outdoor ⚠"
                    : "🌧️ Météo pluvieuse — impact modéré";
            case "Thunderstorm" -> "⛈️ Orage prévu — impact négatif sur la participation ⚠";
            case "Clear"        -> outdoor
                    ? "☀️ Beau temps → excellent pour événement outdoor ✔"
                    : "☀️ Beau temps → bonne météo pour l'événement ✔";
            case "Clouds"       -> "☁️ Ciel nuageux — météo neutre";
            case "Snow"         -> indoor
                    ? "❄️ Neige → favorable pour événement indoor ✔"
                    : "❄️ Neige — impact négatif sur les déplacements ⚠";
            case "Mist", "Fog", "Haze" -> "🌫️ Brouillard — visibilité réduite ℹ";
            default -> WeatherService.toEmoji(weather) + " Météo : " + weather;
        };
    }

    /** Conseil météo spécifique au formulaire d'ajout (indoor/outdoor explicite). */
    private String buildFormWeatherAdvice(String weather, boolean isOutdoor) {
        if (weather == null || weather.equalsIgnoreCase("Unknown"))
            return "Données météo indisponibles pour cette date";
        return switch (weather) {
            case "Clear" -> isOutdoor
                    ? "☀️ Parfait pour un événement outdoor ! Beau temps prévu ✔"
                    : "☀️ Beau temps — bonne météo pour attirer les participants ✔";
            case "Clouds" -> isOutdoor
                    ? "☁️ Ciel nuageux — outdoor possible, pas de pluie prévue"
                    : "☁️ Météo neutre — pas d'impact sur un événement indoor";
            case "Rain", "Drizzle" -> isOutdoor
                    ? "🌧️ Pluie prévue — déconseillé pour un événement outdoor ⚠\n   Envisagez de passer en Indoor"
                    : "🌧️ Pluie prévue — votre événement indoor est bien adapté ✔";
            case "Thunderstorm" -> "⛈️ Orage prévu — impact négatif sur la participation ⚠\n   Envisagez de reporter ou de passer en indoor";
            case "Snow" -> isOutdoor
                    ? "❄️ Neige prévue — très déconseillé pour un événement outdoor ⚠"
                    : "❄️ Neige prévue — votre événement indoor est bien adapté ✔";
            case "Mist", "Fog", "Haze" -> "🌫️ Brouillard — déplacements difficiles, prévenez les participants ℹ";
            default -> WeatherService.toEmoji(weather) + " Météo : " + weather;
        };
    }

    // ── Map Page ──────────────────────────────────────────────────────────────
    private void showMapPage() {
        setActive(btnMap);

        Label pageTitle = new Label("🗺️ Carte interactive des événements");
        pageTitle.setStyle("-fx-text-fill:#0f2942; -fx-font-size:24px; -fx-font-weight:900;");
        Label pageSub = new Label(
                "Visualisez les événements sur la carte. Les marqueurs sont colorés selon le score de lieu (centralité).");
        pageSub.setStyle("-fx-text-fill:#637a97; -fx-font-size:13px;");
        pageSub.setWrapText(true);

        // Filtre catégorie
        ComboBox<String> catFilter = new ComboBox<>(
                javafx.collections.FXCollections.observableArrayList(
                        "Tous", "yoga", "wellness", "sport", "meditation", "conference", "atelier"));
        catFilter.setValue("Tous");
        catFilter.setStyle(INPUT);
        catFilter.setPrefWidth(180);

        Button refreshBtn = btn("🔄 Actualiser", SECONDARY);
        Label catLabel = new Label("Catégorie :");
        catLabel.setStyle("-fx-font-weight:700; -fx-text-fill:#0f2942;");
        HBox toolbar = new HBox(12, catLabel, catFilter, refreshBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Carte Leaflet
        MapView mapView = new MapView();
        VBox.setVgrow(mapView, Priority.ALWAYS);

        // Panneau analyse de lieu (sous la carte)
        Label analysisTitle = new Label("📊 Analyse des lieux");
        analysisTitle.setStyle("-fx-font-weight:800; -fx-font-size:14px; -fx-text-fill:#0f2942;");
        javafx.scene.control.TextArea analysisArea = new javafx.scene.control.TextArea();
        analysisArea.setEditable(false);
        analysisArea.setPrefRowCount(5);
        analysisArea.setStyle("-fx-background-color:#f8fbff; -fx-background-radius:12;"
                + "-fx-border-color:#d7e7ff; -fx-border-radius:12; -fx-font-size:12px;");
        analysisArea.setText("Chargement de l'analyse…");

        MapService mapService = new MapService();

        Runnable loadMap = () -> {
            try {
                String cat = "Tous".equals(catFilter.getValue()) ? null : catFilter.getValue();
                List<Event> events = cat == null
                        ? eventService.getAllEvents()
                        : eventService.filterEvents(cat, null, null);
                java.util.Map<Integer, Integer> resCounts = reservationService.getReservationCountsByEvent();
                mapView.loadEvents(events, resCounts);

                // Analyse en arrière-plan
                Thread t = new Thread(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("📍 ").append(events.size()).append(" événement(s) chargé(s)\n\n");

                    // Calculer scores pour chaque lieu
                    java.util.List<String[]> scored = new java.util.ArrayList<>();
                    for (Event ev : events) {
                        double[] coords = mapService.geocode(ev.getLieu());
                        if (coords != null) {
                            double score = mapService.getLocationScore(coords[0], coords[1]);
                            scored.add(new String[]{ev.getTitre(), ev.getLieu(), String.valueOf((int) score),
                                    mapService.getLocationLabel(score)});
                        }
                    }
                    scored.sort((a, b) -> Integer.compare(Integer.parseInt(b[2]), Integer.parseInt(a[2])));

                    if (!scored.isEmpty()) {
                        sb.append("🏆 Meilleur lieu : ").append(scored.get(0)[1])
                          .append(" (score ").append(scored.get(0)[2]).append("/100)\n");
                        sb.append("   → ").append(scored.get(0)[3]).append("\n\n");
                        sb.append("Classement des lieux :\n");
                        for (int i = 0; i < Math.min(scored.size(), 5); i++) {
                            String[] s = scored.get(i);
                            sb.append(String.format("  %d. %s — %s (%s/100)\n",
                                    i + 1, s[0], s[1], s[2]));
                        }
                    } else {
                        sb.append("Aucun lieu géolocalisé. Vérifiez les adresses des événements.");
                    }

                    String result = sb.toString();
                    javafx.application.Platform.runLater(() -> analysisArea.setText(result));
                }, "map-analysis");
                t.setDaemon(true);
                t.start();

            } catch (java.sql.SQLException ex) { showError(ex.getMessage()); }
        };

        catFilter.setOnAction(e -> loadMap.run());
        refreshBtn.setOnAction(e -> loadMap.run());
        loadMap.run();

        VBox analysisBox = new VBox(8, analysisTitle, analysisArea);
        analysisBox.setPadding(new Insets(16));
        analysisBox.setStyle("-fx-background-color:white; -fx-background-radius:14;"
                + "-fx-border-color:#e8f0fe; -fx-border-radius:14;");

        VBox page = new VBox(14, pageTitle, pageSub, toolbar, mapView, analysisBox);
        page.setPadding(new Insets(8));
        VBox.setVgrow(mapView, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox root = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        contentArea.getChildren().setAll(root);
    }

    private VBox buildHowItWorksBox() {
        Label title = new Label("⚙️ Comment fonctionne l'IA ?");
        title.setStyle("-fx-font-size:14px; -fx-font-weight:800; -fx-text-fill:#0f2942;");
        String[][] steps = {
            {"❌ Élimination",   "Supprime les créneaux saturés, conflits et périodes peu actives"},
            {"🟡 Identification","Repère les moments où les étudiants sont actifs et les créneaux libres"},
            {"🥇 Classement",    "Score pondéré : historique 25%, disponibilité 20%, catégorie 15%, jour 15%, saison 15%, engagement 10%"},
            {"🎯 Prédiction",    "Calcule la probabilité de succès de chaque créneau"},
        };
        VBox stepsBox = new VBox(8);
        for (String[] step : steps) {
            Label t = new Label(step[0]);
            t.setStyle("-fx-font-weight:800; -fx-font-size:12px; -fx-text-fill:#0f2942;");
            Label d = new Label(step[1]);
            d.setStyle("-fx-text-fill:#637a97; -fx-font-size:11px;");
            d.setWrapText(true);
            stepsBox.getChildren().add(new VBox(2, t, d));
        }
        VBox box = new VBox(10, title, stepsBox);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color:#f0f4ff; -fx-background-radius:14; -fx-border-color:#d7e7ff; -fx-border-radius:14;");
        return box;
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    private void logout() {
        stage.close();
        try {
            new Main().start(new Stage());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private TextField inputField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle(INPUT);
        return tf;
    }

    private Button btn(String text, String style) {
        Button b = new Button(text);
        b.setStyle(style);
        return b;
    }

    private StackPane createImagePane(String path, double width, double height, String fallbackText) {
        StackPane box = new StackPane();
        box.setMinSize(width, height);
        box.setPrefSize(width, height);
        box.setMaxSize(width, height);
        box.setStyle("-fx-background-color: linear-gradient(to bottom right,#44b9d0,#1b5f8e);"
                + "-fx-background-radius:16;");
        box.setClip(new javafx.scene.shape.Rectangle(width, height) {{
            setArcWidth(16); setArcHeight(16);
        }});
        if (path != null && !path.isBlank()) {
            File file = new File(path);
            if (!file.isAbsolute()) file = new File(System.getProperty("user.dir"), path);
            if (file.exists()) {
                // Charger en pleine résolution puis laisser JavaFX redimensionner avec lissage
                Image img = new Image(file.toURI().toString());
                ImageView iv = new ImageView(img);
                iv.setFitWidth(width);
                iv.setFitHeight(height);
                iv.setPreserveRatio(false);
                iv.setSmooth(true);
                // Clip arrondi sur l'image
                iv.setClip(new javafx.scene.shape.Rectangle(width, height) {{
                    setArcWidth(16); setArcHeight(16);
                }});
                box.getChildren().add(iv);
                return box;
            }
        }
        Label l = new Label(fallbackText);
        l.setStyle("-fx-text-fill:white; -fx-font-size:14px; -fx-font-weight:800;");
        l.setWrapText(true);
        l.setMaxWidth(width - 20);
        l.setAlignment(javafx.geometry.Pos.CENTER);
        box.getChildren().add(l);
        return box;
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }

    private void showWarning(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }
}
