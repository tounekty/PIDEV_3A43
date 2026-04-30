package org.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.auth.AppUser;
import org.example.auth.AuthService;
import org.example.config.DatabaseConnection;
import org.example.event.EventEngagementService;
import org.example.event.EventService;
import org.example.reservation.ReservationService;

import java.sql.SQLException;

public class Main extends Application {

    private static final String ROOT      = "-fx-background-color: linear-gradient(to bottom right,#f0f4ff,#e8f0fe);";
    private static final String CARD      = "-fx-background-color: white; -fx-background-radius: 24; -fx-border-radius: 24; -fx-border-color: rgba(120,169,230,0.20); -fx-effect: dropshadow(gaussian, rgba(46,94,166,0.14), 28, 0.18, 0, 8);";
    private static final String PRIMARY   = "-fx-background-color: linear-gradient(to right,#0f69ff,#38a4ff); -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 14; -fx-padding: 13 0 13 0; -fx-font-size: 14px;";
    private static final String SECONDARY = "-fx-background-color: transparent; -fx-text-fill: #0f69ff; -fx-font-weight: 700; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0; -fx-border-color: transparent;";
    private static final String INPUT     = "-fx-background-color: #f5f8ff; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #d7e7ff; -fx-padding: 12 14; -fx-font-size: 13px;";
    private static final String INPUT_FOCUS = "-fx-background-color: #f0f6ff; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #0f69ff; -fx-border-width: 2; -fx-padding: 11 13; -fx-font-size: 13px;";
    private static final String LABEL     = "-fx-text-fill: #29496f; -fx-font-weight: 700; -fx-font-size: 13px;";

    // Shared stage content container
    private StackPane root;
    private Stage primaryStage;
    private final AuthService authService = new AuthService();

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        initializeDatabase();

        root = new StackPane();
        root.setStyle(ROOT);
        root.setPadding(new Insets(40));

        showLoginCard();

