package com.mindcare.controller.admin;

import com.mindcare.legacy.admin.GestionEventsLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class GestionEventsViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new GestionEventsLegacyContent().build());
    }
}
