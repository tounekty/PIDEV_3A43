package org.example.ui.template;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Template for creating consistent button bars.
 */
public class ButtonBarTemplate {
    private final HBox hbox;
    private final Map<String, Button> buttons = new LinkedHashMap<>();

    public ButtonBarTemplate() {
        this.hbox = new HBox();
        this.hbox.setSpacing(10);
        this.hbox.setAlignment(Pos.CENTER_LEFT);
    }

    public ButtonBarTemplate addButton(String text, Runnable action) {
        Button button = new Button(text);
        if (action != null) {
            button.setOnAction(e -> action.run());
        }
        this.hbox.getChildren().add(button);
        this.buttons.put(text, button);
        return this;
    }

    public ButtonBarTemplate setPadding(Insets insets) {
        this.hbox.setPadding(insets);
        return this;
    }

    public ButtonBarTemplate setSpacing(double spacing) {
        this.hbox.setSpacing(spacing);
        return this;
    }

    public HBox build() {
        return this.hbox;
    }

    public Button getButton(String text) {
        return this.buttons.get(text);
    }

    public Map<String, Button> getButtons() {
        return this.buttons;
    }
}
