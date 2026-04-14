package com.mindcare.components;

import com.mindcare.model.User;
import com.mindcare.utils.NavigationManager;
import com.mindcare.utils.SessionManager;
import com.mindcare.view.admin.AdminDashboardView;
import com.mindcare.view.auth.LoginView;
import com.mindcare.view.client.*;
import com.mindcare.view.psychologue.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

/**
 * SidebarComponent â€“ left navigation panel.
 * Uses navigateContent() so only the content area is swapped on navigation â€”
 * the sidebar itself is NEVER rebuilt, so active state is always correct.
 */
public class SidebarComponent extends VBox {

    private final List<Button> menuItems = new ArrayList<>();
    private Button activeButton;

    public SidebarComponent() {
        getStyleClass().add("sidebar");
        setMinWidth(240); setMaxWidth(240);
        buildSidebar();
    }

    private void buildSidebar() {
        VBox logoArea = buildLogoArea();

        VBox menu = new VBox(2);
        menu.setPadding(new Insets(10, 10, 10, 10));
        buildMenuItems(menu);

        ScrollPane scroll = new ScrollPane(menu);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox userArea = buildUserArea();

        getChildren().addAll(logoArea, scroll, userArea);
    }

    // â”€â”€ Logo â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private VBox buildLogoArea() {
        VBox box = new VBox(4);
        box.getStyleClass().add("sidebar-logo-area");
        box.setAlignment(Pos.CENTER_LEFT);

