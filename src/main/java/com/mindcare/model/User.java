package com.mindcare.model;

/**
 * User model representing all platform users (Client, Psychologue, Admin, SuperAdmin).
 */
public class User {

    public enum Role {
        CLIENT, PSYCHOLOGUE, ADMIN, SUPER_ADMIN
    }

    public enum Status {
        ACTIVE, BLOCKED, PENDING_VERIFICATION
    }

    private int id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private Role role;
    private Status status;
    private String avatarUrl;
    private String bio;
    private String phone;
    private String location;
    private String createdAt;

    public User() {}

    public User(int id, String firstName, String lastName, String email, Role role) {
        this.id = id;
        this.username = firstName != null ? firstName.toLowerCase() : null;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.status = Status.ACTIVE;
    }

    public User(int id, String username, String email, String firstName, String lastName, Role role, Status status) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.status = status;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() { return firstName + " " + lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return getFullName() + " (" + role + ")";
    }
}
