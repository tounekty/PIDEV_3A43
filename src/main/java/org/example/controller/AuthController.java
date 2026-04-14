package org.example.controller;

import org.example.model.User;
import org.example.service.AuthService;

import java.sql.SQLException;

public class AuthController {
    private final AuthService authService;

    public AuthController() {
        this.authService = new AuthService();
    }

    public void initializeUsers() throws SQLException {
        authService.initializeUsers();
    }

    public User login(String username, String password) throws SQLException {
        return authService.login(username, password);
    }

    public void register(String username, String password, String role) throws SQLException {
        authService.register(username, password, role);
    }
}
