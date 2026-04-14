package com.mindcare.controller.client;

import com.mindcare.legacy.client.ServiceRequestListLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class ServiceRequestListViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new ServiceRequestListLegacyContent().build());
    }
}
