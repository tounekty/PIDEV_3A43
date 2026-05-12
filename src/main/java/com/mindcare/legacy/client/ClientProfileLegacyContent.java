package com.mindcare.legacy.client;

import com.mindcare.view.client.*;

import com.mindcare.components.BadgeLabel;
import com.mindcare.dao.DataAccessException;
import com.mindcare.dao.UserDAO;
import com.mindcare.components.MainLayout;
import com.mindcare.utils.NavigationManager;
import com.mindcare.utils.SessionManager;
import com.mindcare.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * ClientProfileLegacyContent â€“ editable user profile for clients.
 */
public class ClientProfileLegacyContent implements NavigationManager.Buildable {

    private final UserDAO userDAO = new UserDAO();

    @Override
    public Node build() {
        return buildContent();
    }

    private ScrollPane buildContent() {
        User user = SessionManager.getInstance().getCurrentUser();
        VBox content = new VBox(24);
        content.setPadding(new Insets(0));

        Label title = new Label("My Profile");
        title.getStyleClass().add("page-title");
        Label sub   = new Label("Manage your account information");
        sub.getStyleClass().add("page-subtitle");

        content.getChildren().addAll(new VBox(6, title, sub), buildProfileCard(user), buildSecurityCard());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox buildProfileCard(User user) {
        VBox card = new VBox(20);
        card.getStyleClass().add("card");

        // Avatar + header
        String avatarLetter = "U";
        if (user != null && user.getFirstName() != null && !user.getFirstName().isBlank()) {
            avatarLetter = String.valueOf(user.getFirstName().charAt(0));
        }
        Label avatar = new Label(avatarLetter);
        avatar.setStyle("-fx-background-color: rgba(15,175,122,0.2); -fx-background-radius: 40; " +
            "-fx-min-width: 72; -fx-min-height: 72; -fx-max-width: 72; -fx-max-height: 72; " +
            "-fx-alignment: center; -fx-text-fill: #0FAF7A; -fx-font-weight: bold; -fx-font-size: 28px;");

        String name  = user != null ? user.getFullName() : "User";
        String email = user != null ? user.getEmail() : "";
        String role  = user != null ? user.getRole().name() : "";

        Label nameLabel  = new Label(name);  nameLabel.getStyleClass().add("card-title");
        Label emailLabel = new Label(email); emailLabel.getStyleClass().add("label-secondary");

        VBox nameBox = new VBox(4, nameLabel, emailLabel, new BadgeLabel(role, BadgeLabel.Style.PRIMARY));

        HBox avatarRow = new HBox(16, avatar, nameBox);
        avatarRow.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(avatarRow);

        // Editable fields
        card.getChildren().add(new Label("Personal Information") {{ getStyleClass().add("section-title"); }});

        TextField firstNameField = textField(user != null ? user.getFirstName() : "");
        TextField lastNameField = textField(user != null ? user.getLastName() : "");
        TextField emailField = textField(email);
        TextField phoneField = textField(user != null && user.getPhone() != null ? user.getPhone() : "");
        TextField locationField = textField(user != null && user.getLocation() != null ? user.getLocation() : "");

        HBox nameRow = new HBox(16,
            wrapField("First Name", firstNameField),
            wrapField("Last Name", lastNameField)
        );
        HBox.setHgrow(nameRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(nameRow.getChildren().get(1), Priority.ALWAYS);

        card.getChildren().addAll(
            nameRow,
            wrapField("Email", emailField),
            wrapField("Phone", phoneField),
            wrapField("Location", locationField)
        );

        Button saveBtn = new Button("Save Changes");
        saveBtn.getStyleClass().addAll("btn", "btn-primary");
        saveBtn.setOnAction(e -> saveProfileChanges(user, firstNameField, lastNameField, emailField, phoneField, locationField));
        card.getChildren().add(saveBtn);

        return card;
    }

    private VBox buildSecurityCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        Label title = new Label("Security Settings");
        title.getStyleClass().add("section-title");
        card.getChildren().add(title);

        Label currentLbl = new Label("Current Password"); currentLbl.getStyleClass().add("form-label");
        PasswordField current = new PasswordField(); current.getStyleClass().add("password-field"); current.setMaxWidth(Double.MAX_VALUE);

        Label newLbl = new Label("New Password"); newLbl.getStyleClass().add("form-label");
        PasswordField newPass = new PasswordField(); newPass.getStyleClass().add("password-field"); newPass.setMaxWidth(Double.MAX_VALUE);

        Label confirmLbl = new Label("Confirm New Password"); confirmLbl.getStyleClass().add("form-label");
        PasswordField confirm = new PasswordField(); confirm.getStyleClass().add("password-field"); confirm.setMaxWidth(Double.MAX_VALUE);

        Button updateBtn = new Button("Update Password");
        updateBtn.getStyleClass().addAll("btn", "btn-secondary");

        card.getChildren().addAll(
            new VBox(6, currentLbl, current),
            new VBox(6, newLbl, newPass),
            new VBox(6, confirmLbl, confirm),
            updateBtn
        );
        return card;
    }

    private TextField textField(String value) {
        TextField tf = new TextField(value);
        tf.getStyleClass().add("text-field");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private VBox wrapField(String label, TextField textField) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("form-label");
        return new VBox(6, lbl, textField);
    }

    private void saveProfileChanges(User sessionUser, TextField firstNameField, TextField lastNameField,
                                    TextField emailField, TextField phoneField, TextField locationField) {
        if (sessionUser == null) {
            showInfo("Profile", "No active user session.");
            return;
        }

        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();

        if (firstName.isBlank() || lastName.isBlank() || email.isBlank()) {
            showError("Please fill in first name, last name and email.");
            return;
        }

        sessionUser.setFirstName(firstName);
        sessionUser.setLastName(lastName);
        sessionUser.setEmail(email);
        sessionUser.setPhone(phoneField.getText().trim());
        sessionUser.setLocation(locationField.getText().trim());


        try {
            if (userDAO.updateUser(sessionUser)) {
                SessionManager.getInstance().login(sessionUser);
                showInfo("Profile Updated", "Your profile changes have been saved.");
            } else {
                showError("Unable to save profile changes.");
            }
        } catch (DataAccessException exception) {
            showError("Database error while saving profile.");
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/mindcare/styles/orion-theme.css").toExternalForm()
        );
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Profile Error");
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/mindcare/styles/orion-theme.css").toExternalForm()
        );
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

