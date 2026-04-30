package org.example.auth;

import org.example.config.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

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
            // Agrandir la colonne pour les hash BCrypt (60 chars)
            try {
                statement.execute("ALTER TABLE app_user MODIFY COLUMN password VARCHAR(100) NOT NULL");
            } catch (SQLException ignored) {}
            // Ajouter colonne phone si elle n'existe pas
            try {
                statement.execute("ALTER TABLE app_user ADD COLUMN phone VARCHAR(20) NULL");
            } catch (SQLException ignored) {} // déjà existante
        }

        seedUser("admin", "admin123", "admin");
        seedUser("etudiant", "etud123", "etudiant");
    }

    public AppUser login(String username, String password) throws SQLException {
        String sql = "SELECT id, username, password, role FROM app_user WHERE username = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String stored = resultSet.getString("password");
                    boolean matches;
                    if (stored != null && stored.startsWith("$2")) {
                        // Hash BCrypt
                        matches = BCrypt.checkpw(password, stored);
                    } else {
                        // Ancien mot de passe en clair → migration automatique
                        matches = password.equals(stored);
                        if (matches) {
                            migratePasswordToBcrypt(resultSet.getInt("id"), password);
                        }
                    }
                    if (matches) {
                        return new AppUser(
                                resultSet.getInt("id"),
                                resultSet.getString("username"),
                                resultSet.getString("role")
                        );
                    }
                }
            }
        }
        return null;
    }

    /**
     * Crée un nouveau compte étudiant.
     * Lève une SQLException si le nom est déjà pris ou si les données sont invalides.
     */
    public AppUser register(String username, String password) throws SQLException {
        if (username == null || username.isBlank())
            throw new SQLException("Le nom d'utilisateur est obligatoire.");
        if (password == null || password.length() < 4)
            throw new SQLException("Le mot de passe doit contenir au moins 4 caractères.");

        String checkSql = "SELECT id FROM app_user WHERE username = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement check = connection.prepareStatement(checkSql)) {
            check.setString(1, username.trim());
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) throw new SQLException("Ce nom d'utilisateur est déjà pris.");
            }
        }

        String insertSql = "INSERT INTO app_user(username, password, role) VALUES (?, ?, 'etudiant')";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, username.trim());
            stmt.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return new AppUser(keys.getInt(1), username.trim(), "etudiant");
                }
            }
        }
        throw new SQLException("Erreur lors de la création du compte.");
    }

    /** Migration transparente : hash BCrypt au premier login. */
    private void migratePasswordToBcrypt(int userId, String plainPassword) {
        String sql = "UPDATE app_user SET password = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, BCrypt.hashpw(plainPassword, BCrypt.gensalt()));
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AuthService] Migration BCrypt échouée: " + e.getMessage());
        }
    }

    private void seedUser(String username, String plainPassword, String role) throws SQLException {
        String existsSql = "SELECT id, password FROM app_user WHERE username = ?";
        String insertSql = "INSERT INTO app_user(username, password, role) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement existsStatement = connection.prepareStatement(existsSql)) {
            existsStatement.setString(1, username);

            try (ResultSet resultSet = existsStatement.executeQuery()) {
                if (resultSet.next()) {
                    // Migrer si pas encore hashé
                    String stored = resultSet.getString("password");
                    if (stored != null && !stored.startsWith("$2")) {
                        migratePasswordToBcrypt(resultSet.getInt("id"), stored);
                    }
                    return;
                }
            }

            try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                insertStatement.setString(1, username);
                insertStatement.setString(2, BCrypt.hashpw(plainPassword, BCrypt.gensalt()));
                insertStatement.setString(3, role);
                insertStatement.executeUpdate();
            }
        }
    }
}
