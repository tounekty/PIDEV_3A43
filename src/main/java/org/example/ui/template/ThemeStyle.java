package org.example.ui.template;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Shared visual language for the MindCare desktop experience.
 */
public final class ThemeStyle {
    public static final Color PRIMARY_COLOR = Color.web("#245EBD");
    public static final Color PRIMARY_DARK = Color.web("#163D7A");
    public static final Color SECONDARY_COLOR = Color.web("#5E88C9");
    public static final Color ACCENT_COLOR = Color.web("#1C4F96");
    public static final Color SUCCESS_COLOR = Color.web("#3A8F6D");
    public static final Color BACKGROUND_COLOR = Color.web("#EDF6FF");
    public static final Color SURFACE_COLOR = Color.web("#F9FBFF");
    public static final Color CARD_COLOR = Color.web("#FFFFFF");
    public static final Color TEXT_COLOR = Color.web("#1C4F96");
    public static final Color TEXT_SECONDARY = Color.web("#6F87A6");
    public static final Color LIGHT_TEXT = Color.web("#9AAEC8");
    public static final Color BORDER_COLOR = Color.web("#CFE3FF");
    public static final Color DIVIDER_COLOR = Color.web("#D7E7FF");
    public static final Color DANGER_COLOR = Color.web("#C63D48");
    public static final Color PRIMARY_SOFT = Color.web("#D7E7FF");
    public static final Color PRIMARY_SURFACE = Color.web("#EDF6FF");

    private ThemeStyle() {
    }

    public static String getCSS() {
        return String.join("",
                ".root {",
                " -fx-font-family: 'Segoe UI Variable', 'Segoe UI', 'Trebuchet MS', sans-serif;",
                " -fx-font-size: 13px;",
                " -fx-background-color: linear-gradient(to bottom right, #F7FBFF 0%, #EDF6FF 48%, #E1EDFF 100%);",
                " -fx-accent: #1C4F96;",
                " -fx-focus-color: transparent;",
                " -fx-faint-focus-color: transparent;",
                "}",
                ".label { -fx-text-fill: #1C4F96; }",
                ".text-field, .text-area, .combo-box, .date-picker {",
                " -fx-background-color: #FFFFFF;",
                " -fx-background-radius: 14;",
                " -fx-border-color: #C5DCF9;",
                " -fx-border-radius: 14;",
                " -fx-border-width: 1.2;",
                " -fx-padding: 11 13 11 13;",
                " -fx-text-fill: #1C4F96;",
                " -fx-prompt-text-fill: #9AAEC8;",
                "}",
                ".text-area .content {",
                " -fx-background-color: transparent;",
                " -fx-background-radius: 14;",
                "}",
                ".text-field:focused, .text-area:focused, .combo-box:focused, .date-picker:focused {",
                " -fx-border-color: #163D7A;",
                " -fx-background-color: #FFFFFF;",
                " -fx-effect: dropshadow(gaussian, rgba(28,79,150,0.20), 14, 0.18, 0, 3);",
                "}",
                ".button {",
                " -fx-background-color: linear-gradient(to bottom right, #1A468D, #2B63BE);",
                " -fx-background-radius: 14;",
                " -fx-border-radius: 14;",
                " -fx-padding: 11 18 11 18;",
                " -fx-font-size: 13px;",
                " -fx-font-weight: 700;",
                " -fx-text-fill: white;",
                " -fx-cursor: hand;",
                " -fx-effect: dropshadow(gaussian, rgba(25,71,141,0.28), 16, 0.22, 0, 4);",
                "}",
                ".button:hover { -fx-background-color: linear-gradient(to bottom right, #163E7D, #2458AB); }",
                ".button:pressed { -fx-background-color: #13396F; }",
                ".button.secondary {",
                " -fx-background-color: #F5FAFF;",
                " -fx-text-fill: #1C4F96;",
                " -fx-border-color: #C5DCF9;",
                " -fx-border-width: 1.2;",
                " -fx-effect: none;",
                "}",
                ".button.secondary:hover { -fx-background-color: #EAF3FF; }",
                ".button.success {",
                " -fx-background-color: linear-gradient(to bottom right, #236C53, #3A8F6D);",
                "}",
                ".button.danger {",
                " -fx-background-color: linear-gradient(to bottom right, #C63D48, #A92F38);",
                "}",
                ".combo-box .list-cell, .date-picker .text-field { -fx-text-fill: #1C4F96; }",
                ".combo-box-popup .list-view {",
                " -fx-background-color: #FFFFFF;",
                " -fx-border-color: #C5DCF9;",
                " -fx-border-radius: 12;",
                " -fx-background-radius: 12;",
                "}",
                ".combo-box-popup .list-cell { -fx-padding: 10 12 10 12; }",
                ".combo-box-popup .list-cell:filled:selected, .combo-box-popup .list-cell:filled:hover {",
                " -fx-background-color: #DFECFF;",
                " -fx-text-fill: #1C4F96;",
                "}",
                ".tab-pane {",
                " -fx-tab-min-height: 46;",
                " -fx-tab-max-height: 46;",
                "}",
                ".tab-pane .tab-header-area { -fx-padding: 0 28 0 28; }",
                ".tab-pane .tab-header-background {",
                " -fx-background-color: transparent;",
                " -fx-border-color: transparent;",
                "}",
                ".tab-pane .tab {",
                " -fx-background-color: rgba(255,255,255,0.65);",
                " -fx-background-radius: 14 14 0 0;",
                " -fx-padding: 10 20 10 20;",
                " -fx-border-color: transparent;",
                "}",
                ".tab-pane .tab:selected {",
                " -fx-background-color: #FFFFFF;",
                " -fx-effect: dropshadow(gaussian, rgba(34,49,63,0.10), 16, 0.16, 0, 2);",
                "}",
                ".tab-pane .tab-label {",
                " -fx-text-fill: #6F87A6;",
                " -fx-font-size: 13px;",
                " -fx-font-weight: 700;",
                "}",
                ".tab-pane .tab:selected .tab-label { -fx-text-fill: #163D7A; }",
                ".table-view {",
                " -fx-background-color: #FFFFFF;",
                " -fx-background-radius: 18;",
                " -fx-border-color: #C5DCF9;",
                " -fx-border-radius: 18;",
                " -fx-padding: 6;",
                "}",
                ".table-view .column-header-background {",
                " -fx-background-color: linear-gradient(to right, #F4F9FF, #EAF3FF);",
                " -fx-background-radius: 14 14 0 0;",
                "}",
                ".table-view .column-header, .table-view .filler {",
                " -fx-background-color: transparent;",
                " -fx-size: 42;",
                " -fx-border-color: transparent transparent #DCE9FC transparent;",
                "}",
                ".table-view .column-header .label {",
                " -fx-text-fill: #1C4F96;",
                " -fx-font-weight: 700;",
                "}",
                ".table-row-cell {",
                " -fx-background-color: #FFFFFF;",
                " -fx-cell-size: 44;",
                " -fx-border-color: transparent transparent #EEF4FD transparent;",
                " -fx-text-fill: #1C4F96;",
                "}",
                ".table-row-cell:odd { -fx-background-color: #FBFDFF; }",
                ".table-row-cell:filled:hover { -fx-background-color: #F2F8FF; }",
                ".table-row-cell:filled:selected {",
                " -fx-background-color: #DFECFF;",
                " -fx-text-background-color: #163D7A;",
                "}",
                ".scroll-pane { -fx-background-color: transparent; -fx-border-color: transparent; }",
                ".scroll-pane > .viewport { -fx-background-color: transparent; }",
                ".scroll-bar:vertical, .scroll-bar:horizontal { -fx-background-color: transparent; }",
                ".scroll-bar .thumb {",
                " -fx-background-color: #C5D8F3;",
                " -fx-background-radius: 999;",
                "}",
                ".separator .line { -fx-border-color: #D7E7FF; }",
                ".chart {",
                " -fx-background-color: #FFFFFF;",
                " -fx-background-radius: 20;",
                " -fx-padding: 16;",
                " -fx-border-color: #DCE9FC;",
                " -fx-border-radius: 20;",
                " -fx-border-width: 1;",
                "}",
                ".chart-title {",
                " -fx-text-fill: #1C4F96;",
                " -fx-font-size: 14px;",
                " -fx-font-weight: 700;",
                "}",
                ".chart-legend { -fx-background-color: transparent; }",
                ".default-color0.chart-pie { -fx-pie-color: #163D7A; }",
                ".default-color1.chart-pie { -fx-pie-color: #245EBD; }",
                ".default-color2.chart-pie { -fx-pie-color: #5E88C9; }",
                ".default-color3.chart-pie { -fx-pie-color: #1C4F96; }",
                ".default-color4.chart-pie { -fx-pie-color: #3A8F6D; }",
                ".default-color0.chart-bar { -fx-bar-fill: #245EBD; }",
                ".axis-label, .axis { -fx-text-fill: #6F87A6; }",
                ".dialog-pane {",
                " -fx-background-color: #F8FBFF;",
                " -fx-background-radius: 14;",
                "}",
                ".dialog-pane:header *.header-panel {",
                " -fx-background-color: linear-gradient(to right, #1A468D, #2B63BE);",
                "}",
                ".dialog-pane:header *.header-panel *.label {",
                " -fx-text-fill: white;",
                " -fx-font-size: 15px;",
                " -fx-font-weight: 700;",
                "}"
        );
    }

