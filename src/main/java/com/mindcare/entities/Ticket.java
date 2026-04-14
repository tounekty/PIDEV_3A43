package com.mindcare.entities;

import java.time.LocalDateTime;

public class Ticket {

    public enum Status {
        OPEN,
        IN_PROGRESS,
        WAITING_USER,
        CLOSED;

        public static Status fromDb(String value) {
            if (value == null || value.isBlank()) {
                return OPEN;
            }
            String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
            for (Status status : values()) {
                if (status.name().equals(normalized)) {
                    return status;
                }
            }
            return OPEN;
        }
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH;

        public static Priority fromDb(String value) {
            if (value == null || value.isBlank()) {
                return MEDIUM;
            }
            String normalized = value.trim().toUpperCase();
            for (Priority priority : values()) {
                if (priority.name().equals(normalized)) {
                    return priority;
                }
            }
            return MEDIUM;
        }
    }

    private int id;
    private Integer userId;
    private Integer categoryId;
    private String subject;
    private String description;
    private Status status;
    private Priority priority;
    private String resolution;
    private int messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
    private String aiSentiment;
    private Integer aiUrgency;
    private String aiSuggestedPriority;
    private String aiSummary;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getAiSentiment() {
        return aiSentiment;
    }

    public void setAiSentiment(String aiSentiment) {
        this.aiSentiment = aiSentiment;
    }

    public Integer getAiUrgency() {
        return aiUrgency;
    }

    public void setAiUrgency(Integer aiUrgency) {
        this.aiUrgency = aiUrgency;
    }

    public String getAiSuggestedPriority() {
        return aiSuggestedPriority;
    }

    public void setAiSuggestedPriority(String aiSuggestedPriority) {
        this.aiSuggestedPriority = aiSuggestedPriority;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }
}
