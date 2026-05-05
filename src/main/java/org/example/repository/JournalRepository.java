package org.example.repository;

import org.example.model.Journal;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface JournalRepository {
    void createTableIfNotExists() throws SQLException;
    void save(Journal journal) throws SQLException;
    Journal findById(int id) throws SQLException;
    List<Journal> findAll() throws SQLException;
    List<Journal> findByUserId(int userId) throws SQLException;
    void update(Journal journal) throws SQLException;
    void delete(int id) throws SQLException;
    List<Journal> findByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException;
    List<Journal> findByMoodId(int moodId) throws SQLException;
}
