package org.example.service;

import org.example.model.ForumMessage;
import org.example.repository.ForumMessageRepository;
import org.example.repository.impl.ForumMessageRepositoryImpl;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ForumMessageService {
    public static final int MAX_THREAD_LEVEL = 3;
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
        Integer parentMessageId = message.getParentMessageId();
        if (parentMessageId != null) {
            if (parentMessageId <= 0) {
                throw new SQLException("Parent message ID is invalid.");
            }
            ForumMessage parent = forumMessageRepository.findById(parentMessageId);
            if (parent == null) {
                throw new SQLException("Parent message not found.");
            }
            if (parent.getIdSujet() != message.getIdSujet()) {
                throw new SQLException("Parent message belongs to another subject.");
            }
            int parentLevel = computeLevel(parent);
            if (parentLevel >= MAX_THREAD_LEVEL) {
                throw new SQLException("Le niveau maximum de reponse (3) est atteint.");
            }
            message.setThreadLevel(parentLevel + 1);
        } else {
            message.setThreadLevel(1);
        }
        forumMessageRepository.save(message);
    }

    public List<ForumMessage> getMessagesBySubject(int subjectId) throws SQLException {
        return getMessagesBySubject(subjectId, null);
    }

    public List<ForumMessage> getMessagesBySubject(int subjectId, Integer userId) throws SQLException {
        if (subjectId <= 0) {
            throw new SQLException("Valid subject ID is required.");
        }
        return forumMessageRepository.findBySubjectId(subjectId, userId);
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

    public void reactToMessage(int messageId, int userId, boolean like) throws SQLException {
        if (messageId <= 0) {
            throw new SQLException("Valid message ID is required.");
        }
        if (userId <= 0) {
            throw new SQLException("Valid user ID is required.");
        }
        forumMessageRepository.reactToMessage(messageId, userId, like);
    }

    private int computeLevel(ForumMessage message) throws SQLException {
        int level = 1;
        Integer currentParentId = message.getParentMessageId();
        Set<Integer> visited = new HashSet<>();
        visited.add(message.getId());

        while (currentParentId != null) {
            if (!visited.add(currentParentId)) {
                throw new SQLException("Circular reply chain detected.");
            }
            ForumMessage parent = forumMessageRepository.findById(currentParentId);
            if (parent == null) {
                break;
            }
            level++;
            currentParentId = parent.getParentMessageId();
        }
        return level;
    }
}
