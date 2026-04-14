package com.mindcare.legacy.client;

import com.mindcare.view.client.*;

import com.mindcare.components.*;
import com.mindcare.model.Contract;
import com.mindcare.model.ServiceRequest;
import com.mindcare.service.MockDataService;
import com.mindcare.utils.NavigationManager;
import com.mindcare.utils.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;

import java.util.List;
import java.util.Map;

/**
 * ClientDashboardLegacyContent â€“ main overview for clients.
 */
public class ClientDashboardLegacyContent implements NavigationManager.Buildable {

    private final MockDataService data = MockDataService.getInstance();

    @Override
    public Node build() {
        return buildContent();
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(28);
        content.setPadding(new Insets(28, 32, 28, 32));

        // Welcome header
        content.getChildren().add(buildWelcomeHeader());

        // KPI Cards
        content.getChildren().add(buildKpiRow());

        // Two-column area: recent requests + contracts
        FlowPane twoCol = new FlowPane();
        twoCol.setHgap(24);
        twoCol.setVgap(24);
        twoCol.setPrefWrapLength(1000);
        VBox requestsSection = wrapSection("Recent Service Requests", buildRequestsTable());
        VBox contractsSection = wrapSection("Active Contracts", buildContractsTable());
        requestsSection.setMinWidth(420);
        contractsSection.setMinWidth(420);
        requestsSection.setMaxWidth(Double.MAX_VALUE);
        contractsSection.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(requestsSection, Priority.ALWAYS);
        HBox.setHgrow(contractsSection, Priority.ALWAYS);
        twoCol.getChildren().addAll(
            requestsSection,
            contractsSection
        );
        content.getChildren().add(twoCol);

        // Quick actions
        content.getChildren().add(buildQuickActions());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox buildWelcomeHeader() {
        String name = SessionManager.getInstance().getCurrentUser().getFirstName();
        Label welcome = new Label("Welcome back, " + name);
        welcome.getStyleClass().add("page-title");

        Label sub = new Label("Here's what's happening with your projects today.");
        sub.getStyleClass().add("page-subtitle");

        VBox box = new VBox(6, welcome, sub);
        return box;
    }

    private FlowPane buildKpiRow() {
        Map<String, Object> stats = data.getClientStats();

        FlowPane row = new FlowPane();
        row.setHgap(16);
        row.setVgap(16);
        row.setPrefWrapLength(1000);
        row.getChildren().addAll(
            new StatCard("Active Requests",  stats.get("activeRequests").toString(),  Feather.BRIEFCASE,     StatCard.Color.BLUE),
            new StatCard("Offers Received",  stats.get("receivedOffers").toString(),  Feather.INBOX,         StatCard.Color.GREEN),
            new StatCard("Active Contracts", stats.get("activeContracts").toString(), Feather.FILE_TEXT,     StatCard.Color.ORANGE),
            new StatCard("Open Tickets",     stats.get("openTickets").toString(),     Feather.HELP_CIRCLE,   StatCard.Color.RED)
        );
        row.getChildren().forEach(node -> {
            if (node instanceof Region region) {
                region.setMinWidth(220);
                region.setPrefWidth(260);
            }
        });
        return row;
    }

    private VBox wrapSection(String title, Node inner) {
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("section-title");

        VBox card = new VBox(14, titleLbl, inner);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        VBox.setVgrow(inner, Priority.ALWAYS);
        return card;
    }

    private TableView<ServiceRequest> buildRequestsTable() {
        List<ServiceRequest> requests = data.getServiceRequests().stream()
            .filter(r -> r.getClientName().equals("Alice Martin"))
            .limit(4).toList();

        TableView<ServiceRequest> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setPrefHeight(220);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ServiceRequest, String> titleCol = col("Title", "title", 160);
        TableColumn<ServiceRequest, String> catCol   = col("Category", "category", 110);
        TableColumn<ServiceRequest, String> budgetCol = colStr("Budget", r -> r.getBudgetFormatted(), 80);

        TableColumn<ServiceRequest, Void> statusCol = new TableColumn<>("Status");
        statusCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                ServiceRequest req = getTableRow().getItem();
                setGraphic(BadgeLabel.forStatus(req.getStatus().name()));
            }
        });

        table.getColumns().addAll(titleCol, catCol, budgetCol, statusCol);
        table.getItems().addAll(requests);
        return table;
    }

    private TableView<Contract> buildContractsTable() {
        List<Contract> contracts = data.getContracts().stream()
            .filter(c -> c.getClientName().equals("Alice Martin"))
            .limit(4).toList();

        TableView<Contract> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setPrefHeight(220);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Contract, String> titleCol  = col("Project", "serviceRequestTitle", 140);
        TableColumn<Contract, String> workerCol = col("Psychologue", "PsychologueName", 100);
        TableColumn<Contract, Void>   statusCol = new TableColumn<>("Status");
        statusCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                setGraphic(BadgeLabel.forStatus(getTableRow().getItem().getStatus().name()));
            }
        });

        table.getColumns().addAll(titleCol, workerCol, statusCol);
        table.getItems().addAll(contracts);
        return table;
    }

    private VBox buildQuickActions() {
        Label title = new Label("Quick Actions");
        title.getStyleClass().add("section-title");

        Button postReq = new Button("+ Post a Request");
        postReq.getStyleClass().addAll("btn", "btn-primary");
        postReq.setOnAction(e -> NavigationManager.getInstance().navigateTo(new CreateServiceRequestView()));

        Button viewOffers = new Button("View Offers");
        viewOffers.getStyleClass().addAll("btn", "btn-secondary");
        viewOffers.setOnAction(e -> NavigationManager.getInstance().navigateTo(new OffersReceivedView()));

        Button viewContracts = new Button("My Contracts");
        viewContracts.getStyleClass().addAll("btn", "btn-secondary");
        viewContracts.setOnAction(e -> NavigationManager.getInstance().navigateTo(new ContractsView()));

        HBox btnRow = new HBox(12, postReq, viewOffers, viewContracts);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        btnRow.setFillHeight(true);
        postReq.setMaxWidth(Double.MAX_VALUE);
        viewOffers.setMaxWidth(Double.MAX_VALUE);
        viewContracts.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(postReq, Priority.ALWAYS);
        HBox.setHgrow(viewOffers, Priority.ALWAYS);
        HBox.setHgrow(viewContracts, Priority.ALWAYS);

        VBox card = new VBox(14, title, btnRow);
        card.getStyleClass().add("card");
        return card;
    }

    // Helpers
    @SuppressWarnings("unchecked")
    private <T> TableColumn<T, String> col(String header, String prop, int w) {
        TableColumn<T, String> c = new TableColumn<>(header);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setMinWidth(w);
        return c;
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<T, String> colStr(String header, java.util.function.Function<T, String> fn, int w) {
        TableColumn<T, String> c = new TableColumn<>(header);
        c.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(fn.apply(cd.getValue())));
        c.setMinWidth(w);
        return c;
    }
}

