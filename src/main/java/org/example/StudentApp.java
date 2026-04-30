package org.example;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.auth.AppUser;
import org.example.event.Event;
import org.example.event.EventEngagementService;
import org.example.event.EventReview;
import org.example.event.EventService;
import org.example.reservation.ReservationRecord;
import org.example.reservation.ReservationService;
import org.example.reservation.ReservationStatus;
import org.example.util.DualMonthCalendarView;
import org.example.util.EmailService;
import org.example.util.MapService;
import org.example.util.MapView;
import org.example.util.StudentMapView;
import org.example.util.WeatherService;
import org.example.event.RecommendationService;
import org.example.event.RecommendationResult;
import org.example.util.QRCodeService;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Student front-end: browse events, reserve, like, review.
 */
public class StudentApp {

    // ── Style constants ──────────────────────────────────────────────────────
    private static final String ROOT      = "-fx-background-color: #F9FAFB;";
    private static final String CARD      = "-fx-background-color: white; -fx-background-radius: 18; -fx-border-radius: 18; -fx-border-color: #E5E7EB; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);";
    private static final String PRIMARY   = "-fx-background-color: linear-gradient(to right,#2563EB,#3B82F6); -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 11 20 11 20; -fx-cursor: hand;";
    private static final String SECONDARY = "-fx-background-color: white; -fx-text-fill: #2563EB; -fx-font-weight: 700; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #BFDBFE; -fx-padding: 10 16 10 16; -fx-cursor: hand;";
    private static final String DANGER    = "-fx-background-color: #FEF2F2; -fx-text-fill: #DC2626; -fx-font-weight: 700; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #FECACA; -fx-padding: 10 16 10 16; -fx-cursor: hand;";
    private static final String INPUT     = "-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #E5E7EB; -fx-padding: 10 14 10 14; -fx-font-size: 13px;";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Services ─────────────────────────────────────────────────────────────
    private final EventService             eventService             = new EventService();
    private final ReservationService       reservationService       = new ReservationService();
    private final EventEngagementService   engagementService        = new EventEngagementService();
    private final EmailService             emailService             = new EmailService();
    private final RecommendationService    recommendationService    = new RecommendationService();
    private final WeatherService           weatherService           = new WeatherService();
    private final QRCodeService            qrCodeService            = new QRCodeService();

    // ── State ─────────────────────────────────────────────────────────────────
    private final AppUser currentUser;
    private final Stage   stage = new Stage();
    private StackPane     contentArea;

    // Nav buttons
    private Button btnEvents;
    private Button btnCalendar;
    private Button btnMyReservations;
    private Button btnMap;

    public StudentApp(AppUser user) {
        this.currentUser = user;
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle(ROOT);
        root.setTop(buildNavBar());

        contentArea = new StackPane();
        contentArea.setPadding(new Insets(20));
        root.setCenter(contentArea);

        Scene scene = new Scene(root, 1280, 820);
        stage.setScene(scene);
        stage.setTitle("MindCare Events — Espace Étudiant");
        stage.show();

        showEventsPage();
    }

