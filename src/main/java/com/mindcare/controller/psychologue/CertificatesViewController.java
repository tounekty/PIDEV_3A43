package com.mindcare.controller.psychologue;

import com.mindcare.legacy.psychologue.CertificatesLegacyContent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class CertificatesViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        contentRoot.getChildren().setAll(new CertificatesLegacyContent().build());
    }
}
