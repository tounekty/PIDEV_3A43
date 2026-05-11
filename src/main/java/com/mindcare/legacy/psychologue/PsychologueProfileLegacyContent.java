package com.mindcare.legacy.psychologue;

import com.mindcare.view.psychologue.*;

import com.mindcare.dao.DataAccessException;
import com.mindcare.dao.UserDAO;
import com.mindcare.view.client.ClientProfileView;
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
 * PsychologueProfileLegacyContent â€“ worker profile with skills and bio.
 */
public class PsychologueProfileLegacyContent implements NavigationManager.Buildable {

    private final UserDAO userDAO = new UserDAO();

    @Override
    public Node build() {
        return buildContent();
    }

    private ScrollPane buildContent() {
        User user = SessionManager.getInstance().getCurrentUser();
        VBox content = new VBox(24);

        Label title = new Label("My Profile");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Manage your freelancer profile and skills");
        sub.getStyleClass().add("page-subtitle");

        content.getChildren().addAll(new VBox(6, title, sub), buildPersonalCard(user), buildSkillsCard(), buildSecurityCard());
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox buildPersonalCard(User user) {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        String avatarLetter = "W";
        if (user != null && user.getFirstName() != null && !user.getFirstName().isBlank()) {
            avatarLetter = String.valueOf(user.getFirstName().charAt(0));
        }
        Label avatar = new Label(avatarLetter);
        avatar.setStyle("-fx-background-color: rgba(15,175,122,0.2); -fx-background-radius: 40; " +
            "-fx-min-width: 72; -fx-min-height: 72; -fx-max-width: 72; -fx-max-height: 72; " +
            "-fx-alignment: center; -fx-text-fill: #0FAF7A; -fx-font-weight: bold; -fx-font-size: 28px;");

        String name  = user != null ? user.getFullName() : "Psychologue";
        Label nameLabel = new Label(name); nameLabel.getStyleClass().add("card-title");
        Label roleLabel = new Label("Psychologue"); roleLabel.getStyleClass().addAll("badge", "badge-success");

        HBox avatarRow = new HBox(16, avatar, new VBox(6, nameLabel, roleLabel));
        avatarRow.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(avatarRow);

        card.getChildren().add(new Label("Personal Information") {{ getStyleClass().add("section-title"); }});

        TextField firstNameField = profileField(user != null ? user.getFirstName() : "");
        TextField lastNameField = profileField(user != null ? user.getLastName() : "");
        TextField emailField = profileField(user != null ? user.getEmail() : "");
        TextField phoneField = profileField(user != null && user.getPhone() != null ? user.getPhone() : "");
        TextField locationField = profileField(user != null && user.getLocation() != null ? user.getLocation() : "");

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

        Button save = new Button("Save Changes");
        save.getStyleClass().addAll("btn", "btn-primary");
        save.setOnAction(e -> saveProfileChanges(user, firstNameField, lastNameField, emailField, phoneField, locationField));
        card.getChildren().add(save);
        return card;
    }

    private VBox buildSkillsCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        Label title = new Label("Skills & Biography");
        title.getStyleClass().add("section-title");
        card.getChildren().add(title);

        Label bioLabel = new Label("Bio / Introduction");
        bioLabel.getStyleClass().add("form-label");
        TextArea bioArea = new TextArea("Experienced full-stack developer with 5+ years building enterprise applications...");
        bioArea.getStyleClass().add("text-area");
        bioArea.setPrefRowCount(4);
        bioArea.setMaxWidth(Double.MAX_VALUE);

        Label skillsLabel = new Label("Skills (comma separated)");
        skillsLabel.getStyleClass().add("form-label");
        TextField skillsField = new TextField("Java, React, Node.js, AWS, PostgreSQL");
        skillsField.getStyleClass().add("text-field");
        skillsField.setMaxWidth(Double.MAX_VALUE);

        Label hourlyLabel = new Label("Hourly Rate (USD)");
        hourlyLabel.getStyleClass().add("form-label");
        TextField hourlyField = new TextField("75");
        hourlyField.getStyleClass().add("text-field");
        hourlyField.setMaxWidth(200);

        Button save = new Button("Update Profile");
        save.getStyleClass().addAll("btn", "btn-primary");

        card.getChildren().addAll(
            new VBox(6, bioLabel, bioArea),
            new VBox(6, skillsLabel, skillsField),
            new VBox(6, hourlyLabel, hourlyField),
            save
        );
        return card;
    }

    private VBox buildSecurityCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        Label title = new Label("Security"); title.getStyleClass().add("section-title");
        Label lbl1  = new Label("Current Password"); lbl1.getStyleClass().add("form-label");
        PasswordField p1 = new PasswordField(); p1.getStyleClass().add("password-field"); p1.setMaxWidth(Double.MAX_VALUE);
        Label lbl2  = new Label("New Password"); lbl2.getStyleClass().add("form-label");
        PasswordField p2 = new PasswordField(); p2.getStyleClass().add("password-field"); p2.setMaxWidth(Double.MAX_VALUE);
        Button save = new Button("Update Password"); save.getStyleClass().addAll("btn", "btn-secondary");
        card.getChildren().addAll(title, new VBox(6, lbl1, p1), new VBox(6, lbl2, p2), save);
        return card;
    }

    private TextField profileField(String value) {
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

