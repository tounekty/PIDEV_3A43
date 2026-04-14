package com.mindcare.controller.admin;

import com.mindcare.entities.SubTicket;
import com.mindcare.entities.Ticket;
import com.mindcare.services.SubTicketService;
import com.mindcare.services.TicketService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

import java.time.format.DateTimeFormatter;

public class GestionForumSujetsDetailsController {

    @FXML
    private Label ticketIdLabel;
    @FXML
    private Label subjectLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label priorityLabel;

    @FXML
    private TableView<SubTicket> messageTable;
    @FXML
    private TableColumn<SubTicket, String> colMessage;
    @FXML
    private TableColumn<SubTicket, String> colSender;
    @FXML
    private TableColumn<SubTicket, String> colRead;
    @FXML
    private TableColumn<SubTicket, String> colDate;

    @FXML
    private TextArea messageArea;

    private final SubTicketService subTicketService = new SubTicketService();
    private final TicketService ticketService = new TicketService();
    private final ObservableList<SubTicket> messages = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Ticket currentTicket;

    @FXML
    private void initialize() {
        colMessage.setCellValueFactory(cell -> new SimpleStringProperty(value(cell.getValue().getMessage())));
        colSender.setCellValueFactory(cell -> new SimpleStringProperty(value(cell.getValue().getSenderRole())));
        colRead.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isRead() ? "READ" : "UNREAD"));
        colDate.setCellValueFactory(cell -> {
            if (cell.getValue().getCreatedAt() == null) {
                return new SimpleStringProperty("-");
            }
            return new SimpleStringProperty(cell.getValue().getCreatedAt().format(DATE_TIME_FORMATTER));
        });
    }

    public void setTicket(Ticket ticket) {
        this.currentTicket = ticketService.findById(ticket.getId());
        if (this.currentTicket == null) {
            this.currentTicket = ticket;
        }

        ticketIdLabel.setText(String.valueOf(this.currentTicket.getId()));
        subjectLabel.setText(value(this.currentTicket.getSubject()));
        statusLabel.setText(this.currentTicket.getStatus().name());
        priorityLabel.setText(this.currentTicket.getPriority().name());

        loadMessages();
    }

    @FXML
    private void onSendMessage() {
        if (currentTicket == null) {
            return;
        }
        String text = messageArea.getText();
        if (text == null || text.isBlank()) {
            showError("Message required", "Write a message first");
            return;
        }

        try {
            SubTicket subTicket = new SubTicket();
            subTicket.setTicketId(currentTicket.getId());
            subTicket.setMessage(text.trim());
            subTicket.setSenderRole("ADMIN");
            subTicket.setInternal(false);
            subTicket.setRead(false);

            subTicketService.add(subTicket);
            messageArea.clear();
            loadMessages();
            System.out.println("[GestionForumSujetsDetailsController] Send message ticket=" + currentTicket.getId() + " messageId=" + subTicket.getId());
        } catch (Exception exception) {
            showError("Send message failed", exception.getMessage());
        }
    }

    @FXML
    private void onMarkAsRead() {
        SubTicket selected = messageTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No message selected", "Select a message first");
            return;
        }

        try {
            subTicketService.markAsRead(selected.getId());
            loadMessages();
            System.out.println("[GestionForumSujetsDetailsController] Mark message as read id=" + selected.getId());
        } catch (Exception exception) {
            showError("Mark as read failed", exception.getMessage());
        }
    }

    private void loadMessages() {
        if (currentTicket == null) {
            return;
        }
        try {
            messages.setAll(subTicketService.getByTicket(currentTicket.getId()));
            messageTable.setItems(messages);
        } catch (Exception exception) {
            showError("Load messages failed", exception.getMessage());
        }
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

