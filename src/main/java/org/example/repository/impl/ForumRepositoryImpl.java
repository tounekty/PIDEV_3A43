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
    private static final String REACTION_TABLE = "sujet_forum_reaction";

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

        String reactionSql = """
                CREATE TABLE IF NOT EXISTS sujet_forum_reaction (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    id_sujet INT NOT NULL,
                    id_user INT NOT NULL,
                    is_like TINYINT(1) NOT NULL,
                    reacted_at DATETIME NOT NULL,
                    UNIQUE KEY uq_subject_user (id_sujet, id_user)
                )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            statement.execute(reactionSql);
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
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    subject.setId(generatedKeys.getInt(1));
                }
            }
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
        String deleteReactionSql = "DELETE FROM " + REACTION_TABLE + " WHERE id_sujet = ?";
        String deleteSubjectSql = "DELETE FROM sujet_forum WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement deleteReactionStatement = connection.prepareStatement(deleteReactionSql);
             PreparedStatement deleteSubjectStatement = connection.prepareStatement(deleteSubjectSql)) {
            deleteReactionStatement.setInt(1, id);
            deleteReactionStatement.executeUpdate();

            deleteSubjectStatement.setInt(1, id);
            deleteSubjectStatement.executeUpdate();
        }
    }

    @Override
    public List<ForumSubject> findAll() throws SQLException {
        return findAll(null);
    }

    @Override
    public List<ForumSubject> findAll(Integer userId) throws SQLException {
        String sql = baseSelect() + " ORDER BY s.is_pinned DESC, s.date_creation DESC";
        return querySubjects(sql, null, userId);
    }

    @Override
    public List<ForumSubject> findByQuery(String query, String sortBy) throws SQLException {
        return findByQuery(query, sortBy, null);
    }

    @Override
    public List<ForumSubject> findByQuery(String query, String sortBy, Integer userId) throws SQLException {
        String orderBy = orderByClause(sortBy);
        if (query == null || query.isBlank()) {
            String sql = baseSelect() + " " + orderBy;
            return querySubjects(sql, null, userId);
        }

        String sql = baseSelect()
                + " WHERE (LOWER(s.titre) LIKE ? OR LOWER(s.category) LIKE ? OR LOWER(s.status) LIKE ? OR LOWER(s.description) LIKE ?) "
                + orderBy;
        String search = "%" + query.toLowerCase() + "%";
        return querySubjects(sql, new String[]{search, search, search, search}, userId);
    }

    @Override
    public ForumSubject findById(int id) throws SQLException {
        return findById(id, null);
    }

    @Override
    public ForumSubject findById(int id, Integer userId) throws SQLException {
        String sql = baseSelect() + " WHERE s.id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int idx = 1;
            if (userId == null) {
                statement.setNull(idx++, java.sql.Types.INTEGER);
            } else {
                statement.setInt(idx++, userId);
            }
            statement.setInt(idx, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapSubject(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public void reactToSubject(int subjectId, int userId, boolean like) throws SQLException {
        String selectSql = "SELECT is_like FROM " + REACTION_TABLE + " WHERE id_sujet = ? AND id_user = ?";
        String insertSql = "INSERT INTO " + REACTION_TABLE + " (id_sujet, id_user, is_like, reacted_at) VALUES (?, ?, ?, ?)";
        String updateSql = "UPDATE " + REACTION_TABLE + " SET is_like = ?, reacted_at = ? WHERE id_sujet = ? AND id_user = ?";
        String deleteSql = "DELETE FROM " + REACTION_TABLE + " WHERE id_sujet = ? AND id_user = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement selectStatement = connection.prepareStatement(selectSql)) {
            selectStatement.setInt(1, subjectId);
            selectStatement.setInt(2, userId);

            try (ResultSet resultSet = selectStatement.executeQuery()) {
                if (resultSet.next()) {
                    boolean existingLike = resultSet.getBoolean("is_like");
                    if (existingLike == like) {
                        try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql)) {
                            deleteStatement.setInt(1, subjectId);
                            deleteStatement.setInt(2, userId);
                            deleteStatement.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                            updateStatement.setBoolean(1, like);
                            updateStatement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                            updateStatement.setInt(3, subjectId);
                            updateStatement.setInt(4, userId);
                            updateStatement.executeUpdate();
                        }
                    }
                } else {
                    try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                        insertStatement.setInt(1, subjectId);
                        insertStatement.setInt(2, userId);
                        insertStatement.setBoolean(3, like);
                        insertStatement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                        insertStatement.executeUpdate();
                    }
                }
            }
        }
    }

    private List<ForumSubject> querySubjects(String sql, String[] params, Integer userId) throws SQLException {
        List<ForumSubject> subjects = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            if (userId == null) {
                statement.setNull(index++, java.sql.Types.INTEGER);
            } else {
                statement.setInt(index++, userId);
            }
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    statement.setString(index++, params[i]);
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
                + "s.category, s.attachment_path, s.attachment_mime_type, s.attachment_size, s.id_user, u.username, "
                + "(SELECT COUNT(*) FROM " + REACTION_TABLE + " sr WHERE sr.id_sujet = s.id AND sr.is_like = 1) AS like_count, "
                + "(SELECT COUNT(*) FROM " + REACTION_TABLE + " sr WHERE sr.id_sujet = s.id AND sr.is_like = 0) AS dislike_count, "
            + "(SELECT COUNT(*) FROM message_forum mf WHERE mf.id_sujet = s.id) AS message_count, "
                + "(SELECT sr.is_like FROM " + REACTION_TABLE + " sr WHERE sr.id_sujet = s.id AND sr.id_user = ? LIMIT 1) AS user_reaction_like "
                + "FROM " + TABLE_NAME + " s "
                + "LEFT JOIN users u ON u.id = s.id_user";
    }

    private String orderByClause(String sortBy) {
        if (sortBy == null) {
            return "ORDER BY s.is_pinned DESC, s.date_creation DESC";
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
        Boolean userReactionLike = resultSet.getObject("user_reaction_like") == null
            ? null
            : resultSet.getBoolean("user_reaction_like");
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
                resultSet.getString("username"),
                resultSet.getInt("like_count"),
                resultSet.getInt("dislike_count"),
                resultSet.getInt("message_count"),
                userReactionLike
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
