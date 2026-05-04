package com.mindcare.controller.psychologue;

import com.mindcare.legacy.psychologue.SubmitOfferLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class SubmitOfferViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new SubmitOfferLegacyContent().build());
    }
}
