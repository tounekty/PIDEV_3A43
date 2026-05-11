package com.mindcare.legacy.psychologue;

import com.mindcare.view.psychologue.*;

import com.mindcare.components.MainLayout;
import com.mindcare.model.ServiceRequest;
import com.mindcare.utils.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * SubmitOfferLegacyContent â€“ form for workers to submit an offer on a request.
 */
public class SubmitOfferLegacyContent implements NavigationManager.Buildable {

    private final ServiceRequest request;

    public SubmitOfferLegacyContent(ServiceRequest request) {
        this.request = request;
    }

    // Also callable with no request (for direct navigation from sidebar)
    public SubmitOfferLegacyContent() {
        this.request = null;
    }

    @Override
    public Node build() {
        return buildContent();
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);

        Label title = new Label("Submit an Offer");
        title.getStyleClass().add("page-title");
        Label sub = new Label(request != null ? "For: " + request.getTitle() : "Select a request to submit your offer");
        sub.getStyleClass().add("page-subtitle");

        VBox card = new VBox(20);
        card.getStyleClass().add("card");
        card.setMaxWidth(680);

        if (request != null) {
            // Request info
            VBox infoBox = new VBox(8);
            infoBox.getStyleClass().add("ai-block");
            Label reqTitle = new Label(request.getTitle()); reqTitle.getStyleClass().add("card-title");
            Label reqCat   = new Label(request.getCategory()); reqCat.getStyleClass().add("label-secondary");
            Label reqBudget = new Label("Client budget: " + request.getBudgetFormatted()); reqBudget.getStyleClass().add("label-accent");
            infoBox.getChildren().addAll(reqTitle, reqCat, reqBudget);
            card.getChildren().add(infoBox);
        }

        // Price
        TextField priceField = new TextField();
        priceField.setPromptText("Your offer price in USD");
        priceField.getStyleClass().add("text-field");
        priceField.setMaxWidth(Double.MAX_VALUE);

        // Delivery time
        TextField deliveryField = new TextField();
        deliveryField.setPromptText("e.g. 7 days, 2 weeks");
        deliveryField.getStyleClass().add("text-field");
        deliveryField.setMaxWidth(Double.MAX_VALUE);

        // Cover letter
        TextArea coverLetter = new TextArea();
        coverLetter.setPromptText("Introduce yourself and explain why you're the best fit for this project...");
        coverLetter.getStyleClass().add("text-area");
        coverLetter.setPrefRowCount(6);
        coverLetter.setMaxWidth(Double.MAX_VALUE);

        HBox priceDelivery = new HBox(16,
            wrapField("Your Price (USD)", priceField),
            wrapField("Delivery Time", deliveryField)
        );
        HBox.setHgrow(priceDelivery.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(priceDelivery.getChildren().get(1), Priority.ALWAYS);

        Button submitBtn = new Button("Submit Offer");
        submitBtn.getStyleClass().addAll("btn", "btn-primary");
        submitBtn.setOnAction(e -> NavigationManager.getInstance().navigateTo(new MyOffersView()));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        cancelBtn.setOnAction(e -> NavigationManager.getInstance().navigateTo(new BrowseRequestsView()));

        HBox btnRow = new HBox(12, submitBtn, cancelBtn);

        card.getChildren().addAll(
            priceDelivery,
            wrapField("Cover Letter", coverLetter),
            btnRow
        );

        content.getChildren().addAll(new VBox(6, title, sub), card);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox wrapField(String label, Control field) {
        Label lbl = new Label(label); lbl.getStyleClass().add("form-label");
        VBox box = new VBox(6, lbl, field);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }
}

