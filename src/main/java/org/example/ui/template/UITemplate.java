package org.example.ui.template;

import javafx.scene.Parent;

/**
 * Base template interface for all UI screens.
 */
public interface UITemplate {
    /**
     * Build and return the root node for this template.
     */
    Parent build();
}