        Label brand = new Label("MINDCARE");
        brand.getStyleClass().add("sidebar-brand-text");
        Label sub = new Label("CODEVEINS PLATFORM");
        sub.getStyleClass().add("sidebar-brand-sub");
        box.getChildren().addAll(brand, sub);
        return box;
    }

    // â”€â”€ Menu Items â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void buildMenuItems(VBox menu) {
        User.Role role = SessionManager.getInstance().getRole();

        if (role == User.Role.CLIENT) {
            addSection(menu, "MAIN");
            addItem(menu, "Dashboard",        Feather.HOME,           "Dashboard",        () -> new ClientDashboardView().build());
            addItem(menu, "Gestion Commentaires", Feather.BRIEFCASE,      "Gestion Commentaires", () -> new ServiceRequestListView().build());
            addItem(menu, "Offers Received",  Feather.INBOX,          "Offers Received",  () -> new OffersReceivedView().build());
            addItem(menu, "Prenez un rendez-vous",        Feather.FILE_TEXT,      "Prenez un rendez-vous",        () -> new ContractsView().build());
            addSection(menu, "COMMUNICATION");
            addItem(menu, "Messages",         Feather.MESSAGE_CIRCLE, "Messages",         () -> new MessagingView().build());
            addItem(menu, "Gestion Forum - Sujets",  Feather.HELP_CIRCLE,    "Gestion Forum - Sujets",  () -> new TicketsView().build());
            addSection(menu, "ACCOUNT");
            addItem(menu, "Profile",          Feather.USER,           "Profile",          () -> new ClientProfileView().build());

        } else if (role == User.Role.PSYCHOLOGUE) {
            addSection(menu, "MAIN");
            addItem(menu, "Dashboard",        Feather.HOME,           "Dashboard",        () -> new PsychologueDashboardView().build());
            addItem(menu, "Browse Requests",  Feather.SEARCH,         "Browse Requests",  () -> new BrowseRequestsView().build());
            addItem(menu, "My Offers",        Feather.SEND,           "My Offers",        () -> new MyOffersView().build());
            addItem(menu, "Gestion rendez-vous",        Feather.FILE_TEXT,      "Gestion rendez-vous",        () -> new com.mindcare.view.psychologue.GestionRendezVousView().build());
            addSection(menu, "COMMUNICATION");
            addItem(menu, "Messages",         Feather.MESSAGE_CIRCLE, "Messages",         () -> new PsychologueMessagingView().build());
            addItem(menu, "Gestion Forum - Sujets",  Feather.HELP_CIRCLE,    "Gestion Forum - Sujets",  () -> new PsychologueTicketsView().build());
            addSection(menu, "CREDENTIALS");
            addItem(menu, "Gestion Forum - Messages",     Feather.AWARD,          "Gestion Forum - Messages",     () -> new CertificatesView().build());
            addSection(menu, "ACCOUNT");
            addItem(menu, "Profile",          Feather.USER,           "Profile",          () -> new PsychologueProfileView().build());

        } else if (role == User.Role.ADMIN || role == User.Role.SUPER_ADMIN) {
            addSection(menu, "OVERVIEW");
            addItem(menu, "Dashboard",        Feather.BAR_CHART_2,    "Dashboard",        () -> new AdminDashboardView().build());
            addSection(menu, "MANAGEMENT");
            addItem(menu, "Gestion User",            Feather.USERS,          "Gestion User",            () -> new com.mindcare.view.admin.GestionUserView().build());
            addItem(menu, "Gestion Resources",       Feather.TAG,            "Gestion Resources",       () -> new com.mindcare.view.admin.GestionResourcesView().build());
            addItem(menu, "Gestion Commentaires", Feather.BRIEFCASE,      "Gestion Commentaires", () -> new com.mindcare.view.admin.GestionCommentairesView().build());
            addItem(menu, "Gestion Events",           Feather.INBOX,          "Gestion Events",           () -> new com.mindcare.view.admin.GestionEventsView().build());
            addItem(menu, "Gestion rendez-vous",        Feather.FILE_TEXT,      "Gestion rendez-vous",        () -> new com.mindcare.view.admin.GestionReservationsView().build());
            addSection(menu, "SUPPORT");
            addItem(menu, "Gestion Forum - Sujets",          Feather.HELP_CIRCLE,    "Gestion Forum - Sujets",          () -> new com.mindcare.view.admin.GestionForumSujetsView().build());
            addItem(menu, "Gestion Forum - Messages",     Feather.AWARD,          "Gestion Forum - Messages",     () -> new com.mindcare.view.admin.GestionForumMessagesView().build());
            addSection(menu, "SYSTEM");
            addItem(menu, "Dossiers Étudiants",    Feather.ACTIVITY,       "Dossiers Étudiants",    () -> new com.mindcare.view.admin.DossiersEtudiantsView().build());
        }

        // Activate first item by default
        if (!menuItems.isEmpty()) setActive(menuItems.get(0));
    }

    private void addSection(VBox menu, String label) {
        Label section = new Label(label);
        section.getStyleClass().add("sidebar-section-label");
        menu.getChildren().add(section);
    }

    /**
     * Adds a nav item. On click: marks this button active, then calls
     * navigateContent() to swap ONLY the content area â€” sidebar stays alive.
     */
    private void addItem(VBox menu, String text, Feather icon, String pageTitle,
                         java.util.function.Supplier<javafx.scene.Node> contentSupplier) {
        FontIcon fontIcon = FontIcon.of(icon, 16);
        fontIcon.getStyleClass().add("sidebar-item-icon");

        Label label = new Label(text);
        label.getStyleClass().add("sidebar-item-label");

        HBox row = new HBox(10, fontIcon, label);
        row.setAlignment(Pos.CENTER_LEFT);

        Button btn = new Button();
        btn.setGraphic(row);
        btn.getStyleClass().add("sidebar-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(42);
        btn.setAlignment(Pos.CENTER_LEFT);

        btn.setOnAction(e -> {
            setActive(btn);
            // KEY FIX: navigateContent swaps only the content area,
            // so this SidebarComponent is never recreated.
            NavigationManager.getInstance().navigateContent(pageTitle, contentSupplier::get);
        });

        menuItems.add(btn);
        menu.getChildren().add(btn);
    }

    private void setActive(Button btn) {
        if (activeButton != null) activeButton.getStyleClass().remove("active");
        activeButton = btn;
        btn.getStyleClass().add("active");
    }

    // â”€â”€ User Area â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private VBox buildUserArea() {
        VBox box = new VBox(6);
        box.getStyleClass().add("sidebar-user-area");
        box.setPadding(new Insets(12, 12, 14, 12));

        User user = SessionManager.getInstance().getCurrentUser();
        String name     = user != null ? user.getFullName()    : "User";
        String role     = user != null ? formatRole(user.getRole()) : "";
        String initials = user != null
            ? (user.getFirstName().substring(0, 1) + user.getLastName().substring(0, 1)).toUpperCase()
            : "U";

        // Avatar circle
        Label avatar = new Label(initials);
        avatar.setStyle(
            "-fx-background-color: rgba(13,110,253,0.15);" +
            "-fx-background-radius: 22;" +
            "-fx-border-color: rgba(13,110,253,0.35);" +
            "-fx-border-radius: 22; -fx-border-width: 1;" +
            "-fx-min-width: 40; -fx-min-height: 40;" +
            "-fx-max-width: 40; -fx-max-height: 40;" +
            "-fx-alignment: center;" +
            "-fx-text-fill: #0D6EFD;" +
            "-fx-font-weight: bold; -fx-font-size: 14px;"
        );

        // Name + role badge
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: #1F2A44; -fx-font-weight: bold; -fx-font-size: 12px;");
        nameLabel.setWrapText(false);

        Label roleLabel = new Label(role);
        roleLabel.setStyle(
            "-fx-text-fill: #0D6EFD; -fx-font-size: 10px; -fx-font-weight: bold;" +
            "-fx-background-color: rgba(13,110,253,0.1); -fx-background-radius: 4;" +
            "-fx-padding: 1 6 1 6;"
        );

        VBox textCol = new VBox(3, nameLabel, roleLabel);
        textCol.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        // Sign-out icon button (red tinted)
        FontIcon signOutIcon = FontIcon.of(Feather.LOG_OUT, 15);
        signOutIcon.setStyle("-fx-icon-color: #EF4444;");
        Button logoutBtn = new Button();
        logoutBtn.setGraphic(signOutIcon);
        logoutBtn.setTooltip(new javafx.scene.control.Tooltip("Sign Out"));
        logoutBtn.setStyle(
            "-fx-background-color: rgba(239,68,68,0.08);" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: rgba(239,68,68,0.2);" +
            "-fx-border-radius: 8; -fx-border-width: 1;" +
            "-fx-cursor: hand; -fx-padding: 7 10;"
        );
        logoutBtn.setOnAction(e -> {
            SessionManager.getInstance().logout();
            NavigationManager.getInstance().navigateTo(new LoginView());
        });

        HBox userRow = new HBox(10, avatar, textCol, logoutBtn);
        userRow.setAlignment(Pos.CENTER);
        userRow.setPadding(new Insets(8, 6, 8, 6));
        userRow.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #F7FAFF;" +
            "-fx-border-radius: 10; -fx-border-width: 1;"
        );

        box.getChildren().add(userRow);
        return box;
    }

    private String formatRole(User.Role role) {
        if (role == null) {
            return "";
        }
        return switch (role) {
            case CLIENT -> "ETUDIANT";
            case PSYCHOLOGUE -> "PSYCHOLOGUE";
            case ADMIN -> "ADMIN";
            case SUPER_ADMIN -> "SUPER ADMIN";
        };
    }
}


