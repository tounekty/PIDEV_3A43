package com.mindcare.legacy.admin;

import com.mindcare.view.admin.*;

import com.mindcare.components.*;
import com.mindcare.model.Offer;
import com.mindcare.service.MockDataService;
import com.mindcare.utils.NavigationManager;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

/**
 * GestionEventsLegacyContent Ã¢â‚¬â€œ admin view for managing all offers.
 */
public class GestionEventsLegacyContent implements NavigationManager.Buildable {

    private final MockDataService data = MockDataService.getInstance();

    @Override
    public Node build() {
        return buildContent();
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);

        Label title = new Label("Gestion Events");
        title.getStyleClass().add("page-title");
        Label sub   = new Label("Monitor all submitted offers across the platform");
        sub.getStyleClass().add("page-subtitle");

        content.getChildren().addAll(new VBox(6, title, sub), buildTable());
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox buildTable() {
        TableView<Offer> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(400);

        TableColumn<Offer, String> reqCol    = new TableColumn<>("Service Request");
        reqCol.setCellValueFactory(new PropertyValueFactory<>("serviceRequestTitle"));
        reqCol.setMinWidth(160);

        TableColumn<Offer, String> workerCol = new TableColumn<>("Psychologue");
        workerCol.setCellValueFactory(new PropertyValueFactory<>("PsychologueName"));
        workerCol.setMinWidth(120);

        TableColumn<Offer, String> priceCol  = new TableColumn<>("Price");
        priceCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getPriceFormatted()));
        priceCol.setMinWidth(80);

        TableColumn<Offer, String> delivCol  = new TableColumn<>("Delivery");
        delivCol.setCellValueFactory(new PropertyValueFactory<>("deliveryTime"));
        delivCol.setMinWidth(80);

        TableColumn<Offer, Void>   statusCol = new TableColumn<>("Status");
        statusCol.setMinWidth(100);
        statusCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                setGraphic(BadgeLabel.forStatus(getTableRow().getItem().getStatus().name()));
            }
        });

        table.getColumns().addAll(reqCol, workerCol, priceCol, delivCol, statusCol);
        table.getItems().addAll(data.getOffers());

        VBox card = new VBox(table);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(0));
        return card;
    }
}


