package com.mindcare.services;

import java.time.LocalDateTime;

public class ZoomMeetingData {

    private final String meetingId;
    private final String joinUrl;
    private final LocalDateTime createdAt;

    public ZoomMeetingData(String meetingId, String joinUrl, LocalDateTime createdAt) {
        this.meetingId = meetingId;
        this.joinUrl = joinUrl;
        this.createdAt = createdAt;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public String getJoinUrl() {
        return joinUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
