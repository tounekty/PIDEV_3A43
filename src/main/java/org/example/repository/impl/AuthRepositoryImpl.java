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
                    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                    banned_until DATETIME NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    @Override
    public User login(String username, String password) throws SQLException {
        String sql = "SELECT id, username, email, first_name, last_name, password, role, status, created_at, updated_at, banned_until FROM users WHERE username = ? OR email = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, username);

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
                                resultSet.getString("status"),
                                resultSet.getTimestamp("created_at") != null ? resultSet.getTimestamp("created_at").toLocalDateTime() : null,
                                resultSet.getTimestamp("updated_at") != null ? resultSet.getTimestamp("updated_at").toLocalDateTime() : null
                        );

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
    public void save(String username, String hashedPassword, String role) throws SQLException {
        String sql = "INSERT INTO users (username, password, role, email, first_name, last_name, status) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, hashedPassword);
            preparedStatement.setString(3, role != null ? role : "CLIENT");
            preparedStatement.setString(4, username + "@mindcare.com");
            preparedStatement.setString(5, username);
            preparedStatement.setString(6, username);
            preparedStatement.setString(7, "ACTIVE");
            preparedStatement.executeUpdate();
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
}