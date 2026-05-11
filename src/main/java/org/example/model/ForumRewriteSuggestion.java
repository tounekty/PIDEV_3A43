package org.example.model;

public class ForumRewriteSuggestion {
    private final String title;
    private final String description;

    public ForumRewriteSuggestion(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
