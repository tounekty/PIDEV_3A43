package com.mindcare.view.client;

import com.mindcare.utils.NavigationManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.io.IOException;

public class OffersReceivedView implements NavigationManager.Buildable {

    @Override
    public Node build() {
        try {
            return FXMLLoader.load(getClass().getResource("/com/mindcare/view/client/OffersReceivedView.fxml"));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load OffersReceivedView.fxml", exception);
        }
    }
}
