package org.example.repository.impl;

import org.example.config.DatabaseConnection;
import org.example.model.ForumSubject;
import org.example.repository.ForumRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ForumRepositoryImpl implements ForumRepository {

    private static final String TABLE_NAME = "sujet_forum";

    @Override
    public void createTableIfNotExists() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS sujet_forum (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    titre VARCHAR(255) NOT NULL,
                    description TEXT,
                    date_creation DATETIME NOT NULL,
                    image_url VARCHAR(500),
                    is_pinned TINYINT(1) NOT NULL DEFAULT 0,
                    is_anonymous TINYINT(1) NOT NULL DEFAULT 0,
                    status VARCHAR(100),
                    category VARCHAR(100),
                    attachment_path VARCHAR(500),
                    attachment_mime_type VARCHAR(150),
                    attachment_size BIGINT,
                    id_user INT
                )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    @Override
    public void save(ForumSubject subject) throws SQLException {
        String sql = """
                INSERT INTO sujet_forum
                (titre, description, date_creation, image_url, is_pinned, is_anonymous, status, category,
                 attachment_path, attachment_mime_type, attachment_size, id_user)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, subject.getTitre());
            statement.setString(2, normalize(subject.getDescription()));
            statement.setTimestamp(3, toTimestamp(subject.getDateCreation()));
            statement.setString(4, normalize(subject.getImageUrl()));
            statement.setBoolean(5, subject.isPinned());
            statement.setBoolean(6, subject.isAnonymous());
            statement.setString(7, normalize(subject.getStatus()));
            statement.setString(8, normalize(subject.getCategory()));
            statement.setString(9, normalize(subject.getAttachmentPath()));
            statement.setString(10, normalize(subject.getAttachmentMimeType()));
            if (subject.getAttachmentSize() == null) {
                statement.setNull(11, java.sql.Types.BIGINT);
            } else {
                statement.setLong(11, subject.getAttachmentSize());
            }
            if (subject.getIdUser() == null) {
                statement.setNull(12, java.sql.Types.INTEGER);
            } else {
                statement.setInt(12, subject.getIdUser());
            }
            statement.executeUpdate();
        }
    }

    @Override
    public void update(ForumSubject subject) throws SQLException {
        String sql = """
                UPDATE sujet_forum
                SET titre = ?, description = ?, date_creation = ?, image_url = ?, is_pinned = ?, is_anonymous = ?,
                    status = ?, category = ?, attachment_path = ?, attachment_mime_type = ?,
                    attachment_size = ?, id_user = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, subject.getTitre());
            statement.setString(2, normalize(subject.getDescription()));
            statement.setTimestamp(3, toTimestamp(subject.getDateCreation()));
            statement.setString(4, normalize(subject.getImageUrl()));
            statement.setBoolean(5, subject.isPinned());
            statement.setBoolean(6, subject.isAnonymous());
            statement.setString(7, normalize(subject.getStatus()));
            statement.setString(8, normalize(subject.getCategory()));
            statement.setString(9, normalize(subject.getAttachmentPath()));
            statement.setString(10, normalize(subject.getAttachmentMimeType()));
            if (subject.getAttachmentSize() == null) {
                statement.setNull(11, java.sql.Types.BIGINT);
            } else {
                statement.setLong(11, subject.getAttachmentSize());
            }
            if (subject.getIdUser() == null) {
                statement.setNull(12, java.sql.Types.INTEGER);
            } else {
                statement.setInt(12, subject.getIdUser());
            }
            statement.setInt(13, subject.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM sujet_forum WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    @Override
    public List<ForumSubject> findAll() throws SQLException {
        String sql = baseSelect() + " ORDER BY s.is_pinned DESC, s.date_creation DESC";
        return querySubjects(sql, null);
    }

    @Override
    public List<ForumSubject> findByQuery(String query, String sortBy) throws SQLException {
        String orderBy = orderByClause(sortBy);
        if (query == null || query.isBlank()) {
            String sql = baseSelect() + " " + orderBy;
            return querySubjects(sql, null);
        }

        String sql = baseSelect()
                + " WHERE (LOWER(s.titre) LIKE ? OR LOWER(s.category) LIKE ? OR LOWER(s.status) LIKE ? OR LOWER(s.description) LIKE ?) "
                + orderBy;
        String search = "%" + query.toLowerCase() + "%";
        return querySubjects(sql, new String[]{search, search, search, search});
    }

    @Override
    public ForumSubject findById(int id) throws SQLException {
        String sql = baseSelect() + " WHERE s.id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapSubject(resultSet);
                }
            }
        }
        return null;
    }

    private List<ForumSubject> querySubjects(String sql, String[] params) throws SQLException {
        List<ForumSubject> subjects = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    statement.setString(i + 1, params[i]);
                }
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    subjects.add(mapSubject(resultSet));
                }
            }
        }

        return subjects;
    }

    private String baseSelect() {
        return "SELECT s.id, s.titre, s.description, s.date_creation, s.image_url, s.is_pinned, s.is_anonymous, s.status, "
                + "s.category, s.attachment_path, s.attachment_mime_type, s.attachment_size, s.id_user, u.username "
                + "FROM " + TABLE_NAME + " s "
                + "LEFT JOIN users u ON u.id = s.id_user";
    }

    private String orderByClause(String sortBy) {
        if (sortBy == null) {
            return "ORDER BY s.pinned DESC, s.date_creation DESC";
        }
        return switch (sortBy.trim()) {
            case "Date" -> "ORDER BY s.date_creation DESC";
            case "Pinned" -> "ORDER BY s.is_pinned DESC, s.date_creation DESC";
            case "Categorie" -> "ORDER BY s.category ASC, s.date_creation DESC";
            case "Statut" -> "ORDER BY s.status ASC, s.date_creation DESC";
            default -> "ORDER BY s.is_pinned DESC, s.date_creation DESC";
        };
    }

    private ForumSubject mapSubject(ResultSet resultSet) throws SQLException {
        LocalDateTime dateCreation = null;
        Timestamp timestamp = resultSet.getTimestamp("date_creation");
        if (timestamp != null) {
            dateCreation = timestamp.toLocalDateTime();
        }

        Integer idUser = resultSet.getObject("id_user") != null ? resultSet.getInt("id_user") : null;
        ForumSubject subject = new ForumSubject(
                resultSet.getInt("id"),
                resultSet.getString("titre"),
                resultSet.getString("description"),
                dateCreation,
                resultSet.getString("image_url"),
                resultSet.getBoolean("is_pinned"),
                resultSet.getBoolean("is_anonymous"),
                resultSet.getString("status"),
                resultSet.getString("category"),
                resultSet.getString("attachment_path"),
                resultSet.getString("attachment_mime_type"),
                resultSet.getObject("attachment_size") != null ? resultSet.getLong("attachment_size") : null,
                idUser,
                resultSet.getString("username")
        );
        return subject;
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return Timestamp.valueOf(value != null ? value : LocalDateTime.now());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
