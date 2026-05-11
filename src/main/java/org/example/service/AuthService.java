package org.example.service;

import org.example.model.User;
import org.example.repository.AuthRepository;
import org.example.repository.impl.AuthRepositoryImpl;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.example.service.EmailService;

public class AuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AuthRepository authRepository;
    private final EmailService emailService;

    public AuthService() {
        this.authRepository = new AuthRepositoryImpl();
        this.emailService = new EmailService();
    }

    public void initializeUsers() throws SQLException {
        authRepository.createTableIfNotExists();

        if (!authRepository.existsByEmail("admin@mindcare.com")) {
            String hashedPassword = BCrypt.hashpw("admin123", BCrypt.gensalt());
            authRepository.save("admin@mindcare.com", "Admin", "System", hashedPassword, "ADMIN", true, null, null);
            System.out.println("✅ Admin user created: admin@mindcare.com / admin123");
        }
        // Always ensure demo accounts are verified (in case they were seeded before verification was enabled)
        try (var conn = org.example.config.DatabaseConnection.getConnection();
             var ps = conn.prepareStatement(
                "UPDATE users SET email_verified = 1 WHERE email IN ('admin@mindcare.com','etudiant@mindcare.com') AND email_verified = 0")) {
            ps.executeUpdate();
        }

        if (!authRepository.existsByEmail("etudiant@mindcare.com")) {
            String hashedPassword = BCrypt.hashpw("etud123", BCrypt.gensalt());
            authRepository.save("etudiant@mindcare.com", "Etudiant", "Demo", hashedPassword, "ETUDIANT", true, null, null);
            System.out.println("✅ Student user created: etudiant@mindcare.com / etud123");
        }
    }

    public User login(String email, String password) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            throw new SQLException("L'email est requis.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new SQLException("Le mot de passe est requis.");
        }

        if (!isValidEmail(email.trim())) {
            throw new SQLException("Format email invalide.");
        }

        User user = authRepository.login(email.trim(), password);

        if (user == null) {
            throw new SQLException("Email ou mot de passe incorrect.");
        }

        if (!user.isEmailVerified()) {
            throw new SQLException("Compte non activé. Vérifiez votre email pour confirmer votre compte.");
        }

        if (user.isBanned()) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            throw new SQLException("Votre compte est banni jusqu'au " + user.getBannedUntil().format(formatter));
        }

        return user;
    }

    public User loginByFaceId(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            throw new SQLException("Face ID non reconnu.");
        }

        User user = authRepository.findByEmail(email.trim());

        if (user == null) {
            throw new SQLException("Utilisateur Face ID introuvable.");
        }

        if (!user.isEmailVerified()) {
            throw new SQLException("Compte non activé. Vérifiez votre email pour confirmer votre compte.");
        }

        if (user.isBanned()) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            throw new SQLException("Votre compte est banni jusqu'au " + user.getBannedUntil().format(formatter));
        }

        return user;
    }

    public void register(String email, String firstName, String lastName, String password, String role) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            throw new SQLException("L'email est requis.");
        }
        if (!isValidEmail(email.trim())) {
            throw new SQLException("Format email invalide.");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new SQLException("Le mot de passe est requis.");
        }

        if (password.length() < 6) {
            throw new SQLException("Le mot de passe doit contenir au moins 6 caractères.");
        }

        String normalizedEmail = email.trim();
        String normalizedFirstName = optional(firstName);
        String normalizedLastName = optional(lastName);
        if (normalizedFirstName.isEmpty() || normalizedLastName.isEmpty()) {
            throw new SQLException("Le prénom et le nom sont requis.");
        }

        if (authRepository.existsByEmail(normalizedEmail)) {
            throw new SQLException("Cet email existe déjà.");
        }

        String activationToken = generateToken();
        LocalDateTime activationExpiry = LocalDateTime.now().plusHours(24);
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        authRepository.save(normalizedEmail, normalizedFirstName, normalizedLastName,
                hashedPassword, role != null ? role : "CLIENT", false, activationToken, activationExpiry);

        try {
            emailService.sendAccountVerificationEmail(normalizedEmail, normalizedFirstName, activationToken, activationExpiry);
        } catch (IOException e) {
            authRepository.deleteByEmail(normalizedEmail);
            throw new SQLException("Impossible d'envoyer l'email de confirmation: " + e.getMessage(), e);
        }
    }

    public void confirmAccount(String token) throws SQLException {
        String normalizedToken = requiredToken(token, "Le token de confirmation est requis.");
        User user = authRepository.findByActivationToken(normalizedToken);

        if (user == null) {
            throw new SQLException("Token de confirmation invalide.");
        }
        if (user.getActivationTokenExpiresAt() != null && user.getActivationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new SQLException("Token de confirmation expiré.");
        }

        authRepository.markEmailVerified(user.getId());
    }

    public void requestPasswordReset(String email) throws SQLException {
        String normalizedEmail = required(email, "L'email est requis.");
        if (!isValidEmail(normalizedEmail)) {
            throw new SQLException("Format email invalide.");
        }

        User user = authRepository.findByEmail(normalizedEmail);
        if (user == null) {
            throw new SQLException("Aucun compte trouvé pour cet email.");
        }
        if (!user.isEmailVerified()) {
            throw new SQLException("Compte non activé. Vérifiez votre email pour confirmer votre compte.");
        }

        String resetToken = generateToken();
        LocalDateTime resetExpiry = LocalDateTime.now().plusHours(1);
        authRepository.setPasswordResetToken(user.getId(), resetToken, resetExpiry);

        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName() != null ? user.getFirstName() : "Utilisateur", resetToken, resetExpiry);
        } catch (IOException e) {
            authRepository.clearPasswordResetToken(user.getId());
            throw new SQLException("Impossible d'envoyer l'email de réinitialisation: " + e.getMessage(), e);
        }
    }

    public void resetPassword(String token, String newPassword) throws SQLException {
        String normalizedToken = requiredToken(token, "Le token de réinitialisation est requis.");
        String normalizedPassword = optional(newPassword);

        if (normalizedPassword.length() < 6) {
            throw new SQLException("Le mot de passe doit contenir au moins 6 caractères.");
        }

        User user = authRepository.findByPasswordResetToken(normalizedToken);
        if (user == null) {
            throw new SQLException("Token de réinitialisation invalide.");
        }
        if (user.getResetPasswordTokenExpiresAt() != null && user.getResetPasswordTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new SQLException("Token de réinitialisation expiré.");
        }

        authRepository.updatePassword(user.getId(), BCrypt.hashpw(normalizedPassword, BCrypt.gensalt()));
        authRepository.clearPasswordResetToken(user.getId());
    }

    public List<User> findActiveUsersByEmails(Set<String> emails) throws SQLException {
        if (emails == null || emails.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = emails.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(java.util.Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        if (normalized.isEmpty()) {
            return List.of();
        }
        UserService userService = new UserService();
        return userService.getAllUsers().stream()
                .filter(User::isEmailVerified)
                .filter(user -> !user.isBanned())
                .filter(user -> user.getEmail() != null && normalized.contains(user.getEmail().toLowerCase(java.util.Locale.ROOT)))
                .toList();
    }

    private boolean isValidEmail(String value) {
        return EMAIL_PATTERN.matcher(value).matches();
    }

    private String required(String value, String message) throws SQLException {
        String normalized = optional(value);
        if (normalized.isEmpty()) {
            throw new SQLException(message);
        }
        return normalized;
    }

    private String requiredToken(String value, String message) throws SQLException {
        String normalized = optional(value);
        if (normalized.isEmpty()) {
            throw new SQLException(message);
        }
        return normalized;
    }

    private String optional(String value) {
        return value == null ? "" : value.trim();
    }

    private String generateToken() throws SQLException {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = String.format("%06d", RANDOM.nextInt(1_000_000));
            if (authRepository.findByActivationToken(code) == null
                    && authRepository.findByPasswordResetToken(code) == null) {
                return code;
            }
        }
        throw new SQLException("Impossible de générer un code de validation unique. Réessayez.");
    }


}
