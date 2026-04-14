package com.mindcare.controller.auth;

import com.mindcare.legacy.auth.ForgotPasswordLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class ForgotPasswordViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new ForgotPasswordLegacyContent().build());
    }
}