    // ── Navigation bar ────────────────────────────────────────────────────────
    private HBox buildNavBar() {
        // Logo with icon
        Label logoIcon = new Label("🧠");
        logoIcon.setStyle("-fx-font-size: 22px;");
        Label logoText = new Label("MindCare");
        logoText.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 20px; -fx-font-weight: 900;");
        HBox logo = new HBox(6, logoIcon, logoText);
        logo.setAlignment(Pos.CENTER_LEFT);

        btnEvents = navBtn("Événements");
        btnEvents.setOnAction(e -> showEventsPage());

        btnCalendar = navBtn("📅 Calendrier");
        btnCalendar.setOnAction(e -> showCalendarPage());

        btnMyReservations = navBtn("Mes Réservations");
        btnMyReservations.setOnAction(e -> showMyReservationsPage());

        btnMap = navBtn("🗺️ Carte");
        btnMap.setOnAction(e -> showMapPageStudent());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label("👤 " + currentUser.getUsername());
        badge.setStyle("-fx-text-fill: #2563EB; -fx-background-color: #EFF6FF;"
                + "-fx-background-radius: 999; -fx-padding: 7 16 7 16;"
                + "-fx-font-weight: 700; -fx-font-size: 13px;");

        Button logout = new Button("🚪 Déconnexion");
        logout.setStyle("-fx-background-color: #FEF2F2; -fx-text-fill: #DC2626;"
                + "-fx-font-weight: 700; -fx-background-radius: 10; -fx-border-radius: 10;"
                + "-fx-border-color: #FECACA; -fx-padding: 8 14; -fx-cursor: hand;");
        logout.setOnAction(e -> logout());

        HBox nav = new HBox(20, logo, btnEvents, btnCalendar, btnMyReservations, btnMap, spacer, badge, logout);
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(14, 28, 14, 28));
        nav.setStyle("-fx-background-color: white;"
                + "-fx-border-color: #E5E7EB; -fx-border-width: 0 0 1 0;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 6, 0, 0, 2);");
        return nav;
    }

    private Button navBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #374151;"
                + "-fx-font-weight: 600; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 8 14;"
                + "-fx-background-radius: 10;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #2563EB;"
                + "-fx-font-weight: 700; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 8 14;"
                + "-fx-background-radius: 10;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: #374151;"
                + "-fx-font-weight: 600; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 8 14;"
                + "-fx-background-radius: 10;"));
        return b;
    }

    private void setActiveNav(Button active) {
        String normal  = "-fx-background-color: transparent; -fx-text-fill: #374151;"
                + "-fx-font-weight: 600; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 8 14;"
                + "-fx-background-radius: 10;";
        String activeS = "-fx-background-color: #EFF6FF; -fx-text-fill: #2563EB;"
                + "-fx-font-weight: 800; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 8 14;"
                + "-fx-background-radius: 10; -fx-border-color: #BFDBFE; -fx-border-radius: 10; -fx-border-width: 1;";
        if (btnEvents != null)         btnEvents.setStyle(btnEvents == active ? activeS : normal);
        if (btnCalendar != null)       btnCalendar.setStyle(btnCalendar == active ? activeS : normal);
        if (btnMyReservations != null) btnMyReservations.setStyle(btnMyReservations == active ? activeS : normal);
        if (btnMap != null)            btnMap.setStyle(btnMap == active ? activeS : normal);
    }

    // ── Calendar page ─────────────────────────────────────────────────────────
    private void showCalendarPage() {
        setActiveNav(btnCalendar);

        Label pageTitle = new Label("Calendrier des événements");
        pageTitle.setStyle("-fx-text-fill:#0f2942; -fx-font-size:26px; -fx-font-weight:800;");

        Label pageSubtitle = new Label("Sélectionnez une date pour voir les événements disponibles.");
        pageSubtitle.setStyle("-fx-text-fill:#637a97; -fx-font-size:13px;");

        // ── Dual-month calendar ──
        DualMonthCalendarView calendar = new DualMonthCalendarView(
                eventService, reservationService, this::loadCalendarForDate);

        // Store reference and force fresh reload from DB every time page is shown
        currentCalendar = calendar;
        calendar.reloadFromDatabase();

        // Load CSS for calendar styles
        java.net.URL cssUrl = getClass().getResource("/calendar-styles.css");
        if (cssUrl != null) {
            calendar.getStylesheets().add(cssUrl.toExternalForm());
        }

        VBox calendarBox = new VBox(calendar);
        calendarBox.setPadding(new Insets(16));
        calendarBox.setStyle("-fx-background-color: white; -fx-background-radius: 18; "
                + "-fx-border-color: #d7e7ff; -fx-border-radius: 18; "
                + "-fx-effect: dropshadow(gaussian, rgba(46,94,166,0.08), 14, 0, 0, 4);");

        // ── Events list for selected date ──
        Label eventsForDateTitle = new Label("Choisissez une date pour voir les événements");
        eventsForDateTitle.setStyle("-fx-text-fill:#10233f; -fx-font-size:15px; -fx-font-weight:800;");

        calendarEventsBox = new VBox(10);
        calendarEventsBox.setPadding(new Insets(4, 0, 0, 0));

        Label noDateLabel = new Label("Cliquez sur une date du calendrier ci-dessus.");
        noDateLabel.setStyle("-fx-text-fill:#9ab0cc; -fx-font-size:13px; -fx-font-style:italic;");
        calendarEventsBox.getChildren().add(noDateLabel);

        // Legend
        HBox legend = new HBox(20,
                legendDot("#f59e0b", "Peu de places"),
                legendDot("#22c55e", "Disponible"),
                legendDot("#9e9e9e", "Complet"));
        legend.setAlignment(Pos.CENTER_LEFT);

        // Refresh button
        Button refreshBtn = new Button("Actualiser");
        refreshBtn.setStyle(SECONDARY);
        refreshBtn.setOnAction(e -> {
            calendar.reloadFromDatabase();
        });

        HBox calendarHeader = new HBox(12, eventsForDateTitle);
        calendarHeader.setAlignment(Pos.CENTER_LEFT);

        ScrollPane eventsScroll = new ScrollPane(calendarEventsBox);
        eventsScroll.setFitToWidth(true);
        eventsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        eventsScroll.setPrefHeight(300);

        VBox eventsPanel = new VBox(12, calendarHeader, eventsScroll);
        eventsPanel.setPadding(new Insets(16));
        eventsPanel.setStyle("-fx-background-color: #f8fbff; -fx-background-radius: 18; "
                + "-fx-border-color: #d7e7ff; -fx-border-radius: 18;");

        HBox topRow = new HBox(12, refreshBtn, legend);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox page = new VBox(18, pageTitle, pageSubtitle, topRow, calendarBox, eventsPanel);
        page.setPadding(new Insets(8));

        ScrollPane pageScroll = new ScrollPane(page);
        pageScroll.setFitToWidth(true);
        pageScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        contentArea.getChildren().setAll(pageScroll);
    }

    private VBox calendarEventsBox; // shared reference updated by loadCalendarForDate
    private DualMonthCalendarView currentCalendar; // reference to reload

    private void loadCalendarForDate(LocalDate date) {
        if (calendarEventsBox == null) return;
        calendarEventsBox.getChildren().clear();

        try {
            LocalDateTime from = date.atStartOfDay();
            LocalDateTime to   = date.plusDays(1).atStartOfDay();
            List<Event> events = eventService.getEventsInRange(from, to);

            if (events.isEmpty()) {
                Label none = new Label("Aucun événement le "
                        + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                none.setStyle("-fx-text-fill:#9ab0cc; -fx-font-size:13px; -fx-font-style:italic;");
                calendarEventsBox.getChildren().add(none);
                return;
            }

            Map<Integer, Integer> resCounts = reservationService.getReservationCountsByEvent();
            Set<Integer> reserved = reservationService.getReservedEventIdsByUser(currentUser.getId());

            Runnable[] reloadRef = new Runnable[1];
            reloadRef[0] = () -> loadCalendarForDate(date);

            for (Event ev : events) {
                int resCount  = resCounts.getOrDefault(ev.getId(), 0);
                int remaining = ev.getCapacite() - resCount;
                boolean isReserved = reserved.contains(ev.getId());

                // Compact event row card
                StackPane img = createImagePane(ev.getImage(), 80, 60, ev.getTitre());
                img.setCursor(Cursor.HAND);
                img.setOnMouseClicked(e -> showEventDetails(ev, reloadRef[0]));

                Label titleLbl = new Label(ev.getTitre());
                titleLbl.setStyle("-fx-font-weight:800; -fx-font-size:14px; -fx-text-fill:#10233f;");
                titleLbl.setWrapText(true);

                Label dateLbl = new Label("📅 " + ev.getDateEvent().format(FMT)
                        + "   📍 " + ev.getLieu());
                dateLbl.setStyle("-fx-text-fill:#637a97; -fx-font-size:12px;");

                String spotsText = remaining > 0 ? remaining + " place(s)" : "Complet";
                Label spotsLbl = new Label(spotsText);
                spotsLbl.setStyle("-fx-text-fill:" + (remaining > 0 ? "#1a7a4a" : "#c63d48")
                        + "; -fx-font-size:12px; -fx-font-weight:700;");

                Button reserveBtn = new Button(isReserved ? "✓ Réservé" : (remaining <= 0 ? "Complet" : "Réserver"));
                reserveBtn.setStyle(isReserved || remaining <= 0 ? SECONDARY : PRIMARY);
                reserveBtn.setDisable(isReserved || remaining <= 0);
                reserveBtn.setOnAction(e -> showReservationDialog(ev, reloadRef[0]));

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                VBox info = new VBox(4, titleLbl, dateLbl, spotsLbl);
                HBox.setHgrow(info, Priority.ALWAYS);

                HBox row = new HBox(14, img, info, spacer, reserveBtn);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(12, 16, 12, 16));
                row.setStyle("-fx-background-color: white; -fx-background-radius: 14; "
                        + "-fx-border-color: #e8f0fe; -fx-border-radius: 14; "
                        + "-fx-effect: dropshadow(gaussian, rgba(46,94,166,0.06), 6, 0, 0, 2);");
                row.setCursor(Cursor.HAND);
                row.setOnMouseClicked(e -> showEventDetails(ev, reloadRef[0]));

                calendarEventsBox.getChildren().add(row);
            }
        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    private HBox legendDot(String color, String label) {
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(7);
        dot.setStyle("-fx-fill: " + color + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill:#637a97; -fx-font-size:12px;");
        HBox item = new HBox(6, dot, lbl);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    // ── Events page ───────────────────────────────────────────────────────────
    private void showEventsPage() {
        setActiveNav(btnEvents);

        // ── Page background ──
        contentArea.setStyle("-fx-background-color: #F8F9FA;");

        // ── Page title ──
        Label pageTitle = new Label("Événements disponibles");
        pageTitle.setStyle("-fx-text-fill:#1a1a2e; -fx-font-size:28px; -fx-font-weight:900;");
        Label pageSub = new Label("Découvrez et réservez les événements qui vous correspondent");
        pageSub.setStyle("-fx-text-fill:#6c757d; -fx-font-size:13px;");

        // ── Search bar with icon ──
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size:16px; -fx-padding: 0 4 0 8;");
        TextField search = new TextField();
        search.setPromptText("Rechercher un événement...");
        search.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;"
                + "-fx-font-size: 14px; -fx-text-fill: #222;");
        HBox.setHgrow(search, Priority.ALWAYS);
        HBox searchBox = new HBox(4, searchIcon, search);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setStyle("-fx-background-color: white; -fx-background-radius: 14;"
                + "-fx-border-color: #dee2e6; -fx-border-radius: 14;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);"
                + "-fx-padding: 4 8;");
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        // ── Filters ──
        ComboBox<String> filterCat = new ComboBox<>(FXCollections.observableArrayList(
                "🎯 Catégorie", "yoga", "wellness", "sport", "meditation", "conference", "atelier"));
        filterCat.setValue("🎯 Catégorie");
        filterCat.setStyle(INPUT + "-fx-pref-width: 150px;");

        ComboBox<String> filterSort = new ComboBox<>(FXCollections.observableArrayList(
                "📅 Trier par", "Date", "Capacite", "Lieu"));
        filterSort.setValue("📅 Trier par");
        filterSort.setStyle(INPUT + "-fx-pref-width: 140px;");

        Button clearBtn = new Button("✕ Effacer");
        clearBtn.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #6c757d;"
                + "-fx-font-weight: 700; -fx-background-radius: 12; -fx-border-radius: 12;"
                + "-fx-border-color: #dee2e6; -fx-padding: 10 16; -fx-cursor: hand;");
        clearBtn.setOnAction(e -> {
            search.clear();
            filterCat.setValue("🎯 Catégorie");
            filterSort.setValue("📅 Trier par");
        });

        HBox toolbar = new HBox(10, searchBox, filterCat, filterSort, clearBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(12, 16, 12, 16));
        toolbar.setStyle("-fx-background-color: white; -fx-background-radius: 16;"
                + "-fx-border-color: #e9ecef; -fx-border-radius: 16;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 6, 0, 0, 2);");

        // ── Date strip ──
        LocalDate stripToday = LocalDate.now();
        DateTimeFormatter stripMonthFmt = DateTimeFormatter.ofPattern("MMM. yyyy", java.util.Locale.FRENCH);
        Label availTitle = new Label("📅 Vérifiez les disponibilités");
        availTitle.setStyle("-fx-text-fill:#1a1a2e; -fx-font-size:15px; -fx-font-weight:800;");
        Label availMonth = new Label(stripToday.format(stripMonthFmt).toUpperCase());
        availMonth.setStyle("-fx-text-fill:#adb5bd; -fx-font-size:11px; -fx-font-weight:700;");

        HBox dateStrip = buildDateStrip();
        ScrollPane dateScroll = new ScrollPane(dateStrip);
        dateScroll.setFitToHeight(true);
        dateScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        dateScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        dateScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox stripSection = new VBox(8, availTitle, availMonth, dateScroll);
        stripSection.setPadding(new Insets(16, 18, 16, 18));
        stripSection.setStyle("-fx-background-color: white; -fx-background-radius: 18;"
                + "-fx-border-color: #e9ecef; -fx-border-radius: 18;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");

        // ── Event grid ──
        TilePane grid = new TilePane();
        grid.setHgap(22);
        grid.setVgap(22);
        grid.setPrefColumns(3);
        grid.setTileAlignment(Pos.TOP_LEFT);
        grid.setPrefTileWidth(380);
        grid.setPadding(new Insets(4, 0, 12, 0));

        Runnable[] loadGridRef = new Runnable[1];
        loadGridRef[0] = () -> {
            try {
                String q = search.getText();
                String catFilter = filterCat.getValue();
                String sortVal   = filterSort.getValue();

                String sortBy = "📅 Trier par".equals(sortVal) ? null : sortVal;
                List<Event> events = eventService.getEvents(
                        q == null || q.isBlank() ? null : q, sortBy);

                // Apply category filter client-side
                if (catFilter != null && !catFilter.startsWith("🎯")) {
                    events = events.stream()
                            .filter(ev -> catFilter.equalsIgnoreCase(ev.getCategorie()))
                            .collect(java.util.stream.Collectors.toList());
                }

                Map<Integer, Integer> resCounts  = reservationService.getReservationCountsByEvent();
                Set<Integer>          reserved   = reservationService.getReservedEventIdsByUser(currentUser.getId());
                Set<Integer>          liked      = engagementService.getLikedEventIdsByUser(currentUser.getId());
                Map<Integer, Integer> likeCounts = engagementService.getLikeCountsByEvent();

                grid.getChildren().clear();
                for (Event ev : events) {
                    int resCount  = resCounts.getOrDefault(ev.getId(), 0);
                    int remaining = ev.getCapacite() - resCount;
                    boolean isReserved = reserved.contains(ev.getId());
                    boolean isLiked    = liked.contains(ev.getId());
                    int likeCount      = likeCounts.getOrDefault(ev.getId(), 0);
                    grid.getChildren().add(buildEventCard(ev, remaining, isReserved, isLiked, likeCount, loadGridRef[0]));
                }
            } catch (SQLException ex) { showError(ex.getMessage()); }
        };

        search.textProperty().addListener((obs, o, n) -> loadGridRef[0].run());
        filterCat.setOnAction(e -> loadGridRef[0].run());
        filterSort.setOnAction(e -> loadGridRef[0].run());
        clearBtn.setOnAction(e -> {
            search.clear();
            filterCat.setValue("🎯 Catégorie");
            filterSort.setValue("📅 Trier par");
            loadGridRef[0].run();
        });
        loadGridRef[0].run();

        // ── 🎯 Recommandé pour vous ──
        VBox recommendedSection = buildRecommendedSection(loadGridRef[0]);

        // ── 🗺️ Carte interactive intégrée ──
        Label mapTitle = new Label("🗺️ Événements près de vous");
        mapTitle.setStyle("-fx-text-fill:#1a1a2e; -fx-font-size:18px; -fx-font-weight:800;");
        Label mapSub = new Label("La carte détecte votre position et affiche les événements triés par distance");
        mapSub.setStyle("-fx-text-fill:#6c757d; -fx-font-size:12px;");

        // WebView avec taille FIXE — clé pour éviter les trous gris
        javafx.scene.web.WebView mapWebView = new javafx.scene.web.WebView();
        mapWebView.setPrefWidth(840);
        mapWebView.setPrefHeight(480);
        mapWebView.setMinHeight(480);
        mapWebView.setMaxHeight(480);
        mapWebView.setContextMenuEnabled(false);

        javafx.scene.web.WebEngine mapEngine = mapWebView.getEngine();
        mapEngine.setUserAgent(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        );

        // Charger la carte et les événements APRÈS que le layout est finalisé
        mapWebView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;
            // Attendre que le layout soit complètement rendu
            Platform.runLater(() -> {
                try {
                    String leafletJs  = new String(
                        StudentMapView.class.getResourceAsStream("/leaflet.js").readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8);
                    String leafletCss = new String(
                        StudentMapView.class.getResourceAsStream("/leaflet.css").readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8);

                    String html = buildInlineMapHtml(leafletJs, leafletCss);
                    mapEngine.loadContent(html);

                    // Charger les événements après que la page est prête
                    mapEngine.getLoadWorker().stateProperty().addListener((obs2, o2, n2) -> {
                        if (n2 != javafx.concurrent.Worker.State.SUCCEEDED) return;
                        // invalidateSize immédiat
                        mapEngine.executeScript(
                            "setTimeout(function(){map.invalidateSize();},100);" +
                            "setTimeout(function(){map.invalidateSize();},300);" +
                            "setTimeout(function(){map.invalidateSize();},700);"
                        );
                        // Charger les marqueurs
                        new Thread(() -> {
                            try {
                                List<Event> evs = eventService.getAllEvents();
                                Map<Integer, Integer> rc = reservationService.getReservationCountsByEvent();
                                MapService ms = new MapService();
                                String json = ms.buildMarkersJson(evs, rc);
                                Platform.runLater(() -> {
                                    try {
                                        mapEngine.executeScript("loadMarkers(" + json + ")");
                                        mapEngine.executeScript(
                                            "setTimeout(function(){map.invalidateSize();},200);" +
                                            "setTimeout(function(){map.invalidateSize();},500);"
                                        );
                                    } catch (Exception ignored) {}
                                });
                            } catch (Exception ex) {
                                System.err.println("[InlineMap] " + ex.getMessage());
                            }
                        }, "inline-map-load").start();
                    });
                } catch (Exception ex) {
                    System.err.println("[InlineMap] " + ex.getMessage());
                }
            });
        });

        VBox mapSection = new VBox(8, mapTitle, mapSub, mapWebView);
        mapSection.setPadding(new Insets(16, 18, 16, 18));
        mapSection.setStyle("-fx-background-color: white; -fx-background-radius: 18;" +
                "-fx-border-color: #e9ecef; -fx-border-radius: 18;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");

        // ── Section title for events ──
        Label eventsTitle = new Label("🔥 Tous les événements");
        eventsTitle.setStyle("-fx-text-fill:#1a1a2e; -fx-font-size:18px; -fx-font-weight:800;");

        VBox page = new VBox(16, pageTitle, pageSub, toolbar, stripSection,
                recommendedSection, mapSection, eventsTitle, grid);
        page.setPadding(new Insets(8));
        page.setStyle("-fx-background-color: #F8F9FA;");

        ScrollPane mainScroll = new ScrollPane(page);
        mainScroll.setFitToWidth(true);
        mainScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        mainScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mainScroll.setStyle("-fx-background-color: #F8F9FA; -fx-background: #F8F9FA;");
        VBox.setVgrow(mainScroll, Priority.ALWAYS);

        contentArea.getChildren().setAll(mainScroll);
    }

    private VBox buildEventCard(Event ev, int remaining, boolean isReserved, boolean isLiked,
                                int likeCount, Runnable reload) {
        StackPane img = createImagePane(ev.getImage(), 380, 210, ev.getTitre());
        img.setCursor(javafx.scene.Cursor.HAND);
        img.setOnMouseClicked(e -> showEventDetails(ev, reload));

        // Category badge
        Label catTag = new Label(ev.getCategorie() != null ? ev.getCategorie().toUpperCase() : "GÉNÉRAL");
        catTag.setStyle("-fx-background-color: #e8f5e9; -fx-text-fill:#2e7d32;"
                + "-fx-font-size:10px; -fx-font-weight:900; -fx-background-radius:6; -fx-padding:3 10;");

        // Title
        Label title = new Label(ev.getTitre());
        title.setStyle("-fx-font-size:16px; -fx-font-weight:900; -fx-text-fill:#1a1a2e;");
        title.setWrapText(true);

        // Date & location
        Label dateLabel = new Label("📅 " + (ev.getDateEvent() != null ? ev.getDateEvent().format(FMT) : ""));
        dateLabel.setStyle("-fx-text-fill:#495057; -fx-font-size:12px; -fx-font-weight:600;");
        Label lieuLabel = new Label("📍 " + ev.getLieu());
        lieuLabel.setStyle("-fx-text-fill:#495057; -fx-font-size:12px;");

        // Spots badge
        String spotsText = remaining > 0 ? "✅ " + remaining + " place(s)" : "🔴 Complet";
        String spotsColor = remaining > 0 ? "#1a7a4a" : "#c63d48";
        String spotsBg    = remaining > 0 ? "#e8f5e9" : "#fce4ec";
        Label spots = new Label(spotsText);
        spots.setStyle("-fx-text-fill:" + spotsColor + "; -fx-background-color:" + spotsBg
                + "; -fx-font-size:11px; -fx-font-weight:700;"
                + "-fx-background-radius:6; -fx-padding:3 10;");

        // Like button
        Button likeBtn = new Button((isLiked ? "❤️" : "🤍") + " " + likeCount);
        likeBtn.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #495057;"
                + "-fx-font-size:12px; -fx-background-radius:10; -fx-border-radius:10;"
                + "-fx-border-color:#dee2e6; -fx-padding:7 12; -fx-cursor:hand;");
        likeBtn.setOnAction(e -> {
            try { engagementService.toggleLike(ev.getId(), currentUser.getId()); reload.run(); }
            catch (SQLException ex) { showError(ex.getMessage()); }
        });

        // Details button
        Button detailsBtn = new Button("👁 Voir détails");
        detailsBtn.setStyle("-fx-background-color: #f0f4ff; -fx-text-fill: #3d5a99;"
                + "-fx-font-weight:700; -fx-font-size:12px; -fx-background-radius:10;"
                + "-fx-border-radius:10; -fx-border-color:#c5d0e6; -fx-padding:7 14; -fx-cursor:hand;");
        detailsBtn.setOnAction(e -> showEventDetails(ev, reload));

        // Reserve button
        Button reserveBtn = new Button(isReserved ? "✓ Réservé" : "🎟 Réserver maintenant");
        reserveBtn.setStyle(isReserved
                ? "-fx-background-color:#e9ecef; -fx-text-fill:#6c757d; -fx-font-weight:700;"
                  + "-fx-background-radius:10; -fx-padding:9 16; -fx-font-size:12px;"
                : "-fx-background-color: linear-gradient(to right,#2e7d32,#43a047);"
                  + "-fx-text-fill:white; -fx-font-weight:800; -fx-font-size:13px;"
                  + "-fx-background-radius:12; -fx-padding:10 20; -fx-cursor:hand;");
        reserveBtn.setDisable(isReserved || remaining <= 0);
        reserveBtn.setMaxWidth(Double.MAX_VALUE);
        reserveBtn.setOnAction(e -> showReservationDialog(ev, reload));

        // Hover effect on reserve button
        if (!isReserved && remaining > 0) {
            reserveBtn.setOnMouseEntered(e -> reserveBtn.setStyle(
                    "-fx-background-color: linear-gradient(to right,#1b5e20,#2e7d32);"
                    + "-fx-text-fill:white; -fx-font-weight:800; -fx-font-size:13px;"
                    + "-fx-background-radius:12; -fx-padding:10 20; -fx-cursor:hand;"
                    + "-fx-effect: dropshadow(gaussian, rgba(46,125,50,0.4), 10, 0, 0, 3);"));
            reserveBtn.setOnMouseExited(e -> reserveBtn.setStyle(
                    "-fx-background-color: linear-gradient(to right,#2e7d32,#43a047);"
                    + "-fx-text-fill:white; -fx-font-weight:800; -fx-font-size:13px;"
                    + "-fx-background-radius:12; -fx-padding:10 20; -fx-cursor:hand;"));
        }

        HBox topActions = new HBox(8, likeBtn, detailsBtn);
        topActions.setAlignment(Pos.CENTER_LEFT);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#e9ecef;");

        VBox body = new VBox(8, catTag, title, dateLabel, lieuLabel, spots, sep, topActions, reserveBtn);
        body.setPadding(new Insets(14, 16, 16, 16));

        VBox card = new VBox(img, body);
        card.setPrefWidth(380);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 18;"
                + "-fx-border-radius: 18; -fx-border-color: #e9ecef;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);");

        // Hover effect on card
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 18;"
                + "-fx-border-radius: 18; -fx-border-color: #adb5bd;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 18, 0, 0, 6);"
                + "-fx-scale-x: 1.01; -fx-scale-y: 1.01;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 18;"
                + "-fx-border-radius: 18; -fx-border-color: #e9ecef;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);"));

        return card;
    }

    private void showReservationDialog(Event ev, Runnable reload) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Réserver : " + ev.getTitre());
        dialog.setHeaderText(null);

        TextField nomField    = inputField("Nom");
        TextField prenomField = inputField("Prénom");
        TextField telField    = inputField("Téléphone");
        TextField emailField  = inputField("Email");

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(12);
        ColumnConstraints lc = new ColumnConstraints(100);
        ColumnConstraints fc = new ColumnConstraints(); fc.setHgrow(Priority.ALWAYS); fc.setFillWidth(true);
        form.getColumnConstraints().addAll(lc, fc);
        addDialogRow(form, 0, "Nom",       nomField);
        addDialogRow(form, 1, "Prénom",    prenomField);
        addDialogRow(form, 2, "Téléphone", telField);
        addDialogRow(form, 3, "Email",     emailField);
        form.setPadding(new Insets(16));
        form.setPrefWidth(380);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                // ── Contrôle de saisie ────────────────────────────────────────
                String nom    = nomField.getText().trim();
                String prenom = prenomField.getText().trim();
                String tel    = telField.getText().trim();
                String email  = emailField.getText().trim();
                try {
                    org.example.util.ValidationUtil.validateName(nom, "Le nom");
                    org.example.util.ValidationUtil.validateName(prenom, "Le prénom");
                    org.example.util.ValidationUtil.validatePhone(tel);
                    org.example.util.ValidationUtil.validateEmail(email);
                } catch (IllegalArgumentException ex) {
                    showError(ex.getMessage());
                    return;
                }
                // ─────────────────────────────────────────────────────────────
                try {
                    ReservationService.ReservationResult result = reservationService.reserveEvent(ev, currentUser.getId());
                    if (result.status == ReservationStatus.CONFIRMED) {
                        showInfo("Réservation confirmée !");
                    } else {
                        showInfo("Ajouté à la liste d'attente (position " + result.waitlistPosition + ").");
                    }
                    // Send confirmation email in background thread (non-blocking)
                    String toEmail = email;
                    if (!toEmail.isBlank()) {
                        String studentName = (prenom + " " + nom).trim();
                        String statusStr   = result.status.getValue();
                        Integer waitPos    = result.waitlistPosition;
                        int resId          = result.reservationId;
                        int confirmedCount;
                        try { confirmedCount = reservationService.getConfirmedCountByEvent(ev.getId()); }
                        catch (SQLException ignored) { confirmedCount = 0; }
                        final int finalConfirmed = confirmedCount;
                        new Thread(() -> emailService.sendReservationConfirmation(
                                toEmail, studentName, ev, statusStr, waitPos,
                                resId, currentUser.getId(), finalConfirmed
                        ), "email-sender").start();
                    }
                    reload.run();
                } catch (SQLException ex) {
                    showError(ex.getMessage());
                }
            }
        });
    }

    private void addDialogRow(GridPane grid, int row, String label, TextField field) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-weight:700; -fx-text-fill:#10233f;");
        grid.add(lbl, 0, row);
        grid.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private void showReviewDialog(Event ev) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Avis : " + ev.getTitre());
        dialog.setHeaderText(null);

        ComboBox<Integer> ratingBox = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        ratingBox.setValue(5);
        ratingBox.setStyle(INPUT);

        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Votre commentaire…");
        commentArea.setStyle(INPUT);
        commentArea.setPrefRowCount(4);

        // Pre-fill if existing review
        try {
            EventReview existing = engagementService.getReviewByEventAndUser(ev.getId(), currentUser.getId());
            if (existing != null) {
                ratingBox.setValue(existing.getRating());
                commentArea.setText(existing.getComment());
            }
        } catch (SQLException ignored) {}

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(12);
        ColumnConstraints lc = new ColumnConstraints(100);
        ColumnConstraints fc = new ColumnConstraints(); fc.setHgrow(Priority.ALWAYS); fc.setFillWidth(true);
        form.getColumnConstraints().addAll(lc, fc);
        Label noteLbl = new Label("Note (1-5)"); noteLbl.setStyle("-fx-font-weight:700; -fx-text-fill:#10233f;");
        Label commLbl = new Label("Commentaire"); commLbl.setStyle("-fx-font-weight:700; -fx-text-fill:#10233f;");
        form.add(noteLbl, 0, 0); form.add(ratingBox, 1, 0);
        form.add(commLbl, 0, 1); form.add(commentArea, 1, 1);
        form.setPadding(new Insets(16));
        form.setPrefWidth(400);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                // ── Contrôle de saisie ────────────────────────────────────────
                try {
                    org.example.util.ValidationUtil.validateComment(commentArea.getText());
                } catch (IllegalArgumentException ex) {
                    showError(ex.getMessage());
                    return;
                }
                // ─────────────────────────────────────────────────────────────
                try {
                    engagementService.addOrUpdateReview(ev.getId(), currentUser.getId(),
                            ratingBox.getValue(), commentArea.getText());
                    showInfo("Avis enregistré !");
                } catch (SQLException ex) { showError(ex.getMessage()); }
            }
        });
    }

    // ── My Reservations page ──────────────────────────────────────────────────
    private void showMyReservationsPage() {
        setActiveNav(btnMyReservations);

        Label pageTitle = new Label("Mes Réservations");
        pageTitle.setStyle("-fx-text-fill:#0f2942; -fx-font-size:26px; -fx-font-weight:800;");

        ListView<ReservationRecord> list = new ListView<>();
        list.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
        VBox.setVgrow(list, Priority.ALWAYS);

        Runnable loadList = () -> {
            try {
                List<ReservationRecord> all = reservationService.getAllReservations();
                List<ReservationRecord> mine = all.stream()
                        .filter(r -> r.getUserId() == currentUser.getId())
                        .toList();
                list.setItems(FXCollections.observableArrayList(mine));
            } catch (SQLException ex) { showError(ex.getMessage()); }
        };

        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(ReservationRecord r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setGraphic(null); return; }

                Label title = new Label(r.getEventTitle() != null ? r.getEventTitle() : "Événement #" + r.getEventId());
                title.setStyle("-fx-font-weight:800; -fx-font-size:14px; -fx-text-fill:#10233f;");

                Label date = new Label("📅 Réservé le " + (r.getReservedAt() != null ? r.getReservedAt().format(FMT) : ""));
                date.setStyle("-fx-text-fill:#637a97; -fx-font-size:12px;");

                // Status badge — we look up status via reservation id
                String statusText;
                String badgeStyle;
                ReservationStatus st = null;
                try {
                    st = reservationService.getReservationStatus(r.getId());
                    if (st == ReservationStatus.WAITLISTED) {
                        statusText = "En attente";
                        badgeStyle = "-fx-background-color:#fff3e0; -fx-text-fill:#e65100; -fx-background-radius:8; -fx-padding:3 10; -fx-font-weight:700; -fx-font-size:11px;";
                    } else {
                        statusText = "Confirmée";
                        badgeStyle = "-fx-background-color:#e6f9f0; -fx-text-fill:#1a7a4a; -fx-background-radius:8; -fx-padding:3 10; -fx-font-weight:700; -fx-font-size:11px;";
                    }
                } catch (SQLException ex) {
                    statusText = "Confirmée";
                    badgeStyle = "-fx-background-color:#e6f9f0; -fx-text-fill:#1a7a4a; -fx-background-radius:8; -fx-padding:3 10; -fx-font-weight:700; -fx-font-size:11px;";
                }
                Label statusBadge = new Label(statusText);
                statusBadge.setStyle(badgeStyle);

                Button cancelBtn = new Button("Annuler");
                cancelBtn.setStyle(DANGER);
                cancelBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Annuler la réservation pour « " + r.getEventTitle() + " » ?",
                            ButtonType.YES, ButtonType.NO);
                    confirm.setHeaderText(null);
                    confirm.showAndWait().ifPresent(bt -> {
                        if (bt == ButtonType.YES) {
                            try {
                                reservationService.cancelReservation(r.getEventId(), r.getId(), currentUser.getId());
                                loadList.run();
                            } catch (SQLException ex) { showError(ex.getMessage()); }
                        }
                    });
                });

                HBox meta = new HBox(14, date, statusBadge);
                meta.setAlignment(Pos.CENTER_LEFT);

                // QR Code & ticket viewer for CONFIRMED reservations
                VBox rightPanel = new VBox(10);
                rightPanel.setAlignment(Pos.CENTER_RIGHT);
                
                if (st == ReservationStatus.CONFIRMED) {
                    try {
                        Event ev = eventService.getEventById(r.getEventId());
                        if (ev != null && ev.getDateEvent() != null) {
                            // Générer le payload QR
                            String payload = qrCodeService.generateQRPayload(
                                    r.getId(), r.getUserId(), r.getEventId(), ev.getDateEvent());

                            // Générer QR localement via ZXing (sans internet)
                            String ticketUrl = qrCodeService.generateTicketUrl(payload);
                            byte[] qrBytes = qrCodeService.generateQRBytes(ticketUrl, 150);

                            // Créer l'image QR
                            ImageView qrImage = new ImageView();
                            qrImage.setFitWidth(80);
                            qrImage.setFitHeight(80);
                            qrImage.setPreserveRatio(true);
                            qrImage.setCursor(Cursor.HAND);

                            if (qrBytes != null) {
                                // Charger depuis les bytes locaux (pas de réseau)
                                Image img = new Image(new java.io.ByteArrayInputStream(qrBytes));
                                qrImage.setImage(img);
                            } else {
                                // Fallback réseau si ZXing échoue
                                String qrImageUrl = qrCodeService.generateQRImageUrl(ticketUrl, 150);
                                new Thread(() -> {
                                    try {
                                        Image img = new Image(qrImageUrl, true);
                                        Platform.runLater(() -> qrImage.setImage(img));
                                    } catch (Exception ex) {
                                        System.err.println("[StudentApp] QR fallback error: " + ex.getMessage());
                                    }
                                }, "qr-loader").start();
                            }

                            // Clic pour ouvrir le billet
                            qrImage.setOnMouseClicked(e -> openTicketPage(payload));

                            Button viewTicketBtn = new Button("🎟 Voir mon billet");
                            viewTicketBtn.setStyle("-fx-background-color: linear-gradient(to right,#2563EB,#3B82F6); "
                                    + "-fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 8; "
                                    + "-fx-padding: 8 12; -fx-font-size: 11px; -fx-cursor: hand;");
                            viewTicketBtn.setOnAction(e -> openTicketPage(payload));

                            rightPanel.getChildren().addAll(qrImage, viewTicketBtn);
                        }
                    } catch (SQLException ex) {
                        System.err.println("[StudentApp] QR generation error: " + ex.getMessage());
                    }
                }

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                
                VBox infoBox = new VBox(6, title, meta);
                HBox.setHgrow(infoBox, Priority.ALWAYS);
                
                HBox row = new HBox(12, infoBox, spacer, rightPanel, cancelBtn);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(12, 16, 12, 16));
                row.setStyle("-fx-background-color: white; -fx-background-radius:14; -fx-border-color:#edf2fb; -fx-border-radius:14;");
                setGraphic(row);
                setStyle("-fx-background-color: transparent; -fx-padding: 4 0;");
            }
        });

        loadList.run();

        Button refresh = new Button("Actualiser");
        refresh.setStyle(SECONDARY);
        refresh.setOnAction(e -> loadList.run());

        VBox page = new VBox(18, pageTitle, new HBox(refresh), list);
        page.setPadding(new Insets(8));
        VBox.setVgrow(list, Priority.ALWAYS);

        contentArea.getChildren().setAll(page);
    }

    // ── Event details window ──────────────────────────────────────────────────
    private void showEventDetails(Event ev, Runnable reload) {
        Stage detailStage = new Stage();
        detailStage.setTitle(ev.getTitre());
        detailStage.initOwner(stage);

        // ── Data ──
        int reserved = 0;
        try { reserved = reservationService.getReservationCountByEvent(ev.getId()); } catch (SQLException ignored) {}
        int remaining = Math.max(ev.getCapacite() - reserved, 0);
        boolean full = remaining <= 0;
        boolean alreadyReserved;
        try { alreadyReserved = reservationService.hasActiveReservation(ev.getId(), currentUser.getId()); }
        catch (SQLException ignored) { alreadyReserved = false; }
        int likeCount = 0;
        boolean alreadyLiked = false;
        try {
            likeCount    = engagementService.getLikeCountsByEvent().getOrDefault(ev.getId(), 0);
            alreadyLiked = engagementService.hasLikedEvent(ev.getId(), currentUser.getId());
        } catch (SQLException ignored) {}
        List<EventReview> reviews = new java.util.ArrayList<>();
        double avgRating = 0.0;
        try {
            reviews   = engagementService.getReviewsByEvent(ev.getId());
            avgRating = engagementService.getAverageRatingsByEvent().getOrDefault(ev.getId(), 0.0);
        } catch (SQLException ignored) {}

        // ── Strings ──
        String cat      = ev.getCategorie() != null ? ev.getCategorie() : "Général";
        String dateStr  = ev.getDateEvent() != null
                ? ev.getDateEvent().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH)) : "—";
        String heureStr = ev.getDateEvent() != null
                ? ev.getDateEvent().format(DateTimeFormatter.ofPattern("HH:mm")) : "—";
        int dureeH = ev.getDurationMinutes() / 60;
        int dureeM = ev.getDurationMinutes() % 60;
        String dureeStr = dureeH > 0
                ? (dureeM > 0 ? dureeH + "h" + dureeM : dureeH + " heure" + (dureeH > 1 ? "s" : ""))
                : dureeM + " min";

        // ── Météo placeholders (mis à jour en arrière-plan) ──
        Label metaWeatherIcon = new Label("🌡️");
        metaWeatherIcon.setStyle("-fx-font-size:22px;");
        Label metaWeatherText = new Label("…");
        metaWeatherText.setStyle("-fx-font-size:12px; -fx-text-fill:#555; -fx-font-weight:600;");
        VBox metaWeatherItem = new VBox(4, metaWeatherIcon, metaWeatherText);
        metaWeatherItem.setAlignment(Pos.CENTER);
        metaWeatherItem.setPadding(new Insets(0, 32, 0, 32));

        // Widget météo dans le panneau info
        Label wIcon   = new Label("🌡️");
        wIcon.setStyle("-fx-font-size:26px;");
        Label wCond   = new Label("Chargement météo…");
        wCond.setStyle("-fx-font-size:13px; -fx-font-weight:800; -fx-text-fill:#0f2942;");
        Label wImpact = new Label("");
        wImpact.setStyle("-fx-font-size:11px; -fx-text-fill:#637a97; -fx-font-style:italic;");
        wImpact.setWrapText(true);
        VBox wInfo = new VBox(2, wCond, wImpact);
        wInfo.setAlignment(Pos.CENTER_LEFT);
        HBox weatherWidget = new HBox(10, wIcon, wInfo);
        weatherWidget.setAlignment(Pos.CENTER_LEFT);
        weatherWidget.setPadding(new Insets(10, 14, 10, 14));
        weatherWidget.setStyle("-fx-background-color:#f0f9ff; -fx-background-radius:12; -fx-border-color:#bae6fd; -fx-border-radius:12;");

        // Charger météo en arrière-plan
        Thread weatherThread = new Thread(() -> {
            LocalDate eventDate = ev.getDateEvent() != null
                    ? ev.getDateEvent().toLocalDate() : LocalDate.now();
            String cond    = weatherService.getWeatherCondition("Tunis", eventDate);
            int    tempC   = weatherService.getTemperature("Tunis", eventDate);
            String emoji   = WeatherService.toEmoji(cond);
            String display = cond.equals("Unknown") ? "Météo indisponible"
                           : emoji + "  " + cond + "  " + tempC + "°C";
            String impact  = buildWeatherImpact(cond, cat);
            String bg = cond.equals("Clear") ? "#fffbeb"
                    : (cond.equals("Rain") || cond.equals("Drizzle") || cond.equals("Thunderstorm")) ? "#eff6ff"
                    : cond.equals("Snow") ? "#f0f9ff" : "#f9fafb";
            String border = cond.equals("Clear") ? "#fde68a"
                    : (cond.equals("Rain") || cond.equals("Drizzle") || cond.equals("Thunderstorm")) ? "#bfdbfe"
                    : cond.equals("Snow") ? "#bae6fd" : "#e5e7eb";
            Platform.runLater(() -> {
                metaWeatherIcon.setText(emoji);
                metaWeatherText.setText(cond.equals("Unknown") ? "N/A" : cond + " " + tempC + "°C");
                wIcon.setText(emoji);
                wCond.setText(display);
                wImpact.setText(impact);
                weatherWidget.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:12;"
                        + "-fx-border-color:" + border + "; -fx-border-radius:12;");
            });
        }, "weather-detail");
        weatherThread.setDaemon(true);
        weatherThread.start();

        // ── TOP META BAR ──
        HBox metaBar = new HBox(0,
                metaBarItem("🏛", cat),
                metaBarSep(),
                metaBarItem("⏱", dureeStr),
                metaBarSep(),
                metaBarItem("📅", dateStr),
                metaBarSep(),
                metaBarItem("🕐", heureStr),
                metaBarSep(),
                metaWeatherItem
        );
        metaBar.setAlignment(Pos.CENTER);
        metaBar.setPadding(new Insets(18, 32, 18, 32));
        metaBar.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-width: 0 0 1 0;");

        // ── MAIN CONTENT CARD (image + info side by side) ──
        // Left: event info panel
        Label titleLbl = new Label(ev.getTitre());
        titleLbl.setStyle("-fx-font-size:22px; -fx-font-weight:900; -fx-text-fill:#1a1a1a; -fx-font-family:'Georgia';");
        titleLbl.setWrapText(true);

        Label subLbl = new Label(ev.getDescription() != null && ev.getDescription().length() > 80
                ? ev.getDescription().substring(0, 80) + "…" : (ev.getDescription() != null ? ev.getDescription() : ""));
        subLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#888; -fx-font-style:italic;");
        subLbl.setWrapText(true);

        // Info rows inside card
        Label lieuRow = new Label("📍  " + ev.getLieu());
        lieuRow.setStyle("-fx-font-size:13px; -fx-text-fill:#444; -fx-font-weight:600;");
        Label dateRow = new Label("📅  " + dateStr);
        dateRow.setStyle("-fx-font-size:13px; -fx-text-fill:#444;");
        Label heureRow = new Label("🕐  " + heureStr);
        heureRow.setStyle("-fx-font-size:13px; -fx-text-fill:#444;");

        // Capacity row
        String capText = full ? "🔴  Complet" : "✅  " + remaining + " place(s) disponible(s)";
        Label capRow = new Label(capText);
        capRow.setStyle("-fx-font-size:13px; -fx-font-weight:700; -fx-text-fill:" + (full ? "#c63d48" : "#1a7a4a") + ";");

        // Separator line
        javafx.scene.control.Separator sep1 = new javafx.scene.control.Separator();
        sep1.setStyle("-fx-background-color:#e5e7eb;");

        // Program-style section (description as "program")
        Label programLabel = new Label("PROGRAMME");
        programLabel.setStyle("-fx-font-size:11px; -fx-font-weight:900; -fx-text-fill:#8b0000; -fx-letter-spacing:2;");

        Label descContent = new Label(ev.getDescription() != null ? ev.getDescription() : "");
        descContent.setWrapText(true);
        descContent.setStyle("-fx-font-size:12px; -fx-text-fill:#333; -fx-line-spacing:3;");
        descContent.setMaxWidth(310);

        VBox infoPanel = new VBox(10,
                titleLbl, subLbl,
                new javafx.scene.control.Separator(),
                lieuRow, dateRow, heureRow, capRow,
                weatherWidget,
                sep1,
                programLabel, descContent
        );
        infoPanel.setPadding(new Insets(20, 20, 20, 20));
        infoPanel.setPrefWidth(340);
        infoPanel.setStyle("-fx-background-color: #faf9f7;");

        // Right: image
        StackPane imgPane = createImagePane(ev.getImage(), 380, 340, ev.getTitre());
        imgPane.setMinSize(380, 340);
        imgPane.setPrefSize(380, 340);
        imgPane.setStyle(imgPane.getStyle().replace("-fx-background-radius:22", "-fx-background-radius:0"));

        HBox contentCard = new HBox(0, infoPanel, imgPane);
        contentCard.setStyle("-fx-background-color: #faf9f7; -fx-border-color: #d4c9b0; -fx-border-width: 1;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 14, 0, 0, 4);");
        contentCard.setMaxWidth(720);

        // ── DESCRIPTION FULL ──
        Label descFullTitle = new Label(ev.getDescription() != null && ev.getDescription().length() > 0
                ? "" : "");
        Label descFull = new Label(ev.getDescription() != null ? ev.getDescription() : "");
        descFull.setWrapText(true);
        descFull.setStyle("-fx-font-size:13px; -fx-text-fill:#333; -fx-line-spacing:5;");
        descFull.setMaxWidth(680);

        VBox descSection = new VBox(8, descFull);
        descSection.setPadding(new Insets(0, 20, 0, 20));

        // ── LIKES & ACTIONS BAR ──
        int[] likeRef = {likeCount};
        Label likeCountLbl = new Label(likeCount + " ❤️");
        likeCountLbl.setStyle("-fx-font-size:13px; -fx-text-fill:#888; -fx-font-weight:600;");

        Button likeBtn = new Button(alreadyLiked ? "❤️  Retirer" : "🤍  J'aime");
        likeBtn.setStyle(alreadyLiked
                ? "-fx-background-color:#fce4ec; -fx-text-fill:#c62828; -fx-font-weight:700; -fx-background-radius:10; -fx-border-radius:10; -fx-border-color:#f48fb1; -fx-padding:8 16; -fx-cursor:hand;"
                : "-fx-background-color:#f3f4f6; -fx-text-fill:#555; -fx-font-weight:700; -fx-background-radius:10; -fx-border-radius:10; -fx-border-color:#d1d5db; -fx-padding:8 16; -fx-cursor:hand;");
        likeBtn.setOnAction(e -> {
            try {
                boolean nowLiked = engagementService.toggleLike(ev.getId(), currentUser.getId());
                likeRef[0] += nowLiked ? 1 : -1;
                likeCountLbl.setText(likeRef[0] + " ❤️");
                likeBtn.setText(nowLiked ? "❤️  Retirer" : "🤍  J'aime");
                likeBtn.setStyle(nowLiked
                        ? "-fx-background-color:#fce4ec; -fx-text-fill:#c62828; -fx-font-weight:700; -fx-background-radius:10; -fx-border-radius:10; -fx-border-color:#f48fb1; -fx-padding:8 16; -fx-cursor:hand;"
                        : "-fx-background-color:#f3f4f6; -fx-text-fill:#555; -fx-font-weight:700; -fx-background-radius:10; -fx-border-radius:10; -fx-border-color:#d1d5db; -fx-padding:8 16; -fx-cursor:hand;");
                reload.run();
            } catch (SQLException ex) { showError(ex.getMessage()); }
        });

        Button reserveBtn = new Button(alreadyReserved ? "✓ Déjà réservé" : (full ? "🔴 Complet" : "🎟  Réserver"));
        reserveBtn.setStyle(alreadyReserved || full
                ? "-fx-background-color:#e5e7eb; -fx-text-fill:#9ca3af; -fx-font-weight:700; -fx-background-radius:10; -fx-padding:9 22; -fx-font-size:13px;"
                : "-fx-background-color: linear-gradient(to right,#1d4ed8,#2563eb); -fx-text-fill:white; -fx-font-weight:800; -fx-background-radius:10; -fx-padding:9 22; -fx-font-size:13px; -fx-cursor:hand;"
                  + "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.35), 8, 0, 0, 3);");
        reserveBtn.setDisable(alreadyReserved || full);
        reserveBtn.setOnAction(e -> { showReservationDialog(ev, reload); detailStage.close(); });

        Button writeReviewBtn = new Button("✏️  Avis");
        writeReviewBtn.setStyle("-fx-background-color:#f0fdf4; -fx-text-fill:#15803d; -fx-font-weight:700; -fx-background-radius:10; -fx-border-radius:10; -fx-border-color:#bbf7d0; -fx-padding:8 16; -fx-cursor:hand;");
        writeReviewBtn.setOnAction(e -> {
            showReviewDialog(ev);
            detailStage.close();
            showEventDetails(ev, reload);
        });

        Button closeBtn = new Button("✕  Fermer");
        closeBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#9ca3af; -fx-font-weight:600; -fx-background-radius:10; -fx-padding:8 16; -fx-cursor:hand;");
        closeBtn.setOnAction(e -> detailStage.close());

        Region spacerAct = new Region();
        HBox.setHgrow(spacerAct, Priority.ALWAYS);
        HBox actionsBar = new HBox(10, likeBtn, likeCountLbl, spacerAct, writeReviewBtn, reserveBtn, closeBtn);
        actionsBar.setAlignment(Pos.CENTER_LEFT);
        actionsBar.setPadding(new Insets(14, 20, 14, 20));
        actionsBar.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-width: 1 0 0 0;");

        // ── REVIEWS SECTION ──
        String avgText = reviews.isEmpty() ? "Aucun avis"
                : String.format("⭐ %.1f / 5  ·  %d avis", avgRating, reviews.size());
        Label avgLbl = new Label(avgText);
        avgLbl.setStyle("-fx-font-size:13px; -fx-text-fill:#555; -fx-font-weight:700;");

        VBox reviewList = new VBox(8);
        if (reviews.isEmpty()) {
            Label noR = new Label("Soyez le premier à laisser un avis !");
            noR.setStyle("-fx-text-fill:#9ab0cc; -fx-font-size:12px; -fx-font-style:italic;");
            reviewList.getChildren().add(noR);
        } else {
            for (EventReview r : reviews) {
                String stars = "⭐".repeat(r.getRating()) + "☆".repeat(5 - r.getRating());
                Label starLbl = new Label(stars);
                starLbl.setStyle("-fx-font-size:13px;");
                Label userLbl = new Label("👤 " + (r.getUsername() != null ? r.getUsername() : "Anonyme"));
                userLbl.setStyle("-fx-text-fill:#2563eb; -fx-font-size:12px; -fx-font-weight:700;");
                Label dateLbl2 = new Label(r.getCreatedAt() != null
                        ? r.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
                dateLbl2.setStyle("-fx-text-fill:#b0bec5; -fx-font-size:11px;");
                Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
                HBox rHeader = new HBox(8, userLbl, sp, dateLbl2, starLbl);
                rHeader.setAlignment(Pos.CENTER_LEFT);
                Label commentLbl = new Label(r.getComment());
                commentLbl.setWrapText(true);
                commentLbl.setStyle("-fx-text-fill:#555; -fx-font-size:12px;");
                VBox rCard = new VBox(5, rHeader, commentLbl);
                rCard.setPadding(new Insets(10, 14, 10, 14));
                rCard.setStyle("-fx-background-color:white; -fx-background-radius:10; -fx-border-color:#e5e7eb; -fx-border-radius:10;");
                reviewList.getChildren().add(rCard);
            }
        }

        Label reviewsTitle = new Label("⭐  Avis des étudiants");
        reviewsTitle.setStyle("-fx-font-size:14px; -fx-font-weight:800; -fx-text-fill:#1a1a2e;");
        VBox reviewsSection = new VBox(10, reviewsTitle, avgLbl, reviewList);
        reviewsSection.setPadding(new Insets(16, 20, 16, 20));
        reviewsSection.setStyle("-fx-background-color:#f8fbff; -fx-background-radius:14; -fx-border-color:#d7e7ff; -fx-border-radius:14;");

        // ── ASSEMBLE PAGE ──
        VBox page = new VBox(0,
                metaBar,
                contentCard,
                new javafx.scene.layout.Region() {{ setMinHeight(16); }},
                descSection,
                new javafx.scene.layout.Region() {{ setMinHeight(12); }},
                reviewsSection,
                actionsBar
        );
        page.setStyle("-fx-background-color: white;");
        page.setPadding(new Insets(0, 20, 0, 20));

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: white; -fx-background: white;");

        Scene scene = new Scene(scroll, 760, 820);
        detailStage.setScene(scene);
        detailStage.setResizable(true);
        detailStage.show();
    }

    // ── Map page (student) ────────────────────────────────────────────────────
    private void showMapPageStudent() {
        setActiveNav(btnMap);

        Label pageTitle = new Label("Événements près de vous");
        pageTitle.setStyle("-fx-text-fill:#0f2942; -fx-font-size:22px; -fx-font-weight:800;");

        Label subtitle = new Label("La carte détecte votre position et affiche les événements triés par distance");
        subtitle.setStyle("-fx-text-fill:#637a97; -fx-font-size:12px;");

        StudentMapView mapView = new StudentMapView();
        mapView.setOnEventClick(eventId -> {
            try {
                Event ev = eventService.getEventById(eventId);
                if (ev != null) showEventDetails(ev, this::showMapPageStudent);
            } catch (SQLException ex) { showError(ex.getMessage()); }
        });
        VBox.setVgrow(mapView, Priority.ALWAYS);

        VBox page = new VBox(10, pageTitle, subtitle, mapView);
        page.setPadding(new Insets(16, 16, 0, 16));
        VBox.setVgrow(mapView, Priority.ALWAYS);
        VBox.setVgrow(page, Priority.ALWAYS);

        contentArea.getChildren().setAll(page);

        // Charger les événements après affichage de la page
        new Thread(() -> {
            try {
                List<Event> evs = eventService.getAllEvents();
                Map<Integer, Integer> rc = reservationService.getReservationCountsByEvent();
                Platform.runLater(() -> mapView.loadEvents(evs, rc));
            } catch (SQLException ex) {
                System.err.println("[MapPage] " + ex.getMessage());
            }
        }, "map-load").start();
    }

    private void openMapWindow(Runnable reload) {
        showMapPageStudent();
    }

    /** HTML complet de la carte inline avec Leaflet. */
    private String buildInlineMapHtml(String js, String css) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>" +
            css +
            "html,body{margin:0;padding:0;width:100%;height:480px;overflow:hidden;}" +
            "#map{width:100%;height:480px;}" +
            "#banner{position:absolute;top:10px;left:50%;transform:translateX(-50%);" +
            "z-index:9999;background:rgba(255,255,255,.95);border-radius:999px;" +
            "padding:6px 16px;font-size:12px;font-weight:700;color:#0f2942;" +
            "box-shadow:0 2px 10px rgba(0,0,0,.15);white-space:nowrap;pointer-events:none;}" +
            "#legend{position:absolute;top:10px;right:10px;z-index:9999;" +
            "background:rgba(255,255,255,.95);border-radius:10px;padding:8px 12px;" +
            "box-shadow:0 2px 10px rgba(0,0,0,.12);font-size:11px;pointer-events:none;}" +
            ".lr{display:flex;align-items:center;gap:5px;margin:2px 0;color:#415a78;}" +
            ".ld{width:9px;height:9px;border-radius:50%;flex-shrink:0;}" +
            "#nearby{position:absolute;bottom:10px;left:10px;z-index:9999;" +
            "background:rgba(255,255,255,.95);border-radius:10px;padding:8px 12px;" +
            "box-shadow:0 2px 10px rgba(0,0,0,.12);max-width:200px;max-height:150px;" +
            "overflow-y:auto;display:none;}" +
            "#nearby h4{font-size:11px;font-weight:800;color:#0f2942;margin:0 0 4px 0;}" +
            ".ni{padding:3px 0;border-bottom:1px solid #f0f4ff;cursor:pointer;font-size:11px;}" +
            ".ni:last-child{border-bottom:none;}" +
            ".nn{color:#0f2942;font-weight:700;}.nd{color:#9ab0cc;font-size:10px;}" +
            ".leaflet-popup-content-wrapper{border-radius:12px!important;}" +
            ".pb{padding:10px 12px;min-width:180px;}" +
            ".pt{font-size:13px;font-weight:800;color:#0f2942;margin-bottom:4px;}" +
            ".pr{font-size:11px;color:#415a78;margin:2px 0;}" +
            ".pd{font-size:11px;font-weight:700;color:#2563eb;margin:3px 0;}" +
            ".pbtn{display:block;width:100%;margin-top:6px;padding:6px;" +
            "background:linear-gradient(to right,#2563eb,#3b82f6);" +
            "color:white;border:none;border-radius:7px;font-size:11px;font-weight:700;cursor:pointer;}" +
            "</style></head><body>" +
            "<div id='map'></div>" +
            "<div id='banner'>Chargement de la carte...</div>" +
            "<div id='legend'>" +
            "<div class='lr'><div class='ld' style='background:#3b82f6'></div>Vous</div>" +
            "<div class='lr'><div class='ld' style='background:#22c55e'></div>&lt;2 km</div>" +
            "<div class='lr'><div class='ld' style='background:#f59e0b'></div>2-5 km</div>" +
            "<div class='lr'><div class='ld' style='background:#ef4444'></div>&gt;5 km</div>" +
            "</div>" +
            "<div id='nearby'><h4>Proches</h4><div id='nl'></div></div>" +
            "<script>" + js + "\n" +
            "if(typeof map!=='undefined'&&map){map.remove();}" +
            "var map=L.map('map').setView([36.8065,10.1815],13);" +
            "L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png'," +
            "{attribution:'(c) OpenStreetMap (c) CARTO',subdomains:'abcd',maxZoom:20}).addTo(map);" +
            "setTimeout(function(){map.invalidateSize();},100);" +
            "setTimeout(function(){map.invalidateSize();},300);" +
            "setTimeout(function(){map.invalidateSize();},700);" +
            "setTimeout(function(){map.invalidateSize();},1500);" +
            "var uLat=null,uLng=null,allM=[],allD=[],uMk=null;" +
            "function mkI(c){var s='<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"26\" height=\"34\" viewBox=\"0 0 32 42\">'+" +
            "'<path d=\"M16 0C7.163 0 0 7.163 0 16c0 10 16 26 16 26S32 26 32 16C32 7.163 24.837 0 16 0z\" fill=\"'+c+'\" stroke=\"white\" stroke-width=\"2.5\"/>'+" +
            "'<circle cx=\"16\" cy=\"16\" r=\"7\" fill=\"white\" opacity=\"0.9\"/></svg>';" +
            "return L.divIcon({html:s,iconSize:[26,34],iconAnchor:[13,34],popupAnchor:[0,-36],className:''});}" +
            "function uI(){var s='<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"32\" height=\"32\" viewBox=\"0 0 36 36\">'+" +
            "'<circle cx=\"18\" cy=\"18\" r=\"16\" fill=\"#3b82f6\" stroke=\"white\" stroke-width=\"3\"/>'+" +
            "'<circle cx=\"18\" cy=\"18\" r=\"7\" fill=\"white\"/></svg>';" +
            "return L.divIcon({html:s,iconSize:[32,32],iconAnchor:[16,16],className:''});}" +
            "function hav(a,b,c,d){var R=6371,dL=(c-a)*Math.PI/180,dG=(d-b)*Math.PI/180;" +
            "var x=Math.sin(dL/2)*Math.sin(dL/2)+Math.cos(a*Math.PI/180)*Math.cos(c*Math.PI/180)*Math.sin(dG/2)*Math.sin(dG/2);" +
            "return R*2*Math.atan2(Math.sqrt(x),Math.sqrt(1-x));}" +
            "function dc(k){return k<2?'#22c55e':k<5?'#f59e0b':'#ef4444';}" +
            "function addM(ev,c){var d=uLat!==null?hav(uLat,uLng,ev.lat,ev.lng).toFixed(1)+' km':'';" +
            "var dh=d?'<div class=\"pd\">'+d+' de vous</div>':'';" +
            "var p='<div class=\"pb\">'+'<div class=\"pt\">'+ev.titre+'</div>'+" +
            "'<div class=\"pr\">'+ev.lieu+'</div>'+'<div class=\"pr\">'+ev.date+'</div>'+" +
            "'<div class=\"pr\">'+(ev.remaining>0?ev.remaining+' place(s)':'Complet')+'</div>'+dh+'</div>';" +
            "allM.push(L.marker([ev.lat,ev.lng],{icon:mkI(c)}).bindPopup(p,{maxWidth:220}).addTo(map));}" +
            "function loadMarkers(data){allM.forEach(function(m){map.removeLayer(m);});allM=[];allD=data;" +
            "if(!data||!data.length){document.getElementById('banner').textContent='Aucun evenement';return;}" +
            "document.getElementById('banner').textContent=data.length+' evenement(s)';" +
            "data.forEach(function(ev){addM(ev,uLat!==null?dc(hav(uLat,uLng,ev.lat,ev.lng)):ev.color);});" +
            "if(uLat===null&&allM.length>0){var b=L.featureGroup(allM).getBounds();" +
            "if(b.isValid())map.fitBounds(b.pad(0.1),{maxZoom:14});}" +
            "setTimeout(function(){map.invalidateSize();},300);}" +
            "function locate(){if(!navigator.geolocation){document.getElementById('banner').textContent='GPS non disponible';return;}" +
            "navigator.geolocation.getCurrentPosition(function(pos){" +
            "uLat=pos.coords.latitude;uLng=pos.coords.longitude;" +
            "if(uMk)map.removeLayer(uMk);" +
            "uMk=L.marker([uLat,uLng],{icon:uI(),zIndexOffset:1000}).addTo(map);" +
            "L.circle([uLat,uLng],{radius:2000,color:'#3b82f6',fillColor:'#3b82f6',fillOpacity:0.07,weight:1}).addTo(map);" +
            "document.getElementById('banner').textContent='Position detectee';" +
            "allM.forEach(function(m){map.removeLayer(m);});allM=[];" +
            "var s=allD.slice().sort(function(a,b){return hav(uLat,uLng,a.lat,a.lng)-hav(uLat,uLng,b.lat,b.lng);});" +
            "s.forEach(function(ev){addM(ev,dc(hav(uLat,uLng,ev.lat,ev.lng)));});" +
            "var nb=s.filter(function(ev){return hav(uLat,uLng,ev.lat,ev.lng)<=5;});" +
            "if(nb.length>0){var p=document.getElementById('nearby'),l=document.getElementById('nl');" +
            "p.style.display='block';l.innerHTML='';" +
            "nb.slice(0,5).forEach(function(ev){var k=hav(uLat,uLng,ev.lat,ev.lng).toFixed(1);" +
            "var i=document.createElement('div');i.className='ni';" +
            "i.innerHTML='<div class=\"nn\">'+ev.titre+'</div><div class=\"nd\">'+k+' km</div>';" +
            "l.appendChild(i);});}" +
            "map.setView([uLat,uLng],13);" +
            "},function(){document.getElementById('banner').textContent='Position non disponible';},{timeout:8000});}" +
            "locate();" +
            "</script></body></html>";
    }

    /** Génère le texte d'impact météo selon la condition et la catégorie. */
    private String buildWeatherImpact(String weather, String category) {
        if (weather == null || weather.equalsIgnoreCase("Unknown")) return "Données météo indisponibles";
        boolean indoor  = WeatherService.isIndoor(category);
        boolean outdoor = WeatherService.isOutdoor(category);
        return switch (weather) {
            case "Rain", "Drizzle"       -> indoor  ? "Pluie → favorable pour cet événement indoor ✔"
                                           : outdoor ? "Pluie → peut impacter la participation ⚠"
                                           : "Météo pluvieuse — prévoyez un abri";
            case "Thunderstorm"          -> "Orage prévu — impact négatif sur la participation ⚠";
            case "Clear"                 -> outdoor ? "Beau temps → parfait pour cet événement ✔"
                                                    : "Beau temps → bonne météo pour venir ✔";
            case "Clouds"                -> "Ciel nuageux — météo neutre";
            case "Snow"                  -> indoor  ? "Neige → restez au chaud, événement indoor ✔"
                                                    : "Neige — déplacements difficiles ⚠";
            case "Mist", "Fog", "Haze"   -> "Brouillard — prévoyez plus de temps de trajet ℹ";
            default                      -> "Météo : " + weather;
        };
    }

    /** Un item de la barre meta (icône + texte centré verticalement). */
    private VBox metaBarItem(String icon, String text) {
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:22px;");
        Label textLbl = new Label(text);
        textLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#555; -fx-font-weight:600;");
        VBox box = new VBox(4, iconLbl, textLbl);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(0, 32, 0, 32));
        return box;
    }

    /** Séparateur vertical fin entre les items de la barre meta. */
    private javafx.scene.shape.Line metaBarSep() {
        javafx.scene.shape.Line line = new javafx.scene.shape.Line(0, 0, 0, 40);
        line.setStyle("-fx-stroke: #d1d5db;");
        return line;
    }

    // ── Recommended section ───────────────────────────────────────────────────
    private VBox buildRecommendedSection(Runnable reload) {
        Label title = new Label("🎯 Recommandé pour vous");
        title.setStyle("-fx-text-fill:#1a1a2e; -fx-font-size:18px; -fx-font-weight:900;");
        Label subtitle = new Label("Basé sur vos préférences et votre comportement");
        subtitle.setStyle("-fx-text-fill:#6c757d; -fx-font-size:12px;");

        // Use TilePane for proper 3-column grid
        TilePane recGrid = new TilePane();
        recGrid.setHgap(16);
        recGrid.setVgap(16);
        recGrid.setPrefColumns(3);
        recGrid.setTileAlignment(Pos.TOP_LEFT);
        recGrid.setPrefTileWidth(300);

        try {
            List<RecommendationResult> recs = recommendationService.recommendForUser(currentUser.getId(), 3);
            if (recs.isEmpty()) {
                Label noRec = new Label("💡 Réservez ou aimez des événements pour obtenir des recommandations personnalisées.");
                noRec.setStyle("-fx-text-fill:#adb5bd; -fx-font-size:13px; -fx-font-style:italic;");
                noRec.setWrapText(true);
                recGrid.getChildren().add(noRec);
            } else {
                Map<Integer, Integer> resCounts = reservationService.getReservationCountsByEvent();
                Set<Integer> reserved = reservationService.getReservedEventIdsByUser(currentUser.getId());
                Set<Integer> liked    = engagementService.getLikedEventIdsByUser(currentUser.getId());
                Map<Integer, Integer> likeCounts = engagementService.getLikeCountsByEvent();

                for (RecommendationResult rec : recs) {
                    Event ev = rec.getEvent();
                    int resCount  = resCounts.getOrDefault(ev.getId(), 0);
                    int remaining = ev.getCapacite() - resCount;
                    boolean isReserved = reserved.contains(ev.getId());
                    boolean isLiked    = liked.contains(ev.getId());
                    int likeCount      = likeCounts.getOrDefault(ev.getId(), 0);

                    VBox miniCard = buildMiniEventCard(ev, remaining, isReserved, isLiked,
                            likeCount, rec.getScorePercent(), reload);
                    recGrid.getChildren().add(miniCard);
                }
            }
        } catch (SQLException ex) {
            Label error = new Label("Erreur chargement recommandations.");
            error.setStyle("-fx-text-fill:#c63d48; -fx-font-size:12px;");
            recGrid.getChildren().add(error);
        }

        VBox section = new VBox(10, title, subtitle, recGrid);
        section.setPadding(new Insets(18, 20, 18, 20));
        section.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 18;"
                + "-fx-border-color: #bbf7d0; -fx-border-radius: 18;"
                + "-fx-effect: dropshadow(gaussian, rgba(34,197,94,0.12), 10, 0, 0, 3);");
        return section;
    }

    private VBox buildMiniEventCard(Event ev, int remaining, boolean isReserved,
                                    boolean isLiked, int likeCount, String matchPercent, Runnable reload) {
        StackPane img = createImagePane(ev.getImage(), 300, 170, ev.getTitre());
        img.setCursor(Cursor.HAND);
        img.setStyle(img.getStyle() + " -fx-background-radius: 14 14 0 0;");
        img.setOnMouseClicked(e -> showEventDetails(ev, reload));

        // Match badge
        Label matchBadge = new Label("✨ " + matchPercent + " match");
        matchBadge.setStyle("-fx-background-color:#DCFCE7; -fx-text-fill:#15803D;"
                + "-fx-font-size:11px; -fx-font-weight:800; -fx-background-radius:6; -fx-padding:3 10;");

        Label titleLbl = new Label(ev.getTitre());
        titleLbl.setStyle("-fx-font-size:14px; -fx-font-weight:800; -fx-text-fill:#111827;");
        titleLbl.setWrapText(true);

        Label catLbl = new Label(ev.getCategorie() != null ? "🏷 " + ev.getCategorie() : "");
        catLbl.setStyle("-fx-text-fill:#6B7280; -fx-font-size:12px;");

        Label dateLbl = new Label(ev.getDateEvent() != null
                ? "📅 " + ev.getDateEvent().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        dateLbl.setStyle("-fx-text-fill:#6B7280; -fx-font-size:12px;");

        Label lieuLbl = new Label("📍 " + ev.getLieu());
        lieuLbl.setStyle("-fx-text-fill:#6B7280; -fx-font-size:12px;");

        String spotsText = remaining > 0 ? "✅ " + remaining + " place(s)" : "🔴 Complet";
        Label spotsLbl = new Label(spotsText);
        spotsLbl.setStyle("-fx-text-fill:" + (remaining > 0 ? "#15803D" : "#DC2626")
                + "; -fx-font-size:11px; -fx-font-weight:700;");

        Button reserveBtn = new Button(isReserved ? "✓ Réservé" : "🎟 Réserver");
        reserveBtn.setStyle(isReserved
                ? "-fx-background-color:#F3F4F6; -fx-text-fill:#6B7280; -fx-font-weight:700;"
                  + "-fx-background-radius:10; -fx-padding:8 14; -fx-font-size:12px;"
                : "-fx-background-color: linear-gradient(to right,#2563EB,#3B82F6);"
                  + "-fx-text-fill:white; -fx-font-weight:800; -fx-font-size:12px;"
                  + "-fx-background-radius:10; -fx-padding:9 16; -fx-cursor:hand;");
        reserveBtn.setDisable(isReserved || remaining <= 0);
        reserveBtn.setMaxWidth(Double.MAX_VALUE);
        reserveBtn.setOnAction(e -> showReservationDialog(ev, reload));

        VBox body = new VBox(6, matchBadge, titleLbl, catLbl, dateLbl, lieuLbl, spotsLbl, reserveBtn);
        body.setPadding(new Insets(12, 14, 14, 14));

        VBox card = new VBox(img, body);
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16;"
                + "-fx-border-color: #D1FAE5; -fx-border-radius: 16; -fx-border-width: 1.5;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);");

        // Hover
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 16;"
                + "-fx-border-color: #6EE7B7; -fx-border-radius: 16; -fx-border-width: 2;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.14), 16, 0, 0, 5);"
                + "-fx-scale-x: 1.01; -fx-scale-y: 1.01;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 16;"
                + "-fx-border-color: #D1FAE5; -fx-border-radius: 16; -fx-border-width: 1.5;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);"));
        return card;
    }

    // ── Date strip (horizontal date bar) ─────────────────────────────────────
    private HBox buildDateStrip() {
        HBox strip = new HBox(8);
        strip.setAlignment(Pos.CENTER_LEFT);
        strip.setPadding(new Insets(4, 0, 4, 0));

        LocalDate today = LocalDate.now();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE", java.util.Locale.FRENCH);

        // Build availability for next 9 days
        Map<LocalDate, Integer> availMap   = new java.util.HashMap<>();
        Map<LocalDate, Integer> optionsMap = new java.util.HashMap<>();
        try {
            List<Event> allEvents = eventService.getAllEvents();
            for (int i = 0; i < 9; i++) {
                LocalDate d = today.plusDays(i);
                int totalOptions = 0;
                boolean hasEvent = false;
                boolean allFull  = true;
                for (Event ev : allEvents) {
                    if (!ev.getDateEvent().toLocalDate().equals(d)) continue;
                    hasEvent = true;
                    int confirmed = reservationService.getConfirmedCountByEvent(ev.getId());
                    int remaining = ev.getCapacite() - confirmed;
                    if (remaining > 0) { totalOptions += remaining; allFull = false; }
                }
                int code = hasEvent ? (allFull ? 2 : (totalOptions < 3 ? 0 : 1)) : 1;
                availMap.put(d, code);
                optionsMap.put(d, totalOptions);
            }
        } catch (SQLException ignored) {}

        // Day cards
        for (int i = 0; i < 9; i++) {
            LocalDate d       = today.plusDays(i);
            int       code    = availMap.getOrDefault(d, 1);
            int       options = optionsMap.getOrDefault(d, 0);
            boolean   isToday = d.equals(today);

            String dayName  = isToday ? "AUJOURD'HUI"
                    : d.format(dayFmt).toUpperCase(java.util.Locale.FRENCH) + ".";
            String barColor = code == 2 ? "#9e9e9e" : (code == 0 ? "#f59e0b" : "#22c55e");

            // Day name label
            Label dayLbl = new Label(dayName);
            dayLbl.setStyle("-fx-text-fill: #8a9bb5; -fx-font-size: 12px; -fx-font-weight: 700;");

            // Day number label — large
            Label numLbl = new Label(String.valueOf(d.getDayOfMonth()));
            numLbl.setStyle("-fx-text-fill: #0f2942; -fx-font-size: 30px; -fx-font-weight: 900;");

            // Options label
            Label optLbl = new Label(options + " options");
            optLbl.setStyle("-fx-text-fill: #8a9bb5; -fx-font-size: 12px;");

            // Colored bar
            javafx.scene.layout.Region bar = new javafx.scene.layout.Region();
            bar.setPrefHeight(3);
            bar.setPrefWidth(60);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.setStyle("-fx-background-color: " + barColor + "; -fx-background-radius: 99;");

            VBox card = new VBox(2);
            card.setAlignment(Pos.CENTER);
            card.setPadding(new Insets(12, 14, 10, 14));
            card.setPrefWidth(110);
            card.setMinWidth(110);
            card.setMaxWidth(110);
            card.getChildren().addAll(dayLbl, numLbl, optLbl, bar);

            String baseStyle = "-fx-background-color: white;"
                    + "-fx-background-radius: 12;"
                    + "-fx-border-radius: 12;"
                    + "-fx-border-color: " + (isToday ? "#b8d0ff" : "#dde8f5") + ";"
                    + "-fx-border-width: " + (isToday ? "2" : "1") + ";"
                    + "-fx-effect: dropshadow(gaussian,rgba(46,94,166,0.07),6,0,0,2);"
                    + "-fx-cursor: hand;";
            String hoverStyle = "-fx-background-color: #f5f9ff;"
                    + "-fx-background-radius: 12;"
                    + "-fx-border-radius: 12;"
                    + "-fx-border-color: #b8d0ff;"
                    + "-fx-border-width: 2;"
                    + "-fx-effect: dropshadow(gaussian,rgba(46,94,166,0.12),8,0,0,2);"
                    + "-fx-cursor: hand;";

            card.setStyle(baseStyle);
            card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
            card.setOnMouseExited(e  -> card.setStyle(baseStyle));

            LocalDate finalD = d;
            card.setOnMouseClicked(e -> {
                showCalendarPage();
                if (currentCalendar != null) {
                    currentCalendar.setSelectedDate(finalD);
                    loadCalendarForDate(finalD);
                }
            });

            strip.getChildren().add(card);
        }

        // "Plus de dates" card
        Label calIcon = new Label("📅");
        calIcon.setStyle("-fx-font-size: 18px;");
        calIcon.setMaxWidth(Double.MAX_VALUE);
        calIcon.setAlignment(Pos.CENTER);

        Label moreLbl = new Label("Plus de\ndates");
        moreLbl.setStyle("-fx-text-fill: #0f69ff; -fx-font-size: 10px;"
                + "-fx-font-weight: 700; -fx-text-alignment: center;");
        moreLbl.setMaxWidth(Double.MAX_VALUE);
        moreLbl.setAlignment(Pos.CENTER);

        VBox moreCard = new VBox(4);
        moreCard.setAlignment(Pos.CENTER);
        moreCard.setPadding(new Insets(12, 14, 12, 14));
        moreCard.setPrefWidth(110);
        moreCard.setMinWidth(110);
        moreCard.setMaxWidth(110);
        moreCard.getChildren().addAll(calIcon, moreLbl);
        moreCard.setStyle("-fx-background-color: #f0f5ff;"
                + "-fx-background-radius: 12;"
                + "-fx-border-radius: 12;"
                + "-fx-border-color: #d7e7ff;"
                + "-fx-border-width: 1;"
                + "-fx-cursor: hand;");
        moreCard.setOnMouseEntered(e -> moreCard.setStyle("-fx-background-color: #e4eeff;"
                + "-fx-background-radius: 12; -fx-border-radius: 12;"
                + "-fx-border-color: #b8d0ff; -fx-border-width: 2; -fx-cursor: hand;"));
        moreCard.setOnMouseExited(e  -> moreCard.setStyle("-fx-background-color: #f0f5ff;"
                + "-fx-background-radius: 12; -fx-border-radius: 12;"
                + "-fx-border-color: #d7e7ff; -fx-border-width: 1; -fx-cursor: hand;"));
        moreCard.setOnMouseClicked(e -> showCalendarPage());

        strip.getChildren().add(moreCard);
        return strip;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
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
                // Charger en pleine résolution pour éviter le flou
                Image img = new Image(file.toURI().toString());
                ImageView iv = new ImageView(img);
                iv.setFitWidth(width);
                iv.setFitHeight(height);
                iv.setPreserveRatio(false);
                iv.setSmooth(true);
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

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }

    /**
     * Affiche le billet dans une fenêtre JavaFX.
     * Charge les détails de l'événement et affiche un joli ticket.
     */
    private void openTicketPage(String payload) {
        try {
            // Parse payload: reservationId|userId|eventId|expiration|HASH
            String[] parts = payload.split("\\|");
            if (parts.length != 5) {
                showError("Billet invalide");
                return;
            }
            
            int reservationId = Integer.parseInt(parts[0]);
            int userId = Integer.parseInt(parts[1]);
            int eventId = Integer.parseInt(parts[2]);
            
            // Load event details
            Event event = eventService.getEventById(eventId);
            if (event == null) {
                showError("Événement non trouvé");
                return;
            }
            
            // Create ticket window
            Stage ticketStage = new Stage();
            ticketStage.setTitle("🎟 Mon Billet");
            ticketStage.initOwner(stage);
            ticketStage.setResizable(false);
            
            // Get event details
            String eventTitle = event.getTitre();
            String eventDate = event.getDateEvent() != null 
                    ? event.getDateEvent().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH))
                    : "—";
            String eventHeure = event.getDateEvent() != null
                    ? event.getDateEvent().format(DateTimeFormatter.ofPattern("HH:mm"))
                    : "—";
            String eventLieu = event.getLieu() != null ? event.getLieu() : "—";
            String eventCat = event.getCategorie() != null ? event.getCategorie() : "—";
            
            // Get user name
            String username = currentUser.getUsername();
            
            // Build ticket visually
            // Header
            Label headerBrand = new Label("🎪 MindCare Events");
            headerBrand.setStyle("-fx-font-size:16px; -fx-font-weight:900; -fx-text-fill:white;");
            
            Label headerStatus = new Label("✅ TICKET VALIDÉ");
            headerStatus.setStyle("-fx-font-size:12px; -fx-font-weight:800; -fx-text-fill:white; "
                    + "-fx-background-color:#22c55e; -fx-background-radius:20; -fx-padding:4 12;");
            
            HBox header = new HBox(12, headerBrand, headerStatus);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setPadding(new Insets(16, 20, 16, 20));
            header.setStyle("-fx-background-color: linear-gradient(135deg, #2563EB, #1d4ed8); "
                    + "-fx-background-radius:20 20 0 0;");
            
            // Title
            Label title = new Label(eventTitle);
            title.setStyle("-fx-font-size:24px; -fx-font-weight:900; -fx-text-fill:#1a1a2e;");
            title.setWrapText(true);
            
            // Info rows
            VBox infoBox = new VBox(12);
            infoBox.setPadding(new Insets(16, 20, 16, 20));
            
            infoBox.getChildren().addAll(
                    buildTicketRow("📅 DATE", eventDate),
                    buildTicketRow("🕐 HEURE", eventHeure),
                    buildTicketRow("📍 LIEU", eventLieu),
                    buildTicketRow("🏷️  CATÉGORIE", eventCat),
                    new Separator(),
                    buildTicketRow("👤 TITULAIRE", username),
                    buildTicketRow("🎟  RÉSERVATION", "#" + reservationId)
            );
            
            // QR Code — généré localement via ZXing (sans internet)
            String ticketUrl = qrCodeService.generateTicketUrl(payload);
            byte[] qrBytes = qrCodeService.generateQRBytes(ticketUrl, 180);

            ImageView qrImage = new ImageView();
            qrImage.setFitWidth(180);
            qrImage.setFitHeight(180);
            qrImage.setPreserveRatio(true);

            if (qrBytes != null) {
                // Charger depuis les bytes locaux (instantané, sans réseau)
                qrImage.setImage(new Image(new java.io.ByteArrayInputStream(qrBytes)));
            } else {
                // Fallback réseau si ZXing échoue
                String qrImageUrl = qrCodeService.generateQRImageUrl(ticketUrl, 180);
                new Thread(() -> {
                    try {
                        Image img = new Image(qrImageUrl, true);
                        Platform.runLater(() -> qrImage.setImage(img));
                    } catch (Exception ex) {
                        System.err.println("[Ticket] QR fallback error: " + ex.getMessage());
                    }
                }, "qr-load").start();
            }
            
            VBox qrBox = new VBox(qrImage);
            qrBox.setAlignment(Pos.CENTER);
            qrBox.setPadding(new Insets(16, 20, 20, 20));
            qrBox.setStyle("-fx-background-color:#f9fafb;");
            
            // Tip
            Label tipIcon = new Label("💡");
            tipIcon.setStyle("-fx-font-size:16px;");
            Label tipText = new Label("Présentez ce billet à l'entrée de l'événement");
            tipText.setStyle("-fx-font-size:12px; -fx-text-fill:#065f46; -fx-font-weight:600;");
            HBox tip = new HBox(8, tipIcon, tipText);
            tip.setAlignment(Pos.CENTER_LEFT);
            tip.setPadding(new Insets(12, 16, 12, 16));
            tip.setStyle("-fx-background-color:#d1fae5; -fx-background-radius:10;");
            
            // Close button
            Button closeBtn = new Button("Fermer");
            closeBtn.setStyle("-fx-padding:10 20; -fx-font-size:13px; -fx-font-weight:700; "
                    + "-fx-background-radius:8; -fx-background-color:#e5e7eb; -fx-text-fill:#374151;");
            closeBtn.setOnAction(e -> ticketStage.close());
            
            HBox footer = new HBox(closeBtn);
            footer.setAlignment(Pos.CENTER);
            footer.setPadding(new Insets(16));
            
            // Main layout
            VBox root = new VBox(
                    header, title, infoBox, qrBox, tip, footer
            );
            root.setStyle("-fx-background-color: white;");
            
            Scene scene = new Scene(root, 420, 750);
            ticketStage.setScene(scene);
            ticketStage.show();
            
        } catch (Exception ex) {
            showError("Erreur billet: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    private HBox buildTicketRow(String label, String value) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-size:11px; -fx-font-weight:800; -fx-text-fill:#9ca3af;");
        
        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-font-size:14px; -fx-font-weight:700; -fx-text-fill:#1a1a2e;");
        valueNode.setWrapText(true);
        
        VBox contentBox = new VBox(2, labelNode, valueNode);
        HBox row = new HBox(contentBox);
        HBox.setHgrow(contentBox, Priority.ALWAYS);
        return row;
    }
}
