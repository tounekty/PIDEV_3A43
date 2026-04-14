package com.mindcare.controller.admin;

import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class GestionForumSujetsViewController {

    @FXML
    private StackPane contentRoot;

    @FXML
    private void initialize() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/mindcare/view/admin/GestionForumSujets.fxml"));
            contentRoot.getChildren().setAll(root);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load admin ticket module", exception);
        }
    }
}
