package org.example.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.model.User;
import org.example.service.AuthenticationService;
import org.example.service.ServiceException;

/**
 * Login screen shown before the main journaling workspace.
 */
public class LoginView {
    private final Stage stage;
    private final LoginSuccessHandler onLoginSuccess;
    private final AuthenticationService authenticationService = new AuthenticationService();

    private final TextField firstNameField = new TextField();
    private final TextField lastNameField = new TextField();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button loginButton = new Button("Se connecter");
    private final Button registerButton = new Button("Creer un compte");
    private final Button switchModeButton = new Button("Inscription");
    private final Label errorLabel = new Label();
    private VBox firstNameBlock;
    private VBox lastNameBlock;
    private boolean registerMode;

    public LoginView(Stage stage, LoginSuccessHandler onLoginSuccess) {
        this.stage = stage;
        this.onLoginSuccess = onLoginSuccess;
    }

    public Parent build() {
        StackPane root = new StackPane();
        root.setPadding(new Insets(36));
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #F9FBFF, #EDF6FF 55%, #D7E7FF 100%);"
        );

        VBox card = new VBox(18);
        card.setMaxWidth(620);
        card.setPadding(new Insets(34, 36, 34, 36));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 28;" +
                "-fx-border-color: #D7E7FF;" +
                "-fx-border-radius: 28;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(22,61,122,0.14), 34, 0.16, 0, 10);"
        );

        Label titleLabel = new Label("Connexion");
        titleLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: 800; -fx-text-fill: #1C4F96;");

        Label subtitleLabel = new Label("Connecte-toi avec ton email et ton mot de passe.");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6F87A6;");

        VBox form = new VBox(16);
        firstNameBlock = createFieldBlock("Prenom", firstNameField, "Prenom");
        lastNameBlock = createFieldBlock("Nom", lastNameField, "Nom");
        form.getChildren().addAll(
                firstNameBlock,
                lastNameBlock,
                createFieldBlock("Email", emailField, "adresse@email.com"),
                createFieldBlock("Mot de passe", passwordField, "Mot de passe")
        );

        firstNameField.setOnAction(e -> lastNameField.requestFocus());
        lastNameField.setOnAction(e -> emailField.requestFocus());
        emailField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> {
            if (registerMode) {
                handleRegister();
            } else {
                handleLogin();
            }
        });

        configureInputValidation();
        styleInput(firstNameField);
        styleInput(lastNameField);
        styleInput(emailField);
        styleInput(passwordField);

        loginButton.setDefaultButton(true);
        stylePrimaryButton(loginButton);
        loginButton.setOnAction(e -> handleLogin());
        stylePrimaryButton(registerButton);
        registerButton.setOnAction(e -> handleRegister());
        styleSecondaryButton(switchModeButton);
        switchModeButton.setOnAction(e -> setRegisterMode(!registerMode));

        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setStyle(
                "-fx-background-color: #FFF4F4;" +
                "-fx-text-fill: #C63D48;" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: #FFD5D8;" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 1;" +
                "-fx-padding: 12 14 12 14;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: 700;"
        );

        Label footerLabel = new Label("Utilise le compte cree dans la base de donnees.");
        footerLabel.setWrapText(true);
        footerLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #6F87A6;");

        card.getChildren().addAll(titleLabel, subtitleLabel, form, loginButton, registerButton, switchModeButton, errorLabel, footerLabel);
        root.getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);
        setRegisterMode(false);

        return root;
    }

    private VBox createFieldBlock(String labelText, TextField field, String promptText) {
        VBox block = new VBox(8);
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #2B4A73;");
        field.setPromptText(promptText);
        block.getChildren().addAll(label, field);
        return block;
    }

    private void configureInputValidation() {
        firstNameField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 255 ? change : null));
        lastNameField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 255 ? change : null));
        emailField.setTextFormatter(new TextFormatter<String>(change -> {
            String next = change.getControlNewText();
            if (next.length() > 180) {
                return null;
            }
            if (!change.getText().matches("[A-Za-z0-9_@.\\-+]*")) {
                return null;
            }
            return change;
        }));

        passwordField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 255 ? change : null));
    }

    private void styleInput(TextField field) {
        field.setPrefHeight(52);
        field.setStyle(
                "-fx-background-color: #F9FBFF;" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #CFE3FF;" +
                "-fx-border-radius: 18;" +
                "-fx-border-width: 1.2;" +
                "-fx-text-fill: #1C4F96;" +
                "-fx-prompt-text-fill: #9AAEC8;" +
                "-fx-padding: 12 18 12 18;" +
                "-fx-font-size: 15px;"
        );
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                field.setStyle(
                        "-fx-background-color: #FFFFFF;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #163D7A;" +
                        "-fx-border-radius: 18;" +
                        "-fx-border-width: 1.4;" +
                        "-fx-text-fill: #1C4F96;" +
                        "-fx-prompt-text-fill: #9AAEC8;" +
                        "-fx-padding: 12 18 12 18;" +
                        "-fx-font-size: 15px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(22,61,122,0.16), 14, 0.14, 0, 3);"
                );
            } else {
                field.setStyle(
                        "-fx-background-color: #F9FBFF;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #CFE3FF;" +
                        "-fx-border-radius: 18;" +
                        "-fx-border-width: 1.2;" +
                        "-fx-text-fill: #1C4F96;" +
                        "-fx-prompt-text-fill: #9AAEC8;" +
                        "-fx-padding: 12 18 12 18;" +
                        "-fx-font-size: 15px;"
                );
            }
        });
    }

    private void stylePrimaryButton(Button button) {
        button.setStyle(
                "-fx-background-color: linear-gradient(to right, #163D7A, #245EBD);" +
                "-fx-text-fill: #FFFFFF;" +
                "-fx-background-radius: 18;" +
                "-fx-border-radius: 18;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: 800;" +
                "-fx-padding: 14 22 14 22;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(22,61,122,0.24), 18, 0.18, 0, 6);"
        );
    }

    private void styleSecondaryButton(Button button) {
        button.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-text-fill: #1C4F96;" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #CFE3FF;" +
                "-fx-border-radius: 18;" +
                "-fx-border-width: 1.2;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: 800;" +
                "-fx-padding: 12 20 12 20;" +
                "-fx-cursor: hand;"
        );
    }

    private void setRegisterMode(boolean registerMode) {
        this.registerMode = registerMode;
        firstNameBlock.setVisible(registerMode);
        firstNameBlock.setManaged(registerMode);
        lastNameBlock.setVisible(registerMode);
        lastNameBlock.setManaged(registerMode);
        loginButton.setVisible(!registerMode);
        loginButton.setManaged(!registerMode);
        registerButton.setVisible(registerMode);
        registerButton.setManaged(registerMode);
        switchModeButton.setText(registerMode ? "Retour connexion" : "Inscription");
        clearMessage();
    }

    private void handleLogin() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim().toLowerCase();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        try {
            User user = authenticationService.login(email, password);
            onLoginSuccess.onSuccess(stage, user.getId(), user.getDisplayName(), user.isAdmin());
        } catch (ServiceException e) {
            showError(e.getMessage());
        }
    }

    private void handleRegister() {
        String firstName = firstNameField.getText() == null ? "" : firstNameField.getText().trim();
        String lastName = lastNameField.getText() == null ? "" : lastNameField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim().toLowerCase();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        clearMessage();

        try {
            User user = authenticationService.registerStudent(firstName, lastName, email, password);
            onLoginSuccess.onSuccess(stage, user.getId(), user.getDisplayName(), user.isAdmin());
        } catch (ServiceException e) {
            showError(e.getMessage());
        }
    }

    private void clearMessage() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setText("");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        passwordField.clear();
        passwordField.requestFocus();
    }

    @FunctionalInterface
    public interface LoginSuccessHandler {
        void onSuccess(Stage stage, int userId, String displayName, boolean isAdmin);
    }
}
