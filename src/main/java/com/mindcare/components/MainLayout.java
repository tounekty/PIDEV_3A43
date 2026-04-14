package com.mindcare.components;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

/**
 * MainLayout – the core application shell.
 * Sidebar on the left, topbar at top, scrollable content area in center.
 */
public class MainLayout extends BorderPane {

    private final SidebarComponent sidebar;
    private final TopbarComponent topbar;
    private final StackPane contentArea;

    public MainLayout(String title) {
        getStyleClass().add("main-layout");

        sidebar     = new SidebarComponent();
        topbar      = new TopbarComponent(title);
        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");

        setLeft(sidebar);
        setTop(topbar);
        setCenter(contentArea);
    }

    public void setContent(Node content) {
        contentArea.getChildren().setAll(content);
    }

    public void setTitle(String title) {
        topbar.setTitle(title);
    }

    public SidebarComponent getSidebar()   { return sidebar; }
    public TopbarComponent  getTopbar()    { return topbar; }
    public StackPane        getContentArea() { return contentArea; }
}
