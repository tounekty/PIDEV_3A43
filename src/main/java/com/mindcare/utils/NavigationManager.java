package com.mindcare.utils;

import com.mindcare.components.MainLayout;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * NavigationManager – singleton controlling screen transitions.
 *
 * Keeps ONE persistent MainLayout (sidebar + topbar) so the sidebar
 * active state is never reset when navigating between pages.
 *
 * - navigateTo()      → full-screen swap (login, register, etc.)
 * - navigateContent() → swaps only the MainLayout content area (post-login)
 */
public class NavigationManager {

    private static NavigationManager instance;

    private StackPane  rootContainer;
    private Stage      primaryStage;
    private Node       currentScreen;

    /** Persistent shell – built once after login, reused for all in-app navigation. */
    private MainLayout mainLayout;

    private NavigationManager() {}

    public static NavigationManager getInstance() {
        if (instance == null) instance = new NavigationManager();
        return instance;
    }

    /** Called once from the app start() before any navigation. */
    public void initialize(StackPane root, Stage stage) {
        this.rootContainer = root;
        this.primaryStage  = stage;
    }

    /**
     * Full-screen navigation (login, register, forgot-password).
     * Clears the stored MainLayout so the next in-app nav builds a fresh one.
     */
    public void navigateTo(Buildable screen) {
        // If navigating to auth screens, we don't need the main layout
        mainLayout = null;
        Node view = screen.build();
        setScreen(view);
    }

    /**
     * In-app navigation – creates the MainLayout once, then only swaps the content pane.
     * The sidebar stays alive and retains its active-button state.
     */
    public void navigateContent(String title, Buildable contentBuilder) {
        if (mainLayout == null) {
            mainLayout = new MainLayout(title);
            rootContainer.getChildren().setAll(mainLayout);
        } else {
            mainLayout.setTitle(title);
        }
        Node content = contentBuilder.build();
        mainLayout.setContent(content);
        currentScreen = mainLayout;
    }

    /** Low-level: set any node as the active full-screen view. */
    public void setScreen(Node view) {
        if (rootContainer == null) throw new IllegalStateException("NavigationManager not initialized.");
        rootContainer.getChildren().setAll(view);
        currentScreen = view;
    }

    public Stage   getPrimaryStage()  { return primaryStage; }
    public Node    getCurrentScreen() { return currentScreen; }
    public MainLayout getMainLayout() { return mainLayout; }

    @FunctionalInterface
    public interface Buildable {
        Node build();
    }
}
