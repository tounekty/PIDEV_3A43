package com.mindcare.controller.admin;

import com.mindcare.dao.UserDAO;
import com.mindcare.entities.CategoryTicket;
import com.mindcare.entities.Ticket;
import com.mindcare.model.User;
import com.mindcare.services.CategoryTicketService;
import com.mindcare.services.TicketService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Collectors;

public class GestionForumSujetsController {

    @FXML
    private TableView<Ticket> ticketTable;
    @FXML
    private TableColumn<Ticket, String> colId;
    @FXML
    private TableColumn<Ticket, String> colSubject;
    @FXML
    private TableColumn<Ticket, String> colStatus;
    @FXML
    private TableColumn<Ticket, String> colPriority;
    @FXML
    private TableColumn<Ticket, String> colCreatedAt;

    @FXML
    private TextField subjectField;
    @FXML
    private TextField searchField;
    @FXML
    private ChoiceBox<String> sortChoice;
    @FXML
    private ComboBox<User> userChoice;
    @FXML
    private ComboBox<CategoryTicket> categoryChoice;
    @FXML
    private ChoiceBox<Ticket.Status> statusChoice;
    @FXML
    private ChoiceBox<Ticket.Priority> priorityChoice;
    @FXML
    private TextArea resolutionArea;

    private final TicketService ticketService = new TicketService();
    private final CategoryTicketService categoryTicketService = new CategoryTicketService();
    private final UserDAO userDAO = new UserDAO();
    private final ObservableList<Ticket> tickets = FXCollections.observableArrayList();
    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final ObservableList<CategoryTicket> categories = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private void initialize() {
        colId.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getId())));
        colSubject.setCellValueFactory(cell -> new SimpleStringProperty(value(cell.getValue().getSubject())));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));
        colPriority.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPriority().name()));
        colCreatedAt.setCellValueFactory(cell -> {
            if (cell.getValue().getCreatedAt() == null) {
                return new SimpleStringProperty("-");
            }
            return new SimpleStringProperty(cell.getValue().getCreatedAt().format(DATE_TIME_FORMATTER));
        });

        colPriority.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                setStyle(switch (item) {
                    case "HIGH" -> "-fx-text-fill: #b91c1c; -fx-font-weight: bold;";
                    case "MEDIUM" -> "-fx-text-fill: #c2410c; -fx-font-weight: bold;";
                    case "LOW" -> "-fx-text-fill: #166534; -fx-font-weight: bold;";
                    default -> "-fx-font-weight: bold;";
                });
            }
        });

        statusChoice.setItems(FXCollections.observableArrayList(Ticket.Status.values()));
        priorityChoice.setItems(FXCollections.observableArrayList(Ticket.Priority.values()));
        statusChoice.setValue(Ticket.Status.OPEN);
        priorityChoice.setValue(Ticket.Priority.MEDIUM);

        sortChoice.setItems(FXCollections.observableArrayList("Newest", "Oldest", "Priority: HIGH->LOW", "Priority: LOW->HIGH"));
        sortChoice.setValue("Newest");
        sortChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> applySearchAndSort());

        searchField.textProperty().addListener((obs, oldVal, value) -> applySearchAndSort());

        loadUsers();
        loadCategories();

        ticketTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                subjectField.setText(selected.getSubject());
                statusChoice.setValue(selected.getStatus());
                priorityChoice.setValue(selected.getPriority());
                resolutionArea.setText(value(selected.getDescription() != null ? selected.getDescription() : selected.getResolution()));
                if (selected.getUserId() != null) {
                    users.stream()
                        .filter(user -> user.getId() == selected.getUserId())
                        .findFirst()
                        .ifPresent(user -> userChoice.setValue(user));
                }
                if (selected.getCategoryId() != null) {
                    categories.stream()
                        .filter(cat -> cat.getId() == selected.getCategoryId())
                        .findFirst()
                        .ifPresent(cat -> categoryChoice.setValue(cat));
                }
            }
        });

        loadTickets();
    }

    @FXML
    private void onAddTicket() {
        try {
            Ticket ticket = new Ticket();
            ticket.setSubject(required(subjectField.getText(), "Subject is required"));
            ticket.setUserId(requiredUserId());
            if (categoryChoice.getValue() != null) {
                ticket.setCategoryId(categoryChoice.getValue().getId());
            }
            ticket.setStatus(optionalStatus(statusChoice.getValue()));
            ticket.setPriority(optionalPriority(priorityChoice.getValue()));
            ticket.setDescription(value(resolutionArea.getText()));
            ticket.setResolution(value(resolutionArea.getText()));
            ticket.setMessageCount(0);

            ticketService.add(ticket);
            System.out.println("[GestionForumSujetsController] Add ticket action id=" + ticket.getId());
            clearForm();
            loadTickets();
        } catch (Exception exception) {
            showError("Add ticket failed", rootMessage(exception));
        }
    }

    @FXML
    private void onUpdateTicket() {
        Ticket selected = ticketTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No selection", "Select a ticket first");
            return;
        }

        try {
            Ticket updated = new Ticket();
            updated.setId(selected.getId());
            updated.setSubject(required(subjectField.getText(), "Subject is required"));
            updated.setUserId(requiredUserId());
            if (categoryChoice.getValue() != null) {
                updated.setCategoryId(categoryChoice.getValue().getId());
            }
            updated.setStatus(optionalStatus(statusChoice.getValue()));
            updated.setPriority(optionalPriority(priorityChoice.getValue()));
            updated.setDescription(value(resolutionArea.getText()));
            updated.setResolution(value(resolutionArea.getText()));

            ticketService.update(updated);
            System.out.println("[GestionForumSujetsController] Update ticket action id=" + updated.getId());
            loadTickets();
        } catch (IllegalArgumentException exception) {
            showError("Invalid update", exception.getMessage());
        } catch (Exception exception) {
            showError("Update ticket failed", rootMessage(exception));
        }
    }

    @FXML
    private void onDeleteTicket() {
        Ticket selected = ticketTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No selection", "Select a ticket first");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete ticket #" + selected.getId() + "?", ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) {
            return;
        }

        try {
            ticketService.delete(selected.getId());
            System.out.println("[GestionForumSujetsController] Delete ticket action id=" + selected.getId());
            clearForm();
            loadTickets();
        } catch (Exception exception) {
            showError("Delete ticket failed", rootMessage(exception));
        }
    }

    @FXML
    private void onOpenTicket() {
        Ticket selected = ticketTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No selection", "Select a ticket first");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mindcare/view/admin/GestionForumSujetsDetails.fxml"));
            Parent root = loader.load();

            GestionForumSujetsDetailsController detailsController = loader.getController();
            detailsController.setTicket(selected);

            Stage stage = new Stage();
            stage.setTitle("Ticket #" + selected.getId());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadTickets();
            System.out.println("[GestionForumSujetsController] Open ticket action id=" + selected.getId());
        } catch (IOException exception) {
            showError("Open details failed", rootMessage(exception));
        }
    }

    private void loadTickets() {
        try {
            tickets.setAll(ticketService.getAll());
            applySearchAndSort();
        } catch (Exception exception) {
            showError("Load tickets failed", rootMessage(exception));
        }
    }

    private void loadUsers() {
        try {
            users.setAll(userDAO.getAllUsers());
            userChoice.setItems(users);
            if (!users.isEmpty()) {
                userChoice.setValue(users.get(0));
            }
        } catch (Exception exception) {
            showError("Load users failed", rootMessage(exception));
        }
    }

    private void loadCategories() {
        try {
            categories.setAll(categoryTicketService.getAll());
            categoryChoice.setItems(categories);
            if (!categories.isEmpty()) {
                categoryChoice.setValue(categories.get(0));
            }
        } catch (Exception exception) {
            showError("Load categories failed", rootMessage(exception));
        }
    }

    private Integer requiredUserId() {
        User selectedUser = userChoice.getValue();
        if (selectedUser == null || selectedUser.getId() <= 0) {
            throw new IllegalArgumentException("Please select a valid user");
        }
        return selectedUser.getId();
    }

    private void applySearchAndSort() {
        String search = value(searchField.getText()).trim().toLowerCase(Locale.ROOT);
        List<Ticket> filtered = tickets.stream()
            .filter(ticket -> matchesSearch(ticket, search))
            .collect(Collectors.toList());

        String sort = sortChoice.getValue();
        Comparator<Ticket> comparator;
        if ("Oldest".equals(sort)) {
            comparator = Comparator.comparing(Ticket::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        } else if ("Priority: HIGH->LOW".equals(sort)) {
            comparator = Comparator.comparingInt(this::priorityWeight).reversed();
        } else if ("Priority: LOW->HIGH".equals(sort)) {
            comparator = Comparator.comparingInt(this::priorityWeight);
        } else {
            comparator = Comparator.comparing(Ticket::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }

        filtered.sort(comparator);
        ticketTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private boolean matchesSearch(Ticket ticket, String search) {
        if (search.isBlank()) {
            return true;
        }
        return String.valueOf(ticket.getId()).contains(search)
            || value(ticket.getSubject()).toLowerCase(Locale.ROOT).contains(search)
            || ticket.getStatus().name().toLowerCase(Locale.ROOT).contains(search)
            || ticket.getPriority().name().toLowerCase(Locale.ROOT).contains(search);
    }

    private int priorityWeight(Ticket ticket) {
        return switch (ticket.getPriority()) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
        };
    }

    private Ticket.Status optionalStatus(Ticket.Status status) {
        return status == null ? Ticket.Status.OPEN : status;
    }

    private Ticket.Priority optionalPriority(Ticket.Priority priority) {
        return priority == null ? Ticket.Priority.MEDIUM : priority;
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private void clearForm() {
        subjectField.clear();
        resolutionArea.clear();
        statusChoice.setValue(Ticket.Status.OPEN);
        priorityChoice.setValue(Ticket.Priority.MEDIUM);
        if (!users.isEmpty()) {
            userChoice.setValue(users.get(0));
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "Unknown error" : current.getMessage();
    }
}

