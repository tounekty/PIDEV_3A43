package com.mindcare.legacy.admin;

import com.mindcare.view.admin.*;

import com.mindcare.components.MainLayout;
import com.mindcare.utils.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * GestionResourcesLegacyContent Ã¢â‚¬â€œ admin can create, edit and delete service categories.
 */
public class GestionResourcesLegacyContent implements NavigationManager.Buildable {

    private final List<String[]> categories = List.of(
        new String[]{"1", "Web Development",    "35", "Active"},
        new String[]{"2", "UI/UX Design",        "22", "Active"},
        new String[]{"3", "Data Science",        "18", "Active"},
        new String[]{"4", "Backend Development", "28", "Active"},
        new String[]{"5", "Content Creation",    "14", "Active"},
        new String[]{"6", "Digital Marketing",   "10", "Inactive"},
        new String[]{"7", "DevOps",              "9",  "Active"}
    );

    @Override
    public Node build() {
        return buildContent();
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);

        Label title = new Label("Gestion Resources");
        title.getStyleClass().add("page-title");
        Label sub   = new Label("Define the service categories available on the platform");
        sub.getStyleClass().add("page-subtitle");

        Button addBtn = new Button("+ New Category");
        addBtn.getStyleClass().addAll("btn", "btn-primary");

        HBox headerRow = new HBox();
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        headerRow.getChildren().addAll(new VBox(6, title, sub), spacer, addBtn);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(headerRow, buildTable());
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox buildTable() {
        TableView<String[]> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(400);

        TableColumn<String[], String> idCol     = new TableColumn<>("ID");
        idCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[0]));
        idCol.setMinWidth(50);

        TableColumn<String[], String> nameCol   = new TableColumn<>("Category Name");
        nameCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[1]));
        nameCol.setMinWidth(180);

        TableColumn<String[], String> countCol  = new TableColumn<>("Active Workers");
        countCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[2]));
        countCol.setMinWidth(120);

        TableColumn<String[], String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[3]));
        statusCol.setMinWidth(100);

        TableColumn<String[], Void>   actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Button editBtn   = new Button("Edit");   editBtn.getStyleClass().addAll("btn","btn-secondary","btn-sm");
                Button deleteBtn = new Button("Delete"); deleteBtn.getStyleClass().addAll("btn","btn-danger","btn-sm");
                setGraphic(new HBox(8, editBtn, deleteBtn));
            }
        });

        table.getColumns().addAll(idCol, nameCol, countCol, statusCol, actionsCol);
        table.getItems().addAll(categories);

        VBox card = new VBox(table);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(0));
        return card;
    }
}


