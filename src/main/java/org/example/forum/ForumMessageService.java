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

public class ForumMessageService {
    public void createTableIfNotExists() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS message_forum (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    contenu LONGTEXT NOT NULL,
                    date_message DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    is_anonymous TINYINT(1) NOT NULL DEFAULT 0,
                    attachment_path VARCHAR(500) NULL,
                    attachment_mime_type VARCHAR(120) NULL,
                    attachment_size BIGINT NULL,
                    id_sujet INT NOT NULL,
                    id_user INT NOT NULL
                )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            ensureColumnExists(statement, "contenu", "ALTER TABLE message_forum ADD COLUMN contenu LONGTEXT NOT NULL AFTER id");
            ensureColumnExists(statement, "date_message", "ALTER TABLE message_forum ADD COLUMN date_message DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER contenu");
            ensureColumnExists(statement, "is_anonymous", "ALTER TABLE message_forum ADD COLUMN is_anonymous TINYINT(1) NOT NULL DEFAULT 0 AFTER date_message");
            ensureColumnExists(statement, "attachment_path", "ALTER TABLE message_forum ADD COLUMN attachment_path VARCHAR(500) NULL AFTER is_anonymous");
            ensureColumnExists(statement, "attachment_mime_type", "ALTER TABLE message_forum ADD COLUMN attachment_mime_type VARCHAR(120) NULL AFTER attachment_path");
            ensureColumnExists(statement, "attachment_size", "ALTER TABLE message_forum ADD COLUMN attachment_size BIGINT NULL AFTER attachment_mime_type");
            ensureColumnExists(statement, "id_sujet", "ALTER TABLE message_forum ADD COLUMN id_sujet INT NOT NULL AFTER attachment_size");
            ensureColumnExists(statement, "id_user", "ALTER TABLE message_forum ADD COLUMN id_user INT NOT NULL AFTER id_sujet");
        }
    }

    public void addMessage(ForumMessage message) throws SQLException {
        String sql = """
                INSERT INTO message_forum(contenu, date_message, is_anonymous, attachment_path, attachment_mime_type,
                                          attachment_size, id_sujet, id_user)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, message.getContenu());
            preparedStatement.setTimestamp(2, Timestamp.valueOf(message.getDateMessage()));
            preparedStatement.setInt(3, message.isAnonymous() ? 1 : 0);
            preparedStatement.setString(4, emptyToNull(message.getAttachmentPath()));
            preparedStatement.setString(5, emptyToNull(message.getAttachmentMimeType()));
            if (message.getAttachmentSize() == null) {
                preparedStatement.setNull(6, java.sql.Types.BIGINT);
            } else {
                preparedStatement.setLong(6, message.getAttachmentSize());
            }
            preparedStatement.setInt(7, message.getIdSujet());
            preparedStatement.setInt(8, message.getIdUser());
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    message.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<ForumMessage> getMessagesBySubject(int subjectId) throws SQLException {
        String sql = """
                SELECT m.id, m.contenu, m.date_message, m.is_anonymous, m.attachment_path, m.attachment_mime_type,
                       m.attachment_size, m.id_sujet, m.id_user, u.username
                FROM message_forum m
                LEFT JOIN app_user u ON u.id = m.id_user
                WHERE m.id_sujet = ?
                ORDER BY m.date_message DESC
                """;
        List<ForumMessage> messages = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, subjectId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    messages.add(mapMessage(resultSet));
                }
            }
        }

        return messages;
    }

    public void deleteMessage(int id) throws SQLException {
        String sql = "DELETE FROM message_forum WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
        }
    }

    public void deleteMessagesForSubject(int subjectId) throws SQLException {
        String sql = "DELETE FROM message_forum WHERE id_sujet = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, subjectId);
            preparedStatement.executeUpdate();
        }
    }

    private ForumMessage mapMessage(ResultSet resultSet) throws SQLException {
        Timestamp dateMessage = resultSet.getTimestamp("date_message");

        return new ForumMessage(
                resultSet.getInt("id"),
                resultSet.getString("contenu"),
                dateMessage == null ? LocalDateTime.now() : dateMessage.toLocalDateTime(),
                resultSet.getInt("is_anonymous") == 1,
                resultSet.getString("attachment_path"),
                resultSet.getString("attachment_mime_type"),
                resultSet.getObject("attachment_size") == null ? null : resultSet.getLong("attachment_size"),
                resultSet.getInt("id_sujet"),
                resultSet.getInt("id_user"),
                resultSet.getString("username")
        );
    }

    private void ensureColumnExists(Statement statement, String columnName, String alterSql) throws SQLException {
        try (ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM message_forum LIKE '" + columnName + "'")) {
            if (!columns.next()) {
                statement.execute(alterSql);
            }
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
