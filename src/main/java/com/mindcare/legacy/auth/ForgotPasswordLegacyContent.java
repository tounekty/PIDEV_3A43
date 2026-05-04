package com.mindcare.legacy.auth;

import com.mindcare.view.auth.*;

import com.mindcare.utils.NavigationManager;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * ForgotPasswordLegacyContent – password reset request screen.
 */
public class ForgotPasswordLegacyContent implements NavigationManager.Buildable {

    @Override
    public Node build() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #020617;");

        VBox card = new VBox(22);
        card.getStyleClass().add("auth-card");
        card.setAlignment(Pos.TOP_CENTER);

        Label logo = new Label("⬡ MINDCARE");
        logo.getStyleClass().add("auth-logo-text");

        Label title = new Label("Reset Password");
        title.getStyleClass().add("auth-title");

        Label subtitle = new Label("Enter your email to receive reset instructions");
        subtitle.getStyleClass().add("auth-subtitle");

        VBox header = new VBox(6, logo, title, subtitle);
        header.setAlignment(Pos.CENTER);

        TextField emailField = new TextField();
        emailField.setPromptText("you@mindcare.io");
        emailField.getStyleClass().add("text-field");
        emailField.setMaxWidth(Double.MAX_VALUE);

        Label emailLabel = new Label("Email Address");
        emailLabel.getStyleClass().add("form-label");

        Label confirmLabel = new Label("");
        confirmLabel.setVisible(false);

        Button sendBtn = new Button("Send Reset Link");
        sendBtn.getStyleClass().addAll("btn", "btn-primary");
        sendBtn.setMaxWidth(Double.MAX_VALUE);
        sendBtn.setPrefHeight(44);
        sendBtn.setOnAction(e -> {
            confirmLabel.setText("✓ If that email exists, a reset link has been sent.");
            confirmLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 13px;");
            confirmLabel.setVisible(true);
        });

        Button backBtn = new Button("← Back to Sign In");
        backBtn.getStyleClass().add("auth-link");
        backBtn.setOnAction(e -> NavigationManager.getInstance().navigateTo(new LoginView()));

        card.getChildren().addAll(header, new VBox(6, emailLabel, emailField), confirmLabel, sendBtn, backBtn);

        root.getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);
        return root;
    }
}
