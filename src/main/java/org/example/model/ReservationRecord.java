package org.example.model;

import java.time.LocalDateTime;

public class ReservationRecord {
    private final int id;
    private final int eventId;
    private final int userId;
    private final String eventTitle;
    private final String authorName;
    private final LocalDateTime reservedAt;

    public ReservationRecord(int id, int eventId, int userId, String eventTitle, String authorName, LocalDateTime reservedAt) {
        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.eventTitle = eventTitle;
        this.authorName = authorName;
        this.reservedAt = reservedAt;
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

    public String getEventTitle() {
        return eventTitle;
    }

    public String getAuthorName() {
        return authorName;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }
}
