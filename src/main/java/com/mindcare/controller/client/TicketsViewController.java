package com.mindcare.controller.client;

import com.mindcare.legacy.client.TicketsLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class TicketsViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new TicketsLegacyContent().build());
    }
}
