package com.mindcare.legacy.admin;

import com.mindcare.view.admin.*;

import com.mindcare.components.MainLayout;
import com.mindcare.utils.NavigationManager;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * DossiersEtudiantsLegacyContent Ã¢â‚¬â€œ system event logs for admin oversight.
 */
public class DossiersEtudiantsLegacyContent implements NavigationManager.Buildable {

    private static final List<String[]> LOGS = List.of(
        new String[]{"2026-03-12 01:15:32", "AUTH",     "INFO",    "User alice@mindcare.io logged in"},
        new String[]{"2026-03-12 01:10:14", "CONTRACT", "INFO",    "Contract #3 status changed to ACTIVE"},
        new String[]{"2026-03-12 01:05:42", "TICKET",   "WARNING", "High priority ticket #4 opened by clara@mindcare.io"},
        new String[]{"2026-03-12 00:58:11", "CERTIFICATE","INFO",  "Certificate submitted by bob@mindcare.io for review"},
        new String[]{"2026-03-12 00:50:30", "AUTH",     "INFO",    "User bob@mindcare.io registered as WORKER"},
        new String[]{"2026-03-12 00:44:08", "OFFER",    "INFO",    "Offer #6 submitted on request #1"},
        new String[]{"2026-03-12 00:40:55", "AUTH",     "WARNING", "Failed login attempt for unknown@mindcare.io"},
        new String[]{"2026-03-12 00:35:20", "CONTRACT", "INFO",    "Contract #2 marked COMPLETED"},
        new String[]{"2026-03-12 00:28:12", "TICKET",   "INFO",    "Ticket #3 resolved by admin henry@mindcare.io"},
        new String[]{"2026-03-12 00:15:44", "SERVICE",  "INFO",    "Service request #5 posted by alice@mindcare.io"},
        new String[]{"2026-03-12 00:05:11", "AUTH",     "INFO",    "User david@mindcare.io logged in"},
        new String[]{"2026-03-11 23:55:33", "SYSTEM",   "INFO",    "Daily stats report generated successfully"}
    );

    @Override
    public Node build() {
        return buildContent();
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);

        Label title = new Label("Dossiers Étudiants");
        title.getStyleClass().add("page-title");
        Label sub   = new Label("Dossiers étudiants et suivi administratif");
        sub.getStyleClass().add("page-subtitle");

        content.getChildren().addAll(new VBox(6, title, sub), buildLogsCard());
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox buildLogsCard() {
        TableView<String[]> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(520);

        TableColumn<String[], String> timestampCol = new TableColumn<>("Timestamp");
        timestampCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[0]));
        timestampCol.setMinWidth(150);

        TableColumn<String[], String> moduleCol    = new TableColumn<>("Module");
        moduleCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[1]));
        moduleCol.setMinWidth(100);

        TableColumn<String[], Void>   levelCol     = new TableColumn<>("Level");
        levelCol.setMinWidth(90);
        levelCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                String level = getTableRow().getItem()[2];
                Label badge = new Label(level);
                badge.getStyleClass().add("badge");
                badge.getStyleClass().add(switch (level) {
                    case "WARNING" -> "badge-warning";
                    case "ERROR"   -> "badge-danger";
                    default        -> "badge-info";
                });
                setGraphic(badge);
            }
        });

        TableColumn<String[], String> messageCol   = new TableColumn<>("Message");
        messageCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[3]));
        messageCol.setMinWidth(300);

        table.getColumns().addAll(timestampCol, moduleCol, levelCol, messageCol);
        table.getItems().addAll(LOGS);

        VBox card = new VBox(table);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(0));
        return card;
    }
}


