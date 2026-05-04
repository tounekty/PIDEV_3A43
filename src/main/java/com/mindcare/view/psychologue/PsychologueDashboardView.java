package com.mindcare.view.psychologue;

import com.mindcare.utils.NavigationManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.io.IOException;

public class PsychologueDashboardView implements NavigationManager.Buildable {

    @Override
    public Node build() {
        try {
            return FXMLLoader.load(getClass().getResource("/com/mindcare/view/worker/PsychologueDashboardView.fxml"));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load PsychologueDashboardView.fxml", exception);
        }
    }
}
