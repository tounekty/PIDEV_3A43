package com.mindcare.controller.admin;

import com.mindcare.legacy.admin.GestionResourcesLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class GestionResourcesViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new GestionResourcesLegacyContent().build());
    }
}
