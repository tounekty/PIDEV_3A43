package org.example.reservation;

import java.time.LocalDateTime;

public class ReservationRecord {
    private final int id;
    private final int eventId;
    private final int userId;
    private final String eventTitle;
    private final String authorName;
    private final LocalDateTime reservedAt;
    private final String status;

    public ReservationRecord(int id, int eventId, int userId, String eventTitle,
                             String authorName, LocalDateTime reservedAt, String status) {
        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.eventTitle = eventTitle;
        this.authorName = authorName;
        this.reservedAt = reservedAt;
        this.status = status != null ? status : "CONFIRMED";
    }

    // Constructeur de compatibilité (sans status)
    public ReservationRecord(int id, int eventId, int userId, String eventTitle,
                             String authorName, LocalDateTime reservedAt) {
        this(id, eventId, userId, eventTitle, authorName, reservedAt, "CONFIRMED");
    }

    public int getId()                  { return id; }
    public int getEventId()             { return eventId; }
    public int getUserId()              { return userId; }
    public String getEventTitle()       { return eventTitle; }
    public String getAuthorName()       { return authorName; }
    public LocalDateTime getReservedAt(){ return reservedAt; }
    public String getStatus()           { return status; }
}
