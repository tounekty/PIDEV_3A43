package com.mindcare.legacy.admin;

import com.mindcare.view.admin.*;

import com.mindcare.components.*;
import com.mindcare.model.ServiceRequest;
import com.mindcare.service.MockDataService;
import com.mindcare.utils.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

/**
 * GestionCommentairesLegacyContent Ã¢â‚¬â€œ admin oversight of all service requests.
 */
public class GestionCommentairesLegacyContent implements NavigationManager.Buildable {

    private final MockDataService data = MockDataService.getInstance();

    @Override
    public Node build() {
        return buildContent();
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);

        Label title = new Label("Gestion Commentaires");
        title.getStyleClass().add("page-title");
        Label sub   = new Label("View and moderate all client service requests");
        sub.getStyleClass().add("page-subtitle");

        content.getChildren().addAll(new VBox(6, title, sub), buildFilterBar(), buildTable());
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private HBox buildFilterBar() {
        TextField search = new TextField();
        search.setPromptText("Search...");
        search.getStyleClass().add("filter-input");
        HBox.setHgrow(search, Priority.ALWAYS);

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All Statuses", "OPEN", "IN_PROGRESS", "COMPLETED", "CANCELLED");
        statusFilter.setValue("All Statuses");
        statusFilter.getStyleClass().add("combo-box");

        HBox bar = new HBox(12, search, statusFilter);
        bar.getStyleClass().add("filter-bar");
        return bar;
    }

    private VBox buildTable() {
        TableView<ServiceRequest> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(420);

        TableColumn<ServiceRequest, String> idCol     = new TableColumn<>("#");
        idCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().getId())));
        idCol.setMinWidth(40);

        TableColumn<ServiceRequest, String> titleCol  = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setMinWidth(160);

        TableColumn<ServiceRequest, String> clientCol = new TableColumn<>("Client");
        clientCol.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        clientCol.setMinWidth(120);

        TableColumn<ServiceRequest, String> catCol    = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        catCol.setMinWidth(120);

        TableColumn<ServiceRequest, String> budgetCol = new TableColumn<>("Budget");
        budgetCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getBudgetFormatted()));
        budgetCol.setMinWidth(80);

        TableColumn<ServiceRequest, Void>   statusCol = new TableColumn<>("Status");
        statusCol.setMinWidth(100);
        statusCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                setGraphic(BadgeLabel.forStatus(getTableRow().getItem().getStatus().name()));
            }
        });

        TableColumn<ServiceRequest, Void>   actionsCol = new TableColumn<>("Actions");
        actionsCol.setMinWidth(120);
        actionsCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Button viewBtn = new Button("View"); viewBtn.getStyleClass().addAll("btn","btn-secondary","btn-sm");
                Button delBtn  = new Button("Delete"); delBtn.getStyleClass().addAll("btn","btn-danger","btn-sm");
                setGraphic(new HBox(6, viewBtn, delBtn));
            }
        });

        table.getColumns().addAll(idCol, titleCol, clientCol, catCol, budgetCol, statusCol, actionsCol);
        table.getItems().addAll(data.getServiceRequests());

        VBox card = new VBox(table);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(0));
        return card;
    }
}


