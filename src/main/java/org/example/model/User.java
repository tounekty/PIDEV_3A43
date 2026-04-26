package org.example.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class User {
    private final int id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String status;
    private LocalDateTime bannedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor for login (with all fields)
    public User(int id, String username, String email, String firstName, String lastName,
                String role, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Simplified constructor for basic info
    public User(int id, String username, String email, String role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.status = "ACTIVE";
        this.firstName = "";
        this.lastName = "";
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getBannedUntil() {
        return bannedUntil;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setRole(String role) {
        List<String> allowedRoles = Arrays.asList("ADMIN", "PSYCHOLOGUE", "CLIENT", "ETUDIANT");
        if (!allowedRoles.contains(role)) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        this.role = role;
    }

    public void setStatus(String status) {
        List<String> allowedStatuses = Arrays.asList("ACTIVE", "INACTIVE", "BANNED");
        if (!allowedStatuses.contains(status)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        this.status = status;
    }

    public void setBannedUntil(LocalDateTime bannedUntil) {
        this.bannedUntil = bannedUntil;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isBanned() {
        if (bannedUntil == null) {
            return false;
        }
        return bannedUntil.isAfter(LocalDateTime.now());
    }

    public boolean isActive() {
        return "ACTIVE".equals(status) && !isBanned();
    }

    public String getFullName() {
        if (firstName == null || firstName.isBlank()) {
            return username;
        }
        return firstName + " " + lastName;
    }

    public List<String> getRoles() {
        return switch (role) {
            case "ADMIN" -> Arrays.asList("ROLE_ADMIN", "ROLE_USER");
            case "PSYCHOLOGUE" -> Arrays.asList("ROLE_PSYCHOLOGUE", "ROLE_USER");
            case "ETUDIANT" -> Arrays.asList("ROLE_ETUDIANT", "ROLE_USER");
            case "CLIENT" -> Arrays.asList("ROLE_CLIENT", "ROLE_USER");
            default -> Arrays.asList("ROLE_USER");
        };
    }

    @Override
    public String toString() {
        return getFullName() + " (" + email + ")";
    }
}