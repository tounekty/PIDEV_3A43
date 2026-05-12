package com.mindcare.controller.client;

import com.mindcare.legacy.client.MessagingLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class MessagingViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new MessagingLegacyContent().build());
    }
}
