package com.mindcare.controller.client;

import com.mindcare.legacy.client.OffersReceivedLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class OffersReceivedViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new OffersReceivedLegacyContent().build());
    }
}
