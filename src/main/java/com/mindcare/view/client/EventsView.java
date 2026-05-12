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
import org.example.model.Event;
import org.example.service.EventService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EventsView implements NavigationManager.Buildable {

    private final EventService eventService = new EventService();
    private final ObservableList<Event> events = FXCollections.observableArrayList();

    private TableView<Event> table;
    private TextField titleField;
    private TextArea descriptionArea;
    private TextField locationField;
    private DatePicker datePicker;
    private TextField timeField;
    private TextField capacityField;
    private ComboBox<String> categoryField;
    private TextField imageField;

    private Button createBtn;
    private Button updateBtn;
    private Button deleteBtn;
    private Button clearBtn;

    private Label statusLabel;

    @Override
    public Node build() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(18));

        Label title = new Label("Events");
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

    private TableView<Event> buildTable() {
        TableView<Event> tv = new TableView<>();
        tv.setItems(events);

        TableColumn<Event, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("titre"));
        titleCol.setPrefWidth(150);

        TableColumn<Event, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(200);

        TableColumn<Event, LocalDateTime> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("dateEvent"));
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

        TableColumn<Event, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        locationCol.setPrefWidth(120);

        TableColumn<Event, Integer> capacityCol = new TableColumn<>("Capacity");
        capacityCol.setCellValueFactory(new PropertyValueFactory<>("capacite"));
        capacityCol.setPrefWidth(80);

        TableColumn<Event, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        categoryCol.setPrefWidth(100);

        tv.getColumns().addAll(titleCol, descCol, dateCol, locationCol, capacityCol, categoryCol);

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
        titleField.setPromptText("Event title");

        descriptionArea = new TextArea();
        descriptionArea.setPromptText("Event description...");
        descriptionArea.setPrefRowCount(3);

        locationField = new TextField();
        locationField.setPromptText("Location");

        datePicker = new DatePicker(LocalDate.now());
        datePicker.setConverter(new StringConverter<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            public String toString(LocalDate object) {
                return object == null ? "" : object.format(fmt);
            }

            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.isBlank()) return null;
                return LocalDate.parse(string, fmt);
            }
        });

        timeField = new TextField();
        timeField.setPromptText("HH:mm");

        capacityField = new TextField();
        capacityField.setPromptText("Capacity");

        categoryField = new ComboBox<>(FXCollections.observableArrayList(
                "Workshop", "Seminar", "Conference", "Social", "Sports", "Other"
        ));
        categoryField.setPromptText("Category");

        imageField = new TextField();
        imageField.setPromptText("Image URL (optional)");

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

        grid.add(label("Location"), 0, r);
        grid.add(locationField, 1, r++);

        grid.add(label("Date"), 0, r);
        grid.add(datePicker, 1, r++);

        grid.add(label("Time"), 0, r);
        grid.add(timeField, 1, r++);

        grid.add(label("Capacity"), 0, r);
        grid.add(capacityField, 1, r++);

        grid.add(label("Category"), 0, r);
        grid.add(categoryField, 1, r++);

        grid.add(label("Image URL"), 0, r);
        grid.add(imageField, 1, r++);

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
            List<Event> all = eventService.getAllEvents();
            events.setAll(all);
            statusLabel.setText("Loaded " + all.size() + " events");
        } catch (SQLException e) {
            statusLabel.setText("Failed to load events: " + e.getMessage());
        }
    }

    private void populateForm(Event event) {
        titleField.setText(event.getTitre());
        descriptionArea.setText(event.getDescription());
        locationField.setText(event.getLieu());
        if (event.getDateEvent() != null) {
            datePicker.setValue(event.getDateEvent().toLocalDate());
            timeField.setText(event.getDateEvent().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        capacityField.setText(String.valueOf(event.getCapacite()));
        categoryField.setValue(event.getCategorie());
        imageField.setText(event.getImage());
    }

    private void clearForm() {
        table.getSelectionModel().clearSelection();
        titleField.clear();
        descriptionArea.clear();
        locationField.clear();
        datePicker.setValue(LocalDate.now());
        timeField.clear();
        capacityField.clear();
        categoryField.setValue(null);
        imageField.clear();
    }

    private void onCreate() {
        try {
            LocalDateTime dateTime = parseDateTime(datePicker.getValue(), timeField.getText());
            int capacity = parseInt(capacityField.getText(), 50);

            Event event = new Event();
            event.setTitre(titleField.getText());
            event.setDescription(descriptionArea.getText());
            event.setLieu(locationField.getText());
            event.setDateEvent(dateTime);
            event.setCapacite(capacity);
            event.setCategorie(categoryField.getValue());
            event.setImage(safeTrim(imageField.getText()));
            event.setTitle(titleField.getText());
            event.setEventDate(dateTime);
            event.setLocation(locationField.getText());
            event.setIdUser(null);

            eventService.addEvent(event);
            statusLabel.setText("Event created successfully");
            reload();
            clearForm();
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void onUpdate() {
        Event selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select an event to update.");
            return;
        }

        try {
            LocalDateTime dateTime = parseDateTime(datePicker.getValue(), timeField.getText());
            int capacity = parseInt(capacityField.getText(), selected.getCapacite());

            selected.setTitre(titleField.getText());
            selected.setDescription(descriptionArea.getText());
            selected.setLieu(locationField.getText());
            selected.setDateEvent(dateTime);
            selected.setCapacite(capacity);
            selected.setCategorie(categoryField.getValue());
            selected.setImage(safeTrim(imageField.getText()));
            selected.setTitle(titleField.getText());
            selected.setEventDate(dateTime);
            selected.setLocation(locationField.getText());

            eventService.updateEvent(selected);
            statusLabel.setText("Event updated successfully");
            reload();
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void onDelete() {
        Event selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select an event to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Event");
        confirm.setHeaderText("Delete event \"" + selected.getTitre() + "\"?");
        confirm.setContentText("This action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            eventService.deleteEvent(selected.getId());
            statusLabel.setText("Event deleted successfully");
            reload();
            clearForm();
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private LocalDateTime parseDateTime(LocalDate date, String time) {
        LocalTime localTime = LocalTime.parse(time.isEmpty() ? "00:00" : time, DateTimeFormatter.ofPattern("HH:mm"));
        return LocalDateTime.of(date, localTime);
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
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
