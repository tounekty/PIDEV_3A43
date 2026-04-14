package com.mindcare.legacy.admin;

import com.mindcare.view.admin.*;

import com.mindcare.components.*;
import com.mindcare.model.Certificate;
import com.mindcare.service.MockDataService;
import com.mindcare.utils.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * GestionForumMessagesLegacyContent Ã¢â‚¬â€œ admin panel for validating worker certificates.
 */
public class GestionForumMessagesLegacyContent implements NavigationManager.Buildable {

    private final MockDataService data = MockDataService.getInstance();

    @Override
    public Node build() {
        return buildContent();
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);

        Label title = new Label("Gestion Forum - Messages");
        title.getStyleClass().add("page-title");
        Label sub   = new Label("Review and validate worker professional certificates");
        sub.getStyleClass().add("page-subtitle");

        content.getChildren().addAll(new VBox(6, title, sub), buildPendingCard(), buildAllCard());
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox buildPendingCard() {
        List<Certificate> pending = data.getCertificates().stream()
            .filter(c -> c.getStatus() == Certificate.Status.PENDING).toList();

        Label header = new Label("Ã¢ÂÂ³ Pending Review (" + pending.size() + ")");
        header.getStyleClass().add("section-title");

        VBox list = new VBox(12);
        for (Certificate cert : pending) {
            list.getChildren().add(buildCertCard(cert, true));
        }

        VBox card = new VBox(14, header, list.getChildren().isEmpty() ?
            new Label("No pending certificates.") {{ getStyleClass().add("label-muted"); }} : list);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildAllCard() {
        Label header = new Label("All Certificates");
        header.getStyleClass().add("section-title");

        TableView<Certificate> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(280);

        TableColumn<Certificate, String> workerCol  = new TableColumn<>("Psychologue");
        workerCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("PsychologueName"));

        TableColumn<Certificate, String> nameCol    = new TableColumn<>("Certificate");
        nameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));

        TableColumn<Certificate, String> issuerCol  = new TableColumn<>("Issuer");
        issuerCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("issuer"));

        TableColumn<Certificate, Void>   statusCol  = new TableColumn<>("Status");
        statusCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                setGraphic(BadgeLabel.forStatus(getTableRow().getItem().getStatus().name()));
            }
        });

        table.getColumns().addAll(workerCol, nameCol, issuerCol, statusCol);
        table.getItems().addAll(data.getCertificates());

        VBox card = new VBox(14, header, table);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildCertCard(Certificate cert, boolean showActions) {
        Label name   = new Label(cert.getName());   name.getStyleClass().add("card-title");
        Label worker = new Label("Psychologue: " + cert.getWorkerName()); worker.getStyleClass().add("label-secondary");
        Label issuer = new Label("Issued by " + cert.getIssuer() + " Ã¢â‚¬â€ " + cert.getIssuedDate()); issuer.getStyleClass().add("label-muted");

        // AI block
        VBox aiBlock = new VBox(6);
        aiBlock.getStyleClass().add("ai-block");
        Label aiTitle = new Label("AI Analysis"); aiTitle.getStyleClass().add("ai-block-title");
        Label aiText  = new Label(cert.getAiAnalysis()); aiText.getStyleClass().add("label-muted"); aiText.setWrapText(true);
        aiBlock.getChildren().addAll(aiTitle, aiText);

        VBox info = new VBox(6, name, worker, issuer, aiBlock);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button approveBtn = new Button("Ã¢Å“â€œ Approve"); approveBtn.getStyleClass().addAll("btn","btn-success","btn-sm");
        Button rejectBtn  = new Button("Ã¢Å“â€” Reject");  rejectBtn.getStyleClass().addAll("btn","btn-danger","btn-sm");

        VBox btnBox = new VBox(8, approveBtn, rejectBtn);
        btnBox.setAlignment(Pos.TOP_CENTER);

        HBox cardContent = new HBox(20, info, btnBox);
        cardContent.setAlignment(Pos.TOP_LEFT);

        VBox card = new VBox(cardContent);
        card.setStyle("-fx-background-color: #263040; -fx-background-radius: 10; -fx-padding: 16;");
        return card;
    }
}


