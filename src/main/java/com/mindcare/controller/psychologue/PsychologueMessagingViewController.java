package com.mindcare.controller.psychologue;

import com.mindcare.legacy.psychologue.PsychologueMessagingLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class PsychologueMessagingViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new PsychologueMessagingLegacyContent().build());
    }
}
