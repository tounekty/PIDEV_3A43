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
        String sql = "SELECT id, username, email, first_name, last_name, role, email_verified, face_id_enabled, created_at, updated_at, banned_until FROM users ORDER BY id";
        List<User> users = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User user = new User(
                        rs.getInt("id"),
                        rs.getString("username"),
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
    public boolean existsByUsernameOrEmail(String username, String email, Integer excludeUserId) throws SQLException {
        String sql = "SELECT id FROM users WHERE (username = ? OR email = ?)" + (excludeUserId != null ? " AND id <> ?" : "");

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            if (excludeUserId != null) {
                ps.setInt(3, excludeUserId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public void create(String username, String email, String firstName, String lastName,
                       String hashedPassword, String role, java.time.LocalDateTime bannedUntil, boolean emailVerified) throws SQLException {
        String sql = "INSERT INTO users (username, email, first_name, last_name, password, role, banned_until, email_verified) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, firstName);
            ps.setString(4, lastName);
            ps.setString(5, hashedPassword);
            ps.setString(6, role);
            if (bannedUntil != null) {
                ps.setTimestamp(7, java.sql.Timestamp.valueOf(bannedUntil));
            } else {
                ps.setNull(7, java.sql.Types.TIMESTAMP);
            }
            ps.setBoolean(8, emailVerified);
            ps.executeUpdate();
        }
    }

    @Override
    public void update(int userId, String username, String email, String firstName, String lastName,
                       String hashedPasswordOrNull, String role, java.time.LocalDateTime bannedUntil) throws SQLException {
        String sqlWithoutPassword = "UPDATE users SET username = ?, email = ?, first_name = ?, last_name = ?, role = ?, banned_until = ? WHERE id = ?";
        String sqlWithPassword = "UPDATE users SET username = ?, email = ?, first_name = ?, last_name = ?, password = ?, role = ?, banned_until = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection()) {
            if (hashedPasswordOrNull == null) {
                try (PreparedStatement ps = connection.prepareStatement(sqlWithoutPassword)) {
                    ps.setString(1, username);
                    ps.setString(2, email);
                    ps.setString(3, firstName);
                    ps.setString(4, lastName);
                    ps.setString(5, role);
                    if (bannedUntil != null) {
                        ps.setTimestamp(6, java.sql.Timestamp.valueOf(bannedUntil));
                    } else {
                        ps.setNull(6, java.sql.Types.TIMESTAMP);
                    }
                    ps.setInt(7, userId);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = connection.prepareStatement(sqlWithPassword)) {
                    ps.setString(1, username);
                    ps.setString(2, email);
                    ps.setString(3, firstName);
                    ps.setString(4, lastName);
                    ps.setString(5, hashedPasswordOrNull);
                    ps.setString(6, role);
                    if (bannedUntil != null) {
                        ps.setTimestamp(7, java.sql.Timestamp.valueOf(bannedUntil));
                    } else {
                        ps.setNull(7, java.sql.Types.TIMESTAMP);
                    }
                    ps.setInt(8, userId);
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

