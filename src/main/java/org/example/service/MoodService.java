package org.example.service;

import org.example.model.Mood;
import org.example.repository.MoodRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class MoodService {
    private final MoodRepository moodRepository = new MoodRepository();

    public Mood createMood(String moodType, LocalDate moodDate, String note, String sleepTime, String wakeTime, Double sleepHours) throws ServiceException {
        return createMood(moodType, moodDate, note, null, null, sleepTime, wakeTime, sleepHours, null);
    }

    public Mood createMood(String moodType, LocalDate moodDate, String note, Integer stressLevel, Integer energyLevel) throws ServiceException {
        return createMood(moodType, moodDate, note, stressLevel, energyLevel, null, null, null, null);
    }

    public Mood createMood(String moodType, LocalDate moodDate, String note, Integer stressLevel, Integer energyLevel,
                           Integer userId) throws ServiceException {
        return createMood(moodType, moodDate, note, stressLevel, energyLevel, null, null, null, null, userId);
    }

    public Mood createMood(String moodType, LocalDate moodDate, String note, String sleepTime, String wakeTime, Double sleepHours, String adminComment) throws ServiceException {
        return createMood(moodType, moodDate, note, null, null, sleepTime, wakeTime, sleepHours, adminComment);
    }

    public Mood createMood(String moodType, LocalDate moodDate, String note, Integer stressLevel, Integer energyLevel,
                           String sleepTime, String wakeTime, Double sleepHours, String adminComment) throws ServiceException {
        return createMood(moodType, moodDate, note, stressLevel, energyLevel, sleepTime, wakeTime, sleepHours, adminComment, null);
    }

    public Mood createMood(String moodType, LocalDate moodDate, String note, Integer stressLevel, Integer energyLevel,
                           String sleepTime, String wakeTime, Double sleepHours, String adminComment, Integer userId) throws ServiceException {
        validateMoodInput(moodType, moodDate, stressLevel, energyLevel, sleepTime, wakeTime, sleepHours);

        try {
            Mood mood = new Mood(0, moodType.trim(), moodDate, note, stressLevel, energyLevel, sleepTime, wakeTime, sleepHours, adminComment, false, null, null, userId);
            int id = moodRepository.create(mood);
            return new Mood(id, moodType.trim(), moodDate, note, stressLevel, energyLevel, sleepTime, wakeTime, sleepHours, adminComment, false, null, null, userId);
        } catch (SQLException e) {
            throw new ServiceException("Failed to create mood: " + e.getMessage(), e);
        }
    }

    public boolean updateMood(int id, String moodType, LocalDate moodDate, String note, String sleepTime, String wakeTime, Double sleepHours) throws ServiceException {
        return updateMood(id, moodType, moodDate, note, null, null, sleepTime, wakeTime, sleepHours, null);
    }

    public boolean updateMood(int id, String moodType, LocalDate moodDate, String note, Integer stressLevel, Integer energyLevel) throws ServiceException {
        return updateMood(id, moodType, moodDate, note, stressLevel, energyLevel, null, null, null, null);
    }

    public boolean updateMood(int id, String moodType, LocalDate moodDate, String note, String sleepTime, String wakeTime, Double sleepHours, String adminComment) throws ServiceException {
        return updateMood(id, moodType, moodDate, note, null, null, sleepTime, wakeTime, sleepHours, adminComment);
    }

    public boolean updateMood(int id, String moodType, LocalDate moodDate, String note, Integer stressLevel, Integer energyLevel,
                              String sleepTime, String wakeTime, Double sleepHours, String adminComment) throws ServiceException {
        return updateMood(id, moodType, moodDate, note, stressLevel, energyLevel, sleepTime, wakeTime, sleepHours, adminComment, false);
    }

    public boolean updateMood(int id, String moodType, LocalDate moodDate, String note, Integer stressLevel, Integer energyLevel,
                              String sleepTime, String wakeTime, Double sleepHours, String adminComment, boolean supportEmailSent) throws ServiceException {
        return updateMood(id, moodType, moodDate, note, stressLevel, energyLevel, sleepTime, wakeTime, sleepHours, adminComment, supportEmailSent, null);
    }

    public boolean updateMood(int id, String moodType, LocalDate moodDate, String note, Integer stressLevel, Integer energyLevel,
                              String sleepTime, String wakeTime, Double sleepHours, String adminComment, boolean supportEmailSent,
                              String caseStatus) throws ServiceException {
        validateMoodInput(moodType, moodDate, stressLevel, energyLevel, sleepTime, wakeTime, sleepHours);

        try {
            Mood existing = moodRepository.findById(id);
            String nextStatus = caseStatus == null && existing != null ? existing.getCaseStatus() : caseStatus;
            Mood mood = new Mood(id, moodType.trim(), moodDate, note, stressLevel, energyLevel, sleepTime, wakeTime, sleepHours,
                    adminComment, supportEmailSent, null, null, existing == null ? null : existing.getUserId(), nextStatus);
            return moodRepository.update(mood);
        } catch (SQLException e) {
            throw new ServiceException("Failed to update mood: " + e.getMessage(), e);
        }
    }

    public boolean updateCaseStatus(int id, String caseStatus) throws ServiceException {
        if (!isValidCaseStatus(caseStatus)) {
            throw new ServiceException("Invalid case status.");
        }
        Mood mood = getMoodById(id);
        if (mood == null) {
            throw new ServiceException("Mood not found.");
        }
        boolean supportEmailSent = mood.isSupportEmailSent() || "Support email sent".equals(caseStatus) || "Solved".equals(caseStatus);
        return updateMood(
                mood.getId(),
                mood.getMoodType(),
                mood.getMoodDate(),
                mood.getNote(),
                mood.getStressLevel(),
                mood.getEnergyLevel(),
                mood.getSleepTime(),
                mood.getWakeTime(),
                mood.getSleepHours(),
                mood.getAdminComment(),
                supportEmailSent,
                caseStatus
        );
    }

    public boolean markSupportEmailSent(int id) throws ServiceException {
        Mood mood = getMoodById(id);
        if (mood == null) {
            throw new ServiceException("Mood not found.");
        }
        return updateMood(
                mood.getId(),
                mood.getMoodType(),
                mood.getMoodDate(),
                mood.getNote(),
                mood.getStressLevel(),
                mood.getEnergyLevel(),
                mood.getSleepTime(),
                mood.getWakeTime(),
                mood.getSleepHours(),
                mood.getAdminComment(),
                true,
                "Support email sent"
        );
    }

    public boolean deleteMood(int id) throws ServiceException {
        try {
            return moodRepository.delete(id);
        } catch (SQLException e) {
            throw new ServiceException("Failed to delete mood: " + e.getMessage(), e);
        }
    }

    public List<Mood> getAllMoods() throws ServiceException {
        try {
            return moodRepository.findAll();
        } catch (SQLException e) {
            throw new ServiceException("Failed to retrieve moods: " + e.getMessage(), e);
        }
    }

    public Mood getMoodById(int id) throws ServiceException {
        try {
            return moodRepository.findById(id);
        } catch (SQLException e) {
            throw new ServiceException("Failed to retrieve mood: " + e.getMessage(), e);
        }
    }

    public List<Mood> getMoodsByDate(LocalDate date) throws ServiceException {
        try {
            return moodRepository.findByDate(date);
        } catch (SQLException e) {
            throw new ServiceException("Failed to retrieve moods for date: " + e.getMessage(), e);
        }
    }

    public MoodStatistics getMoodStatistics() throws ServiceException {
        try {
            List<Mood> allMoods = moodRepository.findAll();
            return new MoodStatistics(allMoods);
        } catch (SQLException e) {
            throw new ServiceException("Failed to calculate statistics: " + e.getMessage(), e);
        }
    }

    private void validateMoodInput(String moodType, LocalDate moodDate, Integer stressLevel, Integer energyLevel,
                                   String sleepTime, String wakeTime, Double sleepHours) throws ServiceException {
        if (moodType == null || moodType.trim().isEmpty()) {
            throw new ServiceException("Mood type cannot be empty");
        }
        if (moodType.trim().length() < 2 || moodType.trim().length() > FormValidator.MOOD_TYPE_MAX_LENGTH) {
            throw new ServiceException("Mood type must contain between 2 and " + FormValidator.MOOD_TYPE_MAX_LENGTH + " characters");
        }
        if (moodDate == null) {
            throw new ServiceException("Mood date cannot be null");
        }
        if (moodDate.isAfter(LocalDate.now())) {
            throw new ServiceException("Mood date cannot be in the future");
        }
        if (!isValidLevel(stressLevel)) {
            throw new ServiceException("Stress level must be between 1 and 10");
        }
        if (!isValidLevel(energyLevel)) {
            throw new ServiceException("Energy level must be between 1 and 10");
        }
        if (sleepTime != null && !sleepTime.isBlank() && !sleepTime.matches("^([01]\\d|2[0-3]):[0-5]\\d$")) {
            throw new ServiceException("Sleep time must use HH:mm format");
        }
        if (wakeTime != null && !wakeTime.isBlank() && !wakeTime.matches("^([01]\\d|2[0-3]):[0-5]\\d$")) {
            throw new ServiceException("Wake time must use HH:mm format");
        }
        if (sleepHours != null && (sleepHours < FormValidator.SLEEP_HOURS_MIN || sleepHours > FormValidator.SLEEP_HOURS_MAX)) {
            throw new ServiceException("Sleep hours must be between " + FormValidator.SLEEP_HOURS_MIN + " and " + FormValidator.SLEEP_HOURS_MAX);
        }
    }

    private boolean isValidLevel(Integer level) {
        return level == null || (level >= FormValidator.LEVEL_MIN && level <= FormValidator.LEVEL_MAX);
    }

    private boolean isValidCaseStatus(String status) {
        return "New".equals(status)
                || "In review".equals(status)
                || "Support email sent".equals(status)
                || "Appointment proposed".equals(status)
                || "Escalated to psychologist".equals(status)
                || "Solved".equals(status);
    }
}
