package com.mindcare.view.psychologue;

import com.mindcare.utils.NavigationManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.io.IOException;

public class MyOffersView implements NavigationManager.Buildable {

    @Override
    public Node build() {
        try {
            return FXMLLoader.load(getClass().getResource("/com/mindcare/view/worker/MyOffersView.fxml"));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load MyOffersView.fxml", exception);
        }
    }
}
