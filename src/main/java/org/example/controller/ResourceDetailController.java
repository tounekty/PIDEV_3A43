package org.example.controller;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.example.model.Commentaire;
import org.example.model.Resource;
import org.example.model.User;
import org.example.service.CommentaireService;
import org.example.service.GTTSTextToSpeechService;
import org.example.service.ResourceService;
import org.example.service.TranslationService;
import org.example.ui.UIAnimationService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class ResourceDetailController {
    @FXML private Label titleLabel;
    @FXML private Label typeLabel;
    @FXML private TextArea descriptionArea;
    @FXML private Label createdAtLabel;
    @FXML private Button ttsPlayBtn;
    @FXML private ComboBox<String> ttsVoiceCombo;
    @FXML private ComboBox<String> ttsSpeedCombo;
    @FXML private ComboBox<String> translationDescLangCombo;
    @FXML private Button translateDescBtn;
    @FXML private VBox imageSection;
    @FXML private ImageView resourceImageView;
    @FXML private Label imageErrorLabel;
    @FXML private VBox videoSection;
    @FXML private WebView videoWebView;
    @FXML private Button openVideoBtn;
    @FXML private TextField searchCommentField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> ratingFilterCombo;
    @FXML private ComboBox<String> sortCommentCombo;
    @FXML private ComboBox<String> translationLangCombo;
    @FXML private Button translateCommentBtn;
    @FXML private ListView<Commentaire> commentaireListView;
    @FXML private Button addCommentBtn;
    @FXML private Button deleteCommentBtn;
    @FXML private Button approveCommentBtn;
    
    private ResourceService resourceService = new ResourceService();
    private CommentaireService commentaireService = new CommentaireService();
    private Resource resource;
    private ObservableList<Commentaire> commentaireList;
    private final List<Commentaire> allCommentaires = new ArrayList<>();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private boolean adminMode = false;
    private User currentUser;
    private int currentUserId = 1;
    private String currentVideoUrl;
    private MediaPlayer mediaPlayer;
    
    @FXML
    public void initialize() {
        commentaireListView.setCellFactory(v -> new CommentaireCardCell());
        statusFilterCombo.getItems().setAll("Tous statuts", "Approuves", "En attente");
        statusFilterCombo.setValue("Tous statuts");
        ratingFilterCombo.getItems().setAll("Toutes notes", "Note >= 4", "Note >= 3", "Note >= 2");
        ratingFilterCombo.setValue("Toutes notes");
        sortCommentCombo.getItems().setAll("Plus récents", "Plus anciens", "Note haute", "Note basse", "Auteur A-Z");
        sortCommentCombo.setValue("Plus récents");

        if (ttsVoiceCombo != null) {
            ttsVoiceCombo.getItems().setAll("Féminine", "Masculine", "Standard");
            ttsVoiceCombo.setValue("Féminine");
        }

        if (ttsSpeedCombo != null) {
            ttsSpeedCombo.getItems().setAll("Lente", "Normale", "Rapide");
            ttsSpeedCombo.setValue("Normale");
        }
        
        // Initialisation traduction
        java.util.List<String> langLabels = new java.util.ArrayList<>();
        for (TranslationService.Language lang : TranslationService.getAvailableLanguages()) {
            langLabels.add(lang.code.toUpperCase() + " - " + lang.label);
        }
        translationLangCombo.getItems().setAll(langLabels);
        translationLangCombo.setValue("EN - English");
        
        translationDescLangCombo.getItems().setAll(langLabels);
        translationDescLangCombo.setValue("EN - English");
        
        searchCommentField.textProperty().addListener((obs, oldVal, newVal) -> applyCommentFilters());
        statusFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyCommentFilters());
        ratingFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyCommentFilters());
        sortCommentCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyCommentFilters());
    }
    
    public void setResource(Resource resource) {
        this.resource = resource;
        loadResourceData();
        loadCommentaires();
    }

    public void setAdminMode(boolean adminMode) {
        this.adminMode = adminMode;
        if (approveCommentBtn != null) {
            approveCommentBtn.setVisible(adminMode);
            approveCommentBtn.setManaged(adminMode);
        }
    }

    public void setCurrentUserId(int currentUserId) {
        this.currentUserId = currentUserId;
    }

    /** Utilisateur connecté (profil utilisé pour les commentaires et droits). */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            this.currentUserId = user.getId();
        }
    }
    
    private void loadResourceData() {
        titleLabel.setText(resource.getTitle());
        typeLabel.setText(resource.getType());
        descriptionArea.setText(resource.getDescription());
        descriptionArea.setWrapText(true);
        descriptionArea.setEditable(false);
        createdAtLabel.setText("Créé le: " + resource.getCreatedAt().format(DATE_FMT));

        String imageUrl = resource.getImageUrl();
        boolean hasImage = imageUrl != null && !imageUrl.isBlank();
        if (hasImage) {
            imageSection.setVisible(true);
            imageSection.setManaged(true);
            imageErrorLabel.setVisible(false);
            imageErrorLabel.setManaged(false);

            String source = resolveImageSource(imageUrl.trim());
            if (source == null) {
                resourceImageView.setImage(null);
                imageErrorLabel.setText("Lien image invalide.");
                imageErrorLabel.setVisible(true);
                imageErrorLabel.setManaged(true);
            } else {
                Image image = new Image(source, true);
                image.errorProperty().addListener((obs, oldVal, isError) -> {
                    if (Boolean.TRUE.equals(isError)) {
                        resourceImageView.setImage(null);
                        imageErrorLabel.setText("Impossible de charger l'image. Vérifiez que le lien est direct (.jpg/.png). ");
                        imageErrorLabel.setVisible(true);
                        imageErrorLabel.setManaged(true);
                    }
                });
                resourceImageView.setImage(image);
                if (image.isError()) {
                    resourceImageView.setImage(null);
                    imageErrorLabel.setText("Impossible de charger l'image. Vérifiez que le lien est direct (.jpg/.png). ");
                    imageErrorLabel.setVisible(true);
                    imageErrorLabel.setManaged(true);
                }
            }
        } else {
            imageSection.setVisible(false);
            imageSection.setManaged(false);
        }

        boolean isVideo = Resource.TYPE_VIDEO.equalsIgnoreCase(resource.getType());
        videoSection.setVisible(isVideo);
        videoSection.setManaged(isVideo);
        if (!isVideo) {
            return;
        }

        String videoUrl = resource.getVideoUrl();
        currentVideoUrl = videoUrl;
        if (videoUrl != null && !videoUrl.isBlank()) {
            videoWebView.getEngine().setJavaScriptEnabled(true);
            loadVideoPreview(videoUrl.trim());
            openVideoBtn.setDisable(false);
        } else {
            videoWebView.getEngine().loadContent("<html><body style='font-family:Arial;padding:12px;color:#5f6f86;'>Aucune vidéo disponible pour cette ressource.</body></html>");
            openVideoBtn.setDisable(true);
        }
    }

    private String extractYoutubeId(String url) {
        if (url.contains("youtube.com/watch?v=")) {
            String id = url.substring(url.indexOf("v=") + 2);
            int amp = id.indexOf('&');
            return amp >= 0 ? id.substring(0, amp) : id;
        }
        if (url.contains("youtu.be/")) {
            String id = url.substring(url.indexOf("youtu.be/") + 9);
            int qm = id.indexOf('?');
            return qm >= 0 ? id.substring(0, qm) : id;
        }
        if (url.contains("youtube.com/embed/")) {
            String id = url.substring(url.indexOf("youtube.com/embed/") + 18);
            int qm = id.indexOf('?');
            return qm >= 0 ? id.substring(0, qm) : id;
        }
        return null;
    }

    private String resolveImageSource(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String value = normalizeImageUrl(raw.trim());
        String lower = value.toLowerCase();

        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file:/")) {
            return value;
        }

        try {
            File file = new File(value);
            if (!file.isAbsolute()) {
                file = new File(System.getProperty("user.dir"), value);
            }
            if (file.exists()) {
                return file.toURI().toString();
            }
        } catch (Exception ignored) {
            return null;
        }

        return null;
    }

    private String normalizeImageUrl(String raw) {
        try {
            URI uri = URI.create(raw);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();

            // Supporte les liens Google Images du type:
            // https://www.google.com/imgres?imgurl=<direct-image-url>&...
            if (host.contains("google.") && uri.getRawQuery() != null) {
                String imgUrl = getQueryParam(uri.getRawQuery(), "imgurl");
                if (imgUrl != null && !imgUrl.isBlank()) {
                    return imgUrl;
                }
            }
        } catch (Exception ignored) {
            return raw;
        }
        return raw;
    }

    private String getQueryParam(String rawQuery, String key) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            if (!key.equalsIgnoreCase(k)) {
                continue;
            }
            return URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
        }
        return null;
    }

    private String toWatchVideoUrl(String url) {
        String id = extractYoutubeId(url);
        if (id != null && !id.isBlank()) {
            return "https://www.youtube.com/watch?v=" + id;
        }
        return url;
    }

    private void loadVideoPreview(String url) {
        String id = extractYoutubeId(url);
        if (id != null && !id.isBlank()) {
            videoWebView.getEngine().loadContent(buildYoutubePreviewHtml(id, toWatchVideoUrl(url)));
            return;
        }
        videoWebView.getEngine().loadContent(buildExternalVideoPreviewHtml(url));
    }

    private String buildYoutubePreviewHtml(String videoId, String watchUrl) {
        String safeVideoId = htmlEscape(videoId);
        String safeWatchUrl = htmlEscape(watchUrl);
        String thumbnailUrl = "https://img.youtube.com/vi/" + safeVideoId + "/hqdefault.jpg";

        return """
                <!doctype html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        html, body { margin:0; height:100%%; font-family:Arial, sans-serif; background:#102033; color:white; }
                        .preview { position:relative; height:100%%; min-height:300px; overflow:hidden; background:#102033; }
                        .preview::before { content:""; position:absolute; inset:0; background:url('%s') center/cover no-repeat; opacity:.82; filter:brightness(.72); }
                        .overlay { position:absolute; inset:0; display:flex; flex-direction:column; align-items:center; justify-content:center; gap:12px; text-align:center; padding:24px; background:linear-gradient(180deg, rgba(8,19,33,.18), rgba(8,19,33,.72)); }
                        .play { width:74px; height:52px; border-radius:16px; background:#ff0033; display:flex; align-items:center; justify-content:center; box-shadow:0 12px 28px rgba(0,0,0,.25); }
                        .tri { width:0; height:0; margin-left:5px; border-top:14px solid transparent; border-bottom:14px solid transparent; border-left:22px solid white; }
                        h1 { margin:0; font-size:24px; line-height:1.2; font-weight:700; }
                        p { margin:0; max-width:620px; color:#dce7f5; font-size:14px; line-height:1.45; }
                        a { color:#ffffff; font-weight:700; text-decoration:underline; }
                    </style>
                </head>
                <body>
                    <div class="preview">
                        <div class="overlay">
                            <div class="play"><div class="tri"></div></div>
                            <h1>Video YouTube</h1>
                            <p>Le lecteur YouTube embarque peut etre bloque dans JavaFX. Utilisez le bouton <b>Ouvrir sur YouTube</b> au-dessus, ou ouvrez <a href="%s">la video</a>.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(thumbnailUrl, safeWatchUrl);
    }

    private String buildExternalVideoPreviewHtml(String videoUrl) {
        String safeVideoUrl = htmlEscape(videoUrl);
        return """
                <!doctype html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        html, body { margin:0; height:100%%; font-family:Arial, sans-serif; background:#f5f9fd; color:#10233f; }
                        .box { height:100%%; min-height:300px; display:flex; flex-direction:column; align-items:center; justify-content:center; gap:10px; text-align:center; padding:24px; box-sizing:border-box; }
                        h1 { margin:0; font-size:22px; }
                        p { margin:0; color:#5f6f86; line-height:1.45; }
                        a { color:#0f5db8; font-weight:700; }
                    </style>
                </head>
                <body>
                    <div class="box">
                        <h1>Video externe</h1>
                        <p>Utilisez le bouton d'ouverture au-dessus pour consulter la video.</p>
                        <a href="%s">%s</a>
                    </div>
                </body>
                </html>
                """.formatted(safeVideoUrl, safeVideoUrl);
    }

    private String buildVideoMessageHtml(String message) {
        return """
                <!doctype html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family:Arial,sans-serif;margin:0;height:100%%;display:flex;align-items:center;justify-content:center;color:#5f6f86;background:#f5f9fd;">
                    <div>%s</div>
                </body>
                </html>
                """.formatted(htmlEscape(message));
    }

    private String htmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @FXML
    private void handleOpenVideo() {
        try {
            String url = currentVideoUrl == null ? "" : toWatchVideoUrl(currentVideoUrl.trim());
            if (url.isBlank()) {
                showWarning("Aucune URL vidéo disponible.");
                return;
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            } else {
                showWarning("Ouverture navigateur non supportée sur ce système.");
            }
        } catch (Exception e) {
            showError("Impossible d'ouvrir la vidéo: " + e.getMessage());
        }
    }
    
    private void loadCommentaires() {
        try {
            List<Commentaire> commentaires = adminMode
                    ? commentaireService.getCommentairesByResourceAll(resource.getId())
                    : commentaireService.getCommentairesByResource(resource.getId());
            allCommentaires.clear();
            allCommentaires.addAll(commentaires);
            applyCommentFilters();
        } catch (SQLException e) {
            showError("Erreur chargement commentaires: " + e.getMessage());
        }
    }

    private void applyCommentFilters() {
        String query = searchCommentField == null || searchCommentField.getText() == null
                ? ""
                : searchCommentField.getText().trim().toLowerCase(Locale.ROOT);
        List<Commentaire> filtered = new ArrayList<>();
        String status = statusFilterCombo == null ? "Tous statuts" : statusFilterCombo.getValue();
        String ratingMode = ratingFilterCombo == null ? "Toutes notes" : ratingFilterCombo.getValue();
        for (Commentaire c : allCommentaires) {
            if (!query.isEmpty() && !containsAny(c, query)) {
                continue;
            }
            if ("Approuves".equals(status) && !c.isApproved()) {
                continue;
            }
            if ("En attente".equals(status) && c.isApproved()) {
                continue;
            }
            if (!matchesRatingFilter(c.getRating(), ratingMode)) {
                continue;
            }
            {
                filtered.add(c);
            }
        }
        filtered.sort(resolveCommentComparator(sortCommentCombo == null ? null : sortCommentCombo.getValue()));
        commentaireList = FXCollections.observableArrayList(filtered);
        commentaireListView.setItems(commentaireList);
    }

    private boolean matchesRatingFilter(int rating, String ratingMode) {
        if (ratingMode == null || "Toutes notes".equals(ratingMode)) {
            return true;
        }
        if ("Note >= 4".equals(ratingMode)) {
            return rating >= 4;
        }
        if ("Note >= 3".equals(ratingMode)) {
            return rating >= 3;
        }
        if ("Note >= 2".equals(ratingMode)) {
            return rating >= 2;
        }
        return true;
    }

    private boolean containsAny(Commentaire c, String query) {
        return safe(c.getAuthorName()).toLowerCase(Locale.ROOT).contains(query)
                || safe(c.getAuthorEmail()).toLowerCase(Locale.ROOT).contains(query)
                || safe(c.getContent()).toLowerCase(Locale.ROOT).contains(query);
    }

    private Comparator<Commentaire> resolveCommentComparator(String mode) {
        if (mode == null) {
            return Comparator.comparing(Commentaire::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        return switch (mode) {
            case "Plus anciens" -> Comparator.comparing(Commentaire::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "Note haute" -> Comparator.comparingInt(Commentaire::getRating).reversed();
            case "Note basse" -> Comparator.comparingInt(Commentaire::getRating);
            case "Auteur A-Z" -> Comparator.comparing(c -> safe(c.getAuthorName()), String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(Commentaire::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }









    private String safe(String value) {
        return value == null ? "" : value;
    }

    @FXML
    private void handlePlayDescription() {
        if (mediaPlayer != null && mediaPlayer.getStatus() != MediaPlayer.Status.STOPPED) {
            stopDescriptionPlayback();
            return;
        }

        String text = descriptionArea.getText();
        if (text == null || text.trim().isEmpty()) {
            showWarning("Description vide. Rien à lire.");
            return;
        }

        ttsPlayBtn.setDisable(true);
        ttsPlayBtn.setText("");
        ttsPlayBtn.setGraphic(new javafx.scene.control.Label("⏳ Synthèse..."));

        // Run synthesis in background
        new Thread(() -> {
            try {
                String voice = resolveSelectedVoice();
                int speed = resolveSelectedSpeechRate();
                String mp3Path = GTTSTextToSpeechService.synthesizeToFile(text, "fr-FR", voice, speed);
                Platform.runLater(() -> playAudio(mp3Path));
            } catch (IOException e) {
                Platform.runLater(() -> {
                    showError("Erreur TTS: " + e.getMessage());
                    ttsPlayBtn.setDisable(false);
                    ttsPlayBtn.setText("");
                    ttsPlayBtn.setGraphic(UIAnimationService.createPlayIcon());
                });
            }
        }).start();
    }

    private String resolveSelectedVoice() {
        String selected = ttsVoiceCombo == null ? null : ttsVoiceCombo.getValue();
        if (selected == null) {
            return "fr+f3";
        }

        return switch (selected) {
            case "Masculine" -> "fr+m3";
            case "Standard" -> "fr";
            default -> "fr+f3";
        };
    }

    private int resolveSelectedSpeechRate() {
        String selected = ttsSpeedCombo == null ? null : ttsSpeedCombo.getValue();
        if (selected == null) {
            return 140;
        }

        return switch (selected) {
            case "Lente" -> 120;
            case "Rapide" -> 180;
            default -> 140;
        };
    }

    private void stopDescriptionPlayback() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
        } finally {
            ttsPlayBtn.setDisable(false);
            ttsPlayBtn.setText("");
            ttsPlayBtn.setGraphic(UIAnimationService.createPlayIcon());
        }
    }

    private void playAudio(String filePath) {
        try {
            // Stop previous player if exists
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }

            File audioFile = new File(filePath);
            String audioUrl = audioFile.toURI().toString();
            Media media = new Media(audioUrl);
            mediaPlayer = new MediaPlayer(media);
            ttsPlayBtn.setDisable(false);

            mediaPlayer.setOnReady(() -> {
                mediaPlayer.play();
                ttsPlayBtn.setText("");
                ttsPlayBtn.setGraphic(UIAnimationService.createStopIcon());
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                stopDescriptionPlayback();
            });

            mediaPlayer.setOnError(() -> {
                showError("Erreur lecture MP3: " + (mediaPlayer == null ? "unknown" : mediaPlayer.getError()));
                stopDescriptionPlayback();
            });
        } catch (Exception e) {
            showError("Erreur initialisation lecteur: " + e.getMessage());
            ttsPlayBtn.setDisable(false);
            ttsPlayBtn.setText("");
            ttsPlayBtn.setGraphic(UIAnimationService.createPlayIcon());
        }
    }

    @FXML
    private void handleAddComment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/fxml/commentaire_form.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Ajouter un commentaire");
            stage.setScene(new Scene(loader.load(), 600, 400));
            
            CommentaireFormController controller = loader.getController();
            controller.setResource(resource);
            controller.setConnectedUser(currentUser);
            controller.setCurrentUserId(currentUserId);
            controller.setOnCommentaireSaved(() -> {
                loadCommentaires();
                stage.close();
            });
            
            stage.showAndWait();
        } catch (IOException e) {
            showError("Erreur ouverture formulaire: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleDeleteComment() {
        Commentaire selected = commentaireListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Sélectionnez un commentaire à supprimer");
            return;
        }
        
        // Vérifier les permissions
        boolean isOwner = selected.getUserId() == currentUserId;
        boolean isAdmin = adminMode;
        
        if (!isOwner && !isAdmin) {
            showWarning("Vous ne pouvez supprimer que vos propres commentaires");
            return;
        }
        
        String deleteMsg = isOwner && !isAdmin 
            ? "Êtes-vous sûr de vouloir supprimer votre commentaire ?" 
            : "Êtes-vous sûr de vouloir supprimer ce commentaire ?";
        
        if (confirmDelete(deleteMsg)) {
            try {
                commentaireService.deleteCommentaire(selected.getId());
                loadCommentaires();
                showInfo("Commentaire supprimé avec succès");
            } catch (SQLException e) {
                showError("Erreur suppression: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleApproveComment() {
        Commentaire selected = commentaireListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Sélectionnez un commentaire à approuver");
            return;
        }
        
        try {
            commentaireService.approveCommentaire(selected.getId());
            loadCommentaires();
            showInfo("Commentaire approuvé");
        } catch (SQLException e) {
            showError("Erreur approbation: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleTranslateComments() {
        if (commentaireList == null || commentaireList.isEmpty()) {
            showWarning("Aucun commentaire à traduire");
            return;
        }
        
        String selected = translationLangCombo.getValue();
        if (selected == null || selected.isEmpty()) {
            showWarning("Sélectionnez une langue cible");
            return;
        }
        
        // Extraire le code langue
        String targetLang = selected.split(" - ")[0].toLowerCase();
        String sourceLang = "fr";
        
        System.out.println("Starting translation to: " + targetLang);
        
        try {
            int translatedCount = 0;
            for (Commentaire comment : commentaireList) {
                String originalContent = comment.getContent();
                
                // Vérifier si déjà traduit
                if (originalContent.startsWith("🌍 [")) {
                    System.out.println("Skipping already translated comment");
                    continue;
                }
                
                System.out.println("Translating: " + originalContent);
                String translatedContent = TranslationService.translate(originalContent, sourceLang, targetLang);
                System.out.println("Result: " + translatedContent);
                
                // Vérifier si la traduction est différente
                if (!translatedContent.equals(originalContent)) {
                    // Succès - ajouter le marqueur
                    String markedContent = "🌍 [" + targetLang.toUpperCase() + "] " + translatedContent + "\n\n📝 " + originalContent;
                    comment.setContent(markedContent);
                    translatedCount++;
                } else {
                    System.out.println("Translation returned same text");
                }
            }
            
            commentaireListView.refresh();
            if (translatedCount > 0) {
                showInfo("✅ " + translatedCount + " commentaire(s) traduit(s) en " + selected.split(" - ")[1]);
            } else {
                showWarning("⚠️ Aucun commentaire n'a pu être traduit. Vérifiez votre connexion internet.");
            }
        } catch (Exception e) {
            System.err.println("Error in handleTranslateComments: " + e.getMessage());
            e.printStackTrace();
            showError("Erreur traduction: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleTranslateDescription() {
        String description = descriptionArea.getText();
        if (description == null || description.isBlank()) {
            showWarning("Aucune description à traduire");
            return;
        }
        
        String selected = translationDescLangCombo.getValue();
        if (selected == null || selected.isEmpty()) {
            showWarning("Sélectionnez une langue cible");
            return;
        }
        
        // Extraire le code langue
        String targetLang = selected.split(" - ")[0].toLowerCase();
        String sourceLang = "fr";
        
        System.out.println("Starting description translation to: " + targetLang);
        
        try {
            // Vérifier si déjà traduite
            if (description.startsWith("🌍 [")) {
                showWarning("Description déjà traduite. Rechargez la ressource pour réinitialiser.");
                return;
            }
            
            System.out.println("Translating description: " + description);
            String translatedDesc = TranslationService.translate(description, sourceLang, targetLang);
            System.out.println("Translation result: " + translatedDesc);
            
            // Vérifier si traduction réussie
            if (!translatedDesc.equals(description)) {
                // Succès - modifier le TextArea
                String markedDescription = "🌍 [" + targetLang.toUpperCase() + "]\n" + translatedDesc + "\n\n📝 Original:\n" + description;
                descriptionArea.setText(markedDescription);
                descriptionArea.setWrapText(true);
                showInfo("✅ Description traduite en " + selected.split(" - ")[1]);
            } else {
                showWarning("⚠️ La description n'a pas pu être traduite. Vérifiez votre connexion internet.");
            }
        } catch (Exception e) {
            System.err.println("Error in handleTranslateDescription: " + e.getMessage());
            e.printStackTrace();
            showError("Erreur traduction description: " + e.getMessage());
        }
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
    
    private boolean confirmDelete(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer la suppression");
        alert.setContentText(message);
        return alert.showAndWait().filter(response -> response == ButtonType.OK).isPresent();
    }

    private String stars(int rating) {
        int safe = Math.max(1, Math.min(5, rating));
        return "★".repeat(safe) + "☆".repeat(5 - safe);
    }

    private int resolveCurrentVoterId() throws SQLException {
        int voterUserId = currentUser != null ? currentUser.getId() : currentUserId;
        if (voterUserId <= 0) {
            throw new SQLException("Connexion requise pour voter.");
        }
        return voterUserId;
    }

    private void updateCommentVoteCounts(Commentaire target, Commentaire updated) {
        if (target == null || updated == null) {
            return;
        }
        target.setLikeCount(updated.getLikeCount());
        target.setDislikeCount(updated.getDislikeCount());
        for (Commentaire comment : allCommentaires) {
            if (comment.getId() == target.getId()) {
                comment.setLikeCount(updated.getLikeCount());
                comment.setDislikeCount(updated.getDislikeCount());
                break;
            }
        }
    }

    private class CommentaireCardCell extends ListCell<Commentaire> {
        @Override
        protected void updateItem(Commentaire c, boolean empty) {
            super.updateItem(c, empty);
            if (empty || c == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            Label author = new Label(c.getAuthorName());
            author.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill:#10233f;");

            Label rating = new Label(stars(c.getRating()));
            rating.setStyle("-fx-text-fill:#f7b500; -fx-font-size: 14px; -fx-font-weight: 700;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox head = new HBox(10, author, spacer, rating);
            head.setAlignment(Pos.CENTER_LEFT);

            Label content = new Label(c.getContent());
            content.setWrapText(true);
            // Like/Dislike buttons
            Button likeBtn = new Button(" " + c.getLikeCount());
            likeBtn.setGraphic(UIAnimationService.createLikeIcon());
            likeBtn.setStyle("-fx-background-color: #f0f4f8; -fx-border-color: #d0dce8; -fx-padding: 6 10 6 10; -fx-background-radius: 8; -fx-font-size: 11px; -fx-cursor: hand;");

            Button dislikeBtn = new Button(" " + c.getDislikeCount());
            dislikeBtn.setGraphic(UIAnimationService.createDislikeIcon());
            dislikeBtn.setStyle("-fx-background-color: #f0f4f8; -fx-border-color: #d0dce8; -fx-padding: 6 10 6 10; -fx-background-radius: 8; -fx-font-size: 11px; -fx-cursor: hand;");
            likeBtn.setOnAction(e -> {
                try {
                    int voterUserId = resolveCurrentVoterId();
                    Commentaire updated = commentaireService.addLike(c.getId(), voterUserId);
                    updateCommentVoteCounts(c, updated);
                    likeBtn.setText(" " + c.getLikeCount());
                    dislikeBtn.setText(" " + c.getDislikeCount());
                } catch (SQLException ex) {
                    showError("Erreur like: " + ex.getMessage());
                }
            });
            dislikeBtn.setOnAction(e -> {
                try {
                    int voterUserId = resolveCurrentVoterId();
                    Commentaire updated = commentaireService.addDislike(c.getId(), voterUserId);
                    updateCommentVoteCounts(c, updated);
                    likeBtn.setText(" " + c.getLikeCount());
                    dislikeBtn.setText(" " + c.getDislikeCount());
                } catch (SQLException ex) {
                    showError("Erreur dislike: " + ex.getMessage());
                }
            });

            HBox likeBox = new HBox(8, likeBtn, dislikeBtn);
            likeBox.setAlignment(Pos.CENTER_LEFT);

            VBox card = new VBox(8, head, content, likeBox);
            card.setPadding(new Insets(12));
            card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #d9e7fb;");

            setGraphic(card);
        }
    }
}
