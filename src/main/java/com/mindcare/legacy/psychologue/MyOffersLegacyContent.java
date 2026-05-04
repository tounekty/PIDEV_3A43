package com.mindcare.legacy.psychologue;

import com.mindcare.view.psychologue.*;

import com.mindcare.components.*;
import com.mindcare.model.Offer;
import com.mindcare.service.MockDataService;
import com.mindcare.utils.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

/**
 * MyOffersLegacyContent â€“ worker's submitted offers.
 */
public class MyOffersLegacyContent implements NavigationManager.Buildable {

    private final MockDataService data = MockDataService.getInstance();

    @Override
    public Node build() {
        return buildContent();
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);

        Label title = new Label("My Submitted Offers");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Track all your submitted proposals");
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

        TableColumn<Offer, String> reqCol      = new TableColumn<>("Service Request");
        reqCol.setCellValueFactory(new PropertyValueFactory<>("serviceRequestTitle"));
        reqCol.setMinWidth(160);

        TableColumn<Offer, String> priceCol    = new TableColumn<>("Your Price");
        priceCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getPriceFormatted()));
        priceCol.setMinWidth(90);

        TableColumn<Offer, String> deliveryCol = new TableColumn<>("Delivery");
        deliveryCol.setCellValueFactory(new PropertyValueFactory<>("deliveryTime"));
        deliveryCol.setMinWidth(80);

        TableColumn<Offer, String> dateCol     = new TableColumn<>("Submitted");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        dateCol.setMinWidth(90);

        TableColumn<Offer, Void>   statusCol   = new TableColumn<>("Status");
        statusCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                setGraphic(BadgeLabel.forStatus(getTableRow().getItem().getStatus().name()));
            }
        });
        statusCol.setMinWidth(100);

        table.getColumns().addAll(reqCol, priceCol, deliveryCol, dateCol, statusCol);
        table.getItems().addAll(data.getOffers());

        VBox card = new VBox(table);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(0));
        return card;
    }
}

