package org.example.service;

import org.example.model.Mood;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Alert system for mood tracking.
 */
public class MoodAlert {
    private final String alertType;      // "BAD_MOOD_STREAK", "LOW_MOOD_THIS_MONTH", etc.
    private final String title;
    private final String message;
    private final int severity;          // 1 (low) to 5 (critical)
    private final LocalDate detectedDate;
    private final List<Mood> triggeredBy;

    public MoodAlert(String alertType, String title, String message, int severity, LocalDate detectedDate, List<Mood> triggeredBy) {
        this.alertType = alertType;
        this.title = title;
        this.message = message;
        this.severity = severity;
        this.detectedDate = detectedDate;
        this.triggeredBy = triggeredBy;
    }

    public String getAlertType() {
        return alertType;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public int getSeverity() {
        return severity;
    }

    public LocalDate getDetectedDate() {
        return detectedDate;
    }

    public List<Mood> getTriggeredBy() {
        return triggeredBy;
    }

    public String getSeverityLabel() {
        return switch (severity) {
            case 1 -> "LOW";
            case 2 -> "MEDIUM";
            case 3 -> "HIGH";
            case 4 -> "CRITICAL";
            case 5 -> "EMERGENCY";
            default -> "UNKNOWN";
        };
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s (Severity: %s)", 
                alertType, title, message, getSeverityLabel());
    }
}
