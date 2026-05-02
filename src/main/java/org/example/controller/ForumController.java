package org.example.controller;

import org.example.model.ForumSubject;
import org.example.service.ForumService;

import java.sql.SQLException;
import java.util.List;


public class ForumController {
    private final ForumService forumService = new ForumService();

    public void createTableIfNotExists() throws SQLException {
        forumService.createTableIfNotExists();
    }

    public List<ForumSubject> getAllSubjects() throws SQLException {
        return forumService.getAllSubjects();  // Changed to use forumService
    }

    public void addSubject(ForumSubject subject) throws SQLException {
        forumService.addSubject(subject);
    }

    public void updateSubject(ForumSubject subject) throws SQLException {
        forumService.updateSubject(subject);
    }

    public void deleteSubject(int id) throws SQLException {
        forumService.deleteSubject(id);
    }

    public List<ForumSubject> getSubjects(String query, String sortBy) throws SQLException {
        return forumService.getSubjects(query, sortBy);
    }

    public List<ForumSubject> getSubjects(String query, String sortBy, Integer userId) throws SQLException {
        return forumService.getSubjects(query, sortBy, userId);
    }

    public void reactToSubject(int subjectId, int userId, boolean like) throws SQLException {
        forumService.reactToSubject(subjectId, userId, like);
    }
}