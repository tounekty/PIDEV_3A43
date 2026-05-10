package com.mindcare.controller.psychologue;

import com.mindcare.legacy.psychologue.PsychologueDashboardLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class PsychologueDashboardViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new PsychologueDashboardLegacyContent().build());
    }
}
