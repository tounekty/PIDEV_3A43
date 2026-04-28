package com.mindcare.components;

import javafx.scene.control.Label;

/**
 * BadgeLabel – styled status badge for use in tables and cards.
 */
public class BadgeLabel extends Label {

    public enum Style { SUCCESS, WARNING, DANGER, INFO, NEUTRAL, PRIMARY }

    public BadgeLabel(String text, Style style) {
        super(text);
        getStyleClass().add("badge");
        getStyleClass().add(styleClass(style));
    }

    private String styleClass(Style style) {
        return switch (style) {
            case SUCCESS -> "badge-success";
            case WARNING -> "badge-warning";
            case DANGER  -> "badge-danger";
            case INFO    -> "badge-info";
            case NEUTRAL -> "badge-neutral";
            case PRIMARY -> "badge-primary";
        };
    }

    // Factory helpers for common statuses
    public static BadgeLabel forStatus(String status) {
        return switch (status.toUpperCase()) {
            case "OPEN", "ACTIVE", "APPROVED"              -> new BadgeLabel(status, Style.SUCCESS);
            case "PENDING", "IN_PROGRESS", "EN COURS"     -> new BadgeLabel(status, Style.WARNING);
            case "COMPLETED", "RESOLVED", "CLOSED"         -> new BadgeLabel(status, Style.PRIMARY);
            case "CANCELLED", "REJECTED", "WITHDRAWN"      -> new BadgeLabel(status, Style.DANGER);
            case "DISPUTED"                                -> new BadgeLabel(status, Style.DANGER);
            default                                        -> new BadgeLabel(status, Style.NEUTRAL);
        };
    }
}
