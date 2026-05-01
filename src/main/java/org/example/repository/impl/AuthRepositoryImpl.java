package org.example.repository.impl;

import org.example.config.DatabaseConnection;
import org.example.model.User;
import org.example.repository.AuthRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

public class AuthRepositoryImpl implements AuthRepository {

    @Override
    public void createTableIfNotExists() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS users (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    username VARCHAR(100) NOT NULL UNIQUE,
                    email VARCHAR(180) UNIQUE,
                    first_name VARCHAR(100),
                    last_name VARCHAR(100),
                    password VARCHAR(255) NOT NULL,
                    role VARCHAR(50) NOT NULL DEFAULT 'CLIENT',
                    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                    activation_token VARCHAR(120) NULL,
                    activation_token_expires_at DATETIME NULL,
                    reset_password_token VARCHAR(120) NULL,
                    reset_password_token_expires_at DATETIME NULL,
                    banned_until DATETIME NULL,
                    face_id_enabled BOOLEAN DEFAULT FALSE,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            try {
                statement.execute("ALTER TABLE users ADD COLUMN face_id_enabled BOOLEAN DEFAULT FALSE");
            } catch (SQLException e) {
                // Column already exists, ignore
            }
            try {
                statement.execute("ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE");
            } catch (SQLException e) {
                // Column already exists, ignore
            }
            try {
                statement.execute("ALTER TABLE users ADD COLUMN activation_token VARCHAR(120) NULL");
            } catch (SQLException e) {
                // Column already exists, ignore
            }
            try {
                statement.execute("ALTER TABLE users ADD COLUMN activation_token_expires_at DATETIME NULL");
            } catch (SQLException e) {
                // Column already exists, ignore
            }
            try {
                statement.execute("ALTER TABLE users ADD COLUMN reset_password_token VARCHAR(120) NULL");
            } catch (SQLException e) {
                // Column already exists, ignore
            }
            try {
                statement.execute("ALTER TABLE users ADD COLUMN reset_password_token_expires_at DATETIME NULL");
            } catch (SQLException e) {
                // Column already exists, ignore
            }
            try {
                statement.execute("ALTER TABLE users DROP COLUMN status");
            } catch (SQLException e) {
                // Column might not exist, ignore
            }
        }
    }

    @Override
    public User login(String email, String password) throws SQLException {
        String sql = "SELECT id, username, email, first_name, last_name, password, role, email_verified, face_id_enabled, created_at, updated_at, banned_until, activation_token, activation_token_expires_at, reset_password_token, reset_password_token_expires_at FROM users WHERE email = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, email);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String hashedPassword = resultSet.getString("password");
                    if (BCrypt.checkpw(password, hashedPassword)) {
                        User user = new User(
                                resultSet.getInt("id"),
                                resultSet.getString("username"),
                                resultSet.getString("email"),
                                resultSet.getString("first_name"),
                                resultSet.getString("last_name"),
                                resultSet.getString("role"),
                                resultSet.getTimestamp("created_at") != null ? resultSet.getTimestamp("created_at").toLocalDateTime() : null,
                                resultSet.getTimestamp("updated_at") != null ? resultSet.getTimestamp("updated_at").toLocalDateTime() : null
                        );
                                user.setEmailVerified(resultSet.getBoolean("email_verified"));
                                user.setActivationToken(resultSet.getString("activation_token"));
                                user.setActivationTokenExpiresAt(resultSet.getTimestamp("activation_token_expires_at") != null ? resultSet.getTimestamp("activation_token_expires_at").toLocalDateTime() : null);
                                user.setResetPasswordToken(resultSet.getString("reset_password_token"));
                                user.setResetPasswordTokenExpiresAt(resultSet.getTimestamp("reset_password_token_expires_at") != null ? resultSet.getTimestamp("reset_password_token_expires_at").toLocalDateTime() : null);

                        if (resultSet.getTimestamp("banned_until") != null) {
                            user.setBannedUntil(resultSet.getTimestamp("banned_until").toLocalDateTime());
                        }

                        return user;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public User loginByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, email, first_name, last_name, password, role, email_verified, face_id_enabled, created_at, updated_at, banned_until, activation_token, activation_token_expires_at, reset_password_token, reset_password_token_expires_at FROM users WHERE username = ? OR email = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, username);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    User user = new User(
                            resultSet.getInt("id"),
                            resultSet.getString("username"),
                            resultSet.getString("email"),
                            resultSet.getString("first_name"),
                            resultSet.getString("last_name"),
                            resultSet.getString("role"),
                            resultSet.getTimestamp("created_at") != null ? resultSet.getTimestamp("created_at").toLocalDateTime() : null,
                            resultSet.getTimestamp("updated_at") != null ? resultSet.getTimestamp("updated_at").toLocalDateTime() : null
                    );
                    user.setEmailVerified(resultSet.getBoolean("email_verified"));
                    user.setActivationToken(resultSet.getString("activation_token"));
                    user.setActivationTokenExpiresAt(resultSet.getTimestamp("activation_token_expires_at") != null ? resultSet.getTimestamp("activation_token_expires_at").toLocalDateTime() : null);
                    user.setResetPasswordToken(resultSet.getString("reset_password_token"));
                    user.setResetPasswordTokenExpiresAt(resultSet.getTimestamp("reset_password_token_expires_at") != null ? resultSet.getTimestamp("reset_password_token_expires_at").toLocalDateTime() : null);
                    user.setFaceIdEnabled(resultSet.getBoolean("face_id_enabled"));

                    if (resultSet.getTimestamp("banned_until") != null) {
                        user.setBannedUntil(resultSet.getTimestamp("banned_until").toLocalDateTime());
                    }

                    return user;
                }
            }
        }

        return null;
    }

    @Override
    public void save(String username, String email, String firstName, String lastName,
                     String hashedPassword, String role, boolean emailVerified,
                     String activationToken, LocalDateTime activationTokenExpiresAt) throws SQLException {
        String sql = "INSERT INTO users (username, password, role, email, first_name, last_name, email_verified, activation_token, activation_token_expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, hashedPassword);
            preparedStatement.setString(3, role != null ? role : "CLIENT");
            preparedStatement.setString(4, email);
            preparedStatement.setString(5, firstName == null ? "" : firstName);
            preparedStatement.setString(6, lastName == null ? "" : lastName);
            preparedStatement.setBoolean(7, emailVerified);
            preparedStatement.setString(8, activationToken);
            if (activationTokenExpiresAt != null) {
                preparedStatement.setTimestamp(9, java.sql.Timestamp.valueOf(activationTokenExpiresAt));
            } else {
                preparedStatement.setNull(9, java.sql.Types.TIMESTAMP);
            }
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public void deleteByUsername(String username) throws SQLException {
        String sql = "DELETE FROM users WHERE username = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }

    @Override
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT id, username, email, first_name, last_name, password, role, email_verified, face_id_enabled, created_at, updated_at, banned_until, activation_token, activation_token_expires_at, reset_password_token, reset_password_token_expires_at FROM users WHERE email = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return buildUser(rs);
            }
        }
    }

    @Override
    public User findByActivationToken(String token) throws SQLException {
        String sql = "SELECT id, username, email, first_name, last_name, password, role, email_verified, face_id_enabled, created_at, updated_at, banned_until, activation_token, activation_token_expires_at, reset_password_token, reset_password_token_expires_at FROM users WHERE activation_token = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                return buildUser(rs);
            }
        }
    }

    @Override
    public User findByPasswordResetToken(String token) throws SQLException {
        String sql = "SELECT id, username, email, first_name, last_name, password, role, email_verified, face_id_enabled, created_at, updated_at, banned_until, activation_token, activation_token_expires_at, reset_password_token, reset_password_token_expires_at FROM users WHERE reset_password_token = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                return buildUser(rs);
            }
        }
    }

    @Override
    public void markEmailVerified(int userId) throws SQLException {
        String sql = "UPDATE users SET email_verified = TRUE, activation_token = NULL, activation_token_expires_at = NULL WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public void clearActivationToken(int userId) throws SQLException {
        String sql = "UPDATE users SET activation_token = NULL, activation_token_expires_at = NULL WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public void setPasswordResetToken(int userId, String token, LocalDateTime expiresAt) throws SQLException {
        String sql = "UPDATE users SET reset_password_token = ?, reset_password_token_expires_at = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, token);
            if (expiresAt != null) {
                ps.setTimestamp(2, java.sql.Timestamp.valueOf(expiresAt));
            } else {
                ps.setNull(2, java.sql.Types.TIMESTAMP);
            }
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public void clearPasswordResetToken(int userId) throws SQLException {
        String sql = "UPDATE users SET reset_password_token = NULL, reset_password_token_expires_at = NULL WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public void updatePassword(int userId, String hashedPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE username = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private User buildUser(ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) {
            return null;
        }

        User user = new User(
                resultSet.getInt("id"),
                resultSet.getString("username"),
                resultSet.getString("email"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getString("role"),
                resultSet.getTimestamp("created_at") == null ? null : resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at") == null ? null : resultSet.getTimestamp("updated_at").toLocalDateTime()
        );
        user.setEmailVerified(resultSet.getBoolean("email_verified"));
        user.setFaceIdEnabled(resultSet.getBoolean("face_id_enabled"));
        user.setActivationToken(resultSet.getString("activation_token"));
        user.setActivationTokenExpiresAt(resultSet.getTimestamp("activation_token_expires_at") == null ? null : resultSet.getTimestamp("activation_token_expires_at").toLocalDateTime());
        user.setResetPasswordToken(resultSet.getString("reset_password_token"));
        user.setResetPasswordTokenExpiresAt(resultSet.getTimestamp("reset_password_token_expires_at") == null ? null : resultSet.getTimestamp("reset_password_token_expires_at").toLocalDateTime());
        if (resultSet.getTimestamp("banned_until") != null) {
            user.setBannedUntil(resultSet.getTimestamp("banned_until").toLocalDateTime());
        }
        return user;
    }
}