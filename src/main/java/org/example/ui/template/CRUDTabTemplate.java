package org.example.ui.template;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reusable elevated CRUD tab layout with hero copy, actions, filters and data grid.
 */
public class CRUDTabTemplate<T> {
    private final String tabName;
    private final FormTemplate formTemplate;
    private final TableView<T> tableView;
    private final TextField searchField;
    private final ComboBox<String> sortCombo;
    private final Label statsLabel;
    private final Runnable onCreateAction;
    private final Runnable onUpdateAction;
    private final Runnable onDeleteAction;
    private final Runnable onClearAction;
    private final Runnable onRefreshAction;
    private final Runnable onApplyFiltersAction;
    private final Runnable onResetFiltersAction;
    private final Map<String, Runnable> extraSecondaryActions;

    public CRUDTabTemplate(String tabName, FormTemplate formTemplate, TableView<T> tableView) {
        this(tabName, formTemplate, tableView, new TextField(), new ComboBox<>(), new Label("No entries yet"),
                null, null, null, null, null, null, null, new LinkedHashMap<>());
        this.tableView.setPrefHeight(360);
        this.sortCombo.setPrefWidth(180);
    }

    private CRUDTabTemplate(String tabName, FormTemplate formTemplate, TableView<T> tableView,
                            TextField searchField, ComboBox<String> sortCombo, Label statsLabel,
                            Runnable onCreateAction, Runnable onUpdateAction, Runnable onDeleteAction,
                            Runnable onClearAction, Runnable onRefreshAction,
                            Runnable onApplyFiltersAction, Runnable onResetFiltersAction,
                            Map<String, Runnable> extraSecondaryActions) {
        this.tabName = tabName;
        this.formTemplate = formTemplate;
        this.tableView = tableView;
        this.searchField = searchField;
        this.sortCombo = sortCombo;
        this.statsLabel = statsLabel;
        this.onCreateAction = onCreateAction;
        this.onUpdateAction = onUpdateAction;
        this.onDeleteAction = onDeleteAction;
        this.onClearAction = onClearAction;
        this.onRefreshAction = onRefreshAction;
        this.onApplyFiltersAction = onApplyFiltersAction;
        this.onResetFiltersAction = onResetFiltersAction;
        this.extraSecondaryActions = extraSecondaryActions;
    }

    public CRUDTabTemplate<T> withActions(Runnable onCreate, Runnable onUpdate, Runnable onDelete,
                                          Runnable onClear, Runnable onRefresh) {
        return new CRUDTabTemplate<>(this.tabName, this.formTemplate, this.tableView,
                this.searchField, this.sortCombo, this.statsLabel,
                onCreate, onUpdate, onDelete, onClear, onRefresh,
                this.onApplyFiltersAction, this.onResetFiltersAction,
                new LinkedHashMap<>(this.extraSecondaryActions));
    }

    public CRUDTabTemplate<T> withFilters(Runnable onApplyFilters, Runnable onResetFilters) {
        return new CRUDTabTemplate<>(this.tabName, this.formTemplate, this.tableView,
                this.searchField, this.sortCombo, this.statsLabel,
                this.onCreateAction, this.onUpdateAction, this.onDeleteAction,
                this.onClearAction, this.onRefreshAction,
                onApplyFilters, onResetFilters,
                new LinkedHashMap<>(this.extraSecondaryActions));
    }

    public CRUDTabTemplate<T> withSecondaryAction(String label, Runnable action) {
        Map<String, Runnable> actions = new LinkedHashMap<>(this.extraSecondaryActions);
        actions.put(label, action);
        return new CRUDTabTemplate<>(this.tabName, this.formTemplate, this.tableView,
                this.searchField, this.sortCombo, this.statsLabel,
                this.onCreateAction, this.onUpdateAction, this.onDeleteAction,
                this.onClearAction, this.onRefreshAction,
                this.onApplyFiltersAction, this.onResetFiltersAction,
                actions);
    }

    public CRUDTabTemplate<T> setSearchPlaceholder(String text) {
        this.searchField.setPromptText(text);
        this.searchField.setPrefWidth(260);
        return this;
    }

    public CRUDTabTemplate<T> setSortOptions(java.util.List<String> options) {
        this.sortCombo.getItems().setAll(options);
        if (!options.isEmpty()) {
            this.sortCombo.getSelectionModel().select(0);
        }
        return this;
    }

    public CRUDTabTemplate<T> setStatsText(String text) {
        this.statsLabel.setText(text);
        return this;
    }

    public Tab build() {
        VBox introCard = createSectionIntro();
        VBox formCard = createWrappedSection("Entry details", "Fill in the fields below, then save or update your entry.", formTemplate.build());
        VBox actionCard = createActionCard();
        VBox filtersCard = createFilterCard();
        VBox tableCard = createTableCard();

        VBox workspace = new VBox(18);
        workspace.getChildren().addAll(formCard, actionCard, filtersCard, tableCard);

        VBox content = new VBox(20);
        content.setPadding(new Insets(12, 18, 24, 18));
        content.getChildren().addAll(introCard, workspace);
        VBox.setVgrow(workspace, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        Tab tab = new Tab(tabName, scrollPane);
        tab.setClosable(false);
        return tab;
    }

    public TextField getSearchField() {
        return searchField;
    }

    public ComboBox<String> getSortCombo() {
        return sortCombo;
    }

    public Label getStatsLabel() {
        return statsLabel;
    }

    private VBox createSectionIntro() {
        VBox intro = new VBox(4);
        intro.setPadding(new Insets(4, 4, 2, 4));
        Label title = new Label(tabName + " workspace");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #1C4F96;");

        Label body = new Label("Create, edit, and review your " + tabName.toLowerCase() + " entries in one place.");
        body.setStyle("-fx-font-size: 13px; -fx-text-fill: #6F87A6;");
        body.setWrapText(true);

        intro.getChildren().addAll(title, body);
        return intro;
    }

    private VBox createActionCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 24;" +
                "-fx-border-color: " + ThemeStyle.toHex(ThemeStyle.BORDER_COLOR) + ";" +
                "-fx-border-radius: 24;" +
                "-fx-border-width: 1;"
        );

