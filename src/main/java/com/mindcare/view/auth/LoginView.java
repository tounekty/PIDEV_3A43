package com.mindcare.view.auth;

import com.mindcare.utils.NavigationManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.io.IOException;

public class LoginView implements NavigationManager.Buildable {

    @Override
    public Node build() {
        try {
            return FXMLLoader.load(getClass().getResource("/com/mindcare/view/auth/LoginView.fxml"));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load LoginView.fxml", exception);
        }
    }
}
