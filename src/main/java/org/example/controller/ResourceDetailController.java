package org.example.controller;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.example.model.Commentaire;
import org.example.model.Resource;
import org.example.service.CommentaireService;
import org.example.service.ResourceService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
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
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ResourceDetailController {
    @FXML private Label titleLabel;
    @FXML private Label typeLabel;
    @FXML private TextArea descriptionArea;
    @FXML private Label createdAtLabel;
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
    @FXML private Label commentTotalValueLabel;
    @FXML private Label commentAverageValueLabel;
    @FXML private Label commentApprovedValueLabel;
    @FXML private Label commentPendingValueLabel;
    @FXML private PieChart commentPieChart;
    @FXML private Button exportCommentBtn;
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
    private int currentUserId = 1;
    private String currentVideoUrl;
    
    @FXML
    public void initialize() {
        commentaireListView.setCellFactory(v -> new CommentaireCardCell());
        statusFilterCombo.getItems().setAll("Tous statuts", "Approuves", "En attente");
        statusFilterCombo.setValue("Tous statuts");
        ratingFilterCombo.getItems().setAll("Toutes notes", "Note >= 4", "Note >= 3", "Note >= 2");
        ratingFilterCombo.setValue("Toutes notes");
        sortCommentCombo.getItems().setAll("Plus récents", "Plus anciens", "Note haute", "Note basse", "Auteur A-Z");
        sortCommentCombo.setValue("Plus récents");
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
        if (deleteCommentBtn != null) {
            deleteCommentBtn.setVisible(adminMode);
            deleteCommentBtn.setManaged(adminMode);
        }
    }

    public void setCurrentUserId(int currentUserId) {
        this.currentUserId = currentUserId;
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
            videoWebView.getEngine().load(toWatchVideoUrl(videoUrl.trim()));
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
        updateCommentStats(filtered);
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

    private void updateCommentStats(List<Commentaire> comments) {
        int total = comments.size();
        double average = comments.stream().mapToInt(Commentaire::getRating).average().orElse(0.0);
        long approved = comments.stream().filter(Commentaire::isApproved).count();
        long pending = Math.max(0, total - approved);

        commentTotalValueLabel.setText(String.valueOf(total));
        commentAverageValueLabel.setText(String.format(Locale.ROOT, "%.2f", average));
        commentApprovedValueLabel.setText(String.valueOf(approved));
        commentPendingValueLabel.setText(String.valueOf(pending));

        commentPieChart.setTitle("Statut des commentaires");
        commentPieChart.setData(FXCollections.observableArrayList(
            new PieChart.Data("Approuves", approved),
            new PieChart.Data("En attente", pending)
        ));
    }

    @FXML
    private void handleExportComments() {
        if (resource == null || commentaireList == null || commentaireList.isEmpty()) {
            showWarning("Aucun commentaire à exporter.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les commentaires (CSV)");
        chooser.setInitialFileName("commentaires-resource-" + resource.getId() + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));
        File target = chooser.showSaveDialog(commentaireListView.getScene().getWindow());
        if (target == null) {
            return;
        }

        StringBuilder csv = new StringBuilder("comment_id,auteur,email,note,approved,created_at,contenu\n");
        for (Commentaire c : commentaireList) {
            csv.append(c.getId()).append(',')
                    .append(csvEscape(c.getAuthorName())).append(',')
                    .append(csvEscape(c.getAuthorEmail())).append(',')
                    .append(c.getRating()).append(',')
                    .append(c.isApproved()).append(',')
                    .append(csvEscape(c.getCreatedAt() == null ? "" : c.getCreatedAt().format(DATE_FMT))).append(',')
                    .append(csvEscape(c.getContent()))
                    .append('\n');
        }

        try {
            Files.writeString(target.toPath(), csv.toString(), StandardCharsets.UTF_8);
            showInfo("Export commentaires réussi: " + commentaireList.size() + " commentaire(s)");
        } catch (IOException e) {
            showError("Erreur export commentaires: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportCommentsPdf() {
        if (resource == null || commentaireList == null || commentaireList.isEmpty()) {
            showWarning("Aucun commentaire a exporter en PDF.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les commentaires (PDF)");
        chooser.setInitialFileName("commentaires-resource-" + resource.getId() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        File target = chooser.showSaveDialog(commentaireListView.getScene().getWindow());
        if (target == null) {
            return;
        }

        List<String> lines = new ArrayList<>();
        lines.add("Ressource #" + resource.getId() + " - " + safe(resource.getTitle()));
        lines.add(" ");
        for (Commentaire c : commentaireList) {
            String date = c.getCreatedAt() == null ? "" : c.getCreatedAt().format(DATE_FMT);
            lines.add(String.format(Locale.ROOT, "C#%d | %s | note %d/5 | %s", c.getId(), safe(c.getAuthorName()), c.getRating(), date));
            lines.add("  " + safe(c.getContent()));
            lines.add(" ");
        }

        try {
            writeSimplePdf(target, "Export Commentaires", lines);
            showInfo("Export PDF commentaires reussi: " + commentaireList.size() + " commentaire(s)");
        } catch (IOException e) {
            showError("Erreur export PDF commentaires: " + e.getMessage());
        }
    }

    private void writeSimplePdf(File target, String title, List<String> lines) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float margin = 50;
            float y = page.getMediaBox().getHeight() - margin;

            PDPageContentStream content = new PDPageContentStream(doc, page);
            content.setFont(PDType1Font.HELVETICA_BOLD, 14);
            content.beginText();
            content.newLineAtOffset(margin, y);
            content.showText(title);
            content.endText();
            y -= 24;

            content.setFont(PDType1Font.HELVETICA, 10);
            for (String line : lines) {
                if (y < margin) {
                    content.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    content = new PDPageContentStream(doc, page);
                    content.setFont(PDType1Font.HELVETICA, 10);
                    y = page.getMediaBox().getHeight() - margin;
                }

                String normalized = line == null ? "" : line.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
                int max = 105;
                for (int i = 0; i < normalized.length(); i += max) {
                    String part = normalized.substring(i, Math.min(i + max, normalized.length()));
                    content.beginText();
                    content.newLineAtOffset(margin, y);
                    content.showText(part);
                    content.endText();
                    y -= 14;
                    if (y < margin) {
                        content.close();
                        page = new PDPage(PDRectangle.A4);
                        doc.addPage(page);
                        content = new PDPageContentStream(doc, page);
                        content.setFont(PDType1Font.HELVETICA, 10);
                        y = page.getMediaBox().getHeight() - margin;
                    }
                }
                if (normalized.isEmpty()) {
                    y -= 8;
                }
            }

            content.close();
            doc.save(target);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String csvEscape(String value) {
        String safeValue = value == null ? "" : value;
        return '"' + safeValue.replace("\"", "\"\"") + '"';
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
        
        if (confirmDelete()) {
            try {
                commentaireService.deleteCommentaire(selected.getId());
                loadCommentaires();
                showInfo("Commentaire supprimé");
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
    
    private boolean confirmDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer la suppression");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce commentaire?");
        return alert.showAndWait().filter(response -> response == ButtonType.OK).isPresent();
    }

    private String stars(int rating) {
        int safe = Math.max(1, Math.min(5, rating));
        return "★".repeat(safe) + "☆".repeat(5 - safe);
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
            content.setStyle("-fx-text-fill:#4a607f; -fx-font-size: 13px;");

            VBox card = new VBox(8, head, content);
            card.setPadding(new Insets(12));
            card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #d9e7fb;");

            setGraphic(card);
        }
    }
}
