package com.mindcare.controller.admin;

import com.mindcare.legacy.admin.GestionReservationsLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class GestionReservationsViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new GestionReservationsLegacyContent().build());
    }
}
