package com.mindcare.legacy.psychologue;

import com.mindcare.view.psychologue.*;

import com.mindcare.components.*;
import com.mindcare.model.Contract;
import com.mindcare.service.MockDataService;
import com.mindcare.utils.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/** PsychologueContractsLegacyContent â€“ shows the worker's assigned contracts. */
public class PsychologueContractsLegacyContent implements NavigationManager.Buildable {

    private final MockDataService data = MockDataService.getInstance();

    @Override
    public Node build() {
        return buildContent();
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);

        Label title = new Label("My Contracts");
        title.getStyleClass().add("page-title");
        Label sub   = new Label("Track and manage your active projects");
        sub.getStyleClass().add("page-subtitle");

        List<Contract> contracts = data.getContracts().stream()
            .filter(c -> c.getWorkerName().equals("Bob Chen")).toList();

        VBox cards = new VBox(14);
        for (Contract c : contracts) {
            cards.getChildren().add(buildContractCard(c));
        }

        content.getChildren().addAll(new VBox(6, title, sub), cards);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox buildContractCard(Contract contract) {
        Label titleLbl  = new Label(contract.getServiceRequestTitle()); titleLbl.getStyleClass().add("card-title");
        Label client    = new Label("Client: " + contract.getClientName()); client.getStyleClass().add("label-secondary");
        Label amount    = new Label(contract.getAmountFormatted()); amount.getStyleClass().add("label-accent"); amount.setStyle("-fx-font-weight:bold;-fx-font-size:16px;");
        BadgeLabel badge = BadgeLabel.forStatus(contract.getStatus().name());

        ProgressBar pb  = new ProgressBar(contract.getProgress() / 100.0);
        pb.getStyleClass().add("progress-bar");
        pb.setMaxWidth(Double.MAX_VALUE);

        Label progLabel = new Label(contract.getProgress() + "% complete");
        progLabel.getStyleClass().add("label-muted");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topRow = new HBox(16, new VBox(4, titleLbl, client), spacer, amount, badge);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Button msgBtn = new Button("Message Client");
        msgBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        msgBtn.setOnAction(e -> NavigationManager.getInstance().navigateTo(new PsychologueMessagingView()));

        VBox card = new VBox(12, topRow, pb, new HBox(12, progLabel, new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, msgBtn));
        card.getStyleClass().add("card");
        return card;
    }
}

