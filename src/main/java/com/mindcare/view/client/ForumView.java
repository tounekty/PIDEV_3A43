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
import org.example.model.ForumSubject;
import org.example.service.ForumService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ForumView implements NavigationManager.Buildable {

    private final ForumService forumService = new ForumService();
    private final ObservableList<ForumSubject> subjects = FXCollections.observableArrayList();

    private TableView<ForumSubject> table;
    private TextField titleField;
    private TextArea descriptionArea;
    private TextField categoryField;
    private CheckBox pinnedCheckBox;
    private CheckBox anonymousCheckBox;
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

        Label title = new Label("Forum");
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

    private TableView<ForumSubject> buildTable() {
        TableView<ForumSubject> tv = new TableView<>();
        tv.setItems(subjects);

        TableColumn<ForumSubject, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<ForumSubject, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("titre"));
        titleCol.setPrefWidth(150);

        TableColumn<ForumSubject, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(200);

        TableColumn<ForumSubject, LocalDateTime> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
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

        TableColumn<ForumSubject, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(100);

        TableColumn<ForumSubject, Integer> likesCol = new TableColumn<>("Likes");
        likesCol.setCellValueFactory(new PropertyValueFactory<>("likeCount"));
        likesCol.setPrefWidth(60);

        TableColumn<ForumSubject, Integer> messagesCol = new TableColumn<>("Messages");
        messagesCol.setCellValueFactory(new PropertyValueFactory<>("messageCount"));
        messagesCol.setPrefWidth(80);

        tv.getColumns().addAll(idCol, titleCol, descCol, dateCol, categoryCol, likesCol, messagesCol);

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
        titleField.setPromptText("Subject title");

        descriptionArea = new TextArea();
        descriptionArea.setPromptText("Subject description...");
        descriptionArea.setPrefRowCount(3);

        categoryField = new TextField();
        categoryField.setPromptText("Category");

        imageUrlField = new TextField();
        imageUrlField.setPromptText("Image URL (optional)");

        pinnedCheckBox = new CheckBox("Pinned");
        anonymousCheckBox = new CheckBox("Anonymous");

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

        grid.add(label("Category"), 0, r);
        grid.add(categoryField, 1, r++);

        grid.add(label("Image URL"), 0, r);
        grid.add(imageUrlField, 1, r++);

        grid.add(new HBox(10, pinnedCheckBox, anonymousCheckBox), 0, r++);
        GridPane.setColumnSpan(new HBox(10, pinnedCheckBox, anonymousCheckBox), 2);

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
            List<ForumSubject> all = forumService.getAllSubjects();
            subjects.setAll(all);
            statusLabel.setText("Loaded " + all.size() + " forum subjects");
        } catch (SQLException e) {
            statusLabel.setText("Failed to load forum: " + e.getMessage());
        }
    }

    private void populateForm(ForumSubject subject) {
        titleField.setText(subject.getTitre());
        descriptionArea.setText(subject.getDescription());
        categoryField.setText(subject.getCategory());
        imageUrlField.setText(subject.getImageUrl());
        pinnedCheckBox.setSelected(subject.isPinned());
        anonymousCheckBox.setSelected(subject.isAnonymous());
    }

    private void clearForm() {
        table.getSelectionModel().clearSelection();
        titleField.clear();
        descriptionArea.clear();
        categoryField.clear();
        imageUrlField.clear();
        pinnedCheckBox.setSelected(false);
        anonymousCheckBox.setSelected(false);
    }

    private void onCreate() {
        try {
            ForumSubject subject = new ForumSubject();
            subject.setTitre(titleField.getText());
            subject.setDescription(descriptionArea.getText());
            subject.setCategory(categoryField.getText());
            subject.setImageUrl(safeTrim(imageUrlField.getText()));
            subject.setPinned(pinnedCheckBox.isSelected());
            subject.setAnonymous(anonymousCheckBox.isSelected());
            subject.setDateCreation(LocalDateTime.now());
            subject.setIdUser(null);

            forumService.addSubject(subject);
            statusLabel.setText("Subject created successfully");
            reload();
            clearForm();
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void onUpdate() {
        ForumSubject selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a subject to update.");
            return;
        }

        try {
            selected.setTitre(titleField.getText());
            selected.setDescription(descriptionArea.getText());
            selected.setCategory(categoryField.getText());
            selected.setImageUrl(safeTrim(imageUrlField.getText()));
            selected.setPinned(pinnedCheckBox.isSelected());
            selected.setAnonymous(anonymousCheckBox.isSelected());

            forumService.updateSubject(selected);
            statusLabel.setText("Subject updated successfully");
            reload();
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void onDelete() {
        ForumSubject selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a subject to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Subject");
        confirm.setHeaderText("Delete subject \"" + selected.getTitre() + "\"?");
        confirm.setContentText("This action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            forumService.deleteSubject(selected.getId());
            statusLabel.setText("Subject deleted successfully");
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
