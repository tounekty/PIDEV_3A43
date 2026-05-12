package org.example.repository.impl;

import com.mindcare.db.DBConnection;
import org.example.model.Journal;
import org.example.repository.JournalRepository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JournalRepositoryImpl implements JournalRepository {
    
    @Override
    public void createTableIfNotExists() throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS journal (
                id INT AUTO_INCREMENT PRIMARY KEY,
                title VARCHAR(255) NOT NULL DEFAULT 'Untitled',
                content TEXT,
                entry_date DATE NOT NULL DEFAULT '2000-01-01',
                mood_id INT NULL,
                admin_comment TEXT,
                user_id INT NULL,
                INDEX idx_journal_user (user_id),
                INDEX idx_journal_date (entry_date),
                INDEX idx_journal_mood (mood_id),
                FOREIGN KEY (mood_id) REFERENCES mood(id) ON DELETE SET NULL,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """;
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
        }
    }

    @Override
    public void save(Journal journal) throws SQLException {
        String insertSQL = """
            INSERT INTO journal (title, content, entry_date, mood_id, admin_comment, user_id)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, journal.getTitle());
            pstmt.setString(2, journal.getContent());
            pstmt.setDate(3, journal.getEntryDate() != null ? Date.valueOf(journal.getEntryDate()) : Date.valueOf(LocalDate.now()));
            pstmt.setObject(4, journal.getMoodId());
            pstmt.setString(5, journal.getAdminComment());
            pstmt.setObject(6, journal.getUserId());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating journal failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    journal.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating journal failed, no ID obtained.");
                }
            }
        }
    }

    @Override
    public Journal findById(int id) throws SQLException {
        String selectSQL = "SELECT * FROM journal WHERE id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToJournal(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Journal> findAll() throws SQLException {
        List<Journal> journals = new ArrayList<>();
        String selectSQL = "SELECT * FROM journal ORDER BY entry_date DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                journals.add(mapResultSetToJournal(rs));
            }
        }
        return journals;
    }

    @Override
    public List<Journal> findByUserId(int userId) throws SQLException {
        List<Journal> journals = new ArrayList<>();
        String selectSQL = "SELECT * FROM journal WHERE user_id = ? ORDER BY entry_date DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    journals.add(mapResultSetToJournal(rs));
                }
            }
        }
        return journals;
    }

    @Override
    public void update(Journal journal) throws SQLException {
        String updateSQL = """
            UPDATE journal SET 
                title = ?, 
                content = ?, 
                entry_date = ?, 
                mood_id = ?, 
                admin_comment = ?, 
                user_id = ?
            WHERE id = ?
            """;
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            
            pstmt.setString(1, journal.getTitle());
            pstmt.setString(2, journal.getContent());
            pstmt.setDate(3, journal.getEntryDate() != null ? Date.valueOf(journal.getEntryDate()) : Date.valueOf(LocalDate.now()));
            pstmt.setObject(4, journal.getMoodId());
            pstmt.setString(5, journal.getAdminComment());
            pstmt.setObject(6, journal.getUserId());
            pstmt.setInt(7, journal.getId());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating journal failed, no rows affected.");
            }
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String deleteSQL = "DELETE FROM journal WHERE id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
            
            pstmt.setInt(1, id);
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting journal failed, no rows affected.");
            }
        }
    }

    @Override
    public List<Journal> findByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        List<Journal> journals = new ArrayList<>();
        String selectSQL = "SELECT * FROM journal WHERE entry_date BETWEEN ? AND ? ORDER BY entry_date DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            
            pstmt.setDate(1, Date.valueOf(startDate));
            pstmt.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    journals.add(mapResultSetToJournal(rs));
                }
            }
        }
        return journals;
    }

    @Override
    public List<Journal> findByMoodId(int moodId) throws SQLException {
        List<Journal> journals = new ArrayList<>();
        String selectSQL = "SELECT * FROM journal WHERE mood_id = ? ORDER BY entry_date DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            
            pstmt.setInt(1, moodId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    journals.add(mapResultSetToJournal(rs));
                }
            }
        }
        return journals;
    }

    private Journal mapResultSetToJournal(ResultSet rs) throws SQLException {
        Journal journal = new Journal();
        journal.setId(rs.getInt("id"));
        journal.setTitle(rs.getString("title"));
        journal.setContent(rs.getString("content"));
        
        Date entryDate = rs.getDate("entry_date");
        if (entryDate != null) {
            journal.setEntryDate(entryDate.toLocalDate());
        }
        
        journal.setMoodId(rs.getObject("mood_id", Integer.class));
        journal.setAdminComment(rs.getString("admin_comment"));
        journal.setUserId(rs.getObject("user_id", Integer.class));
        
        return journal;
    }
}
