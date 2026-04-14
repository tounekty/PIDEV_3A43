package org.example.controller;

import org.example.model.Resource;
import org.example.service.ResourceService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;

public class ResourceFormController {
    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField filePathField;
    @FXML private TextField videoUrlField;
    @FXML private TextField imageUrlField;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    
    private ResourceService resourceService = new ResourceService();
    private Resource resource;
    private Runnable onResourceSaved;
    private int currentUserId = 1;
    
    @FXML
    public void initialize() {
        typeCombo.getItems().addAll(Resource.TYPE_ARTICLE, Resource.TYPE_VIDEO);
        typeCombo.setValue(Resource.TYPE_ARTICLE);
    }
    
    public void setResource(Resource resource) {
        this.resource = resource;
        if (resource != null) {
            titleField.setText(resource.getTitle());
            descriptionField.setText(resource.getDescription());
            typeCombo.setValue(resource.getType());
            filePathField.setText(resource.getFilePath());
            videoUrlField.setText(resource.getVideoUrl());
            imageUrlField.setText(resource.getImageUrl());
        }
    }
    
    public void setOnResourceSaved(Runnable onResourceSaved) {
        this.onResourceSaved = onResourceSaved;
    }

    public void setCurrentUserId(int currentUserId) {
        this.currentUserId = currentUserId;
    }
    
    @FXML
    private void handleSave() {
        try {
            // Validation
            if (titleField.getText().isEmpty()) {
                showWarning("Le titre est obligatoire");
                return;
            }
            if (descriptionField.getText().isEmpty()) {
                showWarning("La description est obligatoire");
                return;
            }
            
            if (resource == null) {
                // Créer nouvelle ressource
                Resource newResource = new Resource(
                    titleField.getText(),
                    descriptionField.getText(),
                    typeCombo.getValue(),
                    currentUserId
                );
                newResource.setFilePath(filePathField.getText());
                newResource.setVideoUrl(videoUrlField.getText());
                newResource.setImageUrl(imageUrlField.getText());
                
                resourceService.createResource(newResource);
                showInfo("Ressource créée avec succès");
            } else {
                // Modifier ressource existante
                resource.setTitle(titleField.getText());
                resource.setDescription(descriptionField.getText());
                resource.setType(typeCombo.getValue());
                resource.setFilePath(filePathField.getText());
                resource.setVideoUrl(videoUrlField.getText());
                resource.setImageUrl(imageUrlField.getText());
                
                resourceService.updateResource(resource);
                showInfo("Ressource modifiée avec succès");
            }
            
            if (onResourceSaved != null) {
                onResourceSaved.run();
            }
        } catch (IllegalArgumentException e) {
            showWarning(e.getMessage());
        } catch (SQLException e) {
            showError("Erreur base de données: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
