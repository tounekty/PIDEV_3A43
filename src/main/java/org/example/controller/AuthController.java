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

    public User login(String email, String password) throws SQLException {
        return authService.login(email, password);
    }

    public User loginByFaceId(String email) throws SQLException {
        return authService.loginByFaceId(email);
    }

    public void register(String email, String firstName, String lastName, String password, String role) throws SQLException {
        authService.register(email, firstName, lastName, password, role);
    }

    public void confirmAccount(String token) throws SQLException {
        authService.confirmAccount(token);
    }

    public void requestPasswordReset(String email) throws SQLException {
        authService.requestPasswordReset(email);
    }

    public void resetPassword(String token, String newPassword) throws SQLException {
        authService.resetPassword(token, newPassword);
    }
}