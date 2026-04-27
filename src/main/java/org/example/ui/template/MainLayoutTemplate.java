package org.example.ui.template;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

import java.util.Arrays;
import java.util.List;

/**
 * Template for the main application layout with title and tab pane.
 */
public class MainLayoutTemplate implements UITemplate {
    private final String appTitle;
    private final List<Tab> tabs;

    public MainLayoutTemplate(String appTitle, Tab... tabs) {
        this.appTitle = appTitle;
        this.tabs = Arrays.asList(tabs);
    }

    @Override
    public Parent build() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        // Title
        Label titleLabel = new Label(appTitle);
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        BorderPane.setMargin(titleLabel, new Insets(0, 0, 12, 0));
        root.setTop(titleLabel);

        // Tab pane
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(this.tabs);
        root.setCenter(tabPane);

        return root;
    }
}
