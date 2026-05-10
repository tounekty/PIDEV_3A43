package com.mindcare.controller.psychologue;

import com.mindcare.legacy.psychologue.BrowseRequestsLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class BrowseRequestsViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new BrowseRequestsLegacyContent().build());
    }
}