        Scene scene = new Scene(root, 700, 560);
        stage.setScene(scene);
        stage.setTitle("MindCare Events — Connexion");
        stage.setResizable(false);
        stage.show();
    }

    // ── Login card ────────────────────────────────────────────────────────────
    private void showLoginCard() {
        // Logo
        Label logo = new Label("MindCare Events");
        logo.setStyle("-fx-text-fill: #0f69ff; -fx-font-size: 30px; -fx-font-weight: 900;");
        Label subtitle = new Label("Plateforme de gestion des événements étudiants");
        subtitle.setStyle("-fx-text-fill: #637a97; -fx-font-size: 13px;");

        // Fields
        TextField usernameField = styledField("Nom d'utilisateur");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");
        passwordField.setStyle(INPUT);
        addFocusStyle(usernameField);
        addFocusStyle(passwordField);

        // Form
        VBox form = new VBox(14,
                fieldRow("Utilisateur", usernameField),
                fieldRow("Mot de passe", passwordField)
        );

        // Login button
        Button loginBtn = new Button("Se connecter");
        loginBtn.setStyle(PRIMARY);
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setOnAction(e -> attemptLogin(usernameField, passwordField));
        passwordField.setOnAction(e -> attemptLogin(usernameField, passwordField));

        // Separator
        Label orLabel = new Label("— ou —");
        orLabel.setStyle("-fx-text-fill: #b0bec5; -fx-font-size: 12px;");
        orLabel.setMaxWidth(Double.MAX_VALUE);
        orLabel.setAlignment(Pos.CENTER);

        // Register link
        Button registerLink = new Button("Créer un compte étudiant");
        registerLink.setStyle(SECONDARY);
        registerLink.setMaxWidth(Double.MAX_VALUE);
        registerLink.setAlignment(Pos.CENTER);
        registerLink.setOnAction(e -> showRegisterCard());

        // Hint
        Label hint = new Label("Comptes démo : admin / admin123   |   etudiant / etud123");
        hint.setStyle("-fx-text-fill: #b0bec5; -fx-font-size: 11px;");
        hint.setWrapText(true);

        VBox card = new VBox(20, logo, subtitle, form, loginBtn, orLabel, registerLink, hint);
        card.setPadding(new Insets(40, 44, 36, 44));
        card.setStyle(CARD);
        card.setMaxWidth(480);
        card.setAlignment(Pos.TOP_LEFT);

        StackPane.setAlignment(card, Pos.CENTER);
        root.getChildren().setAll(card);
    }

    private void attemptLogin(TextField usernameField, PasswordField passwordField) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // ── Contrôle de saisie ────────────────────────────────────────────────
        if (username.isBlank() || password.isBlank()) {
            showWarning("Veuillez remplir tous les champs.");
            return;
        }
        if (username.length() > 50) {
            showWarning("Le nom d'utilisateur ne peut pas dépasser 50 caractères.");
            return;
        }
        if (password.length() > 100) {
            showWarning("Le mot de passe ne peut pas dépasser 100 caractères.");
            return;
        }
        // ─────────────────────────────────────────────────────────────────────
        try {
            AppUser user = authService.login(username, password);
            if (user == null) {
                showError("Identifiants invalides. Vérifiez votre nom d'utilisateur et mot de passe.");
                return;
            }
            primaryStage.close();
            if (user.isAdmin()) {
                new AdminApp(user).show();
            } else {
                new StudentApp(user).show();
            }
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    // ── Register card ─────────────────────────────────────────────────────────
    private void showRegisterCard() {
        // Logo
        Label logo = new Label("Créer un compte");
        logo.setStyle("-fx-text-fill: #0f69ff; -fx-font-size: 26px; -fx-font-weight: 900;");
        Label subtitle = new Label("Rejoignez MindCare Events en tant qu'étudiant");
        subtitle.setStyle("-fx-text-fill: #637a97; -fx-font-size: 13px;");

        // Fields
        TextField usernameField  = styledField("Choisissez un nom d'utilisateur");
        PasswordField passField  = new PasswordField();
        passField.setPromptText("Choisissez un mot de passe (min. 4 caractères)");
        passField.setStyle(INPUT);
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirmez le mot de passe");
        confirmField.setStyle(INPUT);

        addFocusStyle(usernameField);
        addFocusStyle(passField);
        addFocusStyle(confirmField);

        // Form
        VBox form = new VBox(14,
                fieldRow("Utilisateur",  usernameField),
                fieldRow("Mot de passe", passField),
                fieldRow("Confirmer",    confirmField)
        );

        // Register button
        Button registerBtn = new Button("Créer mon compte");
        registerBtn.setStyle(PRIMARY);
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setOnAction(e -> attemptRegister(usernameField, passField, confirmField));

        // Back to login link
        Button backLink = new Button("← Retour à la connexion");
        backLink.setStyle(SECONDARY);
        backLink.setMaxWidth(Double.MAX_VALUE);
        backLink.setOnAction(e -> showLoginCard());

        // Info note
        Label note = new Label("ℹ  Les nouveaux comptes ont le rôle Étudiant. Contactez un admin pour un accès admin.");
        note.setStyle("-fx-text-fill: #9ab0cc; -fx-font-size: 11px;");
        note.setWrapText(true);

        VBox card = new VBox(20, logo, subtitle, form, registerBtn, backLink, note);
        card.setPadding(new Insets(40, 44, 36, 44));
        card.setStyle(CARD);
        card.setMaxWidth(480);
        card.setAlignment(Pos.TOP_LEFT);

        StackPane.setAlignment(card, Pos.CENTER);
        root.getChildren().setAll(card);
    }

    private void attemptRegister(TextField usernameField, PasswordField passField, PasswordField confirmField) {
        String username = usernameField.getText().trim();
        String password = passField.getText();
        String confirm  = confirmField.getText();

        // ── Contrôle de saisie ────────────────────────────────────────────────
        if (username.isBlank() || password.isBlank() || confirm.isBlank()) {
            showWarning("Veuillez remplir tous les champs.");
            return;
        }
        try {
            org.example.util.ValidationUtil.validateUsername(username);
        } catch (IllegalArgumentException ex) {
            showWarning(ex.getMessage());
            return;
        }
        try {
            org.example.util.ValidationUtil.validatePassword(password);
        } catch (IllegalArgumentException ex) {
            showWarning(ex.getMessage());
            return;
        }
        if (!password.equals(confirm)) {
            showWarning("Les mots de passe ne correspondent pas.");
            return;
        }
        // ─────────────────────────────────────────────────────────────────────
        try {
            AppUser user = authService.register(username, password);
            showInfo("Compte créé avec succès ! Bienvenue, " + user.getUsername() + " !");
            primaryStage.close();
            new StudentApp(user).show();
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    // ── DB init ───────────────────────────────────────────────────────────────
    private void initializeDatabase() {
        try {
            DatabaseConnection.getConnection();
            new AuthService().initializeUsers();
            new EventService().createTableIfNotExists();
            new ReservationService().initializeReservations();
            new EventEngagementService().initializeTables();
            // Start notification scheduler in background
            new org.example.util.NotificationScheduler().start();
            // Start API server for QR ticket validation on port 8080
            new Thread(() -> {
                try { new org.example.api.ApiServer().start(8080); }
                catch (Exception ignored) { /* already running */ }
            }, "api-server").start();
        } catch (SQLException e) {
            showError("Erreur d'initialisation de la base : " + e.getMessage());
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────
    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle(INPUT);
        return tf;
    }

    private void addFocusStyle(Control field) {
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (field instanceof TextField tf) {
                tf.setStyle(isFocused ? INPUT_FOCUS : INPUT);
            } else if (field instanceof PasswordField pf) {
                pf.setStyle(isFocused ? INPUT_FOCUS : INPUT);
            }
        });
    }

    private HBox fieldRow(String labelText, Control field) {
        Label lbl = new Label(labelText);
        lbl.setStyle(LABEL);
        lbl.setMinWidth(110);
        HBox row = new HBox(14, lbl, field);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(field, Priority.ALWAYS);
        return row;
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void showWarning(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
