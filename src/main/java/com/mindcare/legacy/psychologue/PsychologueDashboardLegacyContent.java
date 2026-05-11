package com.mindcare.legacy.psychologue;

import com.mindcare.view.psychologue.*;

import com.mindcare.components.*;
import com.mindcare.model.Offer;
import com.mindcare.model.Contract;
import com.mindcare.service.MockDataService;
import com.mindcare.utils.NavigationManager;
import com.mindcare.utils.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;

import java.util.Map;

/**
 * PsychologueDashboardLegacyContent Ã¢â‚¬â€œ main overview for workers / freelancers.
 */
public class PsychologueDashboardLegacyContent implements NavigationManager.Buildable {

    private final MockDataService data = MockDataService.getInstance();

    @Override
    public Node build() {
        return buildContent();
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(28);
        content.setPadding(new Insets(28, 32, 28, 32));

        String name = SessionManager.getInstance().getCurrentUser().getFirstName();
        Label welcome = new Label("Welcome, " + name);
        welcome.getStyleClass().add("page-title");
        Label sub = new Label("Manage your freelance business from one place.");
        sub.getStyleClass().add("page-subtitle");

        content.getChildren().addAll(new VBox(6, welcome, sub), buildKpiRow(), buildTwoColumns(), buildCTA());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private HBox buildKpiRow() {
        Map<String, Object> stats = data.getWorkerStats();
        HBox row = new HBox(16,
            new StatCard("Active Offers",     stats.get("activeOffers").toString(),    Feather.SEND,        StatCard.Color.BLUE),
            new StatCard("Active Contracts",  stats.get("activeContracts").toString(), Feather.FILE_TEXT,   StatCard.Color.GREEN),
            new StatCard("Gestion Forum - Messages",      stats.get("certificates").toString(),    Feather.AWARD,       StatCard.Color.ORANGE),
            new StatCard("Open Tickets",      stats.get("openTickets").toString(),     Feather.HELP_CIRCLE, StatCard.Color.RED)
        );
        row.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        return row;
    }

    private HBox buildTwoColumns() {
        VBox offersCard = buildSection("My Recent Offers", buildOffersList());
        VBox contractCard = buildSection("Active Contracts", buildContractsList());
        HBox row = new HBox(24, offersCard, contractCard);
        HBox.setHgrow(offersCard, Priority.ALWAYS);
        HBox.setHgrow(contractCard, Priority.ALWAYS);
        return row;
    }

    private VBox buildSection(String title, Node inner) {
        Label lbl = new Label(title);
        lbl.getStyleClass().add("section-title");
        VBox card = new VBox(14, lbl, inner);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildOffersList() {
        VBox list = new VBox(10);
        data.getOffers().stream().limit(3).forEach(o -> {
            Label title  = new Label(o.getServiceRequestTitle()); title.getStyleClass().add("label-primary"); title.setStyle("-fx-font-weight:bold;");
            Label price  = new Label(o.getPriceFormatted());       price.getStyleClass().add("label-accent");
            BadgeLabel badge = BadgeLabel.forStatus(o.getStatus().name());
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            HBox row = new HBox(12, new VBox(3, title, price), sp, badge);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color: transparent transparent #263040 transparent; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 10 0;");
            list.getChildren().add(row);
        });
        return list;
    }

    private VBox buildContractsList() {
        VBox list = new VBox(10);
        data.getContracts().stream().filter(c -> c.getWorkerName().equals("Bob Chen")).limit(3).forEach(c -> {
            Label title  = new Label(c.getServiceRequestTitle()); title.getStyleClass().add("label-primary"); title.setStyle("-fx-font-weight:bold;");
            Label client = new Label("Client: " + c.getClientName()); client.getStyleClass().add("label-secondary");
            ProgressBar pb = new ProgressBar(c.getProgress() / 100.0);
            pb.getStyleClass().add("progress-bar");
            pb.setMaxWidth(Double.MAX_VALUE);
            list.getChildren().add(new VBox(6, new VBox(3, title, client), pb));
        });
        return list;
    }

    private VBox buildCTA() {
        Label title = new Label("Find Your Next Project");
        title.getStyleClass().add("section-title");

        Button browseBtn = new Button("Browse Service Requests");
        browseBtn.getStyleClass().addAll("btn", "btn-primary");
        browseBtn.setOnAction(e -> NavigationManager.getInstance().navigateTo(new BrowseRequestsView()));

        Button offersBtn = new Button("View My Offers");
        offersBtn.getStyleClass().addAll("btn", "btn-secondary");
        offersBtn.setOnAction(e -> NavigationManager.getInstance().navigateTo(new MyOffersView()));

        HBox btnRow = new HBox(12, browseBtn, offersBtn);
        VBox card = new VBox(14, title, btnRow);
        card.getStyleClass().add("card");
        return card;
    }
}


