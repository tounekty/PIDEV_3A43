package org.example.repository;

import org.example.db.ConnectionFactory;
import org.example.model.User;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class UserRepository {
    public int createStudent(String firstName, String lastName, String email, String hashedPassword) throws SQLException {
        String sql = """
                INSERT INTO `user` (
                  first_name, last_name, email, role, password, banned_until,
                  reset_code, reset_code_expires_at, is_verified, verification_token, created_at
                )
                VALUES (?, ?, ?, 'etudiant', ?, NULL, NULL, NULL, 1, NULL, NOW())
                """;
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setString(4, hashedPassword);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("No generated id returned.");
            }
        }
    }

    public User findByEmail(String email) throws SQLException {
        String sql = """
                SELECT id, first_name, last_name, email, role, password, banned_until, is_verified
                FROM `user`
                WHERE LOWER(email) = LOWER(?)
                LIMIT 1
                """;
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
                return null;
            }
        }
    }

    public String findEmailForMoodId(int moodId) throws SQLException {
        return findEmailForMoodId(moodId, null);
    }

    public String findEmailForMoodId(int moodId, Integer excludedUserId) throws SQLException {
        try (Connection conn = ConnectionFactory.getConnection()) {
            String userIdColumn = null;
            if (hasColumn(conn, "mood", "id_user")) {
                userIdColumn = "id_user";
            } else if (hasColumn(conn, "mood", "user_id")) {
                userIdColumn = "user_id";
            }

            if (userIdColumn == null) {
                return findFirstStudentEmail(conn, excludedUserId);
            }

            String sql = """
                    SELECT u.email
                    FROM mood m
                    JOIN `user` u ON u.id = m.%s
                    WHERE m.id = ? AND LOWER(u.role) NOT LIKE '%%admin%%' %s
                    LIMIT 1
                    """.formatted(userIdColumn, excludedUserId == null ? "" : "AND u.id <> ?");
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, moodId);
                if (excludedUserId != null) {
                    ps.setInt(2, excludedUserId);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("email");
                    }
                }
            }
            return findFirstStudentEmail(conn, excludedUserId);
        }
    }

    public String findFirstAdminEmail() throws SQLException {
        try (Connection conn = ConnectionFactory.getConnection()) {
            String sql = """
                    SELECT email
                    FROM `user`
                    WHERE LOWER(role) LIKE '%admin%'
                    ORDER BY id ASC
                    LIMIT 1
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("email") : null;
            }
        }
    }

    private String findFirstStudentEmail(Connection conn, Integer excludedUserId) throws SQLException {
        String sql = """
                SELECT email
                FROM `user`
                WHERE (LOWER(role) = 'etudiant' OR LOWER(role) = 'student') %s
                ORDER BY id ASC
                LIMIT 1
                """.formatted(excludedUserId == null ? "" : "AND id <> ?");
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ) {
            if (excludedUserId != null) {
                ps.setInt(1, excludedUserId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("email") : null;
            }
        }
    }

    private User map(ResultSet rs) throws SQLException {
        Timestamp bannedUntil = rs.getTimestamp("banned_until");
        return new User(
                rs.getInt("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("role"),
                rs.getString("password"),
                bannedUntil == null ? null : bannedUntil.toLocalDateTime(),
                rs.getBoolean("is_verified")
        );
    }

    private boolean hasColumn(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(conn.getCatalog(), null, tableName, columnName)) {
            return rs.next();
        }
    }
}
