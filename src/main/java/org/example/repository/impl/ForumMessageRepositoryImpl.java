package org.example.repository.impl;

import org.example.config.DatabaseConnection;
import org.example.model.ForumMessage;
import org.example.repository.ForumMessageRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ForumMessageRepositoryImpl implements ForumMessageRepository {
    private static final String MESSAGE_TABLE = "message_forum";
    private static final String REACTION_TABLE = "message_forum_reaction";

    @Override
    public void createTableIfNotExists() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS message_forum (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    contenu LONGTEXT NOT NULL,
                    date_message DATETIME NOT NULL,
                    is_anonymous TINYINT(1) NOT NULL,
                    attachment_path VARCHAR(255),
                    attachment_mime_type VARCHAR(100),
                    attachment_size INT,
                    id_sujet INT NOT NULL,
                    id_user INT NOT NULL,
                    parent_message_id INT
                )
                """;

        String reactionSql = """
                CREATE TABLE IF NOT EXISTS message_forum_reaction (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    id_message INT NOT NULL,
                    id_user INT NOT NULL,
                    is_like TINYINT(1) NOT NULL,
                    reacted_at DATETIME NOT NULL,
                    UNIQUE KEY uq_message_user (id_message, id_user)
                )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            statement.execute(reactionSql);
        }
    }

    @Override
    public void save(ForumMessage message) throws SQLException {
        String sql = """
                INSERT INTO message_forum
                (contenu, date_message, is_anonymous, attachment_path, attachment_mime_type, attachment_size, id_sujet, id_user, parent_message_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime dateMessage = message.getDateMessage() != null ? message.getDateMessage() : LocalDateTime.now();
            statement.setString(1, message.getContenu());
            statement.setTimestamp(2, Timestamp.valueOf(dateMessage));
            statement.setBoolean(3, message.isAnonymous());
            statement.setString(4, normalize(message.getAttachmentPath()));
            statement.setString(5, normalize(message.getAttachmentMimeType()));
            if (message.getAttachmentSize() == null) {
                statement.setNull(6, java.sql.Types.INTEGER);
            } else {
                statement.setLong(6, message.getAttachmentSize());
            }
            statement.setInt(7, message.getIdSujet());
            statement.setInt(8, message.getIdUser());
            if (message.getParentMessageId() == null) {
                statement.setNull(9, java.sql.Types.INTEGER);
            } else {
                statement.setInt(9, message.getParentMessageId());
            }
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    message.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String deleteReactionSql = "DELETE FROM " + REACTION_TABLE + " WHERE id_message = ?";
        String deleteMessageSql = "DELETE FROM " + MESSAGE_TABLE + " WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement deleteReactionStatement = connection.prepareStatement(deleteReactionSql);
             PreparedStatement deleteMessageStatement = connection.prepareStatement(deleteMessageSql)) {
            deleteReactionStatement.setInt(1, id);
            deleteReactionStatement.executeUpdate();

            deleteMessageStatement.setInt(1, id);
            deleteMessageStatement.executeUpdate();
        }
    }

    @Override
    public void deleteBySubjectId(int subjectId) throws SQLException {
        String deleteReactionSql = "DELETE mr FROM " + REACTION_TABLE + " mr "
                + "JOIN " + MESSAGE_TABLE + " m ON m.id = mr.id_message WHERE m.id_sujet = ?";
        String deleteMessageSql = "DELETE FROM " + MESSAGE_TABLE + " WHERE id_sujet = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement deleteReactionStatement = connection.prepareStatement(deleteReactionSql);
             PreparedStatement deleteMessageStatement = connection.prepareStatement(deleteMessageSql)) {
            deleteReactionStatement.setInt(1, subjectId);
            deleteReactionStatement.executeUpdate();

            deleteMessageStatement.setInt(1, subjectId);
            deleteMessageStatement.executeUpdate();
        }
    }

    @Override
    public List<ForumMessage> findBySubjectId(int subjectId) throws SQLException {
        return findBySubjectId(subjectId, null);
    }

    @Override
    public List<ForumMessage> findBySubjectId(int subjectId, Integer userId) throws SQLException {
        String sql = """
              SELECT m.id, m.contenu, m.date_message, m.is_anonymous, m.attachment_path,
                  m.attachment_mime_type, m.attachment_size, m.id_sujet, m.id_user, m.parent_message_id, u.username,
                  (SELECT COUNT(*) FROM message_forum_reaction mr WHERE mr.id_message = m.id AND mr.is_like = 1) AS like_count,
                  (SELECT COUNT(*) FROM message_forum_reaction mr WHERE mr.id_message = m.id AND mr.is_like = 0) AS dislike_count,
                  (SELECT mr.is_like FROM message_forum_reaction mr WHERE mr.id_message = m.id AND mr.id_user = ? LIMIT 1) AS user_reaction_like
                FROM message_forum m
                LEFT JOIN users u ON u.id = m.id_user
                WHERE m.id_sujet = ?
                ORDER BY m.date_message ASC
                """;

        List<ForumMessage> messages = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int idx = 1;
            if (userId == null) {
                statement.setNull(idx++, java.sql.Types.INTEGER);
            } else {
                statement.setInt(idx++, userId);
            }
            statement.setInt(idx, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    messages.add(mapMessage(resultSet));
                }
            }
        }
        return messages;
    }

    @Override
    public ForumMessage findById(int id) throws SQLException {
        String sql = """
                SELECT m.id, m.contenu, m.date_message, m.is_anonymous, m.attachment_path,
                  m.attachment_mime_type, m.attachment_size, m.id_sujet, m.id_user, m.parent_message_id, u.username,
                  0 AS like_count, 0 AS dislike_count, NULL AS user_reaction_like
                FROM message_forum m
                LEFT JOIN users u ON u.id = m.id_user
                WHERE m.id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapMessage(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public void reactToMessage(int messageId, int userId, boolean like) throws SQLException {
        String selectSql = "SELECT is_like FROM " + REACTION_TABLE + " WHERE id_message = ? AND id_user = ?";
        String insertSql = "INSERT INTO " + REACTION_TABLE + " (id_message, id_user, is_like, reacted_at) VALUES (?, ?, ?, ?)";
        String updateSql = "UPDATE " + REACTION_TABLE + " SET is_like = ?, reacted_at = ? WHERE id_message = ? AND id_user = ?";
        String deleteSql = "DELETE FROM " + REACTION_TABLE + " WHERE id_message = ? AND id_user = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement selectStatement = connection.prepareStatement(selectSql)) {
            selectStatement.setInt(1, messageId);
            selectStatement.setInt(2, userId);
            try (ResultSet resultSet = selectStatement.executeQuery()) {
                if (resultSet.next()) {
                    boolean existingLike = resultSet.getBoolean("is_like");
                    if (existingLike == like) {
                        try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql)) {
                            deleteStatement.setInt(1, messageId);
                            deleteStatement.setInt(2, userId);
                            deleteStatement.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                            updateStatement.setBoolean(1, like);
                            updateStatement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                            updateStatement.setInt(3, messageId);
                            updateStatement.setInt(4, userId);
                            updateStatement.executeUpdate();
                        }
                    }
                } else {
                    try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                        insertStatement.setInt(1, messageId);
                        insertStatement.setInt(2, userId);
                        insertStatement.setBoolean(3, like);
                        insertStatement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                        insertStatement.executeUpdate();
                    }
                }
            }
        }
    }

    private ForumMessage mapMessage(ResultSet resultSet) throws SQLException {
        Integer parentMessageId = resultSet.getObject("parent_message_id") == null
                ? null
                : resultSet.getInt("parent_message_id");
        Boolean userReactionLike = resultSet.getObject("user_reaction_like") == null
                ? null
                : resultSet.getBoolean("user_reaction_like");

        return new ForumMessage(
                resultSet.getInt("id"),
                resultSet.getString("contenu"),
                toLocalDateTime(resultSet.getTimestamp("date_message")),
                resultSet.getBoolean("is_anonymous"),
                resultSet.getString("attachment_path"),
                resultSet.getString("attachment_mime_type"),
                resultSet.getObject("attachment_size") != null ? resultSet.getLong("attachment_size") : null,
                resultSet.getInt("id_sujet"),
                resultSet.getInt("id_user"),
                resultSet.getString("username"),
                parentMessageId,
                resultSet.getInt("like_count"),
                resultSet.getInt("dislike_count"),
                userReactionLike
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
