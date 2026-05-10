package com.mindcare.view.client;

import com.mindcare.utils.NavigationManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import org.example.model.Resource;
import org.example.service.ResourceService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ResourcesView implements NavigationManager.Buildable {

    private final ResourceService resourceService = new ResourceService();
    private final ObservableList<Resource> resources = FXCollections.observableArrayList();

    private TableView<Resource> table;
    private TextField titleField;
    private TextArea descriptionArea;
    private ComboBox<String> typeField;
    private TextField filePathField;
    private TextField videoUrlField;
    private TextField imageUrlField;

    private Button createBtn;
    private Button updateBtn;
    private Button deleteBtn;
    private Button clearBtn;

    private Label statusLabel;

    @Override
    public Node build() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(18));

        Label title = new Label("Resources");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 700;");

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #64748b;");

        VBox header = new VBox(6, title, statusLabel);
        header.setPadding(new Insets(0, 0, 12, 0));

        root.setTop(header);

        table = buildTable();
        VBox tableBox = new VBox(10, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        Node form = buildForm();

        SplitPane split = new SplitPane(tableBox, form);
        split.setDividerPositions(0.62);
        root.setCenter(split);

        reload();

        return root;
    }

    private TableView<Resource> buildTable() {
        TableView<Resource> tv = new TableView<>();
        tv.setItems(resources);

        TableColumn<Resource, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<Resource, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(180);

        TableColumn<Resource, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(200);

        TableColumn<Resource, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(100);

        TableColumn<Resource, LocalDateTime> dateCol = new TableColumn<>("Created");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        dateCol.setPrefWidth(140);
        dateCol.setCellFactory(col -> new TableCell<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(fmt));
                }
            }
        });

        tv.getColumns().addAll(idCol, titleCol, descCol, typeCol, dateCol);

        tv.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected != null) {
                populateForm(selected);
            }
        });

        return tv;
    }

    private Node buildForm() {
        VBox form = new VBox(10);
        form.setPadding(new Insets(0, 0, 0, 14));

        titleField = new TextField();
        titleField.setPromptText("Resource title");

        descriptionArea = new TextArea();
        descriptionArea.setPromptText("Resource description...");
        descriptionArea.setPrefRowCount(3);

        typeField = new ComboBox<>(FXCollections.observableArrayList(
                Resource.TYPE_ARTICLE, Resource.TYPE_VIDEO
        ));
        typeField.setPromptText("Type");

        filePathField = new TextField();
        filePathField.setPromptText("File path (optional)");

        videoUrlField = new TextField();
        videoUrlField.setPromptText("Video URL (optional)");

        imageUrlField = new TextField();
        imageUrlField.setPromptText("Image URL (optional)");

        createBtn = new Button("Create");
        updateBtn = new Button("Update");
        deleteBtn = new Button("Delete");
        clearBtn = new Button("Clear");

        createBtn.setMaxWidth(Double.MAX_VALUE);
        updateBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setMaxWidth(Double.MAX_VALUE);

        createBtn.getStyleClass().addAll("btn", "btn-primary");
        updateBtn.getStyleClass().addAll("btn", "btn-secondary");
        deleteBtn.getStyleClass().addAll("btn", "btn-danger");
        clearBtn.getStyleClass().addAll("btn", "btn-secondary");

        createBtn.setOnAction(e -> onCreate());
        updateBtn.setOnAction(e -> onUpdate());
        deleteBtn.setOnAction(e -> onDelete());
        clearBtn.setOnAction(e -> clearForm());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(35);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(65);
        grid.getColumnConstraints().addAll(c1, c2);

        int r = 0;
        grid.add(label("Title"), 0, r);
        grid.add(titleField, 1, r++);

        grid.add(label("Description"), 0, r);
        grid.add(descriptionArea, 1, r++);

        grid.add(label("Type"), 0, r);
        grid.add(typeField, 1, r++);

        grid.add(label("File path"), 0, r);
        grid.add(filePathField, 1, r++);

        grid.add(label("Video URL"), 0, r);
        grid.add(videoUrlField, 1, r++);

        grid.add(label("Image URL"), 0, r);
        grid.add(imageUrlField, 1, r++);

        HBox buttonsRow = new HBox(10, createBtn, updateBtn, deleteBtn, clearBtn);
        buttonsRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(createBtn, Priority.ALWAYS);
        HBox.setHgrow(updateBtn, Priority.ALWAYS);
        HBox.setHgrow(deleteBtn, Priority.ALWAYS);
        HBox.setHgrow(clearBtn, Priority.ALWAYS);

        form.getChildren().addAll(grid, buttonsRow);
        VBox.setVgrow(descriptionArea, Priority.ALWAYS);

        return new ScrollPane(form);
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
    }

    private void reload() {
        try {
            List<Resource> all = resourceService.getAllResources();
            resources.setAll(all);
            statusLabel.setText("Loaded " + all.size() + " resources");
        } catch (SQLException e) {
            statusLabel.setText("Failed to load resources: " + e.getMessage());
        }
    }

    private void populateForm(Resource resource) {
        titleField.setText(resource.getTitle());
        descriptionArea.setText(resource.getDescription());
        typeField.setValue(resource.getType());
        filePathField.setText(resource.getFilePath());
        videoUrlField.setText(resource.getVideoUrl());
        imageUrlField.setText(resource.getImageUrl());
    }

    private void clearForm() {
        table.getSelectionModel().clearSelection();
        titleField.clear();
        descriptionArea.clear();
        typeField.setValue(null);
        filePathField.clear();
        videoUrlField.clear();
        imageUrlField.clear();
    }

    private void onCreate() {
        try {
            Resource resource = new Resource();
            resource.setTitle(titleField.getText());
            resource.setDescription(descriptionArea.getText());
            resource.setType(typeField.getValue());
            resource.setFilePath(safeTrim(filePathField.getText()));
            resource.setVideoUrl(safeTrim(videoUrlField.getText()));
            resource.setImageUrl(safeTrim(imageUrlField.getText()));
            resource.setCreatedAt(LocalDateTime.now());
            resource.setUserId(1);

            resourceService.createResource(resource);
            statusLabel.setText("Resource created successfully");
            reload();
            clearForm();
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void onUpdate() {
        Resource selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a resource to update.");
            return;
        }

        try {
            selected.setTitle(titleField.getText());
            selected.setDescription(descriptionArea.getText());
            selected.setType(typeField.getValue());
            selected.setFilePath(safeTrim(filePathField.getText()));
            selected.setVideoUrl(safeTrim(videoUrlField.getText()));
            selected.setImageUrl(safeTrim(imageUrlField.getText()));

            resourceService.updateResource(selected);
            statusLabel.setText("Resource updated successfully");
            reload();
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void onDelete() {
        Resource selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a resource to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Resource");
        confirm.setHeaderText("Delete resource \"" + selected.getTitle() + "\"?");
        confirm.setContentText("This action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            resourceService.deleteResource(selected.getId());
            statusLabel.setText("Resource deleted successfully");
            reload();
            clearForm();
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private String safeTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
