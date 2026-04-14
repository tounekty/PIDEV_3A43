package com.mindcare.controller.psychologue;

import com.mindcare.legacy.psychologue.PsychologueContractsLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class PsychologueContractsViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new PsychologueContractsLegacyContent().build());
    }
}
