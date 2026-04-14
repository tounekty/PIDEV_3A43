package com.mindcare.controller.client;

import com.mindcare.legacy.client.CreateServiceRequestLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class CreateServiceRequestViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new CreateServiceRequestLegacyContent().build());
    }
}
