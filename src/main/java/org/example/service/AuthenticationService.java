package org.example.service;

import org.example.model.User;
import org.example.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class AuthenticationService {
    private final UserRepository userRepository = new UserRepository();

    public User registerStudent(String firstName, String lastName, String email, String password) throws ServiceException {
        String cleanFirstName = firstName == null ? "" : firstName.trim();
        String cleanLastName = lastName == null ? "" : lastName.trim();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String cleanPassword = password == null ? "" : password;

        if (cleanFirstName.isBlank() || cleanLastName.isBlank() || normalizedEmail.isBlank() || cleanPassword.isBlank()) {
            throw new ServiceException("Tous les champs sont obligatoires.");
        }
        if (!normalizedEmail.matches("[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ServiceException("Email invalide.");
        }
        if (cleanPassword.length() < 6) {
            throw new ServiceException("Le mot de passe doit contenir au moins 6 caracteres.");
        }

        try {
            if (userRepository.findByEmail(normalizedEmail) != null) {
                throw new ServiceException("Cet email existe deja.");
            }
            String hashedPassword = BCrypt.hashpw(cleanPassword, BCrypt.gensalt(13));
            userRepository.createStudent(cleanFirstName, cleanLastName, normalizedEmail, hashedPassword);
            return login(normalizedEmail, cleanPassword);
        } catch (ServiceException e) {
            throw e;
        } catch (SQLException e) {
            throw new ServiceException("Erreur lors de l'inscription: " + e.getMessage(), e);
        }
    }

    public User login(String email, String password) throws ServiceException {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (normalizedEmail.isBlank() || password == null || password.isBlank()) {
            throw new ServiceException("Renseigne l'email et le mot de passe.");
        }

        try {
            User user = userRepository.findByEmail(normalizedEmail);
            if (user == null || !passwordMatches(password, user.getPassword())) {
                throw new ServiceException("Email ou mot de passe invalide.");
            }
            if (!user.isVerified()) {
                throw new ServiceException("Compte non verifie.");
            }
            if (user.getBannedUntil() != null && user.getBannedUntil().isAfter(LocalDateTime.now())) {
                throw new ServiceException("Compte suspendu jusqu'au " + user.getBannedUntil() + ".");
            }
            return user;
        } catch (SQLException e) {
            throw new ServiceException("Erreur de connexion a la base de donnees: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ServiceException("Format du mot de passe stocke invalide.", e);
        }
    }

    private boolean passwordMatches(String plainPassword, String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isBlank()) {
            return false;
        }
        String bcryptHash = hashedPassword.startsWith("$2y$")
                ? "$2a$" + hashedPassword.substring(4)
                : hashedPassword;
        return BCrypt.checkpw(plainPassword, bcryptHash);
    }
}
