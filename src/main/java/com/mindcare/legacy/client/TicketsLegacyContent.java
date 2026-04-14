package com.mindcare.legacy.client;

import com.mindcare.view.client.*;

import com.mindcare.components.*;
import com.mindcare.model.Ticket;
import com.mindcare.service.MockDataService;
import com.mindcare.utils.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * TicketsLegacyContent Ã¢â‚¬â€œ client support ticket management.
 */
public class TicketsLegacyContent implements NavigationManager.Buildable {

    private final MockDataService data = MockDataService.getInstance();

    @Override
    public Node build() {
        return buildContent();
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(28);
        content.setPadding(new Insets(28, 32, 28, 32));

        // Header section
        VBox headerSection = buildHeaderSection();
        content.getChildren().add(headerSection);

        // Two-column layout: Form on left, Search on right
        HBox formSearchRow = new HBox(24);
        formSearchRow.setStyle("-fx-fill-height: true;");
        
        VBox formSection = buildCreateFormSection();
        VBox searchSection = buildSearchSection();
        
        HBox.setHgrow(formSection, Priority.ALWAYS);
        HBox.setHgrow(searchSection, Priority.ALWAYS);
        formSection.setMaxWidth(Double.MAX_VALUE);
        searchSection.setMaxWidth(Double.MAX_VALUE);
        
        formSearchRow.getChildren().addAll(formSection, searchSection);
        content.getChildren().add(formSearchRow);

        // Tickets list
        content.getChildren().add(buildTicketsList());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox buildHeaderSection() {
        Label title = new Label("Gestion Forum - Sujets");
        title.getStyleClass().add("page-title");
        
        Label sub = new Label("Get help with any issues on the platform");
        sub.getStyleClass().add("page-subtitle");
        
        return new VBox(6, title, sub);
    }

    private VBox buildCreateFormSection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        card.setMinHeight(380);

        Label formTitle = new Label("Report an Issue");
        formTitle.getStyleClass().add("section-title");

        TextField subject = new TextField();
        subject.setPromptText("Brief summary of your issue");
        subject.getStyleClass().add("text-field");
        subject.setMaxWidth(Double.MAX_VALUE);

        TextArea desc = new TextArea();
        desc.setPromptText("Describe your issue in detail...");
        desc.getStyleClass().add("text-area");
        desc.setPrefRowCount(5);
        desc.setWrapText(true);
        desc.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> priority = new ComboBox<>();
        priority.getItems().addAll("Low", "Medium", "High", "Urgent");
        priority.setValue("Medium");
        priority.setMaxWidth(Double.MAX_VALUE);
        priority.getStyleClass().add("combo-box");

        Button submit = new Button("Submit Ticket");
        submit.getStyleClass().addAll("btn", "btn-primary");
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setPrefHeight(36);

        VBox.setVgrow(desc, Priority.ALWAYS);

        card.getChildren().addAll(
            formTitle,
            new VBox(6, new Label("Subject") {{ getStyleClass().add("form-label"); }}, subject),
            new VBox(6, new Label("Description") {{ getStyleClass().add("form-label"); }}, desc),
            new VBox(6, new Label("Priority") {{ getStyleClass().add("form-label"); }}, priority),
            new Region() {{ setPrefHeight(8); }},
            submit
        );
        return card;
    }

    private VBox buildSearchSection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        card.setMinHeight(380);

        Label searchTitle = new Label("Search & Filter");
        searchTitle.getStyleClass().add("section-title");

        TextField searchField = new TextField();
        searchField.setPromptText("Search by subject or description...");
        searchField.getStyleClass().add("text-field");
        searchField.setMaxWidth(Double.MAX_VALUE);

        Label statusLabel = new Label("Status");
        statusLabel.getStyleClass().add("form-label");
        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All", "Open", "In Progress", "Resolved", "Closed");
        statusFilter.setValue("All");
        statusFilter.setMaxWidth(Double.MAX_VALUE);
        statusFilter.getStyleClass().add("combo-box");

