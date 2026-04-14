package org.example.repository;

import org.example.model.User;

import java.sql.SQLException;

public interface AuthRepository {
    void createTableIfNotExists() throws SQLException;
    
    User login(String username, String password) throws SQLException;
    
    void save(String username, String hashedPassword, String role) throws SQLException;
    
    boolean existsByUsername(String username) throws SQLException;
}
