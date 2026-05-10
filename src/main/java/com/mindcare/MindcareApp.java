package com.mindcare;

import com.mindcare.db.DBConnection;
import com.mindcare.services.AppointmentService;
import com.mindcare.utils.NavigationManager;
import com.mindcare.view.auth.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MindCare Desktop Application - Main Entry Point
 * MindCare Desktop Application for Student Mental Health
 */
public class MindcareApp extends Application {

    public static final String APP_TITLE = "MindCare";
    public static final double MIN_WIDTH  = 1200;
    public static final double MIN_HEIGHT = 750;
    private final ScheduledExecutorService appointmentStatusScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "appointment-status-scheduler");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void start(Stage primaryStage) {
        // Root container – NavigationManager swaps children here
        StackPane root = new StackPane();
        root.getStyleClass().add("root-pane");

        // Boot the navigation system
        NavigationManager nav = NavigationManager.getInstance();
        nav.initialize(root, primaryStage);

        // Build scene and apply global CSS theme
        Scene scene = new Scene(root, MIN_WIDTH, MIN_HEIGHT);
        String css = getClass().getResource("/com/mindcare/styles/orion-theme.css").toExternalForm();
        scene.getStylesheets().add(css);

        // Stage setup
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();

        // Navigate to the login screen first
        nav.navigateTo(new LoginView());

        // Keep appointment status in sync with real time while the app is running.
        appointmentStatusScheduler.scheduleAtFixedRate(() -> {
            try {
                new AppointmentService().refreshTimeBasedStatuses();
            } catch (Exception ignored) {
                // Background sync should never crash the application thread.
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() {
        appointmentStatusScheduler.shutdownNow();
        DBConnection.shutdown();
    }
}
