package org.example.service;

import org.example.model.User;
import org.example.repository.AuthRepository;
import org.example.repository.impl.AuthRepositoryImpl;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;

public class AuthService {
    private final AuthRepository authRepository;

    public AuthService() {
        this.authRepository = new AuthRepositoryImpl();
    }

    public void initializeUsers() throws SQLException {
        authRepository.createTableIfNotExists();

        if (!authRepository.existsByUsername("admin")) {
            String hashedPassword = BCrypt.hashpw("admin123", BCrypt.gensalt());
            authRepository.save("admin", hashedPassword, "ADMIN");
            System.out.println("✅ Admin user created: admin / admin123");
        }

        if (!authRepository.existsByUsername("etudiant")) {
            String hashedPassword = BCrypt.hashpw("etud123", BCrypt.gensalt());
            authRepository.save("etudiant", hashedPassword, "ETUDIANT");
            System.out.println("✅ Student user created: etudiant / etud123");
        }
    }

    public User login(String username, String password) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            throw new SQLException("Le nom d'utilisateur est requis.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new SQLException("Le mot de passe est requis.");
        }

        User user = authRepository.login(username.trim(), password);

        if (user == null) {
            throw new SQLException("Nom d'utilisateur ou mot de passe incorrect.");
        }

        if (!user.isActive()) {
            if (user.isBanned()) {
                throw new SQLException("Ce compte est banni.");
            } else {
                throw new SQLException("Ce compte est désactivé.");
            }
        }

        return user;
    }

    public void register(String username, String password, String role) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            throw new SQLException("Le nom d'utilisateur est requis.");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new SQLException("Le mot de passe est requis.");
        }

        if (password.length() < 6) {
            throw new SQLException("Le mot de passe doit contenir au moins 6 caractères.");
        }

        if (authRepository.existsByUsername(username.trim())) {
            throw new SQLException("Ce nom d'utilisateur existe déjà.");
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        authRepository.save(username.trim(), hashedPassword, role != null ? role : "CLIENT");
    }
}