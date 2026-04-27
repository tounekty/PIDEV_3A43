package org.example.service;

import org.example.model.Journal;
import org.example.repository.JournalRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for journal management.
 */
public class JournalService {
    private final JournalRepository journalRepository = new JournalRepository();

    /**
     * Create a new journal entry with validation.
     */
    public Journal createJournal(String title, String content, LocalDate entryDate, Integer moodId) throws ServiceException {
        return createJournal(title, content, entryDate, moodId, null);
    }

    public Journal createJournal(String title, String content, LocalDate entryDate, Integer moodId, String adminComment) throws ServiceException {
        validateJournalInput(title, content, entryDate);

        try {
            Journal journal = new Journal(0, title.trim(), content.trim(), entryDate, moodId, adminComment);
            int id = journalRepository.create(journal);
            return new Journal(id, title.trim(), content.trim(), entryDate, moodId, adminComment);
        } catch (SQLException e) {
            throw new ServiceException("Failed to create journal entry: " + e.getMessage(), e);
        }
    }

    /**
     * Update existing journal entry.
     */
    public boolean updateJournal(int id, String title, String content, LocalDate entryDate, Integer moodId) throws ServiceException {
        return updateJournal(id, title, content, entryDate, moodId, null);
    }

    public boolean updateJournal(int id, String title, String content, LocalDate entryDate, Integer moodId, String adminComment) throws ServiceException {
        validateJournalInput(title, content, entryDate);

        try {
            Journal journal = new Journal(id, title.trim(), content.trim(), entryDate, moodId, adminComment);
            return journalRepository.update(journal);
        } catch (SQLException e) {
            throw new ServiceException("Failed to update journal entry: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a journal entry.
     */
    public boolean deleteJournal(int id) throws ServiceException {
        try {
            return journalRepository.delete(id);
        } catch (SQLException e) {
            throw new ServiceException("Failed to delete journal entry: " + e.getMessage(), e);
        }
    }

    /**
     * Get all journal entries.
     */
    public List<Journal> getAllJournals() throws ServiceException {
        try {
            return journalRepository.findAll();
        } catch (SQLException e) {
            throw new ServiceException("Failed to retrieve journal entries: " + e.getMessage(), e);
        }
    }

    /**
     * Get a journal entry by ID.
     */
    public Journal getJournalById(int id) throws ServiceException {
        try {
            return journalRepository.findById(id);
        } catch (SQLException e) {
            throw new ServiceException("Failed to retrieve journal entry: " + e.getMessage(), e);
        }
    }

    /**
     * Get journal entries for a specific date.
     */
    public List<Journal> getJournalsByDate(LocalDate date) throws ServiceException {
        try {
            return journalRepository.findByDate(date);
        } catch (SQLException e) {
            throw new ServiceException("Failed to retrieve journal entries for date: " + e.getMessage(), e);
        }
    }

    /**
     * Get journal entries linked to a specific mood.
     */
    public List<Journal> getJournalsByMoodId(int moodId) throws ServiceException {
        try {
            return journalRepository.findByMoodId(moodId);
        } catch (SQLException e) {
            throw new ServiceException("Failed to retrieve journal entries for mood: " + e.getMessage(), e);
        }
    }

    /**
     * Count entries for this month.
     */
    public long countThisMonth() throws ServiceException {
        try {
            return journalRepository.countThisMonth();
        } catch (SQLException e) {
            throw new ServiceException("Failed to count entries: " + e.getMessage(), e);
        }
    }

    private void validateJournalInput(String title, String content, LocalDate entryDate) throws ServiceException {
        if (title == null || title.trim().isEmpty()) {
            throw new ServiceException("Journal title cannot be empty");
        }
        if (title.trim().length() < 3 || title.trim().length() > FormValidator.JOURNAL_TITLE_MAX_LENGTH) {
            throw new ServiceException("Journal title must contain between 3 and " + FormValidator.JOURNAL_TITLE_MAX_LENGTH + " characters");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new ServiceException("Journal content cannot be empty");
        }
        if (content.trim().length() < 10 || content.trim().length() > FormValidator.JOURNAL_CONTENT_MAX_LENGTH) {
            throw new ServiceException("Journal content must contain between 10 and " + FormValidator.JOURNAL_CONTENT_MAX_LENGTH + " characters");
        }
        if (entryDate == null) {
            throw new ServiceException("Entry date cannot be null");
        }
        if (entryDate.isAfter(LocalDate.now())) {
            throw new ServiceException("Entry date cannot be in the future");
        }
    }
}
