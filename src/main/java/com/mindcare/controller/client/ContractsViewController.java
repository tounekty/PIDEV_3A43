package com.mindcare.controller.client;

import com.mindcare.legacy.client.ContractsLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class ContractsViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new ContractsLegacyContent().build());
    }
}
