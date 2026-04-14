package com.mindcare.legacy.admin;

import com.mindcare.view.admin.*;

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
 * GestionForumSujetsLegacyContent Ã¢â‚¬â€œ admin ticket management with respond and close actions.
 */
public class GestionForumSujetsLegacyContent implements NavigationManager.Buildable {

    private final MockDataService data = MockDataService.getInstance();

    @Override
    public Node build() {
        return buildContent();
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);

        Label title = new Label("Gestion Forum - Sujets");
        title.getStyleClass().add("page-title");
        Label sub   = new Label("Manage and respond to all platform support requests");
        sub.getStyleClass().add("page-subtitle");

        content.getChildren().addAll(new VBox(6, title, sub), buildKpiRow(), buildTicketTable());
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private HBox buildKpiRow() {
        long open     = data.getTickets().stream().filter(t -> t.getStatus() == Ticket.Status.OPEN).count();
        long progress = data.getTickets().stream().filter(t -> t.getStatus() == Ticket.Status.IN_PROGRESS).count();
        long resolved = data.getTickets().stream().filter(t -> t.getStatus() == Ticket.Status.RESOLVED || t.getStatus() == Ticket.Status.CLOSED).count();

        HBox row = new HBox(16,
            new StatCard("Open Tickets",       String.valueOf(open),     org.kordamp.ikonli.feather.Feather.HELP_CIRCLE, StatCard.Color.RED),
            new StatCard("In Progress",        String.valueOf(progress), org.kordamp.ikonli.feather.Feather.ACTIVITY,    StatCard.Color.ORANGE),
            new StatCard("Resolved / Closed",  String.valueOf(resolved), org.kordamp.ikonli.feather.Feather.CHECK_CIRCLE,StatCard.Color.GREEN)
        );
        row.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        return row;
    }

    private VBox buildTicketTable() {
        TableView<Ticket> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(400);

        TableColumn<Ticket, String> subjectCol  = new TableColumn<>("Subject");
        subjectCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("subject"));
        subjectCol.setMinWidth(180);

        TableColumn<Ticket, String> userCol     = new TableColumn<>("User");
        userCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("userName"));
        userCol.setMinWidth(120);

        TableColumn<Ticket, Void>   priorityCol = new TableColumn<>("Priority");
        priorityCol.setMinWidth(90);
        priorityCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                Ticket t = getTableRow().getItem();
                BadgeLabel badge = switch (t.getPriority()) {
                    case URGENT -> new BadgeLabel("URGENT", BadgeLabel.Style.DANGER);
                    case HIGH   -> new BadgeLabel("HIGH",   BadgeLabel.Style.WARNING);
                    case MEDIUM -> new BadgeLabel("MEDIUM", BadgeLabel.Style.INFO);
                    case LOW    -> new BadgeLabel("LOW",    BadgeLabel.Style.NEUTRAL);
                };
                setGraphic(badge);
            }
        });

        TableColumn<Ticket, Void>   statusCol   = new TableColumn<>("Status");
        statusCol.setMinWidth(110);
        statusCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                setGraphic(BadgeLabel.forStatus(getTableRow().getItem().getStatus().name()));
            }
        });

        TableColumn<Ticket, String> dateCol     = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("createdAt"));
        dateCol.setMinWidth(90);

        TableColumn<Ticket, Void>   actionsCol   = new TableColumn<>("Actions");
        actionsCol.setMinWidth(200);
        actionsCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Button respondBtn = new Button("Respond"); respondBtn.getStyleClass().addAll("btn","btn-primary","btn-sm");
                Button closeBtn   = new Button("Close");   closeBtn.getStyleClass().addAll("btn","btn-secondary","btn-sm");
                setGraphic(new HBox(8, respondBtn, closeBtn));
            }
        });

        table.getColumns().addAll(subjectCol, userCol, priorityCol, statusCol, dateCol, actionsCol);
        table.getItems().addAll(data.getTickets());

        VBox card = new VBox(table);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(0));
        return card;
    }
}


