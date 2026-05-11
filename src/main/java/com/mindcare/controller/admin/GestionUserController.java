package com.mindcare.controller.admin;

import com.mindcare.components.BadgeLabel;
import com.mindcare.dao.DataAccessException;
import com.mindcare.dao.UserDAO;
import com.mindcare.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.Optional;

public class GestionUserController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, Void> roleColumn;
    @FXML private TableColumn<User, String> joinedColumn;
    @FXML private TableColumn<User, Void> actionsColumn;

    private final UserDAO userDAO = new UserDAO();
    private final ObservableList<User> users = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        roleFilter.getItems().setAll("All Roles", "ETUDIANT", "Psychologue", "ADMIN");
        roleFilter.setValue("All Roles");
        statusFilter.getItems().setAll("All Statuses", "ACTIVE", "BLOCKED");
        statusFilter.setValue("All Statuses");

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        joinedColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        roleColumn.setCellFactory(column -> createRoleCell());
        actionsColumn.setCellFactory(column -> createActionsCell());

        usersTable.setItems(users);
        loadUsers();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        roleFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void loadUsers() {
        try {
            users.setAll(userDAO.getAllUsers());
            applyFilters();
        } catch (DataAccessException exception) {
            showDatabaseError("Unable to load users.", exception);
        }
    }

    private void applyFilters() {
        String query = safeLower(searchField.getText());
        String selectedRole = roleFilter.getValue();
        String normalizedRole = "ETUDIANT".equalsIgnoreCase(selectedRole) ? "CLIENT" : selectedRole;
        String selectedStatus = statusFilter.getValue();

        ObservableList<User> filteredUsers = users.filtered(user -> {
            boolean matchesQuery = query.isBlank()
                || safeLower(user.getEmail()).contains(query);

            boolean matchesRole = selectedRole == null
                || "All Roles".equals(selectedRole)
                || (user.getRole() != null && user.getRole().name().equalsIgnoreCase(normalizedRole));

            boolean matchesStatus = selectedStatus == null
                || "All Statuses".equals(selectedStatus)
                || (user.getStatus() != null && user.getStatus().name().equalsIgnoreCase(selectedStatus));

            return matchesQuery && matchesRole && matchesStatus;
        });

        usersTable.setItems(filteredUsers);
    }

    private TableCell<User, Void> createRoleCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                User user = getTableRow().getItem();
                User.Role role = user.getRole() != null ? user.getRole() : User.Role.CLIENT;
                BadgeLabel badge = switch (role) {
                    case CLIENT -> new BadgeLabel("ETUDIANT", BadgeLabel.Style.INFO);
                    case PSYCHOLOGUE -> new BadgeLabel("Psychologue", BadgeLabel.Style.SUCCESS);
                    case ADMIN -> new BadgeLabel("ADMIN", BadgeLabel.Style.WARNING);
                    case SUPER_ADMIN -> new BadgeLabel("SUPER ADMIN", BadgeLabel.Style.DANGER);
                };
                setGraphic(badge);
            }
        };
    }

    private TableCell<User, Void> createActionsCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                User rowUser = getTableRow().getItem();
                Button viewBtn = new Button("View");
                viewBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
                viewBtn.setOnAction(event -> showUserDetails(rowUser));

                User.Status rowStatus = rowUser.getStatus() != null ? rowUser.getStatus() : User.Status.ACTIVE;
                boolean blocked = rowStatus == User.Status.BLOCKED;
                Button blockBtn = new Button(blocked ? "Unblock" : "Block");
                blockBtn.getStyleClass().addAll("btn", blocked ? "btn-secondary" : "btn-danger", "btn-sm");
                blockBtn.setOnAction(event -> toggleBlockStatus(rowUser));

                Button deleteBtn = new Button("Delete");
                deleteBtn.getStyleClass().addAll("btn", "btn-ghost", "btn-sm");
                deleteBtn.setOnAction(event -> deleteUser(rowUser));

                setGraphic(new HBox(6, viewBtn, blockBtn, deleteBtn));
            }
        };
    }

    private void showUserDetails(User user) {
        if (user == null) {
            return;
        }
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("User Details");
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/mindcare/styles/orion-theme.css").toExternalForm()
        );
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        Label title = new Label(user.getFullName());
        title.getStyleClass().add("card-title");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(8);

        grid.addRow(1, detailKey("Email"), detailValue(valueOrDash(user.getEmail())));
        grid.addRow(2, detailKey("Role"), detailValue(valueOrDash(user.getRole() != null ? user.getRole().name() : null)));
        grid.addRow(3, detailKey("Status"), detailValue(valueOrDash(user.getStatus() != null ? user.getStatus().name() : null)));

        VBox content = new VBox(12, title, grid);
        content.setPadding(new Insets(4, 0, 0, 0));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-border-color: #D5E1F4;" +
            "-fx-border-width: 1;"
        );
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/mindcare/styles/orion-theme.css").toExternalForm()
        );

        dialog.showAndWait();
    }

    private void toggleBlockStatus(User user) {
        if (user == null) {
            return;
        }
        User.Status currentStatus = user.getStatus() != null ? user.getStatus() : User.Status.ACTIVE;
        User.Status nextStatus = currentStatus == User.Status.BLOCKED ? User.Status.ACTIVE : User.Status.BLOCKED;
        String action = nextStatus == User.Status.BLOCKED ? "block" : "unblock";
        if (!confirmAction("Confirm Action", "Do you want to " + action + " " + user.getFullName() + "?")) {
            return;
        }
        user.setStatus(nextStatus);
        try {
            if (userDAO.updateUser(user)) {
                loadUsers();
            } else {
                showDatabaseError("Unable to update user status.", null);
            }
        } catch (DataAccessException exception) {
            showDatabaseError("Unable to update user status.", exception);
        }
    }

    private void deleteUser(User user) {
        if (user == null) {
            return;
        }
        if (!confirmAction("Delete User", "Are you sure you want to delete " + user.getFullName() + "?")) {
            return;
        }
        try {
            if (userDAO.deleteUser(user.getId())) {
                users.remove(user);
                applyFilters();
            } else {
                showDatabaseError("Unable to delete user.", null);
            }
        } catch (DataAccessException exception) {
            showDatabaseError("Unable to delete user.", exception);
        }
    }

    private void showDatabaseError(String message, Throwable exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Database Error");
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/mindcare/styles/orion-theme.css").toExternalForm()
        );
        alert.setHeaderText("User operation failed");
        String detail = exception != null && exception.getCause() != null ? exception.getCause().getMessage() : "";
        alert.setContentText(detail.isBlank() ? message : message + "\n\nDetails: " + detail);
        alert.showAndWait();
    }

    private boolean confirmAction(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/mindcare/styles/orion-theme.css").toExternalForm()
        );
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private Label detailKey(String text) {
        Label label = new Label(text + ":");
        label.setStyle("-fx-text-fill: #5B6B84; -fx-font-size: 12px;");
        label.setAlignment(Pos.CENTER_LEFT);
        return label;
    }

    private Label detailValue(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #1F2A44; -fx-font-size: 12px;");
        label.setWrapText(true);
        return label;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}


