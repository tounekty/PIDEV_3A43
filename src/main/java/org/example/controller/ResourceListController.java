package org.example.controller;

import java.io.File;
import java.io.IOException;
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
import org.example.model.User;
import org.example.service.CommentaireService;
import org.example.service.ResourceService;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ResourceListController {
    @FXML private TextField searchField;
    @FXML private TableView<Resource> resourceTable;
    @FXML private TableColumn<Resource, String> titleColumn;
    @FXML private TableColumn<Resource, String> typeColumn;
    @FXML private Button newResourceBtn;
    @FXML private Button deleteBtn;
    @FXML private Button editBtn;
    @FXML private Button viewBtn;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label totalValueLabel;
    @FXML private Label articleValueLabel;
    @FXML private Label videoValueLabel;
    @FXML private Label commentValueLabel;
    @FXML private PieChart resourcePieChart;

    private final ResourceService resourceService = new ResourceService();
    private final CommentaireService commentaireService = new CommentaireService();
    private final ObservableList<Resource> resourceList = FXCollections.observableArrayList();
    private User currentUser;
    private int currentUserId = 1;
    private boolean adminMode = true;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void setCurrentUserId(int currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            this.currentUserId = user.getId();
        }
    }

    public void setAdminMode(boolean adminMode) {
        this.adminMode = adminMode;
        if (newResourceBtn != null) {
            newResourceBtn.setVisible(adminMode);
            newResourceBtn.setManaged(adminMode);
        }
        if (editBtn != null) {
            editBtn.setVisible(adminMode);
            editBtn.setManaged(adminMode);
        }
        if (deleteBtn != null) {
            deleteBtn.setVisible(adminMode);
            deleteBtn.setManaged(adminMode);
        }
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        setupFilters();
        setupSort();
        refreshResources();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshResources());
        typeFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshResources());
        sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshResources());
    }

    private void setupFilters() {
        typeFilterCombo.setItems(FXCollections.observableArrayList(
                "Tous",
                "Articles",
                "Videos"
        ));
        typeFilterCombo.setValue("Tous");
    }

    private void setupTableColumns() {
        titleColumn.setCellValueFactory(cellData -> Bindings.createObjectBinding(() -> cellData.getValue().getTitle()));
        typeColumn.setCellValueFactory(cellData -> Bindings.createObjectBinding(() -> cellData.getValue().getType()));
    }

    private void setupSort() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Plus recent",
                "Plus ancien",
                "Titre A-Z",
                "Titre Z-A",
                "Type"
        ));
        sortCombo.setValue("Plus recent");
    }

    private void refreshResources() {
        try {
            String query = searchField.getText() == null ? "" : searchField.getText().trim();
            List<Resource> base = query.isEmpty()
                    ? resourceService.getAllResources()
                    : resourceService.searchResources(query);

            String typeFilter = typeFilterCombo.getValue();
            if ("Articles".equals(typeFilter)) {
                base.removeIf(r -> !Resource.TYPE_ARTICLE.equalsIgnoreCase(safe(r.getType())));
            } else if ("Videos".equals(typeFilter)) {
                base.removeIf(r -> !Resource.TYPE_VIDEO.equalsIgnoreCase(safe(r.getType())));
            }

            base.sort(resolveComparator(sortCombo.getValue()));
            resourceList.setAll(base);
            resourceTable.setItems(resourceList);
            updateStats(base);
        } catch (SQLException e) {
            showError("Erreur de recherche: " + e.getMessage());
        }
    }

    private Comparator<Resource> resolveComparator(String sortMode) {
        if (sortMode == null) {
            return Comparator.comparing(Resource::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        return switch (sortMode) {
            case "Plus ancien" -> Comparator.comparing(Resource::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "Titre A-Z" -> Comparator.comparing(r -> safe(r.getTitle()), String.CASE_INSENSITIVE_ORDER);
            case "Titre Z-A" -> Comparator.comparing((Resource r) -> safe(r.getTitle()), String.CASE_INSENSITIVE_ORDER).reversed();
                case "Type" -> Comparator.comparing((Resource r) -> safe(r.getType()), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(r -> safe(r.getTitle()), String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(Resource::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }

    private void updateStats(List<Resource> resources) {
        long total = resources.size();
        long articles = resources.stream().filter(r -> Resource.TYPE_ARTICLE.equalsIgnoreCase(safe(r.getType()))).count();
        long videos = resources.stream().filter(r -> Resource.TYPE_VIDEO.equalsIgnoreCase(safe(r.getType()))).count();
        long totalComments = 0;
        try {
            for (Resource resource : resources) {
                totalComments += commentaireService.getCommentairesByResourceAll(resource.getId()).size();
            }
        } catch (SQLException ignored) {
            totalComments = 0;
        }

        totalValueLabel.setText(String.valueOf(total));
        articleValueLabel.setText(String.valueOf(articles));
        videoValueLabel.setText(String.valueOf(videos));
        commentValueLabel.setText(String.valueOf(totalComments));

        resourcePieChart.setTitle("Repartition des types");
        resourcePieChart.setData(FXCollections.observableArrayList(
            new PieChart.Data("Articles", articles),
            new PieChart.Data("Videos", videos)
        ));
    }

    @FXML
    private void handleNewResource() {
        openResourceEditWindow(null);
    }

    @FXML
    private void handleEditResource() {
        Resource selected = resourceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Selectionnez une ressource a modifier");
            return;
        }
        openResourceEditWindow(selected);
    }

    @FXML
    private void handleViewResource() {
        Resource selected = resourceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Selectionnez une ressource a consulter");
            return;
        }
        openResourceDetailWindow(selected);
    }

    @FXML
    private void handleDeleteResource() {
        Resource selected = resourceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Selectionnez une ressource a supprimer");
            return;
        }

        if (confirmDelete()) {
            try {
                resourceService.deleteResource(selected.getId());
                refreshResources();
                showInfo("Ressource supprimee avec succes");
            } catch (SQLException e) {
                showError("Erreur suppression: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportResources() {
        if (resourceList.isEmpty()) {
            showWarning("Aucune ressource a exporter.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les ressources (CSV)");
        chooser.setInitialFileName("ressources.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));
        File target = chooser.showSaveDialog(resourceTable.getScene().getWindow());
        if (target == null) {
            return;
        }

        StringBuilder csv = new StringBuilder("id,titre,type,created_at\n");
        for (Resource r : resourceList) {
            csv.append(r.getId()).append(',')
                    .append(csvEscape(r.getTitle())).append(',')
                    .append(csvEscape(r.getType())).append(',')
                    .append(csvEscape(r.getCreatedAt() == null ? "" : r.getCreatedAt().format(DATE_FMT)))
                    .append('\n');
        }

        try {
            Files.writeString(target.toPath(), csv.toString(), StandardCharsets.UTF_8);
            showInfo("Export ressources reussi: " + target.getName());
        } catch (IOException e) {
            showError("Erreur export ressources: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportComments() {
        try {
            List<Resource> allResources = resourceService.getAllResources();
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Exporter les commentaires (CSV)");
            chooser.setInitialFileName("commentaires.csv");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));
            File target = chooser.showSaveDialog(resourceTable.getScene().getWindow());
            if (target == null) {
                return;
            }

            StringBuilder csv = new StringBuilder("resource_id,resource_titre,comment_id,auteur,email,note,approved,created_at,contenu\n");
            int count = 0;
            for (Resource resource : allResources) {
                List<Commentaire> comments = commentaireService.getCommentairesByResourceAll(resource.getId());
                for (Commentaire c : comments) {
                    csv.append(resource.getId()).append(',')
                            .append(csvEscape(resource.getTitle())).append(',')
                            .append(c.getId()).append(',')
                            .append(csvEscape(c.getAuthorName())).append(',')
                            .append(csvEscape(c.getAuthorEmail())).append(',')
                            .append(c.getRating()).append(',')
                            .append(c.isApproved()).append(',')
                            .append(csvEscape(c.getCreatedAt() == null ? "" : c.getCreatedAt().format(DATE_FMT))).append(',')
                            .append(csvEscape(c.getContent()))
                            .append('\n');
                    count++;
                }
            }

            Files.writeString(target.toPath(), csv.toString(), StandardCharsets.UTF_8);
            showInfo("Export commentaires reussi: " + count + " commentaire(s)");
        } catch (SQLException | IOException e) {
            showError("Erreur export commentaires: " + e.getMessage());
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
    private void handleExportResourcesPdf() {
        if (resourceList.isEmpty()) {
            showWarning("Aucune ressource a exporter en PDF.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les ressources (PDF)");
        chooser.setInitialFileName("ressources.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        File target = chooser.showSaveDialog(resourceTable.getScene().getWindow());
        if (target == null) {
            return;
        }

        List<String> lines = new ArrayList<>();
        lines.add("Liste des ressources");
        lines.add(" ");
        for (Resource r : resourceList) {
            String date = r.getCreatedAt() == null ? "" : r.getCreatedAt().format(DATE_FMT);
            lines.add(String.format(Locale.ROOT, "#%d | %s | %s | %s", r.getId(), safe(r.getTitle()), safe(r.getType()), date));
        }

        try {
            writeSimplePdf(target, "Export Ressources", lines);
            showInfo("PDF ressources reussi: " + target.getName());
        } catch (IOException e) {
            showError("Erreur export PDF ressources: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportCommentsPdf() {
        try {
            List<Resource> allResources = resourceService.getAllResources();
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Exporter les commentaires (PDF)");
            chooser.setInitialFileName("commentaires.pdf");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
            File target = chooser.showSaveDialog(resourceTable.getScene().getWindow());
            if (target == null) {
                return;
            }

            List<String> lines = new ArrayList<>();
            lines.add("Liste des commentaires");
            lines.add(" ");
            int count = 0;
            for (Resource resource : allResources) {
                List<Commentaire> comments = commentaireService.getCommentairesByResourceAll(resource.getId());
                if (comments.isEmpty()) {
                    continue;
                }
                lines.add("Ressource #" + resource.getId() + " - " + safe(resource.getTitle()));
                for (Commentaire c : comments) {
                    String date = c.getCreatedAt() == null ? "" : c.getCreatedAt().format(DATE_FMT);
                    lines.add(String.format(Locale.ROOT, "  C#%d | %s | note %d/5 | %s", c.getId(), safe(c.getAuthorName()), c.getRating(), date));
                    lines.add("    " + safe(c.getContent()));
                    count++;
                }
                lines.add(" ");
            }

            if (count == 0) {
                showWarning("Aucun commentaire a exporter en PDF.");
                return;
            }

            writeSimplePdf(target, "Export Commentaires", lines);
            showInfo("PDF commentaires reussi: " + count + " commentaire(s)");
        } catch (SQLException | IOException e) {
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

    private void openResourceEditWindow(Resource resource) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/fxml/resource_form.fxml"));
            Stage stage = new Stage();
            stage.setTitle(resource == null ? "Nouvelle Ressource" : "Modifier Ressource");
            stage.setScene(new Scene(loader.load(), 720, 660));
            stage.setMinWidth(640);
            stage.setMinHeight(560);
            stage.setResizable(true);

            ResourceFormController controller = loader.getController();
            controller.setResource(resource);
            controller.setCurrentUserId(currentUserId);
            controller.setOnResourceSaved(() -> {
                refreshResources();
                stage.close();
            });

            stage.showAndWait();
        } catch (IOException e) {
            showError("Erreur ouverture formulaire: " + e.getMessage());
        }
    }

    private void openResourceDetailWindow(Resource resource) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/fxml/resource_detail.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Detail: " + resource.getTitle());
            stage.setScene(new Scene(loader.load(), 880, 700));
            stage.setMinWidth(760);
            stage.setMinHeight(560);
            stage.setResizable(true);

            ResourceDetailController controller = loader.getController();
            controller.setAdminMode(adminMode);
            controller.setCurrentUser(currentUser);
            controller.setCurrentUserId(currentUserId);
            controller.setResource(resource);

            stage.show();
        } catch (IOException e) {
            showError("Erreur ouverture detail: " + e.getMessage());
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
        alert.setTitle("Succes");
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
        alert.setContentText("Etes-vous sur? Les commentaires associes seront aussi supprimes.");
        return alert.showAndWait().filter(response -> response == ButtonType.OK).isPresent();
    }
}
