package com.mindcare.legacy.auth;

import com.mindcare.view.auth.*;

import com.mindcare.dao.DataAccessException;
import com.mindcare.dao.UserDAO;
import com.mindcare.model.User;
import com.mindcare.utils.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * RegisterLegacyContent – new user registration form.
 */
public class RegisterLegacyContent implements NavigationManager.Buildable {

    private final UserDAO userDAO = new UserDAO();

    @Override
    public Node build() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #020617;");

        VBox card = new VBox(20);
        card.getStyleClass().add("auth-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setMaxWidth(480);

        Label logo = new Label("⬡ MINDCARE");
        logo.getStyleClass().add("auth-logo-text");

        Label title = new Label("Create Your Account");
        title.getStyleClass().add("auth-title");

        Label subtitle = new Label("Join the MindCare community");
        subtitle.getStyleClass().add("auth-subtitle");

        VBox titleBox = new VBox(6, logo, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);

        // Name row
        TextField firstField = field("First Name", "Alice");
        TextField lastField  = field("Last Name",  "Martin");
        HBox nameRow = new HBox(12, wrap("First Name", firstField), wrap("Last Name", lastField));
        nameRow.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(nameRow.getChildren().get(1), Priority.ALWAYS);

        TextField emailField = field("Email Address", "you@mindcare.io");
        PasswordField passField = passField("Password", "Minimum 8 characters");
        PasswordField confirmField = passField("Confirm Password", "Repeat your password");

        // Role selector
        Label roleLabel = new Label("I want to join as...");
        roleLabel.getStyleClass().add("form-label");

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("Etudiant – I need services", "Psychologue / Freelancer – I provide services");
        roleBox.setValue("Etudiant – I need services");
        roleBox.getStyleClass().add("combo-box");
        roleBox.setMaxWidth(Double.MAX_VALUE);

        // Error
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        // Submit
        Button submitBtn = new Button("Create Account");
        submitBtn.getStyleClass().addAll("btn", "btn-primary");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setPrefHeight(44);
        submitBtn.setOnAction(e -> {
            String firstName = firstField.getText().trim();
            String lastName = lastField.getText().trim();
            String email = emailField.getText().trim();
            String password = passField.getText();
            String confirm = confirmField.getText();

            if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank() || confirm.isBlank()) {
                showError(errorLabel, "Please complete all required fields.");
                return;
            }
            if (!password.equals(confirm)) {
                showError(errorLabel, "Password and confirmation do not match.");
                return;
            }
            if (password.length() < 6) {
                showError(errorLabel, "Password must be at least 6 characters.");
                return;
            }

            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setUsername(buildUsername(firstName, lastName));
            user.setRole(roleBox.getValue().startsWith("Psychologue") ? User.Role.PSYCHOLOGUE : User.Role.CLIENT);
            user.setStatus(User.Status.ACTIVE);

            try {
                if (userDAO.getUserByEmail(email) != null) {
                    showError(errorLabel, "An account with this email already exists.");
                    return;
                }
                if (userDAO.insertUser(user)) {
                    errorLabel.setText("Registration successful. You can sign in now.");
                    errorLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 12px;");
                    errorLabel.setVisible(true);
                    NavigationManager.getInstance().navigateTo(new LoginView());
                } else {
                    showError(errorLabel, "Registration failed. Please try again.");
                }
            } catch (DataAccessException exception) {
                showError(errorLabel, "Database error: unable to register now.");
            }
        });

        // Back to login
        Button loginBtn = new Button("Already have an account? Sign in");
        loginBtn.getStyleClass().add("auth-link");
        loginBtn.setOnAction(e -> NavigationManager.getInstance().navigateTo(new LoginView()));

        card.getChildren().addAll(
            titleBox, nameRow,
            wrap("Email Address", emailField),
            wrap("Password", passField),
            wrap("Confirm Password", confirmField),
            new VBox(6, roleLabel, roleBox),
            errorLabel, submitBtn, loginBtn
        );

        root.getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);
        return root;
    }

    private TextField field(String label, String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("text-field");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private PasswordField passField(String label, String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.getStyleClass().add("password-field");
        pf.setMaxWidth(Double.MAX_VALUE);
        return pf;
    }

    private VBox wrap(String label, Control field) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("form-label");
        VBox box = new VBox(6, lbl, field);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private String buildUsername(String firstName, String lastName) {
        return (firstName + "." + lastName).toLowerCase().replaceAll("\\s+", "");
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");
        label.setVisible(true);
    }
}
