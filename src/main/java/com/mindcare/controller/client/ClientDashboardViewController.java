package com.mindcare.controller.client;

import com.mindcare.legacy.client.ClientDashboardLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class ClientDashboardViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new ClientDashboardLegacyContent().build());
    }
}
