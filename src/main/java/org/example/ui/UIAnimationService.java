package org.example.ui;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Utility service for UI animations and icons using Ikonli
 */
public class UIAnimationService {

    /**
     * Create a FontAwesome icon for play/speaker
     */
    public static FontIcon createPlayIcon() {
        FontIcon icon = new FontIcon(FontAwesomeSolid.PLAY);
        icon.setIconSize(16);
        return icon;
    }

    /**
     * Create a FontAwesome icon for stop/pause
     */
    public static FontIcon createStopIcon() {
        FontIcon icon = new FontIcon(FontAwesomeSolid.PAUSE);
        icon.setIconSize(16);
        return icon;
    }

    /**
     * Create a FontAwesome icon for like/thumbs-up
     */
    public static FontIcon createLikeIcon() {
        FontIcon icon = new FontIcon(FontAwesomeRegular.THUMBS_UP);
        icon.setIconSize(16);
        return icon;
    }

    /**
     * Create a FontAwesome icon for dislike/thumbs-down
     */
    public static FontIcon createDislikeIcon() {
        FontIcon icon = new FontIcon(FontAwesomeRegular.THUMBS_DOWN);
        icon.setIconSize(16);
        return icon;
    }

    /**
     * Create a FontAwesome icon for delete/trash
     */
    public static FontIcon createDeleteIcon() {
        FontIcon icon = new FontIcon(FontAwesomeSolid.TRASH);
        icon.setIconSize(16);
        return icon;
    }

    /**
     * Create a FontAwesome icon for add/plus
     */
    public static FontIcon createAddIcon() {
        FontIcon icon = new FontIcon(FontAwesomeSolid.PLUS);
        icon.setIconSize(16);
        return icon;
    }

    /**
     * Create a FontAwesome icon for check/checkmark
     */
    public static FontIcon createCheckIcon() {
        FontIcon icon = new FontIcon(FontAwesomeSolid.CHECK);
        icon.setIconSize(16);
        return icon;
    }

    /**
     * Animate a fade-in effect on a node
     */
    public static void fadeIn(Node node, Duration duration) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    /**
     * Animate a fade-out effect on a node
     */
    public static void fadeOut(Node node, Duration duration) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.play();
    }

    /**
     * Animate a slide-down effect on a node
     */
    public static void slideDown(Node node, Duration duration, double distance) {
        node.setTranslateY(-distance);
        TranslateTransition slide = new TranslateTransition(duration, node);
        slide.setToY(0);
        slide.play();
    }

    /**
     * Animate a pulse effect on a node
     */
    public static void pulse(Node node) {
        FadeTransition fade1 = new FadeTransition(Duration.millis(200), node);
        fade1.setFromValue(1.0);
        fade1.setToValue(0.7);
        
        FadeTransition fade2 = new FadeTransition(Duration.millis(200), node);
        fade2.setFromValue(0.7);
        fade2.setToValue(1.0);
        
        fade1.setOnFinished(e -> fade2.play());
        fade1.play();
    }
}
