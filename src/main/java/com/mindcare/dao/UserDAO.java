package com.mindcare.dao;

import com.mindcare.db.DBConnection;
import com.mindcare.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserDAO {

    private String tableName;
    private String idColumn;

    private String emailColumn;
    private String firstNameColumn;
    private String lastNameColumn;
    private String roleColumn;
    private String statusColumn;
    private String passwordColumn;
    private String bannedUntilColumn;
    private String verifiedColumn;

    public UserDAO() {
        initializeSchema();
    }

    public List<User> getAllUsers() {
        String sql = "SELECT " +
            idColumn + " AS id, " +

            emailColumn + " AS email, " +
            selectExpr(firstNameColumn, "first_name") + ", " +
            selectExpr(lastNameColumn, "last_name") + ", " +
            selectExpr(roleColumn, "role") + ", " +
            selectExpr(statusColumn, "status") + ", " +
            selectExpr(passwordColumn, "password") + ", " +
            selectExpr(bannedUntilColumn, "banned_until") + ", " +
            selectExpr(verifiedColumn, "is_verified") +
            " FROM " + tableName;

        List<User> users = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to load users from the database.", exception);
        }

        return users;
    }

    public User getUserById(int id) {
        String sql = "SELECT " +
            idColumn + " AS id, " +

            emailColumn + " AS email, " +
            selectExpr(firstNameColumn, "first_name") + ", " +
            selectExpr(lastNameColumn, "last_name") + ", " +
            selectExpr(roleColumn, "role") + ", " +
            selectExpr(statusColumn, "status") + ", " +
            selectExpr(passwordColumn, "password") + ", " +
            selectExpr(bannedUntilColumn, "banned_until") + ", " +
            selectExpr(verifiedColumn, "is_verified") +
            " FROM " + tableName + " WHERE " + idColumn + " = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUser(resultSet);
                }
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to load user details.", exception);
        }

        return null;
    }

    public User getUserByEmail(String email) {
        String sql = "SELECT " +
            idColumn + " AS id, " +

            emailColumn + " AS email, " +
            selectExpr(firstNameColumn, "first_name") + ", " +
            selectExpr(lastNameColumn, "last_name") + ", " +
            selectExpr(roleColumn, "role") + ", " +
            selectExpr(statusColumn, "status") + ", " +
            selectExpr(passwordColumn, "password") + ", " +
            selectExpr(bannedUntilColumn, "banned_until") + ", " +
            selectExpr(verifiedColumn, "is_verified") +
            " FROM " + tableName + " WHERE " + emailColumn + " = ? LIMIT 1";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUser(resultSet);
                }
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to load user by email.", exception);
        }

        return null;
    }

    public User authenticate(String email, String rawPassword) {
        User user = getUserByEmail(email);
        if (user == null) {
            return null;
        }
        if (!matchesPassword(rawPassword, user.getPassword())) {
            return null;
        }
        return user;
    }

    public boolean insertUser(User user) {
        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();


        add(columns, values, emailColumn, user.getEmail());
        add(columns, values, firstNameColumn, user.getFirstName());
        add(columns, values, lastNameColumn, user.getLastName());
        add(columns, values, roleColumn, toDatabaseRole(user.getRole()));
        add(columns, values, statusColumn, toDatabaseStatus(user.getStatus()));
        add(columns, values, verifiedColumn, user.getStatus() == User.Status.PENDING_VERIFICATION ? 0 : 1);

        if (passwordColumn != null) {
            String password = user.getPassword();
            if (password == null || password.isBlank()) {
                password = "mindcare123";
            }
            add(columns, values, passwordColumn, hashPassword(password));
        }

        if (columns.isEmpty()) {
            throw new DataAccessException("Unable to create user: no writable columns detected.", null);
        }

        String placeholders = String.join(", ", java.util.Collections.nCopies(values.size(), "?"));
        String sql = "INSERT INTO " + tableName + " (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bind(statement, values);
            int rows = statement.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        user.setId(keys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to create user.", exception);
        }

        return false;
    }

    public boolean updateUser(User user) {
        List<String> setParts = new ArrayList<>();
        List<Object> values = new ArrayList<>();


        addUpdate(setParts, values, emailColumn, user.getEmail());
        addUpdate(setParts, values, firstNameColumn, user.getFirstName());
        addUpdate(setParts, values, lastNameColumn, user.getLastName());
        addUpdate(setParts, values, roleColumn, toDatabaseRole(user.getRole()));
        addUpdate(setParts, values, statusColumn, toDatabaseStatus(user.getStatus()));

        if (verifiedColumn != null) {
            addUpdate(setParts, values, verifiedColumn, user.getStatus() == User.Status.PENDING_VERIFICATION ? 0 : 1);
        }

        if (passwordColumn != null && user.getPassword() != null && !user.getPassword().isBlank()) {
            addUpdate(setParts, values, passwordColumn, hashPassword(user.getPassword()));
        }

        if (setParts.isEmpty()) {
            return false;
        }

        String sql = "UPDATE " + tableName + " SET " + String.join(", ", setParts) + " WHERE " + idColumn + " = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            bind(statement, values);
            statement.setInt(values.size() + 1, user.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to update user.", exception);
        }
    }

    public boolean deleteUser(int id) {
        String sql = "DELETE FROM " + tableName + " WHERE " + idColumn + " = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to delete user.", exception);
        }
    }

    private void initializeSchema() {
        try (Connection connection = DBConnection.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            if (tableExists(metaData, "user")) {
                tableName = "user";
            } else if (tableExists(metaData, "users")) {
                tableName = "users";
            } else {
                throw new IllegalStateException("No user table found. Expected 'user' or 'users'.");
            }

            List<String> columns = loadColumns(metaData, tableName);
            idColumn = firstExisting(columns, "id", "user_id");

            emailColumn = firstExisting(columns, "email", "mail");
            firstNameColumn = firstExisting(columns, "first_name", "firstname", "prenom", "firstName");
            lastNameColumn = firstExisting(columns, "last_name", "lastname", "nom", "lastName");
            roleColumn = firstExisting(columns, "role", "roles", "user_role", "type");
            statusColumn = firstExisting(columns, "status", "account_status");
            passwordColumn = firstExisting(columns, "password", "mot_de_passe", "hashed_password");
            bannedUntilColumn = firstExisting(columns, "banned_until", "blocked_until");
            verifiedColumn = firstExisting(columns, "is_verified", "verified", "enabled");

            if (idColumn == null || emailColumn == null) {
                throw new IllegalStateException("User table is missing required id/email columns.");
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to inspect user schema.", exception);
        }
    }

    private boolean tableExists(DatabaseMetaData metaData, String candidate) throws SQLException {
        try (ResultSet resultSet = metaData.getTables(null, null, candidate, new String[] {"TABLE"})) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = metaData.getTables(null, null, candidate.toUpperCase(Locale.ROOT), new String[] {"TABLE"})) {
            return resultSet.next();
        }
    }

    private List<String> loadColumns(DatabaseMetaData metaData, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (ResultSet resultSet = metaData.getColumns(null, null, table, null)) {
            while (resultSet.next()) {
                columns.add(resultSet.getString("COLUMN_NAME"));
            }
        }
        return columns;
    }

    private String firstExisting(List<String> columns, String... candidates) {
        for (String candidate : candidates) {
            for (String column : columns) {
                if (column.equalsIgnoreCase(candidate)) {
                    return column;
                }
            }
        }
        return null;
    }

    private String selectExpr(String column, String alias) {
        return column != null ? (column + " AS " + alias) : ("NULL AS " + alias);
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getInt("id"));

        user.setEmail(resultSet.getString("email"));
        user.setFirstName(defaultIfBlank(resultSet.getString("first_name"), resultSet.getString("email")));
        user.setLastName(defaultIfBlank(resultSet.getString("last_name"), ""));
        user.setPassword(resultSet.getString("password"));
        user.setRole(parseRole(resultSet.getString("role")));
        user.setStatus(resolveStatus(resultSet));
        user.setCreatedAt("");
        return user;
    }

    private User.Role parseRole(String value) {
        if (value == null || value.isBlank()) {
            return User.Role.CLIENT;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "client", "student", "etudiant", "student_role", "role_student" -> User.Role.CLIENT;
            case "Psychologue", "psychologue", "psychologist", "psy", "therapist" -> User.Role.PSYCHOLOGUE;
            case "admin", "administrator" -> User.Role.ADMIN;
            case "super_admin", "super-admin", "superadmin" -> User.Role.SUPER_ADMIN;
            default -> {
                try {
                    yield User.Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                    yield User.Role.CLIENT;
                }
            }
        };
    }

    private User.Status resolveStatus(ResultSet resultSet) throws SQLException {
        if (bannedUntilColumn != null) {
            Timestamp bannedUntil = resultSet.getTimestamp("banned_until");
            if (bannedUntil != null && bannedUntil.toLocalDateTime().isAfter(LocalDateTime.now())) {
                return User.Status.BLOCKED;
            }
        }

        if (verifiedColumn != null) {
            int verified = resultSet.getInt("is_verified");
            if (!resultSet.wasNull() && verified == 0) {
                return User.Status.PENDING_VERIFICATION;
            }
        }

        if (statusColumn != null) {
            return parseStatus(resultSet.getString("status"));
        }

        return User.Status.ACTIVE;
    }

    private User.Status parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return User.Status.ACTIVE;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "blocked", "banned", "disabled", "inactive" -> User.Status.BLOCKED;
            case "pending", "pending_verification", "unverified" -> User.Status.PENDING_VERIFICATION;
            default -> User.Status.ACTIVE;
        };
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (storedPassword.startsWith("$2y$") || storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")) {
            String normalizedHash = storedPassword.startsWith("$2y$")
                ? "$2a$" + storedPassword.substring(4)
                : storedPassword;
            return BCrypt.checkpw(rawPassword, normalizedHash);
        }

        return storedPassword.equals(rawPassword);
    }

    private String hashPassword(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(13));
    }

    private String toDatabaseRole(User.Role role) {
        if (role == null) {
            return "etudiant";
        }
        return switch (role) {
            case CLIENT -> "etudiant";
            case PSYCHOLOGUE -> "psychologue";
            case ADMIN -> "admin";
            case SUPER_ADMIN -> "super_admin";
        };
    }

    private String toDatabaseStatus(User.Status status) {
        if (status == null) {
            return "active";
        }
        return switch (status) {
            case ACTIVE -> "active";
            case BLOCKED -> "blocked";
            case PENDING_VERIFICATION -> "pending";
        };
    }

    private void add(List<String> columns, List<Object> values, String column, Object value) {
        if (column != null && value != null) {
            columns.add(column);
            values.add(value);
        }
    }

    private void addUpdate(List<String> setParts, List<Object> values, String column, Object value) {
        if (column != null && value != null) {
            setParts.add(column + " = ?");
            values.add(value);
        }
    }

    private void bind(PreparedStatement statement, List<Object> values) throws SQLException {
        for (int index = 0; index < values.size(); index++) {
            statement.setObject(index + 1, values.get(index));
        }
    }

    private String defaultIfBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback == null ? "" : fallback;
    }
}
