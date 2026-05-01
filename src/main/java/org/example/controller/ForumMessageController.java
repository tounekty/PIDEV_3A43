package org.example.controller;

import org.example.model.ForumMessage;
import org.example.service.ForumMessageService;

import java.sql.SQLException;
import java.util.List;

public class ForumMessageController {
    private final ForumMessageService forumMessageService = new ForumMessageService();

    public void createTableIfNotExists() throws SQLException {
        forumMessageService.createTableIfNotExists();
    }

    public void addMessage(ForumMessage message) throws SQLException {
        forumMessageService.addMessage(message);
    }

    public List<ForumMessage> getMessagesBySubject(int subjectId) throws SQLException {
        return forumMessageService.getMessagesBySubject(subjectId);
    }

    public void deleteMessage(int id) throws SQLException {
        forumMessageService.deleteMessage(id);
    }

    public void deleteMessagesForSubject(int subjectId) throws SQLException {
        forumMessageService.deleteMessagesForSubject(subjectId);
    }
}
