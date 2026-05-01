package org.example.service;

import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.repository.impl.UserRepositoryImpl;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class UserService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final UserRepository userRepository;

    public UserService() {
        this.userRepository = new UserRepositoryImpl();
    }

    public List<User> getAllUsers() throws SQLException {
        return userRepository.findAll();
    }

    public void createUser(String username, String email, String firstName, String lastName,
                           String plainPassword, String role, java.time.LocalDateTime bannedUntil,
                           boolean emailVerified) throws SQLException {
        String normalizedUsername = required(username, "Le nom d'utilisateur est requis.");
        String normalizedEmail = required(email, "L'email est requis.");
        if (!isValidEmail(normalizedEmail)) {
            throw new SQLException("Email invalide (ex: user@mail.com).");
        }
        String normalizedRole = normalizeRole(role);
        String normalizedFirstName = optional(firstName);
        String normalizedLastName = optional(lastName);

        if (plainPassword == null || plainPassword.trim().length() < 6) {
            throw new SQLException("Le mot de passe doit contenir au moins 6 caracteres.");
        }

        if (userRepository.existsByUsernameOrEmail(normalizedUsername, normalizedEmail, null)) {
            throw new SQLException("Username ou email deja utilise.");
        }

        String hashedPassword = BCrypt.hashpw(plainPassword.trim(), BCrypt.gensalt());
        userRepository.create(normalizedUsername, normalizedEmail, normalizedFirstName, normalizedLastName,
            hashedPassword, normalizedRole, bannedUntil, emailVerified);
    }

    public void updateUser(int userId, String username, String email, String firstName, String lastName,
                           String plainPasswordOrNull, String role, java.time.LocalDateTime bannedUntil) throws SQLException {
        String normalizedUsername = required(username, "Le nom d'utilisateur est requis.");
        String normalizedEmail = required(email, "L'email est requis.");
        if (!isValidEmail(normalizedEmail)) {
            throw new SQLException("Email invalide (ex: user@mail.com).");
        }
        String normalizedRole = normalizeRole(role);
        String normalizedFirstName = optional(firstName);
        String normalizedLastName = optional(lastName);

        if (userRepository.existsByUsernameOrEmail(normalizedUsername, normalizedEmail, userId)) {
            throw new SQLException("Username ou email deja utilise.");
        }

        String hashedPassword = null;
        if (plainPasswordOrNull != null && !plainPasswordOrNull.trim().isEmpty()) {
            if (plainPasswordOrNull.trim().length() < 6) {
                throw new SQLException("Le nouveau mot de passe doit contenir au moins 6 caracteres.");
            }
            hashedPassword = BCrypt.hashpw(plainPasswordOrNull.trim(), BCrypt.gensalt());
        }

        userRepository.update(userId, normalizedUsername, normalizedEmail, normalizedFirstName, normalizedLastName,
                hashedPassword, normalizedRole, bannedUntil);
    }

    public void deleteUser(int userId) throws SQLException {
        userRepository.delete(userId);
    }

    public void updateFaceIdStatus(int userId, boolean enabled) throws SQLException {
        if (userRepository instanceof UserRepositoryImpl) {
            ((UserRepositoryImpl) userRepository).updateFaceIdStatus(userId, enabled);
        }
    }

    private String required(String value, String message) throws SQLException {
        String normalized = optional(value);
        if (normalized.isEmpty()) {
            throw new SQLException(message);
        }
        return normalized;
    }

    private String optional(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeRole(String role) throws SQLException {
        String normalized = optional(role).toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ADMIN", "PSYCHOLOGUE", "CLIENT", "ETUDIANT" -> normalized;
            default -> throw new SQLException("Role invalide.");
        };
    }

    private boolean isValidEmail(String value) {
        return EMAIL_PATTERN.matcher(value).matches();
    }
}

