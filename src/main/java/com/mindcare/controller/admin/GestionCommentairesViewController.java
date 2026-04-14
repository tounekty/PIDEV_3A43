package com.mindcare.controller.admin;

import com.mindcare.legacy.admin.GestionCommentairesLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class GestionCommentairesViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new GestionCommentairesLegacyContent().build());
    }
}
