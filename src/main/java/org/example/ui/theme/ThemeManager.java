package org.example.ui.theme;

import javafx.scene.paint.Color;
import javafx.scene.Scene;

/**
 * Manages theme switching between Light, Dark, and Custom themes
 */
public class ThemeManager {
    public enum ThemeType {
        LIGHT, DARK, CUSTOM
    }

    private static ThemeType currentTheme = ThemeType.LIGHT;
    private static Scene currentScene;

    public static void setTheme(ThemeType theme, Scene scene) {
        currentTheme = theme;
        currentScene = scene;
        applyTheme(theme, scene);
    }

    public static ThemeType getCurrentTheme() {
        return currentTheme;
    }

    private static void applyTheme(ThemeType theme, Scene scene) {
        switch (theme) {
            case LIGHT -> applyLightTheme(scene);
            case DARK -> applyDarkTheme(scene);
            case CUSTOM -> applyCustomTheme(scene);
        }
    }

    private static void applyLightTheme(Scene scene) {
        String lightCSS = getLightCSS();
        scene.getStylesheets().clear();
        scene.getStylesheets().add("data:text/css;base64," + encodeCSS(lightCSS));
    }

    private static void applyDarkTheme(Scene scene) {
        String darkCSS = getDarkCSS();
        scene.getStylesheets().clear();
        scene.getStylesheets().add("data:text/css;base64," + encodeCSS(darkCSS));
    }

    private static void applyCustomTheme(Scene scene) {
        String customCSS = getCustomCSS();
        scene.getStylesheets().clear();
        scene.getStylesheets().add("data:text/css;base64," + encodeCSS(customCSS));
    }

    private static String getLightCSS() {
        return """
                .root {
                    -fx-font-family: 'Segoe UI Variable', 'Segoe UI', sans-serif;
                    -fx-font-size: 13px;
                    -fx-background-color: linear-gradient(to bottom right, #F7FBFF 0%, #EDF6FF 48%, #E1EDFF 100%);
                    -fx-text-fill: #1C4F96;
                }
                .label { -fx-text-fill: #1C4F96; }
                .text-field, .text-area, .combo-box, .date-picker {
                    -fx-background-color: #FFFFFF;
                    -fx-background-radius: 14;
                    -fx-border-color: #C5DCF9;
                    -fx-border-width: 1.2;
                    -fx-padding: 11 13 11 13;
                    -fx-text-fill: #1C4F96;
                    -fx-prompt-text-fill: #9AAEC8;
                }
                .button {
                    -fx-background-color: linear-gradient(to bottom right, #1A468D, #2B63BE);
                    -fx-background-radius: 14;
                    -fx-padding: 11 18 11 18;
                    -fx-text-fill: white;
                    -fx-cursor: hand;
                    -fx-font-weight: 700;
                }
                .button:hover { -fx-background-color: linear-gradient(to bottom right, #163E7D, #2458AB); }
                .button:pressed { -fx-background-color: #13396F; }
                .tab-pane .tab-header-background { -fx-background-color: #F9FBFF; }
                .tab-pane .tab { -fx-background-color: #EDF6FF; }
                .tab-pane .tab:selected { -fx-background-color: #FFFFFF; }
                """;
    }

    private static String getDarkCSS() {
        return """
                .root {
                    -fx-font-family: 'Segoe UI Variable', 'Segoe UI', sans-serif;
                    -fx-font-size: 13px;
                    -fx-background-color: linear-gradient(to bottom right, #1E1E2E 0%, #2D2D44 48%, #3A3A52 100%);
                    -fx-text-fill: #E0E6FF;
                }
                .label { -fx-text-fill: #E0E6FF; }
                .text-field, .text-area, .combo-box, .date-picker {
                    -fx-background-color: #2D2D44;
                    -fx-background-radius: 14;
                    -fx-border-color: #4A4A6A;
                    -fx-border-width: 1.2;
                    -fx-padding: 11 13 11 13;
                    -fx-text-fill: #E0E6FF;
                    -fx-prompt-text-fill: #7A7A9A;
                }
                .button {
                    -fx-background-color: linear-gradient(to bottom right, #3B5BFF, #5B7DFF);
                    -fx-background-radius: 14;
                    -fx-padding: 11 18 11 18;
                    -fx-text-fill: white;
                    -fx-cursor: hand;
                    -fx-font-weight: 700;
                }
                .button:hover { -fx-background-color: linear-gradient(to bottom right, #2A4AEE, #4A6CEE); }
                .button:pressed { -fx-background-color: #1A3ADD; }
                .tab-pane .tab-header-background { -fx-background-color: #1E1E2E; }
                .tab-pane .tab { -fx-background-color: #2D2D44; -fx-text-fill: #E0E6FF; }
                .tab-pane .tab:selected { -fx-background-color: #3A3A52; }
                """;
    }

    private static String getCustomCSS() {
        return """
                .root {
                    -fx-font-family: 'Segoe UI Variable', 'Segoe UI', sans-serif;
                    -fx-font-size: 13px;
                    -fx-background-color: linear-gradient(to bottom right, #FFF5E6 0%, #FFE8CC 48%, #FFD9B3 100%);
                    -fx-text-fill: #8B6F47;
                }
                .label { -fx-text-fill: #8B6F47; }
                .text-field, .text-area, .combo-box, .date-picker {
                    -fx-background-color: #FFFBF0;
                    -fx-background-radius: 14;
                    -fx-border-color: #F5D7A1;
                    -fx-border-width: 1.2;
                    -fx-padding: 11 13 11 13;
                    -fx-text-fill: #8B6F47;
                    -fx-prompt-text-fill: #C9A961;
                }
                .button {
                    -fx-background-color: linear-gradient(to bottom right, #D4A574, #E8C896);
                    -fx-background-radius: 14;
                    -fx-padding: 11 18 11 18;
                    -fx-text-fill: white;
                    -fx-cursor: hand;
                    -fx-font-weight: 700;
                }
                .button:hover { -fx-background-color: linear-gradient(to bottom right, #C79666, #D9B87A); }
                .button:pressed { -fx-background-color: #B38858; }
                .tab-pane .tab-header-background { -fx-background-color: #FFF5E6; }
                .tab-pane .tab { -fx-background-color: #FFE8CC; -fx-text-fill: #8B6F47; }
                .tab-pane .tab:selected { -fx-background-color: #FFFBF0; }
                """;
    }

    private static String encodeCSS(String css) {
        try {
            return java.util.Base64.getEncoder().encodeToString(css.getBytes());
        } catch (Exception e) {
            return "";
        }
    }
}