        Label priorityLabel = new Label("Priority");
        priorityLabel.getStyleClass().add("form-label");
        ComboBox<String> priorityFilter = new ComboBox<>();
        priorityFilter.getItems().addAll("All", "Low", "Medium", "High", "Urgent");
        priorityFilter.setValue("All");
        priorityFilter.setMaxWidth(Double.MAX_VALUE);
        priorityFilter.getStyleClass().add("combo-box");

        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().addAll("btn", "btn-secondary");
        searchBtn.setMaxWidth(Double.MAX_VALUE);
        searchBtn.setPrefHeight(36);

        Button resetBtn = new Button("Reset Filters");
        resetBtn.getStyleClass().addAll("btn", "btn-secondary");
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.setPrefHeight(36);

        HBox buttonRow = new HBox(10, searchBtn, resetBtn);
        buttonRow.setStyle("-fx-fill-height: true;");
        HBox.setHgrow(searchBtn, Priority.ALWAYS);
        HBox.setHgrow(resetBtn, Priority.ALWAYS);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(
            searchTitle,
            new VBox(6, new Label("Keyword") {{ getStyleClass().add("form-label"); }}, searchField),
            new VBox(6, statusLabel, statusFilter),
            new VBox(6, priorityLabel, priorityFilter),
            spacer,
            buttonRow
        );
        return card;
    }



    private VBox buildTicketsList() {
        List<Ticket> tickets = data.getTickets().stream()
            .filter(t -> t.getUserName().equals("Alice Martin"))
            .toList();

        VBox card = new VBox(0);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(0));

        Label header = new Label("Your Support Tickets");
        header.getStyleClass().add("section-title");
        header.setPadding(new Insets(20));
        header.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        card.getChildren().add(header);

        if (tickets.isEmpty()) {
            VBox emptyBox = new VBox();
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40, 20, 40, 20));
            
            Label empty = new Label("No tickets found");
            empty.getStyleClass().add("label-muted");
            empty.setStyle("-fx-font-size: 14;");
            
            Label hint = new Label("Create a ticket above to report any issues");
            hint.getStyleClass().add("label-muted");
            hint.setStyle("-fx-font-size: 12;");
            
            emptyBox.getChildren().addAll(empty, hint);
            card.getChildren().add(emptyBox);
        } else {
            Separator separatorTop = new Separator();
            separatorTop.setStyle("-fx-padding: 0;");
            card.getChildren().add(separatorTop);
            
            for (Ticket ticket : tickets) {
                card.getChildren().add(buildTicketRow(ticket));
            }
        }

        return card;
    }

    private HBox buildTicketRow(Ticket ticket) {
        Label subject = new Label(ticket.getSubject());
        subject.getStyleClass().add("label-primary");
        subject.setStyle("-fx-font-weight: bold;");

        Label date = new Label(ticket.getCreatedAt());
        date.getStyleClass().add("label-muted");

        VBox textBox = new VBox(3, subject, date);

        BadgeLabel status   = BadgeLabel.forStatus(ticket.getStatus().name());
        BadgeLabel priority = buildPriorityBadge(ticket.getPriority());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(16, textBox, spacer, priority, status);
        row.setPadding(new Insets(14, 20, 14, 20));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color: transparent transparent #263040 transparent; -fx-border-width: 0 0 1 0;");
        return row;
    }

    private BadgeLabel buildPriorityBadge(Ticket.Priority priority) {
        return switch (priority) {
            case URGENT -> new BadgeLabel("URGENT", BadgeLabel.Style.DANGER);
            case HIGH   -> new BadgeLabel("HIGH",   BadgeLabel.Style.WARNING);
            case MEDIUM -> new BadgeLabel("MEDIUM", BadgeLabel.Style.INFO);
            case LOW    -> new BadgeLabel("LOW",    BadgeLabel.Style.NEUTRAL);
        };
    }
}


