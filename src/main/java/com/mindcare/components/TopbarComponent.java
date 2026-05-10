package com.mindcare.components;

import com.mindcare.utils.SessionManager;
import com.mindcare.model.User;
import com.mindcare.utils.NavigationManager;
import com.mindcare.view.admin.GestionUserView;
import com.mindcare.view.auth.LoginView;
import com.mindcare.view.client.ClientProfileView;
import com.mindcare.view.client.MessagingView;
import com.mindcare.view.psychologue.PsychologueProfileView;
import com.mindcare.view.psychologue.PsychologueMessagingView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * TopbarComponent â€“ clean header with title, search, notification bell, and user chip.
 */
public class TopbarComponent extends HBox {

    private final Label titleLabel;
    private final ContextMenu profileMenu = new ContextMenu();

    public TopbarComponent(String title) {
        this.titleLabel = new Label(title);
        getStyleClass().add("topbar");
        setAlignment(Pos.CENTER);
        setSpacing(12);
        profileMenu.getStyleClass().add("profile-menu");
        build();
    }

    private void build() {
        // Page title
        titleLabel.getStyleClass().add("topbar-title");

        // Push right group to the end
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Search field
        TextField search = new TextField();
        search.setPromptText("Search...");
        search.getStyleClass().add("topbar-search");
        search.setPrefWidth(200);
        search.setMaxHeight(34);

        // Notification bell with badge
        FontIcon bellIcon = FontIcon.of(Feather.BELL, 18);
        bellIcon.setStyle("-fx-icon-color: #5B6B84;");

        Label notifBadge = new Label("3");
        notifBadge.setStyle(
            "-fx-background-color: #EF4444;" +
            "-fx-background-radius: 10;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 9px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 1 4 1 4;" +
            "-fx-min-width: 16;" +
            "-fx-min-height: 16;" +
            "-fx-alignment: center;"
        );

        StackPane bellStack = new StackPane(bellIcon, notifBadge);
        StackPane.setAlignment(notifBadge, Pos.TOP_RIGHT);
        notifBadge.setTranslateX(6);
        notifBadge.setTranslateY(-6);
        bellStack.setStyle("-fx-cursor: hand;");
        bellStack.setPadding(new Insets(4, 6, 4, 6));
        bellStack.setOnMouseClicked(e -> openNotifications());

        // Separator between bell and user chip
        Region sep = new Region();
        sep.setMinWidth(1); sep.setMaxWidth(1);
        sep.setPrefHeight(28);
        sep.setStyle("-fx-background-color: #D5E1F4;");

        // User chip (avatar + name + role)
        HBox userChip = buildUserChip();

        // Group right-side elements together with consistent spacing
        HBox rightGroup = new HBox(14, search, bellStack, sep, userChip);
        rightGroup.setAlignment(Pos.CENTER);

        getChildren().addAll(titleLabel, spacer, rightGroup);
        setPadding(new Insets(0, 20, 0, 24));
    }

    private HBox buildUserChip() {
        User user = SessionManager.getInstance().getCurrentUser();

        String initials = "U";
        String name     = "User";
        String role     = "";

        if (user != null) {
            initials = (String.valueOf(user.getFirstName().charAt(0))
                      + String.valueOf(user.getLastName().charAt(0))).toUpperCase();
            name     = user.getFullName();
            role     = formatRole(user.getRole());
        }

        // Avatar circle
        Label avatar = new Label(initials);
        avatar.setStyle(
            "-fx-background-color: rgba(13,110,253,0.15);" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(13,110,253,0.35);" +
            "-fx-border-radius: 20;" +
            "-fx-border-width: 1;" +
            "-fx-min-width: 36; -fx-min-height: 36;" +
            "-fx-max-width: 36; -fx-max-height: 36;" +
            "-fx-alignment: center;" +
            "-fx-text-fill: #0D6EFD;" +
            "-fx-font-weight: bold; -fx-font-size: 13px;"
        );

        // Name + role stacked
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: #1F2A44; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label roleLabel = new Label(role);
        roleLabel.setStyle("-fx-text-fill: #7A8CA8; -fx-font-size: 10px;");

        VBox textCol = new VBox(1, nameLabel, roleLabel);
        textCol.setAlignment(Pos.CENTER_LEFT);

        // Dropdown caret icon
        FontIcon caret = FontIcon.of(Feather.CHEVRON_DOWN, 12);
        caret.setStyle("-fx-icon-color: #7A8CA8;");

        HBox chip = new HBox(10, avatar, textCol, caret);
        chip.setAlignment(Pos.CENTER);
        chip.setPadding(new Insets(6, 12, 6, 10));
        chip.setStyle(
            "-fx-cursor: hand;" +
            "-fx-background-color: #F7FAFF;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #D5E1F4;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;"
        );
        chip.setOnMouseClicked(e -> showProfileMenu(chip));
        return chip;
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    private void openNotifications() {
        NavigationManager nav = NavigationManager.getInstance();
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null || user.getRole() == null) {
            return;
        }
        switch (user.getRole()) {
            case CLIENT -> nav.navigateContent("Messages", () -> new MessagingView().build());
            case PSYCHOLOGUE -> nav.navigateContent("Messages", () -> new PsychologueMessagingView().build());
            case ADMIN, SUPER_ADMIN -> nav.navigateContent("Gestion User", () -> new GestionUserView().build());
        }
    }

