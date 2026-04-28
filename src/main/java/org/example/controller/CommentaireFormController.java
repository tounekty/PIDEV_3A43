package org.example.controller;

import org.example.model.Resource;
import org.example.model.Commentaire;
import org.example.service.CommentaireService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;

public class CommentaireFormController {
    @FXML private TextField authorNameField;
    @FXML private TextField authorEmailField;
    @FXML private TextArea contentArea;
    @FXML private Button star1Btn;
    @FXML private Button star2Btn;
    @FXML private Button star3Btn;
    @FXML private Button star4Btn;
    @FXML private Button star5Btn;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    
    private CommentaireService commentaireService = new CommentaireService();
    private Resource resource;
    private Runnable onCommentaireSaved;
    private int currentUserId = 1;
    private int selectedRating = 5;
    
    @FXML
    public void initialize() {
        updateStars();
    }
    
    public void setResource(Resource resource) {
        this.resource = resource;
    }
    
    public void setOnCommentaireSaved(Runnable onCommentaireSaved) {
        this.onCommentaireSaved = onCommentaireSaved;
    }

    public void setCurrentUserId(int currentUserId) {
        this.currentUserId = currentUserId;
    }

    @FXML
    private void handleStar1() {
        selectedRating = 1;
        updateStars();
    }

    @FXML
    private void handleStar2() {
        selectedRating = 2;
        updateStars();
    }

    @FXML
    private void handleStar3() {
        selectedRating = 3;
        updateStars();
    }

    @FXML
    private void handleStar4() {
        selectedRating = 4;
        updateStars();
    }

    @FXML
    private void handleStar5() {
        selectedRating = 5;
        updateStars();
    }

    private void updateStars() {
        Button[] stars = {star1Btn, star2Btn, star3Btn, star4Btn, star5Btn};
        for (int i = 0; i < stars.length; i++) {
            boolean filled = i < selectedRating;
            stars[i].setText(filled ? "★" : "☆");
            stars[i].setStyle(filled
                    ? "-fx-background-color: transparent; -fx-text-fill: #f7b500; -fx-font-size: 24px; -fx-font-weight: 700; -fx-padding: 2 2 2 2;"
                    : "-fx-background-color: transparent; -fx-text-fill: #b8c6d9; -fx-font-size: 24px; -fx-font-weight: 700; -fx-padding: 2 2 2 2;");
        }
    }
    
    @FXML
    private void handleSave() {
        try {
            String authorName = fieldText(authorNameField);
            String authorEmail = fieldText(authorEmailField);
            String content = fieldText(contentArea);

            // Validation basique
            if (authorName.isEmpty()) {
                showWarning("Le nom est obligatoire");
                return;
            }
            if (authorEmail.isEmpty()) {
                showWarning("L'email est obligatoire");
                return;
            }
            if (content.isEmpty()) {
                showWarning("Le commentaire est obligatoire");
                return;
            }
            
            Commentaire commentaire = new Commentaire(
                resource.getId(),
                authorName,
                authorEmail,
                content,
                selectedRating,
                currentUserId
            );
            
            commentaireService.createCommentaire(commentaire);
            showInfo("Commentaire ajouté avec succès");
            
            if (onCommentaireSaved != null) {
                onCommentaireSaved.run();
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

    private String fieldText(TextInputControl field) {
        if (field == null || field.getText() == null) {
            return "";
        }
        return field.getText().trim();
    }
}
