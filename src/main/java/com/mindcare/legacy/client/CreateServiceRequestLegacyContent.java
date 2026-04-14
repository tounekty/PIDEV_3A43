package com.mindcare.legacy.client;

import com.mindcare.view.client.*;

import com.mindcare.components.MainLayout;
import com.mindcare.utils.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * CreateServiceRequestLegacyContent – form to post a new service request.
 */
public class CreateServiceRequestLegacyContent implements NavigationManager.Buildable {

    @Override
    public Node build() {
        MainLayout layout = new MainLayout("Create Service Request");
        layout.setContent(buildForm());
        return layout;
    }

    private ScrollPane buildForm() {
        VBox content = new VBox(24);
        content.setPadding(new Insets(0));
        content.setMaxWidth(700);

        Label title = new Label("Post a New Service Request");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Describe what you need and let workers submit their offers");
        sub.getStyleClass().add("page-subtitle");

        VBox card = new VBox(20);
        card.getStyleClass().add("card");

        // Title
        card.getChildren().add(field("Request Title", "e.g. Build a responsive dashboard UI", false));

        // Category
        Label catLabel = new Label("Category");
        catLabel.getStyleClass().add("form-label");
        ComboBox<String> catBox = new ComboBox<>();
        catBox.getItems().addAll("Web Development","Mobile Development","UI/UX Design","Backend Development",
            "Data Science","Content Creation","Digital Marketing","Design","DevOps","Other");
        catBox.setValue("Web Development");
        catBox.getStyleClass().add("combo-box");
        catBox.setMaxWidth(Double.MAX_VALUE);
        card.getChildren().add(new VBox(6, catLabel, catBox));

        // Budget + Deadline row
        HBox bdRow = new HBox(16, field("Budget (USD)", "e.g. 500", false), field("Deadline", "YYYY-MM-DD", false));
        HBox.setHgrow(bdRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(bdRow.getChildren().get(1), Priority.ALWAYS);
        card.getChildren().add(bdRow);

        // Description
        Label descLabel = new Label("Description");
        descLabel.getStyleClass().add("form-label");
        TextArea descArea = new TextArea();
        descArea.setPromptText("Describe the project in detail: requirements, goals, deliverables...");
        descArea.getStyleClass().add("text-area");
        descArea.setPrefRowCount(6);
        descArea.setMaxWidth(Double.MAX_VALUE);
        card.getChildren().add(new VBox(6, descLabel, descArea));

        // Buttons
        Button submit = new Button("Post Service Request");
        submit.getStyleClass().addAll("btn", "btn-primary");
        submit.setOnAction(e -> NavigationManager.getInstance().navigateTo(new ServiceRequestListView()));

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().addAll("btn", "btn-secondary");
        cancel.setOnAction(e -> NavigationManager.getInstance().navigateTo(new ServiceRequestListView()));

        HBox btnRow = new HBox(12, submit, cancel);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(btnRow);

        content.getChildren().addAll(new VBox(6, title, sub), card);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox field(String label, String prompt, boolean password) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("form-label");
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("text-field");
        tf.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, lbl, tf);
    }
}
