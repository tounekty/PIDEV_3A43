package org.example.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class User {
    private final int id;
    private String email;
    private String firstName;
    private String lastName;
    private String password;
    private String role;
    private LocalDateTime bannedUntil;
    private boolean emailVerified;
    private String activationToken;
    private LocalDateTime activationTokenExpiresAt;
    private String resetPasswordToken;
    private LocalDateTime resetPasswordTokenExpiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean faceIdEnabled;

    // Constructor for login (with all fields)
    public User(int id, String email, String firstName, String lastName,
                String role, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.faceIdEnabled = false;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.emailVerified = true;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Simplified constructor for basic info
    public User(int id, String email, String role) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.firstName = "";
        this.lastName = "";
        this.emailVerified = true;
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public int getId() {
        return id;
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

    public LocalDateTime getBannedUntil() {
        return bannedUntil;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public String getActivationToken() {
        return activationToken;
    }

    public LocalDateTime getActivationTokenExpiresAt() {
        return activationTokenExpiresAt;
    }

    public String getResetPasswordToken() {
        return resetPasswordToken;
    }

    public LocalDateTime getResetPasswordTokenExpiresAt() {
        return resetPasswordTokenExpiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isFaceIdEnabled() {
        return faceIdEnabled;
    }

    // Setters
    public void setEmail(String email) {
        this.email = email;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRole(String role) {
        List<String> allowedRoles = Arrays.asList("ADMIN", "PSYCHOLOGUE", "CLIENT", "ETUDIANT");
        if (!allowedRoles.contains(role)) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        this.role = role;
    }

    public void setBannedUntil(LocalDateTime bannedUntil) {
        this.bannedUntil = bannedUntil;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public void setActivationToken(String activationToken) {
        this.activationToken = activationToken;
    }

    public void setActivationTokenExpiresAt(LocalDateTime activationTokenExpiresAt) {
        this.activationTokenExpiresAt = activationTokenExpiresAt;
    }

    public void setResetPasswordToken(String resetPasswordToken) {
        this.resetPasswordToken = resetPasswordToken;
    }

    public void setResetPasswordTokenExpiresAt(LocalDateTime resetPasswordTokenExpiresAt) {
        this.resetPasswordTokenExpiresAt = resetPasswordTokenExpiresAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setFaceIdEnabled(boolean faceIdEnabled) {
        this.faceIdEnabled = faceIdEnabled;
    }

    // Helper methods
    public boolean isAdmin() {
        return role != null && "ADMIN".equalsIgnoreCase(role.trim());
    }

    public boolean isBanned() {
        if (bannedUntil == null) {
            return false;
        }
        return bannedUntil.isAfter(LocalDateTime.now());
    }

    public boolean isActive() {
        return !isBanned();
    }

    public String getFullName() {
        if (firstName == null || firstName.isBlank()) {
            return email != null ? email.split("@")[0] : "User";
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