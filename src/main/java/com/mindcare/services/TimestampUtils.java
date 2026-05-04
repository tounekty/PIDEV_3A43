package com.mindcare.services;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Utility class for safe timestamp conversion.
 * Handles null and zero dates gracefully.
 */
public class TimestampUtils {

    /**
     * Safely converts a SQL Timestamp to LocalDateTime.
     * Returns null if timestamp is null or represents a zero/epoch date.
     *
     * @param timestamp SQL timestamp (may be null)
     * @return LocalDateTime or null if timestamp is invalid
     */
    public static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null || timestamp.getTime() <= 0) {
            return null;
        }
        try {
            return timestamp.toLocalDateTime();
        } catch (Exception e) {
            // Handle any conversion errors gracefully
            return null;
        }
    }
}
