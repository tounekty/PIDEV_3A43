package com.mindcare.controller.psychologue;

import com.mindcare.legacy.psychologue.PsychologueProfileLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class PsychologueProfileViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new PsychologueProfileLegacyContent().build());
    }
}