        Label title = new Label("Quick actions");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1C4F96;");

        Label body = new Label("Save a new entry, update the selected row, or clear the form.");
        body.setStyle("-fx-font-size: 12px; -fx-text-fill: #6F87A6;");
        body.setWrapText(true);

        HBox primaryRow = new HBox(10);
        primaryRow.setAlignment(Pos.CENTER_LEFT);

        HBox secondaryRow = new HBox(10);
        secondaryRow.setAlignment(Pos.CENTER_LEFT);

        if (onCreateAction != null) {
            primaryRow.getChildren().add(createButton("Create " + extractEntityName(), "success", onCreateAction));
        }
        if (onUpdateAction != null) {
            primaryRow.getChildren().add(createButton("Update selected", "", onUpdateAction));
        }
        if (onDeleteAction != null) {
            primaryRow.getChildren().add(createButton("Delete selected", "danger", onDeleteAction));
        }
        if (onClearAction != null) {
            secondaryRow.getChildren().add(createButton("Clear form", "secondary", onClearAction));
        }
        if (onRefreshAction != null) {
            secondaryRow.getChildren().add(createButton("Refresh", "secondary", onRefreshAction));
        }
        for (Map.Entry<String, Runnable> action : extraSecondaryActions.entrySet()) {
            if (action.getValue() != null) {
                secondaryRow.getChildren().add(createButton(action.getKey(), "secondary", action.getValue()));
            }
        }

        card.getChildren().addAll(title, body, primaryRow);
        if (!secondaryRow.getChildren().isEmpty()) {
            card.getChildren().add(secondaryRow);
        }
        return card;
    }

    private VBox createFilterCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 24;" +
                "-fx-border-color: " + ThemeStyle.toHex(ThemeStyle.BORDER_COLOR) + ";" +
                "-fx-border-radius: 24;" +
                "-fx-border-width: 1;"
        );

        Label heading = new Label("Find entries quickly");
        heading.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1C4F96;");

        VBox searchBlock = createFieldBlock("Search", searchField);
        VBox sortBlock = createFieldBlock("Sort", sortCombo);
        searchBlock.setMaxWidth(Double.MAX_VALUE);
        sortBlock.setPrefWidth(220);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        if (onApplyFiltersAction != null) {
            buttons.getChildren().add(createButton("Apply", "", onApplyFiltersAction));
        }
        if (onResetFiltersAction != null) {
            buttons.getChildren().add(createButton("Reset", "secondary", onResetFiltersAction));
        }

        HBox inputsRow = new HBox(12);
        inputsRow.setAlignment(Pos.CENTER_LEFT);
        inputsRow.getChildren().addAll(searchBlock, sortBlock);
        HBox.setHgrow(searchBlock, Priority.ALWAYS);

        card.getChildren().addAll(heading, inputsRow, buttons);
        return card;
    }

    private VBox createTableCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 26;" +
                "-fx-border-color: " + ThemeStyle.toHex(ThemeStyle.BORDER_COLOR) + ";" +
                "-fx-border-radius: 26;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(34,49,63,0.06), 20, 0.15, 0, 6);"
        );

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox heading = new VBox(4);
        Label title = new Label(tabName + " history");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #1C4F96;");

        Label subtitle = new Label("Select a row to edit existing information.");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #6F87A6;");
        heading.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statsLabel.setStyle(
                "-fx-background-color: #F8F4EF;" +
                "-fx-background-radius: 999;" +
                "-fx-padding: 8 14 8 14;" +
                "-fx-text-fill: #1C4F96;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: 700;"
        );

        top.getChildren().addAll(heading, spacer, statsLabel);

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        card.getChildren().addAll(top, new Separator(), tableView);
        return card;
    }

    private VBox createWrappedSection(String titleText, String bodyText, javafx.scene.Node contentNode) {
        VBox card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 24;" +
                "-fx-border-color: " + ThemeStyle.toHex(ThemeStyle.BORDER_COLOR) + ";" +
                "-fx-border-radius: 24;" +
                "-fx-border-width: 1;"
        );

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1C4F96;");

        Label body = new Label(bodyText);
        body.setStyle("-fx-font-size: 12px; -fx-text-fill: #6F87A6;");
        body.setWrapText(true);

        card.getChildren().addAll(title, body, contentNode);
        return card;
    }

    private VBox createFieldBlock(String label, javafx.scene.Node input) {
        VBox block = new VBox(6);
        Label labelControl = new Label(label);
        labelControl.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #1C4F96;");
        block.getChildren().addAll(labelControl, input);
        return block;
    }

    private Button createButton(String text, String styleClass, Runnable action) {
        Button button = new Button(text);
        if (!styleClass.isBlank()) {
            button.getStyleClass().add(styleClass);
        }
        button.setOnAction(e -> action.run());
        return button;
    }

    private String extractEntityName() {
        return tabName.replace(" CRUD", "");
    }
}
