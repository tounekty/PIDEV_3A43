package org.example.auth;

public class AppUser {
    private final int id;
    private final String email;
    private final String role;

    public AppUser(int id, String email, String role) {
        this.id = id;
        this.email = email;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }
}
