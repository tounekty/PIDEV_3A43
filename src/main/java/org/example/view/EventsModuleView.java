package org.example.view;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.event.Event;
import org.example.event.EventEngagementService;
import org.example.event.EventReview;
import org.example.event.EventService;
import org.example.event.RecommendationResult;
import org.example.event.RecommendationService;
import org.example.event.ReminderService;
import org.example.model.User;
import org.example.reservation.ReservationRecord;
import org.example.reservation.ReservationService;
import org.example.reservation.ReservationStatus;
import org.example.util.DualMonthCalendarView;
import org.example.util.StudentMapView;
import org.example.util.ValidationUtil;
import org.example.util.WeatherService;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Full-featured events module matching the friend's aaa StudentApp/AdminApp design.
 * Student tabs: Événements (card grid + date strip + recommendations + map), Calendrier, Mes Réservations, Carte.
 * Admin tabs: Événements (card grid), Ajouter/Modifier, Réservations.
 */
public class EventsModuleView {

    // ── Styles ───────────────────────────────────────────────────────────────
    private static final String INPUT =
            "-fx-background-color:white; -fx-background-radius:10; -fx-border-radius:10;"
            + "-fx-border-color:#d1d5db; -fx-padding:9 12; -fx-font-size:13px;";
    private static final String PRIMARY =
            "-fx-background-color:linear-gradient(to right,#2563eb,#3b82f6); -fx-text-fill:white;"
            + "-fx-font-weight:700; -fx-background-radius:10; -fx-padding:9 18; -fx-cursor:hand;";
    private static final String SUCCESS =
            "-fx-background-color:linear-gradient(to right,#2e7d32,#43a047); -fx-text-fill:white;"
            + "-fx-font-weight:800; -fx-font-size:13px; -fx-background-radius:12; -fx-padding:10 20; -fx-cursor:hand;";
    private static final String DANGER =
            "-fx-background-color:#dc2626; -fx-text-fill:white; -fx-font-weight:700;"
            + "-fx-background-radius:10; -fx-padding:9 18; -fx-cursor:hand;";
    private static final String SECONDARY =
            "-fx-background-color:#f3f4f6; -fx-text-fill:#374151; -fx-font-weight:600;"
            + "-fx-background-radius:10; -fx-border-color:#d1d5db; -fx-border-radius:10;"
            + "-fx-padding:9 18; -fx-cursor:hand;";
    private static final String CARD =
            "-fx-background-color:white; -fx-background-radius:18; -fx-border-radius:18;"
            + "-fx-border-color:#e9ecef; -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,4);";
    private static final String CARD_HOVER =
            "-fx-background-color:white; -fx-background-radius:18; -fx-border-radius:18;"
            + "-fx-border-color:#adb5bd; -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),18,0,0,6);"
            + "-fx-scale-x:1.01; -fx-scale-y:1.01;";
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.FRENCH);
    private static final DateTimeFormatter SHORT_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Services ─────────────────────────────────────────────────────────────
    private final EventService eventService                 = new EventService();
    private final EventEngagementService engagementService  = new EventEngagementService();
    private final ReservationService reservationService     = new ReservationService();
    private final RecommendationService recommendationService = new RecommendationService();
    private final ReminderService reminderService           = new ReminderService();
    private final WeatherService weatherService             = new WeatherService();

    private final User    currentUser;
    private final boolean isAdmin;

    // Admin form refresh callback
    private Runnable refreshAdminGrid = () -> {};

    public EventsModuleView(User user) {
        this.currentUser = user;
        this.isAdmin = user != null && user.isAdmin();
        initSchema();
    }

    // ── Nav button styles ─────────────────────────────────────────────────────
    private static final String NAV_NORMAL =
            "-fx-background-color:transparent; -fx-text-fill:#374151;"
            + "-fx-font-weight:600; -fx-font-size:14px; -fx-cursor:hand; -fx-padding:8 14;"
            + "-fx-background-radius:10;";
    private static final String NAV_ACTIVE =
            "-fx-background-color:#EFF6FF; -fx-text-fill:#2563EB;"
            + "-fx-font-weight:800; -fx-font-size:14px; -fx-cursor:hand; -fx-padding:8 14;"
            + "-fx-background-radius:10; -fx-border-color:#BFDBFE; -fx-border-radius:10; -fx-border-width:1;";
    private static final String NAV_HOVER =
            "-fx-background-color:#EFF6FF; -fx-text-fill:#2563EB;"
            + "-fx-font-weight:700; -fx-font-size:14px; -fx-cursor:hand; -fx-padding:8 14;"
            + "-fx-background-radius:10;";

    // ── Entry point ───────────────────────────────────────────────────────────

    public Node build() {
        // Content area — pages swap here
        StackPane contentArea = new StackPane();
        contentArea.setPadding(new Insets(0));
        contentArea.setStyle("-fx-background-color:#F8F9FA;");
        contentArea.setMaxHeight(Double.MAX_VALUE);
        contentArea.setMaxWidth(Double.MAX_VALUE);

        // Build nav bar
        HBox navBar = buildNavBar(contentArea);

        BorderPane root = new BorderPane();
        root.setTop(navBar);
        root.setCenter(contentArea);
        root.setMaxHeight(Double.MAX_VALUE);
        root.setMaxWidth(Double.MAX_VALUE);
        root.setStyle("-fx-background-color:#F8F9FA;");

        // Show initial page
        if (isAdmin) {
            showAdminPage(contentArea, "events");
        } else {
            showStudentPage(contentArea, "events");
        }

        return root;
    }

    // ── Navigation bar ────────────────────────────────────────────────────────

    private Button[] navBtns;   // [0]=events [1]=cal [2]=reservations [3]=map (student)
    //                            [0]=events [1]=add   [2]=reservations          (admin)

    private HBox buildNavBar(StackPane contentArea) {
        if (isAdmin) {
            Button btnEvents = navBtn("📋 Événements");
            Button btnAdd    = navBtn("➕ Ajouter / Modifier");
            Button btnRes    = navBtn("📌 Réservations");
            navBtns = new Button[]{btnEvents, btnAdd, btnRes};

            btnEvents.setOnAction(e -> { setActiveNav(0); showAdminPage(contentArea, "events"); });
            btnAdd   .setOnAction(e -> { setActiveNav(1); showAdminPage(contentArea, "add"); });
            btnRes   .setOnAction(e -> { setActiveNav(2); showAdminPage(contentArea, "reservations"); });

            setActiveNav(0);

            Label title = new Label("🧠 MindCare — Administration");
            title.setStyle("-fx-text-fill:#2563EB; -fx-font-size:16px; -fx-font-weight:900;");
            Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox nav = new HBox(20, title, btnEvents, btnAdd, btnRes, spacer);
            nav.setAlignment(Pos.CENTER_LEFT);
            nav.setPadding(new Insets(14, 28, 14, 28));
            nav.setStyle("-fx-background-color:white; -fx-border-color:#E5E7EB;"
                    + "-fx-border-width:0 0 1 0;"
                    + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);");
            return nav;
        } else {
            Button btnEvents = navBtn("Événements");
            Button btnCal    = navBtn("📅 Calendrier");
            Button btnMyRes  = navBtn("Mes Réservations");
            Button btnMap    = navBtn("🗺️ Carte");
            navBtns = new Button[]{btnEvents, btnCal, btnMyRes, btnMap};

            btnEvents.setOnAction(e -> { setActiveNav(0); showStudentPage(contentArea, "events"); });
            btnCal   .setOnAction(e -> { setActiveNav(1); showStudentPage(contentArea, "calendar"); });
            btnMyRes .setOnAction(e -> { setActiveNav(2); showStudentPage(contentArea, "reservations"); });
            btnMap   .setOnAction(e -> { setActiveNav(3); showStudentPage(contentArea, "map"); });

            setActiveNav(0);

            Label badge = new Label("👤 " + (currentUser != null ? currentUser.getFullName() : ""));
            badge.setStyle("-fx-text-fill:#2563EB; -fx-background-color:#EFF6FF;"
                    + "-fx-background-radius:999; -fx-padding:7 16 7 16;"
                    + "-fx-font-weight:700; -fx-font-size:13px;");
            Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox nav = new HBox(20, btnEvents, btnCal, btnMyRes, btnMap, spacer, badge);
            nav.setAlignment(Pos.CENTER_LEFT);
            nav.setPadding(new Insets(14, 28, 14, 28));
            nav.setStyle("-fx-background-color:white; -fx-border-color:#E5E7EB;"
                    + "-fx-border-width:0 0 1 0;"
                    + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);");
            return nav;
        }
    }

    private Button navBtn(String text) {
        Button b = new Button(text);
        b.setStyle(NAV_NORMAL);
        b.setOnMouseEntered(e -> { if (!NAV_ACTIVE.equals(b.getStyle())) b.setStyle(NAV_HOVER); });
        b.setOnMouseExited(e  -> { if (!NAV_ACTIVE.equals(b.getStyle())) b.setStyle(NAV_NORMAL); });
        return b;
    }

    private void setActiveNav(int idx) {
        if (navBtns == null) return;
        for (int i = 0; i < navBtns.length; i++)
            navBtns[i].setStyle(i == idx ? NAV_ACTIVE : NAV_NORMAL);
    }

    // ── Page switching ────────────────────────────────────────────────────────

    private void showStudentPage(StackPane area, String page) {
        Node content = switch (page) {
            case "calendar"     -> buildCalendarTab();
            case "reservations" -> buildMyReservationsTab();
            case "map"          -> buildMapTab();
            default             -> buildStudentEventsTab();
        };
        area.getChildren().setAll(content);
        StackPane.setAlignment(content, Pos.TOP_LEFT);
    }

    private void showAdminPage(StackPane area, String page) {
        Node content = switch (page) {
            case "add"          -> buildAdminFormContent(null);
            case "reservations" -> buildAdminReservationsTab();
            default             -> buildAdminEventsTab();
        };
        area.getChildren().setAll(content);
        StackPane.setAlignment(content, Pos.TOP_LEFT);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STUDENT – Events tab
    // ══════════════════════════════════════════════════════════════════════════

    private Node buildStudentEventsTab() {
        // ── Title ──
        Label pageTitle = new Label("Événements disponibles");
        pageTitle.setStyle("-fx-text-fill:#1a1a2e; -fx-font-size:28px; -fx-font-weight:900;");
        Label pageSub = new Label("Découvrez et réservez les événements qui vous correspondent");
        pageSub.setStyle("-fx-text-fill:#6c757d; -fx-font-size:13px;");

        // ── Search bar ──
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size:16px; -fx-padding:0 4 0 8;");
        TextField search = new TextField();
        search.setPromptText("Rechercher un événement...");
        search.setStyle("-fx-background-color:transparent; -fx-border-color:transparent;"
                + "-fx-font-size:14px; -fx-text-fill:#222;");
        HBox.setHgrow(search, Priority.ALWAYS);
        HBox searchBox = new HBox(4, searchIcon, search);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setStyle("-fx-background-color:white; -fx-background-radius:14;"
                + "-fx-border-color:#dee2e6; -fx-border-radius:14;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),6,0,0,2); -fx-padding:4 8;");
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        // ── Filters ──
        ComboBox<String> filterCat = new ComboBox<>(FXCollections.observableArrayList(
                "🎯 Catégorie", "yoga", "wellness", "sport", "meditation", "conference", "atelier"));
        filterCat.setValue("🎯 Catégorie");
        filterCat.setStyle(INPUT + "-fx-pref-width:150px;");

        ComboBox<String> filterSort = new ComboBox<>(FXCollections.observableArrayList(
                "📅 Trier par", "Date", "Capacite", "Lieu"));
        filterSort.setValue("📅 Trier par");
        filterSort.setStyle(INPUT + "-fx-pref-width:140px;");

        Button clearBtn = new Button("✕ Effacer");
        clearBtn.setStyle(SECONDARY);

        HBox toolbar = new HBox(10, searchBox, filterCat, filterSort, clearBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(12, 16, 12, 16));
        toolbar.setStyle("-fx-background-color:white; -fx-background-radius:16;"
                + "-fx-border-color:#e9ecef; -fx-border-radius:16;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);");

        // ── Date availability strip ──
        VBox stripSection = buildDateStrip();

        // ── Recommended section ──
        VBox recommendedSection = buildRecommendedSection();

        // ── Inline map ──
        VBox mapSection = buildInlineMapSection();

        // ── Events grid ──
        Label eventsTitle = new Label("🔥  Tous les événements");
        eventsTitle.setStyle("-fx-text-fill:#1a1a2e; -fx-font-size:18px; -fx-font-weight:800;");

        TilePane grid = new TilePane();
        grid.setHgap(22); grid.setVgap(22);
        grid.setPrefColumns(3);
        grid.setTileAlignment(Pos.TOP_LEFT);
        grid.setPrefTileWidth(380);
        grid.setPadding(new Insets(4, 0, 12, 0));

        Runnable[] loadRef = {null};
        loadRef[0] = () -> {
            try {
                String q = search.getText();
                String cat = filterCat.getValue();
                String sort = filterSort.getValue();
                String sortBy = "📅 Trier par".equals(sort) ? null : sort;
                List<Event> events = eventService.getEvents(
                        q == null || q.isBlank() ? null : q, sortBy);
                if (cat != null && !cat.startsWith("🎯")) {
                    events = events.stream()
                            .filter(ev -> cat.equalsIgnoreCase(ev.getCategorie()))
                            .collect(Collectors.toList());
                }
                Map<Integer, Integer> resCounts = reservationService.getReservationCountsByEvent();
                Set<Integer> reserved = currentUser != null
                        ? reservationService.getReservedEventIdsByUser(currentUser.getId())
                        : Collections.emptySet();
                Set<Integer> liked = currentUser != null
                        ? engagementService.getLikedEventIdsByUser(currentUser.getId())
                        : Collections.emptySet();
                Map<Integer, Integer> likeCounts = engagementService.getLikeCountsByEvent();
                grid.getChildren().clear();
                for (Event ev : events) {
                    int rc = resCounts.getOrDefault(ev.getId(), 0);
                    int remaining = ev.getCapacite() - rc;
                    grid.getChildren().add(buildStudentEventCard(
                            ev, remaining, reserved.contains(ev.getId()),
                            liked.contains(ev.getId()),
                            likeCounts.getOrDefault(ev.getId(), 0),
                            loadRef[0]));
                }
                if (events.isEmpty()) grid.getChildren().add(emptyState("Aucun événement trouvé"));
            } catch (SQLException ex) { showError(ex.getMessage()); }
        };

        search.textProperty().addListener((o, a, b) -> loadRef[0].run());
        filterCat.setOnAction(e -> loadRef[0].run());
        filterSort.setOnAction(e -> loadRef[0].run());
        clearBtn.setOnAction(e -> {
            search.clear();
            filterCat.setValue("🎯 Catégorie");
            filterSort.setValue("📅 Trier par");
            loadRef[0].run();
        });
        loadRef[0].run();

        VBox page = new VBox(16, pageTitle, pageSub, toolbar, stripSection,
                recommendedSection, mapSection, eventsTitle, grid);
        page.setPadding(new Insets(16));
        page.setStyle("-fx-background-color:#F8F9FA;");

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color:#F8F9FA; -fx-background:#F8F9FA;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    // ── Date availability strip ───────────────────────────────────────────────

    private VBox buildDateStrip() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE", Locale.FRENCH);

        Label availTitle = new Label("📅  Vérifiez les disponibilités");
        availTitle.setStyle("-fx-text-fill:#1a1a2e; -fx-font-size:15px; -fx-font-weight:800;");
        Label availMonth = new Label(today.format(DateTimeFormatter.ofPattern("MMM. yyyy", Locale.FRENCH)).toUpperCase());
        availMonth.setStyle("-fx-text-fill:#adb5bd; -fx-font-size:11px; -fx-font-weight:700;");

        // Compute availability
        Map<LocalDate, Integer> codeMap    = new HashMap<>();
        Map<LocalDate, Integer> optionsMap = new HashMap<>();
        try {
            List<Event> all = eventService.getAllEvents();
            for (int i = 0; i < 9; i++) {
                LocalDate d = today.plusDays(i);
                int totalOpts = 0; boolean hasEvent = false; boolean allFull = true;
                for (Event ev : all) {
                    if (!ev.getDateEvent().toLocalDate().equals(d)) continue;
                    hasEvent = true;
                    int confirmed = reservationService.getConfirmedCountByEvent(ev.getId());
                    int rem = ev.getCapacite() - confirmed;
                    if (rem > 0) { totalOpts += rem; allFull = false; }
                }
                int code = hasEvent ? (allFull ? 2 : (totalOpts < 3 ? 0 : 1)) : 1;
                codeMap.put(d, code);
                optionsMap.put(d, totalOpts);
            }
        } catch (SQLException ignored) {}

        // Build cards
        HBox strip = new HBox(8);
        strip.setAlignment(Pos.CENTER_LEFT);
        strip.setPadding(new Insets(4, 0, 4, 0));

        for (int i = 0; i < 9; i++) {
            LocalDate d = today.plusDays(i);
            int code    = codeMap.getOrDefault(d, 1);
            int options = optionsMap.getOrDefault(d, 0);
            boolean isToday = d.equals(today);

            String dayName  = isToday ? "AUJOURD'HUI"
                    : d.format(dayFmt).toUpperCase(Locale.FRENCH) + ".";
            String barColor = code == 2 ? "#9e9e9e" : (code == 0 ? "#f59e0b" : "#22c55e");

            Label dayLbl = new Label(dayName);
            dayLbl.setStyle("-fx-text-fill:#8a9bb5; -fx-font-size:11px; -fx-font-weight:700;");
            Label numLbl = new Label(String.valueOf(d.getDayOfMonth()));
            numLbl.setStyle("-fx-text-fill:#0f2942; -fx-font-size:28px; -fx-font-weight:900;");
            Label optLbl = new Label(options + " options");
            optLbl.setStyle("-fx-text-fill:#8a9bb5; -fx-font-size:11px;");

            Region bar = new Region();
            bar.setPrefHeight(3); bar.setPrefWidth(60); bar.setMaxWidth(Double.MAX_VALUE);
            bar.setStyle("-fx-background-color:" + barColor + "; -fx-background-radius:99;");

            VBox card = new VBox(2, dayLbl, numLbl, optLbl, bar);
            card.setAlignment(Pos.CENTER);
            card.setPadding(new Insets(10, 12, 8, 12));
            card.setPrefWidth(100); card.setMinWidth(100); card.setMaxWidth(100);

            String base  = "-fx-background-color:white; -fx-background-radius:12; -fx-border-radius:12;"
                    + "-fx-border-color:" + (isToday ? "#b8d0ff" : "#dde8f5") + ";"
                    + "-fx-border-width:" + (isToday ? "2" : "1") + ";"
                    + "-fx-effect:dropshadow(gaussian,rgba(46,94,166,0.07),6,0,0,2); -fx-cursor:hand;";
            String hover = "-fx-background-color:#f5f9ff; -fx-background-radius:12; -fx-border-radius:12;"
                    + "-fx-border-color:#b8d0ff; -fx-border-width:2;"
                    + "-fx-effect:dropshadow(gaussian,rgba(46,94,166,0.12),8,0,0,2); -fx-cursor:hand;";
            card.setStyle(base);
            card.setOnMouseEntered(e -> card.setStyle(hover));
            card.setOnMouseExited(e -> card.setStyle(base));
            strip.getChildren().add(card);
        }

        // "+ Plus de dates" card
        Label moreLbl = new Label("Plus de\ndates");
        moreLbl.setStyle("-fx-text-fill:#2563eb; -fx-font-size:12px; -fx-font-weight:700; -fx-text-alignment:center;");
        moreLbl.setWrapText(true);
        VBox moreCard = new VBox(moreLbl);
        moreCard.setAlignment(Pos.CENTER);
        moreCard.setPadding(new Insets(10, 12, 8, 12));
        moreCard.setPrefWidth(80); moreCard.setMinWidth(80);
        moreCard.setStyle("-fx-background-color:#eff6ff; -fx-background-radius:12; -fx-border-radius:12;"
                + "-fx-border-color:#bfdbfe; -fx-border-width:1; -fx-cursor:hand;");
        strip.getChildren().add(moreCard);

        ScrollPane dateScroll = new ScrollPane(strip);
        dateScroll.setFitToHeight(true);
        dateScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        dateScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        dateScroll.setStyle("-fx-background-color:transparent; -fx-background:transparent;");

        VBox section = new VBox(8, availTitle, availMonth, dateScroll);
        section.setPadding(new Insets(16, 18, 16, 18));
        section.setStyle("-fx-background-color:white; -fx-background-radius:18;"
                + "-fx-border-color:#e9ecef; -fx-border-radius:18;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);");
        return section;
    }

    // ── Recommended section ───────────────────────────────────────────────────

    private VBox buildRecommendedSection() {
        Label title = new Label("🎯  Recommandé pour vous");
        title.setStyle("-fx-text-fill:#1a1a2e; -fx-font-size:18px; -fx-font-weight:900;");
        Label subtitle = new Label("Basé sur vos préférences et votre comportement");
        subtitle.setStyle("-fx-text-fill:#6c757d; -fx-font-size:12px;");

        TilePane recGrid = new TilePane();
        recGrid.setHgap(16); recGrid.setVgap(16);
        recGrid.setPrefColumns(3);
        recGrid.setTileAlignment(Pos.TOP_LEFT);
        recGrid.setPrefTileWidth(300);

        Runnable reload = () -> {};
        try {
            List<RecommendationResult> recs = currentUser != null
                    ? recommendationService.recommendForUser(currentUser.getId(), 3)
                    : Collections.emptyList();
            if (recs.isEmpty()) {
                Label noRec = new Label("💡  Réservez ou aimez des événements pour obtenir des recommandations personnalisées.");
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
                    int rc = resCounts.getOrDefault(ev.getId(), 0);
                    recGrid.getChildren().add(buildMiniRecommendCard(ev,
                            ev.getCapacite() - rc,
                            reserved.contains(ev.getId()),
                            liked.contains(ev.getId()),
                            likeCounts.getOrDefault(ev.getId(), 0),
                            rec.getScorePercent(), reload));
                }
            }
        } catch (SQLException ex) {
            recGrid.getChildren().add(new Label("Recommandations indisponibles."));
        }

        VBox section = new VBox(10, title, subtitle, recGrid);
        section.setPadding(new Insets(18, 20, 18, 20));
        section.setStyle("-fx-background-color:#f0fdf4; -fx-background-radius:18;"
                + "-fx-border-color:#bbf7d0; -fx-border-radius:18;"
                + "-fx-effect:dropshadow(gaussian,rgba(34,197,94,0.12),10,0,0,3);");
        return section;
    }

    private VBox buildMiniRecommendCard(Event ev, int remaining, boolean isReserved,
                                        boolean isLiked, int likeCount,
                                        String matchPct, Runnable reload) {
        StackPane img = createImagePane(ev.getImage(), 300, 170, ev.getTitre());

        Label matchBadge = new Label("✨ " + matchPct + " match");
        matchBadge.setStyle("-fx-background-color:#DCFCE7; -fx-text-fill:#15803D;"
                + "-fx-font-size:11px; -fx-font-weight:800; -fx-background-radius:6; -fx-padding:3 10;");
        Label titleLbl = new Label(ev.getTitre());
        titleLbl.setStyle("-fx-font-size:14px; -fx-font-weight:800; -fx-text-fill:#111827;");
        titleLbl.setWrapText(true);
        Label dateLbl = new Label(ev.getDateEvent() != null ? "📅 " + ev.getDateEvent().format(SHORT_FMT) : "");
        dateLbl.setStyle("-fx-text-fill:#6B7280; -fx-font-size:12px;");
        Label lieuLbl = new Label("📍 " + ev.getLieu());
        lieuLbl.setStyle("-fx-text-fill:#6B7280; -fx-font-size:12px;");
        Label spotsLbl = new Label(remaining > 0 ? "✅ " + remaining + " place(s)" : "🔴 Complet");
        spotsLbl.setStyle("-fx-text-fill:" + (remaining > 0 ? "#15803D" : "#DC2626")
                + "; -fx-font-size:11px; -fx-font-weight:700;");

        Button reserveBtn = new Button(isReserved ? "✓ Réservé" : "🎟 Réserver");
        reserveBtn.setStyle(isReserved ? SECONDARY : SUCCESS);
        reserveBtn.setDisable(isReserved || remaining <= 0);
        reserveBtn.setMaxWidth(Double.MAX_VALUE);
        reserveBtn.setOnAction(e -> showReservationDialog(ev, reload));

        VBox body = new VBox(6, matchBadge, titleLbl, dateLbl, lieuLbl, spotsLbl, reserveBtn);
        body.setPadding(new Insets(12, 14, 14, 14));

        VBox card = new VBox(img, body);
        card.setPrefWidth(300); card.setMaxWidth(300);
        String cs = "-fx-background-color:white; -fx-background-radius:16;"
                + "-fx-border-color:#D1FAE5; -fx-border-radius:16; -fx-border-width:1.5;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),10,0,0,3);";
        String ch = "-fx-background-color:white; -fx-background-radius:16;"
                + "-fx-border-color:#6EE7B7; -fx-border-radius:16; -fx-border-width:2;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.14),16,0,0,5);"
                + "-fx-scale-x:1.01; -fx-scale-y:1.01;";
        card.setStyle(cs);
        card.setOnMouseEntered(e -> card.setStyle(ch));
        card.setOnMouseExited(e -> card.setStyle(cs));
        return card;
    }

    // ── Inline map section (Leaflet via WebView) ──────────────────────────────

    private VBox buildInlineMapSection() {
        Label mapTitle = new Label("🗺️  Événements près de vous");
        mapTitle.setStyle("-fx-text-fill:#1a1a2e; -fx-font-size:18px; -fx-font-weight:800;");
        Label mapSub = new Label("La carte détecte votre position et affiche les événements triés par distance");
        mapSub.setStyle("-fx-text-fill:#6c757d; -fx-font-size:12px;");

        javafx.scene.web.WebView mapWebView = new javafx.scene.web.WebView();
        mapWebView.setPrefWidth(840);
        mapWebView.setPrefHeight(480);
        mapWebView.setMinHeight(480);
        mapWebView.setMaxHeight(480);
        mapWebView.setContextMenuEnabled(false);

        javafx.scene.web.WebEngine mapEngine = mapWebView.getEngine();
        mapEngine.setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        mapWebView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;
            Platform.runLater(() -> {
                try {
                    String leafletJs  = new String(
                            StudentMapView.class.getResourceAsStream("/leaflet.js").readAllBytes(),
                            java.nio.charset.StandardCharsets.UTF_8);
                    String leafletCss = new String(
                            StudentMapView.class.getResourceAsStream("/leaflet.css").readAllBytes(),
                            java.nio.charset.StandardCharsets.UTF_8);
                    mapEngine.loadContent(buildInlineMapHtml(leafletJs, leafletCss));
                    mapEngine.getLoadWorker().stateProperty().addListener((obs2, o2, n2) -> {
                        if (n2 != javafx.concurrent.Worker.State.SUCCEEDED) return;
                        mapEngine.executeScript(
                                "setTimeout(function(){map.invalidateSize();},100);"
                                + "setTimeout(function(){map.invalidateSize();},300);"
                                + "setTimeout(function(){map.invalidateSize();},700);");
                        new Thread(() -> {
                            try {
                                List<Event> evs = eventService.getAllEvents();
                                Map<Integer, Integer> rc = reservationService.getReservationCountsByEvent();
                                org.example.util.MapService ms = new org.example.util.MapService();
                                String json = ms.buildMarkersJson(evs, rc);
                                Platform.runLater(() -> {
                                    try { mapEngine.executeScript("loadMarkers(" + json + ")"); }
                                    catch (Exception ignored) {}
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

        VBox section = new VBox(8, mapTitle, mapSub, mapWebView);
        section.setPadding(new Insets(16, 18, 16, 18));
        section.setStyle("-fx-background-color:white; -fx-background-radius:18;"
                + "-fx-border-color:#e9ecef; -fx-border-radius:18;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);");
        return section;
    }

    private String buildInlineMapHtml(String js, String css) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>" + css
                + "html,body{margin:0;padding:0;width:100%;height:480px;overflow:hidden;}"
                + "#map{width:100%;height:480px;}"
                + "#banner{position:absolute;top:10px;left:50%;transform:translateX(-50%);"
                + "z-index:9999;background:rgba(255,255,255,.95);border-radius:999px;"
                + "padding:6px 16px;font-size:12px;font-weight:700;color:#0f2942;"
                + "box-shadow:0 2px 10px rgba(0,0,0,.15);white-space:nowrap;pointer-events:none;}"
                + "#legend{position:absolute;top:10px;right:10px;z-index:9999;"
                + "background:rgba(255,255,255,.95);border-radius:10px;padding:8px 12px;"
                + "box-shadow:0 2px 10px rgba(0,0,0,.12);font-size:11px;pointer-events:none;}"
                + ".lr{display:flex;align-items:center;gap:5px;margin:2px 0;color:#415a78;}"
                + ".ld{width:9px;height:9px;border-radius:50%;flex-shrink:0;}"
                + ".leaflet-popup-content-wrapper{border-radius:12px!important;}"
                + ".pb{padding:10px 12px;min-width:180px;}.pt{font-size:13px;font-weight:800;color:#0f2942;margin-bottom:4px;}"
                + ".pr{font-size:11px;color:#415a78;margin:2px 0;}.pd{font-size:11px;font-weight:700;color:#2563eb;margin:3px 0;}"
                + "</style></head><body>"
                + "<div id='map'></div>"
                + "<div id='banner'>Chargement de la carte...</div>"
                + "<div id='legend'>"
                + "<div class='lr'><div class='ld' style='background:#3b82f6'></div>Vous</div>"
                + "<div class='lr'><div class='ld' style='background:#22c55e'></div>&lt;2 km</div>"
                + "<div class='lr'><div class='ld' style='background:#f59e0b'></div>2-5 km</div>"
                + "<div class='lr'><div class='ld' style='background:#ef4444'></div>&gt;5 km</div>"
                + "</div>"
                + "<script>" + js + "\n"
                + "if(typeof map!=='undefined'&&map){map.remove();}"
                + "var map=L.map('map').setView([36.8065,10.1815],13);"
                + "L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',"
                + "{attribution:'(c)OpenStreetMap(c)CARTO',subdomains:'abcd',maxZoom:20}).addTo(map);"
                + "setTimeout(function(){map.invalidateSize();},100);"
                + "setTimeout(function(){map.invalidateSize();},500);"
                + "setTimeout(function(){map.invalidateSize();},1200);"
                + "var uLat=null,uLng=null,allM=[],allD=[],uMk=null;"
                + "function mkI(c){var s='<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"26\" height=\"34\" viewBox=\"0 0 32 42\">'+"
                + "'<path d=\"M16 0C7.163 0 0 7.163 0 16c0 10 16 26 16 26S32 26 32 16C32 7.163 24.837 0 16 0z\" fill=\"'+c+'\" stroke=\"white\" stroke-width=\"2.5\"/>'+"
                + "'<circle cx=\"16\" cy=\"16\" r=\"7\" fill=\"white\" opacity=\"0.9\"/></svg>';"
                + "return L.divIcon({html:s,iconSize:[26,34],iconAnchor:[13,34],popupAnchor:[0,-36],className:''}); }"
                + "function hav(a,b,c,d){var R=6371,dL=(c-a)*Math.PI/180,dG=(d-b)*Math.PI/180;"
                + "var x=Math.sin(dL/2)*Math.sin(dL/2)+Math.cos(a*Math.PI/180)*Math.cos(c*Math.PI/180)*Math.sin(dG/2)*Math.sin(dG/2);"
                + "return R*2*Math.atan2(Math.sqrt(x),Math.sqrt(1-x));}"
                + "function dc(k){return k<2?'#22c55e':k<5?'#f59e0b':'#ef4444';}"
                + "function addM(ev,c){var p='<div class=\"pb\">'+'<div class=\"pt\">'+ev.titre+'</div>'+"
                + "'<div class=\"pr\">'+ev.lieu+'</div>'+'<div class=\"pr\">'+ev.date+'</div>'+"
                + "'<div class=\"pr\">'+(ev.remaining>0?ev.remaining+' place(s)':'Complet')+'</div></div>';"
                + "allM.push(L.marker([ev.lat,ev.lng],{icon:mkI(c)}).bindPopup(p,{maxWidth:220}).addTo(map));}"
                + "function loadMarkers(data){allM.forEach(function(m){map.removeLayer(m);});allM=[];allD=data;"
                + "if(!data||!data.length){document.getElementById('banner').textContent='Aucun evenement';return;}"
                + "document.getElementById('banner').textContent=data.length+' evenement(s)';"
                + "data.forEach(function(ev){addM(ev,uLat!==null?dc(hav(uLat,uLng,ev.lat,ev.lng)):ev.color);});"
                + "if(uLat===null&&allM.length>0){var b=L.featureGroup(allM).getBounds();"
                + "if(b.isValid())map.fitBounds(b.pad(0.1),{maxZoom:14});}"
                + "setTimeout(function(){map.invalidateSize();},300);}"
                + "function uI(){var s='<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"32\" height=\"32\" viewBox=\"0 0 36 36\">'+"
                + "'<circle cx=\"18\" cy=\"18\" r=\"16\" fill=\"#3b82f6\" stroke=\"white\" stroke-width=\"3\"/>'+"
                + "'<circle cx=\"18\" cy=\"18\" r=\"7\" fill=\"white\"/></svg>';"
                + "return L.divIcon({html:s,iconSize:[32,32],iconAnchor:[16,16],className:''});}"
                + "if(navigator.geolocation){navigator.geolocation.getCurrentPosition(function(pos){"
                + "uLat=pos.coords.latitude;uLng=pos.coords.longitude;"
                + "if(uMk)map.removeLayer(uMk);"
                + "uMk=L.marker([uLat,uLng],{icon:uI(),zIndexOffset:1000}).addTo(map);"
                + "document.getElementById('banner').textContent='Position detectee';"
                + "allM.forEach(function(m){map.removeLayer(m);});allM=[];"
                + "allD.forEach(function(ev){addM(ev,dc(hav(uLat,uLng,ev.lat,ev.lng)));});"
                + "map.setView([uLat,uLng],13);"
                + "},function(){document.getElementById('banner').textContent='Position non disponible';},{timeout:8000});}"
                + "</script></body></html>";
    }

    // ── Student event card ────────────────────────────────────────────────────

    private VBox buildStudentEventCard(Event ev, int remaining, boolean isReserved,
                                       boolean isLiked, int likeCount, Runnable reload) {
        StackPane img = createImagePane(ev.getImage(), 380, 210, ev.getTitre());
        img.setCursor(Cursor.HAND);
        img.setOnMouseClicked(e -> showEventDetails(ev, reload));

        Label catTag = new Label(ev.getCategorie() != null ? ev.getCategorie().toUpperCase() : "GÉNÉRAL");
        catTag.setStyle("-fx-background-color:#e8f5e9; -fx-text-fill:#2e7d32;"
                + "-fx-font-size:10px; -fx-font-weight:900; -fx-background-radius:6; -fx-padding:3 10;");

        Label titleLbl = new Label(ev.getTitre());
        titleLbl.setStyle("-fx-font-size:16px; -fx-font-weight:900; -fx-text-fill:#1a1a2e;");
        titleLbl.setWrapText(true);

        Label dateLbl = new Label("📅 " + (ev.getDateEvent() != null ? ev.getDateEvent().format(FMT) : ""));
        dateLbl.setStyle("-fx-text-fill:#495057; -fx-font-size:12px; -fx-font-weight:600;");
        Label lieuLbl = new Label("📍 " + ev.getLieu());
        lieuLbl.setStyle("-fx-text-fill:#495057; -fx-font-size:12px;");

        String spotsText  = remaining > 0 ? "✅ " + remaining + " place(s)" : "🔴 Complet";
        String spotsColor = remaining > 0 ? "#1a7a4a" : "#c63d48";
        String spotsBg    = remaining > 0 ? "#e8f5e9" : "#fce4ec";
        Label spots = new Label(spotsText);
        spots.setStyle("-fx-text-fill:" + spotsColor + "; -fx-background-color:" + spotsBg
                + "; -fx-font-size:11px; -fx-font-weight:700; -fx-background-radius:6; -fx-padding:3 10;");

        Button likeBtn = new Button((isLiked ? "❤️" : "🤍") + " " + likeCount);
        likeBtn.setStyle("-fx-background-color:#f8f9fa; -fx-text-fill:#495057;"
                + "-fx-font-size:12px; -fx-background-radius:10; -fx-border-radius:10;"
                + "-fx-border-color:#dee2e6; -fx-padding:7 12; -fx-cursor:hand;");
        likeBtn.setOnAction(e -> {
            try { engagementService.toggleLike(ev.getId(), currentUser.getId()); reload.run(); }
            catch (SQLException ex) { showError(ex.getMessage()); }
        });

        Button detailsBtn = new Button("👁 Voir détails");
        detailsBtn.setStyle("-fx-background-color:#f0f4ff; -fx-text-fill:#3d5a99;"
                + "-fx-font-weight:700; -fx-font-size:12px; -fx-background-radius:10;"
                + "-fx-border-radius:10; -fx-border-color:#c5d0e6; -fx-padding:7 14; -fx-cursor:hand;");
        detailsBtn.setOnAction(e -> showEventDetails(ev, reload));

        Button reserveBtn = new Button(isReserved ? "✓ Réservé" : "🎟 Réserver maintenant");
        reserveBtn.setStyle(isReserved
                ? "-fx-background-color:#e9ecef; -fx-text-fill:#6c757d; -fx-font-weight:700;"
                  + "-fx-background-radius:10; -fx-padding:9 16; -fx-font-size:12px;"
                : SUCCESS);
        reserveBtn.setDisable(isReserved || remaining <= 0);
        reserveBtn.setMaxWidth(Double.MAX_VALUE);
        reserveBtn.setOnAction(e -> showReservationDialog(ev, reload));

        HBox topActions = new HBox(8, likeBtn, detailsBtn);
        topActions.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(8, catTag, titleLbl, dateLbl, lieuLbl, spots,
                new Separator(), topActions, reserveBtn);
        body.setPadding(new Insets(14, 16, 16, 16));

        VBox card = new VBox(img, body);
        card.setPrefWidth(380);
        card.setStyle(CARD);
        card.setOnMouseEntered(e -> card.setStyle(CARD_HOVER));
        card.setOnMouseExited(e -> card.setStyle(CARD));
        return card;
    }

    // ── Event details popup ───────────────────────────────────────────────────

    private void showEventDetails(Event ev, Runnable reload) {
        Stage stage = new Stage();
        stage.setTitle(ev.getTitre());

        int reserved = 0;
        try { reserved = reservationService.getReservationCountByEvent(ev.getId()); } catch (SQLException ignored) {}
        int remaining = Math.max(ev.getCapacite() - reserved, 0);
        boolean full = remaining <= 0;
        boolean alreadyReserved = false;
        try { alreadyReserved = currentUser != null && reservationService.hasActiveReservation(ev.getId(), currentUser.getId()); }
        catch (SQLException ignored) {}
        int likeCount = 0;
        boolean alreadyLiked = false;
        try {
            likeCount    = engagementService.getLikeCountsByEvent().getOrDefault(ev.getId(), 0);
            alreadyLiked = currentUser != null && engagementService.hasLikedEvent(ev.getId(), currentUser.getId());
        } catch (SQLException ignored) {}
        List<EventReview> reviews = new ArrayList<>();
        double avgRating = 0.0;
        try {
            reviews   = engagementService.getReviewsByEvent(ev.getId());
            avgRating = engagementService.getAverageRatingsByEvent().getOrDefault(ev.getId(), 0.0);
        } catch (SQLException ignored) {}

        // Weather widget
        Label wIcon = new Label("🌡️"); wIcon.setStyle("-fx-font-size:24px;");
        Label wCond = new Label("Chargement météo…");
        wCond.setStyle("-fx-font-size:13px; -fx-font-weight:800; -fx-text-fill:#0f2942;");
        Label wImpact = new Label(""); wImpact.setStyle("-fx-font-size:11px; -fx-text-fill:#637a97; -fx-font-style:italic;");
        wImpact.setWrapText(true);
        HBox weatherWidget = new HBox(10, wIcon, new VBox(2, wCond, wImpact));
        weatherWidget.setAlignment(Pos.CENTER_LEFT);
        weatherWidget.setPadding(new Insets(10, 14, 10, 14));
        weatherWidget.setStyle("-fx-background-color:#f0f9ff; -fx-background-radius:12; -fx-border-color:#bae6fd; -fx-border-radius:12;");

        new Thread(() -> {
            LocalDate date = ev.getDateEvent() != null ? ev.getDateEvent().toLocalDate() : LocalDate.now();
            String cond = weatherService.getWeatherCondition("Tunis", date);
            int temp    = weatherService.getTemperature("Tunis", date);
            String emoji = WeatherService.toEmoji(cond);
            Platform.runLater(() -> {
                wIcon.setText(emoji);
                wCond.setText(cond.equals("Unknown") ? "Météo indisponible" : emoji + "  " + cond + "  " + temp + "°C");
                wImpact.setText(buildWeatherAdvice(cond, ev.getCategorie()));
            });
        }, "weather-detail").start();

        // Info panel
        Label titleLbl = new Label(ev.getTitre());
        titleLbl.setStyle("-fx-font-size:20px; -fx-font-weight:900; -fx-text-fill:#1a1a1a;");
        titleLbl.setWrapText(true);

        Label capRow = new Label(full ? "🔴  Complet" : "✅  " + remaining + " place(s) disponible(s)");
        capRow.setStyle("-fx-font-size:13px; -fx-font-weight:700; -fx-text-fill:" + (full ? "#c63d48" : "#1a7a4a") + ";");

        Label descLbl = new Label(ev.getDescription() != null ? ev.getDescription() : "");
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#333; -fx-line-spacing:3;");

        // Reviews
        Label reviewsTitle = new Label("⭐  Avis (" + reviews.size() + ")  —  "
                + (reviews.isEmpty() ? "Aucune note" : String.format("%.1f / 5", avgRating)));
        reviewsTitle.setStyle("-fx-font-weight:700; -fx-font-size:13px; -fx-text-fill:#0f2942;");
        VBox reviewsBox = new VBox(6);
        for (EventReview r : reviews.stream().limit(3).collect(Collectors.toList())) {
            Label rv = new Label("★".repeat(r.getRating()) + "  " + r.getAuthorName() + ": " + r.getComment());
            rv.setStyle("-fx-font-size:12px; -fx-text-fill:#495057;"); rv.setWrapText(true);
            reviewsBox.getChildren().add(rv);
        }

        VBox infoPanel = new VBox(10, titleLbl,
                new Label("📅 " + (ev.getDateEvent() != null ? ev.getDateEvent().format(FMT) : "")),
                new Label("📍 " + ev.getLieu()),
                capRow, weatherWidget,
                new Separator(), descLbl,
                new Separator(), reviewsTitle, reviewsBox);
        infoPanel.setPadding(new Insets(20)); infoPanel.setPrefWidth(340);
        infoPanel.setStyle("-fx-background-color:#faf9f7;");

        StackPane imgPane = createImagePane(ev.getImage(), 380, 340, ev.getTitre());
        HBox contentCard = new HBox(infoPanel, imgPane);
        contentCard.setStyle("-fx-background-color:#faf9f7; -fx-border-color:#d4c9b0; -fx-border-width:1;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),14,0,0,4);");

        // Actions bar
        int[] likeRef = {likeCount};
        Label likeCountLbl = new Label(likeCount + " ❤️");
        likeCountLbl.setStyle("-fx-font-size:13px; -fx-text-fill:#888; -fx-font-weight:600;");
        Button likeBtn = new Button(alreadyLiked ? "❤️  Retirer" : "🤍  J'aime");
        likeBtn.setOnAction(e -> {
            try {
                boolean nowLiked = engagementService.toggleLike(ev.getId(), currentUser.getId());
                likeRef[0] += nowLiked ? 1 : -1;
                likeCountLbl.setText(likeRef[0] + " ❤️");
                likeBtn.setText(nowLiked ? "❤️  Retirer" : "🤍  J'aime");
                reload.run();
            } catch (SQLException ex) { showError(ex.getMessage()); }
        });
        boolean finalAlreadyReserved = alreadyReserved;
        Button reserveBtn = new Button(alreadyReserved ? "✓ Déjà réservé" : (full ? "🔴 Complet" : "🎟  Réserver"));
        reserveBtn.setStyle(alreadyReserved || full ? SECONDARY : PRIMARY);
        reserveBtn.setDisable(alreadyReserved || full);
        reserveBtn.setOnAction(e -> { showReservationDialog(ev, reload); stage.close(); });
        Button reviewBtn = new Button("✏️  Avis");
        reviewBtn.setOnAction(e -> { showReviewDialog(ev); stage.close(); showEventDetails(ev, reload); });
        Button closeBtn = new Button("✕  Fermer");
        closeBtn.setOnAction(e -> stage.close());

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actionsBar = new HBox(10, likeBtn, likeCountLbl, spacer, reviewBtn, reserveBtn, closeBtn);
        actionsBar.setAlignment(Pos.CENTER_LEFT);
        actionsBar.setPadding(new Insets(14, 20, 14, 20));
        actionsBar.setStyle("-fx-background-color:white; -fx-border-color:#e5e7eb; -fx-border-width:1 0 0 0;");

        VBox root = new VBox(contentCard, actionsBar);
        VBox.setVgrow(contentCard, Priority.ALWAYS);

        javafx.scene.Scene sc = new javafx.scene.Scene(root, 720, 480);
        stage.setScene(sc);
        stage.setResizable(true);
        stage.show();
    }

    // ── Calendar tab ──────────────────────────────────────────────────────────

    private Node buildCalendarTab() {
        Label pageTitle = new Label("Calendrier des événements");
        pageTitle.setStyle("-fx-text-fill:#0f2942; -fx-font-size:26px; -fx-font-weight:800;");
        Label sub = new Label("Sélectionnez une date pour voir les événements disponibles.");
        sub.setStyle("-fx-text-fill:#637a97; -fx-font-size:13px;");

        Button refreshBtn = new Button("Actualiser");
        refreshBtn.setStyle(SECONDARY);

        DualMonthCalendarView calendar = new DualMonthCalendarView(eventService, reservationService, null);

        VBox eventsForDate = new VBox(8);
        Label eventsDateTitle = new Label("Choisissez une date pour voir les événements");
        eventsDateTitle.setStyle("-fx-font-size:15px; -fx-font-weight:700; -fx-text-fill:#0f2942;");
        Label clickHint = new Label("Cliquez sur une date du calendrier ci-dessus.");
        clickHint.setStyle("-fx-font-size:12px; -fx-text-fill:#637a97; -fx-font-style:italic;");
        eventsForDate.getChildren().addAll(eventsDateTitle, clickHint);
        eventsForDate.setPadding(new Insets(14, 18, 14, 18));
        eventsForDate.setStyle("-fx-background-color:white; -fx-background-radius:14;"
                + "-fx-border-color:#e5e7eb; -fx-border-radius:14;");

        calendar.setOnDateSelected(date -> {
            eventsForDate.getChildren().clear();
            eventsForDate.getChildren().add(new Label("📅  Événements du " + date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH))));
            try {
                List<Event> dayEvents = eventService.filterEvents(null, date, null);
                if (dayEvents.isEmpty()) {
                    Label none = new Label("Aucun événement ce jour.");
                    none.setStyle("-fx-text-fill:#9ca3af; -fx-font-style:italic;");
                    eventsForDate.getChildren().add(none);
                } else {
                    for (Event ev : dayEvents) {
                        int rc = reservationService.getConfirmedCountByEvent(ev.getId());
                        int rem = ev.getCapacite() - rc;
                        HBox row = new HBox(10);
                        Label t = new Label(ev.getTitre());
                        t.setStyle("-fx-font-weight:700; -fx-font-size:13px;");
                        Label info = new Label("📍 " + ev.getLieu() + "  —  " + (rem > 0 ? rem + " places" : "Complet"));
                        info.setStyle("-fx-text-fill:#6b7280; -fx-font-size:11px;");
                        Button resBtn = new Button(rem > 0 ? "🎟 Réserver" : "Complet");
                        resBtn.setStyle(rem > 0 ? PRIMARY + "-fx-font-size:11px; -fx-padding:5 12;" : SECONDARY + "-fx-font-size:11px;");
                        resBtn.setDisable(rem <= 0);
                        resBtn.setOnAction(e -> showReservationDialog(ev, () -> {}));
                        row.getChildren().addAll(new VBox(2, t, info), new Region(), resBtn);
                        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
                        row.setAlignment(Pos.CENTER_LEFT);
                        row.setPadding(new Insets(8, 12, 8, 12));
                        row.setStyle("-fx-background-color:#f9fafb; -fx-background-radius:8;");
                        eventsForDate.getChildren().add(row);
                    }
                }
            } catch (SQLException ex) {
                eventsForDate.getChildren().add(new Label("Erreur: " + ex.getMessage()));
            }
        });

        refreshBtn.setOnAction(e -> calendar.reloadFromDatabase());

        VBox page = new VBox(14, pageTitle, sub, new HBox(refreshBtn), calendar, eventsForDate);
        page.setPadding(new Insets(16));
        page.setStyle("-fx-background-color:#F8F9FA;");

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#F8F9FA; -fx-background:#F8F9FA;");
        return scroll;
    }

    // ── My Reservations tab ───────────────────────────────────────────────────

    private Node buildMyReservationsTab() {
        Label pageTitle = new Label("Mes Réservations");
        pageTitle.setStyle("-fx-text-fill:#0f2942; -fx-font-size:26px; -fx-font-weight:800;");

        ListView<ReservationRecord> list = new ListView<>();
        list.setStyle("-fx-background-color:transparent; -fx-control-inner-background:transparent;");
        VBox.setVgrow(list, Priority.ALWAYS);
        list.setMinHeight(200);

        Runnable loadList = () -> {
            try {
                List<ReservationRecord> mine = reservationService.getAllReservations().stream()
                        .filter(r -> currentUser != null && r.getUserId() == currentUser.getId())
                        .collect(Collectors.toList());
                list.setItems(FXCollections.observableArrayList(mine));
            } catch (SQLException ex) { showError(ex.getMessage()); }
        };

        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(ReservationRecord r, boolean empty) {
                super.updateItem(r, empty); setText(null);
                if (empty || r == null) { setGraphic(null); return; }

                Label titleLbl = new Label(r.getEventTitle() != null ? r.getEventTitle() : "Événement #" + r.getEventId());
                titleLbl.setStyle("-fx-font-weight:800; -fx-font-size:14px; -fx-text-fill:#0f2942;");
                Label dateLbl = new Label("📅 Réservé le " + (r.getReservedAt() != null ? r.getReservedAt().format(FMT) : ""));
                dateLbl.setStyle("-fx-text-fill:#637a97; -fx-font-size:12px;");

                boolean waitlisted = "WAITLISTED".equalsIgnoreCase(r.getStatus());
                Label badge = new Label(waitlisted ? "En attente" : "Confirmée");
                badge.setStyle(waitlisted
                        ? "-fx-background-color:#fff3e0; -fx-text-fill:#e65100; -fx-background-radius:8; -fx-padding:3 10; -fx-font-weight:700;"
                        : "-fx-background-color:#e6f9f0; -fx-text-fill:#1a7a4a; -fx-background-radius:8; -fx-padding:3 10; -fx-font-weight:700;");

                Button cancelBtn = new Button("Annuler");
                cancelBtn.setStyle(DANGER + "-fx-font-size:11px; -fx-padding:5 12;");
                cancelBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Annuler la réservation pour « " + r.getEventTitle() + " » ?",
                            ButtonType.YES, ButtonType.NO);
                    confirm.setHeaderText(null);
                    confirm.showAndWait().ifPresent(bt -> {
                        if (bt == ButtonType.YES) {
                            try {
                                reservationService.cancelReservation(r.getEventId(), r.getId(),
                                        currentUser != null ? currentUser.getId() : 0);
                                loadList.run();
                            } catch (SQLException ex) { showError(ex.getMessage()); }
                        }
                    });
                });

                HBox meta = new HBox(12, dateLbl, badge);
                meta.setAlignment(Pos.CENTER_LEFT);
                VBox info = new VBox(5, titleLbl, meta);
                HBox.setHgrow(info, Priority.ALWAYS);
                HBox row = new HBox(12, info, cancelBtn);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(12, 16, 12, 16));
                row.setStyle("-fx-background-color:white; -fx-background-radius:14;"
                        + "-fx-border-color:#edf2fb; -fx-border-radius:14;");
                setGraphic(row);
                setStyle("-fx-background-color:transparent; -fx-padding:4 0;");
            }
        });

        Button refresh = new Button("Actualiser");
        refresh.setStyle(SECONDARY);
        refresh.setOnAction(e -> loadList.run());
        loadList.run();

        VBox page = new VBox(18, pageTitle, new HBox(refresh), list);
        page.setPadding(new Insets(16));
        page.setStyle("-fx-background-color:#F8F9FA;");
        VBox.setVgrow(list, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#F8F9FA; -fx-background:#F8F9FA;");
        return scroll;
    }

    // ── Dedicated Map tab (StudentMapView) ────────────────────────────────────

    private Node buildMapTab() {
        Label pageTitle = new Label("Événements près de vous");
        pageTitle.setStyle("-fx-text-fill:#0f2942; -fx-font-size:22px; -fx-font-weight:800;");
        Label sub = new Label("La carte détecte votre position et affiche les événements triés par distance");
        sub.setStyle("-fx-text-fill:#637a97; -fx-font-size:12px;");

        StudentMapView mapView = new StudentMapView();
        mapView.setOnEventClick(eventId -> {
            try {
                Event ev = eventService.getEventById(eventId);
                if (ev != null) showEventDetails(ev, () -> {});
            } catch (SQLException ex) { showError(ex.getMessage()); }
        });
        VBox.setVgrow(mapView, Priority.ALWAYS);

        new Thread(() -> {
            try {
                List<Event> evs = eventService.getAllEvents();
                Map<Integer, Integer> rc = reservationService.getReservationCountsByEvent();
                Platform.runLater(() -> mapView.loadEvents(evs, rc));
            } catch (Exception ex) { System.err.println("[MapTab] " + ex.getMessage()); }
        }, "map-tab-load").start();

        VBox page = new VBox(10, pageTitle, sub, mapView);
        page.setPadding(new Insets(16, 16, 0, 16));
        VBox.setVgrow(mapView, Priority.ALWAYS);
        VBox.setVgrow(page, Priority.ALWAYS);
        return page;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ADMIN tabs
    // ══════════════════════════════════════════════════════════════════════════

    private Node buildAdminEventsTab() {
        Label pageTitle = new Label("📅 Événements");
        pageTitle.setStyle("-fx-text-fill:#10233f; -fx-font-size:22px; -fx-font-weight:900;");
        Label pageSub = new Label("Gérez, modifiez et supprimez les événements de la plateforme.");
        pageSub.setStyle("-fx-text-fill:#6b7280; -fx-font-size:12px;");

        TextField search = new TextField();
        search.setPromptText("🔍 Rechercher un événement...");
        search.setStyle(INPUT); HBox.setHgrow(search, Priority.ALWAYS);
        Button addBtn = new Button("➕ Nouvel événement");
        addBtn.setStyle(SUCCESS);
        Button refreshBtn = new Button("🔄 Actualiser");
        refreshBtn.setStyle(SECONDARY);

        // Status label for inline error/info feedback
        Label statusLbl = new Label();
        statusLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#dc2626;");
        statusLbl.setVisible(false);

        FlowPane grid = new FlowPane();
        grid.setHgap(18); grid.setVgap(18);
        grid.setPrefWrapLength(Double.MAX_VALUE);

        Runnable[] loadRef = {null};
        loadRef[0] = () -> {
            grid.getChildren().clear();
            statusLbl.setVisible(false);
            try {
                String q = search.getText();
                List<Event> events = eventService.getEvents(q == null || q.isBlank() ? null : q, null);
                if (events.isEmpty()) {
                    grid.getChildren().add(emptyState("Aucun événement trouvé — cliquez sur « Nouvel événement » pour commencer."));
                } else {
                    for (Event ev : events) grid.getChildren().add(buildAdminEventCard(ev, loadRef[0]));
                }
            } catch (SQLException ex) {
                statusLbl.setText("⚠️ Erreur de chargement : " + ex.getMessage());
                statusLbl.setVisible(true);
                grid.getChildren().add(emptyState("Impossible de charger les événements."));
            }
        };
        this.refreshAdminGrid = loadRef[0];
        search.textProperty().addListener((o, a, b) -> loadRef[0].run());
        refreshBtn.setOnAction(e -> loadRef[0].run());
        addBtn.setOnAction(e -> showAdminFormDialog(null, loadRef[0]));
        loadRef[0].run();

        HBox toolbar = new HBox(10, search, refreshBtn, addBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 4, 0));

        VBox page = new VBox(10, pageTitle, pageSub, toolbar, statusLbl, grid);
        page.setPadding(new Insets(18));
        VBox.setVgrow(grid, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMaxHeight(Double.MAX_VALUE);
        scroll.setMaxWidth(Double.MAX_VALUE);
        scroll.setStyle("-fx-background-color:#F0F4FF; -fx-background:#F0F4FF;");
        StackPane.setAlignment(scroll, Pos.TOP_LEFT);
        return scroll;
    }

    private VBox buildAdminEventCard(Event ev, Runnable reload) {
        StackPane imgPane = createImagePane(ev.getImage(), 320, 170, ev.getTitre());

        Label catBadge = new Label(ev.getCategorie() != null ? ev.getCategorie().toUpperCase() : "GÉNÉRAL");
        catBadge.setStyle("-fx-background-color:#dbeafe; -fx-text-fill:#1d4ed8;"
                + "-fx-font-size:10px; -fx-font-weight:700; -fx-background-radius:6; -fx-padding:3 8;");
        Label titleLbl = new Label(ev.getTitre());
        titleLbl.setStyle("-fx-font-size:14px; -fx-font-weight:900; -fx-text-fill:#0f2942;");
        titleLbl.setWrapText(true);
        Label dateLbl = new Label("📅 " + (ev.getDateEvent() != null ? ev.getDateEvent().format(FMT) : ""));
        dateLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#6b7280;");
        Label lieuLbl = new Label("📍 " + ev.getLieu());
        lieuLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#6b7280;");

        int rc = 0;
        try { rc = reservationService.getConfirmedCountByEvent(ev.getId()); } catch (SQLException ignored) {}
        Label capLbl = new Label("👥 " + rc + " / " + ev.getCapacite());
        capLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#6b7280;");

        Button editBtn = new Button("✏️ Modifier");
        editBtn.setStyle(PRIMARY + "-fx-font-size:11px; -fx-padding:6 12;");
        editBtn.setOnAction(e -> showAdminFormDialog(ev, reload));

        Button deleteBtn = new Button("🗑 Supprimer");
        deleteBtn.setStyle(DANGER + "-fx-font-size:11px; -fx-padding:6 12;");
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

        HBox actions = new HBox(8, editBtn, deleteBtn);
        VBox body = new VBox(7, catBadge, titleLbl, dateLbl, lieuLbl, capLbl, actions);
        body.setPadding(new Insets(10, 12, 12, 12));

        VBox card = new VBox(imgPane, body);
        card.setPrefWidth(320);
        card.setStyle(CARD);
        card.setOnMouseEntered(e -> card.setStyle(CARD_HOVER));
        card.setOnMouseExited(e -> card.setStyle(CARD));
        return card;
    }

    private void showAdminFormDialog(Event toEdit, Runnable reload) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(toEdit == null ? "Nouvel événement" : "Modifier : " + toEdit.getTitre());
        dialog.setHeaderText(null);
        dialog.getDialogPane().setPrefWidth(720);
        dialog.getDialogPane().setContent(buildAdminFormContent(toEdit));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
        reload.run();
    }

    private Node buildAdminFormContent(Event toEdit) {
        Label formTitle = new Label(toEdit == null ? "Nouvel événement" : "Modifier l'événement");
        formTitle.setStyle("-fx-text-fill:#10233f; -fx-font-size:22px; -fx-font-weight:800;");

        TextField titleF = new TextField(toEdit != null ? toEdit.getTitre() : "");
        titleF.setPromptText("Ex: Morning Yoga Session"); titleF.setStyle(INPUT); titleF.setMaxWidth(Double.MAX_VALUE);
        TextArea descArea = new TextArea(toEdit != null ? toEdit.getDescription() : "");
        descArea.setPromptText("Description"); descArea.setStyle(INPUT); descArea.setPrefRowCount(4);
        descArea.setWrapText(true); descArea.setMaxWidth(Double.MAX_VALUE);
        TextField locF = new TextField(toEdit != null ? toEdit.getLieu() : "");
        locF.setPromptText("Ex: Tunis, Lac 2"); locF.setStyle(INPUT); locF.setMaxWidth(Double.MAX_VALUE);
        DatePicker dp = new DatePicker(toEdit != null && toEdit.getDateEvent() != null
                ? toEdit.getDateEvent().toLocalDate() : LocalDate.now().plusDays(1));
        dp.setMaxWidth(Double.MAX_VALUE); dp.setStyle(INPUT);
        Spinner<Integer> hSpin = new Spinner<>(0, 23, toEdit != null && toEdit.getDateEvent() != null
                ? toEdit.getDateEvent().getHour() : 10);
        hSpin.setEditable(true); hSpin.setPrefWidth(80);
        Spinner<Integer> mSpin = new Spinner<>(0, 59, toEdit != null && toEdit.getDateEvent() != null
                ? toEdit.getDateEvent().getMinute() : 0, 5);
        mSpin.setEditable(true); mSpin.setPrefWidth(80);
        HBox timeRow = new HBox(8, new Label("h"), hSpin, new Label("min"), mSpin);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        TextField capF = new TextField(toEdit != null ? String.valueOf(toEdit.getCapacite()) : "");
        capF.setPromptText("Capacité"); capF.setStyle(INPUT); capF.setMaxWidth(Double.MAX_VALUE);
        javafx.collections.ObservableList<String> catItems = FXCollections.observableArrayList(
                "yoga", "wellness", "sport", "meditation", "conference", "atelier", "── + Nouvelle catégorie ──");
        ComboBox<String> catBox = new ComboBox<>(catItems);
        catBox.setEditable(true);
        catBox.setValue(toEdit != null && toEdit.getCategorie() != null ? toEdit.getCategorie() : "yoga");
        catBox.setMaxWidth(Double.MAX_VALUE); catBox.setStyle(INPUT);

        TextField imgF = new TextField(toEdit != null && toEdit.getImage() != null ? toEdit.getImage() : "");
        imgF.setEditable(false); imgF.setStyle(INPUT); imgF.setMaxWidth(Double.MAX_VALUE);
        Label imgLabel = new Label(toEdit != null && toEdit.getImage() != null
                ? new File(toEdit.getImage()).getName() : "Aucune image sélectionnée");
        imgLabel.setStyle("-fx-text-fill:#999; -fx-font-size:12px;");
        Button browseBtn = new Button("📁 Parcourir"); browseBtn.setStyle(SECONDARY);
        browseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.gif"));
            File f = fc.showOpenDialog(null);
            if (f != null) { imgF.setText(f.getAbsolutePath()); imgLabel.setText(f.getName()); }
        });

        // Weather widget
        Label wEmoji = new Label("🌡️"); wEmoji.setStyle("-fx-font-size:22px;");
        Label wCondLbl = new Label("Sélectionnez une date");
        wCondLbl.setStyle("-fx-font-size:13px; -fx-font-weight:700; -fx-text-fill:#0f2942;");
        Label wAdviceLbl = new Label(""); wAdviceLbl.setStyle("-fx-font-size:11px; -fx-font-style:italic; -fx-text-fill:#374151;");
        wAdviceLbl.setWrapText(true);
        HBox weatherBox = new HBox(12, wEmoji, new VBox(3, wCondLbl, wAdviceLbl));
        weatherBox.setAlignment(Pos.CENTER_LEFT);
        weatherBox.setPadding(new Insets(10, 14, 10, 14));
        weatherBox.setStyle("-fx-background-color:#f0f9ff; -fx-background-radius:10; -fx-border-color:#bae6fd; -fx-border-radius:10;");
        dp.valueProperty().addListener((obs, o, n) -> {
            if (n == null) return;
            new Thread(() -> {
                String cond = weatherService.getWeatherCondition("Tunis", n);
                int temp = weatherService.getTemperature("Tunis", n);
                String emoji = WeatherService.toEmoji(cond);
                Platform.runLater(() -> {
                    wEmoji.setText(emoji);
                    wCondLbl.setText(cond.equals("Unknown") ? "Météo indisponible" : emoji + "  " + cond + "  " + temp + "°C");
                    wAdviceLbl.setText(buildWeatherAdvice(cond, null));
                });
            }, "weather-form").start();
        });

        Label statusLbl = new Label(""); statusLbl.setStyle("-fx-font-size:13px; -fx-font-weight:600;");
        Button saveBtn = new Button(toEdit == null ? "💾 Enregistrer" : "💾 Mettre à jour");
        saveBtn.setStyle(SUCCESS); saveBtn.setMaxWidth(Double.MAX_VALUE);
        Button cancelBtn = new Button("✕ Annuler"); cancelBtn.setStyle(SECONDARY);

        Event[] editRef = {toEdit};
        saveBtn.setOnAction(e -> {
            try {
                String titre = titleF.getText().trim();
                String desc  = descArea.getText().trim();
                String lieu  = locF.getText().trim();
                LocalDate date = dp.getValue();
                int h = hSpin.getValue(), m = mSpin.getValue();
                int cap = Integer.parseInt(capF.getText().trim());
                String cat = catBox.getValue() != null ? catBox.getValue().trim() : "";
                String img = imgF.getText().trim(); if (img.isBlank()) img = null;
                LocalDateTime dt = LocalDateTime.of(date, LocalTime.of(h, m));
                Event ev = new Event(titre, desc, dt, lieu, cap, cat, img,
                        currentUser != null ? currentUser.getId() : null);
                if (editRef[0] == null) {
                    eventService.addEvent(ev);
                    statusLbl.setText("✅ Événement créé !");
                    statusLbl.setStyle("-fx-text-fill:#16a34a; -fx-font-weight:700;");
                } else {
                    ev.setId(editRef[0].getId());
                    eventService.updateEvent(ev);
                    statusLbl.setText("✅ Mis à jour !");
                    statusLbl.setStyle("-fx-text-fill:#16a34a; -fx-font-weight:700;");
                }
                refreshAdminGrid.run();
            } catch (NumberFormatException ex) {
                statusLbl.setText("❌ Capacité invalide (entier requis).");
                statusLbl.setStyle("-fx-text-fill:#dc2626; -fx-font-weight:700;");
            } catch (SQLException ex) {
                statusLbl.setText("❌ " + ex.getMessage());
                statusLbl.setStyle("-fx-text-fill:#dc2626; -fx-font-weight:700;");
            }
        });
        cancelBtn.setOnAction(e -> { titleF.clear(); descArea.clear(); locF.clear(); capF.clear(); imgF.clear(); statusLbl.setText(""); });

        GridPane form = new GridPane();
        form.setHgap(14); form.setVgap(12);
        ColumnConstraints lc = new ColumnConstraints(130);
        ColumnConstraints fc = new ColumnConstraints(); fc.setHgrow(Priority.ALWAYS); fc.setFillWidth(true);
        form.getColumnConstraints().addAll(lc, fc);
        int r = 0;
        addFRow(form, r++, "Titre *",       titleF);
        addFRow(form, r++, "Description *", descArea);
        addFRow(form, r++, "Lieu *",         locF);
        addFRow(form, r++, "Date *",         dp);
        addFRow(form, r++, "Heure",          timeRow);
        addFRow(form, r++, "Capacité *",     capF);
        addFRow(form, r++, "Catégorie",      catBox);
        addFRow(form, r++, "Image",          new VBox(6, new HBox(10, browseBtn, imgLabel), imgF));
        addFRow(form, r,   "Météo",          weatherBox);

        VBox card = new VBox(14, formTitle, form, new HBox(10, saveBtn, cancelBtn), statusLbl);
        card.setPadding(new Insets(18)); card.setMaxWidth(660);
        card.setStyle("-fx-background-color:white; -fx-background-radius:16;"
                + "-fx-border-color:#e5e7eb; -fx-border-radius:16;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),10,0,0,3);");

        VBox page = new VBox(14, card);
        page.setPadding(new Insets(14));
        return page;
    }

    private void addFRow(GridPane g, int row, String label, Node field) {
        Label l = new Label(label);
        l.setStyle("-fx-font-weight:700; -fx-font-size:13px; -fx-text-fill:#374151;");
        l.setAlignment(Pos.CENTER_RIGHT); l.setMaxWidth(Double.MAX_VALUE);
        g.add(l, 0, row); g.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private Node buildAdminReservationsTab() {
        Label pageTitle = new Label("Réservations");
        pageTitle.setStyle("-fx-text-fill:#10233f; -fx-font-size:24px; -fx-font-weight:800;");

        TableView<ReservationRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<ReservationRecord, String> evCol = new TableColumn<>("Événement");
        evCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getEventTitle()));
        TableColumn<ReservationRecord, String> uCol = new TableColumn<>("Utilisateur");
        uCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getAuthorName()));
        TableColumn<ReservationRecord, String> dCol = new TableColumn<>("Date réservation");
        dCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getReservedAt() != null ? c.getValue().getReservedAt().format(FMT) : ""));
        TableColumn<ReservationRecord, String> sCol = new TableColumn<>("Statut");
        sCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getStatus()));
        sCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(s);
                badge.setStyle("WAITLISTED".equalsIgnoreCase(s)
                        ? "-fx-background-color:#fff3e0; -fx-text-fill:#e65100; -fx-background-radius:8; -fx-padding:2 8; -fx-font-weight:700;"
                        : "-fx-background-color:#e6f9f0; -fx-text-fill:#1a7a4a; -fx-background-radius:8; -fx-padding:2 8; -fx-font-weight:700;");
                setGraphic(badge); setText(null);
            }
        });
        table.getColumns().addAll(evCol, uCol, dCol, sCol);

        Button refreshBtn = new Button("🔄 Rafraîchir"); refreshBtn.setStyle(SECONDARY);
        Runnable load = () -> {
            try { table.setItems(FXCollections.observableArrayList(reservationService.getAllReservations())); }
            catch (SQLException ex) { showError(ex.getMessage()); }
        };
        refreshBtn.setOnAction(e -> load.run());
        load.run();

        VBox page = new VBox(14, pageTitle, refreshBtn, table);
        page.setPadding(new Insets(14));
        VBox.setVgrow(table, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#F8F9FA; -fx-background:#F8F9FA;");
        return scroll;
    }

    // ── Reservation dialog ────────────────────────────────────────────────────

    private void showReservationDialog(Event ev, Runnable reload) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Réserver : " + ev.getTitre()); dialog.setHeaderText(null);
        TextField nomF = new TextField(); nomF.setPromptText("Nom"); nomF.setStyle(INPUT);
        TextField prenomF = new TextField(); prenomF.setPromptText("Prénom"); prenomF.setStyle(INPUT);
        TextField telF = new TextField(); telF.setPromptText("Téléphone"); telF.setStyle(INPUT);
        TextField emailF = new TextField(); emailF.setPromptText("Email"); emailF.setStyle(INPUT);
        GridPane form = new GridPane(); form.setHgap(12); form.setVgap(12);
        ColumnConstraints lc = new ColumnConstraints(100);
        ColumnConstraints fc = new ColumnConstraints(); fc.setHgrow(Priority.ALWAYS); fc.setFillWidth(true);
        form.getColumnConstraints().addAll(lc, fc);
        addFRow(form, 0, "Nom", nomF); addFRow(form, 1, "Prénom", prenomF);
        addFRow(form, 2, "Téléphone", telF); addFRow(form, 3, "Email", emailF);
        form.setPadding(new Insets(16)); form.setPrefWidth(380);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    ValidationUtil.validateName(nomF.getText().trim(), "Le nom");
                    ValidationUtil.validateName(prenomF.getText().trim(), "Le prénom");
                    ValidationUtil.validatePhone(telF.getText().trim());
                    ValidationUtil.validateEmail(emailF.getText().trim());
                } catch (IllegalArgumentException ex) { showError(ex.getMessage()); return; }
                try {
                    ReservationService.ReservationResult result =
                            reservationService.reserveEvent(ev, currentUser != null ? currentUser.getId() : 0);
                    if (result != null && result.status == ReservationStatus.CONFIRMED) {
                        try { reminderService.createDefaultReminderIfMissing(currentUser.getId(), ev.getId()); }
                        catch (SQLException ignored) {}
                        showInfo("Réservation confirmée ✅");
                    } else if (result != null) {
                        showInfo("Ajouté à la liste d'attente (position " + result.waitlistPosition + ").");
                    }
                    reload.run();
                } catch (SQLException ex) { showError(ex.getMessage()); }
            }
        });
    }

    // ── Review dialog ─────────────────────────────────────────────────────────

    private void showReviewDialog(Event ev) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Avis : " + ev.getTitre()); dialog.setHeaderText(null);
        ComboBox<Integer> ratingBox = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        ratingBox.setValue(5); ratingBox.setStyle(INPUT);
        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Votre commentaire…"); commentArea.setStyle(INPUT); commentArea.setPrefRowCount(4);
        try {
            EventReview ex = engagementService.getReviewByEventAndUser(ev.getId(), currentUser.getId());
            if (ex != null) { ratingBox.setValue(ex.getRating()); commentArea.setText(ex.getComment()); }
        } catch (SQLException ignored) {}
        GridPane form = new GridPane(); form.setHgap(12); form.setVgap(12);
        ColumnConstraints lc = new ColumnConstraints(100);
        ColumnConstraints fc = new ColumnConstraints(); fc.setHgrow(Priority.ALWAYS); fc.setFillWidth(true);
        form.getColumnConstraints().addAll(lc, fc);
        addFRow(form, 0, "Note (1-5)", ratingBox); addFRow(form, 1, "Commentaire", commentArea);
        form.setPadding(new Insets(16)); form.setPrefWidth(420);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    ValidationUtil.validateComment(commentArea.getText());
                    engagementService.addOrUpdateReview(ev.getId(), currentUser.getId(),
                            ratingBox.getValue(), commentArea.getText());
                    showInfo("Avis enregistré ✅");
                } catch (IllegalArgumentException | SQLException ex) { showError(ex.getMessage()); }
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void initSchema() {
        try {
            eventService.createTableIfNotExists();
            engagementService.initializeTables();
            reservationService.initializeReservations();
            reminderService.initializeReminderTable();
        } catch (Exception e) {
            System.err.println("[EventsModuleView] Schema init: " + e.getMessage());
        }
    }

    private StackPane createImagePane(String imagePath, double w, double h, String fallback) {
        StackPane pane = new StackPane();
        pane.setPrefSize(w, h); pane.setMinHeight(h); pane.setMaxHeight(h);
        if (imagePath != null && !imagePath.isBlank()) {
            try {
                File f = new File(imagePath);
                Image img = f.exists()
                        ? new Image(f.toURI().toString(), w, h, true, true, true)
                        : new Image(imagePath, w, h, true, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(w); iv.setFitHeight(h); iv.setPreserveRatio(false);
                pane.getChildren().add(iv);
                pane.setStyle("-fx-background-color:#e5e7eb;");
                return pane;
            } catch (Exception ignored) {}
        }
        pane.setStyle("-fx-background-color:linear-gradient(to bottom right,#60a5fa,#a78bfa);");
        Label lbl = new Label(fallback != null && !fallback.isBlank()
                ? fallback.substring(0, Math.min(fallback.length(), 2)).toUpperCase() : "EV");
        lbl.setStyle("-fx-font-size:32px; -fx-font-weight:900; -fx-text-fill:white;");
        pane.getChildren().add(lbl);
        return pane;
    }

    private Node emptyState(String msg) {
        Label l = new Label(msg);
        l.setStyle("-fx-font-size:15px; -fx-text-fill:#9ca3af; -fx-font-style:italic;");
        VBox b = new VBox(l); b.setAlignment(Pos.CENTER); b.setPadding(new Insets(30));
        return b;
    }

    private String buildWeatherAdvice(String cond, String category) {
        if (cond == null || "Unknown".equals(cond)) return "";
        return switch (cond) {
            case "Rain", "Drizzle", "Thunderstorm" -> "⚠️ Pluie prévue — préférez un lieu couvert.";
            case "Snow"   -> "❄️ Neige prévue — déplacements difficiles.";
            case "Clear"  -> "☀️ Beau temps — idéal pour un événement en plein air.";
            case "Clouds" -> "⛅ Ciel nuageux — conditions acceptables.";
            default -> "";
        };
    }

    private void showError(String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            a.setHeaderText(null); a.showAndWait();
        });
    }

    private void showInfo(String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
            a.setHeaderText(null); a.showAndWait();
        });
    }
}
