package com.mindcare.legacy.auth;

import com.mindcare.view.auth.*;

import com.mindcare.dao.DataAccessException;
import com.mindcare.dao.UserDAO;
import com.mindcare.model.User;
import com.mindcare.service.MockDataService;
import com.mindcare.utils.NavigationManager;
import com.mindcare.utils.SessionManager;
import com.mindcare.view.client.ClientDashboardView;
import com.mindcare.view.admin.AdminDashboardView;
import com.mindcare.view.psychologue.PsychologueDashboardView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * LoginLegacyContent - the entry screen for the MindCare platform.
 */
public class LoginLegacyContent implements NavigationManager.Buildable {

    private final UserDAO userDAO = new UserDAO();

    @Override
    public Node build() {
        StackPane root = new StackPane();
        root.getStyleClass().add("auth-container");
        root.setStyle("-fx-background-color: #020617;");

        VBox bg = new VBox();
        bg.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        bg.setStyle("-fx-background-color: #020617;");
        root.getChildren().add(bg);

        VBox card = buildCard();
        StackPane.setAlignment(card, Pos.CENTER);
        root.getChildren().add(card);

        return root;
    }

    private VBox buildCard() {
        VBox card = new VBox(22);
        card.getStyleClass().add("auth-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setMaxWidth(440);
        card.setPrefWidth(440);

        // Logo – try real image, fallback to styled text
        VBox logoBox = buildLogoBox();

        Label title    = new Label("Welcome Back");
        title.getStyleClass().add("auth-title");
        title.setAlignment(Pos.CENTER);

        Label subtitle = new Label("Sign in to your MindCare account");
        subtitle.getStyleClass().add("auth-subtitle");

        VBox titleBox = new VBox(6, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);

        VBox emailBox    = buildField("Email Address", "you@mindcare.io",  false);
        TextField emailFld = (TextField) emailBox.getChildren().get(1);

        VBox passwordBox = buildField("Password", "••••••••", true);
        PasswordField passFld = (PasswordField) passwordBox.getChildren().get(1);

        Button forgotBtn = new Button("Forgot password?");
        forgotBtn.getStyleClass().add("auth-link");
        forgotBtn.setOnAction(e -> NavigationManager.getInstance().navigateTo(new ForgotPasswordView()));
        HBox forgotRow = new HBox(forgotBtn);
        forgotRow.setAlignment(Pos.CENTER_RIGHT);

        Label errorLbl = new Label("");
        errorLbl.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");
        errorLbl.setVisible(false);

        Button loginBtn = new Button("Sign In →");
        loginBtn.getStyleClass().addAll("btn", "btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(46);
        loginBtn.setOnAction(e -> handleLogin(emailFld.getText(), passFld.getText(), errorLbl));

        Label divText = new Label("  or continue with demo  ");
        divText.getStyleClass().add("label-muted");
        HBox divider = new HBox(divText);
        divider.setAlignment(Pos.CENTER);

        HBox demoRow = buildDemoRow();

        Button registerBtn = new Button("Create an account");
        registerBtn.getStyleClass().add("auth-link");
        registerBtn.setOnAction(e -> NavigationManager.getInstance().navigateTo(new RegisterView()));

        Label regLabel = new Label("Don't have an account? ");
        regLabel.getStyleClass().add("label-secondary");

        HBox registerRow = new HBox(regLabel, registerBtn);
        registerRow.setAlignment(Pos.CENTER);

        card.getChildren().addAll(
            logoBox, titleBox,
            emailBox, passwordBox, forgotRow,
            errorLbl, loginBtn,
            divider, demoRow,
            registerRow
        );
        return card;
    }

    private VBox buildLogoBox() {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);

        Label logoNode = new Label("⬡ MINDCARE");
        logoNode.getStyleClass().add("auth-logo-text");

        Label tagline = new Label("MindCare Desktop Application for Student Mental Health");
        tagline.getStyleClass().add("auth-subtitle");

        box.getChildren().addAll(logoNode, tagline);
        return box;
    }

    private VBox buildField(String label, String prompt, boolean password) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("form-label");
        Control field;
        if (password) {
            PasswordField pf = new PasswordField();
            pf.setPromptText(prompt);
            pf.getStyleClass().add("password-field");
            pf.setMaxWidth(Double.MAX_VALUE);
            field = pf;
        } else {
            TextField tf = new TextField();
            tf.setPromptText(prompt);
            tf.getStyleClass().add("text-field");
            tf.setMaxWidth(Double.MAX_VALUE);
            field = tf;
        }
        return new VBox(6, lbl, field);
    }

    private HBox buildDemoRow() {
        Button clientBtn = demoBtn("Etudiant", Feather.USER,    "#3B82F6", () -> quickLogin(MockDataService.getInstance().getClientUser()));
        Button psychologueBtn = demoBtn("Psychologue", Feather.AWARD,   "#0FAF7A", () -> quickLogin(MockDataService.getInstance().getPsychologueUser()));
        Button adminBtn  = demoBtn("Admin",  Feather.SHIELD,  "#8B5CF6", () -> quickLogin(MockDataService.getInstance().getAdminUser()));
        HBox row = new HBox(10, clientBtn, psychologueBtn, adminBtn);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private Button demoBtn(String text, Feather icon, String color, Runnable action) {
        FontIcon fi = FontIcon.of(icon, 14);
        fi.setStyle("-fx-icon-color: " + color + ";");
        Button btn = new Button(text, fi);
        btn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void quickLogin(User user) {
        SessionManager.getInstance().login(user);
        redirectToRole(user.getRole());
    }

    private void handleLogin(String email, String password, Label errorLabel) {
        if (email.isBlank() || password.isBlank()) {
            errorLabel.setText("Please enter your email and password.");
            errorLabel.setVisible(true);
            return;
        }
        try {
            User user = userDAO.authenticate(email.trim(), password);
            if (user == null) {
                errorLabel.setText("Invalid email or password.");
                errorLabel.setVisible(true);
                return;
            }
            if (user.getStatus() == User.Status.BLOCKED) {
                errorLabel.setText("This account is blocked. Contact support.");
                errorLabel.setVisible(true);
                return;
            }

            errorLabel.setVisible(false);
            SessionManager.getInstance().login(user);
            redirectToRole(user.getRole());
        } catch (DataAccessException exception) {
            errorLabel.setText("Database error: unable to sign in right now.");
            errorLabel.setVisible(true);
        }
    }

    private void redirectToRole(User.Role role) {
        NavigationManager nav = NavigationManager.getInstance();
        switch (role) {
            case CLIENT      -> nav.navigateContent("Dashboard",       () -> new ClientDashboardView().build());
            case PSYCHOLOGUE      -> nav.navigateContent("Dashboard",       () -> new PsychologueDashboardView().build());
            case ADMIN       -> nav.navigateContent("Admin Dashboard", () -> new AdminDashboardView().build());
            case SUPER_ADMIN -> nav.navigateContent("Admin Dashboard", () -> new AdminDashboardView().build());
        }
    }
}
