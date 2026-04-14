package com.mindcare.controller.admin;

import com.mindcare.legacy.admin.DossiersEtudiantsLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class DossiersEtudiantsViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new DossiersEtudiantsLegacyContent().build());
    }
}
