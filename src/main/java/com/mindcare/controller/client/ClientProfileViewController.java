package com.mindcare.controller.client;

import com.mindcare.dao.UserDAO;
import com.mindcare.legacy.client.ClientProfileLegacyContent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientProfileViewController {

    private static final Logger logger = Logger.getLogger(ClientProfileViewController.class.getName());

    @FXML
    private StackPane contentRoot;
    
    private int currentUserId = 1; // TODO: Set from SessionManager or auth context

    @FXML
    private void initialize() {
        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(15));
        mainContainer.setStyle("-fx-background-color: #F5F5F5;");
        
        // Add legacy profile content
        mainContainer.getChildren().add(new ClientProfileLegacyContent().build());
        
        // Wrap in ScrollPane for scrollability
        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-control-inner-background: #F5F5F5;");
        
        contentRoot.getChildren().setAll(scrollPane);
    }
}
