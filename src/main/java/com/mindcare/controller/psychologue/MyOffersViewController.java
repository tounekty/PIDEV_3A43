package com.mindcare.controller.psychologue;

import com.mindcare.legacy.psychologue.MyOffersLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class MyOffersViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new MyOffersLegacyContent().build());
    }
}
