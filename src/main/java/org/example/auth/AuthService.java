package org.example.auth;

import org.example.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class AuthService {
    public void initializeUsers() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS app_user (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    username VARCHAR(100) NOT NULL UNIQUE,
                    password VARCHAR(100) NOT NULL,
                    role VARCHAR(30) NOT NULL
                )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }

        seedUser("admin@mindcare.com", "admin123", "admin");
        seedUser("etudiant@mindcare.com", "etud123", "etudiant");
    }

    public AppUser login(String email, String password) throws SQLException {
        String sql = "SELECT id, username AS email, role FROM app_user WHERE username = ? AND password = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, email);
            preparedStatement.setString(2, password);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new AppUser(
                            resultSet.getInt("id"),
                            resultSet.getString("email"),
                            resultSet.getString("role")
                    );
                }
            }
        }

        return null;
    }

    private void seedUser(String username, String password, String role) throws SQLException {
        String existsSql = "SELECT id FROM app_user WHERE username = ?";
        String insertSql = "INSERT INTO app_user(username, password, role) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement existsStatement = connection.prepareStatement(existsSql)) {
            existsStatement.setString(1, username);

            try (ResultSet resultSet = existsStatement.executeQuery()) {
                if (resultSet.next()) {
                    return;
                }
            }

            try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                insertStatement.setString(1, username);
                insertStatement.setString(2, password);
                insertStatement.setString(3, role);
                insertStatement.executeUpdate();
            }
        }
    }
}
