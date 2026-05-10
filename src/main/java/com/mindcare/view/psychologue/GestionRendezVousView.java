package com.mindcare.view.psychologue;

import com.mindcare.legacy.psychologue.GestionRendezVousLegacyContent;
import com.mindcare.utils.NavigationManager;
import javafx.scene.Node;

public class GestionRendezVousView implements NavigationManager.Buildable {

    @Override
    public Node build() {
        return new GestionRendezVousLegacyContent().build();
    }
}
