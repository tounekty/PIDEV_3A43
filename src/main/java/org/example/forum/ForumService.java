package org.example.forum;

import org.example.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ForumService {
    public void createTableIfNotExists() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS sujet_forum (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    titre VARCHAR(255) NOT NULL,
                    description LONGTEXT NULL,
                    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    image_url VARCHAR(500) NULL,
                    is_pinned TINYINT(1) NOT NULL DEFAULT 0,
                    is_anonymous TINYINT(1) NOT NULL DEFAULT 0,
                    status VARCHAR(50) NULL,
                    category VARCHAR(100) NULL,
                    attachment_path VARCHAR(500) NULL,
                    attachment_mime_type VARCHAR(120) NULL,
                    attachment_size BIGINT NULL,
                    id_user INT NOT NULL
                )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            ensureColumnExists(statement, "titre", "ALTER TABLE sujet_forum ADD COLUMN titre VARCHAR(255) NOT NULL AFTER id");
            ensureColumnExists(statement, "description", "ALTER TABLE sujet_forum ADD COLUMN description LONGTEXT NULL AFTER titre");
            ensureColumnExists(statement, "date_creation", "ALTER TABLE sujet_forum ADD COLUMN date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER description");
            ensureColumnExists(statement, "image_url", "ALTER TABLE sujet_forum ADD COLUMN image_url VARCHAR(500) NULL AFTER date_creation");
            ensureColumnExists(statement, "is_pinned", "ALTER TABLE sujet_forum ADD COLUMN is_pinned TINYINT(1) NOT NULL DEFAULT 0 AFTER image_url");
            ensureColumnExists(statement, "is_anonymous", "ALTER TABLE sujet_forum ADD COLUMN is_anonymous TINYINT(1) NOT NULL DEFAULT 0 AFTER is_pinned");
            ensureColumnExists(statement, "status", "ALTER TABLE sujet_forum ADD COLUMN status VARCHAR(50) NULL AFTER is_anonymous");
            ensureColumnExists(statement, "category", "ALTER TABLE sujet_forum ADD COLUMN category VARCHAR(100) NULL AFTER status");
            ensureColumnExists(statement, "attachment_path", "ALTER TABLE sujet_forum ADD COLUMN attachment_path VARCHAR(500) NULL AFTER category");
            ensureColumnExists(statement, "attachment_mime_type", "ALTER TABLE sujet_forum ADD COLUMN attachment_mime_type VARCHAR(120) NULL AFTER attachment_path");
            ensureColumnExists(statement, "attachment_size", "ALTER TABLE sujet_forum ADD COLUMN attachment_size BIGINT NULL AFTER attachment_mime_type");
            ensureColumnExists(statement, "id_user", "ALTER TABLE sujet_forum ADD COLUMN id_user INT NOT NULL AFTER attachment_size");
        }
    }

    public void addSubject(ForumSubject subject) throws SQLException {
        String sql = """
                INSERT INTO sujet_forum(titre, description, date_creation, image_url, is_pinned, is_anonymous, status, category,
                                       attachment_path, attachment_mime_type, attachment_size, id_user)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, subject.getTitre());
            preparedStatement.setString(2, emptyToNull(subject.getDescription()));
            preparedStatement.setTimestamp(3, Timestamp.valueOf(subject.getDateCreation()));
            preparedStatement.setString(4, emptyToNull(subject.getImageUrl()));
            preparedStatement.setInt(5, subject.isPinned() ? 1 : 0);
            preparedStatement.setInt(6, subject.isAnonymous() ? 1 : 0);
            preparedStatement.setString(7, emptyToNull(subject.getStatus()));
            preparedStatement.setString(8, emptyToNull(subject.getCategory()));
            preparedStatement.setString(9, emptyToNull(subject.getAttachmentPath()));
            preparedStatement.setString(10, emptyToNull(subject.getAttachmentMimeType()));
            if (subject.getAttachmentSize() == null) {
                preparedStatement.setNull(11, java.sql.Types.BIGINT);
            } else {
                preparedStatement.setLong(11, subject.getAttachmentSize());
            }
            preparedStatement.setInt(12, subject.getIdUser());
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    subject.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public void updateSubject(ForumSubject subject) throws SQLException {
        String sql = """
                UPDATE sujet_forum
                SET titre = ?, description = ?, image_url = ?, is_pinned = ?, is_anonymous = ?,
                    status = ?, category = ?, attachment_path = ?, attachment_mime_type = ?, attachment_size = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, subject.getTitre());
            preparedStatement.setString(2, emptyToNull(subject.getDescription()));
            preparedStatement.setString(3, emptyToNull(subject.getImageUrl()));
            preparedStatement.setInt(4, subject.isPinned() ? 1 : 0);
            preparedStatement.setInt(5, subject.isAnonymous() ? 1 : 0);
            preparedStatement.setString(6, emptyToNull(subject.getStatus()));
            preparedStatement.setString(7, emptyToNull(subject.getCategory()));
            preparedStatement.setString(8, emptyToNull(subject.getAttachmentPath()));
            preparedStatement.setString(9, emptyToNull(subject.getAttachmentMimeType()));
            if (subject.getAttachmentSize() == null) {
                preparedStatement.setNull(10, java.sql.Types.BIGINT);
            } else {
                preparedStatement.setLong(10, subject.getAttachmentSize());
            }
            preparedStatement.setInt(11, subject.getId());
            preparedStatement.executeUpdate();
        }
    }

    public void deleteSubject(int id) throws SQLException {
        String sql = "DELETE FROM sujet_forum WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
        }
    }

    public List<ForumSubject> getSubjects(String query, String sortBy) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT s.id, s.titre, s.description, s.date_creation, s.image_url, s.is_pinned, s.is_anonymous,
                       s.status, s.category, s.attachment_path, s.attachment_mime_type, s.attachment_size, s.id_user,
                       u.username
                FROM sujet_forum s
                LEFT JOIN app_user u ON u.id = s.id_user
                """);
        List<String> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            sql.append("WHERE s.titre LIKE ? OR s.description LIKE ? OR s.category LIKE ? OR s.status LIKE ? ");
            String search = "%" + query.trim() + "%";
            for (int i = 0; i < 4; i++) {
                params.add(search);
            }
        }

        String orderBy = "s.date_creation DESC";
        if (sortBy != null) {
            switch (sortBy) {
                case "Date":
                    orderBy = "s.date_creation DESC";
                    break;
                case "Pinned":
                    orderBy = "s.is_pinned DESC, s.date_creation DESC";
                    break;
                case "Categorie":
                    orderBy = "s.category";
                    break;
                case "Statut":
                    orderBy = "s.status";
                    break;
                default:
                    orderBy = "s.date_creation DESC";
                    break;
            }
        }
        sql.append("ORDER BY ").append(orderBy);

        List<ForumSubject> subjects = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                preparedStatement.setString(i + 1, params.get(i));
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    subjects.add(mapSubject(resultSet));
                }
            }
        }

        return subjects;
    }

    private ForumSubject mapSubject(ResultSet resultSet) throws SQLException {
        Timestamp dateCreation = resultSet.getTimestamp("date_creation");

        return new ForumSubject(
                resultSet.getInt("id"),
                resultSet.getString("titre"),
                resultSet.getString("description"),
                dateCreation == null ? LocalDateTime.now() : dateCreation.toLocalDateTime(),
                resultSet.getString("image_url"),
                resultSet.getInt("is_pinned") == 1,
                resultSet.getInt("is_anonymous") == 1,
                resultSet.getString("status"),
                resultSet.getString("category"),
                resultSet.getString("attachment_path"),
                resultSet.getString("attachment_mime_type"),
                resultSet.getObject("attachment_size") == null ? null : resultSet.getLong("attachment_size"),
                resultSet.getInt("id_user"),
                resultSet.getString("username")
        );
    }

    private void ensureColumnExists(Statement statement, String columnName, String alterSql) throws SQLException {
        try (ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM sujet_forum LIKE '" + columnName + "'")) {
            if (!columns.next()) {
                statement.execute(alterSql);
            }
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