    private void openProfile() {
        NavigationManager nav = NavigationManager.getInstance();
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null || user.getRole() == null) {
            return;
        }
        switch (user.getRole()) {
            case CLIENT -> nav.navigateContent("Profile", () -> new ClientProfileView().build());
            case PSYCHOLOGUE -> nav.navigateContent("Profile", () -> new PsychologueProfileView().build());
            case ADMIN, SUPER_ADMIN -> nav.navigateContent("Profile", () -> new ClientProfileView().build());
        }
    }

    private void showProfileMenu(HBox chip) {
        profileMenu.hide();
        profileMenu.getItems().setAll(buildProfileMenuItems());
        profileMenu.setOnShown(event -> keepMenuInsideVisibleBounds(chip));
        profileMenu.show(chip, Side.BOTTOM, 0, 6);
    }

    private MenuItem[] buildProfileMenuItems() {
        User user = SessionManager.getInstance().getCurrentUser();
        String userName = user != null ? user.getFullName() : "User";
        String roleName = user != null && user.getRole() != null ? formatRole(user.getRole()) : "UNKNOWN";
        Label headerLabel = new Label(userName + " - " + roleName);
        headerLabel.getStyleClass().add("profile-menu-header-label");
        MenuItem headerItem = new MenuItem();
        headerItem.setGraphic(headerLabel);
        headerItem.getStyleClass().add("profile-menu-header");
        headerItem.setDisable(true);

        MenuItem profileItem = new MenuItem("Open Profile");
        profileItem.setGraphic(FontIcon.of(Feather.USER, 14));
        profileItem.getStyleClass().add("profile-menu-item");
        profileItem.setOnAction(e -> openProfile());

        MenuItem notificationsItem = new MenuItem("Open Notifications");
        notificationsItem.setGraphic(FontIcon.of(Feather.BELL, 14));
        notificationsItem.getStyleClass().add("profile-menu-item");
        notificationsItem.setOnAction(e -> openNotifications());

        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setGraphic(FontIcon.of(Feather.LOG_OUT, 14));
        logoutItem.getStyleClass().addAll("profile-menu-item", "profile-menu-danger");
        logoutItem.setOnAction(e -> {
            SessionManager.getInstance().logout();
            NavigationManager.getInstance().navigateTo(new LoginView());
        });

        return new MenuItem[]{headerItem, new SeparatorMenuItem(), profileItem, notificationsItem, new SeparatorMenuItem(), logoutItem};
    }

    private void keepMenuInsideVisibleBounds(HBox chip) {
        if (chip.getScene() == null || chip.getScene().getWindow() == null) {
            return;
        }
        Screen screen = Screen.getScreensForRectangle(
            chip.getScene().getWindow().getX(),
            chip.getScene().getWindow().getY(),
            chip.getScene().getWindow().getWidth(),
            chip.getScene().getWindow().getHeight()
        ).stream().findFirst().orElse(Screen.getPrimary());

        double minX = screen.getVisualBounds().getMinX() + 8;
        double maxX = screen.getVisualBounds().getMaxX() - 8;
        double minY = screen.getVisualBounds().getMinY() + 8;
        double maxY = screen.getVisualBounds().getMaxY() - 8;

        double targetX = profileMenu.getX();
        double targetY = profileMenu.getY();
        double menuWidth = profileMenu.getWidth();
        double menuHeight = profileMenu.getHeight();

        if (targetX + menuWidth > maxX) {
            targetX = maxX - menuWidth;
        }
        if (targetX < minX) {
            targetX = minX;
        }
        if (targetY + menuHeight > maxY) {
            targetY = maxY - menuHeight;
        }
        if (targetY < minY) {
            targetY = minY;
        }

        profileMenu.setX(targetX);
        profileMenu.setY(targetY);
    }

    private String formatRole(User.Role role) {
        if (role == null) {
            return "UNKNOWN";
        }
        return switch (role) {
            case CLIENT -> "ETUDIANT";
            case PSYCHOLOGUE -> "PSYCHOLOGUE";
            case ADMIN -> "ADMIN";
            case SUPER_ADMIN -> "SUPER ADMIN";
        };
    }
}


