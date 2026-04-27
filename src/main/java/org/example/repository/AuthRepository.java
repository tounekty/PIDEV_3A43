package org.example.repository;

import org.example.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public interface AuthRepository {
    void createTableIfNotExists() throws SQLException;
    
    User login(String username, String password) throws SQLException;
    
    void save(String username, String hashedPassword, String role) throws SQLException;

    void save(String username, String hashedPassword, String role, String email) throws SQLException;
    
    boolean existsByUsername(String username) throws SQLException;

    boolean existsByEmail(String email) throws SQLException;

    List<User> findActiveUsersByUsernames(Set<String> usernames) throws SQLException;
}
