package com.mindcare.legacy.client;

import com.mindcare.view.client.*;

import com.mindcare.components.*;
import com.mindcare.model.Offer;
import com.mindcare.service.MockDataService;
import com.mindcare.utils.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * OffersReceivedLegacyContent â€“ shows all offers submitted for the client's requests.
 */
public class OffersReceivedLegacyContent implements NavigationManager.Buildable {

    private final MockDataService data = MockDataService.getInstance();

    @Override
    public Node build() {
        return buildContent();
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(0));

        Label title = new Label("Offers Received");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Review and respond to offers from freelancers");
        sub.getStyleClass().add("page-subtitle");

        content.getChildren().addAll(new VBox(6, title, sub), buildOffersCard());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox buildOffersCard() {
        List<Offer> offers = data.getOffers();

        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        for (Offer offer : offers) {
            card.getChildren().add(buildOfferRow(offer));
            Region div = new Region();
            div.getStyleClass().add("divider");
            card.getChildren().add(div);
        }
        if (card.getChildren().isEmpty()) {
            Label empty = new Label("No offers received yet.");
            empty.getStyleClass().add("label-muted");
            card.getChildren().add(empty);
        }

        return card;
    }

    private HBox buildOfferRow(Offer offer) {
        // Worker info
        Label workerName = new Label(offer.getWorkerName());
        workerName.getStyleClass().add("label-primary");
        workerName.setStyle("-fx-font-weight: bold;");

        Label requestTitle = new Label(offer.getServiceRequestTitle());
        requestTitle.getStyleClass().add("label-secondary");
        requestTitle.setStyle("-fx-font-size: 11px;");

        VBox workerBox = new VBox(3, workerName, requestTitle);

        // Details
        Label price    = new Label(offer.getPriceFormatted());
        price.getStyleClass().add("label-accent");
        price.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");

        Label delivery = new Label("â± " + offer.getDeliveryTime());
        delivery.getStyleClass().add("label-secondary");

        VBox priceBox = new VBox(3, price, delivery);
        priceBox.setAlignment(Pos.CENTER_RIGHT);

        // Status badge
        BadgeLabel badge = BadgeLabel.forStatus(offer.getStatus().name());

        // Actions
        Button acceptBtn = new Button("Accept");
        acceptBtn.getStyleClass().addAll("btn", "btn-success", "btn-sm");

        Button rejectBtn = new Button("Decline");
        rejectBtn.getStyleClass().addAll("btn", "btn-danger", "btn-sm");

        Button viewBtn = new Button("Details");
        viewBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");

        HBox actions = new HBox(8, viewBtn, acceptBtn, rejectBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(16, workerBox, priceBox, spacer, badge, actions);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}

