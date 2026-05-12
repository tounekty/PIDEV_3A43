package org.example.service;

import org.example.model.Journal;
import org.example.repository.JournalRepository;
import org.example.repository.impl.JournalRepositoryImpl;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class JournalService {
    private final JournalRepository journalRepository;

    public JournalService() {
        this.journalRepository = new JournalRepositoryImpl();
    }

    public void createTableIfNotExists() throws SQLException {
        journalRepository.createTableIfNotExists();
    }

    public Journal createJournal(String title, String content, LocalDate entryDate, Integer moodId, Integer userId) throws SQLException {
        if (title == null || title.trim().isEmpty()) {
            throw new SQLException("Journal title is required");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new SQLException("Journal content is required");
        }
        if (entryDate == null) {
            entryDate = LocalDate.now();
        }

        Journal journal = new Journal(title.trim(), content.trim(), entryDate, moodId, userId);
        journalRepository.save(journal);
        return journal;
    }

    public Journal getJournalById(int id) throws SQLException {
        return journalRepository.findById(id);
    }

    public List<Journal> getAllJournals() throws SQLException {
        return journalRepository.findAll();
    }

    public List<Journal> getJournalsByUserId(int userId) throws SQLException {
        return journalRepository.findByUserId(userId);
    }

    public void updateJournal(int id, String title, String content, LocalDate entryDate, Integer moodId) throws SQLException {
        Journal journal = journalRepository.findById(id);
        if (journal == null) {
            throw new SQLException("Journal entry not found");
        }

        if (title != null && !title.trim().isEmpty()) {
            journal.setTitle(title.trim());
        }
        if (content != null && !content.trim().isEmpty()) {
            journal.setContent(content.trim());
        }
        if (entryDate != null) {
            journal.setEntryDate(entryDate);
        }
        journal.setMoodId(moodId);

        journalRepository.update(journal);
    }

    public void deleteJournal(int id) throws SQLException {
        Journal journal = journalRepository.findById(id);
        if (journal == null) {
            throw new SQLException("Journal entry not found");
        }
        journalRepository.delete(id);
    }

    public List<Journal> getJournalsByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        if (startDate == null || endDate == null) {
            throw new SQLException("Start date and end date are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new SQLException("Start date cannot be after end date");
        }
        return journalRepository.findByDateRange(startDate, endDate);
    }

    public List<Journal> getJournalsByMoodId(int moodId) throws SQLException {
        return journalRepository.findByMoodId(moodId);
    }

    public void addAdminComment(int id, String adminComment) throws SQLException {
        Journal journal = journalRepository.findById(id);
        if (journal == null) {
            throw new SQLException("Journal entry not found");
        }
        journal.setAdminComment(adminComment);
        journalRepository.update(journal);
    }

    public Journal getLatestJournalByUserId(int userId) throws SQLException {
        List<Journal> journals = getJournalsByUserId(userId);
        return journals.isEmpty() ? null : journals.get(0);
    }
}
