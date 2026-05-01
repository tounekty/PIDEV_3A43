package org.example.repository;

import org.example.model.User;

import java.sql.SQLException;
import java.time.LocalDateTime;

public interface AuthRepository {
    void createTableIfNotExists() throws SQLException;
    
    User login(String email, String password) throws SQLException;
    User loginByUsername(String username) throws SQLException;
    
    void save(String username, String email, String firstName, String lastName,
              String hashedPassword, String role, boolean emailVerified,
              String activationToken, LocalDateTime activationTokenExpiresAt) throws SQLException;
    
    boolean existsByUsername(String username) throws SQLException;

    void deleteByUsername(String username) throws SQLException;

    User findByEmail(String email) throws SQLException;

    User findByActivationToken(String token) throws SQLException;

    User findByPasswordResetToken(String token) throws SQLException;

    void markEmailVerified(int userId) throws SQLException;

    void clearActivationToken(int userId) throws SQLException;

    void setPasswordResetToken(int userId, String token, LocalDateTime expiresAt) throws SQLException;

    void clearPasswordResetToken(int userId) throws SQLException;

    void updatePassword(int userId, String hashedPassword) throws SQLException;
}
