package org.example.event;

import java.time.LocalDateTime;

public class EventReview {
    private final int id;
    private final int eventId;
    private final int userId;
    private final String username;
    private final int rating;
    private final String comment;
    private final LocalDateTime createdAt;

    public EventReview(int id, int eventId, int userId, String username, int rating, String comment, LocalDateTime createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.username = username;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public int getEventId() {
        return eventId;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
