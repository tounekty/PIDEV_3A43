package org.example.ui.template;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Premium main layout with a soft editorial header and elevated content area.
 */
public class StyledMainLayoutTemplate implements UITemplate {
    private final String appTitle;
    private final String appSubtitle;
    private final javafx.scene.control.Tab[] tabs;
    private final Runnable primaryTopAction;
    private final String primaryTopActionLabel;
    private final Runnable secondaryTopAction;
    private final String secondaryTopActionLabel;

    public StyledMainLayoutTemplate(String appTitle, javafx.scene.control.Tab... tabs) {
        this(appTitle, "Track your moods, capture your thoughts, and notice your patterns.", null, null, null, null, tabs);
    }

    public StyledMainLayoutTemplate(String appTitle, Runnable onAdminAction, javafx.scene.control.Tab... tabs) {
        this(appTitle, "Track your moods, capture your thoughts, and notice your patterns.", "Admin dashboard", onAdminAction, null, null, tabs);
    }

    public StyledMainLayoutTemplate(String appTitle, String appSubtitle, Runnable onAdminAction, javafx.scene.control.Tab... tabs) {
        this(appTitle, appSubtitle, "Admin dashboard", onAdminAction, null, null, tabs);
    }

    public StyledMainLayoutTemplate(String appTitle, String appSubtitle, String topActionLabel, Runnable onTopAction, javafx.scene.control.Tab... tabs) {
        this(appTitle, appSubtitle, topActionLabel, onTopAction, null, null, tabs);
    }

    public StyledMainLayoutTemplate(String appTitle, String appSubtitle, String primaryTopActionLabel,
                                    Runnable primaryTopAction, String secondaryTopActionLabel,
                                    Runnable secondaryTopAction, javafx.scene.control.Tab... tabs) {
        this.appTitle = appTitle;
        this.appSubtitle = appSubtitle;
        this.tabs = tabs;
        this.primaryTopAction = primaryTopAction;
        this.primaryTopActionLabel = primaryTopActionLabel;
        this.secondaryTopAction = secondaryTopAction;
        this.secondaryTopActionLabel = secondaryTopActionLabel;
    }

    @Override
    public Parent build() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + ThemeStyle.toHex(ThemeStyle.BACKGROUND_COLOR) + ";");
        root.setTop(createModernHeader());

        javafx.scene.control.TabPane tabPane = new javafx.scene.control.TabPane();
        tabPane.setTabClosingPolicy(javafx.scene.control.TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(this.tabs);
        tabPane.setStyle("-fx-background-color: transparent;");

        StackPane contentShell = new StackPane(tabPane);
        contentShell.setPadding(new Insets(0, 32, 28, 32));
        contentShell.setStyle("-fx-background-color: transparent;");
        root.setCenter(contentShell);

        return root;
    }

    private VBox createModernHeader() {
        VBox shell = new VBox();
        shell.setPadding(new Insets(28, 32, 26, 32));
        shell.setStyle("-fx-background-color: transparent;");

        VBox hero = new VBox(18);
        hero.setPadding(new Insets(30, 34, 30, 34));
        hero.setStyle(
                "-fx-background-color: linear-gradient(to right, #163D7A 0%, #1C4F96 55%, #245EBD 100%);" +
                "-fx-background-radius: 30;" +
                "-fx-effect: dropshadow(gaussian, rgba(22,61,122,0.26), 28, 0.2, 0, 10);"
        );

        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.TOP_LEFT);

        VBox titleBlock = new VBox(8);
        Label eyebrow = new Label("Mindful journaling");
        eyebrow.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: rgba(255,255,255,0.78); -fx-letter-spacing: 1.1;");

        Label titleLabel = new Label(appTitle);
        titleLabel.setStyle("-fx-font-size: 38px; -fx-font-weight: 800; -fx-text-fill: white;");

        Label subtitleLabel = new Label(appSubtitle);
        subtitleLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: rgba(255,255,255,0.82);");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(520);

        titleBlock.getChildren().addAll(eyebrow, titleLabel, subtitleLabel);
        topRow.getChildren().add(titleBlock);

        if (primaryTopAction != null || secondaryTopAction != null) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox actions = new HBox(10);
            if (secondaryTopAction != null) {
                actions.getChildren().add(createTopActionButton(secondaryTopActionLabel, secondaryTopAction));
            }
            if (primaryTopAction != null) {
                actions.getChildren().add(createTopActionButton(primaryTopActionLabel, primaryTopAction));
            }
            topRow.getChildren().addAll(spacer, actions);
        }

        HBox insightRow = new HBox(14);
        insightRow.getChildren().addAll(
                createMetricChip("Mood log", "Capture how today feels"),
                createMetricChip("Journal", "Keep notes connected to emotions")
        );

        hero.getChildren().addAll(topRow, insightRow);
        shell.getChildren().add(hero);
        return shell;
    }

    private VBox createMetricChip(String title, String text) {
        VBox chip = new VBox(4);
        chip.setPadding(new Insets(14, 16, 14, 16));
        chip.setPrefWidth(220);
        chip.setStyle(
                "-fx-background-color: rgba(255,255,255,0.10);" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: rgba(255,255,255,0.16);" +
                "-fx-border-radius: 20;"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: white;");

        Label bodyLabel = new Label(text);
        bodyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.72);");
        bodyLabel.setWrapText(true);

        chip.getChildren().addAll(titleLabel, bodyLabel);
        return chip;
    }

    private Button createTopActionButton(String label, Runnable action) {
        Button btn = new Button(label == null || label.isBlank() ? "Open" : label);
        String baseStyle =
                "-fx-padding: 12 20 12 20;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: 700;" +
                "-fx-background-color: rgba(255,255,255,0.12);" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 16;" +
                "-fx-border-radius: 16;" +
                "-fx-border-color: rgba(255,255,255,0.22);" +
                "-fx-border-width: 1.1;";
        String hoverStyle =
                "-fx-padding: 12 20 12 20;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: 700;" +
                "-fx-background-color: rgba(255,255,255,0.18);" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 16;" +
                "-fx-border-radius: 16;" +
                "-fx-border-color: rgba(255,255,255,0.30);" +
                "-fx-border-width: 1.1;";

        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
        btn.setOnAction(e -> action.run());
        return btn;
    }
}
