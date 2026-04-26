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

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
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
             PreparedStatement statement = connection.prepareStatement(sql)) {
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
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM message_forum WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteBySubjectId(int subjectId) throws SQLException {
        String sql = "DELETE FROM message_forum WHERE id_sujet = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, subjectId);
            statement.executeUpdate();
        }
    }

    @Override
    public List<ForumMessage> findBySubjectId(int subjectId) throws SQLException {
        String sql = """
              SELECT m.id, m.contenu, m.date_message, m.is_anonymous, m.attachment_path,
                  m.attachment_mime_type, m.attachment_size, m.id_sujet, m.id_user, m.parent_message_id, u.username
                FROM message_forum m
                LEFT JOIN users u ON u.id = m.id_user
                WHERE m.id_sujet = ?
                ORDER BY m.date_message ASC
                """;

        List<ForumMessage> messages = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, subjectId);
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
                       m.attachment_mime_type, m.attachment_size, m.id_sujet, m.id_user, m.parent_message_id, u.username
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

    private ForumMessage mapMessage(ResultSet resultSet) throws SQLException {
        Integer parentMessageId = resultSet.getObject("parent_message_id") == null
                ? null
                : resultSet.getInt("parent_message_id");

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
                parentMessageId
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
