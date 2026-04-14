package com.mindcare.view.admin;

import com.mindcare.utils.NavigationManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.io.IOException;

public class AdminDashboardView implements NavigationManager.Buildable {

    @Override
    public Node build() {
        try {
            return FXMLLoader.load(getClass().getResource("/com/mindcare/view/admin/AdminDashboardView.fxml"));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load AdminDashboardView.fxml", exception);
        }
    }
}

