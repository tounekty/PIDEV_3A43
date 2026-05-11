package org.example.repository.impl;

import org.example.config.DatabaseConnection;
import org.example.model.User;
import org.example.repository.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserRepositoryImpl implements UserRepository {

    @Override
    public List<User> findAll() throws SQLException {
        String sql = "SELECT id, email, first_name, last_name, role, email_verified, face_id_enabled, created_at, updated_at, banned_until FROM users ORDER BY id";
        List<User> users = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User user = new User(
                        rs.getInt("id"),
                        rs.getString("email"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("role"),
                        rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime()
                );
                user.setEmailVerified(rs.getBoolean("email_verified"));
                user.setFaceIdEnabled(rs.getBoolean("face_id_enabled"));
                if (rs.getTimestamp("banned_until") != null) {
                    user.setBannedUntil(rs.getTimestamp("banned_until").toLocalDateTime());
                }
                users.add(user);
            }
        }

        return users;
    }

    @Override
    public boolean existsByEmail(String email, Integer excludeUserId) throws SQLException {
        String sql = "SELECT id FROM users WHERE email = ?" + (excludeUserId != null ? " AND id <> ?" : "");

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            if (excludeUserId != null) {
                ps.setInt(2, excludeUserId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public void create(String email, String firstName, String lastName,
                       String hashedPassword, String role, java.time.LocalDateTime bannedUntil, boolean emailVerified) throws SQLException {
        String sql = """
                INSERT INTO users (
                    email, first_name, last_name, password, role, 
                    email_verified, is_verified, 
                    banned_until,
                    created_at, updated_at, 
                    face_id_enabled, face_id_subject,
                    verification_token, 
                    reset_code, reset_code_expires_at,
                    created_by_id, updated_by_id,
                    activation_token, activation_token_expires_at,
                    reset_password_token, reset_password_token_expires_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, firstName == null ? "" : firstName);
            ps.setString(3, lastName == null ? "" : lastName);
            ps.setString(4, hashedPassword);
            ps.setString(5, role != null ? role : "CLIENT");
            ps.setBoolean(6, emailVerified);
            ps.setBoolean(7, emailVerified);
            if (bannedUntil != null) {
                ps.setTimestamp(8, java.sql.Timestamp.valueOf(bannedUntil));
            } else {
                ps.setNull(8, java.sql.Types.TIMESTAMP);
            }
            ps.setTimestamp(9, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setTimestamp(10, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setBoolean(11, false); // face_id_enabled
            ps.setNull(12, java.sql.Types.VARCHAR); // face_id_subject
            ps.setNull(13, java.sql.Types.VARCHAR); // verification_token
            ps.setNull(14, java.sql.Types.VARCHAR); // reset_code
            ps.setNull(15, java.sql.Types.TIMESTAMP); // reset_code_expires_at
            ps.setNull(16, java.sql.Types.INTEGER); // created_by_id
            ps.setNull(17, java.sql.Types.INTEGER); // updated_by_id
            ps.setNull(18, java.sql.Types.VARCHAR); // activation_token
            ps.setNull(19, java.sql.Types.TIMESTAMP); // activation_token_expires_at
            ps.setNull(20, java.sql.Types.VARCHAR); // reset_password_token
            ps.setNull(21, java.sql.Types.TIMESTAMP); // reset_password_token_expires_at
            
            ps.executeUpdate();
        }
    }

    @Override
    public void update(int userId, String email, String firstName, String lastName,
                       String hashedPasswordOrNull, String role, java.time.LocalDateTime bannedUntil) throws SQLException {
        String sqlWithoutPassword = "UPDATE users SET email = ?, first_name = ?, last_name = ?, role = ?, banned_until = ? WHERE id = ?";
        String sqlWithPassword = "UPDATE users SET email = ?, first_name = ?, last_name = ?, password = ?, role = ?, banned_until = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection()) {
            if (hashedPasswordOrNull == null) {
                try (PreparedStatement ps = connection.prepareStatement(sqlWithoutPassword)) {
                    ps.setString(1, email);
                    ps.setString(2, firstName);
                    ps.setString(3, lastName);
                    ps.setString(4, role);
                    if (bannedUntil != null) {
                        ps.setTimestamp(5, java.sql.Timestamp.valueOf(bannedUntil));
                    } else {
                        ps.setNull(5, java.sql.Types.TIMESTAMP);
                    }
                    ps.setInt(6, userId);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = connection.prepareStatement(sqlWithPassword)) {
                    ps.setString(1, email);
                    ps.setString(2, firstName);
                    ps.setString(3, lastName);
                    ps.setString(4, hashedPasswordOrNull);
                    ps.setString(5, role);
                    if (bannedUntil != null) {
                        ps.setTimestamp(6, java.sql.Timestamp.valueOf(bannedUntil));
                    } else {
                        ps.setNull(6, java.sql.Types.TIMESTAMP);
                    }
                    ps.setInt(7, userId);
                    ps.executeUpdate();
                }
            }
        }
    }

    @Override
    public void delete(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public void updateFaceIdStatus(int userId, boolean enabled) throws SQLException {
        String sql = "UPDATE users SET face_id_enabled = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBoolean(1, enabled);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }
}

