package com.mindcare.view.client;

import com.mindcare.utils.NavigationManager;
import javafx.scene.Node;

/**
 * JournalView – delegates to {@link MoodJournalView} so the Journal sidebar
 * link opens the same combined Mood / Journal / Heatmap module with the
 * Journal tab selected by default. Keeping this class avoids touching the
 * existing sidebar wiring while ensuring a single, consistent design.
 */
public class JournalView implements NavigationManager.Buildable {

    @Override
    public Node build() {
        return new MoodJournalView(MoodJournalView.InitialTab.JOURNAL).build();
    }
}
