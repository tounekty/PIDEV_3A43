package com.mindcare.legacy.psychologue;

import com.mindcare.view.psychologue.*;

import com.mindcare.components.*;
import com.mindcare.model.ServiceRequest;
import com.mindcare.service.MockDataService;
import com.mindcare.utils.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

/**
 * BrowseRequestsLegacyContent â€“ worker browses open service requests and submits offers.
 */
public class BrowseRequestsLegacyContent implements NavigationManager.Buildable {

    private final MockDataService data = MockDataService.getInstance();

    @Override
    public Node build() {
        return buildContent();
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(0));

        Label title = new Label("Available Service Requests");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Find projects that match your skills and submit an offer");
        sub.getStyleClass().add("page-subtitle");

        // Filter bar
        HBox filterBar = buildFilterBar();

        // Request cards
        VBox requestsList = buildRequestCards();

        content.getChildren().addAll(new VBox(6, title, sub), filterBar, requestsList);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private HBox buildFilterBar() {
        TextField search = new TextField();
        search.setPromptText("Search requests...");
        search.getStyleClass().add("filter-input");
        HBox.setHgrow(search, Priority.ALWAYS);

        ComboBox<String> catFilter = new ComboBox<>();
        catFilter.getItems().addAll("All Categories", "Web Development", "UI/UX Design", "Data Science",
            "Backend Development", "Content Creation", "Design");
        catFilter.setValue("All Categories");
        catFilter.getStyleClass().add("combo-box");

        ComboBox<String> budgetFilter = new ComboBox<>();
        budgetFilter.getItems().addAll("Any Budget", "< $500", "$500-$1000", "$1000-$3000", "$3000+");
        budgetFilter.setValue("Any Budget");
        budgetFilter.getStyleClass().add("combo-box");

        HBox bar = new HBox(12, search, catFilter, budgetFilter);
        bar.getStyleClass().add("filter-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private VBox buildRequestCards() {
        List<ServiceRequest> open = data.getServiceRequests().stream()
            .filter(r -> r.getStatus() == ServiceRequest.Status.OPEN)
            .toList();

        VBox list = new VBox(14);
        for (ServiceRequest req : open) {
            list.getChildren().add(buildRequestCard(req));
        }
        return list;
    }

    private HBox buildRequestCard(ServiceRequest req) {
        // Left: info
        Label titleLbl = new Label(req.getTitle());
        titleLbl.getStyleClass().add("card-title");

        Label cat = new Label(req.getCategory());
        cat.getStyleClass().add("label-secondary");
        cat.setStyle("-fx-font-size: 12px;");

        Label desc = new Label("Detailed requirements posted. Looking for skilled professionals.");
        desc.getStyleClass().add("label-muted");
        desc.setWrapText(true);

        HBox meta = new HBox(16,
            metaChip("Deadline:", req.getDeadline()),
            metaChip("Offers:", req.getOffersCount() + " offers")
        );

        VBox infoBox = new VBox(8, titleLbl, cat, desc, meta);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Right: budget + action
        Label budget = new Label(req.getBudgetFormatted());
        budget.getStyleClass().add("label-accent");
        budget.setStyle("-fx-font-weight: bold; -fx-font-size: 22px;");

        Label budgetLabel = new Label("Budget");
        budgetLabel.getStyleClass().add("label-muted");

        Button submitBtn = new Button("Submit Offer");
        submitBtn.getStyleClass().addAll("btn", "btn-primary");
        submitBtn.setOnAction(e -> NavigationManager.getInstance().navigateTo(new SubmitOfferView(req)));

        VBox rightBox = new VBox(8, budget, budgetLabel, submitBtn);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        rightBox.setMinWidth(140);

        HBox card = new HBox(24, infoBox, rightBox);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setAlignment(Pos.CENTER_LEFT);
        return card;
    }

    private HBox metaChip(String icon, String text) {
        Label lbl = new Label(icon + " " + text);
        lbl.getStyleClass().add("label-muted");
        return new HBox(lbl);
    }
}