    public static String getCssDataUri() {
        String encoded = URLEncoder.encode(getCSS(), StandardCharsets.UTF_8).replace("+", "%20");
        return "data:text/css," + encoded;
    }

    public static Background getPrimaryBackground() {
        return new Background(new BackgroundFill(SURFACE_COLOR, new CornerRadii(28), Insets.EMPTY));
    }

    public static Background getCardBackground() {
        return new Background(new BackgroundFill(CARD_COLOR, new CornerRadii(24), Insets.EMPTY));
    }

    public static Background getSoftBackground() {
        return new Background(new BackgroundFill(BACKGROUND_COLOR, new CornerRadii(24), Insets.EMPTY));
    }

    public static Label createTitleLabel(String text) {
        Label title = new Label(text);
        title.setStyle("-fx-font-size: 34px; -fx-font-weight: 800; -fx-text-fill: #1C4F96;");
        title.setWrapText(true);
        return title;
    }

    public static Label createHeaderLabel(String text) {
        Label header = new Label(text);
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #1C4F96;");
        header.setWrapText(true);
        return header;
    }

    public static Label createSubtitleLabel(String text) {
        Label subtitle = new Label(text);
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #6F87A6;");
        subtitle.setWrapText(true);
        return subtitle;
    }

    public static Label createStatLabel(String text) {
        Label stat = new Label(text);
        stat.setStyle("-fx-font-size: 13px; -fx-text-fill: #6F87A6;");
        return stat;
    }

    public static Label createStatValueLabel(String text) {
        Label value = new Label(text);
        value.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: #1C4F96;");
        return value;
    }

    public static Label createFormSectionLabel(String text) {
        Label section = new Label(text);
        section.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1C4F96;");
        return section;
    }

    public static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
