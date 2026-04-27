package org.example.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.db.SchemaInitializer;
import org.example.ui.template.ThemeStyle;

public class MoodJournalApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        SchemaInitializer.ensureSchema();
        showLogin(stage);
        stage.show();
    }

    private void showLogin(Stage stage) {
        Scene scene = new Scene(new LoginView(stage, this::showMainApp).build(), 980, 720);
        scene.getStylesheets().add(ThemeStyle.getCssDataUri());
        stage.setTitle("MindCare Login");
        stage.setScene(scene);
    }

    private void showMainApp(Stage stage, int userId, String username, boolean isAdmin) {
        if (isAdmin) {
            Scene adminScene = new Scene(new AdminDashboard(userId, () -> showLogin(stage)).build(), 980, 720);
            adminScene.getStylesheets().add(ThemeStyle.getCssDataUri());
            stage.setTitle("MindCare Admin Dashboard");
            stage.setScene(adminScene);
            return;
        }

        MainController controller = new MainController(userId, username, false, () -> showLogin(stage));
        Scene rootScene = new Scene(controller.createView(), 980, 720);
        controller.setScene(rootScene);
        rootScene.getStylesheets().add(ThemeStyle.getCssDataUri());
        stage.setTitle("MindCare");
        stage.setScene(rootScene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
