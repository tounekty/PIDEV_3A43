package org.example.service;

import org.example.model.ForumMessage;
import org.example.repository.ForumMessageRepository;
import org.example.repository.impl.ForumMessageRepositoryImpl;

import java.sql.SQLException;
import java.util.List;

public class ForumMessageService {
    private final ForumMessageRepository forumMessageRepository;

    public ForumMessageService() {
        this.forumMessageRepository = new ForumMessageRepositoryImpl();
    }

    public void createTableIfNotExists() throws SQLException {
        forumMessageRepository.createTableIfNotExists();
    }

    public void addMessage(ForumMessage message) throws SQLException {
        if (message == null || message.getContenu() == null || message.getContenu().isBlank()) {
            throw new SQLException("Message content is required.");
        }
        forumMessageRepository.save(message);
    }

    public List<ForumMessage> getMessagesBySubject(int subjectId) throws SQLException {
        if (subjectId <= 0) {
            throw new SQLException("Valid subject ID is required.");
        }
        return forumMessageRepository.findBySubjectId(subjectId);
    }

    public void deleteMessage(int id) throws SQLException {
        if (id <= 0) {
            throw new SQLException("Valid message ID is required.");
        }
        forumMessageRepository.delete(id);
    }

    public void deleteMessagesForSubject(int subjectId) throws SQLException {
        if (subjectId <= 0) {
            throw new SQLException("Valid subject ID is required.");
        }
        forumMessageRepository.deleteBySubjectId(subjectId);
    }
}
