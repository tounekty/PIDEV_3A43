package com.mindcare.controller.auth;

import com.mindcare.legacy.auth.RegisterLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class RegisterViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new RegisterLegacyContent().build());
    }
}
