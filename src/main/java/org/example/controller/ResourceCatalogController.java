package org.example.controller;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class ResourceCatalogController {
    @FXML
    private TextField searchField;

    @FXML
    private GridPane resourceGridPane;

    @FXML
    private ScrollPane gridScrollPane;

    private final ResourceService resourceService = new ResourceService();
    private ObservableList<Resource> resourceList = FXCollections.observableArrayList();
    private int currentUserId = 1;
    private boolean adminMode = false;
    private static final int COLUMNS = 4;
    private static final int CARD_WIDTH = 220;
    private static final int IMAGE_HEIGHT = 180;
    private static final int CARD_HEIGHT = 420;

    @FXML
    public void initialize() {
        if (resourceGridPane == null) {
            resourceGridPane = new GridPane();
            resourceGridPane.setHgap(16);
            resourceGridPane.setVgap(16);
            resourceGridPane.setPadding(new Insets(14));
            resourceGridPane.setStyle("-fx-background-color: transparent;");

            if (gridScrollPane != null) {
                gridScrollPane.setContent(resourceGridPane);
                gridScrollPane.setFitToWidth(true);
                gridScrollPane.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
            }
        }

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
            displayResourcesInGrid();
        } catch (SQLException e) {
            showError("Erreur chargement ressources: " + e.getMessage());
        }
    }

    private void displayResourcesInGrid() {
        resourceGridPane.getChildren().clear();
        int col = 0;
        int row = 0;

        for (Resource resource : resourceList) {
            VBox card = createResourceCard(resource);
            resourceGridPane.add(card, col, row);
            GridPane.setHgrow(card, Priority.ALWAYS);

            col++;
            if (col >= COLUMNS) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createResourceCard(Resource resource) {
        // Image container
        ImageView imageView = new ImageView();
        imageView.setFitWidth(CARD_WIDTH);
        imageView.setFitHeight(IMAGE_HEIGHT);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        StackPane imageCard = new StackPane();
        imageCard.getStyleClass().add("grid-image-card");
        imageCard.setMinSize(CARD_WIDTH, IMAGE_HEIGHT);
        imageCard.setPrefSize(CARD_WIDTH, IMAGE_HEIGHT);
        imageCard.setMaxSize(CARD_WIDTH, IMAGE_HEIGHT);

        String imageSource = resolveCardVisualSource(resource);
        loadCardImage(imageCard, imageView, imageSource, resource);

        // Title
        Label title = new Label(resource.getTitle());
        title.getStyleClass().add("grid-card-title");
        title.setWrapText(true);
        title.setMaxWidth(CARD_WIDTH - 10);
        title.setAlignment(Pos.CENTER);
        title.setTextAlignment(TextAlignment.CENTER);
        title.setMaxHeight(46);

        // Type badge
        Label type = new Label(resource.getType().toUpperCase());
        type.getStyleClass().add("grid-type-pill");

        // Description
        String desc = resource.getDescription();
        if (desc != null && desc.length() > 100) {
            desc = desc.substring(0, 100) + "...";
        }
        Label description = new Label(desc != null ? desc : "");
        description.setWrapText(true);
        description.getStyleClass().add("grid-card-description");
        description.setMaxWidth(CARD_WIDTH - 10);
        description.setMaxHeight(64);
        description.setTextAlignment(TextAlignment.LEFT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Open button
        Button openButton = new Button("Voir");
        openButton.getStyleClass().add("grid-open-button");
        openButton.setMaxWidth(Double.MAX_VALUE);
        openButton.setOnAction(e -> openResourceDetail(resource));

        // Card container
        VBox card = new VBox(10, imageCard, title, type, description, spacer, openButton);
        card.getStyleClass().add("grid-resource-card");
        card.setMinWidth(CARD_WIDTH);
        card.setPrefWidth(CARD_WIDTH);
        card.setFillWidth(true);
        card.setAlignment(Pos.TOP_CENTER);
        card.setMinHeight(CARD_HEIGHT);
        card.setPrefHeight(CARD_HEIGHT);

        return card;
    }

    private void openResourceDetail(Resource resource) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/fxml/resource_detail.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Ressource: " + resource.getTitle());
            stage.setScene(new Scene(loader.load(), 880, 700));
            stage.setMinWidth(760);
            stage.setMinHeight(560);
            stage.setResizable(true);

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

    private String normalizeImageSource(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String value = normalizeImageUrl(rawValue.trim());

        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("file:/")) {
            return value;
        }

        try {
            Path absolute = Path.of(value);
            if (Files.exists(absolute)) {
                return absolute.toUri().toString();
            }
        } catch (Exception ignored) {
            // Ignore invalid path formatting and continue fallback.
        }

        try {
            Path relative = Path.of(System.getProperty("user.dir")).resolve(value).normalize();
            if (Files.exists(relative)) {
                return relative.toUri().toString();
            }
        } catch (Exception ignored) {
            // Ignore and return null if no valid source is found.
        }

        return null;
    }

    private void loadCardImage(StackPane imageCard, ImageView imageView, String imageSource, Resource resource) {
        imageCard.getChildren().clear();
        if (imageSource == null || imageSource.isBlank()) {
            imageCard.getChildren().add(createFallbackVisual(resource));
            return;
        }

        Image image = new Image(imageSource, CARD_WIDTH, IMAGE_HEIGHT, true, true, true);
        image.errorProperty().addListener((obs, oldVal, isError) -> {
            if (Boolean.TRUE.equals(isError)) {
                imageCard.getChildren().setAll(createFallbackVisual(resource));
            }
        });

        imageView.setImage(image);
        imageCard.getChildren().add(imageView);

        if (image.isError()) {
            imageCard.getChildren().setAll(createFallbackVisual(resource));
        }
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

    private String resolveCardVisualSource(Resource resource) {
        String imageSource = normalizeImageSource(resource.getImageUrl());
        if (imageSource != null) {
            return imageSource;
        }

        if (Resource.TYPE_VIDEO.equalsIgnoreCase(resource.getType())) {
            return buildYoutubeThumbnailUrl(resource.getVideoUrl());
        }

        return null;
    }

    private String buildYoutubeThumbnailUrl(String videoUrl) {
        String videoId = extractYoutubeId(videoUrl);
        if (videoId == null || videoId.isBlank()) {
            return null;
        }
        return "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
    }

    private String extractYoutubeId(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        String value = url.trim();
        if (value.contains("youtube.com/watch?v=")) {
            String id = value.substring(value.indexOf("v=") + 2);
            int amp = id.indexOf('&');
            return amp >= 0 ? id.substring(0, amp) : id;
        }
        if (value.contains("youtu.be/")) {
            String id = value.substring(value.indexOf("youtu.be/") + 9);
            int qm = id.indexOf('?');
            return qm >= 0 ? id.substring(0, qm) : id;
        }
        if (value.contains("youtube.com/embed/")) {
            String id = value.substring(value.indexOf("youtube.com/embed/") + 18);
            int qm = id.indexOf('?');
            return qm >= 0 ? id.substring(0, qm) : id;
        }
        if (value.contains("youtube.com/shorts/")) {
            String id = value.substring(value.indexOf("youtube.com/shorts/") + 17);
            int qm = id.indexOf('?');
            int slash = id.indexOf('/');
            int cut = qm >= 0 ? qm : (slash >= 0 ? slash : -1);
            return cut >= 0 ? id.substring(0, cut) : id;
        }
        return null;
    }

    private Label createFallbackVisual(Resource resource) {
        Label fallback = new Label();
        fallback.getStyleClass().add("grid-image-placeholder");

        if (Resource.TYPE_VIDEO.equalsIgnoreCase(resource.getType())) {
            fallback.setText("▶ Vidéo");
            fallback.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #6c85a8;");
        } else {
            fallback.setText("Aucune image");
        }

        return fallback;
    }
}
