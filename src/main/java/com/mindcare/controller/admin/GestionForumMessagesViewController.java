package com.mindcare.controller.admin;

import com.mindcare.legacy.admin.GestionForumMessagesLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class GestionForumMessagesViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new GestionForumMessagesLegacyContent().build());
    }
}
