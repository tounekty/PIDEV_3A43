package com.mindcare.legacy.client;

import com.mindcare.view.client.*;

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

import java.util.List;

/**
 * ServiceRequestListLegacyContent Ã¢â‚¬â€œ client's list of posted service requests.
 */
public class ServiceRequestListLegacyContent implements NavigationManager.Buildable {

    private final MockDataService data = MockDataService.getInstance();

    @Override
    public Node build() {
        return buildContent();
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(0));

        // Header
        Label title = new Label("My Service Requests");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Manage and track all your posted service requests");
        sub.getStyleClass().add("page-subtitle");

        Button addBtn = new Button("+ New Request");
        addBtn.getStyleClass().addAll("btn", "btn-primary");
        addBtn.setOnAction(e -> NavigationManager.getInstance().navigateTo(new CreateServiceRequestView()));

        HBox headerRow = new HBox(title);
        HBox.setHgrow(new Region(), Priority.ALWAYS);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerRow.getChildren().addAll(spacer, addBtn);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(new VBox(6, title, sub), headerRow.getChildren().isEmpty() ? addBtn : addBtn, buildTable());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox buildTable() {
        List<ServiceRequest> items = data.getServiceRequests().stream()
            .filter(r -> r.getClientName().equals("Alice Martin"))
            .toList();

        TableView<ServiceRequest> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(420);

        TableColumn<ServiceRequest, String> titleCol  = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setMinWidth(180);

        TableColumn<ServiceRequest, String> catCol    = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        catCol.setMinWidth(120);

        TableColumn<ServiceRequest, String> budgetCol = new TableColumn<>("Budget");
        budgetCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getBudgetFormatted()));
        budgetCol.setMinWidth(80);

        TableColumn<ServiceRequest, String> deadlineCol = new TableColumn<>("Deadline");
        deadlineCol.setCellValueFactory(new PropertyValueFactory<>("deadline"));
        deadlineCol.setMinWidth(100);

        TableColumn<ServiceRequest, String> offersCol = new TableColumn<>("Gestion Events");
        offersCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().getOffersCount())));
        offersCol.setMinWidth(60);

        TableColumn<ServiceRequest, Void> statusCol = new TableColumn<>("Status");
        statusCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                setGraphic(BadgeLabel.forStatus(getTableRow().getItem().getStatus().name()));
            }
        });
        statusCol.setMinWidth(100);

        table.getColumns().addAll(titleCol, catCol, budgetCol, deadlineCol, offersCol, statusCol);
        table.getItems().addAll(items);

        VBox card = new VBox(table);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(0));
        return card;
    }
}


