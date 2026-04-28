package org.example.controller;

import java.sql.SQLException;

import org.example.model.Resource;
import org.example.service.HuggingFaceImageService;
import org.example.service.ResourceService;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ResourceFormController {
    private static final String IMAGE_MODE_LINK = "Lien image";
    private static final String IMAGE_MODE_HF = "Image IA Hugging Face";

    private enum ImageReviewDecision {
        APPROVE,
        REGENERATE,
        CANCEL
    }

    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField filePathField;
    @FXML private TextField videoUrlField;
    @FXML private TextField imageUrlField;
    @FXML private ComboBox<String> imageModeCombo;
    @FXML private TextArea imagePromptField;
    @FXML private Label imageStatusLabel;
    @FXML private Button generateImageBtn;
    @FXML private Button clearImageBtn;
    @FXML private TextArea aiPromptField;
    @FXML private Label aiStatusLabel;
    @FXML private Button generateAiBtn;
    @FXML private Button clearAiBtn;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    
    private ResourceService resourceService = new ResourceService();
    private HuggingFaceImageService huggingFaceImageService = new HuggingFaceImageService();
    private Resource resource;
    private Runnable onResourceSaved;
    private int currentUserId = 1;
    
    @FXML
    public void initialize() {
        typeCombo.getItems().addAll(Resource.TYPE_ARTICLE, Resource.TYPE_VIDEO);
        typeCombo.setValue(Resource.TYPE_ARTICLE);
        imageModeCombo.setItems(FXCollections.observableArrayList(IMAGE_MODE_LINK, IMAGE_MODE_HF));
        imageModeCombo.setValue(IMAGE_MODE_HF);
        if (generateImageBtn != null) {
            generateImageBtn.setText("Generer image Hugging Face");
        }
        imageModeCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateImageMode());
        updateImageMode();
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
    private void handleGenerateWithAi() {
        String prompt = fieldText(aiPromptField);
        if (prompt.isEmpty()) {
            showWarning("Entrez un prompt pour générer une ressource");
            return;
        }

        setAiBusy(true, "Génération en cours...");
        Task<Resource> task = new Task<>() {
            @Override
            protected Resource call() throws Exception {
                return resourceService.generateArticleDraft(prompt, currentUserId);
            }
        };

        task.setOnSucceeded(event -> {
            Resource generated = task.getValue();
            fillFormFromResource(generated);
            setAiBusy(false, "Brouillon Groq généré. Vérifiez puis enregistrez.");
            if (confirmPublishGeneratedResource()) {
                handleSave();
            }
        });

        task.setOnFailed(event -> {
            Throwable error = task.getException();
            setAiBusy(false, "Échec génération IA");
            showError(formatAiError(error));
        });

        Thread thread = new Thread(task, "ai-resource-generator");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleClearAiPrompt() {
        aiPromptField.clear();
        setAiStatus("Prêt");
    }

    @FXML
    private void handleGenerateImageWithAi() {
        if (!IMAGE_MODE_HF.equals(imageModeCombo.getValue())) {
            imageModeCombo.setValue(IMAGE_MODE_HF);
        }

        String imagePrompt = buildImagePrompt();
        if (imagePrompt.isBlank()) {
            showWarning("Ajoutez un prompt image, un titre ou une description avant de générer l'image.");
            return;
        }

        generateAndReviewImage(imagePrompt);
    }

    private void generateAndReviewImage(String imagePrompt) {
        setImageBusy(true, "Génération image...");
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return huggingFaceImageService.generateImage(imagePrompt);
            }
        };

        task.setOnSucceeded(event -> {
            String generatedImageUrl = task.getValue();
            ImageReviewDecision decision = showImageApprovalDialog(generatedImageUrl);

            if (decision == ImageReviewDecision.APPROVE) {
                imageUrlField.setText(generatedImageUrl);
                imageModeCombo.setValue(IMAGE_MODE_HF);
                setImageBusy(false, "Image approuvee");
                return;
            }

            if (decision == ImageReviewDecision.REGENERATE) {
                setImageBusy(false, "Regeneration demandee...");
                generateAndReviewImage(imagePrompt);
                return;
            }

            setImageBusy(false, "Image non retenue");
        });

        task.setOnFailed(event -> {
            Throwable error = task.getException();
            setImageBusy(false, "Échec image IA");
            showError(formatImageError(error));
        });

        Thread thread = new Thread(task, "hugging-face-image-generator");
        thread.setDaemon(true);
        thread.start();
    }

    private ImageReviewDecision showImageApprovalDialog(String imageUrl) {
        Dialog<ImageReviewDecision> dialog = new Dialog<>();
        dialog.setTitle("Validation de l'image");
        dialog.setHeaderText("Apercu de l'image generee");

        ButtonType approveButton = new ButtonType("Approuver", ButtonBar.ButtonData.OK_DONE);
        ButtonType regenerateButton = new ButtonType("Regenerer", ButtonBar.ButtonData.OTHER);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().setAll(approveButton, regenerateButton, cancelButton);

        Image image = new Image(imageUrl, false);
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(720);
        imageView.setFitHeight(420);

        Label infoLabel = new Label("Approuvez cette image ou demandez une nouvelle generation.");
        VBox content = new VBox(10, infoLabel, imageView);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(button -> {
            if (button == approveButton) {
                return ImageReviewDecision.APPROVE;
            }
            if (button == regenerateButton) {
                return ImageReviewDecision.REGENERATE;
            }
            return ImageReviewDecision.CANCEL;
        });

        return dialog.showAndWait().orElse(ImageReviewDecision.CANCEL);
    }

    @FXML
    private void handleClearImage() {
        imageUrlField.clear();
        imagePromptField.clear();
        setImageStatus("Image effacée");
    }
    
    @FXML
    private void handleSave() {
        try {
            String title = fieldText(titleField);
            String description = fieldText(descriptionField);
            String filePath = fieldText(filePathField);
            String videoUrl = fieldText(videoUrlField);
            String imageUrl = fieldText(imageUrlField);

            // Validation
            if (title.isEmpty()) {
                showWarning("Le titre est obligatoire");
                return;
            }
            if (description.isEmpty()) {
                showWarning("La description est obligatoire");
                return;
            }
            
            if (resource == null) {
                // Créer nouvelle ressource
                Resource newResource = new Resource(
                    title,
                    description,
                    typeCombo.getValue(),
                    currentUserId
                );
                newResource.setFilePath(filePath);
                newResource.setVideoUrl(videoUrl);
                newResource.setImageUrl(imageUrl);
                
                resourceService.createResource(newResource);
                showInfo("Ressource créée avec succès");
            } else {
                // Modifier ressource existante
                resource.setTitle(title);
                resource.setDescription(description);
                resource.setType(typeCombo.getValue());
                resource.setFilePath(filePath);
                resource.setVideoUrl(videoUrl);
                resource.setImageUrl(imageUrl);
                
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

    private void fillFormFromResource(Resource generated) {
        titleField.setText(generated.getTitle());
        descriptionField.setText(generated.getDescription());
        typeCombo.setValue(Resource.TYPE_ARTICLE);
        filePathField.setText(generated.getFilePath());
        videoUrlField.setText(generated.getVideoUrl());
        imageUrlField.setText(generated.getImageUrl());
        imageModeCombo.setValue(generated.getImageUrl() == null || generated.getImageUrl().isBlank()
            ? IMAGE_MODE_LINK
            : IMAGE_MODE_HF);
    }

    private boolean confirmPublishGeneratedResource() {
        ButtonType publishButton = new ButtonType("Publier maintenant", ButtonBar.ButtonData.OK_DONE);
        ButtonType editButton = new ButtonType("Modifier avant", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Valider la ressource");
        alert.setHeaderText("La ressource et son image ont ete generees.");
        alert.setContentText("Voulez-vous publier cette ressource maintenant ?");
        alert.getButtonTypes().setAll(publishButton, editButton);

        return alert.showAndWait().filter(response -> response == publishButton).isPresent();
    }

    private void setAiBusy(boolean busy, String status) {
        generateAiBtn.setDisable(busy);
        clearAiBtn.setDisable(busy);
        aiPromptField.setDisable(busy);
        setAiStatus(status);
    }

    private void setAiStatus(String status) {
        if (aiStatusLabel != null) {
            aiStatusLabel.setText(status);
        }
    }

    private void updateImageMode() {
        boolean aiMode = IMAGE_MODE_HF.equals(imageModeCombo.getValue());
        imagePromptField.setDisable(!aiMode);
        generateImageBtn.setDisable(false);
        setImageStatus(aiMode
                ? "Pret pour Hugging Face"
                : "Mode lien actif. Cliquez sur Generer image pour basculer en IA");
    }

    private String buildImagePrompt() {
        String prompt = fieldText(imagePromptField);
        if (!prompt.isBlank()) {
            return prompt;
        }

        String title = fieldText(titleField);
        String description = fieldText(descriptionField);
        return (title + "\n" + description).trim();
    }

    private void setImageBusy(boolean busy, String status) {
        if (busy) {
            imageModeCombo.setDisable(true);
            imagePromptField.setDisable(true);
            generateImageBtn.setDisable(true);
            clearImageBtn.setDisable(true);
        } else {
            imageModeCombo.setDisable(false);
            clearImageBtn.setDisable(false);
            updateImageMode();
        }
        setImageStatus(status);
    }

    private void setImageStatus(String status) {
        if (imageStatusLabel != null) {
            imageStatusLabel.setText(status);
        }
    }

    private String formatAiError(Throwable error) {
        String message = error == null ? "Erreur inconnue." : error.getMessage();
        if (message != null && message.contains("GROQ_API_KEY")) {
            return "GROQ_API_KEY est manquante. Créez une clé gratuite sur https://console.groq.com/keys, ajoutez-la dans PowerShell, puis relancez l'application.";
        }
        if (message != null && (message.contains("HTTP 401") || message.toLowerCase().contains("invalid api key"))) {
            return "Votre clé Groq est invalide. Vérifiez GROQ_API_KEY dans PowerShell et relancez l'application.";
        }
        if (message != null && (message.contains("HTTP 429") || message.toLowerCase().contains("rate limit"))) {
            return "Limite gratuite Groq atteinte pour le moment. Patientez quelques minutes, puis réessayez.";
        }
        if (message != null && message.contains("HUGGING_FACE_API_KEY")) {
            return "HUGGING_FACE_API_KEY est manquante. Ajoutez votre token Hugging Face puis relancez l'application.";
        }
        if (message != null && (message.contains("Cannot POST /models/") || message.contains("HTTP 404"))) {
            return "Modele Hugging Face indisponible pour votre token. Configurez HUGGING_FACE_MODEL=black-forest-labs/FLUX.1-schnell puis relancez l'application.";
        }
        if (message != null && message.contains("GEMINI_API_KEY")) {
            return "Cette version utilise Hugging Face pour l'image. Configurez HUGGING_FACE_API_KEY.";
        }
        return "Erreur génération IA: " + message;
    }

    private String formatImageError(Throwable error) {
        String message = error == null ? "Erreur inconnue." : error.getMessage();
        if (message != null && message.contains("HUGGING_FACE_API_KEY")) {
            return "HUGGING_FACE_API_KEY est manquante. Ajoutez votre token Hugging Face puis relancez l'application.";
        }
        if (message != null && (message.contains("Cannot POST /models/") || message.contains("HTTP 404"))) {
            return "Modele Hugging Face indisponible pour votre token. Configurez HUGGING_FACE_MODEL=black-forest-labs/FLUX.1-schnell puis relancez l'application.";
        }
        if (message != null && message.contains("GEMINI_API_KEY")) {
            return "Cette version utilise Hugging Face pour l'image. Configurez HUGGING_FACE_API_KEY.";
        }
        if (message != null && (message.contains("HTTP 401") || message.contains("HTTP 403"))) {
            return "Token Hugging Face invalide ou non autorise. Verifiez HUGGING_FACE_API_KEY.";
        }
        if (message != null && message.contains("HTTP 429")) {
            return "Limite Hugging Face atteinte. Patientez puis reessayez.";
        }
        return "Erreur génération image: " + message;
    }

    private String fieldText(TextInputControl field) {
        if (field == null || field.getText() == null) {
            return "";
        }
        return field.getText().trim();
    }
}
