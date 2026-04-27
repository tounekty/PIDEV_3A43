package org.example.repository;

import org.example.db.ConnectionFactory;
import org.example.model.Journal;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class JournalRepository {

    public int create(Journal journal) throws SQLException {
        String sql = "INSERT INTO journal (title, content, entry_date, mood_id, admin_comment) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, journal.getTitle());
            ps.setString(2, journal.getContent());
            ps.setDate(3, Date.valueOf(journal.getEntryDate()));
            setMoodId(ps, 4, journal.getMoodId());
            ps.setString(5, journal.getAdminComment());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("No generated id returned.");
            }
        }
    }

    public List<Journal> findAll() throws SQLException {
        String sql = "SELECT id, title, content, entry_date, mood_id, admin_comment FROM journal ORDER BY id DESC";
        List<Journal> journals = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                journals.add(map(rs));
            }
        }
        return journals;
    }

    public Journal findById(int id) throws SQLException {
        String sql = "SELECT id, title, content, entry_date, mood_id, admin_comment FROM journal WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
                return null;
            }
        }
    }

    public boolean update(Journal journal) throws SQLException {
        String sql = "UPDATE journal SET title = ?, content = ?, entry_date = ?, mood_id = ?, admin_comment = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, journal.getTitle());
            ps.setString(2, journal.getContent());
            ps.setDate(3, Date.valueOf(journal.getEntryDate()));
            setMoodId(ps, 4, journal.getMoodId());
            ps.setString(5, journal.getAdminComment());
            ps.setInt(6, journal.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM journal WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Journal> findByDate(java.time.LocalDate date) throws SQLException {
        String sql = "SELECT id, title, content, entry_date, mood_id, admin_comment FROM journal WHERE entry_date = ? ORDER BY id DESC";
        List<Journal> journals = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    journals.add(map(rs));
                }
            }
        }
        return journals;
    }

    public List<Journal> findByMoodId(int moodId) throws SQLException {
        String sql = "SELECT id, title, content, entry_date, mood_id, admin_comment FROM journal WHERE mood_id = ? ORDER BY id DESC";
        List<Journal> journals = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, moodId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    journals.add(map(rs));
                }
            }
        }
        return journals;
    }

    public long countThisMonth() throws SQLException {
        String sql = "SELECT COUNT(*) FROM journal WHERE YEAR(entry_date) = YEAR(NOW()) AND MONTH(entry_date) = MONTH(NOW())";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        }
    }

    private Journal map(ResultSet rs) throws SQLException {
        int moodIdValue = rs.getInt("mood_id");
        Integer moodId = rs.wasNull() ? null : moodIdValue;
        return new Journal(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getDate("entry_date").toLocalDate(),
                moodId,
                rs.getString("admin_comment")
        );
    }

    private void setMoodId(PreparedStatement ps, int index, Integer moodId) throws SQLException {
        if (moodId == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, moodId);
        }
    }
}
