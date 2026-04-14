package com.mindcare.controller.psychologue;

import com.mindcare.legacy.psychologue.PsychologueTicketsLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class PsychologueTicketsViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new PsychologueTicketsLegacyContent().build());
    }
}
