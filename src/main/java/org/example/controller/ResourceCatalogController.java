package org.example.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.example.model.Resource;
import org.example.service.ResourceService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ResourceCatalogController {
    @FXML
    private TextField searchField;

    @FXML
    private ListView<Resource> resourceListView;

    private final ResourceService resourceService = new ResourceService();
    private ObservableList<Resource> resourceList = FXCollections.observableArrayList();
    private int currentUserId = 1;
    private boolean adminMode = false;

    @FXML
    public void initialize() {
        resourceListView.setCellFactory(list -> new ResourceCardCell());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadResources(newVal));
        loadResources("");
    }

    public void setCurrentUserId(int currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setAdminMode(boolean adminMode) {
        this.adminMode = adminMode;
    }

    @FXML
    private void handleRefresh() {
        loadResources(searchField.getText());
    }

    private void loadResources(String query) {
        try {
            List<Resource> resources = (query == null || query.isBlank())
                    ? resourceService.getAllResources()
                    : resourceService.searchResources(query);
            resourceList.setAll(resources);
            resourceListView.setItems(resourceList);
        } catch (SQLException e) {
            showError("Erreur chargement ressources: " + e.getMessage());
        }
    }

    private void openResourceDetail(Resource resource) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/fxml/resource_detail.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Ressource: " + resource.getTitle());
            stage.setScene(new Scene(loader.load(), 1000, 860));

            ResourceDetailController controller = loader.getController();
            controller.setAdminMode(adminMode);
            controller.setCurrentUserId(currentUserId);
            controller.setResource(resource);

            stage.show();
        } catch (IOException e) {
            showError("Erreur ouverture ressource: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private class ResourceCardCell extends ListCell<Resource> {
        @Override
        protected void updateItem(Resource resource, boolean empty) {
            super.updateItem(resource, empty);
            if (empty || resource == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            Label title = new Label(resource.getTitle());
            title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #10233f;");

            Label type = new Label(resource.getType().toUpperCase());
            type.setStyle("-fx-background-color: #e7f0ff; -fx-text-fill: #0f69ff; -fx-background-radius: 999; -fx-padding: 4 10 4 10; -fx-font-weight: 700;");

            Label description = new Label(resource.getDescription());
            description.setWrapText(true);
            description.setStyle("-fx-text-fill: #4a607f; -fx-font-size: 13px;");

            Button openButton = new Button("Voir et commenter");
            openButton.setStyle("-fx-background-color: linear-gradient(to right,#0f69ff,#38a4ff); -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 10 14 10 14;");
            openButton.setOnAction(e -> openResourceDetail(resource));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox topRow = new HBox(10, title, type, spacer, openButton);
            topRow.setAlignment(Pos.CENTER_LEFT);

            VBox box = new VBox(10, topRow, description);
            box.setPadding(new Insets(18));
            box.setStyle("-fx-background-color: linear-gradient(to right, rgba(255,255,255,0.98), rgba(244,249,255,0.95)); -fx-background-radius:18; -fx-border-radius:18; -fx-border-color: rgba(138,182,238,0.30);");

            setGraphic(box);
        }
    }
}
