package org.example.repository;

import org.example.model.User;

import java.sql.SQLException;
import java.util.List;

public interface UserRepository {
    List<User> findAll() throws SQLException;

    boolean existsByEmail(String email, Integer excludeUserId) throws SQLException;

    void create(String email, String firstName, String lastName,
                String hashedPassword, String role, java.time.LocalDateTime bannedUntil, boolean emailVerified) throws SQLException;

    void update(int userId, String email, String firstName, String lastName,
                String hashedPasswordOrNull, String role, java.time.LocalDateTime bannedUntil) throws SQLException;

    void delete(int userId) throws SQLException;
}

