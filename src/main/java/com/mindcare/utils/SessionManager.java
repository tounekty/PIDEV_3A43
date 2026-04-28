package com.mindcare.utils;

import com.mindcare.model.User;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * SessionManager – singleton holding the currently authenticated user.
 * Swap the mock user here when integrating with a real backend.
 */
public class SessionManager {

    private static final Logger logger = Logger.getLogger(SessionManager.class.getName());
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(User user) {
        this.currentUser = user;
    }

    /**
     * Logout: clears session
     */
    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User.Role getRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }

    public boolean hasRole(User.Role role) {
        return currentUser != null && currentUser.getRole() == role;
    }

    public boolean isAdmin() {
        return hasRole(User.Role.ADMIN) || hasRole(User.Role.SUPER_ADMIN);
    }
}
