package com.mindcare.view.client;

import com.mindcare.utils.NavigationManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.io.IOException;

public class CreateServiceRequestView implements NavigationManager.Buildable {

    @Override
    public Node build() {
        try {
            return FXMLLoader.load(getClass().getResource("/com/mindcare/view/client/CreateServiceRequestView.fxml"));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load CreateServiceRequestView.fxml", exception);
        }
    }
}
