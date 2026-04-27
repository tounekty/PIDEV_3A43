package org.example.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Validation service for mood & journal input validation.
 */
public class FormValidator {
    public static final int MOOD_TYPE_MAX_LENGTH = 50;
    public static final int MOOD_NOTE_MAX_LENGTH = 500;
    public static final int JOURNAL_TITLE_MAX_LENGTH = 100;
    public static final int JOURNAL_CONTENT_MAX_LENGTH = 5000;
    public static final int ADMIN_COMMENT_MAX_LENGTH = 1000;
    public static final int LEVEL_MIN = 1;
    public static final int LEVEL_MAX = 10;
    public static final double SLEEP_HOURS_MIN = 0.0;
    public static final double SLEEP_HOURS_MAX = 24.0;
    private static final String TIME_PATTERN = "^([01]\\d|2[0-3]):[0-5]\\d$";

    private FormValidator() {
    }

    /**
     * Validate mood form inputs.
     */
    public static void validateMoodForm(String moodType, LocalDate moodDate, String note, String sleepTime, String wakeTime, String sleepHours) throws ValidationException {
        validateMoodForm(moodType, moodDate, note, null, null, sleepTime, wakeTime, sleepHours);
    }

    public static void validateMoodForm(String moodType, LocalDate moodDate, String note, Integer stressLevel, Integer energyLevel) throws ValidationException {
        validateMoodForm(moodType, moodDate, note, stressLevel, energyLevel, null, null, null);
    }

    public static void validateMoodForm(String moodType, LocalDate moodDate, String note, Integer stressLevel, Integer energyLevel,
                                        String sleepTime, String wakeTime, String sleepHours) throws ValidationException {
        List<String> errors = new ArrayList<>();

        if (moodType == null || moodType.trim().isEmpty()) {
            errors.add("- Mood type is required");
        } else if (moodType.trim().length() < 2) {
            errors.add("- Mood type must be at least 2 characters");
        } else if (moodType.trim().length() > MOOD_TYPE_MAX_LENGTH) {
            errors.add("- Mood type cannot exceed " + MOOD_TYPE_MAX_LENGTH + " characters");
        }

        if (moodDate == null) {
            errors.add("- Mood date is required");
        } else if (moodDate.isAfter(LocalDate.now())) {
            errors.add("- Mood date cannot be in the future");
        }

        if (note != null && note.trim().length() > MOOD_NOTE_MAX_LENGTH) {
            errors.add("- Note cannot exceed " + MOOD_NOTE_MAX_LENGTH + " characters");
        }

        if (!isValidLevel(stressLevel)) {
            errors.add("- Stress level must be between " + LEVEL_MIN + " and " + LEVEL_MAX);
        }

        if (!isValidLevel(energyLevel)) {
            errors.add("- Energy level must be between " + LEVEL_MIN + " and " + LEVEL_MAX);
        }

        if (sleepTime != null && !sleepTime.trim().isEmpty() && !sleepTime.trim().matches(TIME_PATTERN)) {
            errors.add("- Sleep time must use HH:mm format");
        }

        if (wakeTime != null && !wakeTime.trim().isEmpty() && !wakeTime.trim().matches(TIME_PATTERN)) {
            errors.add("- Wake time must use HH:mm format");
        }

        if (sleepHours != null && !sleepHours.trim().isEmpty()) {
            try {
                double hours = Double.parseDouble(sleepHours.trim());
                if (hours < SLEEP_HOURS_MIN || hours > SLEEP_HOURS_MAX) {
                    errors.add("- Sleep hours must be between " + SLEEP_HOURS_MIN + " and " + SLEEP_HOURS_MAX);
                }
            } catch (NumberFormatException e) {
                errors.add("- Sleep hours must be a valid number");
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Validation Errors:\n" + String.join("\n", errors));
        }
    }

    private static boolean isValidLevel(Integer level) {
        return level == null || (level >= LEVEL_MIN && level <= LEVEL_MAX);
    }

    /**
     * Validate journal form inputs.
     */
    public static void validateJournalForm(String title, String content, LocalDate date, String moodId) throws ValidationException {
        List<String> errors = new ArrayList<>();

        if (title == null || title.trim().isEmpty()) {
            errors.add("- Journal title is required");
        } else if (title.trim().length() < 3) {
            errors.add("- Journal title must be at least 3 characters");
        } else if (title.trim().length() > JOURNAL_TITLE_MAX_LENGTH) {
            errors.add("- Journal title cannot exceed " + JOURNAL_TITLE_MAX_LENGTH + " characters");
        }

        if (content == null || content.trim().isEmpty()) {
            errors.add("- Journal content is required");
        } else if (content.trim().length() < 10) {
            errors.add("- Journal content must be at least 10 characters");
        } else if (content.trim().length() > JOURNAL_CONTENT_MAX_LENGTH) {
            errors.add("- Journal content cannot exceed " + JOURNAL_CONTENT_MAX_LENGTH + " characters");
        }

        if (date == null) {
            errors.add("- Entry date is required");
        } else if (date.isAfter(LocalDate.now())) {
            errors.add("- Entry date cannot be in the future");
        }

        if (moodId != null && !moodId.trim().isEmpty()) {
            try {
                int id = Integer.parseInt(moodId.trim());
                if (id <= 0) {
                    errors.add("- Mood ID must be a positive number");
                }
            } catch (NumberFormatException e) {
                errors.add("- Mood ID must be a valid number");
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Validation Errors:\n" + String.join("\n", errors));
        }
    }

    /**
     * Validate mood type string.
     */
    public static void validateMoodType(String moodType) throws ValidationException {
        if (moodType == null || moodType.trim().isEmpty()) {
            throw new ValidationException("Please select or enter a mood type");
        }
    }

    public static void validateSleepHours(Double sleepHours) throws ValidationException {
        if (sleepHours != null && (sleepHours < SLEEP_HOURS_MIN || sleepHours > SLEEP_HOURS_MAX)) {
            throw new ValidationException("Sleep hours must be between " + SLEEP_HOURS_MIN + " and " + SLEEP_HOURS_MAX);
        }
    }

    /**
     * Validate admin comment.
     */
    public static void validateAdminComment(String comment) throws ValidationException {
        if (comment != null && comment.trim().length() > ADMIN_COMMENT_MAX_LENGTH) {
            throw new ValidationException("Admin comment cannot exceed " + ADMIN_COMMENT_MAX_LENGTH + " characters");
        }
    }

    /**
     * Validate email format (helper).
     */
    public static void validateEmail(String email) throws ValidationException {
        if (email == null || !email.matches("[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ValidationException("Invalid email format");
        }
    }
}
