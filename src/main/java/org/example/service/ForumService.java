package org.example.service;

import org.example.model.ForumSubject;
import org.example.repository.ForumRepository;
import org.example.repository.impl.ForumRepositoryImpl;

import java.sql.SQLException;
import java.util.List;

public class ForumService {
    private final ForumRepository forumRepository;

    public ForumService() {
        this.forumRepository = new ForumRepositoryImpl();
    }
    public List<ForumSubject> getAllSubjects() throws SQLException {
        return forumRepository.findAll();
    }
    public void createTableIfNotExists() throws SQLException {
        forumRepository.createTableIfNotExists();
    }

    public void addSubject(ForumSubject subject) throws SQLException {
        if (subject == null || subject.getTitre() == null || subject.getTitre().isBlank()) {
            throw new SQLException("Subject title is required.");
        }
        forumRepository.save(subject);
    }

    public void updateSubject(ForumSubject subject) throws SQLException {
        if (subject == null || subject.getId() <= 0) {
            throw new SQLException("Valid subject ID is required.");
        }
        forumRepository.update(subject);
    }

    public void deleteSubject(int id) throws SQLException {
        if (id <= 0) {
            throw new SQLException("Valid subject ID is required.");
        }
        forumRepository.delete(id);
    }

    public List<ForumSubject> getSubjects(String query, String sortBy) throws SQLException {
        return forumRepository.findByQuery(query, sortBy);
    }

    public List<ForumSubject> getSubjects(String query, String sortBy, Integer userId) throws SQLException {
        return forumRepository.findByQuery(query, sortBy, userId);
    }

    public void reactToSubject(int subjectId, int userId, boolean like) throws SQLException {
        if (subjectId <= 0) {
            throw new SQLException("Valid subject ID is required.");
        }
        if (userId <= 0) {
            throw new SQLException("Valid user ID is required.");
        }
        forumRepository.reactToSubject(subjectId, userId, like);
    }
}
