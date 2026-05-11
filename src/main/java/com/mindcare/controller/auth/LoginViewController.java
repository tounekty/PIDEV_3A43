package com.mindcare.controller.auth;

import com.mindcare.legacy.auth.LoginLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class LoginViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new LoginLegacyContent().build());
    }
}
