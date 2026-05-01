package org.example.controller;

import org.example.model.User;
import org.example.service.UserService;

import java.sql.SQLException;
import java.util.List;

public class UserController {
    private final UserService userService;

    public UserController() {
        this.userService = new UserService();
    }

    public List<User> getAllUsers() throws SQLException {
        return userService.getAllUsers();
    }

    public void createUser(String username, String email, String firstName, String lastName,
                           String plainPassword, String role, java.time.LocalDateTime bannedUntil) throws SQLException {
        userService.createUser(username, email, firstName, lastName, plainPassword, role, bannedUntil, true);
    }

    public void updateUser(int userId, String username, String email, String firstName, String lastName,
                           String plainPasswordOrNull, String role, java.time.LocalDateTime bannedUntil) throws SQLException {
        userService.updateUser(userId, username, email, firstName, lastName, plainPasswordOrNull, role, bannedUntil);
    }

    public void deleteUser(int id) throws SQLException {
        userService.deleteUser(id);
    }

    public void updateFaceIdStatus(int id, boolean enabled) throws SQLException {
        userService.updateFaceIdStatus(id, enabled);
    }
}
