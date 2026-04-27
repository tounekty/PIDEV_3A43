package org.example.model;

import java.time.LocalDateTime;

public class User {
    private final int id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String role;
    private final String password;
    private final LocalDateTime bannedUntil;
    private final boolean verified;

    public User(int id, String firstName, String lastName, String email, String role, String password,
                LocalDateTime bannedUntil, boolean verified) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.password = password;
        this.bannedUntil = bannedUntil;
        this.verified = verified;
    }

    public int getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getPassword() {
        return password;
    }

    public LocalDateTime getBannedUntil() {
        return bannedUntil;
    }

    public boolean isVerified() {
        return verified;
    }

    public String getDisplayName() {
        String fullName = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        return fullName.isBlank() ? email : fullName;
    }

    public boolean isAdmin() {
        return role != null && role.toLowerCase().contains("admin");
    }
}
