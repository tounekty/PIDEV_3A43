package com.mindcare.view.psychologue;

import com.mindcare.utils.NavigationManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.io.IOException;

public class PsychologueContractsView implements NavigationManager.Buildable {

    @Override
    public Node build() {
        try {
            return FXMLLoader.load(getClass().getResource("/com/mindcare/view/worker/PsychologueContractsView.fxml"));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load PsychologueContractsView.fxml", exception);
        }
    }
}
