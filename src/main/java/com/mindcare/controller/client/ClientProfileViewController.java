package com.mindcare.controller.client;

import com.mindcare.legacy.client.ClientProfileLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class ClientProfileViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new ClientProfileLegacyContent().build());
    }
}
