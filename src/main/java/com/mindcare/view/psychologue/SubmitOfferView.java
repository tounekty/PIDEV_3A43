package com.mindcare.view.psychologue;

import com.mindcare.legacy.psychologue.SubmitOfferLegacyContent;
import com.mindcare.model.ServiceRequest;
import com.mindcare.utils.NavigationManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.io.IOException;

public class SubmitOfferView implements NavigationManager.Buildable {

    private final ServiceRequest request;

    public SubmitOfferView() {
        this.request = null;
    }

    public SubmitOfferView(ServiceRequest request) {
        this.request = request;
    }

    @Override
    public Node build() {
        if (request != null) {
            return new SubmitOfferLegacyContent(request).build();
        }
        try {
            return FXMLLoader.load(getClass().getResource("/com/mindcare/view/worker/SubmitOfferView.fxml"));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load SubmitOfferView.fxml", exception);
        }
    }
}
